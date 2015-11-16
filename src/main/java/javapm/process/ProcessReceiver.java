package javapm.process;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * The socket receiver of socket server.
 * Each receiver is a single thread that communicate with
 * one client. It implements the <code>run()</code> from
 * the <code>Runnable</code>interface.
 *
 * @author Jian Fang(jianf)
 * @author Fangyu Gao(fangyug)
 * @see javapm.process.ProcessServer
 */
public class ProcessReceiver implements Runnable{
	/**
	 * Socket communication with client.
	 */
	Socket clientSocket;

	/**
	 * Constructor with an already exist socket as input.
	 */
	ProcessReceiver(Socket clientSocket) {
		this.clientSocket = clientSocket;
	}

    /**
     * The implementation of <code>Runnable</code> interface.
     * After connected and received process, the receiver
     * determine which class the process is, then send a
     * signal to the client to tell if the migration succeed.
     */
        private void getfile(String fileName) throws FileNotFoundException, IOException
        {
            byte[] mybytearray = new byte[1024*1024];
            InputStream is = clientSocket.getInputStream();
            FileOutputStream fos = new FileOutputStream(fileName);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            int fileSize = in.readInt();
            int bytesRead = is.read(mybytearray, 0,fileSize );
            System.out.println("filesize"+fileSize);
            bos.write(mybytearray, 0, bytesRead);
            System.out.println("bytesRead"+bytesRead);
            bos.close();

        }
	public void run() {
		try {
                        getfile("i.txt");
                        getfile("o.txt");
			ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
			DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());

			Object object = in.readObject();

			MigratableProcess process = null;
            if(object instanceof MigratableProcess){
            	process = (MigratableProcess)object;
            	process.migrated();
            	out.writeBoolean(true);
	            ProcessManager.getInstance().startProcess(process);
            }
            else {
            	out.writeBoolean(false);
            }
            in.close();
            out.close();
            clientSocket.close();
		}
		catch (IOException e) {
			System.out.println("processing client request error"+e);
        } catch (ClassNotFoundException e) {
        	System.out.println("client sent unrecognized object"+e);
        }
	}
}
