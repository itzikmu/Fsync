package Server;

import Helper.FolderSync;

import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;


// ClientHandler class
class ClientHandler implements Runnable {
    private String name;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    Socket socket;
    static volatile int TotalClients;
    static volatile UpdateParams updateParams;
    private boolean clientDone;

    boolean thisClientUpdated;
    private Thread readThread;

    private volatile Object readObject;

    // constructor
    public ClientHandler(Socket socket, String name, ObjectInputStream ois, ObjectOutputStream oos, UpdateParams _updateParams) {
        this.ois = ois;
        this.oos = oos;
        this.name = name;
        this.socket = socket;
        this.updateParams = _updateParams;
        this.clientDone = false;
        this.thisClientUpdated = false;
    }

    @Override
    public void run() {

        try {


            syncClient(true);

            readObject = ois.readObject();
            UpdateParams updateFromClient = FolderSync.getUpdate(socket, ois, oos, (String) readObject);
            updateParams.GetParamsUpdate(updateFromClient);
            System.out.println(name + ": need to update " + (TotalClients - 1) + " clients");
            updateParams.setNumOfClientsToUpdate(TotalClients - 1);
            readObject = null;
            thisClientUpdated = true;

            runReadThread();


            while (!clientDone) {

                Thread.sleep(100);

                if (readObject != null) {  // this client handler got update request from his client
                    System.out.println(name + ": got update request from client");

                    while (updateParams.numOfClientsNotUpdated > 0) {
                        ;
                    }
                    updateFromClient = FolderSync.getUpdate(socket, ois, oos, (String) readObject);
                    updateParams.GetParamsUpdate(updateFromClient);

                    readObject = null;
                    thisClientUpdated = true;
                    System.out.println(name + ": need to update " + (TotalClients - 1) + " clients");
                    updateParams.setNumOfClientsToUpdate(TotalClients - 1);

                    runReadThread();

                } else if (!thisClientUpdated && updateParams.numOfClientsNotUpdated > 0) { // this client handler need to update his client after server got updated
                    if (updateParams.updateType.equals("DELETE")) {
                        sendDeleteFile(updateParams.fileToDelete);
                    } else if (updateParams.updateType.equals("RENAME")) {
                        sendRenameFile(updateParams.fileToRenameFrom, updateParams.getFileToRenameTo);
                    } else {
                        syncClient(false);
                    }

                    System.out.println(name + ": updated this client. still " + (updateParams.numOfClientsNotUpdated - 1) + " to update");
                    thisClientUpdated = true;
                    updateParams.decreaseNotUpdatedClientsCounter();

                    runReadThread();

                }

                if ((updateParams.numOfClientsNotUpdated == 0) && thisClientUpdated)   // this is the last client updated , reset everything
                {
                    if (!updateParams.updateType.equals("")) {
                        System.out.println(name + ": updated all clients");
                        updateParams.resetParams();
                    }

                    thisClientUpdated = false;
                }

            }
        } catch (java.net.SocketException e) {
            e.printStackTrace();
            closeHandler();

        } catch (Exception e) {

            e.printStackTrace();
            System.out.println(name + "socket is dead");
        }
    }

    private void closeHandler(){
        clientDone = true;
        TotalClients--;
    }

    private void runReadThread() throws java.net.SocketException{

        readThread = new Thread() {
            public void run() {
                try {
                    System.out.println(name + ":listening for client messages");
                    Object temp = ois.readObject();
                    System.out.println(name + ": got message from client " + temp.toString());
                    readObject = temp;
                } catch (java.net.SocketException e) {
                   closeHandler();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        readThread.start();
    }


    private void syncClient(boolean isFirstRun) {
        try {

            File baseDirFolder = new File(Server.baseDir);

            oos.writeObject(FolderSync.MODIFY);
            oos.flush();
            if (isFirstRun) {
                ois.readObject();
            } else {
                while (readObject == null) {
                    ;
                }
            }

            readObject = null;

            FolderSync.sendUpdate(socket, ois, oos, baseDirFolder, Server.baseDir.length(), true);

            oos.writeObject(FolderSync.DONE);
            oos.flush();
            System.out.println("server sync finished ...");

            readObject = null;


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendDeleteFile(String pathToDelete) throws Exception {

        System.out.println("deleteFile  start" + pathToDelete);

        oos.writeObject(FolderSync.DELETE);
        oos.flush();
        while (readObject == null) {
            ;
        }
        readObject = null;

        oos.writeObject(pathToDelete);
        oos.flush();
        ois.readObject();
        System.out.println("deleteFile end");
    }

    private void sendRenameFile(String pathRenameFrom, String pathRenameTo) throws Exception {

        System.out.println("renameFile from" + pathRenameFrom + " to " + pathRenameTo);

        oos.writeObject(FolderSync.RENAME);
        oos.flush();
        while (readObject == null) {
            ;
        }
        readObject = null;

        oos.writeObject(pathRenameFrom);
        oos.flush();
        ois.readObject();

        oos.writeObject(pathRenameTo);
        oos.flush();
        ois.readObject();
        System.out.println("fileRename from server end");
    }


}