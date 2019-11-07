package Client;

import Helper.FolderSync;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.*;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class Client {
    final static int ServerPort = 1234;

    private static final String DONE = "DONE";
    private static Socket s;
    private static ObjectOutputStream oos;
    private static ObjectInputStream ois;
    private static String baseDir;
    private static Boolean baseDirExists;

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
//            System.out.println("in watch");

            // Creates a instance of WatchService.
            WatchService watcher = FileSystems.getDefault().newWatchService();

            // Registers the logDir below with a watch service.
            Path folderDir = Paths.get(baseDir);
            folderDir.register(watcher, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);

            // Monitor the logDir at listen for change notification.
            while (true) {
                WatchKey key = watcher.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    if (ENTRY_CREATE.equals(kind)) {
                        System.out.println("Entry was created on log dir.");
                    } else if (ENTRY_MODIFY.equals(kind)) {
                        System.out.println("Entry was modified on log dir.");
                    } else if (ENTRY_DELETE.equals(kind)) {
                        System.out.println("Entry was deleted from log dir.");
                    }

                    if (ENTRY_CREATE.equals(kind) || ENTRY_MODIFY.equals(kind) || ENTRY_DELETE.equals(kind))
                        syncServer();

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

        oos.writeObject(new String(DONE));
        oos.flush();
        System.out.println("sync finished ...");
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