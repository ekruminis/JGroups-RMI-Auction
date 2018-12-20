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

public class buyerImpl extends java.rmi.server.UnicastRemoteObject implements buyer {
	
	// Implementations must have an explicit constructor
	// in order to declare the RemoteException exception
	public buyerImpl() throws java.rmi.RemoteException {
		super();
	}
	
	// Implementation of the browse method :
	// Asks the server to return a string with the data on the auction linked listed,
	// which is then returned to the user
	public String browse(String ticket)
		throws java.rmi.RemoteException {
				String listed = FrontEnd.advertiseList(ticket);
				return listed;
			}
	
	// Implementation of the bidding method : 
	// Asks the server to update the chosen auction with the buyers information that is provided,
	// returns an int that details whether the bid was successful, and if not, why
	public int bid(String ticket, int auctionID, double sum, String buyerName, String buyerEmail)
		throws java.rmi.RemoteException {
				return FrontEnd.bid(ticket, auctionID, sum, buyerName, buyerEmail);
			}
		
	// Implementation of the generate challenge method :
	// Creates a string of chosen length containing random characters, and returns it
	public String genChallenge(int length)
		throws java.rmi.RemoteException {
			String chars = "123456789!£$%^&*(){}:@/.,;~<>¬`ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
			Random rnd = new Random();
			String c = new String();
			for(int x = 0;x < length;x++) {
				c += String.valueOf(chars.charAt(rnd.nextInt(chars.length())));
				
			}
			return c;
	}
	
	// Implementation of the answer challenge method :
	// Opens the users key, and starts a cipher which encrypts an identifer object with the challenge and its solver
	// and returns a sealed object
	public SealedObject answerChallenge(String username, String challenge) 
		throws java.rmi.RemoteException {
			Object sealedChallenge = new Object();
			try{
				// read key
				FileInputStream keyStream = new FileInputStream("userkeys/" + username + ".key");
				ObjectInputStream inputStream = new ObjectInputStream(keyStream);
				SecretKey key = (SecretKey)inputStream.readObject();			
					
				// Start cipher as a AES instance
				Cipher c = Cipher.getInstance("AES");
				c.init(Cipher.ENCRYPT_MODE, key);
			
				// create identifier object
				identifier userIdentity = new identifier(username, challenge);
				
				sealedChallenge = new SealedObject(userIdentity, c);
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
			return (SealedObject)sealedChallenge;
	}
	
	// Implementation of the decrypt object method :
	// Takes a sealed object, decrypts it with the users key,
	// and returns its contents as an object (identifier object)
	public Object decryptObject(String username, SealedObject obj)
		throws java.rmi.RemoteException {
			Object decryptedObj = new Object();
			try {
				// Open up our key
				FileInputStream keyStream = new FileInputStream("userkeys/" + username + ".key");
				ObjectInputStream inputStream = new ObjectInputStream(keyStream);
				SecretKey key = (SecretKey)inputStream.readObject();	
				
				// Start cipher as a AES instance in decryption mode
				Cipher c = Cipher.getInstance("AES");
				c.init(Cipher.DECRYPT_MODE, key);
					
				// Decrypt servers answer with user key
				Object decrypted = obj.getObject(c);	
				return decrypted;
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
			catch (NoSuchPaddingException nspe) {
				System.out.println("NoSuchPaddingException");
				System.out.println(nspe);
			}
			catch (IllegalBlockSizeException ibse) {
				System.out.println("IllegalBlockSizeException");
				System.out.println(ibse);
			}
			return decryptedObj;
		}
	
	// Implementation of the get challenge method :
	// Asks the server to generate and return a random sequence of characters,
	// used to verify the user server-side
	public String getChallenge() 
		throws java.rmi.RemoteException {
			return FrontEnd.getChallenge();
	}
	
	// Implementation of the verification method :
	// Sends the users name and a sealed object which contains the challenge encrypted with the users key,
	// if the user is confirmed as valid, returns a valid session ID
	public String verification(String name, SealedObject ans)
		throws java.rmi.RemoteException {
			return FrontEnd.getTicket(name, ans);
		}
	
	// Implementation of the get server answer method : 
	// Asks the server to encrypt a challenge with the users key and then return it,
	// used to verify the server client-side
	public SealedObject getServAnswer(String name, String challenge) 
		throws java.rmi.RemoteException {
			return FrontEnd.answerChallenge(name, challenge);
		}
	
	// Implementation of the close session method :
	// Asks the server to close a currently active session when the user leaves
	public void closeSession(String ticket)
		throws java.rmi.RemoteException {
			FrontEnd.closeSession(ticket);
		}
}