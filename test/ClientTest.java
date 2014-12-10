package test;

import server.ChatroomServer;
import client.ChatroomClient;
import client.ClientHandler;
import client.P2PClient;

public class ClientTest {
	
	public static void main(String[] args) throws Exception {
		boolean passedTests = true;
		
		System.out.println("---START TESTING---");
		
		//Start the server
		ChatroomServer server = new ChatroomServer(8080);
		server.run(false);
		Thread.sleep(1500);
		
		//Start client 1
		ChatroomClient c1 = new ChatroomClient("127.0.0.1", 8080);
		c1.setUp(false);
		//Thread.sleep(1500);
		
		//Start client 2
		ChatroomClient c2 = new ChatroomClient("127.0.0.1", 8080);
		c2.setUp(false);
		Thread.sleep(1500);
		
		//Check to make sure the user list updates
		if(c1.getUsers().length < 1 && c2.getUsers().length < 1) { 
			System.err.println("\nFAILED: User list can't be less than 1.\n");
			passedTests = false;
		} else {
			System.out.println("\nPASSED.\n");
		}
		
		//Set Client 1's nickname
		c1.sendMessage("/nick bob");
		
		Thread.sleep(1000);
		
		//Check to make sure the nickname was updated in the user list
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
			passedTests = false;
		} else {
			System.out.println("\nPASSED.\n");
		}
		
		//Create a P2P session from client 1 to client 2
		c1.getP2PClients().add(new P2PClient(c2.getConnection(), c1));
		Thread.sleep(1500);
		int c1Index = c1.getP2PClients().get(0).getOutput().getText().indexOf("Welcome to MAD P2P chat!\nYou are speaking with: ");
		
		//Check to mame sure the P2P Window opened properly
		if(c1Index == -1) {
			System.err.println("\nFAILED: P2P Chat didn't start properly.\n");
			passedTests = false;
		} else {
			System.out.println("\nPASSED.\n");
		}
		
		//Send a P2P message from client 1 to client 2
		c1.sendMessage("[P2P] FROM : [" + c1.getConnection() + "] TO : [" + c2.getConnection() + "] : Hello\r\n");
		Thread.sleep(1000);
		
		//Check to make sure the P2P window opens for client 2
		int c2Index = c2.getP2PClients().get(0).getOutput().getText().indexOf("Welcome to MAD P2P chat!\nYou are speaking with: ");
		if(c2Index == -1) {
			System.err.println("\nFAILED: P2P Chat didn't start properly for second client.\n");
			passedTests = false;
		} else {
			System.out.println("\nPASSED.\n");
		}
		
		Thread.sleep(1000);
		c2.sendMessage("[P2P] FROM : [" + c2.getConnection() + "] TO : [" + c1.getConnection() + "] : Hello\r\n");
		Thread.sleep(1500);
		c1Index = c1.getP2PClients().get(0).getOutput().getText().indexOf("[PEER] : [" + c2.getConnection() + "] : Hello");
		if(c2Index == -1) {
			System.err.println("\nFAILED: P2P Chat didn't send message properly for second client.\n");
			passedTests = false;
		} else {
			System.out.println("\nPASSED.\n");
		}		
		
		System.out.println("---FINISHED TESTING---");
		
		if(passedTests) {
			System.out.println("Passed all the tests.");
			System.exit(0);
		} else {
			System.err.println("One or more of the tests failed.");
			System.exit(-1);
		}
	}
}
