package Helper;

import java.io.*;
import java.net.Socket;
import java.util.Vector;

public class FolderSync {

    public static final String DONE = "DONE";
    public static final String RENAME = "RENAME";
    public static final String DELETE = "DELETE";
    public static final String MODIFY = "MODIFY";
    public static String serverBaseDir = "";
    public static String clientBaseDir = "";


    // Process all files and directories under dir
    public static void sendUpdate(Socket sock, ObjectInputStream ois, ObjectOutputStream oos, File dir, int baseFolderLen) throws Exception {
        if (dir.getAbsolutePath() == serverBaseDir || dir.getAbsolutePath() == clientBaseDir) {
            System.out.println("sendUpdate - this is the base dir!");
        } else {
            //int baseFolderLen = (isClient ? clientBaseDir.length() : serverBaseDir.length());
            oos.writeObject(new String(dir.getAbsolutePath().substring(baseFolderLen)));
            oos.flush();

            ois.readObject(); // other side get the dir name

            Boolean isDirectory = dir.isDirectory();
            oos.writeObject(new Boolean(isDirectory)); //Boolean isDirectory
            oos.flush();

            if (isDirectory) {
                if (!(Boolean) ois.readObject()) { // dir NOT exist on the other side
                    oos.writeObject(new Boolean(true)); // send ok
                    oos.flush();
                }

            } else {
                if (!(Boolean) ois.readObject()) { // File NOT exist on the other side
                    oos.writeObject(new Boolean(true)); // ok
                    oos.flush();

                    Transfer.sendFile(sock, oos, dir);

                } else {
                    oos.writeObject(new Long(dir.lastModified())); // send last modified
                    oos.flush();

                    if ((Boolean) ois.readObject()) { // send update
                        Transfer.sendFile(sock, oos, dir);

                    } else { // DO NOTHING!
                        //System.out.println("Everything is UP TO DATE!");
                    }
                }
            }
        }

        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                sendUpdate(sock, ois, oos, new File(dir, children[i]), baseFolderLen);
            }
        }
    }

    public static void getUpdate(Socket sock, ObjectInputStream ois, ObjectOutputStream oos) throws Exception {
        String action = (String) ois.readObject();

        if (action.equals(RENAME)) {
            oos.writeObject(new Boolean(true)); //ok
            oos.flush();
            renameUpdate(sock, ois, oos);
        }

        if (action.equals(DELETE)) {
            oos.writeObject(new Boolean(true)); //ok
            oos.flush();
            deleteUpdate(sock, ois, oos);
        }

        if (action.equals(MODIFY)) {
            oos.writeObject(new Boolean(true)); //ok
            oos.flush();
            modifyUpdate(sock, ois, oos);
        }
    }

    private static void modifyUpdate(Socket sock, ObjectInputStream ois, ObjectOutputStream oos) throws Exception {
        Boolean isDone = false;

        while (!isDone) {
            String path = (String) ois.readObject(); // dir name

            if (path.equals(DONE)) {
                System.out.println("getUpdate done");
                isDone = true;
                break;
            }
            oos.writeObject(new Boolean(true)); // get the dir name
            oos.flush();

            File newFile = new File(System.getProperty("user.dir") + "\\" + path);

            Boolean isDirectory = (Boolean) ois.readObject(); //Boolean isDirectory

            oos.writeObject(new Boolean(newFile.exists())); // send if folder/file exist
            oos.flush();
            if (!newFile.exists()) {
                ois.readObject(); // ok

                if (isDirectory) {
                    newFile.mkdir();

                } else {
                    Transfer.receiveFile(sock, ois, newFile);

                }
            } else { // file already exist
                if (isDirectory)
                    continue;

                Long recvLastModified = (Long) ois.readObject(); // dir last modified on other side
                Long currLastModified = new Long(newFile.lastModified());

                if (recvLastModified > currLastModified) {
                    oos.writeObject(new Boolean(true)); // yes, give me update
                    oos.flush();

                    Transfer.receiveFile(sock, ois, newFile);

                    newFile.setLastModified(recvLastModified);

                } else {
                    oos.writeObject(new Boolean(false)); // no, I'm up to date
                    oos.flush();
                }
            }
        }
    }

    private static void renameUpdate(Socket sock, ObjectInputStream ois, ObjectOutputStream oos) throws Exception {
        File oldFile = new File( System.getProperty("user.dir") + "\\" + (String) ois.readObject());
        oos.writeObject(new Boolean(true)); //ok
        oos.flush();

        File newFile = new File(System.getProperty("user.dir") + "\\" + (String) ois.readObject());
        oos.writeObject(new Boolean(true)); //ok
        oos.flush();

        System.out.println("oldFile: " + oldFile.toString());
        System.out.println("newFile: " + newFile.toString());

        if (oldFile.renameTo(newFile)) {
            System.out.println("Rename successful");

        } else {
            System.out.println("Rename failed");
        }
    }

    private static void deleteUpdate(Socket sock, ObjectInputStream ois, ObjectOutputStream oos) throws Exception {
        File fileToDel = new File(System.getProperty("user.dir") + "\\" + (String) ois.readObject());
        oos.writeObject(new Boolean(true)); //ok
        oos.flush();

        if (fileToDel.delete()) {
            System.out.println("File deleted successfully");
        } else {
            System.out.println("Failed to delete the file");
        }
    }

    public static int fileCount(File file) {
        File[] files = file.listFiles();
        int count = 0;

        for (File f : files) {
            if (f.isDirectory())
                count += fileCount(f);
            else
                count++;
        }

        return count;
    }

//    public static void deleteAllDirsAndFiles(File dir) {
//        if (dir.isDirectory()) {
//            String[] children = dir.list();
//            for (int i = 0; i < children.length; i++) {
//                deleteAllDirsAndFiles(new File(dir, children[i]));
//            }
//        }
//        dir.delete();
//    }

}