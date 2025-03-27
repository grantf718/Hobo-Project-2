import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;


public class TCPServer 
{
    private ServerSocket serverSocket = null;
    private Socket socket = null;
    private InputStream inStream = null;
    private OutputStream outStream = null;

    // ---------------------------------------- // 
    //              Server Port:                //
    // ---------------------------------------- // 
    private final int SERVER_PORT = 1234;
    // ---------------------------------------- // 


    public TCPServer() {
    	// Create server socket 
        try {
			serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("\nServer created on port " + serverSocket.getLocalPort() + ".");
		} 
        catch (IOException e) {
			e.printStackTrace();
		}
    }

    // Accept client connections
    public void listenForConnections() {
        System.out.println("Listening for connections!");
        try {
            while (true) {
                // Accept incoming connection from a client 
            	socket = serverSocket.accept();
            	// Fetch the streams from the connected client	
                inStream = socket.getInputStream();
                outStream = socket.getOutputStream();
                System.out.println("Connected to client " + socket.getLocalSocketAddress());
                createReadThread();
                createWriteThread();
            }
        }
        catch (IOException io) {
            io.printStackTrace();
        }
    }

    // Creates and starts an anonymous thread that continuously monitors the input stream of an active socket connection.
    public void createReadThread() {

        // Capture the current socket and stream in local variables to be used only by each client's unique thread
        final Socket clientSocket = socket;
        final InputStream clientInStream = inStream;

        Thread readThread = new Thread() {
            public void run() {
            	// Check socket connectivity before doing anything
                while (socket.isConnected()) {
                    try {
                        byte[] readBuffer = new byte[200];
                        // Read the data from client
                        int num = clientInStream.read(readBuffer);
                        // Check if inStream is not empty before doing anything
                        if (num > 0) {
                            byte[] arrayBytes = new byte[num];
                            System.arraycopy(readBuffer, 0, arrayBytes, 0, num);
                            String recvedMessage = new String(arrayBytes, "UTF-8");
                            // Print out received message
                            System.out.println("Incoming from " + clientSocket.getInetAddress() + ": " + recvedMessage);
                        } 
                        else {
                            // Commented out notifyAll because it was giving exceptions 
                            // notifyAll();
                        }
                    } 
                    catch (SocketException se) {
                        System.out.println("SocketException: Client " + clientSocket.getInetAddress() + " disconnected.");
                        System.exit(0);

                    }
                    catch (IOException i) {
                        i.printStackTrace();
                        System.out.println("IOException while reading from " + clientSocket.getInetAddress() + ": " + i.getMessage());
                    }
                }
            }
        };
        readThread.setPriority(Thread.MAX_PRIORITY);
        readThread.start();
    }

    //write thread using anonymous class
    public void createWriteThread() 
    {
        Thread writeThread = new Thread() 
        {
            public void run() 
            {
            	//check socket connectivity
                while (socket.isConnected()) 
                {
                    try 
                    {
                    	//Use BufferedReader or Scanner to read from console
                        BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
                        sleep(100);
                        String typedMessage = inputReader.readLine();
                        if (typedMessage != null && typedMessage.length() > 0) 
                        {
                            synchronized (socket) 
                            {
                                outStream.write(typedMessage.getBytes("UTF-8"));
                            }
                            sleep(100);
                        } 
                        else 
                        {
                        	notifyAll();
                        }
                    }
                    catch (IOException i) 
                    {
                        i.printStackTrace();
                    } 
                    catch (InterruptedException ie) 
                    {
                        ie.printStackTrace();
                    }
               }
            }
        };
        writeThread.setPriority(Thread.MAX_PRIORITY);
        writeThread.start();
    }

    public static void main(String[] args)
    {
        TCPServer chatServer = new TCPServer();
        chatServer.listenForConnections();
    }
}
