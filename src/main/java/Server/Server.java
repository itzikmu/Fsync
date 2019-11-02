package Server;

import Helper.FolderSync;
import Helper.Transfer;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

// Server class
public class Server {
    private static final String DONE = "DONE";
    private static Socket sock;
    private static ObjectOutputStream oos;
    private static ObjectInputStream ois;
    private static ServerSocket servsock;
    private static String baseDir;
    static Vector<ClientHandler> ar = new Vector<>();

    public static void main(String[] args) throws Exception {

        startServer();
    }


    public static void startServer() throws Exception {
        System.out.println("Starting File Sync Server!");

        servsock = new ServerSocket(1234);


        while (true) {
            sock = servsock.accept();

            ois = new ObjectInputStream(sock.getInputStream());

//            baseDir = "serverFolder";
            baseDir = (String) ois.readObject();

            oos = new ObjectOutputStream(sock.getOutputStream());

            System.out.println("New client connected! IP: " + sock.getInetAddress().toString() + " Directory: " + baseDir);

            File fBaseDir = new File(baseDir);
            Boolean baseDirExists = fBaseDir.exists();

            if (!baseDirExists)
                fBaseDir.mkdir();

            oos.writeObject(new Boolean(baseDirExists));
            oos.flush();

            Boolean isClientDone = false;

            while (!isClientDone) {
                Vector<String> vec = (Vector<String>) ois.readObject();


                if (vec.elementAt(0).equals(DONE)) {  // check if we are done
                    isClientDone = true; // if so break out
                    break;
                }

                if (vec.size() == 2) { // if the size is 2 then this is a directory
                    File newDir = new File(baseDir, vec.elementAt(1));
                    if (!newDir.exists())
                        newDir.mkdir();

                    oos.writeObject(new Boolean(true)); // tell client that we are ready
                    oos.flush();
                } else {
                    File newFile = new File(baseDir, vec.elementAt(1));
                    Integer updateFromClient = 2; // default = do nothing

                    Long lastModified = new Long(vec.elementAt(2));
                    if (!newFile.exists() || (newFile.lastModified() <= lastModified))
                        updateFromClient = 1;
                    else
                        updateFromClient = 0;

                    if (newFile.exists() && newFile.lastModified() == lastModified)
                        updateFromClient = 2;

                    if (updateFromClient == 1) { // If true receive file from client
                        newFile.delete();

                        oos.writeObject(new Integer(updateFromClient));
                        oos.flush();

                        Transfer.receiveFile(sock, ois, oos, newFile);

                        newFile.setLastModified(lastModified);

                        oos.writeObject(new Boolean(true));
                    } else if (updateFromClient == 0) { // if false send file to client
                        oos.writeObject(new Integer(updateFromClient));
                        oos.flush();

                        ois.readObject();

                        Transfer.sendFile(sock, ois, oos, newFile);

                        ois.readObject();

                        oos.writeObject(new Long(newFile.lastModified()));
                        oos.flush();
                    } else { //updateFromClient == 2 // do nothing
                        oos.writeObject(new Integer(updateFromClient));
                        oos.flush();
                    }
                }


                File baseDirFile = new File(baseDir);
                if (baseDirExists)
                    FolderSync.visitAllDirsAndFiles(sock, ois, oos, fBaseDir.getAbsolutePath(), baseDirFile);

                oos.writeObject(new String(DONE));
                oos.flush();
                System.out.print("sync finished ...");

            }
            oos.close();
            ois.close();
            sock.close();
            System.out.println("Client disconnected.");
        }
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