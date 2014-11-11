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
	
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
    
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
		if(message.indexOf("has joined MAD Chat!") != -1) {
			parseEndOfMessage(message);
		} else if(message.indexOf("has left MAD Chat!") != -1) {
			this.message = message.substring(0, message.indexOf("\t"));
			int index = 12;
			String user = message.substring(index, index + userLength(message.substring(index)));
			for(int i = 0; i < userList.size(); i++) {
				if(userList.get(i).equals(user)) {
					userList.remove(i);
				}
			}
		} else {
			this.message = message;
		}
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		server = ctx.channel();
	}
	
	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		message = "The server shut down.";
		server = null;
	}
	
	public Channel getServer() {
		return server;
	}
	
	private int userLength (String users) {
		int start = 0;
		int end = 0;
		for(int i = 0; i < users.length(); i++) {
			if(users.charAt(i) == '[') {
				start = i + 1;
			} else if(users.charAt(i) == ']') {
				end = i;
				break;
			}
		}
		return end-start;
	}
	
	public static int getNumberOfUsers() {
		return userList.size();
	}
	
	public static String[] getUsers() {
		String[] users = new String[getNumberOfUsers()];
		for(int i = 0; i < getNumberOfUsers(); i++) {
			users[i] = userList.get(i);
		}
		return users;
	}
	
	private void parseEndOfMessage(String message) {
		int userListStart = message.indexOf("\t");
		String users = "";
		this.message = message.substring(0, userListStart);
		users = message.substring(userListStart);	
		int addressLength = userLength(users);
		for(int i = 2; i < users.length(); i+=(addressLength+3)) {
			if(i+addressLength < users.length()) {
				//addressLength = userLength(users.substring(i, i+addressLength));
				String temp = users.substring(i, i+addressLength);
				boolean newUser = true;
				for(int j = 0; j < userList.size(); j++) {
					if(temp.equals(userList.get(j))) {
						newUser = false;
					}
				}
				if(newUser) {
					userList.add(users.substring(i, i+addressLength));
				} else {
					newUser = true;
				}
			}
		}
	}
	
}