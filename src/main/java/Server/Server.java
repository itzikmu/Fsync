package Server;

import Helper.FolderSync;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

// Server class
public class Server {
    private static Socket sock;
    private static ObjectOutputStream oos;
    private static ObjectInputStream ois;

    private static String baseDir;
    private static int clientFilesCount = 0;

    static Vector<ClientHandler> ar = new Vector<>();

    public static void main(String[] args) throws Exception {
        startServer();
    }


    public static void startServer() throws Exception {
        System.out.println("Starting File Sync Server!");

        ServerSocket servsock = new ServerSocket(1234);

        baseDir = "C:\\Temp\\ServerFolder";
        FolderSync.serverBaseDir = baseDir;

        File baseDirFolder = new File(baseDir);

        if (!baseDirFolder.exists())
            baseDirFolder.mkdir();

        System.setProperty("user.dir", baseDir);

        sock = servsock.accept();
        System.out.println("New client connected! IP: " + sock.getInetAddress().toString() + " Directory: " + baseDir);

        ois = new ObjectInputStream(sock.getInputStream());
        oos = new ObjectOutputStream(sock.getOutputStream());

        Boolean isClientDone = false;

        syncClient();

//        int serverFilesCount = FolderSync.fileCount(baseDirFolder);
//        System.out.println(baseDir + " has " + serverFilesCount + " files");

        while (true) {
            System.out.println("Waiting for Client update");
            FolderSync.getUpdate(sock, ois, oos);
        }

//            while (!isClientDone) {
//                syncClient();
//            }
//            oos.close();
//            ois.close();
//            sock.close();
//        System.out.println("Client disconnected.");
    }

    private static void syncClient() throws Exception {
        File baseDirFolder = new File(baseDir);

        oos.writeObject(new String(FolderSync.MODIFY));
        oos.flush();
        ois.readObject();

        FolderSync.sendUpdate(sock, ois, oos, baseDirFolder, baseDir.length());

        done();
    }

    private static void done() throws Exception {
        oos.writeObject(new String(FolderSync.DONE));
        oos.flush();
        System.out.println("server sync finished ...");
    }

    // Vector to store active clients
    // static Vector<ClientHandler> ar = new Vector<>();

    // // counter for clients
    // static int i = 0;

    // public static void main(String[] args) throws IOException
    // {
    //     // server is listening on port 1234
    //     ServerSocket ss = new ServerSocket(1234);

    //     Socket socket;

    //     // running infinite loop for getting
    //     // client request
    //     while (true)
    //     {
    //         // Accept the incoming request
    //         socket = ss.accept();

    //         System.out.println("New client request received : " + socket);

    //         // obtain input and output streams
    //         DataInputStream dis = new DataInputStream(socket.getInputStream());
    //         DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

    //         System.out.println("Creating a new handler for this client...");

    //         // Create a new handler object for handling this request.
    //         ClientHandler mtch = new ClientHandler(socket,"client " + i, dis, dos);

    //         // Create a new Thread with this object.
    //         Thread t = new Thread(mtch);

    //         System.out.println("Adding this client to active client list");

    //         // add this client to active clients list
    //         ar.add(mtch);

    //         // start the thread.
    //         t.start();

    //         // increment i for new client.
    //         // i is used for naming only, and can be replaced
    //         // by any naming scheme
    //         i++;

    //     }
    // }
}