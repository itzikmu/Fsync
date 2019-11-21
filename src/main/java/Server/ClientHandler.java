package Server;

import Helper.FolderSync;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import java.util.StringTokenizer;


// ClientHandler class
class ClientHandler implements Runnable {
    Scanner scn = new Scanner(System.in);
    private String name;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    Socket socket;
    boolean isloggedin;
    static int TotalClientsCounter;
    static int numOfClientsNotUpdated = 0;
    boolean thisClientUpdated = true;
    private  Thread readThread;
    private  Thread getUpdateThread;
    private  Object readObject;

    // constructor
    public ClientHandler(Socket socket, String name, ObjectInputStream ois, ObjectOutputStream oos) {
        this.ois = ois;
        this.oos = oos;
        this.name = name;
        this.socket = socket;
        this.isloggedin = true;
    }

    @Override
    public void run() {

        syncClient(true);
        FolderSync.getUpdate(socket, ois, oos, "MODIFY");
        runReadThread();


        while (true) {
            try {
                if(readThread!= null) {


                    FolderSync.getUpdate(socket, ois, oos, (String)readObject);
                    readObject = null;
                    runReadThread();
                }

                else if (!thisClientUpdated && numOfClientsNotUpdated > 0) {
                        syncClient(false);
                        thisClientUpdated = true;
                        numOfClientsNotUpdated--;


                    runReadThread();
                }

            } catch (Exception e) {

                e.printStackTrace();
            }
        }


    }
    private void runReadThread() {

        readThread = new Thread() {
            public void run() {
                try {
                    System.out.println(name+": ReadThread:listening for client messages");
                    readObject = ois.readObject();
                    System.out.println(name+":ReadThread:got message from client " + readObject.toString());
                } catch (java.net.SocketException e) {
                    System.out.println(name+":socket is closed " + e.getMessage());

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        readThread.start();
    }





    private void syncClient(boolean firstSync) {
        try {


                File baseDirFolder = new File(Server.baseDir);

                oos.writeObject(new String(FolderSync.MODIFY));
                oos.flush();
                if(!firstSync) {
                    while (readObject == null) {
                        ;
                    }
                }
                else{
                    ois.readObject();
                }

                FolderSync.sendUpdate(socket, ois, oos, baseDirFolder, Server.baseDir.length(), true);

                done();



        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void done() throws Exception {
        oos.writeObject(new String(FolderSync.DONE));
        oos.flush();
        System.out.println("server sync finished ...");
    }
}