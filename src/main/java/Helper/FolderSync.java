package Helper;

import java.io.*;
import java.net.Socket;
import java.util.Vector;

public class FolderSync {

    private static final String DONE = "DONE";

    public static void sync(Socket sock, String dirName, String fullDirName) throws Exception {
        ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
        oos.writeObject(new String(dirName));
        oos.flush();

        ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());

        System.out.print("Syncing..");

        // receive if this directory exists
        Boolean fExists = (Boolean) ois.readObject();

        File baseDir = new File(fullDirName); // skipping the base dir as it already should be set up on the server
        String[] children = baseDir.list();

        for (int i=0; i < children.length; i++) {
            visitAllDirsAndFiles(sock, ois, oos, fullDirName, new File(baseDir, children[i]));
        }
        Vector<String> vecDONE = new Vector<String>();
        vecDONE.add(DONE);
        oos.writeObject(vecDONE);
        oos.flush();

        if(fExists)
            getUpdate(sock, ois, oos, fullDirName);

        System.out.println("--------Sync completed!----------");
        System.out.println();
    }

    // Process all files and directories under dir
    public static void visitAllDirsAndFiles(Socket sock, ObjectInputStream ois, ObjectOutputStream oos, String fullDirName, File dir) throws Exception{
        Vector<String> vec = new Vector<String>();
        vec.add(dir.getName());
        vec.add(dir.getAbsolutePath().substring((dir.getAbsolutePath().indexOf(fullDirName) + fullDirName.length())));

        if(dir.isDirectory()) {
            oos.writeObject(vec);
            oos.flush();


            ois.readObject();
        } else {
            vec.add(new Long(dir.lastModified()).toString());
            oos.writeObject(vec);
            oos.flush();

            // receive SEND or RECEIVE
            Integer updateToServer = (Integer) ois.readObject(); //if true update server, else update from server

            if (updateToServer == 1) {  // send file to server
                Transfer.sendFile(sock, ois, oos, dir);

                ois.readObject(); // make sure server got the file

            } else if (updateToServer == 0) { // update file from server.
                dir.delete(); // first delete the current file

                oos.writeObject(new Boolean(true)); // send "Ready"
                oos.flush();

                Transfer.receiveFile(sock, ois, oos, dir);

                oos.writeObject(new Boolean(true)); // send back ok
                oos.flush();

                Long updateLastModified = (Long) ois.readObject(); // update the last modified date for this file from the server
                dir.setLastModified(updateLastModified);

            } // no need to check if update to server == 2 because we do nothing here
        }
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                visitAllDirsAndFiles(sock, ois, oos, fullDirName, new File(dir, children[i]));
            }
        }
    }

    private static void getUpdate(Socket sock, ObjectInputStream ois, ObjectOutputStream oos, String fullDirName) throws Exception {
        Boolean isDone = false;
        Boolean nAll = false;
        while(!isDone) {
            String path = (String) ois.readObject();

            if(path.equals(DONE)) {
                isDone = true;
                break;
            }

            oos.writeObject(new Boolean(true));
            oos.flush();

            File newFile = new File(fullDirName + path);

            Boolean isDirectory = (Boolean) ois.readObject();

            oos.writeObject(new Boolean(newFile.exists()));
            oos.flush();
            if (!newFile.exists()) {
                ois.readObject();
                String userInput = null;
                if (!nAll) {
                    if (isDirectory) {
                        System.out.println("CONFLICT! The folder exists on the server but not on this client.");
                        System.out.println("Would you like to delete the folder on the server (if not, the folder will be copied to this client)?");
                        System.out.println("No - for all folders. I will create a server copy on this client");
                    } else {
                        System.out.println("CONFLICT! The file exists on the server but not on this client.");
                        System.out.println("Would you like to delete the file on the server (if not, the file will be copied to this client)?");
                        System.out.println("No - for all files. I will create a server copy on this client");
                    }
                    System.out.println("Press: [y] for YES, [n] for NO, [a] for (NO for all files) ");
                    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                    try {
                        userInput = br.readLine();
                    } catch (IOException ioe) {
                        System.out.println("You have not typed a correct value, no action will be taken.");
                    }
                } else // if n to all then just set input to n!
                    userInput = "n";
                if (userInput.equalsIgnoreCase("a") || userInput.equalsIgnoreCase("'a'")) {
                    nAll = true;
                    userInput = "n";
                }
                if (userInput.equalsIgnoreCase("y") || userInput.equalsIgnoreCase("'y'")) {
                    if (isDirectory) {
                        oos.writeObject(new Boolean(true)); // reply with yes to delete the server's copy
                        oos.flush();
                    } else {
                        oos.writeObject(new Integer(1));
                        oos.flush();
                    }
                } else if (userInput.equalsIgnoreCase("n") || userInput.equalsIgnoreCase("'n'")) {
                    if (isDirectory) {
                        newFile.mkdir();
                        oos.writeObject(new Boolean(false));
                        oos.flush();
                    } else {
                        oos.writeObject(new Integer(0));
                        oos.flush();
                        Transfer.receiveFile(sock, ois, oos, newFile);

                        oos.writeObject(new Boolean(true));
                        oos.flush();

                        Long lastModified = (Long) ois.readObject();
                        newFile.setLastModified(lastModified);

                        oos.writeObject(new Boolean(true));
                        oos.flush();
                    }
                } else {
                    if (isDirectory) {
                        oos.writeObject(new Boolean(false));
                        oos.flush();
                    } else {
                        oos.writeObject(new Integer(2));
                        oos.flush();
                    }
                }
            }
        }
    }
}
