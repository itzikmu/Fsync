package Server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

// Server class
public class Server {

    private static Socket sock;
    protected static String baseDir;
//    private static int clientFilesCount = 0;

    static int i = 0; // counter for clients

    public static void main(String[] args) throws Exception {
        startServer();
    }


    public static void startServer() throws Exception {
        System.out.println("Starting File Sync Server!");

        ServerSocket servsock = new ServerSocket(1234);

        baseDir = "C:\\Temp\\ServerFolder";

        File baseDirFolder = new File(baseDir);

        if (!baseDirFolder.exists())
            baseDirFolder.mkdir();

        System.setProperty("user.dir", baseDir);
         UpdateParams updateParams = new UpdateParams();
        // running infinite loop for getting client request
        while (true) {
            // Accept the incoming request
            sock = servsock.accept();
            System.out.println("New client connected! IP: " + sock.getInetAddress().toString());

            // obtain input and output streams
            ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
            ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());

            // Create a new handler object for handling this request.
            System.out.println("Creating a new handler for this client...");
            ClientHandler mtch = new ClientHandler(sock, "client " + (i+1), ois, oos, updateParams);

            // Create a new Thread with this object.
            Thread t = new Thread(mtch);

            System.out.println("Adding this client to active client list");


            // start the thread.
            Thread.sleep(2000);
            t.start();
            ClientHandler.TotalClients++;

            // increment i for new client.
            // i is used for naming only, and can be replaced by any naming scheme
            i++;
        }


    }
}