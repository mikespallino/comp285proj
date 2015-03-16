package client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * ChatroomClient implementation.
 * Sets up connections to the main chat room.
 * @author Mike
 */
public class ChatroomClient extends Client {
	
	private EventLoopGroup workerGroup;
	private Channel channel;
	private JList<String> userList;
	private DefaultListModel<String> list = new DefaultListModel<String>();
	private ArrayList<P2PClient> p2pClients = new ArrayList<>();
	
	private static String host;
	private static Integer port;
	private static boolean ready = false;
	
	public ChatroomClient(String host, int port) throws Exception {
		super(host, port);
		ChatroomClient.host = host;
		ChatroomClient.port = port;
		createGUI();
	}
	
    public static void main(String[] args) {
    	mainWindow();
    	try {
    		while(true) {
    			Thread.sleep(1000);
    			if(ready) {
    				break;
    			}
    		}
    		if(host != null && port != null) {
    			ChatroomClient c = new ChatroomClient(host, port);
    			c.setUp();
    		} else {
    			System.err.println("Specify a valid host and port.");
    		}
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    private boolean update(String[] users) throws InterruptedException {
    	Thread.sleep(1000);
		if(!channel.isOpen()) {
			return false;
		}
		for(int i = 0; i < p2pClients.size(); i++) {
			if(p2pClients.get(i) != null) {
				if(!p2pClients.get(i).frame.isVisible()) {
					p2pClients.remove(i);
				}
			}
		}
		output.setCaretPosition(output.getDocument().getLength());
		updateList(users);
		return true;
	}

	/**
     * mainWindow()
     * Creates the initial window that allows the user to choose a host IP and port number.
     * @author Mike
     */
    private static void mainWindow() {    	
    	final JFrame frame = new JFrame("MAD Chat - Client");
    	JLabel hostNameLabel = new JLabel("Host: ");
    	final JTextField hostName = new JTextField(10);
    	JLabel portNumberLabel = new JLabel("Port: ");
    	final JTextField portNumber = new JTextField(5);
    	JButton connect = new JButton("Connect");
    	connect.setActionCommand("clicked");
    	
    	JPanel pane = new JPanel();
    	GroupLayout layout = new GroupLayout(pane);
    	layout.setHorizontalGroup(
				layout.createSequentialGroup()
					.addGroup(
						layout.createParallelGroup()
							.addComponent(hostNameLabel)
							.addComponent(hostName))
					.addGroup(
						layout.createParallelGroup()
							.addComponent(portNumberLabel)
							.addComponent(portNumber)));
		
		layout.setVerticalGroup(
				layout.createSequentialGroup()
					.addGroup(
						layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
							.addComponent(hostNameLabel)
							.addComponent(hostName))
					.addGroup(
						layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
							.addComponent(portNumberLabel)
							.addComponent(portNumber)));
    	
		frame.setLayout(new FlowLayout());
		frame.add(pane);
		frame.add(connect);
		
		connect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(e.getActionCommand().equalsIgnoreCase("clicked")) {
					host = hostName.getText();
					port = new Integer(portNumber.getText());
					ready = true;
					frame.dispose();
				}
			}
		});
		
		portNumber.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent arg0) {
				if(arg0.getKeyCode() == KeyEvent.VK_ENTER) {
					host = hostName.getText();
					port = new Integer(portNumber.getText());
					ready = true;
					frame.dispose();
				}
			}
			@Override
			public void keyReleased(KeyEvent arg0) {}
			@Override
			public void keyTyped(KeyEvent arg0) {}
		});
		
    	frame.setSize(200,100);
    	frame.setLocationRelativeTo(null);
    	frame.pack();
    	frame.setResizable(false);
    	frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    	frame.setVisible(true);
    }
    
    /**
     * setUp()
     * Creates a thread for the client.
     * Sets up the connection
     * Writes data to the server
     * Pulls data from the server
     * @author Mike
     */
	private void setUp() {
		workerGroup = new NioEventLoopGroup();
		try {
			Bootstrap b = new Bootstrap();
			final ClientHandler handler = new ClientHandler(this);
			b.group(workerGroup);
			b.channel(NioSocketChannel.class);
			b.option(ChannelOption.SO_KEEPALIVE, true);
			b.handler(new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) throws Exception {
					// max size 8192, all input delimited by line endings
					ch.pipeline().addLast("framer",new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
					ch.pipeline().addLast("Decoder", new StringDecoder());
					ch.pipeline().addLast("Encoder", new StringEncoder());
					ch.pipeline().addLast(handler);
				}
			});

			incomingMessageEventBus.register(this);
			// Start the client.
			channel = b.connect(host, port).sync().channel();
			output.append("Welcome to MAD Chat!\nServer: " + handler.getServer().remoteAddress() + "\n");
			while(update(ClientHandler.getUsers())) { }
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  finally {
			workerGroup.shutdownGracefully();
		}
	}
	
    /**
     * Sends a message over the server.
     * Primarily for use in for when a P2P Client is open.
     * @param message - String to send to the server.
     * @author Mike
     */
    public void sendMessage(String message) {
    	if(message.equals("")) {
    		System.out.println("Error: Trying to write the empty string.");
    	} else if(message.equals("/bye")){
    		workerGroup.shutdownGracefully();
    		System.exit(0);
    	} else {
    		channel.writeAndFlush(message + "\r\n");
    	}
    	System.out.println("sendMessage:: " + message);
    }
    
    /**
     * createGUI()
     * Builds the GUI using a GroupLayout layout manager.
     * For more information on GroupLayout: http://docs.oracle.com/javase/tutorial/uiswing/layout/group.html
     * @author Mike
     */
	@Override
	public void createGUI() {
		output = new JTextArea(20, 50);
		output.setEditable(false);
		output.setLineWrap(true);
		output.setWrapStyleWord(true);
		areaScrollPane = new JScrollPane(output);
		areaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		areaScrollPane.setPreferredSize(new Dimension(600, 400));
		message = new JTextField(40);
		message.setActionCommand("Enter");
		sendButton = new JButton("Send");
		userList = new JList<String>(list);
		userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		userList.setLayoutOrientation(JList.VERTICAL);
		userListScrollPane = new JScrollPane(userList);
		userListScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		userListScrollPane.setPreferredSize(new Dimension(200, 400));
		
		frame = new JFrame("MAD Chat");
		JPanel panel = new JPanel();
		
		JMenuBar menuBar = new JMenuBar();
		JMenu show = new JMenu("File");
		JMenuItem showList = new JMenuItem("Show User List");
		show.add(showList);
		menuBar.add(show);
		frame.setJMenuBar(menuBar);
		
		showList.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
            	updateList(ClientHandler.getUsers());
				final JFrame userFrame = new JFrame("User List");
				userFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				userFrame.add(userListScrollPane);
				userFrame.pack();
				userFrame.setLocationRelativeTo(frame);
				userFrame.setVisible(true);
				userList.addListSelectionListener(new ListSelectionListener() {
					@Override
					public void valueChanged(ListSelectionEvent arg0) {
						if(arg0.getValueIsAdjusting() == false) {
							boolean createNew = true;
							System.out.println("actionPerformed (list listener):: " + userList.getSelectedValue());
							String peerAddress = userList.getSelectedValue();
							if(userList.getSelectedValue() != null && userList.getSelectedValue().indexOf("(") != -1) {
								peerAddress = userList.getSelectedValue().substring(0, userList.getSelectedValue().indexOf("(")-1);
							}
							if(peerAddress != null) {
								for(int i = 0; i < p2pClients.size(); i++) {
									if(p2pClients.get(i).getHost().equals(peerAddress)) {
										createNew = false;
									}
								}
								if(createNew) {
									p2pClients.add(new P2PClient(peerAddress, ChatroomClient.this));
								}
							}
							userFrame.dispose();
							userList.clearSelection();
						}
					}
				});
			}
		});
		
		/**
		 * Anonymous class for the button action listener.
		 * Writes the message text to the server on click events.
		 * @author Mike
		 */
		sendButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				sendMessage(message.getText());
				message.setText("");
			}
			
		});
		
		message.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				sendMessage(message.getText());
				message.setText("");
			}
		});
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(600, 400);
		
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
		
		frame.add(panel);
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	/**
	 * Update the list of users from the server.
	 * @param users - User List from the server.
	 * @author Mike
	 */
	public void updateList(String[] users) {
		boolean update = false;
		if(users.length == list.size()) {
			for(int i = 0; i < list.size(); i++) {
				if(!list.getElementAt(i).equals(users[i])) {
					update = true;
				}
			}
		} else {
			update = true;
		}
		if(update) {
			userList = null;
			userListScrollPane = null;
			list = new DefaultListModel<String>();
			for(int i = 0; i < users.length; i++) {
				System.out.println("updateList::User: " + users[i]);
				list.addElement(users[i]);
			}
			userList = new JList<String>(list);
			userList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
			userList.setLayoutOrientation(JList.VERTICAL);
			userListScrollPane = new JScrollPane(userList);
			userListScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			userListScrollPane.setPreferredSize(new Dimension(200, 400));
		}
	}
	
	/**
	 * getChannel()
	 * @return - Channel object.
	 * @author Mike
	 */
	public Channel getChannel() {
		return channel;
	}
	
	/**
	 * getConnection()
	 * @return - The local address of the channel.
	 * @author Mike
	 */
	public String getConnection() {
		return channel.localAddress().toString();
	}
	
	/**
	 * getUsers()
	 * @return - the ClientHandler's userList.
	 * @author Mike
	 */
	public static String[] getUsers() {
		return ClientHandler.getUsers();
	}
	
	/**
	 * getP2PClients()
	 * @return - The ArrayList of P2P clients.
	 * @author Mike
	 */
	public ArrayList<P2PClient> getP2PClients() {
		return p2pClients;
	}
	
}