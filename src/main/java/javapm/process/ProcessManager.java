package javapm.process; 

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
//import java.util.logging.Logger;
//import javapm.process.ExitListener;

/**
 * The manager of all migratable processes
 * This manager contains two main parts: the first is the
 * <code>ProcessServer</code>, which is designed to receive the
 * migration request from other <code>ProcessManager</code>; The
 * second part is a console, which is designed to offer a controller
 * interface. Other functions in this class contains generator of
 * process ID, callback function of process exit, etc,.
 *
 * @author Jian Fang(jianf)
 * @author Fangyu Gao(fangyug)
 * @see javapm.process.MigratableProcess
 * @see javapm.process.ProcessServer
 */
public class ProcessManager {
    /**
     * Log handler
     *
     * @see <a href="http://apache.org/log4j/2.x/">Log4J</a>
     */
    private static Logger LOG = LogManager.getLogger(ProcessManager.class);
    
    /**
     * The IP constructed to help with the process of migration
     */
    public static String IP;
    
    /**
     * The singleton instance of <code>ProcessManager</code>
     */
    private static ProcessManager singleton;

    /**
     * The counter for process ID. By using the <code>AtomicLong</code>,
     * We assure the ID generation is thread-safe.
     *
     * @see java.util.concurrent.atomic.AtomicLong
     */
    private AtomicLong idCounter;

    /**
     * The linked queue of current processes.
     * By using the <code>ConcurrentLinkedQueue</code> we assure the
     * queue operations(add, remove...) are thread-safe.
     *
     * @see java.util.concurrent.ConcurrentLinkedQueue
     */
    public ConcurrentLinkedQueue<MigratableProcess> processes;

    /**
     * The set of all migratable classes inherited from
     * <code>MigratableProcess</code>
     *
     * @see javapm.process.MigratableProcess
     */
    Set<Class<? extends MigratableProcess>> processClasses;

    /**
     * Constructor of <code>ProcessManager</code>
     * The constructor is invisible since we need to keep
     * the <code>ProcessManager</code> is single instance.
     * In the constructor we use <code>Reflections</code> library
     * to get all classes inherited from <code>MigratableProcess</code>
     *
     * @see <a href="https://code.google.com/p/reflections/">Reflections Library</a>
     */
    private ProcessManager() {
        idCounter = new AtomicLong(0);
        processes = new ConcurrentLinkedQueue<MigratableProcess>();
        Reflections reflections = new Reflections("javapm.process");
        processClasses = reflections.getSubTypesOf(MigratableProcess.class);
    }

    /**
     * Start the <code>ProcessServer</code> in a new <code>Thread</code>
     *
     * @see javapm.process.ProcessServer
     * @see java.lang.Thread#start()
     */
    public void startServer() {
        Thread serverThread = new Thread(new ProcessServer());
        serverThread.start();
    }

    /**
     * Start the interactive console of <code>ProcessManager</code>.
     * Read user command from <code>System.in</code>, then process
     * the command.
     *
     * @see javapm.process.ProcessManager#processCommand(String)
     */
    public void startConsole() {
        System.out.println("Welcome aboard! Type 'help' for more information");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String line = null;
            try {
                line = br.readLine();
            } catch (IOException e) {
                LOG.fatal("read command error", e);
                System.exit(-1);
            }
            processCommand(line);
        }
    }
    
    public void startExitListener()
    {
        Thread exitListener;
        try {
            exitListener = new Thread(new ExitListener());
            exitListener.start();
        } catch (IOException ex) {
            LOG.fatal("Exit Listener creation failed",ex);
        }
        
    }

    /**
     * Generate a process ID by using <code>getAndIncrement</code>.
     * This function is thread-safe
     *
     * @return the process ID
     * @see java.util.concurrent.atomic.AtomicLong#getAndIncrement()
     */
    public long generateID() {
        return idCounter.getAndIncrement();
    }

    /**
     * Callback for process exit, remove process from <code>processes</code>
     * queue. The <code>processes.remove(Object)</code> is thread-safe.
     *
     * @param process the process instance
     * @see java.util.concurrent.ConcurrentLinkedQueue#remove(Object)
     */
    public void finishProcess(MigratableProcess process) {
        processes.remove(process);
    }

    /**
     * Start a process by using <code>processName</code> and
     * <code>args</code>. We lookup the <code>processName</code>
     * in <code>processClasses</code>, then get the
     * <code>Class</code> object. Next, we use Java's reflection
     * to create a new process instance.
     * Finally we add the process object to our queue.
     *
     * @param processName the process name
     * @param args        the process arguments
     * @return if success return <code>true</code>
     *         else return <code>false</code>
     * @throws IllegalAccessException can't access process constructor
     * @throws InstantiationException can't find default process constructor
     * @throws InvocationTargetException 
     * @throws IllegalArgumentException 
     * @see Class#newInstance()
     * @see javapm.process.MigratableProcess#initProcess(String[])
     * @see javapm.process.ProcessManager#startProcess(MigratableProcess)
     */
    public boolean startProcess(String processName, String[] args)
            throws IllegalAccessException, InstantiationException, IllegalArgumentException, InvocationTargetException {
        Iterator<Class<? extends MigratableProcess>> it = processClasses.iterator();
        while (it.hasNext()) {
            Class<? extends MigratableProcess> process = it.next();
            System.out.println(process);
            if (process.getSimpleName().equals(processName)) {
                Constructor<?>[] ctors = process.getDeclaredConstructors();
                Constructor<?> ctor = null;
            	for (int i = 0; i < ctors.length; i++) {
            	    ctor = ctors[i];
            	    if (ctor.getGenericParameterTypes().length != 0)
            		break;
            	}
            	MigratableProcess processInstance = (MigratableProcess) ctor.newInstance((Object) args);
                startProcess(processInstance);
                
                return true;
            }
        }
        return false;
    }

    /**
     * Start a process by using <code>MigratableProcess</code> object.
     * Add the process object to the linked queue. This function is
     * thread-safe.
     *
     * @param process
     * @see java.util.concurrent.ConcurrentLinkedQueue#offer(Object)
     */
    public void startProcess(MigratableProcess process) {
        Thread thread = new Thread(process);
        thread.start();
        processes.offer(process);
    }

    /**
     * Get the singleton <code>ProcessManager</code> instance.
     * This function is thread-safe.
     *
     * @return the <code>ProcessManager</code> instance
     */
    synchronized public static ProcessManager getInstance() {
        if (singleton == null) {
            singleton = new ProcessManager();
        }
        return singleton;
    }

    /**
     * Lookup a process in <code>processes</code> by process ID
     *
     * @param id process ID
     * @return the <code>MigratableProcess</code> object if found,
     *         else return null
     */
    private MigratableProcess getProcess(long id) {
        Iterator<MigratableProcess> it = processes.iterator();
        while (it.hasNext()) {
            MigratableProcess process = it.next();
            if (process.getId() == id)
                return process;
        }
        return null;
    }

    /**
     * Process command line.
     * Split the command line with blank. The first block is
     * the command, others are command arguments. We use
     * <code>ProcessManagerCommand</code> enum to switch between
     * commands.
     *
     * @param commandLine the user input command line
     * @see java.lang.String#split(String)
     * @see javapm.process.ProcessManagerCommand
     */
    public void processCommand(String commandLine) {
        if (commandLine == null || commandLine.length() == 0)
            return;
        String[] args = commandLine.split("\\s+");
        if (args.length == 0)
            return;
        switch (ProcessManagerCommand.getInstance(args[0].toLowerCase())) {
            case HELP:
                processHelpCommand();
                break;
            case QUIT:
                processQuitCommand();
                break;
            case LS:
                processLsCommand();
                break;
            case PS:
                processPsCommand();
                break;
            case RUN:
                processRunCommand(args);
                break;
            case MG:
                processMigrateCommand(args);
                break;
            case UNKNOWN:
            default:
                System.out.println("unknown command '" + args[0] + "'");
                break;
        }
    }

    /**
     * List all classes inherited from <code>MigratableProcess</code>
     */
    private void processLsCommand() {
        if (processClasses.isEmpty()) {
            System.out.println("No migratable program ");
        } else {
            System.out.println("All migratable programs:");
            System.out.println("-------------------------------");
            Iterator<Class<? extends MigratableProcess>> it = processClasses.iterator();
            while (it.hasNext()) {
                Class<? extends MigratableProcess> processClass = it.next();
                System.out.println(processClass.getSimpleName());
            }
        }
    }
    
    
    /**
     * List all running processes
     */
    private void processPsCommand() {
        if (processes.isEmpty()) {
            System.out.println("No running process");
        } else {
            Iterator<MigratableProcess> it = processes.iterator();
            while (it.hasNext()) {
                MigratableProcess process = it.next();
                System.out.println(process.toString());
                System.out.println("with process id"+process.id);
            }
        }
    }

    /**
     * Run a process by using process name and arguments.
     *
     * @param args command arguments
     * @see javapm.process.ProcessManager#startProcess(String, String[])
     */
    private void processRunCommand(String[] args) {
        if (args.length <= 1) {
            System.out.println("usage: run PROCESS_NAME ARG...");
        } else {
            String processName = args[1];
            String[] processArgs = new String[args.length - 2];
            for (int i = 2; i < args.length; i++) {
                processArgs[i - 2] = args[i];
            }
            boolean contains = false;
            try {
                contains = startProcess(processName, processArgs);
            } catch (Exception e) {
                LOG.error("run command " + processName + " error", e);
                return;
            }
            if (!contains) {
                System.out.println("No such program: '" + processName + "'");
            }
        }
    }
    
    /**
     * Quit the program
     */
    private void processQuitCommand() {
        System.out.println("Bye!");
        System.exit(0);
    }

    /**
     * Migrate the specific process by using process ID
     * First lookup the process by ID, then we connect 
     * the specific host by <code>Socket</code>.
     * If connected, suspend the process and call
     * <code>statMigrating()</code>.
     *
     * @param args command arguments
     * @see javapm.process.ProcessManager#getProcess(long)
     * @see javapm.process.ProcessManager#startMigrating(Socket, MigratableProcess, String)
     */
    private void processMigrateCommand(String[] args) {
        if (args.length <= 2) {
            System.out.println("usage: mg PROCESS_ID HOSTNAME");
        } else {
            long id = Long.parseLong(args[1]);
            String hostName = args[2];
            MigratableProcess process = getProcess(id);
            if (process == null) {
                System.out.println("No such process: " + args[1]);
                return;
            }
            Socket socket = null;
            try {
            	socket = new Socket(hostName, ProcessServer.PORT);
                
	            try {
	                process.suspend();
	            } catch (InterruptedException e) {
	                LOG.error(process.getClass().getSimpleName() +
	                        "[" + id + "] suspend error", e);
	                return;
	            }
	            startMigrating(socket, process, hostName);

	            socket.close();
            }
            catch (IOException e) {
            	System.out.println("Connect " + hostName + " failed: " +
                        e.getMessage());
            	return;
            }
        }

    }

    /**
     * Start migrating the process to specific host.
     * First we send the entire <code>MigratableProcess</code> object
     * by using <code>ObjectOutputStream</code>. After that we receive
     * this migration status from host by using <code>DataInputStream</code>.
     * If the migration fails, the process will restart without losing data..
     *
     * @param socket the server socket
     * @param process the process object
     * @param hostName the host name which the object will migrate to
     * @throws IOException 
     * @see java.net.Socket
     * @see java.io.DataInputStream
     * @see java.io.ObjectOutputStream
     */
    private void sendFile(Socket socket,String filename) throws IOException
    {
        File myFile = new File(filename);
        byte[] myByteArray = new byte[1024];
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        out.writeUTF(filename);
        out.writeLong(myFile.length());
        System.out.println("file length: " + myFile.length());
        FileInputStream fis = new FileInputStream(myFile);
        //bis.read(mybytearray, 0, mybytearray.length);
        OutputStream os = new BufferedOutputStream(socket.getOutputStream()) ;
        //os.write((int)myFile.length());
        int readableBytes=1024*25;
        int n=(int)(myFile.length()/readableBytes);
        int r=(int)(myFile.length()%readableBytes);
        byte b[];
        for(int i=0;i<n;i++)
        {            
            b=new byte[readableBytes];
            fis.read(b);
            os.write(b);  
            os.flush();
        }
        b=new byte[r];
        fis.read(b);
        os.write(b);  
        os.flush();
        
        
    }
//    
//    private void startMigrating(Socket socket, MigratableProcess process, String hostName) throws IOException {
//    	/**
//         * adding code to transfer a file
//         * 
//         * 
//         */
//        String fileName = "i.txt";//to be modified later
//        sendFile(socket, fileName);
//        fileName = "o.txt";
//        sendFile(socket, fileName);
//        
//        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
//    	DataInputStream in = new DataInputStream(socket.getInputStream());
//        boolean status = false;
//        try {    	
//            out.writeObject((Object)process);
//            status = in.readBoolean();
//        }
//    	catch (IOException e1) {
//    		LOG.error(process.getClass().getSimpleName() +
//                    "[" + process.getId() + "] migration error", e1);
//    		restartProcess(process);
//        	socket.close();
//        	return;
//    	}
//        if (status) {
//            System.out.println("Successfully migrated " +
//                    process.getClass().getSimpleName() +
//                    "[" + process.getId() + "]");
//        } 
//        else {
//            System.out.println("Failed to migrate " +
//                    process.getClass().getSimpleName() +
//                    "[" + process.getId() + "]");
//    		restartProcess(process);
//        }
//        try {
//            in.close();
//            out.close();
//        }
//        catch (IOException e) {
//	        System.out.println("file close failed: " +
//	                e.getMessage());
//        }
//        socket.close();
//    }
//
//    
    /*
    changing startMigrating so as to send object as a file
    */
    
    private void startMigrating(Socket socket, MigratableProcess process, String hostName) throws IOException {
    	/**
         * adding code to transfer a file
         * 
         * 
         */
        String fileName = "i.txt";//to be modified later
        sendFile(socket, fileName);
        fileName = "o.txt";
        sendFile(socket, fileName);
        fileName = "object"+process.id + ".ser";
        FileOutputStream fos = new FileOutputStream(fileName);
        ObjectOutputStream out = new ObjectOutputStream(fos);
        
 	DataInputStream in = new DataInputStream(socket.getInputStream());
        
        boolean status = false;
        try {    	
            out.writeObject((Object)process);
            sendFile(socket, fileName);
            status = in.readBoolean();
        }
    	catch (IOException e1) {
    		LOG.error(process.getClass().getSimpleName() +
                    "[" + process.getId() + "] migration error", e1);
    		restartProcess(process);
        	socket.close();
        	return;
    	}
        if (status) {
            System.out.println("Successfully migrated " +
                    process.getClass().getSimpleName() +
                    "[" + process.getId() + "]");
        } 
        else {
            System.out.println("Failed to migrate " +
                    process.getClass().getSimpleName() +
                    "[" + process.getId() + "]");
    		restartProcess(process);
        }
        try {
            out.close();
            fos.close();
        }
        catch (IOException e) {
	        System.out.println("file close failed: " +
	                e.getMessage());
        }
        socket.close();
    }

    
    /**
     * restart the process if migration fails.
     * Regard the process like this a migrated process,
     * so the status can won't lost when running again.
     * 
     * @param process the process object
     */
	private void restartProcess(MigratableProcess process) {
		process.resume();
		process.migrated();
		startProcess(process);
	}

	

    
    /**
     * Print the help information
     */
    private void processHelpCommand() {
        StringBuffer sb = new StringBuffer();
        sb.append("All commands are listed as below\n");
        sb.append("ls:   list all migratable programs\n");
        sb.append("ps:   list all running process\n");
        sb.append("run:  start process.\n");
        sb.append("      run PROCESS_NAME ARG...\n");
        sb.append("mg:   migrate process to another machine\n");
        sb.append(System.getProperty("user.dir"));
        sb.append("      mg PROCESS_ID HOSTNAME\n");
        sb.append("quit: quit Process Manager\n");
        sb.append("help: show help information\n");
        System.out.println(sb.toString());
    }

    /**
     * Main function.
     * Start <code>ProcessManager</code> server and console
     *  
     * @param args program augments, taking in IP as a program argument
     * @see javapm.process.ProcessManager#startServer()
     * @see javapm.process.ProcessManager#startConsole()
     */
    public static void main(String[] args) {
        if(args.length==1)
        {
            IP=args[0];
        }
        ProcessManager.getInstance().startServer();
        ProcessManager.getInstance().startExitListener();
        ProcessManager.getInstance().startConsole();
        
    }
}
