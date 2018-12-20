import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.io.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.rmi.RemoteException;

// Run with =:=  set CLASSPATH=%CLASSPATH%;C:\Users\Edvinas\Desktop\SCC311\cw2_kruminis\2b\jgroups-3.6.14.Final.jar;. 

import org.jgroups.*;
import org.jgroups.blocks.*;
import org.jgroups.util.*;

// *** CODE MAY NOT RUN BECAUSE OF JAVA KEY RESTRICTIONS - UPDATE TO JAVA v12 OR REMOVE KEY SECURITY RESTRICTIONS ***

// Auction listings class,
// details the information that will be held about the auction
class auctionState {
	public int aID;						// ID of the auction
	public double startPrice;			// Starting price
	public String description;			// Description of the item
	public double reservePrice;			// Reverse price (minimum acceptable price)
	public double currentBid;			// Current highest bid
	public String buyerName;			// Current highest bidders name
	public String buyerEmail;			// Current highest bidders email
	public String owner;				// Owner of the item/auction
	
	auctionState(int a, double s, String d, double r, double c, String bn, String be, String o) {
		aID = a;
		startPrice = s;
		description = d;
		reservePrice = r;
		currentBid = c;
		buyerName = bn;
		buyerEmail = be;
		owner = o;
	}
}

// Currently active ticket class
// Contains the ticket of the active sessions and the users under it
class tickets {
	public String name;
	public String sessionID;
	
	tickets(String n, String id) {
		name = n;
		sessionID = id;
	}
}

public class auctionServer {
	
	// Create linked list where auctions will be held, and initiliase some random auctions
	private static LinkedList<auctionState> auctions = new LinkedList<auctionState>(Arrays.asList(
			(new auctionState(0, 10, "magic key", 15, -1, null, null, "jordan")),
			(new auctionState(1, 15, "wand", 15, 25, "emilia", "emilia@gmail.com", "jordan")),
			(new auctionState(2, 20, "sword", 20, -1, null, null, "michael")),
			(new auctionState(3, 50, "box", 80, 54, "johncena", "johncena@wwe.com", "emilia")))
			);
	
	// Create linked list where the active session IDs will be held
	private static LinkedList<tickets> sessions = new LinkedList<tickets>();
	
	// Create new channel and instantiate it
	private static JChannel channel = conChan();
	// Create new dispatcher and instantiate it
	private static RpcDispatcher dispatcher = conDisp();
	// Setup requestoptions to get all response and a timeout of 1sec.
	private static RequestOptions requestOptions = new RequestOptions(ResponseMode.GET_ALL, 1000);
	
	// Connect channel to cluster and return it if successful
	public static JChannel conChan() {
		try{
			JChannel chan = new JChannel();
			chan.connect("AUCTION");
			return chan;
		}
		catch(Exception e) {
			System.out.println(e);
		}
		return null;
	}
	
	// Return channel
	public static JChannel getChan() {
		return channel;
	}
	
	// Connect dispatcher to channel and server and then return it
	public static RpcDispatcher conDisp() {
		RpcDispatcher disp = new RpcDispatcher();
		
		try{
			disp = new RpcDispatcher(getChan(), new auctionServer());
			return disp;
		}
		catch(Exception e) {
			System.out.println(e.getCause());
		}
		return disp;
	}
	
	// Return dispatcher
	public static RpcDispatcher getDisp() {
		return dispatcher;
	}
	
	public auctionServer() {
		System.out.println("New auctionServer has joined!");
	}
	
	// Return list of currently active sessions as a String[]
	public static String[] getSessions() {
		String[] active = new String[sessions.size()*2];

		int x = -1;
		for(tickets t : sessions) {
			active[x+=1] = (t.name);
			active[x+=1] = (t.sessionID);
		}
		return active;
	}
	
	// JGroups cluster call on getSessions()
	public static void clusterGetSessions() {
		try{
			// Get cluster co-ordinator address
			View view = getChan().getView();
			List<Address> addressList = new ArrayList<>();
			Address first = view.getMembers().get(0);
			addressList.add(first);
			
			// Call getSessions() on cluster co-ordinator to get list of current sessions
			String[] listed = null;
			RspList resp = getDisp().callRemoteMethods(addressList,"getSessions",null,null,auctionServer.requestOptions);
			for(Object response : resp.getResults()) {
				listed = (String[])response;
			}
			
			// Add active sessions to this servers session list
			sessions.clear();
			for(int x=0; x < listed.length; x=x+2) {
				sessions.add(new tickets(listed[x], listed[x+1]));
			}
		}
		catch(Exception e) {
			System.out.println("GET-SESSIONS : An error has occured\n\n" + e);
		}
	}
	
	// Return list of currently active auctions as a String[]
	public static String[] getAuctions() {
		String[] listed = new String[auctions.size()*8];
		int x = -1;
		for(auctionState a : auctions) {
			listed[x+=1] = Integer.toString(a.aID);
			listed[x+=1] = Double.toString(a.startPrice);
			listed[x+=1] = a.description;
			listed[x+=1] = Double.toString(a.reservePrice);
			listed[x+=1] = Double.toString(a.currentBid);
			listed[x+=1] = a.buyerName;
			listed[x+=1] = a.buyerEmail;
			listed[x+=1] = a.owner;
		}
		return listed;
	}
	
	// JGroups cluster call on getAuctions()
	public static void clusterGetAuctions() {
		try{
			// Get cluster co-ordinator address
			View view = getChan().getView();
			List<Address> addressList = new ArrayList<>();
			Address first = view.getMembers().get(0);
			addressList.add(first);
			
			// Call getAuctions() on cluster co-ordinator to get list of currently active auctions
			String[] listed = null;
			RspList resp = getDisp().callRemoteMethods(addressList,"getAuctions",null,null,auctionServer.requestOptions);
			for(Object response : resp.getResults()) {
				listed = (String[])response;
			}
			
			// Add active auctions to this servers auctions list
			auctions.clear();
			for(int x=0; x < listed.length; x=x+8) {
				auctions.add(new auctionState(Integer.parseInt(listed[x]),Double.parseDouble(listed[x+1]),listed[x+2],Double.parseDouble(listed[x+3]),Double.parseDouble(listed[x+4]),listed[x+5],listed[x+6],listed[x+7]));
			}
		}
		catch(Exception e) {
			System.out.println("GET-AUCTIONS : An error has occured\n\n" + e);
		}
	}
	
	// Gets information from the linked list about the current active auctions,
	// and holds this information in a string, which is then returned..
	// used in client's browse() method
	public static String advertiseList(String ticket) {	
		if(checkTicket(ticket) == true ) {
			String b = "";
		// Go through all auctions and only get the relevant information
			for( auctionState n : auctions) {
				if(n.currentBid == -1) {
					b = b + "Auction ID: " + n.aID + " Info: " + n.description + " Current price: " + n.startPrice + " Current top bidder: none" + "\n";
				}
				else if(n.currentBid != -1) {
					b = b + "Auction ID: " + n.aID + " Info: " + n.description + " Current price:" + n.currentBid + " Current top bidder: " + n.buyerName +  "\n";
				}
			}
			return b;
		}
		return null;
	}
	
	// JGroups cluster call to get all current auctions
	public static String clusterAdvertiseList(String ticket) throws RemoteException {
		try{
			// Call all clusters and store their responses on a LinkedList
			LinkedList<String> answers = new LinkedList<String>();
			RspList resp =  getDisp().callRemoteMethods(null,"advertiseList",new Object[]{ticket},new Class[]{String.class},auctionServer.requestOptions);
            for(Object response : resp.getResults() ) {
                answers.add((String)response);
			}
			
			// Check if all results match
			for(int k=1;k<answers.size(); k++) {
				if(!answers.get(0).equals(answers.get(k))) {
					System.out.println("ERROR: Results don't match");
					return null;
				}
			}
			
			// All results are equal, so just return any for the client
			return answers.get(0);
		}
		catch(Exception e) {
			System.out.println("BROWSE : An error has occured\n\n" + e);
		}
		return null;
	}
	
	// Adds an auction to the linked list with the info provided,
	// used in seller's createAuction() method
	public static int addToList(String ticket, String name, double startPrice, String description, double reservePrice) {
		if(checkTicket(ticket) == true ) {
			// If linked list is empty, start with auction ID at 0,
			int auctionID = 0;
			if(auctions.isEmpty() == true) {
				auctionID = 0;
			}
			// else check if there are gaps in between auction ID's and fill them in,
			// eg. auctions 0,1,3,4 --> create new at 2
			// if all in order, create at the end..
			else {
				int y = 0;
				for(int x = 0; x <= Integer.MAX_VALUE; x++) {
					if(auctions.size() > y) {
						if (x != auctions.get(y).aID) {
							auctionID = x;
							break;
						}
					y++;
					}
					else {
						auctionID = x;
						break;
					}
				}
			}
			//add to linked list at auction ID index, and return the ID to user
			auctions.add(auctionID, new auctionState(auctionID, startPrice, description, reservePrice, -1, null, null, name));
			return auctionID;
		}
		return -1;
	}
	
	// JGroups cluster call to create auction
	public static int clusterCreateAuction(String ticket, String name, double startPrice, String description, double reservePrice) throws RemoteException {
		int ans = -2;
		try{
			// Call addToList() on all cluster members and store results in LinkedList
			LinkedList<Integer> answers = new LinkedList<Integer>();
			RspList resp =  getDisp().callRemoteMethods(null,"addToList",new Object[]{ticket, name, startPrice, description, reservePrice},new Class[]{String.class, String.class, double.class, String.class, double.class},auctionServer.requestOptions);
            for(Object response : resp.getResults() ) {
                answers.add((int)response);
			}
			
			// Check if all results match
			for(int k=1;k<answers.size(); k++) {
				if(answers.get(0) != answers.get(k)) {
					return -1;
				}
			}
			
			// All results are equal so just return any for the client
			return answers.get(0);
		}
		catch(Exception e) {
			System.out.println("CREATE-AUCTION : An error has occured\n\n" + e);
		}
		return ans;
	}
	
	// Removes the auction from the linked list,
	// used in seller's closeAuction() method
	public static String[] closeAuction(String ticket, int ID) {
		if(checkTicket(ticket) == true ) {
			// iterate through linked lists, check if submitted auction ID exists..
			for ( auctionState n : auctions) {
				if(n.aID == ID) {
					int index = auctions.indexOf(n);
					// check if the ticket holder is the owner
					if(n.owner.equals(getOwner(ticket))){
						// if reserve price was reached, return string array with winners info
						if(n.currentBid >= n.reservePrice) {
							String[] winners = new String[] {n.buyerName, n.buyerEmail, Double.toString(n.currentBid)};
							auctions.remove(index);
							return winners;
						}
						// else, return string array with "NOT SOLD" -> this is checked at seller client side
						else {
							String[] winners = new String[] {"NOT SOLD"};
							auctions.remove(index);
							return winners;
						}
					}
					else {
						// Client who invoked request is not the owner
						return new String[] {"NOT OWNER"};
					}
				}
			}
			// ID provided doesn't exist, so return empty string array
			String[] winners = new String[] {"ERROR"};
			return winners;
		}
		// Client who invoked request has no valid ticket
		return new String[] {"NO TICKET"};
	}
	
	// JGroups cluster call to close auction
	public static String[] clusterCloseAuction(String ticket, int id) {
		try{
			// Call closeAuction() on all cluster members and store results in a LinkedList
			LinkedList<String[]> answers = new LinkedList<String[]>();
			RspList resp =  getDisp().callRemoteMethods(null,"closeAuction",new Object[]{ticket, id},new Class[]{String.class, int.class},auctionServer.requestOptions);
            for(Object response : resp.getResults() ) {
				answers.add((String[])response);
			}
			
			// Check if all results match
			for(int k=1;k<answers.size(); k++) {
				if(!(answers.get(0)[0].equals(answers.get(k)[0]))) {
					System.out.println("An error has occured.. server results don't match!");
					return null;
				}
			}
			
			// All results are equal so just return any for client
			return answers.get(0);
		}
		catch(Exception e) {
			System.out.println("CLOSE-AUCTION : An error has occured\n\n" + e);
		}
		return null;
	}
	
	// Updates the auction with the provided info if successful,
	// used in buyer's bid() method
	public static int bid(String ticket, int auctionID, double sum, String buyerName, String buyerEmail) {
		// check if auction ID provided exists
		if(checkTicket(ticket) == true ) {
			for (auctionState n : auctions) {
				if(n.aID == auctionID) {
					int index = auctions.indexOf(n);
					// if no bids, we can win bid by matching starting price
					if(n.currentBid == -1) {
						if(sum >= n.startPrice) {
							auctions.set(index, new auctionState(n.aID, n.startPrice, n.description, n.reservePrice, sum, buyerName, buyerEmail, n.owner));
							return 2;
						}
					}
					// if bids have been submitted, we can win only by surpassing current highest bid
					else if(n.currentBid != -1) {
						// if successful, update info
						if(sum > n.currentBid) {
							auctions.set(index, new auctionState(n.aID, n.startPrice, n.description, n.reservePrice, sum, buyerName, buyerEmail, n.owner));
							return 2;
						}
						// bid unsuccessful
						else if(sum <= n.currentBid) {
							return 1;
						}
					}
				}
			}
		}
		// provided auction ID doesn't exist or no session found
		return 3;
	}
	
	// JGroups cluster call to bid on item
	public static int clusterBid(String ticket, int auctionID, double sum, String buyerName, String buyerEmail) throws RemoteException {
		try{
			// Call bid() on all cluster members and store results in a LinkedList
			LinkedList<Integer> answers = new LinkedList<Integer>();
			RspList resp = getDisp().callRemoteMethods(null,"bid",new Object[]{ticket, auctionID, sum, buyerName, buyerEmail},new Class[]{String.class, int.class, double.class, String.class, String.class},auctionServer.requestOptions);
            for(Object response : resp.getResults() ) {
                answers.add((int)response);
			}
			
			// Check if all results match
			for(int k=1;k<answers.size(); k++) {
				if(answers.get(0) != answers.get(k)) {
					System.out.println("ERROR: Results don't match");
					return -4;
				}
			}
			
			// All results are equal so just return any for client
			return answers.get(0);
		}
		catch(Exception e) {
			System.out.println("BID : An error has occured\n\n" + e);
		}
		return -4;
	}
	
	// Add a currently active session
	public static void addTicket(String name, String id) {
		sessions.addLast(new tickets(name, id));
	}
	
	// JGroups cluster call to add an active session
	public static void clusterAddTicket(String name, String id) throws RemoteException {
		try{
			// Call addTicket() on all cluster members
			RspList resp =  getDisp().callRemoteMethods(null,"addTicket",new Object[]{name, id},new Class[]{String.class, String.class},auctionServer.requestOptions);
		}
		catch(Exception e) {
			System.out.println("ADD-TICKET : An error has occured\n\n" + e);
		}
	}
	
	// Removes a session ID from the sessions linked list
	public static void closeSession(String ticket) {
		for(tickets t : sessions) {
			if(t.sessionID.equals(ticket)) {
				int index = sessions.indexOf(t);
				sessions.remove(index);
			}
		}
	}
	
	// JGroups cluster call to remove session from active sessions linked list
	public static void clusterCloseSession(String ticket) {
		try{
			// Call closeSession() on all cluster members
			RspList resp =  getDisp().callRemoteMethods(null,"closeSession",new Object[]{ticket},new Class[]{String.class},auctionServer.requestOptions);
		}
		catch(Exception e) {
			System.out.println("CLOSE-SESSION : An error has occured\n\n" + e);
		}
	}
	
	// Checks whether the ticket is valid against the sessions linked list,
	// return true(ticket valid) or false(ticket not valid)
	private static boolean checkTicket(String ticket) {
		for(tickets t : sessions) {
			if(t.sessionID.equals(ticket)) {
				return true;
			}
		}
		return false;
	}
	
	// Returns the name of the ticket owner
	private static String getOwner(String ticket) {
		for(tickets t : sessions) {
			if(t.sessionID.equals(ticket)) {
				return t.name;
			}
		}
		return null;
	}

	// Get latest cluster state
	public static void main(String args[]) {
		clusterGetSessions();
		clusterGetAuctions();
	}
	
}