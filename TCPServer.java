import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class TCPServer 
{
    private ServerSocket serverSocket = null;
    private Socket socket = null;
    private InputStream inStream = null;
    private OutputStream outStream = null;
    private List<OutputStream> clientOutputs = new ArrayList<>(); // List of client output streams 

    // Arrays to store questions and answers from file
    private String[] questions;
	private String[] answers;
    private int questionNum = 0;

    // Boolean to represent if a client has buzzed in. Set to TRUE once any client buzzes in before the rest. 
    // Used to send "ack" vs "negative-ack". Will reset upon new question
    private boolean firstClient = false; 

    // ---------------------------------------- // 
    //              Server Port:                //
    // ---------------------------------------- // 
    private final int SERVER_PORT = 1234;
    // ---------------------------------------- // 


    public TCPServer() {

        // Read in questions from file
        String currentDir = System.getProperty("user.dir"); // Based on current directory
        System.out.println("Current directory: " + currentDir + "\n");

        // Uncomment based on OS:

            // macOS: 
                questions = readFile(currentDir + "/Questions.txt"); 
                answers = readFile(currentDir + "/Answers.txt");

            // Windows:
                // questions = readFile(currentDir + "\\Questions.txt");
                // answers = readFile(currentDir + "\\Answers.txt");

            // DEBUG: Print out arrays of questions and answers
            // for(int i = 0; i < questions.length; i++){ 
            //     System.out.println("Q" + (i+1) + ": " + questions[i]); 
            //     System.out.println("A: " + answers[i] + "\n");
            // }

    	// Create server socket with assigned port
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
            	// Fetch the streams from client	
                inStream = socket.getInputStream();
                outStream = socket.getOutputStream();
                // Store output stream of client
                clientOutputs.add(outStream); 
                System.out.println("Client " + socket.getInetAddress() + " has connected");
                // Create a new threads
                createReadThread();
                // createWriteThread();
                createTerminalThread();
            }
        }
        catch (IOException io) {
            io.printStackTrace();
        }
    }

    // Creates a thread for terminal input so as to not block client handling while waiting for input
    public void createTerminalThread(){
        Thread terminalThread = new Thread() {
            public void run(){
                try {
                    // Check terminal for the start message 
                    BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
                    String typedMessage = inputReader.readLine();
                    if (typedMessage.equals("start")) { // Type 'start' to begin the game
                        nextQuestion();
                    }

                    // DEBUG: Send typed message 'test' to client
                    if (typedMessage.equals("test")) { 
                        synchronized (socket) {
                            outStream.write(typedMessage.getBytes("UTF-8"));
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        terminalThread.start();
    }

    // Creates and starts an anonymous thread that continuously monitors the input stream of an active socket connection.
    public void createReadThread() {

        // Capture the current socket and stream in local variables to be used only by each client's unique thread
        final Socket clientSocket = socket;
        final InputStream clientInStream = inStream;

        Thread readThread = new Thread() {
            public void run() {

                // Store client's IP and username for easy access 
                final String clientIP = clientSocket.getInetAddress().toString();
                String clientUsername = "Unknown user"; // Default to unknown so the code doesnt flip out!

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
                            String receivedMessage = new String(arrayBytes, "UTF-8");

                            // Accept client username if it was sent over
                            if(receivedMessage.startsWith("USER ")){
                                clientUsername = receivedMessage.substring(5); // Remove the USER tag
                                System.out.println("Set username " + clientUsername + " for " + clientIP);

                            // If it's not a username being sent over, process the message as normal
                            } else {
                                // Print out received message
                                System.out.println("Incoming from " + clientUsername + " (" + clientIP + "): " + receivedMessage);

                                // Determine if this client was the first to buzz
                                if(!firstClient){
                                    // If it was, update firstClient to TRUE
                                    firstClient = !firstClient; 

                                    // Determine if the client's answer is correct
                                    if(receivedMessage.equals("correct answer")){
                                        // If correct, respond with "ack"
                                        System.out.println("'" + receivedMessage + "' is correct. Sending 'ack' to " + clientSocket.getInetAddress() + ".");
                                        synchronized (socket) {
                                            String response = "ack";
                                            outStream.write(response.getBytes("UTF-8"));
                                        }
                                    } else {
                                        // If incorrect, respond with "negative-ack"
                                        System.out.println("'" + receivedMessage + "' is incorrect. Sending 'negative-ack' to " + clientUsername + " (" + clientSocket.getInetAddress() + ").");
                                        synchronized (socket) {
                                            String response = "negative-ack";
                                            outStream.write(response.getBytes("UTF-8"));
                                        }

                                    }

                                    // Move to next question
                                    nextQuestion();

                                } else {
                                    System.out.println("Client " + clientSocket.getInetAddress() + " buzzed in too late.");
                                }
                            }
                        } 
                    } catch (EOFException | SocketException se) {
                        se.printStackTrace();
                    } catch (IOException i) {
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
    // public void createWriteThread() {

    //     // Capture the current socket and stream in local variables to be used only by each client's unique thread
    //     final Socket clientSocket = socket;
    //     final InputStream clientInStream = inStream;

    //     // Store client's IP for easy access 
    //     final String clientIP = clientSocket.getInetAddress().toString();

    //     Thread writeThread = new Thread() {

    //         public void run() {
    //         	//check socket connectivity
    //             while (socket.isConnected()) {
    //                 try {

    //                     // Code to write here
                        
    //                 } catch (IOException i) {
    //                     i.printStackTrace();
    //                 } catch (InterruptedException ie) {
    //                     ie.printStackTrace();
    //                 } 
    //            }
    //         }
    //     };
    //     writeThread.setPriority(Thread.MAX_PRIORITY);
    //     writeThread.start();
    // }

    // Moves to the next question
    public void nextQuestion(){
        // Check if there are no questions left
        if (questionNum >= questions.length) {
            System.out.println("No more questions available.");
            return;
            // . . . . . . . . . . . . .
            // . Code to end game here .
            // . . . . . . . . . . . . .
        }

        // Increment question number
        questionNum++; 
        System.out.println("\nMoving on to question " + questionNum);

        // Get new question from array
        String question = questions[questionNum]; 

        // Send new question to all connected clients
        for (OutputStream outStream : clientOutputs) {
            try {
                System.out.println("Sending Q" + questionNum + " to a client");
                outStream.write((question + "\n").getBytes("UTF-8"));
                outStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        
    }

    // Function to read in a text file containing 20 questions or answers separated by line
    public static String[] readFile(String filePath){
        String[] lines = new String[20];
        
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count < 20) {
                lines[count] = line;
                count++;
            }
        } catch (IOException e) {
            System.out.println("An error occurred: " + e.getMessage());
            System.out.println("Make sure the right version of current working directory is uncommented in constructor of TCPClient!");
        }
        return lines;
    }
    
    public static void main(String[] args) {
        TCPServer server = new TCPServer();
        server.listenForConnections();
    }
}
