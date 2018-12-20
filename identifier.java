import java.io.*;

// Identifier class
// Contains the challenge and its solver (Protection against reflection attacks)
class identifier implements Serializable {
	private String solver;
	private String challenge;
	
	identifier(String s, String c) {
		solver = s;
		challenge = c;
	}
	
	// returns the name of the challenge solver
	public String getSolver() {
		return solver;
	}
	
	// returns the challenge
	public String getChallenge() {
		return challenge;
	}
}