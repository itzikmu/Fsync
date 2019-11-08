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
                        System.out.println("Everything is UP TO DATE!");
                    }
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