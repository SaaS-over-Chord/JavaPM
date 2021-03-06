/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javapm.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
//import java.util.logging.Logger;

/**
 *
 * @author vardhan
 * 
 * @author pratyush
 */
public class ExitListener implements Runnable{
    ServerSocket serverSock;
    Socket clientSock;
    
    final int PORT = 15444;
       public ExitListener() throws IOException {
               serverSock= new ServerSocket(PORT);
        }

    @Override
    public void run() {
        try {
            System.out.println("runing");
            clientSock=serverSock.accept();
            BufferedReader br = new BufferedReader( new InputStreamReader(clientSock.getInputStream() ));
            String line = br.readLine();
            System.out.println("accepted");
            if(line.equals("-1"))
            {
                System.out.print("end");
            }
            
            ProcessManager pm = ProcessManager.getInstance();
        ConcurrentLinkedQueue<javapm.process.MigratableProcess> processes = pm.processes;
        Iterator<javapm.process.MigratableProcess> it = processes.iterator();
        
        //notify client that this server is exiting
        Socket sock = new Socket(pm.IP, 15442);
        PrintStream sockOut = new PrintStream(sock.getOutputStream());
        sockOut.println("ending JavaPM instance");
        sockOut.println("try node 172.31.134.34");
        
        //before migrating rell how many nodes are there
        sockOut.println(processes.size());
        
        //finally migrate whatever process left to the client
        while (it.hasNext()) {
            javapm.process.MigratableProcess process = it.next();
            long id=process.getId();
            pm.processCommand("mg "+id+" "+pm.IP);
                
        }
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }
    
    
}
