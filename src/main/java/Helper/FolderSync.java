package Helper;

import Server.UpdateParams;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class FolderSync {

    public static final String DONE = "DONE";
    public static final String RENAME = "RENAME";
    public static final String DELETE = "DELETE";
    public static final String MODIFY = "MODIFY";

    // Process all files and directories under dir
    public static void sendUpdate(Socket sock, ObjectInputStream ois, ObjectOutputStream oos, File dir, int baseFolderLen, boolean syncStart , String name) throws Exception {
        if (syncStart) {
            System.out.println(name + ": Starting to send update");
        } else {
            //  System.out.println("check if exist file " + dir.getName() + " on the other side" );
            oos.writeObject(dir.getAbsolutePath().substring(baseFolderLen + 1));
            oos.flush();

            ois.readObject(); // other side get the dir name

            Boolean isDirectory = dir.isDirectory();
            oos.writeObject(new Boolean(isDirectory)); //Boolean isDirectory
            oos.flush();

            if (isDirectory) {  // isDirectory
                if (!(Boolean) ois.readObject()) { // dir NOT exist on the other side
                    //     System.out.println("dir NOT exist on the other side!");
                    oos.writeObject(new Boolean(true)); // send ok
                    oos.flush();
                }

            } else {          // isFile
                if (!(Boolean) ois.readObject()) { // File NOT exist on the other side
                    //  System.out.println("File NOT exist on the other side!");
                    oos.writeObject(new Boolean(true)); // ok
                    oos.flush();
                    System.out.println("sendUpdate-notExist ");
                    Transfer.sendFile(sock, oos, dir);

                } else {
                    oos.writeObject(new Long(dir.lastModified())); // send last modified
                    oos.flush();

                    if ((Boolean) ois.readObject()) { // send update
                        System.out.println("sendUpdate-fileModified " + dir.lastModified());
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
                sendUpdate(sock, ois, oos, new File(dir, children[i]), baseFolderLen, false, name);
            }
        }
    }

    public static UpdateParams getUpdate(Socket sock, ObjectInputStream ois, ObjectOutputStream oos, String action , String name) {
        try {

            System.out.println(name + ": getUpdate :  " + action + " start");
            UpdateParams params;

            if (action.equals(RENAME)) {
                oos.writeObject(new Boolean(true)); //ok
                oos.flush();
                params = getRenameUpdate( ois, oos, name);
            } else if (action.equals(DELETE)) {
                oos.writeObject(new Boolean(true)); //ok
                oos.flush();
                params = getDeleteUpdate( ois, oos, name);
            } else { // modify
                oos.writeObject(new Boolean(true)); //ok
                oos.flush();
                getModifyUpdate(sock, ois, oos,name);
                params = new UpdateParams();
                params.updateType = "MODIFY";
            }
            System.out.println(name + ": getUpdate: " + action + " End");
            return params;

        } catch (Exception e) {
            e.printStackTrace();
            return null;


        }
    }

    private static void getModifyUpdate(Socket sock, ObjectInputStream ois, ObjectOutputStream oos, String name) throws Exception {
        Boolean isDone = false;

        while (!isDone) {
            String path = (String) ois.readObject(); // dir name
            if (path.equals(DONE)) {
                System.out.println("all modify done");
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
                    System.out.println(name + ": modify-new folder: " + path );
                    newFile.mkdir();

                } else {
                    System.out.println(name + ": modify-new file: " + path );
                    Transfer.receiveFile(sock, ois, newFile);

                }
            } else if (!isDirectory) { // file already exist


                Long recvLastModified = (Long) ois.readObject(); // dir last modified on other side
                Long currLastModified = new Long(newFile.lastModified());

                if (recvLastModified > currLastModified) {
                    oos.writeObject(new Boolean(true)); // yes, give me update
                    oos.flush();
                    System.out.println(name + ": getUpdate-modified" );
                    Transfer.receiveFile(sock, ois, newFile);

                    newFile.setLastModified(recvLastModified);

                } else {
                    oos.writeObject(new Boolean(false)); // no, I'm up to date
                    oos.flush();
                }


            }
        }
    }

    private static UpdateParams getRenameUpdate(ObjectInputStream ois, ObjectOutputStream oos, String name) throws Exception {
        String oldName = (String) ois.readObject();
        File oldFile = new File(System.getProperty("user.dir") + "\\" + oldName);
        oos.writeObject(new Boolean(true)); //ok
        oos.flush();
        String newName = (String) ois.readObject();
        File newFile = new File(System.getProperty("user.dir") + "\\" + newName);
        oos.writeObject(new Boolean(true)); //ok
        oos.flush();


        UpdateParams params = new UpdateParams();
        params.updateType = RENAME;
        params.fileToRenameFrom = oldName;
        params.getFileToRenameTo = newName;


        if (oldFile.renameTo(newFile)) {
            System.out.println(name + ":Rename successful: from " +oldName + " to " + newName );

        } else {
            System.out.println(name + ":Rename failed: from " +oldName + " to " + newName );
        }

        return params;
    }

    private static UpdateParams getDeleteUpdate(ObjectInputStream ois, ObjectOutputStream oos, String name) throws Exception {
        boolean isDone = false;
        UpdateParams params = new UpdateParams();

        while (!isDone) {
            String fileName = (String) ois.readObject();
            if (fileName.equals(DONE)) {
                System.out.println(name + ":all deleting done");
                isDone = true;
                break;
            }


            File fileToDel = new File(System.getProperty("user.dir") + "\\" + fileName);
            oos.writeObject(new Boolean(true)); //ok
            oos.flush();



            if (fileToDel.isDirectory()) {
                try {
                    FileUtils.deleteDirectory(fileToDel);
                    params.updateType = DELETE;
                    params.filesToDelete.add(fileName);
                    System.out.println(name + ":Delete successful: " +fileName);

                } catch (IOException ex) {
                    System.out.println(name + ":Delete failed: " +fileName );
                    System.out.println(ex);
                }
            } else {
                if (fileToDel.delete()) {
                    params.updateType = DELETE;
                    params.filesToDelete.add(fileName);
                    System.out.println(name + ":Delete successful: " +fileName);
                } else {
                    System.out.println(name + ":Delete failed: " +fileName );
                }
            }
        }
        return params;
    }

}