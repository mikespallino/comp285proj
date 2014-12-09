package client;

import server.ChatroomServer;

public class ClientTest {
	
	public static void main(String[] args) throws Exception {
		ChatroomServer server = new ChatroomServer(8080);
		server.run(false);
		Thread.sleep(1500);
		ChatroomClient c1 = new ChatroomClient("127.0.0.1", 8080);
		c1.setUp(false);
		Thread.sleep(1500);
		ChatroomClient c2 = new ChatroomClient("127.0.0.1", 8080);
		c2.setUp(false);
		Thread.sleep(1500);
		if(c1.getUsers().length < 1 && c2.getUsers().length < 1) { 
			System.err.println("\nFAILED: User list can't be less than 1.\n");
		} else {
			System.out.println("\nPASSED.\n");
		}
		
		c1.sendMessage("/nick bob");
		
		Thread.sleep(1000);
		c1.updateList(ClientHandler.getUsers());
		c2.updateList(ClientHandler.getUsers());
		boolean found = false;
		for(int i = 0; i < c1.getUsers().length; i++) {
			if(c1.getUsers()[i].indexOf("(bob") != -1) {
				found = true;
			}
		}
		
		if(!found) {
			System.err.println("\nFAILED: User list must have nickname store with it after /nick assignment.\n");
		} else {
			System.out.println("\nPASSED.\n");
		}
		
		c1.p2pClients.add(new P2PClient(c2.getConnection(), c1));
		Thread.sleep(1500);
		int c1Index = c1.p2pClients.get(0).output.getText().indexOf("Welcome to MAD P2P chat!\nYou are speaking with: ");
		if(c1Index == -1) {
			System.err.println("\nFAILED: P2P Chat didn't start properly.\n");
		} else {
			System.out.println("\nPASSED.\n");
		}
		c1.sendMessage("[P2P] FROM: [" + c1.getConnection() + " (bob)] TO : [" + c1.getConnection() + "] : Hello\r\n");
		Thread.sleep(1000);
	}
}
