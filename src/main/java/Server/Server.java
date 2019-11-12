package Server;

import Helper.FolderSync;
import Helper.Transfer;

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
    private static Boolean baseDirExists;
    static Vector<ClientHandler> ar = new Vector<>();

    public static void main(String[] args) throws Exception {

        startServer();
    }


    public static void startServer() throws Exception {
        System.out.println("Starting File Sync Server!");

        ServerSocket servsock = new ServerSocket(1234);

        sock = servsock.accept();

        ois = new ObjectInputStream(sock.getInputStream());

        baseDir = "C:\\Temp\\ServerFolder";
        FolderSync.serverBaseDir = baseDir;

        oos = new ObjectOutputStream(sock.getOutputStream());

        System.out.println("New client connected! IP: " + sock.getInetAddress().toString() + " Directory: " + baseDir);

        File fBaseDir = new File(baseDir);
        baseDirExists = fBaseDir.exists();

        if (!baseDirExists)
            fBaseDir.mkdir();

        Boolean isClientDone = false;

        syncClient();

        while (true) {
            System.out.println("Waiting for Client update");
            FolderSync.getUpdate(sock, ois, oos, baseDir);
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
        FolderSync.visitAllDirsAndFiles(sock, ois, oos, baseDirFolder, false);

        oos.writeObject(new String(FolderSync.DONE));
        oos.flush();
        System.out.println("sync finished ...");
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