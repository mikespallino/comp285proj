package client;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import util.MessageEvent;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Base class for all Clients.
 * @author Mike
 */
public abstract class Client {

	/**
	 * Swing components for the GUI.
	 * @author Mike
	 */
	protected JFrame frame;
	protected JTextArea output;
	protected JScrollPane areaScrollPane;
	protected JScrollPane userListScrollPane;
	protected JTextField message;
	protected JButton sendButton;
	
	protected String host;
	protected int port;
	
	protected EventBus incomingMessageEventBus;
	
	/**
	 * Base Client constructor.
	 * @author Mike
	 */
	public Client(String host, int port) {
		this.host = host;
		this.port = port;
		incomingMessageEventBus = new EventBus();
	}
	
	/**
	 * P2P Client constructor.
	 * @param host - String host name (ip address)
	 * @param c - reference to the main chatroom client that was started.
	 * @author Mike
	 */
	public Client(String host, ChatroomClient c) {
		this.host = host;
	}
	
	/**
	 * Subclasses must implement createGUI().
	 * Chatroom and P2P will have different implementations so this
	 * can't be implemented here.
	 * @author Mike
	 */
	protected abstract void createGUI();
	
	/**
	 * Generic method to append text to the JTextArea.
	 * @author Mike
	 */
	protected void append(String msg) {
		output.append(msg + "\n");
	}
	
	/**
	 * Generic method to get the host name.
	 * @return host
	 * @author Mike
	 */
	public String getHost() {
		return host;
	}
	
	/**
	 * Generic method to get the output text area.
	 * @return output
	 * @author Mike
	 */
	public JTextArea getOutput() {
		return output;
	}
	
	@Subscribe
	public void handleMessageEvent(MessageEvent e) {
		if(!e.getMessage().equals("")) {
			this.append(e.getMessage());
		}
	}
	
}
