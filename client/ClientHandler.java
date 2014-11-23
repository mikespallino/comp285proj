package client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.ArrayList;

/**
 * ClientHandler implementation
 * Processes incoming messages.
 * @author Mike
 *
 */
public class ClientHandler extends SimpleChannelInboundHandler<String> {

	private String message = "";
	private static ArrayList<String> userList = new ArrayList<>();
	private Channel server;
	
	/**
	 * exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	 * Print the stack trace for the exception.
	 * Close the ChannelHandlerContext object.
	 * @author Mike
	 */
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
    
    /**
     * getMessage()
     * @return message - The message sent by the server.
     * @author Mike
     */
    public String getMessage() {
    	return message;
    }
    
    /**
     * resetMessage()
     * Set the message string back to empty.
     * @author Mike
     */
    public void resetMessage() {
    	message = "";
    }

    /**
     * channelRead0(ChannelHandlerContext ctx, String message)
     * Sets stores the message to be used later by the Client.
     * @author Mike
     */
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String message) throws Exception {
		this.message = message;
		if(message.indexOf("has joined MAD Chat!") != -1) {
			this.message = message.substring(0, message.indexOf("\t"));
			String users = message.substring(message.indexOf("\t") + 1);
			parseEndOfMessage(users);
		} else if(message.indexOf("has left MAD Chat!") != -1) {
			int index = 12;
			String user = message.substring(index, index + userLength(message.substring(index),11));
			for(int i = 0; i < userList.size(); i++) {
				if(userList.get(i).equals(user)) {
					userList.remove(i);
				}
			}
		}
		System.out.println("channelRead0:: Message: " + this.message);
		System.out.println("channelRead0:: Done.");
	}

	/**
	 * handlerAdded(ChannelHandlerContext ctx) throws Exception
	 * Gets the server channel object and stores it privately.
	 * @author Mike
	 */
	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		server = ctx.channel();
	}
	
	/**
	 * handlerRemoved(ChannelHandlerContext ctx) throws Exception
	 * Tell the client that the server shutdown.
	 * Set the server channel to null.
	 * @author Mike
	 */
	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		message = "The server shut down.";
		server = null;
	}
	
	/**
	 * getServer()
	 * @return server - the server handler so that the client can display the remote host. 
	 * @author Mike
	 */
	public Channel getServer() {
		return server;
	}
	
	/**
	 * userLength(String users)
	 * @param users - The end of the server message which appends all the current users to the end of the String sent over the server.
	 * @return the length of the address between the brackets.
	 * @author Mike
	 */
	private int userLength(String users, int from) {
		int start = 0;
		int end = 0;
		if(users.indexOf('[', from) != -1) {
			start = users.indexOf('[', from);
		}
		if(users.indexOf(']', from) != -1) {
			end = users.indexOf(']', from);
		}
		return end - start - 1;
	}
	
	/**
	 * getUsers()
	 * @return an array of users for use in the JList on the Chatroom Client.
	 * @author Mike
	 */
	public static String[] getUsers() {
		String[] users = new String[userList.size()];
		for(int i = 0; i < userList.size(); i++) {
			users[i] = userList.get(i);
		}
		return users;
	}
	
	/**
	 * parseEndOfMessage(String message)
	 * @param message - Server message that someone joined the chatroom.
	 * This pulls the normal message out as a substring.
	 * It then begins to pull out sections of the user list appended to the end of the server message
	 * based on the address length computed buy userLength(users).
	 * It checks to make sure that the user isn't already in the user list before adding it to the ArrayList.
	 * @author Mike
	 */
	private void parseEndOfMessage(String users) {	
		int addressLength = -1;
		for(int i = 0; i < users.length(); i+=(addressLength+3)) {
			addressLength = userLength(users, i);
			if(i+addressLength < users.length()) {
				String temp = users.substring(i+1, i+addressLength+1);
				boolean newUser = true;
				for(int j = 0; j < userList.size(); j++) {
					if(temp.equals(userList.get(j))) {
						newUser = false;
					}
				}
				if(newUser) {
					userList.add(temp);
					System.out.println("parseEndOfMessage:: Added " + temp);
				}
			}
		}
	}
	
}