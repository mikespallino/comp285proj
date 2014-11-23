package client;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * P2PClient implementation
 * Sets up connection to a P2PServer.
 * @author Mike
 */
public class P2PClient extends Client {
	
	ChatroomClient chatroom;
	
	public P2PClient(String peerAddress, ChatroomClient c) {
		super(peerAddress, c);
		chatroom = c;
		createGUI();
		System.out.println("P2PClient:: created GUI");
	}
	
	@Override
	protected void createGUI() {
		frame = new JFrame("MAD P2P Chat: " + host);
		output = new JTextArea(20,20);
		message = new JTextField(40);
		sendButton = new JButton("Send");
		areaScrollPane = new JScrollPane(output);
		areaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		areaScrollPane.setPreferredSize(new Dimension(600, 400));
		
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		JPanel panel = new JPanel();		
		GroupLayout layout = new GroupLayout(panel);
		panel.setLayout(layout);
		layout.setAutoCreateContainerGaps(true);
		
		layout.setHorizontalGroup(
				layout.createSequentialGroup()
					.addGroup(
						layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
							.addComponent(areaScrollPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addGroup(
									layout.createSequentialGroup()
										.addComponent(message, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
										.addGap(10)
										.addComponent(sendButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))));
		layout.setVerticalGroup(
				layout.createSequentialGroup()
					.addGroup(
						layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
							.addComponent(areaScrollPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
					.addGap(10)
					.addGroup(
						layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
							.addComponent(message, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(sendButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
		
		message.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(arg0.getActionCommand().equals("Enter") && message.getText() != null && !message.getText().equals("")) {
					chatroom.sendMessage(message.getText());
					message.setText("");
				}
			}
		});
		
		sendButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(message.getText() != null && !message.getText().equals("")) {
					chatroom.sendMessage(message.getText());
					message.setText("");
				}
			}
		});
		
		frame.add(panel);
		frame.setLocationRelativeTo(null);
		frame.pack();
		frame.setVisible(true);
	}

}
