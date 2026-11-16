package inputValidation;

/*******
 * <p> Title: NameInputRecognizer </p>
 * 
 * <p> Description: A demonstration of the mechanical translation of a Finite State Machine 
 * diagram into an executable Java program for validating human names. 
 * 
 * Requirements:
 * - Length: 1 to 100 characters.
 * - Allowed: Alphabetic letters, single space (between two characters), apostrophes, and hyphens.
 * - Rules: Must start and end with a letter. Consecutive separators are disallowed.</p>
 * 
 * <p> Copyright: Lynn Robert Carter © 2024 </p>
 * 
 * @author Lynn Robert Carter
 * @author Virgil Jones &amp; Team Fall 2026
 * 
 * @version 1.00		2026-09-16 Initial version for TP1 name validation
 * 
 */
public class NameInputRecognizer {

	/**
	 * Default constructor is not used; every member is static.
	 */
	public NameInputRecognizer() {
	}
	
	/**********************************************************************************************
	 * 
	 * Result attributes to be used for GUI applications where a detailed error message and a 
	 * pointer to the character of the error will enhance the user experience.
	 * 
	 */
	
	/** The error message text. */
	public static String nameRecognizerErrorMessage = "";	// The error message text
	/** The input being processed. */
	public static String nameRecognizerInput = "";			// The input being processed
	/** The index of the error location. */
	public static int nameRecognizerIndexofError = -1;		// The index of error location
	/** The current state value. */
	private static int state = 0;						// The current state value
	/** The next state value. */
	private static int nextState = 0;					// The next state value
	/** Is this state a final state? */
	private static boolean finalState = false;			// Is this state a final state?
	/** The input line. */
	private static String inputLine = "";				// The input line
	/** The current character in the line. */
	private static char currentChar;					// The current character in the line
	/** The index of the current character. */
	private static int currentCharNdx;					// The index of the current character
	/** The flag that specifies if the FSM is running. */
	private static boolean running;						// The flag that specifies if the FSM is 
														// running
	/** A name may not exceed 100 characters. */
	private static int nameSize = 0;			// A numeric value may not exceed 100 characters

	

	// Private method to display debugging data
		/** Displays the current state of the FSM as one line of the execution trace. */
		private static void displayDebuggingInfo() {
			// Display the current state of the FSM as part of an execution trace
			if (currentCharNdx >= inputLine.length())
				// display the line with the current state numbers aligned
				System.out.println(((state > 99) ? " " : (state > 9) ? "  " : "   ") + state + 
						((finalState) ? "       F   " : "           ") + "None");
			else
				System.out.println(((state > 99) ? " " : (state > 9) ? "  " : "   ") + state + 
					((finalState) ? "       F   " : "           ") + "  " + currentChar + " " + 
					((nextState > 99) ? "" : (nextState > 9) || (nextState == -1) ? "   " : "    ") + 
					nextState + "     " + nameSize);
		}
		
		// Private method to move to the next character within the limits of the input line
		/** Moves to the next character within the limits of the input line. */
		private static void moveToNextCharacter() {
			currentCharNdx++;
			if (currentCharNdx < inputLine.length())
				currentChar = inputLine.charAt(currentCharNdx);
			else {
				currentChar = ' ';
				running = false;
			}
		}

	/**********
	 * This method is a mechanical transformation of a Finite State Machine diagram into a Java
	 * method.
	 * 
	 * @param input		The input string for the Finite State Machine
	 * @param fieldName The name of the field (e.g., "First Name", "Last Name")
	 * @param required	Check if the field input is required (true) or can remain empty (false)
	 * @return			Empty string if valid, or a descriptive error message if invalid
	 */
	public static String checkForValidName(String input, String fieldName, boolean required) {
		// Check if null, empty, or whitespace
		if (input == null || input.trim().isEmpty()) {
			if(required) {
				nameRecognizerIndexofError = 0;
				return fieldName + " cannot be empty or blank.";
			}
			return "";
		} 
		
		// The local variables used to perform the Finite State Machine simulation
		state = 0;							// This is the FSM state number
		inputLine = input;					// Save the reference to the input line as a global
		currentCharNdx = 0;					// The index of the current character
		currentChar = input.charAt(0);		// The current character from above indexed position

		// The Finite State Machines continues until the end of the input is reached or at some 
		// state the current character does not match any valid transition to a next state

		nameRecognizerInput = input;	// Save a copy of the input
		running = true;						// Start the loop
		nextState = -1;						// There is no next state
		System.out.println("\nCurrent Final Input  Next\nState   State Char  State  Size");
		
		// This is the place where semantic actions for a transition to the initial state occur
		
		nameSize = 0;					// Initialize the UserName size

		// The Finite State Machines continues until the end of the input is reached or at some 
		// state the current character does not match any valid transition to a next state
		while (running) {
			// The switch statement takes the execution to the code for the current state, where
			// that code sees whether or not the current character is valid to transition to a
			// next state
			switch (state) {
			case 0: 
				// State 0: Must start with an alphabetic letter
				if (Character.isLetter(currentChar)) {
					nextState = 1;
					nameSize++;
				} else {
					running = false;
				}
				break;
			
			case 1: 
				// State 1 (Final State): An alphabetic character was seen.
				// Next can be another letter (State 1) or a separator (State 2)
				if (Character.isLetter(currentChar)) {
					nextState = 1;
					nameSize++;
				} else if (currentChar == ' ' || currentChar == '-' || currentChar == '\'') {
					nextState = 2;
					nameSize++;
				} else {
					running = false;
				}

				if (nameSize > 100) {
					running = false;
				}
				break;			
				
			case 2: 
				// State 2: A separator must be followed by a letter
				if (Character.isLetter(currentChar)) {
					nextState = 1;
					nameSize++;
				} else {
					running = false;
				}

				if (nameSize > 100) {
					running = false;
				}
				break;			
			}
			
			if (running) {
				displayDebuggingInfo();
				// When the processing of a state has finished, the FSM proceeds to the next
				// character in the input and if there is one, it fetches that character and
				// updates the currentChar.  If there is no next character the currentChar is
				// set to a blank.
				moveToNextCharacter();
				
				// Move to the next state
				state = nextState;
				// State 1 is the only final state (ends with a letter)
				finalState = (state == 1);
				// Ensure that one of the cases sets this to a valid value
				nextState = -1;
			}
			// Should the FSM get here, the loop starts again
		}
		
		displayDebuggingInfo();
		
		System.out.println("The loop has ended.");
		
		// When the FSM halts, we must determine if the situation is an error or not.  That depends
		// of the current state of the FSM and whether or not the whole string has been consumed.
		// This switch directs the execution to separate code for each of the FSM states and that
		// makes it possible for this code to display a very specific error message to improve the
		// user experience.
		nameRecognizerIndexofError = currentCharNdx;
		
		switch (state) {
		case 0:
			// State 0 is not a final state, so we can return a very specific error message
			return fieldName + " must start with an alphabetic letter.";

		case 1:
			// State 1 is a final state.  Check to see if the name length is valid.
			if (nameSize > 100) {
				return fieldName + " must not exceed 100 characters.";
			} else if (currentCharNdx < input.length()) {
				return fieldName + " contains an invalid character: '" + currentChar + "'. Only letters, spaces, hyphens, and apostrophes are allowed.";
			} else {
				nameRecognizerIndexofError = -1;
				nameRecognizerErrorMessage = "";
				return ""; // Valid!
			}

		case 2:
			// State 2 is not a final state, so we can return a very specific error message
			return fieldName + " cannot end with a space, hyphen, or apostrophe, nor contain consecutive symbols.";
			
		default:
			// This is for the case where we have a state that is outside of the valid range.
			// This should not happen
			return "Invalid input format for " + fieldName + ".";
		}
	}
	
		/**********
		 * Overloaded method for backward compatibility (default to required as false)
		 * 
		 * @param input     The input string for the Finite State Machine
		 * @param fieldName The name of the field (e.g., "First Name", "Last Name")
		 * @return          Empty string if valid, or a descriptive error message if invalid
		 */
		public static String checkForValidName(String input, String fieldName) {
			return checkForValidName(input, fieldName, false);
		}
}