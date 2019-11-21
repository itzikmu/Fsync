package Helper;

import java.io.*;
import java.net.Socket;

public class Transfer {

    public static void sendFile(Socket sock, ObjectOutputStream oos, File dir) throws Exception {
        System.out.println("send " + dir.getName().toString()+ " start");
        byte[] buff = new byte[sock.getSendBufferSize()];

        int bytesRead = 0;

        InputStream in = new FileInputStream(dir);

        while((bytesRead = in.read(buff))>0) {
            oos.write(buff,0,bytesRead);
        }

        in.close();
        oos.flush(); // after sending a file you need to close the socket and reopen one.
        System.out.println("send " + dir.getName().toString()+ " end");
    }

    public static void receiveFile(Socket sock, ObjectInputStream ois, File dir) throws Exception {
        System.out.println("receive " + dir.getName().toString()+ " start");
        FileOutputStream wr = new FileOutputStream(dir);

        byte[] outBuffer = new byte[sock.getReceiveBufferSize()];

        int bytesReceived = 0;

        while((bytesReceived = ois.read(outBuffer))>0) {
            wr.write(outBuffer,0,bytesReceived);
        }

        wr.flush();
        wr.close();
        System.out.println("receive " + dir.getName().toString()+ " end");
    }

}
