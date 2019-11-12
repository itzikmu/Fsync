package Client;

import Helper.FolderSync;

import name.pachler.nio.file.*;
import name.pachler.nio.file.ext.ExtendedWatchEventKind;
import name.pachler.nio.file.ext.ExtendedWatchEventModifier;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

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
    private static Boolean baseDirExists;
    private static WatchService watcher;

    public static void main(String args[]) throws Exception {
        System.out.println("Starting File Sync client!");
        baseDir = "C:\\Temp\\TestFolder";
        FolderSync.clientBaseDir = baseDir;

        File baseDirFolder = new File(baseDir);
        if (!baseDirFolder.exists()) {
            baseDirFolder.mkdir();
        }

        // getting localhost ip
        InetAddress ip = InetAddress.getByName("localhost");

        // establish the connection
        s = new Socket(ip, ServerPort);
        oos = new ObjectOutputStream(s.getOutputStream());
        ois = new ObjectInputStream(s.getInputStream());

        FolderSync.getUpdate(s, ois, oos, baseDir);

        try {
            // Creates a instance of WatchService.
            watcher = FileSystems.getDefault().newWatchService();

            // Registers the logDir below with a watch service.
            Path folderDir = Paths.get(baseDir);
//            folderDir.register(watcher, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
//            registerRecursive(folderDir);

//            folderDir.register(watcher, new WatchEvent.Kind[] {StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY}, ExtendedWatchEventModifier.FILE_TREE);
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
                        continue;
                    }

                    if (ENTRY_CREATE.equals(kind)) {
                        System.out.println("Entry " + fileEntry.toString() + " was created on log dir.");

                    } else if (ENTRY_MODIFY.equals(kind)) {
                        System.out.println("Entry " + fileEntry.toString() + " was modified on log dir.");

                    } else if (ENTRY_DELETE.equals(kind)) {
                        System.out.println("Entry " + fileEntry.toString() + " was deleted from log dir.");

                    } else if (ENTRY_RENAME_FROM.equals(kind)) {
                        System.out.println("Entry " + fileEntry.toString() + " was renamed on log dir.");
                        pathRenameFrom = fileEntry;

                    } else if (ENTRY_RENAME_TO.equals(kind)) {
                        System.out.println("Entry " + fileEntry.toString() + " was renamed on log dir.");
                        pathRenameTo = fileEntry;
                    }

                    if (ENTRY_CREATE.equals(kind) ||
                            ENTRY_MODIFY.equals(kind) ||
                            ENTRY_DELETE.equals(kind)){
                        syncServer();

                    } else if (ENTRY_RENAME_TO.equals(kind)){
                        renameFile(pathRenameFrom, pathRenameTo);
                    }
                }
                key.reset();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void syncServer() throws Exception {
        File baseDirFolder = new File(baseDir);
        FolderSync.visitAllDirsAndFiles(s, ois, oos, baseDirFolder, true);

        oos.writeObject(new String(FolderSync.DONE));
        oos.flush();
        System.out.println("sync finished ...");
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

        oos.writeObject(new String(FolderSync.DONE));
        oos.flush();
        System.out.println("sync finished ...");


    }

//    private static void registerRecursive(Path root) throws IOException {
//        // register all subfolders
//        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
//            @Override
//            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
//                dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
//                return FileVisitResult.CONTINUE;
//            }
//        });
//    }

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