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


public class sellerClient {
	public static void main(String[] args) {
		try {
			boolean servVerified = false;
			boolean running = false;
			String ticket = "";
			
			// Create the reference to the remote object through the remiregistry			
			seller s = (seller)Naming.lookup("rmi://localhost/frontend/seller");
			
			// Ask user for his username
			System.out.print("Please tell us who you are..\nUsername: ");
			Scanner reader11 = new Scanner(System.in);
			String username = reader11.nextLine();
			
			// User verifies server -> Ask server to encrypt a challenge with users key, then client decrypts it,
			// if they match, we know we are talking with the server.
			if(servVerified == false) {
				System.out.println("Server not currently verified!\nVerifying server..");
				
				// Generate a challenge for the server
				String servChallenge = s.genChallenge(256);
				// Ask the server to encrypt it with our key
				SealedObject servAnswer = s.getServAnswer(username, servChallenge);
				if(servAnswer == null) {
					System.out.println("An error has occured.. the server does not recognise user " + username);
					return;
				}
				
				// Decrypt object and get an identifier object back
				identifier decryptedObject = (identifier)s.decryptObject(username, servAnswer);
				
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
				String userChallenge = s.getChallenge();

				ticket = s.verifyUser(username, (s.answerChallenge(username, userChallenge)));
		
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
				System.out.println("1. I want to list an item");
				System.out.println("2. I'd like to close an auction");
				System.out.println("3. Quit");
				System.out.print("Option: ");
				int choice = 0;
				try {
					choice = (reader1.nextInt());
				} catch (InputMismatchException ime) {
					System.out.println("\n***** ERROR: You did not enter a number.. *****\t\n----- Please try again! -----\n");
				}
				reader1.nextLine();
				
				// User wants to list an item, so we ask for more information
				if(choice == 1) {					
					System.out.println("----- LISTING -----");
					Scanner reader2 = new Scanner(System.in);
					System.out.println("Please name the starting price of your product");
					System.out.print("Start price: ");
					double startingPrice;
					// Ask user to input a number only, call an error if value entered is not a number or is negative
					try {
						startingPrice = (reader2.nextDouble());
						if (startingPrice < 0) {
							throw new IllegalArgumentException("\n\n***** ERROR: Starting price can't be negative *****\n----- Please try again! -----\n");
						}
					} catch(InputMismatchException ime) {
						System.out.println("\n***** ERROR: You did not enter a number.. *****\t\n----- Please try again! -----\n");
						continue;
					}
					
					// Ask user to input a string description of the item, call an error if field is empty
					System.out.println("\nPlease give a short description of the item");
					System.out.print("Description: ");
					Scanner reader3 = new Scanner(System.in);
					String descriptionItem = (reader3.nextLine());
					if (descriptionItem.isEmpty()) {
						throw new RuntimeException("\n\n***** ERROR: You did not write any description of the item.. *****\n----- Please try again! -----\n");
					}
					
					// Ask user to input a number only, call an error if value entered is not anumber or is negative
					System.out.println("\nPlease name your reserve price");
					System.out.print("Reserve price: ");
					Scanner reader4 = new Scanner(System.in);
					double itemReservePrice;
					try {
						itemReservePrice = (reader4.nextDouble());
						if (itemReservePrice < 0) {
							throw new IllegalArgumentException("\n\n***** ERROR: Reserve price can't be negative *****\n----- Please try again! -----\n");
						}
					} catch(InputMismatchException ime) {
						System.out.println("\n***** ERROR: You did not enter a number.. *****\n----- Please try again! -----\n");
						continue;
					}
					
					// If successfull, create auction and return its ID for the seller
					int auctionID = s.createAuction(ticket, username, startingPrice, descriptionItem, itemReservePrice);
					if(auctionID != -1) {
						System.out.println("\nYour item has been successfully listed!\n\n*** Auction ID: " + auctionID + " ***");
					}
					else {
						System.out.println("You do not have a valid session ID");
					}
				}
				
				// User wants to close an auction, so we ask for more information
				if(choice == 2) {
					System.out.println("----- CLOSING -----");
					System.out.println("Please give the auction ID");
					System.out.print("ID: ");
					Scanner reader5 = new Scanner(System.in);
					int id;
					try {
						id = (reader5.nextInt());
					} catch (InputMismatchException ime) {
						System.out.println("\n***** ERROR: You did not enter a number.. *****\n----- Please try again! -----\n");
						continue;
					}
					
					String[] winnerInfo = s.closeAuction(ticket, id);
					
					if(winnerInfo != null) {
						if(winnerInfo.length == 1){
							if(winnerInfo[0].equals("NOT SOLD")) {
								System.out.println("\nWe're sorry! Your item did not reach its reserve value, therefore it was not sold..");
							}
							else if(winnerInfo[0].equals("ERROR")) {
								System.out.println("\nError, such auction doesn't exist.. are you sure you entered the auction ID right?");
							}
							else if(winnerInfo[0].equals("NO TICKET")) {
								System.out.println("\nError, you do not have the valid ticket to close that auction");
							}
							else if(winnerInfo[0].equals("NOT OWNER")) {
								System.out.println("\nError, you are not the owner of that auction therefore you can't close it");
							}
						}
						else if(winnerInfo.length > 1) {
								System.out.println("\nThe winner is " + winnerInfo[0] + "(" + winnerInfo[1] + "), with the final bid of " + winnerInfo[2]);
						}
					}
					else {
						System.out.println("\nAn error has occured.. try again");
					}
				}
				
				// User wants to leave, so we close the program
				if(choice == 3) {
					System.out.println("\nThank you for shopping with us,\nGoodnight!\n");
					running = false;
					s.closeSession(ticket);
				}
				
				// User did not pick one of the available options, so we notify the user of that
				if(choice > 3) {
					System.out.println("\nPlease select from one of the options available - 1, 2 or 3 only !!\n");
				}
			}
		}
		
		catch (MalformedURLException murle) {
			System.out.println("MalformedURLException: " + murle);
		}
		catch (RemoteException re) {
			System.out.println("RemoteException: " + re);
		}
		catch (NotBoundException nbe) {
			System.out.println("NotBoundException:" + nbe);
		}
	}
}