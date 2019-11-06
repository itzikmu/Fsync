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
            System.out.println(": sending true 45");
            oos.writeObject(new Boolean(isDirectory)); //Boolean isDirectory
            oos.flush();

            if (isDirectory) {
                if (!(Boolean) ois.readObject()) { // dir NOT exist on the other side
                    System.out.println(": sending true 51");
                    oos.writeObject(new Boolean(true));
                    oos.flush();

                    Boolean delete = (Boolean) ois.readObject();

                    if (delete) {
                        FolderSync.deleteAllDirsAndFiles(dir);
                        return;
                    } //ELSE DO NOTHING
                }
            } else {
                if (!(Boolean) ois.readObject()) { // File NOT exist on the other side
                    System.out.println(": sending true 64");
                    oos.writeObject(new Boolean(true));
                    oos.flush();

                    Integer delete = (Integer) ois.readObject();

                    if (delete == 1) {
                        dir.delete();
                        return;
                    } else if (delete == 0) {
                        Transfer.sendFile(sock, ois, oos, dir);

                        ois.readObject();

                        oos.writeObject(new Long(dir.lastModified()));
                        oos.flush();

                    } // ELSE DO NOTHING!
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

    public static void getUpdate(Socket sock, ObjectInputStream ois, ObjectOutputStream oos, String fullDirName, String name) throws Exception {
        Boolean isDone = false;
        while (!isDone) {
            Object obj = ois.readObject(); // dir name
            System.out.println(name + ": getupdate()" + obj.toString());
            String path = (String) obj;

            if (path.equals(DONE)) {
                isDone = true;
                System.out.println("getUpdate done");
                break;
            }
            System.out.println(name + ": sending true 106");
            oos.writeObject(new Boolean(true));
            oos.flush();

            File newFile = new File(fullDirName + path);

            Boolean isDirectory = (Boolean) ois.readObject(); //Boolean isDirectory

            System.out.println(name + ": sending true 113");
            oos.writeObject(new Boolean(newFile.exists()));
            oos.flush();
            if (!newFile.exists()) {
                ois.readObject();
                if (isDirectory) {
                    newFile.mkdir();
                    oos.writeObject(new Boolean(false));
                    oos.flush();
                } else {
                    oos.writeObject(new Integer(0));
                    oos.flush();
                    Transfer.receiveFile(sock, ois, oos, newFile);
                    System.out.println(name + ": sending true 127");
                    oos.writeObject(new Boolean(true));
                    oos.flush();

                    Long lastModified = (Long) ois.readObject();
                    newFile.setLastModified(lastModified);
                    System.out.println(name + ": sending true 132");
                    oos.writeObject(new Boolean(true));
                    oos.flush();
                }
            }
            System.out.println(name + ": File exist 140");
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
