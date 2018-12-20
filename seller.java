import javax.crypto.*;

public interface seller extends java.rmi.Remote {
	
	// Create an auction that will be listed on the server, and return the auction ID of the new auction
	public int createAuction(String ticket, String name, double startPrice,  String description, double reservePrice)
		throws java.rmi.RemoteException;
	
	// Close an auction, and return information about the winner (or say item was not sold)
	public String[] closeAuction(String ticket, int auctionID)
		throws java.rmi.RemoteException;
		
	// Encrypts a challenge with a users key, returns its sealed object
	public SealedObject answerChallenge(String username, String challenge)
		throws java.rmi.RemoteException;
	
	// Asks the server to generate a challenge (random seq. of characters of set length)
	public String getChallenge() 
		throws java.rmi.RemoteException;
	
	// Generates a challenge (random seq. of characters of set length)
	public String genChallenge(int length)
		throws java.rmi.RemoteException;
	
	// Sends the encrypted challenge and users name to the server, returns a session ID (if info valid)
	public String verifyUser(String name, SealedObject ans)
		throws java.rmi.RemoteException;
	
	// Asks the server to encrypt a challenge with users key and return it
	public SealedObject getServAnswer(String name, String challenge) 
		throws java.rmi.RemoteException;
	
	// Asks the server to remove the active session ID
	public void closeSession(String ticket)
		throws java.rmi.RemoteException;
	
	// Decrypts a sealed object and returns its contents as an Object (identifier object)
	public Object decryptObject(String username, SealedObject obj)
		throws java.rmi.RemoteException;

}