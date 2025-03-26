import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.TimerTask;
import java.util.Timer;
import javax.swing.*;

public class ClientWindow implements ActionListener
{
	private JButton poll;
	private JButton submit;
	private JRadioButton options[];
	private ButtonGroup optionGroup;
	private JLabel questionLabel;
	private JLabel timer;
	private JLabel score;
	private TimerTask clock;
	private String[] questions;
	private String[] answers;
	private int currentQuestion = 0;
	private int currentAnswers = 3;
	private DatagramSocket socket;
	private byte[] incomingData;
	private byte[] userData;
	private DatagramPacket sentPacket;
	private String userAnswer;

	private JFrame window;

	public ClientWindow()
	{
		/*
		 * Here we are making the destination socket
		 * This will be used later to send information back and forth from the game server and the user playing the game
		 */
		try{
			socket = new DatagramSocket();
			
			this.setDestIP(InetAddress.getByName("10.111.134.82"));
			this.setDestPort(9876);

			incomingData = new byte[1024];
            userAnswer = "";

		} catch (Exception e) {
			e.printStackTrace();
		}

		JOptionPane.showMessageDialog(window, "This is a trivia game");
		
		// get all 20 questions from the config file
		questions = readQuestions("C:\\Users\\evanv\\OneDrive\\Computer_Science\\SophomoreYear\\CSC340\\Hobo_Project2\\Questions.txt");
		answers = readAnswers("C:\\Users\\evanv\\OneDrive\\Computer_Science\\SophomoreYear\\CSC340\\Hobo_Project2\\Answers.txt");

		window = new JFrame("Trivia");
		questionLabel = new JLabel(questions[currentQuestion]); // represents the question
		window.add(questionLabel);
		questionLabel.setBounds(10, 5, 350, 100);;
		
		options = new JRadioButton[4];
		optionGroup = new ButtonGroup();
		for(int index = 0; index < options.length; index++)
		{
			options[index] = new JRadioButton(answers[index]);  // represents an option
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
			case "Option 1":	// Your code here
				userAnswer = "Option 1";
				setFalse(0);
								break;
			case "Option 2":	// Your code here
				userAnswer = "Option 2";	
				setFalse(1);
								break;
			case "Option 3":	// Your code here
				userAnswer = "Option 3";	
				setFalse(2);
								break;
			case "Option 4":	// Your code here
				userAnswer = "Option 4";	
				setFalse(3);
								break;
			case "Poll":		// Your code here
				/*
				 * This is where the message to the server will be sent
				 * It can be sent all the time and I want it to send the current time as a way to determine who was the first to press the button 
				 */
				userAnswer = "FIRST";
				sendData(userAnswer);
								break;
			case "Submit":		// Your code here
				/*
				* This is where the answer will be sent to the sever if you are the one to be first in the polling
				*/
				sendData(userAnswer);
								break;
			default:
								System.out.println("Incorrect Option");
		}
	}

	public void sendData(String userAnswer){
		userData = userAnswer.getBytes();
		sentPacket = new DatagramPacket(userData, userData.length, getDestIP(), getDestPort());
		socket.send(sentPacket);
	}
	
	// this function sets the question the user picks as false so they cannot reselect the same answer while it is already selected
	public void setFalse(int num){
		for(int i = 0; i < options.length; i++){
			if(num == i)
				options[i].setEnabled(false);
			else 
				options[i].setEnabled(true);
		}
	}

	public static String[] readQuestions(String filePath){
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

	public static String[] readAnswers(String filePath){
		String[] lines = new String [80];

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
	
	private void incrementQuestionsAndAnswers(){
		currentQuestion++;
		questionLabel.setText(questions[currentQuestion]);

		for(int i = 0; i < options.length; i++){
			currentAnswers++;
			options[i].setText(answers[currentAnswers]);
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
				incrementQuestionsAndAnswers();

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
	
}