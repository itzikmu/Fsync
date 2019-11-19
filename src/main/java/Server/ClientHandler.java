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

        syncClient();
        FolderSync.getUpdate(socket, ois, oos);

        while (true) {
            try {
                // NOW getting 'Im ALlive' TODO: 19/11/2019 recive client logout
//                String action = (String) ois.readObject();
//
//                System.out.println("action: " + action);
//
//                oos.writeObject(new Boolean(true)); //ok
//                oos.flush();

//                if (action.equals("logout")) {
//                    this.isloggedin = false;
//                    this.socket.close();
//                    break;
//                }

                System.out.println("Waiting for Client update...");
                FolderSync.getUpdate(socket, ois, oos);

                // search for the recipient in the connected devices list.
                // ar is the vector storing client of active users
//                for (ClientHandler mc : Server.ar) {
//                    // if the recipient is found, send him update
//                    if (mc.isloggedin == true) {
//                        System.out.println("Send update to " + this.name);
//                        syncClient();
//                    }
//                }

            } catch (Exception e) {

                e.printStackTrace();
            }
        }

//        try {
//            // closing resources
//            this.ois.close();
//            this.oos.close();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private void syncClient() {
        try {
            File baseDirFolder = new File(Server.baseDir);

            oos.writeObject(new String(FolderSync.MODIFY));
            oos.flush();
            ois.readObject();

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