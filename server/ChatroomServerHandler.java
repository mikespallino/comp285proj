package server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * ChatroomServerHandler
 * Handles incoming, and outgoing client connections
 * Writes data received from a client to all other clients.
 * @author Mike
 */
public class ChatroomServerHandler extends ServerHandler {

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
			channel.writeAndFlush("[SERVER] : [" + ctx.channel().remoteAddress() + "] has left MAD Chat!\t" + getUsers() + "\r\n");
		}
		channels.remove(ctx.channel());
	}

	/**
	 * channelRead0(ChannelHandlerContext ctx, String message)
	 * Handles incoming messages from Clients.
	 * Tells all clients what the Client sending the message said.
	 * @author Mike
	 */
	@Override
	public void channelRead0(ChannelHandlerContext ctx, String message) throws Exception {
		if(message.indexOf("[P2P]") == -1) {
			this.message = "[" + ctx.channel().remoteAddress() + "] : " + message;
			for(Channel c: channels) {
				if(c != ctx.channel()) {
					c.writeAndFlush("[" + ctx.channel().remoteAddress() + "] : " + message + "\r\n");
		 		} else {
		 			c.writeAndFlush("[you] : " + message + "\r\n");
		 		}
		 	}
		} else {
			int startFrom = 14;
			int endFrom = message.indexOf(']', startFrom);
			String userFrom = message.substring(startFrom, endFrom);
			int startTo = message.indexOf("TO : [") + 6;
			int endTo = message.indexOf(']', startTo);
			String userTo = message.substring(startTo, endTo);
			String msg = message.substring(endTo+1);
			for(Channel c: channels) {
				if(c.remoteAddress().toString().equals(userTo)) {
					c.writeAndFlush("[P2P] [" + ctx.channel().remoteAddress() + "] : " + msg + "\r\n");
				}
				if(c.remoteAddress().toString().equals(userFrom)) {
					c.writeAndFlush("[you] : " + msg + "\r\n");
				}
			}
		}
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
				temp += "[" + c.remoteAddress() + "] ";
		}
		return temp;
	}
	
}