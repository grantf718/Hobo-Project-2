import java.awt.Color;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.SecureRandom;
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
    private InputStream inStream = null;
    private OutputStream outStream = null;

    private String clientUsername;

    // GUI vars
    private JButton poll;
	private JButton submit;
	private JRadioButton options[];
	private ButtonGroup optionGroup;
	private JLabel questionLabel;
	private JLabel timer;
	private JLabel score;
	private TimerTask clock;
	private JFrame window;

    private String currentQuestion;
    private String[] answerChoices = new String[4];
    private String correctAnswer;

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

        // Create GUI
        createGUI();
    }

    // Create client socket
    public void createSocket() {
        try {
        	// Fetch streams
            inStream = socket.getInputStream();
            outStream = socket.getOutputStream();

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

                            // Go line by line to separate message into Qs and As
                            while ((receivedLine = reader.readLine()) != null) {

                                System.out.println("Line: " + receivedLine);

                                // Handle correct answer
                                if(receivedLine.startsWith("ack")){

                                // Handle incorrect answer
                                } else if (receivedLine.startsWith("negative-ack")){

                                // Set incoming question & display it on GUI 
                                } else if(receivedLine.startsWith("QUESTION ")){
                                    
                                    currentQuestion = receivedLine.substring(9); // Remove QUESTION tag

                                    // DEBUG: Print current question
                                    System.out.println("Current question set to: " + currentQuestion);

                                    // Update GUI with new information 
                                    // questionLabel.setText(currentQuestion);
                                    // window.repaint();

                                    // Update question label on the Event Dispatch Thread
                                    SwingUtilities.invokeLater(() -> {
                                        questionLabel.setText(currentQuestion);
                                        window.repaint();
                                    });


                                // Set incoming answers & display them on GUI
                                } else if (receivedLine.startsWith("ANSWERS ")){

                                    // Set correct answer
                                    correctAnswer = receivedLine.substring(8, receivedLine.indexOf(','));

                                    // Set answer choices
                                    String parts[] = receivedLine.substring(8).split(",");

                                    for (int i = 0; i < 4; i++) {
                                        answerChoices[i] = parts[i];
                                    }

                                    // DEBUG: Print out correct answer and answer choices
                                    System.out.println("Correct answer set to: " + correctAnswer);
                                    System.out.println("Answer choice 1 set to: " + answerChoices[0]);
                                    System.out.println("Answer choice 2 set to: " + answerChoices[1]);
                                    System.out.println("Answer choice 3 set to: " + answerChoices[2]);
                                    System.out.println("Answer choice 4 set to: " + answerChoices[3]);

                                    // Update GUI buttons accordingly on the Event Dispatch Thread
                                    SwingUtilities.invokeLater(() -> {
                                        for (int i = 0; i < options.length; i++) {
                                            options[i].setText(answerChoices[i]);
                                        }
                                        window.repaint();
                                    });

                                    // window.repaint();
                                    
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

    // Create GUI
    public void createGUI(){
        // Window is titled with the player's username
        window = new JFrame(clientUsername); 
        // Question
		questionLabel = new JLabel(currentQuestion); 
		window.add(questionLabel);
		questionLabel.setBounds(10, 5, 350, 100);;
		
        // Answer choice buttons
		options = new JRadioButton[4];
		optionGroup = new ButtonGroup();
		for(int index = 0; index < options.length; index++)
		{
			options[index] = new JRadioButton(answerChoices[index]);  // represents an option
			// if a radio button is clicked, the event would be thrown to this class to handle
			options[index].addActionListener(this);
			options[index].setBounds(10, 110+(index*20), 350, 20);
			window.add(options[index]);
			optionGroup.add(options[index]);
		}

		timer = new JLabel("TIMER");  // represents the countdown shown on the window
		timer.setBounds(250, 250, 100, 20);
		clock = new TimerCode(15);  // represents clocked task that should run after X seconds
		Timer t = new Timer();  // event generator
		t.schedule(clock, 0, 1000); // clock is called every second
		window.add(timer);
		
		
		score = new JLabel("SCORE"); // represents the score
		score.setBounds(50, 250, 100, 20);
		window.add(score);

		poll = new JButton("Poll");  // button that use clicks/ like a buzzer
		poll.setBounds(10, 300, 100, 20);
		poll.addActionListener(this);  // calls actionPerformed of this class
		window.add(poll);
		
		submit = new JButton("Submit");  // button to submit their answer
		submit.setBounds(200, 300, 100, 20);
		submit.addActionListener(this);  // calls actionPerformed of this class
		window.add(submit);
		
		window.setSize(400,400);
		window.setBounds(50, 50, 400, 400);
		window.setLayout(null);
		window.setVisible(true);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setResizable(false);

    }

    	// this method is called when you check/uncheck any radio button
	// this method is called when you press either of the buttons- submit/poll
	@Override
	public void actionPerformed(ActionEvent e)
	{
		System.out.println("You clicked " + e.getActionCommand());
		
		// input refers to the radio button you selected or button you clicked
		String input = e.getActionCommand();  
		switch(input)
		{
			case "Option 1":	
                // Your code here
                break;
			case "Option 2":	
                // Your code here
                break;
			case "Option 3":	
                // Your code here
                break;
			case "Option 4":	
                // Your code here
                break;
			case "Poll":		
                // Your code here
                break;
			case "Submit":		
                // Your code here
                break;
			default:
                System.out.println("Incorrect Option");
		}
	}


    // this class is responsible for running the timer on the window
	public class TimerCode extends TimerTask
	{
		private int duration;  // write setters and getters as you need
		private boolean secondPhase = false;

		public TimerCode(int duration)
		{
			this.duration = duration;
		}

		@Override
		public void run()
		{
			// First Phase (Polling Phase)
			if(duration > 0 && !secondPhase) 
			{
				// Disable options initially
				for(int option = 0; option < options.length; option++)
					options[option].setEnabled(false);

				submit.setEnabled(false);
				poll.setEnabled(true);
			} 
			else if(duration <= 0 && !secondPhase)
			{
				timer.setText("Timer expired");
				window.repaint();

				// Transition to the Second Phase (Answering Phase)
				secondPhase = true;
				duration = 10;
			}

			// Second Phase (Answering Phase)

			if(duration > 0 && secondPhase) 
			{
				// Enable options and submit button
				submit.setEnabled(true);

				for(int option = 0; option < options.length; option++)
					options[option].setEnabled(true);

				poll.setEnabled(false);
			}  
			else if(duration <= 0 && secondPhase)
			{
				timer.setText("Timer expired");
				window.repaint();

				// Reset to the first phase for the next question
				duration = 15;
				secondPhase = false;
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
