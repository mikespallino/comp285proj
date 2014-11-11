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
	
	public void getMessage() {
		if(handlers != null) {
			for(ChatroomServerHandler handler: handlers) {
				if(handler != null && handler.getMessage() != null && !handler.getMessage().equals("")) {
					messages.add(handler.getMessage());
					handler.resetMessage();
				}
			}
		}
	}
	
	public ArrayList<String> getMessages() {
		return messages;
	}
	
	public void resetMessages() {
		messages = new ArrayList<>();
	}
	
	public ArrayList<ChatroomServerHandler> getHandlers() {
		return handlers;
	}
}
