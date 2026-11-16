package inputValidation;

/*******
 * <p> Title: InputValidationTestingAutomation Class. </p>
 * 
 * <p> Description: Automated testbed for validating Password, Email, UserName, 
 * and Name input recognizers for TP1.</p>
 * 
 * @author Virgil Jones &amp; Team Fall 2026
 * 
 * @version 1.00	2026-09-20 Input Validation Test Automation for TP1
 * 
 */
public class InputValidationTestingAutomation {

	/**
	 * Default constructor is not used; every member is static.
	 */
	public InputValidationTestingAutomation() {
	}

	/** Counter of the number of passed tests. */
	static int numPassed = 0;	// Counter of the number of passed tests
	/** Counter of the number of failed tests. */
	static int numFailed = 0;	// Counter of the number of failed tests

	/**
	 * This mainline displays a header to the console, performs a sequence of
	 * test cases, and then displays a footer with a summary of the results
	 * 
	 * @param args not used
	 */
	public static void main(String[] args) {
		/************** Test cases semi-automation report header **************/
		System.out.println("______________________________________");
		System.out.println("\nTest Automation - TP1 Input Validation");

		/************** PASSWORD RECOGNIZER TESTS **************/
		System.out.println("______________________________________");
		System.out.println("\nPASSWORD RECOGNIZER TESTS");
		
		// Pass: Valid password
		performPasswordTestCase(1, "Aa!15678", true);
		
		// Fail: Too short (< 8 characters)
		performPasswordTestCase(2, "A!1a", false);
		
		// Fail: Missing lower case
		performPasswordTestCase(3, "AA!15678", false);
		
		// Fail: Missing upper case
		performPasswordTestCase(4, "aa!15678", false);
		
		// Fail: Missing numeric digit
		performPasswordTestCase(5, "Aa!bcdef", false);
		
		// Fail: Missing special character
		performPasswordTestCase(6, "Aa115678", false);
		
		// Fail: Exceeds length (32 characters)
		performPasswordTestCase(7, "Aa!15678Aa!15678Aa!15678Aa!15678X", false);
		
		// Fail: Empty password
		performPasswordTestCase(8, "", false);


		/************** USERNAME RECOGNIZER TESTS **************/
		System.out.println("______________________________________");
		System.out.println("\nUSERNAME RECOGNIZER TESTS");
		
		// Pass: Valid plain username
		performUserNameTestCase(9, "ValidUser1", true);
		
		// Pass: Valid username with allowed separators
		performUserNameTestCase(10, "user-name_1", true);
		
		// Fail: Starts with a numeric digit (must start with A-Z, a-z)
		performUserNameTestCase(11, "1InvalidStart", false);
		
		// Fail: Too short (< 4 characters)
		performUserNameTestCase(12, "usr", false);
		
		// Fail: Ends with a separator
		performUserNameTestCase(13, "testuser_", false);
		
		// Fail: Contains illegal character
		performUserNameTestCase(14, "user name", false);
		
		// Fail: Empty username
		performUserNameTestCase(15, "", false);


		/************** EMAIL ADDRESS RECOGNIZER TESTS **************/
		System.out.println("______________________________________");
		System.out.println("\nEMAIL ADDRESS RECOGNIZER TESTS");
		
		// Pass: Valid email address
		performEmailTestCase(16, "emailUser@organization.com", true);
		
		// Pass: Valid email with multiple TLD
		performEmailTestCase(17, "email.user@sub.domain.co", true);
		
		// Fail: Missing @ and domain
		performEmailTestCase(18, "emailUser", false);
		
		// Fail: Missing username before domain
		performEmailTestCase(19, "@organization.com", false);
		
		// Fail: Missing TLD
		performEmailTestCase(20, "emailUser@organization", false);
		
		// Fail: Domain exceeds maximum characters
		performEmailTestCase(21, "lrc@1234567890123456789012345678901234567890123456789012345678901234567890.com", false);
		
		// Fail: Empty email
		performEmailTestCase(22, "", false);


		/************** NAME INPUT RECOGNIZER TESTS **************/
		System.out.println("______________________________________");
		System.out.println("\nNAME INPUT RECOGNIZER TESTS");
		
		// Pass: Valid first name
		performNameTestCase(23, "Billy", "First Name", true, true);
		
		// Pass: Name with hyphen or apostrophe
		performNameTestCase(24, "O'Mary-Ann", "First Name", true, true);
		
		// Pass: Name with internal space
		performNameTestCase(25, "Billy Bob", "Last Name", true, true);
		
		// Fail: Must start with an alphabetic letter
		performNameTestCase(26, "-Jones", "Last Name", false, true);
		
		// Fail: Cannot end with a space, hyphen, or apostrophe
		performNameTestCase(27, "Smith-", "Last Name", false, true);
		
		// Fail: Contains numeric characters
		performNameTestCase(28, "Billy123", "First Name", false, true);
		
		// Fail: Required name field cannot be empty (if set to required)
		performNameTestCase(29, "", "First Name", false, true);

		/************** Test cases semi-automation report footer **************/
		System.out.println("______________________________________");
		System.out.println();
		System.out.println("Number of tests passed: " + numPassed);
		System.out.println("Number of tests failed: " + numFailed);
	}

	
	/*
	 * This method sets up the input value for the test from the input parameters,
	 * displays test execution information, invokes precisely the same recognizer
	 * that the interactive JavaFX mainline uses, interprets the returned value,
	 * and displays the interpreted result.
	 */

	/**
	 * Password Test: runs one password test case and reports the result.
	 * @param testCase	the test case number
	 * @param inputText	the input to check
	 * @param expectedPass	true if the input should be accepted
	 */
	private static void performPasswordTestCase(int testCase, String inputText, boolean expectedPass) {
		System.out.println("____________________________________________________________________________\n\nTest case: " + testCase);
		System.out.println("Input: \"" + inputText + "\"");
		System.out.println("______________");
		System.out.println("\nFinite state machine execution trace:");
		
		String resultText = PasswordRecognizer.evaluatePassword(inputText);
		System.out.println();
		
		evaluateResult("password", inputText, resultText, expectedPass);
		displayPasswordEvaluation();
	}

	/**
	 * UserName Test: runs one username test case and reports the result.
	 * @param testCase	the test case number
	 * @param inputText	the input to check
	 * @param expectedPass	true if the input should be accepted
	 */
	private static void performUserNameTestCase(int testCase, String inputText, boolean expectedPass) {
		System.out.println("____________________________________________________________________________\n\nTest case: " + testCase);
		System.out.println("Input: \"" + inputText + "\"");
		System.out.println("______________");
		System.out.println("\nFinite state machine execution trace:");
		
		String resultText = UserNameRecognizer.checkForValidUserName(inputText);
		System.out.println();
		
		evaluateResult("username", inputText, resultText, expectedPass);
	}

	/**
	 * Email Test: runs one email address test case and reports the result.
	 * @param testCase	the test case number
	 * @param inputText	the input to check
	 * @param expectedPass	true if the input should be accepted
	 */
	private static void performEmailTestCase(int testCase, String inputText, boolean expectedPass) {
		System.out.println("____________________________________________________________________________\n\nTest case: " + testCase);
		System.out.println("Input: \"" + inputText + "\"");
		System.out.println("______________");
		System.out.println("\nFinite state machine execution trace:");
		
		String resultText = EmailAddressRecognizer.checkEmailAddress(inputText);
		System.out.println();
		
		evaluateResult("email address", inputText, resultText, expectedPass);
	}

	/**
	 * Name Input Test: runs one name test case and reports the result.
	 * @param testCase	the test case number
	 * @param inputText	the input to check
	 * @param fieldName	the name field being tested (for the report)
	 * @param expectedPass	true if the input should be accepted
	 * @param isRequired	true if the field may not be empty
	 */
	private static void performNameTestCase(int testCase, String inputText, String fieldName, boolean expectedPass, boolean isRequired) {
		System.out.println("____________________________________________________________________________\n\nTest case: " + testCase);
		System.out.println("Input: \"" + inputText + "\" (" + fieldName + ")");
		System.out.println("______________");
		System.out.println("\nFinite state machine execution trace:");
		
		String resultText = NameInputRecognizer.checkForValidName(inputText, fieldName, isRequired);
		System.out.println();
		
		evaluateResult(fieldName, inputText, resultText, expectedPass);
	}

	/**
	 * Evaluate Function: compares a recognizer's result with the expected outcome, prints it,
	 * and updates the pass and fail counters.
	 * @param itemType	the kind of input tested
	 * @param inputText	the input that was checked
	 * @param resultText	the recognizer's message (empty means valid)
	 * @param expectedPass	true if the input should have been accepted
	 */
	private static void evaluateResult(String itemType, String inputText, String resultText, boolean expectedPass) {
		if (resultText != null && resultText != "") {
			if (expectedPass) {
				System.out.println("***Failure*** The " + itemType + " <" + inputText + "> is invalid." + 
						"\nBut it was supposed to be valid, so this is a failure!\n");
				System.out.println("Error message: " + resultText);
				numFailed++;
			} else {
				System.out.println("***Success*** The " + itemType + " <" + inputText + "> is invalid." + 
						"\nBut it was supposed to be invalid, so this is a pass!\n");
				System.out.println("Error message: " + resultText);
				numPassed++;
			}
		} else {
			if (expectedPass) {
				System.out.println("***Success*** The " + itemType + " <" + inputText + 
						"> is valid, so this is a pass!");
				numPassed++;
			} else {
				System.out.println("***Failure*** The " + itemType + " <" + inputText + 
						"> was judged as valid" + 
						"\nBut it was supposed to be invalid, so this is a failure!");
				numFailed++;
			}
		}
	}

	/**
	 * Password requirements: prints which password requirements were and were not met.
	 */
	private static void displayPasswordEvaluation() {
		if (PasswordRecognizer.foundUpperCase)
			System.out.println("At least one upper case letter - Satisfied");
		else
			System.out.println("At least one upper case letter - Not Satisfied");

		if (PasswordRecognizer.foundLowerCase)
			System.out.println("At least one lower case letter - Satisfied");
		else
			System.out.println("At least one lower case letter - Not Satisfied");

		if (PasswordRecognizer.foundNumericDigit)
			System.out.println("At least one digit - Satisfied");
		else
			System.out.println("At least one digit - Not Satisfied");

		if (PasswordRecognizer.foundSpecialChar)
			System.out.println("At least one special character - Satisfied");
		else
			System.out.println("At least one special character - Not Satisfied");

		if (PasswordRecognizer.foundLongEnough)
			System.out.println("At least 8 characters - Satisfied");
		else
			System.out.println("At least 8 characters - Not Satisfied");
	}
}