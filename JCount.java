// JCount.java

/*
 Basic GUI/Threading exercise.
*/

import javax.swing.*;
import java.awt.event.*;

public class JCount extends JPanel {

	JTextField input;
	JButton start,stop;
	JLabel count;
	Worker thread;

	public JCount() {
		super();
		// Set the JCount to use Box layout
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		// YOUR CODE HERE

		input = new JTextField("100000000",10);
		start = new JButton("Start");
		stop = new JButton("Stop");
		count = new JLabel(" ");

		start.addActionListener(e -> {
			if(thread != null){
				thread.interrupt();
			}
			thread = new Worker(input.getText());
			thread.start();
		});

		stop.addActionListener(e -> {
			if(thread != null){
				thread.interrupt();
			}
		});

		add(input);
		add(count);
		add(start);
		add(stop);

	}

	class Worker extends Thread{

		private final int num;

		Worker(String num){
			this.num = Integer.parseInt(num);
		}

		@Override
		public void run(){

			for(int i=0; i<=num; i++){
				final String ct = "" + i;

				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					thread.interrupt();
				}

				if(this.isInterrupted() == true){
					return;
				}

				SwingUtilities.invokeLater(() -> count.setText(ct));

			}


		}

	}
	
	static public void main(String[] args)  {
		// Creates a frame with 4 JCounts in it.
		// (provided)
		JFrame frame = new JFrame("The Count");
		frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));

		frame.add(new JCount());
		frame.add(new JCount());
		frame.add(new JCount());
		frame.add(new JCount());

		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
	}
}

