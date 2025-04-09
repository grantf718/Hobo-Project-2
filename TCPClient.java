import java.awt.Color;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;

public class TCPClient implements ActionListener {

    private Socket socket = null;
    private OutputStream outStream = null;

    private String clientUsername;

    // GUI vars
    private JButton poll;
	private JButton submit;
	private JRadioButton options[];
	private ButtonGroup optionGroup;
	private JLabel questionLabel;
	private JLabel timer;
    private Timer t;
	private JLabel scoreLabel;
    private JLabel totalScore;
    private int clientScore = 0;
	private TimerTask clock;
	private JFrame window;

    private String currentQuestion;
    private String[] answerChoices = new String[4];
    private String currentSelection;
    private String correctAnswer;
    private boolean ack = false;
    private boolean submitted = false;
    private boolean ACKpolled = false;

    // ---------------------------------------- // 
    //            Destination Socket:           //
    // ---------------------------------------- // 
        
        // Destination port: (must match server)
        private final int DEST_PORT = 1234;

        // Destination IP: (must match server)
        private final String DEST_IP = "10.111.134.82"; // <-- Grant's IP

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

        // Create GUI
        createGUI();

        // Disable buttons by default
        for(int option = 0; option < options.length; option++)
            options[option].setEnabled(false);
        poll.setEnabled(false);
        submit.setEnabled(false);

    }

    // Create client socket
    public void createSocket() {
        try {
        	// Fetch output stream
            outStream = socket.getOutputStream();

            // Send server the client's username upon socket creation
            synchronized (socket) {
                clientUsername = "USER " + clientUsername; // Add USER tag so server can recognize it
                outStream.write(clientUsername.getBytes("UTF-8"));
                System.out.println("Sent username to server.");
            }

            // Create threads for client
            createReadThread();

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
                        // byte[] readBuffer = new byte[200];
                        // int num = inStream.read(readBuffer);

                        // if (num > 0) {
                            // byte[] arrayBytes = new byte[num];
                            // System.arraycopy(readBuffer, 0, arrayBytes, 0, num);
                            // String receivedMessage = new String(arrayBytes, "UTF-8");
                            // System.out.println("Incoming from server: " + receivedMessage);


                            // Deal with received message 
                            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                            String receivedLine;

                            // Go line by line to separate message
                            while ((receivedLine = reader.readLine()) != null) {

                                System.out.println("Incoming line: " + receivedLine);

                                // Handle correct answer
                                if(receivedLine.startsWith("ack")){
                                    System.out.println("You were the first to buzz in!");
                                    // Set ack to true for this client
                                    ack = true;
                                    ACKpolled = true;
                                    // Option buttons should become enabled when second phase starts

                                // Handle incorrect answer
                                                                  }

                                else if (receivedLine.startsWith("END")) {
                                    JOptionPane.showMessageDialog(null, "The game has been ended by the host.", "Game Over", JOptionPane.INFORMATION_MESSAGE);
                                    System.exit(0); // Close client
                                }
                                                         
                                else if (receivedLine.startsWith("negative-ack")){ 
                                    System.out.println("You were not the first to buzz in.");
                                    // Ack remains false
                                    ack = false;
                                    // Option buttons should stay disabled when second phase start

 
                                    
                                // Handle question
                                } else if(receivedLine.startsWith("QUESTION ")){
                                    // Set incoming question & display it on GUI 

                                    // Reset tracker of whether client sumitted for next question
                                    submitted = false;
                                    ACKpolled = false;

                                    // Cancel any existing timer and timer task
                                    if(t != null) {
                                        t.cancel();
                                    }
                                    // Create a new Timer and TimerTask for the new question
                                    t = new Timer();
                                    clock = new TimerCode(15);
                                    t.schedule(clock, 0, 1000);
                                    
                                    // Remove QUESTION tag
                                    currentQuestion = receivedLine.substring(9); 

                                    // Print current question
                                    System.out.println("Current question set to: " + currentQuestion);

                                    // Update question label on the Event Dispatch Thread
                                    SwingUtilities.invokeLater(() -> {
                                        // Add html tags for line wrapping 
                                        questionLabel.setText("<html>" + currentQuestion + "</html>");
                                        window.repaint();
                                    });

                                // Handle score
                                } else if(receivedLine.startsWith("SCORE ")){
                                    // Set the displayed score to the appropriate score
                                    if(receivedLine.equals("SCORE -" + 10 + " points")){
                                        clientScore -= 10;
                                    } else if(receivedLine.equals("SCORE +" + 10 + " points")) {
                                        clientScore += 10;
                                    } 

                                // Handle answer choices 
                                } else if (receivedLine.startsWith("ANSWERS ")){
                                    // Set incoming answers & display them on GUI

                                    // Set correct answer
                                    correctAnswer = receivedLine.substring(8, receivedLine.indexOf(','));

                                    // Set answer choices
                                    String parts[] = receivedLine.substring(8).split(", ");

                                    // OLD (Not randomized answer choices)
                                    // for (int i = 0; i < 4; i++) {
                                    //     answerChoices[i] = parts[i];
                                    // }

                                    // // DEBUG: Print out correct answer and answer choices
                                    // System.out.println("Correct answer set to: " + correctAnswer);
                                    // System.out.println("Answer choice 1 set to: " + answerChoices[0]);
                                    // System.out.println("Answer choice 2 set to: " + answerChoices[1]);
                                    // System.out.println("Answer choice 3 set to: " + answerChoices[2]);
                                    // System.out.println("Answer choice 4 set to: " + answerChoices[3]);

                                    // The first item is the correct answer
                                    correctAnswer = parts[0];

                                    // Create a list of pairs (answer text, isCorrect)
                                    List<AbstractMap.SimpleEntry<String, Boolean>> choicesList = new ArrayList<>();
                                    for (int i = 0; i < parts.length; i++) {
                                        boolean isCorrect = (i == 0);
                                        choicesList.add(new AbstractMap.SimpleEntry<>(parts[i], isCorrect));
                                    }

                                    // Shuffle the choices
                                    Collections.shuffle(choicesList);

                                    // Update answerChoices[] and remember the new correctAnswer
                                    for (int i = 0; i < choicesList.size(); i++) {
                                        answerChoices[i] = choicesList.get(i).getKey();
                                        if (choicesList.get(i).getValue()) {
                                            correctAnswer = choicesList.get(i).getKey(); // Update correctAnswer to the shuffled version
                                        }
                                    }

                                    // DEBUG: Print out correct answer and answer choices
                                    System.out.println("Correct answer set to: " + correctAnswer);
                                    for (int i = 0; i < answerChoices.length; i++) {
                                        System.out.println("Answer choice " + (i + 1) + " set to: " + answerChoices[i]);
                                    }


                                    // Update GUI buttons accordingly on the Event Dispatch Thread
                                    SwingUtilities.invokeLater(() -> {
                                        for (int i = 0; i < options.length; i++) {
                                            options[i].setText(answerChoices[i]);
                                        }
                                        window.repaint();
                                    });                                    
                                }
                            }
                        // }
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

    // Create GUI
    public void createGUI(){

        // Window is titled with the player's username
        window = new JFrame(clientUsername); 

        // Question
		questionLabel = new JLabel("Waiting for host to start"); 
		window.add(questionLabel);
		questionLabel.setBounds(10, 5, 350, 100);;
		
        // Answer choice buttons
		options = new JRadioButton[4];
		optionGroup = new ButtonGroup();
		for(int index = 0; index < options.length; index++) {
			options[index] = new JRadioButton(answerChoices[index]);
			options[index].addActionListener(this);
			options[index].setBounds(10, 110+(index*20), 350, 20);
            // Set action command (upon button click) to 'Option #' to standardize across all questions
            options[index].setActionCommand("Option " + index);
			window.add(options[index]);
			optionGroup.add(options[index]);            
		}
		
		// Score
		scoreLabel = new JLabel("SCORE"); 
		scoreLabel.setBounds(40, 230, 100, 20);
		window.add(scoreLabel);

        totalScore = new JLabel("" + clientScore);
        totalScore.setBounds(50, 250, 100, 20);
        window.add(totalScore);

        // Poll
		poll = new JButton("Poll"); 
		poll.setBounds(10, 300, 100, 20);
		poll.addActionListener(this); 
		window.add(poll);
		
        // Submit
		submit = new JButton("Submit"); 
		submit.setBounds(200, 300, 100, 20);
		submit.addActionListener(this); 
		window.add(submit);

        // Timer
		timer = new JLabel("TIMER");  
		timer.setBounds(250, 250, 100, 20);
		clock = new TimerCode(30); 
		t = new Timer(); 
		window.add(timer);

		window.setSize(400,400);
		window.setBounds(50, 50, 400, 400);
		window.setLayout(null);
		window.setVisible(true);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setResizable(false);

    }

    // Getter for ack
    public boolean getAck(){
        return ack;
    }

    // Setter for ack
    public void setAck(boolean ack){
        this.ack = ack;
    }

    // Called whenever a user selects a button
	@Override
	public void actionPerformed(ActionEvent e) {
		
        System.out.println("You clicked " + e.getActionCommand());
		
		// input refers to the radio button you selected or button you clicked
		String input = e.getActionCommand();  

		switch(input){
			case "Option 0":	
                // Set current selection
                currentSelection = answerChoices[0];
                System.out.println("Current selection: " + currentSelection);
                // Enable submit button
                submit.setEnabled(true);
                break;
			case "Option 1":	
                // Set current selection
                currentSelection = answerChoices[1];
                System.out.println("Current selection: " + currentSelection);
                // Enable submit button
                submit.setEnabled(true);
                break;
			case "Option 2":	
                // Set current selection
                currentSelection = answerChoices[2];
                System.out.println("Current selection: " + currentSelection);
                // Enable submit button
                submit.setEnabled(true);
                break;
			case "Option 3":	
                // Set current selection
                currentSelection = answerChoices[3];
                System.out.println("Current selection: " + currentSelection);
                // Enable submit button
                submit.setEnabled(true);
                break;
			case "Poll":	            
                // Send poll message 'buzz' to server
                try {
                    String buzz = "buzz";
                    outStream.write(buzz.getBytes("UTF-8"));
                    System.out.println("Sent 'buzz' to server");
                } catch (IOException i) {
                    i.printStackTrace();
                } 
                break;
			case "Submit":

                submitted = true;

                // Send user's answer to server
                try {
                    String selectionMessage = "ANSWER " + currentSelection;
                    outStream.write(selectionMessage.getBytes("UTF-8"));
                    System.out.println("Sent \"" + selectionMessage + "\" to server");
                } catch (IOException i) {
                    i.printStackTrace();
                } 

                // Disable options & submit button for the rest of the turn
                for(int option = 0; option < options.length; option++)
                    options[option].setEnabled(false);
                submit.setEnabled(false);
                
                break;
		}
	}


    // Runs the timer 
	public class TimerCode extends TimerTask {
		
        private int duration; 
		private static boolean secondPhase = false;

        public int getDuration(){
            return duration;
        }

		public TimerCode(int duration) {
			this.duration = duration;
		}

		@Override
		public void run() {

			// First Phase (Polling Phase)
			if(duration > 0 && !secondPhase) {
				// Disable options
				for(int option = 0; option < options.length; option++)
					options[option].setEnabled(false);
                    
				submit.setEnabled(false);
				poll.setEnabled(true);
            // Transition to the Second Phase (Answering Phase)
			} else if(duration <= 0 && !secondPhase) {
				timer.setText("Timer expired");
				window.repaint();

				secondPhase = true;
				duration = 10;
			}

			// Second Phase (Answering Phase)
			if(duration > 0 && secondPhase) {
                // If you haven't already submitted an answer:
                if(!submitted){
                    // Enable answer buttons & submit upon phase 2 only if you buzzed in first
                    if(getAck()){
                        for(int i = 0; i < options.length; i++){
                            options[i].setEnabled(true);
                        }
                        // submit.setEnabled(true);
                    }
                }
                
				// Disable poll button
				poll.setEnabled(false);

            // Move on to next question
			} else if(duration <= 0 && secondPhase) {
                // Check if client polled but didn't submit an answer
                if(!submitted && ACKpolled){
                    // Deduct 20 point 
                    clientScore -= 20;
                    // Send score update msg to server
                    totalScore.setText("" + clientScore);
                    String noAnswer = "SCORE -" + 20;
                    try {
                        outStream.write(noAnswer.getBytes("UTF-8"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                totalScore.setText("" + clientScore);

				timer.setText("Timer expired");
				window.repaint();
                
                // Wait for 3 seconds before resetting
                try {
                    Thread.sleep(3000); // 3000 milliseconds = 3 seconds
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

				// Reset to the first phase for the next question
				duration = 15; 
				secondPhase = false;

                // Reset ack
                setAck(false);

                // Clear selections from any buttons
                optionGroup.clearSelection();
    

                // Let server know to move onto next question
                try {
                    String nextQuestion = "NEXT";
                    outStream.write(nextQuestion.getBytes("UTF-8"));
                    System.out.println("Sent \"" + nextQuestion + "\" to server");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

			}

			// Set the timer color
			if(duration < 6)
				timer.setForeground(Color.red);
			else
				timer.setForeground(Color.black);

			// Display the duration and decrement
			timer.setText(duration + "");
			window.repaint();
			duration--;  // Decrement duration here, ensuring it always counts down
		}
	}

    public static void main(String[] args) throws Exception {
        TCPClient myChatClient = new TCPClient();
        myChatClient.createSocket();
    }

}