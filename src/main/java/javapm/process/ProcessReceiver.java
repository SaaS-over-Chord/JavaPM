package javapm.process;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
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
 * @author Pratyush Kumar(pratyush)
 * @author Vasu Vardhan(vardhan)
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
        private String getfile() throws FileNotFoundException, IOException
        {
            byte[] mybytearray = new byte[1024*1024];
            InputStream is = clientSocket.getInputStream();
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            String fileName = in.readUTF();
            System.out.println("filename is :"+ fileName);
            FileOutputStream fos = new FileOutputStream(fileName);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
 
            long fileSize = in.readLong();
            /*int bytesRead = is.read(mybytearray, 0,(int)fileSize );
            System.out.println("filesize"+fileSize);
           bos.write(mybytearray, 0, bytesRead);
            System.out.println("bytesRead"+bytesRead);
            bos.close();
                    */
            int readableBytes=(1024*25);        
        int n=(int)(fileSize/readableBytes);
        int r=(int)(fileSize%readableBytes);
        byte b[];
        //waitForSocket(fileLength);
        for(int i=0;i<n;i++)
        {
            waitForSocket((1024*25));
            b= new byte[1024*25];
            in.read(b);
            fos.write(b);
        }
        waitForSocket(r);
        b=new byte[r];
        in.read(b);
        fos.write(b);        
            return fileName;

        }
        
        //added from neetesh
        public void waitForSocket(long size) throws IOException
            {
            try
            {      
                InputStream is = clientSocket.getInputStream();
                DataInputStream in = new DataInputStream(is);
                for(int attempts=0;!(in.available()>=size);attempts++,Thread.sleep(10))
                    if(attempts>1000)
                    {
                        System.out.println("error : timeout");
                        System.exit(1);
                    }
            }    
            catch(InterruptedException ex)
            {
                System.out.println("exception on waitforsocket" +ex);
            }            
        }
        

	public void run() {
		try {
                        //getfile("i.txt");
                        //getfile("o.txt");
			getfile();
                        getfile();
                        String objectFileName = getfile();
                        //ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
			DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
                        FileInputStream fis = new FileInputStream(objectFileName);
                        ObjectInputStream in = new ObjectInputStream(fis); 
			Object object = in.readObject();
                        object = (MigratableProcess)object;
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
