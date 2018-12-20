import java.rmi.Naming;
import java.rmi.RemoteException;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.util.Scanner;
import java.io.*;
import java.text.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;


public class buyerClient {	
	public static void main(String[] args) {
		try {
			boolean servVerified = false;
			boolean running = false;
			String ticket = "";
			
			System.out.println("Connecting to server..");			
			// Create the reference to the remote object through the rmiregistry			
			buyer b = (buyer)Naming.lookup("rmi://localhost/frontend/buyer");
			
			// Ask user for his username
			System.out.print("Please tell us who you are..\nUsername: ");
			Scanner reader11 = new Scanner(System.in);
			String username = reader11.nextLine();
			
			// User verifies server -> Ask server to encrypt a challenge with users key, then client decrypts it,
			// if they match, we know we are talking with the server.
			if(servVerified == false) {
				System.out.println("Server not currently verified!\nVerifying server..");
				
				// Generate a challenge for the server
				String servChallenge = b.genChallenge(256);
				// Ask the server to encrypt it with our key
				SealedObject servAnswer = b.getServAnswer(username, servChallenge);
				if(servAnswer == null) {
					System.out.println("An error has occured.. the server does not recognise user " + username);
					return;
				}
				
				// Decrypt object and get an identifier object back
				identifier decryptedObject = (identifier)b.decryptObject(username, servAnswer);
				
				// Check if the challenge is correct and it was solved by the server
				if(decryptedObject.getChallenge().equals(servChallenge) && decryptedObject.getSolver().equals("SERVER")) {
					System.out.println("Server correctly verified!\n\n");
					servVerified = true;
				}
				else {
					System.out.println("An error has occured.. The server could not be verified!");
					return;
				}
				
			}
			
			// Server verifies user -> Send encrypted challenge to server, and if they match with the server,
			// the user is verified and a unique session ID is given.
			if(servVerified == true) {	
				System.out.println("Verifying user \"" + username + "\"");
				String userChallenge = b.getChallenge();

				ticket = b.verification(username, (b.answerChallenge(username, userChallenge)));
		
				if(!(ticket.equals("NOT_VERIFIED"))) {
					running = true;
					System.out.println("You have been verified!\n** SESSION ID ** : " + ticket);
				}
				else {
					System.out.println("You were not verified by the server!");
					return;
				}
			}
			
			while(running) {
				// Print auction menu
				Scanner reader1 = new Scanner(System.in);
				System.out.println("\n\n-----AUCTION-----");
				System.out.println("Please select one of the following options\n");
				System.out.println("1. I want to browse all auctions");
				System.out.println("2. I'd like to place a bid");
				System.out.println("3. Quit");
				System.out.print("Option: ");
				int choice = 0;
				try {
					choice = (reader1.nextInt());
				} catch (InputMismatchException ime) {
					System.out.println("\n***** ERROR: You did not enter a number.. *****\t\n----- Please try again! -----\n");
				}
				reader1.nextLine();
				
				// User wants to browse available auctions, so we print that information
				if(choice == 1) {
					String listings = b.browse(ticket);
					if(listings != null) {
						System.out.println("\n" + b.browse(ticket));
					}
					else {
						System.out.println("You do not have a valid session ID");
					}
				}
				
				// User wants to place a bid, so we ask for more information
				if(choice == 2) {
					
					// Ask user to input the auction ID of item he/she wants to bid on
					Scanner reader2 = new Scanner(System.in);
					System.out.println("----- BIDDING -----");
					System.out.println("Please give the auction ID of the item you want to bid on");
					System.out.print("ID: ");
					int id;
					try {
						id = reader2.nextInt();
					} catch (InputMismatchException ime) {
						System.out.println("\n***** ERROR: You did not enter a number.. *****\n----- Please try again! -----\n");
						continue;
					}
					
					// Ask user to enter the amount he/she wants to bid
					Scanner reader3 = new Scanner(System.in);
					System.out.println("How much would you like to bid?");
					System.out.print("Bid: ");
					double sum;
					try {
						sum = reader3.nextDouble();
						if (sum < 0) {
							throw new IllegalArgumentException("\n\n***** ERROR: Bid amount can't be negative *****\n----- Please try again! -----\n");
						}
					} catch (InputMismatchException ime) {
						System.out.println("\n***** ERROR: You did not enter a number.. *****\n----- Please try again! -----\n");
						continue;
					}
					
					// Ask user for his/her name
					Scanner reader4 = new Scanner(System.in);
					System.out.println("Please tell us your name");
					System.out.print("Name: ");
					String name = reader4.nextLine();
					if (name.isEmpty()) {
						throw new RuntimeException("\n\n***** ERROR: You did not write your name.. *****\n----- Please try again! -----\n");
					} 
					
					// Ask user his/her email
					Scanner reader5 = new Scanner(System.in);
					System.out.println("Please tell us your email");
					System.out.print("Email: ");
					String email = reader5.nextLine();
					if (email.isEmpty()) {
						throw new RuntimeException("\n\n***** ERROR: You did not write your email.. *****\n----- Please try again! -----\n");
					}
					
					// if successful, try to bid on the item, and check the response from the server
					int response = b.bid(ticket, id, sum, name, email);
					
					// auction ID is wrong
					if(response == 0) {
						System.out.println("\nError.. such an auction doesn't exist, are you sure the auction ID you entered is correct?");
					}
					
					// bid was not high enough
					else if(response == 1) {
						System.out.println("\nError.. your bid is not high enough!");
					}
					
					// bid successful
					else if(response == 2) {
						System.out.println("\nYour bid has been successfully placed!");
					}
					
					else if(response == 3) {
						System.out.println("You do not have a valid session ID");
					}
				}
				
				// User wants to leave, so we close the program
				if(choice == 3) {
					System.out.println("\nThank you for shopping with us,\nGoodnight!\n");
					running = false;
					b.closeSession(ticket);
				}
				
				// User did not pick one of the available options, so we notify the user of that
				if(choice > 3) {
					System.out.println("\nPlease select from one of the options available - 1 or 2 only!!\n");
				}
			}
		}
		
		catch (MalformedURLException murle) {
			System.out.println();
			System.out.println("MalformedURLException");
			System.out.println(murle);
		}
		catch (RemoteException re) {
			System.out.println();
			System.out.println("RemoteException");
			System.out.println(re);
		}
		catch (NotBoundException nbe) {
			System.out.println();
			System.out.println("NotBoundException");
			System.out.println(nbe);
		}
	}
}