package Server;

import Helper.FolderSync;
import name.pachler.nio.file.Path;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.StringTokenizer;


// ClientHandler class
class ClientHandler implements Runnable {
    private String name;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    Socket socket;
    static volatile int TotalClientsCounter;
    static volatile UpdateParams updateParams;

    boolean thisClientUpdated = false;
    private  Thread readThread;

    private  volatile  Object readObject;

    // constructor
    public ClientHandler(Socket socket, String name, ObjectInputStream ois, ObjectOutputStream oos ,UpdateParams _updateParams) {
        this.ois = ois;
        this.oos = oos;
        this.name = name;
        this.socket = socket;
        this.updateParams = _updateParams;
    }

    @Override
    public void run() {

        try {


        syncClient(true);

        readObject = ois.readObject();
        FolderSync.getUpdate(socket, ois, oos, (String)readObject);
        readObject = null;

        runReadThread();


        while (true) {

            Thread.sleep(100);

            if (readObject != null) {  // this client handler got update request from his client
                System.out.println(name +": got update request from client");

                FolderSync.getUpdate(socket, ois, oos, (String) readObject);
                updateParams.updateType = (String) readObject;

                readObject = null;
                thisClientUpdated = true;
                System.out.println(name +": need to update " +(TotalClientsCounter-1) +" clients");
                updateParams.setNumOfClientsToUpdate(TotalClientsCounter-1);

                runReadThread();

            } else if (!thisClientUpdated && updateParams.numOfClientsNotUpdated > 0) { // this client handler need to update his client after server got updated
                syncClient(false);
                System.out.println(name + ": updated this client. still " + (updateParams.numOfClientsNotUpdated - 1) + " to update");
                thisClientUpdated = true;
                updateParams.decreaseNotUpdatedClientsCounter();

                runReadThread();

            }
            if((updateParams.numOfClientsNotUpdated==0) && thisClientUpdated )   // this is the last client updated , reset everything
            {
                System.out.println(name + ": updated all clients");
                updateParams.resetParams();
                thisClientUpdated = false;
            }

        }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }


    private void runReadThread() {

        readThread = new Thread() {
            public void run() {
                try {
                    System.out.println(name+":listening for client messages");
                    Object temp =  ois.readObject();
                    System.out.println(name+": got message from client " + temp.toString());
                    readObject = temp;
                } catch (java.net.SocketException e) {
                    System.out.println(name+": socket is closed " + e.getMessage());

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

                oos.writeObject(new String(FolderSync.MODIFY));
                oos.flush();
                if ( isFirstRun )
                {
                    ois.readObject();
                }
                else {
                    while (readObject == null) {
                        ;
                    }
                }

                readObject = null;

                FolderSync.sendUpdate(socket, ois, oos, baseDirFolder, Server.baseDir.length(), true);
                 readObject = null;
                done();



        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void deleteFile(Path pathToDelete) throws Exception {
        File fileToDelete = new File(pathToDelete.toString());
        System.out.println("deleteFile  start" + pathToDelete.toString());

        oos.writeObject(new String(FolderSync.DELETE));
        oos.flush();
        while(readObject == null)
        { ; }
        readObject=null;

        oos.writeObject(fileToDelete.toString());
        oos.flush();
        ois.readObject();
        System.out.println("deleteFile end");
    }


    private void done() throws Exception {
        oos.writeObject(new String(FolderSync.DONE));
        oos.flush();
        System.out.println("server sync finished ...");
    }
}