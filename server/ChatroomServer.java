package server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * ChatroomServer implementation
 * Sets up the Server for the chat room.
 * @author Mike
 */
public class ChatroomServer extends Server {

	private boolean exit = false;
	private boolean clicked = false;
	
    public ChatroomServer(int port) {
		super(port);
	}
    
    public void run() throws Exception {
    	run(true);
    }
    
    public void run(boolean wait) throws Exception {
    	setupThread.start();
    	if (wait) {
    		finishSetup();
    	}
    }
    
    public void finishSetup() throws InterruptedException {
    	setupThread.join();
    }
    
    /**
     * run()
     * Sets up server
     * Writes data to the clients
     * Pulls data from the clients
     * @author Mike
     */
	private final Thread setupThread = new Thread() {
		public void run() {
			EventLoopGroup bossGroup = new NioEventLoopGroup();
			EventLoopGroup workerGroup = new NioEventLoopGroup();
			ServerChannelInitializer initializer = new ServerChannelInitializer();
			ArrayList<String> messages;
			try {
				ServerBootstrap b = new ServerBootstrap();
				b.group(bossGroup, workerGroup)
						.channel(NioServerSocketChannel.class)
						.childHandler(initializer)
						.option(ChannelOption.SO_BACKLOG, 128)
						.childOption(ChannelOption.SO_KEEPALIVE, true);

				// Bind and start to accept incoming connections.
				ChannelFuture f = b.bind(port).sync();

				while (true) {
					initializer.getMessage();
					messages = initializer.getMessages();
					String[] users = new String[ChatroomServerHandler
							.getChannels().size()];
					int k = 0;
					for (Channel c : ChatroomServerHandler.getChannels()) {
						if (k < users.length) {
							users[k] = c.remoteAddress().toString();
							k++;
						}
					}
					userList.setListData(users);

					// Forces the scroll pane to actually scroll to the bottom
					// when new data is put in
					output.setCaretPosition(output.getDocument().getLength());
					if (messages.size() > 0) {
						for (int i = 0; i < messages.size(); i++) {
							output.append(messages.get(i));
							output.append("\n");
						}
						initializer.resetMessages();
					}
					if (clicked) {
						if (message.getText().equalsIgnoreCase("/bye")) {
							workerGroup.shutdownGracefully();
							message.setText("");
							clicked = false;
							System.exit(0);
						} else if (!message.getText().equals("")) {
							output.append(message.getText() + "\r\n");
							for (Channel channel : ChatroomServerHandler
									.getChannels()) {
								channel.writeAndFlush("[SERVER] : "
										+ message.getText() + "\r\n");
							}
							message.setText("");
							clicked = false;
						}
					}
					if (exit) {
						break;
					}
				}

				// Wait until the server socket is closed.
				f.channel().closeFuture().sync();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				workerGroup.shutdownGracefully();
				bossGroup.shutdownGracefully();
			}
		}
	};
	
	/**
     * createGUI()
     * Builds the GUI using a GroupLayout layout manager.
     * For more information on GroupLayout: http://docs.oracle.com/javase/tutorial/uiswing/layout/group.html
     * @author Mike
     */
	@Override
	public void createGUI() {
		output = new JTextArea(20,40);
		output.setEditable(false);
		JScrollPane areaScrollPane = new JScrollPane(output);
		areaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		areaScrollPane.setPreferredSize(new Dimension(600, 400));
		message = new JTextField(20);
		message.setActionCommand("Enter");
		sendButton = new JButton("Send");
		userList = new JList<String>();
		String[] userListData = {"test", "test1", "test3"};
		userList.setListData(userListData);
		
		frame = new JFrame("MAD Chat - SERVER");
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
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		
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
		frame.pack();
		frame.setVisible(true);
	}

    public static void main(String[] args) throws Exception {
    	new ChatroomServer(8080).run();
    }

}