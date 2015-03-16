package server;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import util.MessageEvent;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

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
	
	protected static EventBus messageBus;
	
	/**
	 * Base Server constructor.
	 * @author Mike
	 */
	public Server(int port) {
		this.port = port;
		messageBus = new EventBus();
		createGUI();
	}
	
	public abstract void setUp();
	
	/**
	 * Subclasses must implement createGUI().
	 * @author Mike
	 */
	public abstract void createGUI();
	
	
	@Subscribe
	public void handleMessageEvent(MessageEvent e) {
		output.append(e.getMessage() + "\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r\n\r");
	}
}
