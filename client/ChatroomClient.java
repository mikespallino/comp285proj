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
	
	private boolean clicked = false;
	private Channel channel;
	private JList<String> userList;
	private DefaultListModel<String> list = new DefaultListModel<String>();
	ArrayList<P2PClient> p2pClients = new ArrayList<>();
	
	private static String host;
	private static String ip;
	private static Integer port;
	private static volatile boolean ready = false;
	
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
    			if(ready)
    				break;
    		}
    		
    		if(host != null && port != null) {
    			new ChatroomClient(host, port).setUp();
    		} else {
    			System.err.println("Specify a valid host and port.");
    		}
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    private static void mainWindow() {    	
    	JFrame frame = new JFrame("MAD Chat - Client");
    	JLabel hostNameLabel = new JLabel("Host: ");
    	JTextField hostName = new JTextField(10);
    	JLabel portNumberLabel = new JLabel("Port: ");
    	JTextField portNumber = new JTextField(5);
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
		
    	frame.setSize(200,100);
    	frame.setLocationRelativeTo(null);
    	frame.pack();
    	frame.setResizable(false);
    	frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    	frame.setVisible(true);
    }
    
    /**
     * setUp()
     * Sets up the connection
     * Writes data to the server
     * Pulls data from the server
     * @author Mike
     */
    public void setUp() throws Exception {
    	setUp(true);
    }
    
    public void setUp(boolean wait) throws Exception {
    	setupThread.start();
    	if (wait) {
    		finishSetup();
    	}
    }
    
    public void finishSetup() throws InterruptedException {
    	setupThread.join();
    }
    
	private final Thread setupThread = new Thread() {
		public void run() {
			EventLoopGroup workerGroup = new NioEventLoopGroup();

			try {
				Bootstrap b = new Bootstrap();
				final ClientHandler handler = new ClientHandler();
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

				// Start the client.
				channel = b.connect(host, port).sync().channel();
				boolean firstRun = true;
				while (true) {
					if (firstRun) {
						ip = (channel.localAddress().toString());
						output.append("Welcome to MAD Chat!\nServer: " + handler.getServer().remoteAddress() + "\n");
						firstRun = false;
					}
					// Forces the scroll pane to actually scroll to the bottom
					// when new data is put in
					output.setCaretPosition(output.getDocument().getLength());
					if (handler.getMessage() != null && !handler.getMessage().equals("")) {
						// send message as usual
						if (handler.getMessage().indexOf("[P2P]") == -1) {
							if(handler.getMessage().indexOf("UPDATE LIST") != -1) {
								updateList(ClientHandler.getUsers());
							} else {
								output.append(handler.getMessage() + "\n");
								System.out.println("setUp (loop):: message: "+ handler.getMessage());
							}
							handler.resetMessage();
						} else {
							// send message to P2P window
							boolean newP2P = true;
							System.out.println(handler.getMessage());
							String peerAddress = handler.getMessage().substring(13,handler.getMessage().indexOf("]",13));
							System.out.println("setUp (loop):: peerAddress: "+ peerAddress);
							String msg = handler.getMessage().substring(handler.getMessage().indexOf(":",handler.getMessage().indexOf("TO")) + 1);
							for (int i = 0; i < p2pClients.size(); i++) {
								if (peerAddress.equals(p2pClients.get(i).getHost())) {
									newP2P = false;
									p2pClients.get(i).append("[PEER] : " + msg);
								}
							}
							if (newP2P) {
								System.out.println("setUp (loop):: Starting client.");
								p2pClients.add(new P2PClient(peerAddress, getParent()));
								p2pClients.get(p2pClients.size() - 1).append("[PEER] : " + msg);
								newP2P = false;
							}
							handler.resetMessage();
						}
					}

					// update list
					updateList(ClientHandler.getUsers());
					
					for(int i = 0; i < p2pClients.size(); i++) {
						if(!p2pClients.get(i).frame.isVisible()) {
							p2pClients.remove(i);
						}
					}

					// send data
					if (clicked) {
						if (message.getText().equalsIgnoreCase("/bye")) {
							workerGroup.shutdownGracefully();
							message.setText("");
							System.exit(0);
						} else if (!message.getText().equals("")) {
							System.out.println(1);
							sendMessage(message.getText());
							message.setText("");
							clicked = false;
						}
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				workerGroup.shutdownGracefully();
			}
		}
	};

	private ChatroomClient getParent() {
		return this;
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
				JFrame userFrame = new JFrame("User List");
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
				clicked = true;
			}
			
		});
		
		message.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(arg0.getActionCommand().equals("Enter")) {
					clicked = true;
				}
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
	 * @return - Channel object
	 * @author Mike
	 */
	public Channel getChannel() {
		return channel;
	}
	
	public String getConnection() {
		return channel.localAddress().toString();
	}

	public static String getIp() {
		return ip;
	}
	
	public static String[] getUsers() {
		return ClientHandler.getUsers();
	}
	
	public void destroy() {
		frame = null;
		userList = null;
		output = null;
		message = null;
		sendButton = null;
		areaScrollPane = null;
	}
	
}