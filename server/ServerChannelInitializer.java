package server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.util.ArrayList;

public class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {
	
	private ArrayList<ChatroomServerHandler> handlers = new ArrayList<>();
	private ArrayList<String> messages = new ArrayList<>();
	
	/**
	 * initChannel(SocketChannel ch) throws Exception
	 * This method adds Decoders and Encoders as well as the server handler objects to the SocketChannel pipeline.
	 * The framer is used to tell the server what type of Strings to expect. (8192 bytes, Delimited by line endings).
	 * The Decoder is used to decode the Strings received over the server.
	 * The Encoder is used to encode the Strings sent over the server.
	 * The ChatroomServerHandler is also added to the pipeline here.
	 * There is an ArrayList of ChatroomServerHandler's, one for every client that connects.
	 * @author Mike
	 */
	@Override
    public void initChannel(SocketChannel ch) throws Exception {
		ChatroomServerHandler newHandler = new ChatroomServerHandler();
		//max size 8192, all input delimited by line endings
		ch.pipeline().addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
		ch.pipeline().addLast("Decoder", new StringDecoder());
		ch.pipeline().addLast("Encoder", new StringEncoder());
   	 
        ch.pipeline().addLast(newHandler);
        handlers.add(newHandler);
    }
	
	/**
	 * getMessage()
	 * This method builds an ArrayList of Strings from all of the ChatroomServerHandler objects that are currently handling clients.
	 * It adds every String that isn't null or empty to the list.
	 * It also resets the message back to the empty String.
	 * @author Mike
	 */
	public void getMessage() {
		if(handlers != null) {
			//Iterator it = handlers.iterator();
			for(ChatroomServerHandler handler: handlers) {
				if(handler != null && handler.getMessage() != null && !handler.getMessage().equals("")) {
					messages.add(handler.getMessage());
					handler.resetMessage();
				}
			}
		}
	}
	
	/**
	 * getMessages()
	 * @return messages - The ArrayList holding the current messages that have been received by clients.
	 * @author Mike
	 */
	public ArrayList<String> getMessages() {
		return messages;
	}
	
	/**
	 * resetMessages()
	 * This method sets messages equal to a new ArrayList of Strings so as to remove all of its contents.
	 * @author Mike
	 */
	public void resetMessages() {
		messages = new ArrayList<>();
	}
	
}
