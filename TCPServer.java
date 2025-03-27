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

    // Boolean to represent if a client has buzzed in. Set to TRUE once any client buzzes in before the rest. 
    // Used to send "ack" vs "negative-ack". Will reset upon new question
    private boolean firstClient = false; 

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
        System.out.println("Listening for connections!\n");
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
                            String recievedMessage = new String(arrayBytes, "UTF-8");
                            // Print out received message
                            System.out.println("Incoming from " + clientSocket.getInetAddress() + ": " + recievedMessage);

                            // Determine if this client was the first to buzz
                            if(!firstClient){
                                // If it was, update firstClient to TRUE
                                firstClient = !firstClient; 

                                // Confirm connection again before responding 
                                if(socket.isConnected()){
                                    // Determine if the client's answer is correct
                                    if(recievedMessage.equals("correct answer")){
                                        // If correct, respond with "ack"
                                        System.out.println("'" + recievedMessage + "' is correct. Sending 'ack' to " + clientSocket.getInetAddress() + ".");
                                        synchronized (socket) {
                                            String response = "ack";
                                            outStream.write(response.getBytes("UTF-8"));
                                        }
                                    } else {
                                        // If incorrect, respond with "negative-ack"
                                        System.out.println("'" + recievedMessage + "' is incorrect. Sending 'negative-ack' to " + clientSocket.getInetAddress() + ".");
                                        synchronized (socket) {
                                            String response = "negative-ack";
                                            outStream.write(response.getBytes("UTF-8"));
                                        }

                                    }
                                } else {
                                    System.out.println("Error while responding to client. The socket was not connected. They had the correct answer.");
                                }

                                
                            } else {
                                System.out.println("Client " + clientSocket.getInetAddress() + " buzzed in too late.");
                            }
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
