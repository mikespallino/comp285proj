package server;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * Base class for all Servers.
 * @author Mike
 */
public abstract class Server {

	/**
	 * Swing components for the GUI.
	 * @author Mike
	 */
	protected int port;
	protected JFrame frame;
	protected JTextArea output;
	protected JTextField message;
	protected JButton sendButton;
	protected JList<String> userList;
	
	/**
	 * Base Server constructor.
	 * @author Mike
	 */
	public Server(int port) {
		this.port = port;
		createGUI();
	}
	
	/**
	 * Subclasses must implement run().
	 * @author Mike
	 */
	public abstract void run() throws Exception;
	
	/**
	 * Subclasses must implement createGUI().
	 * @author Mike
	 */
	public abstract void createGUI();
	
}
