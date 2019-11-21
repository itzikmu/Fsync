package Client;

import Helper.FolderSync;

import name.pachler.nio.file.*;
import name.pachler.nio.file.ext.ExtendedWatchEventKind;
import name.pachler.nio.file.ext.ExtendedWatchEventModifier;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

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
    private static WatchService watcher;
    private  static Object readObject;
    private  static Thread watcherThread;
    private  static Thread readThread;

    public static void main(String args[]) throws Exception {
        System.out.println("Starting File Sync client!");

        System.out.println("Your baseDir is: " + args[0]);
//        baseDir = "C:\\Temp\\TestFolder";
        baseDir = args[0];

        File baseDirFolder = new File(baseDir);
        if (!baseDirFolder.exists()) {
            baseDirFolder.mkdir();
        }

        System.setProperty("user.dir", baseDir);

        // getting localhost ip
        InetAddress ip = InetAddress.getByName("localhost");

        // establish the connection
        s = new Socket(ip, ServerPort);
        oos = new ObjectOutputStream(s.getOutputStream());
        ois = new ObjectInputStream(s.getInputStream());


        FolderSync.getUpdate(s, ois, oos, "MODIFY");
        runReadThread();

        syncServer();

        runReadThread();
        runWatcherThread();


    }
    private static void runReadThread() throws Exception {

        readThread = new Thread() {
            public void run() {
                try {
                    System.out.println("ReadThread:listening for server messages");
                    readObject = ois.readObject();
                    System.out.println("ReadThread:got message from server " + readObject.toString());
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


    private static void runWatcherThread() throws Exception {

         watcherThread = new Thread() {
            public void run() {
                try {
                    // Creates a instance of WatchService.
                    watcher = FileSystems.getDefault().newWatchService();

                    // Registers the logDir below with a watch service.
                    Path folderDir = Paths.get(baseDir);
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



                    // Monitor the logDir at listen for change notification.
                    Path pathRenameFrom = null, pathRenameTo = null;
                    while (true) {

                        WatchKey key = watcher.take();

                        for (WatchEvent<?> event : key.pollEvents()) {

                            WatchEvent.Kind<?> kind = event.kind();

                            // Retrieve the file name associated with the event
                            Path fileEntry = (Path) event.context();

                            if (kind == ExtendedWatchEventKind.KEY_INVALID) {
                                System.out.println("continue.................");
                                continue;
                            }

                            // NOW send 'Im ALlive' TODO: 19/11/2019 send client logout
                            //keepAlive();

                            if (ENTRY_CREATE.equals(kind) ||
                                    ENTRY_MODIFY.equals(kind)) {
                                System.out.println("file " + fileEntry.toString() + " was modified on client dir.");
                                syncServer();

                            } else if (ENTRY_DELETE.equals(kind)) {
                                System.out.println("file " + fileEntry.toString() + " was deleted from client dir.");
                                deleteFile(fileEntry);

                            } else if (ENTRY_RENAME_FROM.equals(kind)) {
                                System.out.println("file " + fileEntry.toString() + " was renamed on client dir.");
                                pathRenameFrom = fileEntry;

                            } else if (ENTRY_RENAME_TO.equals(kind)) {
                                System.out.println("file " + fileEntry.toString() + " was renamed on client dir.");
                                pathRenameTo = fileEntry;
                                renameFile(pathRenameFrom, pathRenameTo);
                            } else
                                System.out.println("continue !!!!!");
                        }
                        key.reset();
                        runReadThread();

                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };


        watcherThread.start();

    }



    private static void syncServer() throws Exception {
        File baseDirFolder = new File(baseDir);

        oos.writeObject(new String(FolderSync.MODIFY));
        oos.flush();
        while(readObject == null)
        { ; }


        FolderSync.sendUpdate(s, ois, oos, baseDirFolder, baseDir.length(), true);

        done();
    }

    private static void deleteFile(Path pathToDelete) throws Exception {
        File fileToDelete = new File(pathToDelete.toString());

        oos.writeObject(new String(FolderSync.DELETE));
        oos.flush();
        ois.readObject();

        oos.writeObject(fileToDelete.toString());
        oos.flush();
        ois.readObject();
    }

    private static void renameFile(Path pathRenameFrom, Path pathRenameTo) throws Exception {
        File fileRenameFrom = new File(pathRenameFrom.toString());
        File fileRenameTo = new File(pathRenameTo.toString());

        oos.writeObject(new String(FolderSync.RENAME));
        oos.flush();
        ois.readObject();

        oos.writeObject(fileRenameFrom.toString());
        oos.flush();
        ois.readObject();

        oos.writeObject(fileRenameTo.toString());
        oos.flush();
        ois.readObject();
    }

    private static void done() throws Exception {
        oos.writeObject(new String(FolderSync.DONE));
        oos.flush();
        System.out.println("client sync finished ...");
    }

//        // sendMessage thread
//        Thread sendMessage = new Thread(new Runnable()
//        {
//            @Override
//            public void run() {
//                while (true) {
//
//                    // read the message to deliver.
//                    String msg = scn.nextLine();
//
//                    try {
//                        // write on the output stream
//                        dos.writeUTF(msg);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        });
//
//        // readMessage thread
//        Thread readMessage = new Thread(new Runnable()
//        {
//            @Override
//            public void run() {
//
//                while (true) {
//                    try {
//                        // read the message sent to this client
//                        String msg = dis.readUTF();
//                        System.out.println(msg);
//                    } catch (IOException e) {
//
//                        e.printStackTrace();
//                    }
//                }
//            }
//        });
//
//        sendMessage.start();
//        readMessage.start();
//    )


}