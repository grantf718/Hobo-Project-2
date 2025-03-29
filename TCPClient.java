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

    private String clientUsername;

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
        clientUsername = JOptionPane.showInputDialog(null, "<html><span style='font-size:20pt; font-weight:bold;'>Welcome to EPIC Pokemon trivia.</span><br><br>Enter username to join:</html>","", JOptionPane.PLAIN_MESSAGE);
        // If user submits a blank username, assign it one (Player XXXX)
        if(clientUsername.isBlank()){
            SecureRandom secureRand = new SecureRandom();
            int r1 = secureRand.nextInt(10), r2 = secureRand.nextInt(10), r3 = secureRand.nextInt(10), r4 = secureRand.nextInt(10);
            clientUsername = "Player " + r1 + r2 + r3 + r4;
            System.out.println("Auto-assigned username: " + clientUsername);
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

            // Immediately send the client's username to the server (with USER tag)
            // String usernameMessage = "USER " + clientUsername;
            // outStream.write(usernameMessage.getBytes("UTF-8"));
            // // outStream.flush();
            // System.out.println("Username sent to server: " + usernameMessage);

            // Send server the client's username upon socket creation
            synchronized (socket) {
                clientUsername = "USER " + clientUsername; // Add USER tag so server can recognize it
                outStream.write(clientUsername.getBytes("UTF-8"));
                System.out.println("Sent username to server.");
            }

            // Create threads for client
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
                            String receivedMessage = new String(arrayBytes, "UTF-8");
                            System.out.println("Incoming from server: " + receivedMessage);

                            if(receivedMessage.startsWith("QUESTION ")){
                                // . . . . . . . . . . . . . . . . .
                                // . Code to display question here .
                                // . . . . . . . . . . . . . . . . .
                            } else if (receivedMessage.startsWith("ANSWERS ")){
                                // . . . . . . . . . . . . . . . . .
                                // . Code to display answers here  .
                                // . . . . . . . . . . . . . . . . .
                            }

                        }
                        // else {
                        // 	notifyAll();
                        // }
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

                        // THIS IS THE ORIGINAL CODE TO ACCEPT A TYPED LINE FROM TERMINAL, 
                        // WHICH IS NOT NEEDED BUT CONTAINS THE CODE TO WRITE TO SERVER.
                        // ------------------------------------------------------------
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

    public static void main(String[] args) throws Exception {
        TCPClient myChatClient = new TCPClient();
        myChatClient.createSocket();
    }

}
