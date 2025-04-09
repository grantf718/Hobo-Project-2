import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class TCPServer {
    private ServerSocket serverSocket = null;
    private Socket socket = null;
    private InputStream inStream = null;
    private OutputStream outStream = null;
    private List<OutputStream> clientOutputs = new ArrayList<>(); // List of client output streams 


    //adding the usernmae map at class level(with other declarations)
    private final Map<Socket, String> clientUsernames = new HashMap<>();

    // Arrays to store questions and answers from file
    private String[] questions;
	private String[] answers;
    private int questionNum = 0;
    private String correctAnswer;

    private HashMap<OutputStream, Integer> allScores = new HashMap<>();

    // Boolean to represent if a client has buzzed in. Set to TRUE once any client buzzes in before the rest. 
    // Used to send "ack" vs "negative-ack". Will reset upon new question
    private boolean firstClient = false; 

    // Set to represent all clients that have buzzed in for the current question. Gets cleared for next question
    private Set<String> buzzedClients = new HashSet<>();

    // new
    private Socket targetClientSocket = null;  // The only client allowed to trigger NEXT
    private final Object targetLock = new Object(); // Lock to protect targetClientSocket
    private final Map<Socket, OutputStream> socketOutputMap = new HashMap<>();

    // ---------------------------------------- // 
    //              Server Port:                //
    // ---------------------------------------- // 
        private final int SERVER_PORT = 1234;
    // ---------------------------------------- // 


    public TCPServer() {

        // Read in questions from file
        String currentDir = System.getProperty("user.dir"); // Based on current directory
        System.out.println("Current directory: " + currentDir + "\n");

        // Automatically get directory syntax based on OS
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            // Windows:
            questions = readFile(currentDir + "\\Questions.txt");
            answers = readFile(currentDir + "\\Answers.txt");
        } else if (os.contains("mac")) {
            // macOS:
            questions = readFile(currentDir + "/Questions.txt"); 
            answers = readFile(currentDir + "/Answers.txt");
        } else {
            System.out.println("OS not supported, Q&A files not read in.");
        }

        // DEBUG: Print out arrays of questions and answers
        // printQnAs();

    	// Create server socket with assigned port
        try {
			serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("\nServer created on port " + serverSocket.getLocalPort() + ".");
		} 
        catch (IOException e) {
			e.printStackTrace();
		}

        createTerminalThread();

    }

    // Accept client connections
    public void listenForConnections() {
        System.out.println("Listening for connections!\n");
        System.out.println("Once everyone's in, enter 'start' to begin the game!\n");
        try {
            while (true) {
                // Accept incoming connection from a client 
            	socket = serverSocket.accept();
            	// Fetch the streams from client	
                inStream = socket.getInputStream();
                outStream = socket.getOutputStream();
                // Store output stream of client
                clientOutputs.add(outStream); 

                socketOutputMap.put(socket, outStream);
                allScores.put(outStream, 0);

                System.out.println("Client " + socket.getInetAddress() + " has connected");

                // new
                synchronized (targetLock) {
                    if (targetClientSocket == null || targetClientSocket.isClosed()) {
                        targetClientSocket = socket;
                        System.out.println("Assigned target client: " + socket.getInetAddress());
                    }
                }                

                // Create a new threads
                createReadThread(socket, inStream, outStream);
                // createWriteThread();
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
                while(true){
                    try {
                        // Check terminal for the start message 
                        BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
                        String typedMessage = inputReader.readLine();
    
                        // Type 'start' to begin the game 
                        if (typedMessage.equals("start") && questionNum == 0) { 
                            System.out.println("Starting");
                            nextQuestion();
                        }
    
                        // DEBUG: Send typed message 'test' to client
                        if (typedMessage.equals("test")) { 
                            synchronized (socket) {
                                outStream.write(typedMessage.getBytes("UTF-8"));
                            }
                        }

                        if (typedMessage.equals("end")) {
                            System.out.println("Ending game for all clients...");
                            for (OutputStream out : clientOutputs) {
                                out.write("END\n".getBytes("UTF-8"));
                            }
                            System.exit(0); // optional: shut down server after notifying clients
                            }

                        if (typedMessage.startsWith("terminate ")) {
                                String usernameToTerminate = typedMessage.substring(10).trim();
                                terminateClientByUsername(usernameToTerminate);
                            }
    
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                
            }
            
        };
        terminalThread.start();
    }

    // Creates and starts an anonymous thread that continuously monitors the input stream of an active socket connection.
    private void createReadThread(Socket clientSocket, InputStream clientIn, OutputStream clientOut) {
        new Thread(() -> {
            String clientIP = clientSocket.getInetAddress().toString();
            String clientUsername = "Unknown";

            byte[] buffer = new byte[512];
            try {
                while (true) {
                    int len = clientIn.read(buffer);
                    if (len == -1) {
                        // clean disconnect
                        System.out.println(clientUsername + " disconnected.");
                        break;
                    }
                    String msg = new String(buffer, 0, len, "UTF-8").trim();
                    System.out.println("From " + clientUsername + ": " + msg);

                    // --- handle USER ---
                    if (msg.startsWith("USER ")) {
                        clientUsername = msg.substring(5);
                        clientUsernames.put(clientSocket, clientUsername); //new
                        System.out.println("Set username " + clientUsername + " for " + clientIP);

                    // --- handle buzz ---
                    } else if (msg.equals("buzz")) {
                        if (buzzedClients.contains(clientIP)) continue;
                        buzzedClients.add(clientIP);
                        if (!firstClient) {
                            firstClient = true;
                            clientOut.write("ack\n".getBytes("UTF-8"));
                            System.out.println(clientUsername + " was first to buzz.");
                        } else {
                            clientOut.write("negative-ack\n".getBytes("UTF-8"));
                            System.out.println(clientUsername + " buzzed too late.");
                        }

                    // --- handle ANSWER ---
                    } else if (msg.startsWith("ANSWER ")) {
                        String answer = msg.substring(7);
                        if (answer.equals(correctAnswer)) {
                            int score = allScores.get(clientOut) + 10;
                            allScores.put(clientOut, score);
                            clientOut.write(("SCORE +10 points\n").getBytes("UTF-8"));
                        } else {
                            int score = allScores.get(clientOut) - 10;
                            allScores.put(clientOut, score);
                            clientOut.write(("SCORE -10 points\n").getBytes("UTF-8"));
                        }

                    // --- handle NEXT ---
                    } else if (msg.equals("NEXT")) {
                        synchronized (targetLock) {
                            if (clientSocket.equals(targetClientSocket)) {
                                System.out.println("Target client requested NEXT.");
                                nextQuestion();
                            }
                        }

                    // --- handle SCORE ---
                    } else if (msg.startsWith("SCORE -20")) {
                        int score = allScores.get(clientOut) - 20;
                        allScores.put(clientOut, score);
                    }
                }
            } catch (SocketException se) {
                System.out.println("Connection reset by " + clientUsername);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                // --- CLEANUP ---
                try {
                    clientIn.close();
                    clientOut.close();
                    clientSocket.close();
                } catch (IOException ignored) {}

                clientOutputs.remove(clientOut);
                socketOutputMap.remove(clientSocket);
                allScores.remove(clientOut);

                synchronized (targetLock) {
                    if (clientSocket.equals(targetClientSocket)) {
                        targetClientSocket = null;
                        // pick a new one if any left
                        for (Socket s : socketOutputMap.keySet()) {
                            if (!s.isClosed()) {
                                targetClientSocket = s;
                                System.out.println("New target: " + s.getInetAddress());
                                break;
                            }
                        }
                        if (targetClientSocket == null)
                            System.out.println("No clients left to assign target.");
                    }
                }
            }
        }, "ClientReader-" + clientSocket.getInetAddress()).start();
    }


    // Moves to the next question
    public void nextQuestion(){
        
        // Check if there are no questions left
        if (questionNum >= questions.length) {
            // End the game
            printScoreboard();
            // Print game over message
            System.out.println("\nGAME OVER! (No more questions)"); 
            
            // // Convert the HashMap with scores to a list
            // List<Map.Entry<OutputStream, Integer>> entries = new ArrayList<>(allScores.entrySet());

            // // Sort the list in descending order by score
            // entries.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

            // // Print the sorted scores
            // for (Map.Entry<OutputStream, Integer> entry : entries) {
            //     System.out.println("Score for " + entry.getKey() + " : " + entry.getValue());
            // }
            
            return;
        }

        // Increment question number
        questionNum++; 
        System.out.println("\nMoving on to question " + questionNum);

        // Set the current correct answer
        correctAnswer = answers[questionNum-1].substring(0, answers[questionNum-1].indexOf(','));;
        System.out.println("(Correct answer for Q" + questionNum + ": " + correctAnswer + ")");
        

        // Clear the set of buzzed in clients
        buzzedClients.clear();

        // Get new Q&A from array, tag both strings 
        String question = "QUESTION Q" + questionNum + ". " + questions[questionNum-1]; 
        String currentAnswers = "ANSWERS " + answers[questionNum-1];

        // Send new question to all connected clients
        for (OutputStream outStream : clientOutputs) {
            System.out.println("Sending Q" + questionNum + " to clients\n");
            try {
                // Send question and answers in one string on different lines 
                String fullMessage = question + "\n" + currentAnswers + "\n";
                outStream.write(fullMessage.getBytes("UTF-8"));

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Reset firstClient to false 
        firstClient = false;
        
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
        }
        return lines;
    }

    // Debug function that prints all questions and answers to terminal 
    public void printQnAs(){
        for (int i = 0; i < 20; i++) {
            System.out.println("Q" + (i + 1) + ": " + questions[i]);
            System.out.println("A: " + answers[i] + "\n");
        }

    }

    private synchronized void terminateClientByUsername(String username) {
        List<Socket> toRemove = new ArrayList<>();
        
        synchronized (clientUsernames) {
            for (Map.Entry<Socket, String> entry : clientUsernames.entrySet()) {
                if (entry.getValue().equalsIgnoreCase(username)) {
                    toRemove.add(entry.getKey());
                }
            }
        }
        
        if (toRemove.isEmpty()) {
            System.out.println("No clients found with username: " + username);
        } else {
            for (Socket s : toRemove) {
                terminateClient(s);
            }
        }
    }
    
    private synchronized void reassignTargetClient() {
        synchronized (targetLock) {
            targetClientSocket = null;
            // Find the first active client to reassign
            for (Socket s : socketOutputMap.keySet()) {
                if (!s.isClosed()) {
                    targetClientSocket = s;
                    System.out.println("New target client assigned: " + 
                        clientUsernames.getOrDefault(s, "Unknown") + 
                        " (" + s.getInetAddress() + ")");
                    break;
                }
            }
            if (targetClientSocket == null) {
                System.out.println("No clients left to assign as target");
            }
        }
    }

    private synchronized void terminateClient(Socket clientSocket) {
        try {
            OutputStream out = socketOutputMap.get(clientSocket);
            if (out != null) {
                try {
                    out.write("TERMINATE\n".getBytes("UTF-8"));
                    out.flush();
                    Thread.sleep(100);
                } catch (Exception e) {
                    System.out.println("Error sending terminate command: " + e.getMessage());
                }
    
                // Clean up all references
                synchronized (this) {
                    clientOutputs.remove(out);
                    socketOutputMap.remove(clientSocket);
                    allScores.remove(out);
                    clientUsernames.remove(clientSocket);
                    
                   // Handle target client reassignment
                    synchronized (targetLock) {
                        if (clientSocket.equals(targetClientSocket)) {
                            reassignTargetClient();  // Use the new method here
                        }
                    }
                }
    
                try {
                    if (!clientSocket.isClosed()) {
                        clientSocket.close();
                    }
                    System.out.println("Terminated client: " + 
                        clientUsernames.get(clientSocket) + " (" + clientSocket.getInetAddress() + ")");
                } catch (IOException e) {
                    System.out.println("Error closing socket: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("Error in terminateClient: " + e.getMessage());
        }
    }

    private void printScoreboard() {
        System.out.println("\n=== GAME OVER ===");
        System.out.println("Final Scores:");
        System.out.println("--------------");
    
        // Get all active players with their usernames and scores
        List<Map.Entry<String, Integer>> playerScores = new ArrayList<>();
        
        synchronized (this) {
            for (Map.Entry<Socket, OutputStream> entry : socketOutputMap.entrySet()) {
                String username = clientUsernames.get(entry.getKey());
                Integer score = allScores.get(entry.getValue());
                if (username != null && score != null) {
                    playerScores.add(new AbstractMap.SimpleEntry<>(username, score));
                }
            }
        }
    
        // Sort by score (highest first)
        playerScores.sort((p1, p2) -> p2.getValue().compareTo(p1.getValue()));
    
        // Print the scoreboard
        int rank = 1;
        for (Map.Entry<String, Integer> player : playerScores) {
            System.out.printf("%d. %-15s: %d points%n", 
                             rank++, 
                             player.getKey(), 
                             player.getValue());
        }
    
        // Declare winner if there are players
        if (!playerScores.isEmpty()) {
            Map.Entry<String, Integer> winner = playerScores.get(0);
            System.out.println("\n🏆 WINNER 🏆");
            System.out.println(winner.getKey() + " wins with " + winner.getValue() + " points!");
            
            // Send winner announcement to all clients
            String winnerMessage = "WINNER " + winner.getKey() + " wins with " + winner.getValue() + " points!";
            broadcastMessage(winnerMessage);
        } else {
            System.out.println("No players finished the game.");
        }
    }
    
    // Helper method to send messages to all clients
    private void broadcastMessage(String message) {
        for (OutputStream out : clientOutputs) {
            try {
                out.write((message + "\n").getBytes("UTF-8"));
            } catch (IOException e) {
                System.out.println("Error broadcasting to client: " + e.getMessage());
            }
        }
    }
    
    public static void main(String[] args) {
        TCPServer server = new TCPServer();
        server.listenForConnections();
    }
}