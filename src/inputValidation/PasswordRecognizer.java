package inputValidation;

/*******
 * <p> Title: PasswordRecognizer Class - Password Evaluation.
 * </p>
 *
 * <p> Description: Evaluates character categories (uppercase, lowercase, number, special character), 
 * rejects spaces and invalid symbols, and flags passwords exceeding the maximum length.
 * 
 * 
 * This is a purely static component of the MVC implementation.  There is no need to instantiate
 * the class.
 *
 * <p> Copyright: Lynn Robert Carter © 2025 </p>
 *
 * @author Lynn Robert Carter
 * @author Virgil Jones &amp; Team Fall 2026
 *
 * @version 2.00	2025-07-30 Rewrite of this application for the Fall 2025 offering of CSE 360
 * and other ASU courses.
 * @version 3.00	2026-09-16 Removed GUI View/Model components; added max length; integrated into inputValidation package.
 * @version 3.01	2026-11-16 TP3 B-20: fixed HW3 defects D1 (a null input threw a
 * NullPointerException), D2 (the five flags were not reset when an empty or over-long input
 * returned early), and D3 (passwordErrorMessage never held the returned message); validated by
 * hw3Tests.PasswordRecognizerTest PW-23 to PW-25.
 */

public class PasswordRecognizer {

	/**
	 * Default constructor is not used; every member is static.
	 */
	public PasswordRecognizer() {
	}
		
	
	/*-********************************************************************************************
	 * 
	 * Attributes used by the Finite State Machine to inform the user about what was and was not
	 * valid and point to the character of the error.  This will enhance the user experience.
	 * 
	 */

	/** The error message text. */
	public static String passwordErrorMessage = "";		// The error message text
	/** The input being processed. */
	public static String passwordInput = "";			// The input being processed
	/** The index where the error was located. */
	public static int passwordIndexofError = -1;		// The index where the error was located
	/** True once an upper case letter has been found. */
	public static boolean foundUpperCase = false;
	/** True once a lower case letter has been found. */
	public static boolean foundLowerCase = false;
	/** True once a numeric digit has been found. */
	public static boolean foundNumericDigit = false;
	/** True once a special character has been found. */
	public static boolean foundSpecialChar = false;
	/** True once the password is long enough. */
	public static boolean foundLongEnough = false;
	/** The input line. */
	private static String inputLine = "";				// The input line
	/** The current character in the line. */
	private static char currentChar;					// The current character in the line
	/** The index of the current character. */
	private static int currentCharNdx;					// The index of the current character
	/** The flag that specifies if the FSM is running. */
	private static boolean running;						// The flag that specifies if the FSM is 
														// running
	/** Max password character length. */
	public static final int MAX_PASS_LENGTH = 32;		// Max password character length

	/**
	 * This private method displays the input line and then on a line under it displays the input
	 * up to the point of the error.  At that point, a question mark is place and the rest of the 
	 * input is ignored. This method is designed to be used to display information to make it clear
	 * to the user where the error in the input can be found, and show that on the console 
	 * terminal.
	 * 
	 */

	private static void displayInputState() {
		// Display the entire input line
		System.out.println(inputLine);
		System.out.println(inputLine.substring(0,currentCharNdx) + "?");
		System.out.println("The password size: " + inputLine.length() + "  |  The currentCharNdx: " + 
				currentCharNdx + "  |  The currentChar: \"" + currentChar + "\"");
	}
	
	/**********
	 * <p> Title: evaluatePassword - Public Method </p>
	 * 
	 * <p> Description: This method is a mechanical transformation of a Directed Graph diagram 
	 * into a Java method. This method is used by both the GUI version of the application as well
	 * as the testing automation version.
	 * 
	 * @param input		The input string evaluated by the directed graph processing
	 * @return			An output string that is empty if every things is okay or it will be
	 * 						a string with a helpful description of the error follow by two lines
	 * 						that shows the input line follow by a line with an up arrow at the
	 *						point where the error was found.
	 */
	
	public static String evaluatePassword(String input) {
		// The following are the local variable used to perform the Directed Graph simulation
		passwordErrorMessage = "";
		passwordIndexofError = 0;			// Initialize the IndexofError
		currentCharNdx = 0;					// The index of the current character

		// A null input is treated as empty, so no input can make this method throw (defect D1,
		// CWE-476; test PW-24)
		if (input == null) input = "";
		inputLine = input;					// Save the reference to the input line as a global

		// The Directed Graph simulation continues until the end of the input is reached or at some
		// state the current character does not match any valid transition to a next state.  This
		// local variable is a working copy of the input.
		passwordInput = input;				// Save a copy of the input

		// The following are the attributes associated with each of the requirements.  They are
		// reset before any check can return, so after every call they describe the input just
		// evaluated.  The live password checklists color themselves from these flags, so stale
		// values would show requirements as met that are not (defect D2; test PW-23).
		foundUpperCase = false;				// Reset the Boolean flag
		foundLowerCase = false;				// Reset the Boolean flag
		foundNumericDigit = false;			// Reset the Boolean flag
		foundSpecialChar = false;			// Reset the Boolean flag
		foundLongEnough = false;			// Reset the Boolean flag

		if (input.length() <= 0) {
			return fail("*** Error *** The password is empty!");
		}

		// Error for if password exceeds MAX_PASS_LENGTH
		if (input.length() > MAX_PASS_LENGTH) {
			return fail("*** Error *** The password exceeds the maximum allowed length of "
					+ MAX_PASS_LENGTH + " characters!");
		}

		// The input is not empty, so we can access the first character
		currentChar = input.charAt(0);		// The current character from the above indexed position

		// This flag determines whether the directed graph (FSM) loop is operating or not
		running = true;						// Start the loop

		// The Directed Graph simulation continues until the end of the input is reached or at some
		// state the current character does not match any valid transition
		while (running) {
			displayInputState();
			// The cascading if statement sequentially tries the current character against all of
			// the valid transitions, each associated with one of the requirements
			if (currentChar >= 'A' && currentChar <= 'Z') {
				System.out.println("Upper case letter found");
				foundUpperCase = true;
			} else if (currentChar >= 'a' && currentChar <= 'z') {
				System.out.println("Lower case letter found");
				foundLowerCase = true;
			} else if (currentChar >= '0' && currentChar <= '9') {
				System.out.println("Digit found");
				foundNumericDigit = true;
			} else if ("~`!@#$%^&*()_-+={}[]|\\:;\"'<>,.?/".indexOf(currentChar) >= 0) {
				System.out.println("Special character found");
				foundSpecialChar = true;
			} else {
				passwordIndexofError = currentCharNdx;
				return fail("*** Error *** An invalid character has been found!");
			}
			if (currentCharNdx >= 7) {
				System.out.println("At least 8 characters found");
				foundLongEnough = true;
			}
			
			// Go to the next character if there is one
			currentCharNdx++;
			if (currentCharNdx >= inputLine.length())
				running = false;
			else
				currentChar = input.charAt(currentCharNdx);
			
			System.out.println();
		}
		
		// Construct a String with a list of the requirement elements that were found.
		String errMessage = "";
		if (!foundUpperCase)
			errMessage += "Upper case; ";
		
		if (!foundLowerCase)
			errMessage += "Lower case; ";
		
		if (!foundNumericDigit)
			errMessage += "Numeric digits; ";
			
		if (!foundSpecialChar)
			errMessage += "Special character; ";
			
		if (!foundLongEnough)
			errMessage += "Long Enough; ";
		
		// isEmpty() compares the contents; == would compare object references
		if (errMessage.isEmpty())
			return "";

		// If it gets here, there something was not found, so return an appropriate message
		passwordIndexofError = currentCharNdx;
		return fail(errMessage + "conditions were not satisfied");
	}

	/**********
	 * <p> Method: String fail(String message) </p>
	 *
	 * <p> Description: Records an error message in passwordErrorMessage and returns it, so the
	 * public field always holds the message evaluatePassword returned, as its documentation says
	 * (defect D3; test PW-25).  Every error return in evaluatePassword goes through this method,
	 * so no return can forget to set the field. </p>
	 *
	 * @param message the error message to record and return
	 *
	 * @return the same message
	 */
	private static String fail(String message) {
		passwordErrorMessage = message;
		return message;
	}
}
 