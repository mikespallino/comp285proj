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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
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

	boolean clicked = false;
	Channel channel;
	String peerAddress = "";
	
	P2PClient(String host, int port) {
		super(host, port);
		createGUI();
	}
	
	public static void main(String[] args) {
		try {
			new P2PClient("127.0.0.1", 8080).setUp();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void createGUI() {
		frame = new JFrame("MAD P2P Chat: ");
		frame.setSize(600,400);
		output = new JTextArea(20, 50);
		output.setEditable(false);
		output.setLineWrap(true);
		output.setWrapStyleWord(true);
		areaScrollPane = new JScrollPane(output);
		areaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		areaScrollPane.setPreferredSize(new Dimension(500, 400));
		message = new JTextField(20);
		message.setActionCommand("Enter");
		sendButton = new JButton("Send");
		
		JPanel panel = new JPanel();
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
							.addComponent(sendButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
		
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
		
		frame.add(panel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

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
            		createList();
            		firstRun = false;
            	}
            	
            	 //Forces the scroll pane to actually scroll to the bottom when new data is put in
            	output.setCaretPosition(output.getDocument().getLength());
            	if(handler.getMessage() != null && !handler.getMessage().equals("")) {
	            	output.append(handler.getMessage());
	            	output.append("\n");
	            	handler.resetMessage();
            	}
            	
            	if(clicked) {
            		if(message.getText().equalsIgnoreCase("/bye")) {
			            workerGroup.shutdownGracefully();
			            message.setText("");
			            System.exit(0);
            		} else if(!message.getText().equals("") && !peerAddress.equals("")) {
						channel.writeAndFlush("[P2P] FROM : [" + channel.localAddress() + "] TO : [" + peerAddress + "] " + message.getText() + "\n");
						message.setText("");
						clicked = false;
					}
				}
            }
        } finally {
            workerGroup.shutdownGracefully();
        }
	}
	
	private void createList() {
		JFrame userFrame = new JFrame("MAD Chat - User List");
		userFrame.setSize(300,400);
		userFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		JList<String> users = new JList<String>();
		users.setListData(ClientHandler.getUsers());
		userFrame.setLayout(new BorderLayout());
		userFrame.add(users, BorderLayout.CENTER);
		userFrame.setLocationRelativeTo(frame);
		userFrame.setResizable(false);
		userFrame.setVisible(true);
		users.addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(KeyEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void keyReleased(KeyEvent arg0) {
				if(arg0.getKeyCode() == KeyEvent.VK_ENTER) {
					peerAddress = users.getSelectedValue();
					userFrame.setVisible(false);
				}
			}

			@Override
			public void keyTyped(KeyEvent arg0) {
			}
			
		});
	}

}
