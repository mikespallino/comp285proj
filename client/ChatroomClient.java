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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
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
	
	public ChatroomClient(String host, int port) {
		super(host, port);
		createGUI();
	}
	
    public static void main(String[] args) {
    	try {
    		new ChatroomClient("10.33.11.51", 8080).setUp();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    /**
     * setUp()
     * Sets up the connection
     * Writes data to the server
     * Pulls data from the server
     * @author Mike
     */
    private void setUp() throws Exception {
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
                	//max size 8192, all input delimited by line endings
                	ch.pipeline().addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
                	ch.pipeline().addLast("Decoder", new StringDecoder());
                	ch.pipeline().addLast("Encoder", new StringEncoder());
                	
                    ch.pipeline().addLast(handler);
                }
            });

            // Start the client.
            channel = b.connect(host, port).sync().channel();
            boolean firstRun = true;
            while(true) {
            	if(firstRun) {
            		output.append("Welcome to MAD Chat!\nServer: " + handler.getServer().remoteAddress() + "\n");
            		firstRun = false;
            	}
            	 //Forces the scroll pane to actually scroll to the bottom when new data is put in
            	output.setCaretPosition(output.getDocument().getLength());
            	if(handler.getMessage() != null && !handler.getMessage().equals("")) {
            		//send message as usual
            		if(handler.getMessage().indexOf("[P2P]") == -1) {
            			output.append(handler.getMessage() + "\n");
            			System.out.println("setUp (loop):: message: " + handler.getMessage());
    	            	handler.resetMessage();
            		} else {
            			//test
            			if(handler.getMessage().indexOf("[P2P] [you]") != -1) {
                			System.out.println("Error.");
                		}
            			//send message to P2P window
            			boolean newP2P = true;
            			System.out.println(handler.getMessage());
            			String peerAddress = handler.getMessage().substring(13, handler.getMessage().indexOf("]", 12));
            			System.out.println("setUp (loop):: peerAddress: " + peerAddress);
            			String msg = handler.getMessage().substring(handler.getMessage().indexOf(":", handler.getMessage().indexOf("TO"))+1);
            			for(int i = 0; i < p2pClients.size(); i++) {
            				if(peerAddress.equals(p2pClients.get(i).getHost())) {
            					newP2P = false;
            					p2pClients.get(i).append("[PEER] : " + msg);
            				}
            			}
            			if(newP2P) { 
            				p2pClients.add(new P2PClient(peerAddress, this));
            				p2pClients.get(p2pClients.size() - 1).append(handler.getMessage());
            				newP2P = false;
            			}
            			handler.resetMessage();
            		}
            	}
            	
            	//update list
            	updateList(ClientHandler.getUsers());
            	
            	//send data
            	if(clicked) {
            		if(message.getText().equalsIgnoreCase("/bye")) {
			            workerGroup.shutdownGracefully();
			            message.setText("");
			            System.exit(0);
            		} else if(!message.getText().equals("")) {
            			System.out.println(1);
            			sendMessage(message.getText());
						message.setText("");
						clicked = false;
					}
				}
            }
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

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
		message = new JTextField(20);
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
							System.out.println("actionPerformed (list listener):: " + userList.getSelectedValue());
							String peerAddress = userList.getSelectedValue();
							new P2PClient(peerAddress, ChatroomClient.this);
							userFrame.dispose();
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
	
	private void updateList(String[] users) {
		if(users.length != list.size()) {
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
	
	public Channel getChannel() {
		return channel;
	}
}
