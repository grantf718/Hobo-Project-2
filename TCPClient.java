import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.SecureRandom;

import javax.swing.JOptionPane;

public class TCPClient 
{
    private Socket socket = null;
    private InputStream inStream = null;
    private OutputStream outStream = null;

    private String[] questions;
	private String[] answers;
    private String clientName;

    // ---------------------------------------- // 
    //            Destination Socket:           //
    // ---------------------------------------- // 
        
        // Destination port: (must match server)
        private final int DEST_PORT = 1234;

        // Destination IP: (must match server)
        // private final String DEST_IP = "localhost"; // <-- localhost (for testing purposes)
        private final String DEST_IP = "10.111.134.82"; // <-- Grant's IP
        // private final String DEST_IP = ""; // <-- Evan's IP
        // private final String DEST_IP = ""; // <-- Jessica's IP

    // ---------------------------------------- // 

    public TCPClient() {

        // Read in questions from file
        String currentDir = System.getProperty("user.dir"); // Based on current directory
        System.out.println("Current directory: " + currentDir + "\n");

        // Uncomment based on OS:

            // macOS file paths: 
                questions = readFile(currentDir + "/Questions.txt"); 
                answers = readFile(currentDir + "/Answers.txt");

            // Windows file paths:
                // questions = readFile(currentDir + "\\Questions.txt");
                // answers = readFile(currentDir + "\\Answers.txt");


            // DEBUG: Print out arrays of questions and answers
            // for(int i = 0; i < questions.length; i++){ 
            //     System.out.println("Q" + (i+1) + ": " + questions[i]); 
            //     System.out.println("A: " + answers[i] + "\n");
            // }

    	// Create a socket to communicate with server
        try {
			socket = new Socket(DEST_IP, DEST_PORT);
			System.out.println("Connected to server!");
		} 
        catch (UnknownHostException e) {
			e.printStackTrace(); 
            System.out.println("\nFAILED TO ESTABLISH A CONNECTION TO THE SERVER.\n\nCheck:\n 1. That the server is running\n 2. The destination socket configured in the client matches the socket of the server you're trying to reach.\n");
        } 
        catch (IOException e) {
			e.printStackTrace();
            System.out.println("\nFAILED TO ESTABLISH A CONNECTION TO THE SERVER.\n\nCheck:\n 1. That the server is running\n 2. The destination socket configured in the client matches the socket of the server you're trying to reach.\n");
		}

        // Enter username popup
        clientName = JOptionPane.showInputDialog(null, "<html><span style='font-size:20pt; font-weight:bold;'>Welcome to EPIC Pokemon trivia.</span><br><br>Enter username to join:</html>","", JOptionPane.PLAIN_MESSAGE);
        // If user submits a blank username, assign it one (Player XXXX)
        if(clientName.isBlank()){
            SecureRandom secureRand = new SecureRandom();
            int r1 = secureRand.nextInt(10), r2 = secureRand.nextInt(10), r3 = secureRand.nextInt(10), r4 = secureRand.nextInt(10);
            clientName = "Player " + r1 + r2 + r3 + r4;
            System.out.println(clientName);
        }

        // . . . . . . . . . . . .
        // . Implement GUI here  .
        // . . . . . . . . . . . .

    }

    // Create client socket
    public void createSocket() {
        try {
        	// Fetch streams
            inStream = socket.getInputStream();
            outStream = socket.getOutputStream();
            createReadThread();
            createWriteThread();
        } 
        catch (UnknownHostException u) {
            u.printStackTrace();
        } 
        catch (IOException io) {
            io.printStackTrace();
        }
    }

    public void createReadThread() {
        Thread readThread = new Thread() {
            public void run() {
                while (socket.isConnected()) {
                    try {
                        byte[] readBuffer = new byte[200];
                        int num = inStream.read(readBuffer);

                        if (num > 0) {
                            byte[] arrayBytes = new byte[num];
                            System.arraycopy(readBuffer, 0, arrayBytes, 0, num);
                            String recvedMessage = new String(arrayBytes, "UTF-8");
                            System.out.println("Incoming from server: " + recvedMessage);
                        }
                        else {
                        	notifyAll();
                        }
                    }
                    catch (SocketException se) {
                        System.exit(0);
                    }
                    catch (IOException i) {
                        i.printStackTrace();
                    }
                }
            }
        };
        readThread.setPriority(Thread.MAX_PRIORITY);
        readThread.start();
    }

    public void createWriteThread() {
        Thread writeThread = new Thread() {
            public void run() {
                while (socket.isConnected()) {
                	try {
                        BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
                        sleep(100);
                        String typedMessage = inputReader.readLine();
                        if (typedMessage != null && typedMessage.length() > 0) {
                            synchronized (socket) {
                                outStream.write(typedMessage.getBytes("UTF-8"));
                            }
                            sleep(100);
                        }
                        else {
                        	notifyAll();
                        }
                    } 
                	catch (IOException i) {
                        i.printStackTrace();
                    } 
                	catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
            }
        };
        writeThread.setPriority(Thread.MAX_PRIORITY);
        writeThread.start();
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

    public static void main(String[] args) throws Exception {
        TCPClient myChatClient = new TCPClient();
        myChatClient.createSocket();
    }

}
