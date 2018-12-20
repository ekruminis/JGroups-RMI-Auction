import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.Naming;
import java.rmi.RemoteException;
import org.jgroups.*;
import org.jgroups.blocks.*;
import org.jgroups.util.*;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.util.Scanner;
import java.io.*;
import java.text.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;

public class FrontEnd {	
	
	// LinkedList of currently registered user names
	private static LinkedList<String> listOfUsers = new LinkedList<String>(Arrays.asList(
			"michael", "jordan", "johncena", "emilia"));
	
	// Current challenge for user
	private static String userChallenge;
	
	// Implementation of the generate challenge method :
	// Creates a string of chosen length containing random characters, and returns it
	private static String genChallenge(int length) {
		String chars = "123456789!£$%^&*(){}:@/.,;~<>¬`ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
		Random rnd = new Random();
		String c = new String();
		for(int x = 0;x < length;x++) {
			c += String.valueOf(chars.charAt(rnd.nextInt(chars.length())));
		}
		return c;
	}
	
	// Opens the selected key, and starts a cipher which encrypts the challenge
	// and returns its sealed object
	public static SealedObject answerChallenge(String username, String challenge) {
		SealedObject sealedChallenge;
		try {
			// read key
			FileInputStream keyStream = new FileInputStream("serverkeys/" + username + ".key");
			ObjectInputStream inputStream = new ObjectInputStream(keyStream);
			SecretKey key = (SecretKey)inputStream.readObject();			
				
			// Start cipher as a AES instance
			Cipher c = Cipher.getInstance("AES");
			c.init(Cipher.ENCRYPT_MODE, key);
			
			identifier servIdentity = new identifier("SERVER", challenge);
			
			// Encrypt client_request with key 
			sealedChallenge = new SealedObject(servIdentity, c);
			return sealedChallenge;
		}
		catch(NoSuchAlgorithmException nsae) {
			System.out.println("NoSuchAlgorithmException");
			System.out.println(nsae);
		}
		catch(FileNotFoundException fnfe) {
			System.out.println("Attempted login as " + username + ".. No such user found");
		}
		catch(IOException ioe) {
			System.out.println("IOException");
			System.out.println(ioe);
		}
		catch (NoSuchPaddingException nspe) {
			System.out.println("NoSuchPaddingException");
            System.out.println(nspe);
		}
		catch (IllegalBlockSizeException ibse) {
			System.out.println("IllegalBlockSizeException");
            System.out.println(ibse);
		}
		catch (InvalidKeyException ike) {
			System.out.println("InvalidKeyException");
            System.out.println(ike);
		}
		catch (ClassNotFoundException cnfe) {
			System.out.println("ClassNotFoundException");
            System.out.println(cnfe);
		}
		return null;
	}
	
	// Generates a ticket for the verified user -> 
	// first checks if the user has a key on server side, then opens up that key,
	// decrypts the sealed object and checks whether it matches the challenge given to the user
	// if it does, generate a session ID, add it to the sessions linked list and return it for the user
	public static String getTicket(String name, SealedObject userAnswer) {
		for(String n : listOfUsers) {
			if(name.equals(n)) {
				try{
					FileInputStream keyStream = new FileInputStream("serverkeys/" + name + ".key");
					ObjectInputStream inputStream = new ObjectInputStream(keyStream);
					SecretKey key = (SecretKey)inputStream.readObject();			

					// Start cipher as a AES instance
					Cipher c = Cipher.getInstance("AES");
					c.init(Cipher.DECRYPT_MODE, key);
					
					identifier decryptedObject = (identifier)userAnswer.getObject(c);
					
					if(decryptedObject.getChallenge().equals(userChallenge) && decryptedObject.getSolver().equals(name)) {
						String sessionID = genChallenge(64);
						auctionServer.clusterAddTicket(name, sessionID);
						System.out.println("User " + name + " verified! ---> ticket: " + sessionID);
						return sessionID;
					}
					else {
						return "NOT_VERIFIED";
					}
				}
				catch(NoSuchAlgorithmException nsae) {
					System.out.println("NoSuchAlgorithmException");
					System.out.println(nsae);
				}
				catch(FileNotFoundException fnfe) {
					System.out.println("FileNotFoundException");
					System.out.println(fnfe);
				}
				catch(IOException ioe) {
					System.out.println("IOException");
					System.out.println(ioe);
				}
				catch (NoSuchPaddingException nspe) {
					System.out.println("NoSuchPaddingException");
					System.out.println(nspe);
				}
				catch (IllegalBlockSizeException ibse) {
					System.out.println("IllegalBlockSizeException");
					System.out.println(ibse);
				}
				catch (InvalidKeyException ike) {
					System.out.println("InvalidKeyException");
					System.out.println(ike);
				}
				catch (ClassNotFoundException cnfe) {
					System.out.println("ClassNotFoundException");
					System.out.println(cnfe);
				}
				catch (BadPaddingException bpe) {
					System.out.println("BadPaddingException");
					System.out.println(bpe);
				}
			}
		}
		return "NOT_VERIFIED";
	}
	
	// Connect FrontEnd to the clients
	private void setup() throws Exception {
		try {
			// Contruct a new buyerImpl object and bind it to the local registry
			buyer b = new buyerImpl();
			Naming.rebind("rmi://localhost/frontend/buyer", b);
			
			// Construct a new sellerImpl object and bind it to the local registry
			seller s = new sellerImpl();
			Naming.rebind("rmi://localhost/frontend/seller", s);
		}
		catch (Exception e) {
			System.out.println("Front-End Error: " + e);
		}
	}
	
	// call advertiseList() on all cluster members and return result back to client
	public static String advertiseList(String ticket) throws RemoteException {
		return auctionServer.clusterAdvertiseList(ticket);
	}
	
	// call bid() on all cluster members and return result back to client
	public static int bid(String ticket, int auctionID, double sum, String buyerName, String buyerEmail) throws RemoteException {
		return auctionServer.clusterBid(ticket, auctionID, sum, buyerName, buyerEmail);
	}
	
	// return a random challenge for user
	public static String getChallenge() throws RemoteException {
		return userChallenge = genChallenge(256);
	}
	
	// call closeSession() on all cluster members
	public static void closeSession(String ticket) throws RemoteException {
		auctionServer.clusterCloseSession(ticket);
	}
	
	// call createAuction() on all cluster members and return result back to client
	public static int addToList(String ticket, String name, double startPrice,  String description, double reservePrice) throws RemoteException{
		return auctionServer.clusterCreateAuction(ticket, name, startPrice, description, reservePrice); 
	}
	
	// call closeAuction() on all cluster members and return result back to client
	public static String[] closeAuction(String ticket, int ID) throws RemoteException {
		return auctionServer.clusterCloseAuction(ticket, ID);
	}
	
	/* private static void register(String username) {
		try{
			//create key
			KeyGenerator keygen = KeyGenerator.getInstance("AES");
			keygen.init(256);
			SecretKey secret = keygen.generateKey();
			
			//save key as file
 			FileOutputStream fout = new FileOutputStream("serverkeys/" + username + ".key");
			ObjectOutputStream oout = new ObjectOutputStream(fout);
			
			oout.writeObject(secret);
			oout.close();
			fout.close(); 
		}
		catch(FileNotFoundException fnfe) {
			System.out.println("FileNotFoundException");
			System.out.println(fnfe);
		}
		catch(IOException ioe) {
			System.out.println("IOException");
			System.out.println(ioe);
		}
		catch (NoSuchPaddingException nspe) {
			System.out.println("NoSuchPaddingException");
            System.out.println(nspe);
		}
		catch (IllegalBlockSizeException ibse) {
			System.out.println("IllegalBlockSizeException");
            System.out.println(ibse);
		}
		catch (InvalidKeyException ike) {
			System.out.println("InvalidKeyException");
            System.out.println(ike);
		}
		catch (ClassNotFoundException cnfe) {
			System.out.println("ClassNotFoundException");
            System.out.println(cnfe);
		}
		catch (BadPaddingException bpe) {
			System.out.println("BadPaddingException");
            System.out.println(bpe);
		}
	} */
	
	// Setup FrontEnd
	public static void main(String args[]) throws Exception {
		new FrontEnd().setup();
		System.out.println("Front-end connected!");
    }
}