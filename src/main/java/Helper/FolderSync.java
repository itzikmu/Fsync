package Helper;

import java.io.*;
import java.net.Socket;
import java.util.Vector;

public class FolderSync {

    private static final String DONE = "DONE";
    public static String serverBaseDir = "";
    public static String clientBaseDir = "";

    // Process all files and directories under dir
    public static void visitAllDirsAndFiles(Socket sock, ObjectInputStream ois, ObjectOutputStream oos, File dir, boolean isClient) throws Exception {
        if (dir.getAbsolutePath() == serverBaseDir || dir.getAbsolutePath() == serverBaseDir) {
            System.out.println("visitAllDirsAndFiles - this is the base dir!");
        } else {
            int baseFolderLen = (isClient ? clientBaseDir.length() : serverBaseDir.length());
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

//                    Boolean delete = (Boolean) ois.readObject();

//                    if (delete) {
//                        FolderSync.deleteAllDirsAndFiles(dir);
//                        return;
//                    } //ELSE DO NOTHING
                }
            } else {
                if (!(Boolean) ois.readObject()) { // File NOT exist on the other side
                    oos.writeObject(new Boolean(true)); // ok
                    oos.flush();

//                    Integer delete = (Integer) ois.readObject(); /// get if need to be delete
//
//                    if (delete == 1) {
//                        dir.delete();
//                        return;
//                    } else if (delete == 0) {
                    Transfer.sendFile(sock, oos, dir);

                    oos.writeObject(new Long(dir.lastModified()));
                    oos.flush();

//                    } // ELSE DO NOTHING!
                }
            }
        }
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                visitAllDirsAndFiles(sock, ois, oos, new File(dir, children[i]), isClient);
            }
        }
    }

    public static void getUpdate(Socket sock, ObjectInputStream ois, ObjectOutputStream oos, String fullDirName) throws Exception {
        Boolean isDone = false;
        while (!isDone) {
            Object obj = ois.readObject(); // dir name
            String path = (String) obj;

            if (path.equals(DONE)) {
                isDone = true;
                System.out.println("getUpdate done");
                break;
            }
            oos.writeObject(new Boolean(true)); // get the dir name
            oos.flush();

            File newFile = new File(fullDirName + path);
            Boolean isDirectory = (Boolean) ois.readObject(); //Boolean isDirectory

            oos.writeObject(new Boolean(newFile.exists())); // send if folder/file exist
            oos.flush();
            if (!newFile.exists()) {
                ois.readObject(); // ok
                if (isDirectory) {
                    newFile.mkdir();
//                    oos.writeObject(new Boolean(false));
//                    oos.flush();
                } else {
//                    oos.writeObject(new Integer(0)); // not exist, no need to delete
//                    oos.flush();
                    Transfer.receiveFile(sock, ois, newFile);
//                    oos.writeObject(new Boolean(true));
//                    oos.flush();

                    obj = ois.readObject(); // dir last modified
                    Long lastModified = (Long) obj;
                    newFile.setLastModified(lastModified);
                }
            }
        }
    }

    public static void deleteAllDirsAndFiles(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                deleteAllDirsAndFiles(new File(dir, children[i]));
            }
        }
        dir.delete();
    }
}
