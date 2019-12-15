package Client;

import Helper.FolderSync;

import name.pachler.nio.file.*;
import name.pachler.nio.file.ext.ExtendedWatchEventKind;
import name.pachler.nio.file.ext.ExtendedWatchEventModifier;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.rmi.server.ExportException;
import java.util.ArrayList;
import java.util.List;

import static name.pachler.nio.file.StandardWatchEventKind.ENTRY_CREATE;
import static name.pachler.nio.file.StandardWatchEventKind.ENTRY_DELETE;
import static name.pachler.nio.file.StandardWatchEventKind.ENTRY_MODIFY;
import static name.pachler.nio.file.ext.ExtendedWatchEventKind.ENTRY_RENAME_FROM;
import static name.pachler.nio.file.ext.ExtendedWatchEventKind.ENTRY_RENAME_TO;


public class Client {
    final static int ServerPort = 1234;

    private static Socket s;
    private static ObjectOutputStream oos;
    private static ObjectInputStream ois;
    private static String baseDir;
    private  static volatile  Object readObject;
    private  static Thread watcherThread;
    private  static Thread readThread;
    private  static volatile boolean needToGetUpdate = false;




    public static void main(String args[]) throws Exception {
        System.out.println("Starting File Sync client!");

        System.out.println("Your baseDir is: " + args[0]);
        baseDir = args[0];

        File baseDirFolder = new File(baseDir);
        if (!baseDirFolder.exists()) {
            baseDirFolder.mkdir();
        }

        System.setProperty("user.dir", baseDir);

        // getting localhost ip
       InetAddress ip = InetAddress.getByName("localhost");
     //   String ip = args[1];

        // establish the connection
        s = new Socket(ip, ServerPort);
        oos = new ObjectOutputStream(s.getOutputStream());
        ois = new ObjectInputStream(s.getInputStream());


        // first two way sync
        ois.readObject();
        FolderSync.getUpdate(s, ois, oos, "MODIFY" , "");
        runReadThread();
        syncServer();


        needToGetUpdate = false;

        runReadThread();
        runWatcherThread();

        while(true)
        {
            if(needToGetUpdate && readObject !=null)
            {
                System.out.println("needToGetUpdate start");
                FolderSync.getUpdate(s, ois, oos, (String)readObject , "");
                readObject = null;
                runReadThread();

                Thread.sleep(100);
                System.out.println("needToGetUpdate end");
                needToGetUpdate=false;
                synchronized(watcherThread) {
                    watcherThread.notify();

                }
            }
        }

    }
    private static void runReadThread() throws Exception {

        readThread = new Thread() {
            public void run() {
                try {
                    System.out.println("ReadThread:listening for server messages");
                    Object temp = ois.readObject();
                    System.out.println("ReadThread:got message from server " + temp.toString());
                    if (temp instanceof String) {
                        needToGetUpdate = true;
                    }
                    readObject = temp;

                } catch (java.net.SocketException e) {
                    System.out.println("socket is closed " + e.getMessage());

                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        };


        readThread.start();

    }

     private static void registerWatcher(WatchService watcher, Path folderDir) {
         try {

             folderDir.register(watcher, new WatchEvent.Kind<?>[]{
                             ENTRY_RENAME_FROM,
                             ENTRY_RENAME_TO,
                             ENTRY_CREATE,
                             ENTRY_DELETE,
                             ENTRY_MODIFY
                     },
                     new WatchEvent.Modifier<?>[]{
                             ExtendedWatchEventModifier.ACCURATE,
                             ExtendedWatchEventModifier.FILE_TREE
                     }
             );
         } catch (Exception e) {
             e.printStackTrace();
         }
     }

    private static void runWatcherThread() throws Exception {

         watcherThread = new Thread() {
            public void run() {
                try {
                    // Creates a instance of WatchService.
                    WatchService watcher = FileSystems.getDefault().newWatchService();

                    // Registers the logDir below with a watch service.
                    Path folderDir = Paths.get(baseDir);

                    registerWatcher(watcher, folderDir);


                    // Monitor the logDir at listen for change notification.
                    Path pathRenameFrom = null, pathRenameTo = null;
                    WatchKey key;
                    boolean needToSyncClient ;
                    boolean needToRename;
                    List<String> filesToDelete = new ArrayList<String>();
                    while (true) {
                        System.out.println("watchdog is listening to folder " );
                        key  = watcher.take();
                        System.out.println("watchdog found new change" );

                        if(needToGetUpdate) {
                            synchronized(watcherThread) {
                                this.wait();
                            }
                            System.out.println("watchdog sleep" );
                            Thread.sleep(2000);
                            System.out.println("watchdog reset key watcher" );
                             watcher = FileSystems.getDefault().newWatchService();
                             registerWatcher(watcher, folderDir);

                            continue;
                        }


                        Thread.sleep(1000);
                        needToSyncClient = false;
                        needToRename = false;
                        filesToDelete.clear();

                        for (WatchEvent<?> event : key.pollEvents()) {
                            WatchEvent.Kind<?> kind = event.kind();
                            // Retrieve the file name associated with the event
                            Path fileEntry = (Path) event.context();
                            if (kind == ExtendedWatchEventKind.KEY_INVALID) {
                                System.out.println("continue.................");
                                continue;
                            }


                            if (ENTRY_CREATE.equals(kind) ||   ENTRY_MODIFY.equals(kind)) {

                                File newFile = new File(System.getProperty("user.dir") + "\\" + fileEntry.toString());

                                if(ENTRY_MODIFY.equals(kind) && newFile.isDirectory())
                                {
                                    //do nothing
                                }
                                else {
                                    System.out.println("file " + fileEntry.toString() + " was modified in client dir.");
                                    needToSyncClient = true;
                                }

                            } else if (ENTRY_DELETE.equals(kind)) {
                                System.out.println("file " + fileEntry.toString() + " was deleted from client dir.");
                                filesToDelete.add(fileEntry.toString());

                            } else if (ENTRY_RENAME_FROM.equals(kind)) {
                                System.out.println("file " + fileEntry.toString() + " was renamed on client dir.");
                                pathRenameFrom = fileEntry;

                            } else if (ENTRY_RENAME_TO.equals(kind)) {
                                System.out.println("file " + fileEntry.toString() + " was renamed on client dir.");
                                pathRenameTo = fileEntry;
                                sendRenameFile(pathRenameFrom, pathRenameTo);
                                needToRename = true;
                                runReadThread();

                            } else {
                                System.out.println("continue !!!!!");
                            }
                        }
                        if(filesToDelete.size() >  0) {
                            sendDeleteFile(filesToDelete);
                            runReadThread();
                        }
                        else if (needToSyncClient && !needToRename) {
                            System.out.println("some files were modified on client dir.");
                            syncServer();
                            runReadThread();
                        }

                        key.reset();

                        }




                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };


        watcherThread.start();

    }



    private static void syncServer() throws Exception {
        System.out.println("syncServer start");
        File baseDirFolder = new File(baseDir);

        oos.writeObject(FolderSync.MODIFY);
        oos.flush();
        while(readObject == null)
        { ; }
        readObject=null;

        System.out.println("sendUpdate " +baseDirFolder +" start");
        FolderSync.sendUpdate(s, ois, oos, baseDirFolder, baseDir.length(), true, "");
        System.out.println("sendUpdate " +baseDirFolder +" end");

        oos.writeObject(FolderSync.DONE);
        oos.flush();
        System.out.println("client sync finished ...");
    }

    private static void sendDeleteFile(List<String> filesToDelete) throws Exception {

        oos.writeObject(FolderSync.DELETE);
        oos.flush();
        while(readObject == null)
        { ; }


        for (String fileName : filesToDelete) {
            System.out.println("send deleteFile: " + fileName+ " start" );
            oos.writeObject(fileName);
            oos.flush();
            ois.readObject();
            System.out.println("send deleteFile: " + fileName+ " end" );
        }

        oos.writeObject(FolderSync.DONE);
        oos.flush();
        System.out.println("all deletes sent");

        readObject=null;
    }

    private static void sendRenameFile(Path pathRenameFrom, Path pathRenameTo) throws Exception {
        File fileRenameFrom = new File(pathRenameFrom.toString());
        File fileRenameTo = new File(pathRenameTo.toString());
        System.out.println("renameFile from" + pathRenameFrom.toString() + " to "  +pathRenameTo.toString() );

        oos.writeObject(FolderSync.RENAME);
        oos.flush();
        while(readObject == null)
        { ; }
        readObject=null;

        oos.writeObject(fileRenameFrom.toString());
        oos.flush();
        ois.readObject();

        oos.writeObject(fileRenameTo.toString());
        oos.flush();
        ois.readObject();
        System.out.println("fileRename from server end");
    }



}