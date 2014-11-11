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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * ChatroomClient implementation.
 * Sets up connections to the main chat room.
 * @author Mike
 */
public class ChatroomClient extends Client {

	private boolean clicked = false;
	
	private Channel channel;
	
	public ChatroomClient(String host, int port) {
		super(host, port);
		createGUI();
	}
	
    public static void main(String[] args) {
    	try {
    		new ChatroomClient("127.0.0.1", 8080).setUp();
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
    @Override
    public void setUp() throws Exception {
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
	            	output.append(handler.getMessage());
	            	output.append("\n");
	            	handler.resetMessage();
            	}
            	userList.setListData(ClientHandler.getUsers());
            	if(clicked) {
            		if(message.getText().equalsIgnoreCase("/bye")) {
			            workerGroup.shutdownGracefully();
			            message.setText("");
			            System.exit(0);
            		} else if(!message.getText().equals("")) {
						channel.writeAndFlush(message.getText() + "\n");
						message.setText("");
						clicked = false;
					}
				}
            }
        } finally {
            workerGroup.shutdownGracefully();
        }
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
		JScrollPane areaScrollPane = new JScrollPane(output);
		areaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		areaScrollPane.setPreferredSize(new Dimension(500, 400));
		message = new JTextField(20);
		message.setActionCommand("Enter");
		sendButton = new JButton("Send");
		userList = new JList<String>();
		String[] userListData = {"test", "test1", "test3"};
		userList.setListData(userListData);
		
		frame = new JFrame("MAD Chat");
		JPanel panel = new JPanel();
		
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
							.addComponent(message, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
					.addGap(10)
					.addGroup(
						layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
							.addComponent(userList, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(sendButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
		
		layout.setVerticalGroup(
				layout.createSequentialGroup()
					.addGroup(
						layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
							.addComponent(areaScrollPane, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(userList, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
					.addGap(10)
					.addGroup(
						layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
							.addComponent(message, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
							.addComponent(sendButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
		
		frame.add(panel);
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.pack();
		frame.setVisible(true);
	}
}
