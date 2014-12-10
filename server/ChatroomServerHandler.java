package server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.HashMap;

/**
 * ChatroomServerHandler
 * Handles incoming, and outgoing client connections
 * Writes data received from a client to all other clients.
 * @author Mike
 */
public class ChatroomServerHandler extends ServerHandler {

	private static HashMap<Channel, String> users = new HashMap<>();
	private static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	private String message = "";
	
	/**
	 * handlerAdded(ChannelHandlerContext ctx)
	 * Handles incoming connections.
	 * Tells all clients on the server who joined.
	 * @author Mike
	 */
	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		message = "[" + ctx.channel().remoteAddress() + "] has joined MAD Chat!";
		channels.add(ctx.channel());
		users.put(ctx.channel(), "");
		for(Channel channel : channels) {
			channel.writeAndFlush("[SERVER] : [" + ctx.channel().remoteAddress() + "] has joined MAD Chat!\t" + getUsers() + "\r\n");
		}
	}
	
	/**
	 * handlerRemoved(ChannelHandlerContext ctx)
	 * Handles outgoing connections.
	 * Tells all clients on the server who left.
	 * @author Mike
	 */
	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		message = "[" + ctx.channel().remoteAddress() + "] has left MAD Chat!";
		for(Channel channel : channels) {
			if(users.containsKey(ctx.channel()) && !users.get(ctx.channel()).equals("")) {
				channel.writeAndFlush("[SERVER] : [" + ctx.channel().remoteAddress() + " (" + users.get(ctx.channel()) + ")] has left MAD Chat!\r\n");
			} else {
				channel.writeAndFlush("[SERVER] : [" + ctx.channel().remoteAddress() + "] has left MAD Chat!\r\n");
			}
		}
		channels.remove(ctx.channel());
		users.remove(ctx.channel());
	}

	/**
	 * channelRead0(ChannelHandlerContext ctx, String message)
	 * Handles incoming messages from Clients.
	 * Tells all clients what the Client sending the message said.
	 * @author Mike
	 */
	@Override
	public void channelRead0(ChannelHandlerContext ctx, String message) throws Exception {
		System.out.println("channelRead0 (before):: " + message);
		if(message.equals("")) {
			System.out.println("Error: Wrote empty string. " + ctx.channel());
		} else {
			if(message.startsWith("/nick")) {
				String name = message.substring(message.indexOf(" ") + 1);
				if(!users.containsValue(name)) {
					users.replace(ctx.channel(), name);
					for(Channel c: channels) {
						c.writeAndFlush("UPDATE LIST " + getUsers() + "\r\n");
					}
				} else {
					ctx.channel().writeAndFlush("[SERVER] : Nickname already in use.\r\n");
				}
				return;
			}
			if(!message.startsWith("[P2P]")) {
				System.out.println("channelRead0 (not P2P):: true");
				this.message = "[" + ctx.channel().remoteAddress() + "] : " + message;
				for(Channel c: channels) {
					if(c != ctx.channel()) {
						if(users.containsKey(ctx.channel()) && !users.get(ctx.channel()).equals("")) {
							c.writeAndFlush("[" + ctx.channel().remoteAddress() + " (" + users.get(ctx.channel()) + ")] : " + message + "\r\n");
						} else {
							c.writeAndFlush("[" + ctx.channel().remoteAddress() + "] : " + message + "\r\n");
						}
			 		} else {
			 			c.writeAndFlush("[you] : " + message + "\r\n");
			 		}
			 	}
			} else {
				int startFrom = message.indexOf("FROM : [") + 8;
				int endFrom = message.indexOf(']', startFrom);
				String userFrom = message.substring(startFrom, endFrom);
				System.out.println(userFrom);
				int startTo = message.indexOf("TO : [") + 6;
				int endTo = message.indexOf(']', startTo);
				String userTo = message.substring(startTo, endTo);
				String msg = message.substring(endTo+1);
				this.message = message;
				for(Channel c: channels) {
					if(c.remoteAddress().toString().equals(userTo)) {
						System.out.println("channelRead0 (P2P):: " + "[P2P] [" + ctx.channel().remoteAddress() + "] : " + msg + "\r\n");
						c.writeAndFlush("[P2P] FROM: [" + userFrom + "] TO : [" + userTo + "] : " + msg + "\r\n");
					}
				}
			}
		}
		System.out.println("channelRead0 (end):: " + message);
	}

	/**
	 * getMessage()
	 * @return message - The message received from the clients by the server.
	 * @author Mike
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * resetMessage()
	 * Sets the message received by clients to the empty string.
	 * This way the Server GUI knows when to not append data to the output.
	 * @author Mike
	 */
	public void resetMessage() {
		message = "";
	}

	/**
	 * getChannels()
	 * @return channels - The working list of clients connected to the server.
	 * @author Mike
	 */
	public static ChannelGroup getChannels() {
		return channels;
	}
	
	/**
	 * getUsers()
	 * @return temp - A string of concatenated remote addresses of the clients connected to the server.
	 * @author Mike
	 */
	private String getUsers() {
		String temp = "";
		for(Channel c : channels) {
			if(users.containsKey(c) && !users.get(c).equals("")) {
				temp += "[" + c.remoteAddress() + " (" + users.get(c) + ")] ";
			} else {
				temp += "[" + c.remoteAddress() + "] ";
			}
		}
		return temp;
	}
	
}