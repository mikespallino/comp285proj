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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

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

	private ServerChannelInitializer initializer;
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;
	
    public ChatroomServer(int port) {
		super(port);
	}
    
    /**
     * setUp()
     * Creates a thread to run the Server.
     * Sets up server.
     * Writes data to the clients.
     * Pulls data from the clients.
     * @author Mike
     */
	@Override
	public void setUp() {
		bossGroup = new NioEventLoopGroup();
		workerGroup = new NioEventLoopGroup();
		initializer = new ServerChannelInitializer();
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
					.childHandler(initializer)
					.option(ChannelOption.SO_BACKLOG, 128)
					.childOption(ChannelOption.SO_KEEPALIVE, true);

			// Bind and start to accept incoming connections.
			ChannelFuture f = b.bind(port).sync();

			while(update(initializer.getMessages())) { }

			// Wait until the server socket is closed.
			f.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}
    
    /**
     * run()
     * Start the server thread (stop the thread).
     * @author Mike
     */
    public boolean update(ArrayList<String> messages) {
    	try {
			Thread.sleep(1000);
			output.setCaretPosition(output.getDocument().getLength());
			initializer.getMessage();
			if (messages.size() > 0) {
				for (int i = 0; i < messages.size(); i++) {
					output.append(messages.get(i));
					output.append("\n");
				}
				initializer.resetMessages();
			}
			String[] users = new String[ChatroomServerHandler.getChannels().size()];
			int k = 0;
			for (Channel c : ChatroomServerHandler.getChannels()) {
				if (k < users.length) {
					users[k] = c.remoteAddress().toString();
					k++;
				}
			}
			userList.setListData(users);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}
    	return true;
    }
    
    private void sendMessage(String message) {
    	if (message.equalsIgnoreCase("/bye")) {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
			System.exit(0);
		} else if(message.startsWith("/kick")) {
			output.append(message + "\r\n");
			System.out.println(message);
			String user = message.substring(6);
			System.out.println(user);
			Map<Channel, String> users = ChatroomServerHandler.getUserMap();
			for(Channel c: users.keySet()) {
				System.out.println(c.remoteAddress().toString());
				if(c.remoteAddress().toString().equals(user) || users.get(c).equals(user)) {
					ChatroomServerHandler.kick(c);
				}
			}

		} else if (message.startsWith("/log")) {
			try {
				File outputFile = new File("logs/server_log.txt");
				if(!outputFile.exists()) {
					outputFile.createNewFile();
				}
				FileWriter fout = new FileWriter(outputFile, true);
				BufferedWriter writer = new BufferedWriter(fout);
				Date date= new Date();
				String log = date.toString() + "\n\r\n\r" + output.getText() + "\n\r\n\r";
				output.setText("");
				writer.append(log);
				writer.close();
				fout.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if (!message.equals("")) {
			output.append(message + "\r\n");
			for (Channel channel : ChatroomServerHandler.getChannels()) {
				channel.writeAndFlush("[SERVER] : " + message + "\r\n");
			}
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
		output = new JTextArea(20,40);
		output.setEditable(false);
		JScrollPane areaScrollPane = new JScrollPane(output);
		areaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		areaScrollPane.setPreferredSize(new Dimension(600, 400));
		message = new JTextField(20);
		message.setActionCommand("Enter");
		sendButton = new JButton("Send");
		userList = new JList<String>();
		
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
				sendMessage(message.getText());
				message.setText("");
			}
			
		});
		
		message.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(arg0.getActionCommand().equals("Enter")) {
					sendMessage(message.getText());
					message.setText("");
				}
			}
		});
		
//		final PopupMenu kickMenu = new PopupMenu();
//		kickMenu.add(new MenuItem("Kick User"));
//		userList.add(kickMenu);
//		
//		userList.addListSelectionListener(new ListSelectionListener() {
//
//			@Override
//			public void valueChanged(ListSelectionEvent arg0) {
//				if(arg0.getValueIsAdjusting() == false) {
//					if(arg0.getSource() instanceof MouseEvent) {
//						MouseEvent e = (MouseEvent) arg0.getSource();
//						if(e.getButton() == MouseEvent.BUTTON2) {
//							kickMenu.show(userList, e.getX(), e.getY());
//							System.out.println("called");
//						}
//					}
//				}
//			}
//			
//		});
		
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
    	new ChatroomServer(8080).setUp();
    }

}