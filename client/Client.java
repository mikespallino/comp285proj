package client;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

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
	
	/**
	 * Base Client constructor.
	 * @author Mike
	 */
	public Client(String host, int port) {
		this.host = host;
		this.port = port;
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
	
}
