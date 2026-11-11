package hw3Tests;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.function.Executable;

import inputValidation.UserNameRecognizer;

/*******
 * <p> Title: UserNameRecognizerTest Class. </p>
 *
 * <p> Description: JUnit 5 boundary value and coverage tests for
 * inputValidation.UserNameRecognizer.checkForValidUserName() (HW3 Task 2.4).  Each test method
 * is one row of the test design in "TP2 Test Designs.pdf", section III.B, and its display name
 * starts with that row's id (UN-01 to UN-28), so the JUnit window lists the tests in the same
 * order as the design.</p>
 *
 * <p> <b>Requirements tested</b> (section III.A of the design): UN-R1 empty input, UN-R2 the
 * first character is a letter, UN-R3 at least 4 characters, UN-R4 at most 32 characters,
 * UN-R5 only letters, digits, and the separators - _ . &amp;, UN-R6 every separator is followed
 * by a letter or digit, UN-R7 "" and index -1 for a valid name, UN-R8 no exception for any
 * input (CWE-476), and UN-R9 the message names the rule that was actually broken.</p>
 *
 * <p> <b>Coverage.</b>  The recognizer is a finite state machine with states 0, 1, and 2.  The
 * tests take every transition out of every state and every exit of the final switch (section
 * III.D of the design).  The only branch no test can reach is the final switch's default case,
 * because the state is only ever 0, 1, or 2.</p>
 *
 * <p> <b>How to read the output.</b>  Every test prints its id, the input, the expected result,
 * and the actual result, then asserts that they are identical.  Each error message starts with a
 * line break, which the console shows as \n.  The recognizer's own state trace is switched off
 * while a test calls it.  The four tests marked "EXPECTED FAIL" (UN-23 to UN-26) are designed to
 * fail because they expose defects D5, D6, and D4 in TP2 Test Designs.pdf, section IV.  HW3 asks
 * for those defects to be listed, not fixed, so the tests are left in place to prove each one.</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-11-01 HW3 Task 2.4: tests for the TP1 username rules
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class UserNameRecognizerTest {

	/** The start of every error message from the final switch. */
	private static final String ERROR = "\n*** ERROR *** ";

	/** The message for an empty username (UN-R1). */
	private static final String EMPTY = "\n*** ERROR *** The input is empty";

	/** The message for a first character that is not a letter (UN-R2). */
	private static final String BAD_START = ERROR + "A UserName must only start with A-Z, a-z.";

	/** The message for fewer than 4 characters (UN-R3). */
	private static final String TOO_SHORT = ERROR + "A UserName must have at least 4 characters.";

	/** The message for more than 32 characters (UN-R4). */
	private static final String TOO_LONG =
			ERROR + "A UserName must have no more than 32 characters.";

	/** The message for a character that is not allowed (UN-R5). */
	private static final String INVALID_CHAR = ERROR + "A UserName character may only contain "
			+ "alphanumeric characters (A-Z, a-z, 0-9), or separators ('-', '_', '.', '&').";

	/** The message for a separator that is not followed by a letter or digit (UN-R6). */
	private static final String BAD_SEPARATOR = ERROR + "A UserName cannot end with a separator, "
			+ "and any '-', '_', '.', or '&' must be followed by an alphanumeric character "
			+ "(A-Z, a-z, 0-9).";

	/**********
	 * Default constructor is not used; JUnit creates an instance for each test.
	 */
	public UserNameRecognizerTest() {
	}


	/*-*******************************************************************************************

	Helper methods

	 */

	/**********
	 * <p> Method: String recognize(String input) </p>
	 *
	 * <p> Description: Calls UserNameRecognizer.checkForValidUserName(input) with the
	 * recognizer's state trace switched off, and returns its result. </p>
	 *
	 * @param input the username to check
	 *
	 * @return "" if the username is valid, otherwise the recognizer's message
	 */
	private static String recognize(String input) {
		// The recognizer prints a table row for every character it reads.  Hiding that trace
		// keeps the Expected and Actual lines readable in the screencast.  The finally block
		// restores the console even when the call throws (UN-26), so later tests still print.
		PrintStream console = System.out;
		System.setOut(new PrintStream(OutputStream.nullOutputStream()));
		try {
			return UserNameRecognizer.checkForValidUserName(input);
		} finally {
			System.setOut(console);
		}
	}

	/**********
	 * <p> Method: void expect(String id, String input, String expected) </p>
	 *
	 * <p> Description: Runs the recognizer on input, prints the Expected and Actual lines, and
	 * asserts that they are identical. </p>
	 *
	 * @param id the test id from the design, such as "UN-03"
	 *
	 * @param input the username to check
	 *
	 * @param expected the required result
	 */
	private static void expect(String id, String input, String expected) {
		print(id, show(input), expected, recognize(input));
	}

	/**********
	 * <p> Method: void expectIndex(String id, int expected) </p>
	 *
	 * <p> Description: Prints and asserts the value of userNameRecognizerIndexofError left by
	 * the most recent call (UN-R7). </p>
	 *
	 * @param id the test id from the design
	 *
	 * @param expected the required index
	 */
	private static void expectIndex(String id, int expected) {
		print(id, "userNameRecognizerIndexofError", String.valueOf(expected),
				String.valueOf(UserNameRecognizer.userNameRecognizerIndexofError));
	}

	/**********
	 * <p> Method: void print(String id, String input, String expected, String actual) </p>
	 *
	 * <p> Description: Prints the Expected and Actual lines for one check and then asserts that
	 * they are identical. </p>
	 *
	 * @param id the test id from the design
	 *
	 * @param input a readable description of the input
	 *
	 * @param expected the required result
	 *
	 * @param actual the result the code produced
	 */
	private static void print(String id, String input, String expected, String actual) {
		// Every error message starts with a line break.  Printing it as \n keeps each result on
		// one line, so Expected and Actual can be compared at a glance.
		System.out.println(id + "  Input:    " + input);
		System.out.println(id + "  Expected: \"" + expected.replace("\n", "\\n") + "\"");
		System.out.println(id + "  Actual:   \"" + actual.replace("\n", "\\n") + "\"");
		System.out.println();
		assertEquals(expected, actual, id);
	}

	/**********
	 * <p> Method: String show(String input) </p>
	 *
	 * <p> Description: Makes an input readable on the console: quoted, with its length. </p>
	 *
	 * @param input the input to describe
	 *
	 * @return the readable form
	 */
	private static String show(String input) {
		if (input == null) return "null";
		return "\"" + input + "\" (length " + input.length() + ")";
	}


	/*-*******************************************************************************************

	UN-R1 and UN-R3: length boundaries at the bottom (0, 3, 4, 5)

	 */

	/**********
	 * <p> Method: void un01_emptyInput() </p>
	 *
	 * <p> Description: UN-01, boundary value length 0.  An empty username is rejected and the
	 * error index is 0 (UN-R1, UN-R7). </p>
	 */
	@Test
	@DisplayName("UN-01 Empty input is rejected (length 0)")
	void un01_emptyInput() {
		expect("UN-01", "", EMPTY);
		expectIndex("UN-01", 0);
	}

	/**********
	 * <p> Method: void un02_threeCharacters() </p>
	 *
	 * <p> Description: UN-02, boundary value minimum - 1.  Three letters are too short. </p>
	 */
	@Test
	@DisplayName("UN-02 Three characters are too short (min - 1)")
	void un02_threeCharacters() {
		expect("UN-02", "abc", TOO_SHORT);
	}

	/**********
	 * <p> Method: void un03_fourCharacters() </p>
	 *
	 * <p> Description: UN-03, boundary value minimum.  Four letters is the shortest valid
	 * username, so the result is "" and the error index is -1 (UN-R3, UN-R7). </p>
	 */
	@Test
	@DisplayName("UN-03 Four characters are accepted (min)")
	void un03_fourCharacters() {
		expect("UN-03", "abcd", "");
		expectIndex("UN-03", -1);
	}

	/**********
	 * <p> Method: void un04_fiveCharacters() </p>
	 *
	 * <p> Description: UN-04, boundary value minimum + 1.  Five letters are accepted. </p>
	 */
	@Test
	@DisplayName("UN-04 Five characters are accepted (min + 1)")
	void un04_fiveCharacters() {
		expect("UN-04", "abcde", "");
	}


	/*-*******************************************************************************************

	UN-R4: length boundaries at the top (31, 32, 33, 41)

	 */

	/**********
	 * <p> Method: void un05_thirtyOneCharacters() </p>
	 *
	 * <p> Description: UN-05, boundary value maximum - 1.  31 characters are accepted. </p>
	 */
	@Test
	@DisplayName("UN-05 31 characters are accepted (max - 1)")
	void un05_thirtyOneCharacters() {
		expect("UN-05", "a" + "b".repeat(30), "");				// 1 + 30 = 31 characters
	}

	/**********
	 * <p> Method: void un06_thirtyTwoCharacters() </p>
	 *
	 * <p> Description: UN-06, boundary value maximum.  32 characters is the longest valid
	 * username (UN-R4). </p>
	 */
	@Test
	@DisplayName("UN-06 32 characters are accepted (max)")
	void un06_thirtyTwoCharacters() {
		expect("UN-06", "a" + "b".repeat(31), "");				// 1 + 31 = 32 characters
	}

	/**********
	 * <p> Method: void un07_thirtyThreeCharacters() </p>
	 *
	 * <p> Description: UN-07, boundary value maximum + 1.  33 characters are rejected, and the
	 * error index points to the 33rd character, index 32 (UN-R4, UN-R7). </p>
	 */
	@Test
	@DisplayName("UN-07 33 characters are rejected (max + 1)")
	void un07_thirtyThreeCharacters() {
		expect("UN-07", "a" + "b".repeat(32), TOO_LONG);		// 1 + 32 = 33 characters
		expectIndex("UN-07", 32);
	}

	/**********
	 * <p> Method: void un08_farOverMaximum() </p>
	 *
	 * <p> Description: UN-08, a value well above the maximum (41).  The machine stops counting
	 * at 33, so a much longer name gets the same message. </p>
	 */
	@Test
	@DisplayName("UN-08 41 characters are rejected (far over max)")
	void un08_farOverMaximum() {
		expect("UN-08", "a" + "b".repeat(40), TOO_LONG);		// 1 + 40 = 41 characters
	}


	/*-*******************************************************************************************

	UN-R2: the first character (state 0)

	 */

	/**********
	 * <p> Method: void un09_digitFirst() </p>
	 *
	 * <p> Description: UN-09, negative test.  A digit cannot start a username; the error is at
	 * index 0.  This is state 0's "anything else" transition and the state 0 exit. </p>
	 */
	@Test
	@DisplayName("UN-09 A digit cannot be first (index 0)")
	void un09_digitFirst() {
		expect("UN-09", "1abc", BAD_START);
		expectIndex("UN-09", 0);
	}

	/**********
	 * <p> Method: void un10_separatorFirst() </p>
	 *
	 * <p> Description: UN-10, negative test.  A separator cannot start a username. </p>
	 */
	@Test
	@DisplayName("UN-10 A separator cannot be first")
	void un10_separatorFirst() {
		expect("UN-10", "_abc", BAD_START);
	}

	/**********
	 * <p> Method: void un11_spaceFirst() </p>
	 *
	 * <p> Description: UN-11, boundary value first position.  A leading space is rejected by
	 * the first-character rule. </p>
	 */
	@Test
	@DisplayName("UN-11 A space cannot be first")
	void un11_spaceFirst() {
		expect("UN-11", " abcd", BAD_START);
	}


	/*-*******************************************************************************************

	UN-R5 and UN-R6: allowed characters and separators (states 1 and 2)

	 */

	/**********
	 * <p> Method: void un12_everySeparator() </p>
	 *
	 * <p> Description: UN-12, positive test.  Each of the four separators is accepted, and the
	 * characters after them include an upper-case letter, a lower-case letter, and a digit.
	 * This takes state 1's separator transition and every part of state 2's letter-or-digit
	 * transition back to state 1. </p>
	 */
	@Test
	@DisplayName("UN-12 Each separator . - _ & is accepted, followed by A-Z, a-z, or 0-9")
	void un12_everySeparator() {
		expect("UN-12", "a.B-c_9&e", "");
	}

	/**********
	 * <p> Method: void un13_rangeEdges() </p>
	 *
	 * <p> Description: UN-13, boundary values of the character ranges.  A, Z, a, z, 0, and 9
	 * are the first and last characters of each range the code tests with &gt;= and &lt;=.  Each
	 * state has its own copy of these tests, so the edges are checked in all three: as the first
	 * character (state 0, letters only), after a letter (state 1), and right after a separator
	 * (state 2). </p>
	 */
	@Test
	@DisplayName("UN-13 Range edges A Z a z 0 9 are accepted in states 0, 1, and 2")
	void un13_rangeEdges() {
		// State 0 accepts only letters, so its four edges each start a name
		expect("UN-13", "Abcd", "");
		expect("UN-13", "Zbcd", "");
		expect("UN-13", "abcd", "");
		expect("UN-13", "zbcd", "");
		// State 1: all six edges after the first letter
		expect("UN-13", "aAZaz09", "");
		// State 2: each edge right after a separator
		expect("UN-13", "a_A-Z.a&z_0-9", "");
	}

	/**********
	 * <p> Method: void un14_separatorLast() </p>
	 *
	 * <p> Description: UN-14, boundary value last position.  A username cannot end with a
	 * separator; this is the state 2 exit.  The index is 5, one past the last character. </p>
	 */
	@Test
	@DisplayName("UN-14 A separator cannot be last (index 5)")
	void un14_separatorLast() {
		expect("UN-14", "abcd_", BAD_SEPARATOR);
		expectIndex("UN-14", 5);
	}

	/**********
	 * <p> Method: void un15_sameSeparatorTwice() </p>
	 *
	 * <p> Description: UN-15, negative test.  Two periods in a row: state 2's "anything else"
	 * transition.  The index points to the second period. </p>
	 */
	@Test
	@DisplayName("UN-15 Two periods in a row are rejected (index 3)")
	void un15_sameSeparatorTwice() {
		expect("UN-15", "ab..cd", BAD_SEPARATOR);
		expectIndex("UN-15", 3);
	}

	/**********
	 * <p> Method: void un16_twoDifferentSeparators() </p>
	 *
	 * <p> Description: UN-16, negative test.  Two different separators in a row are also
	 * rejected. </p>
	 */
	@Test
	@DisplayName("UN-16 Two different separators in a row are rejected (index 5)")
	void un16_twoDifferentSeparators() {
		expect("UN-16", "abcd-_ef", BAD_SEPARATOR);
		expectIndex("UN-16", 5);
	}

	/**********
	 * <p> Method: void un17_separatorCountsTowardLength() </p>
	 *
	 * <p> Description: UN-17, boundary value.  "a-b" has only 3 characters even though one is a
	 * separator, so it is too short. </p>
	 */
	@Test
	@DisplayName("UN-17 Separators count toward the length (a-b is too short)")
	void un17_separatorCountsTowardLength() {
		expect("UN-17", "a-b", TOO_SHORT);
	}

	/**********
	 * <p> Method: void un18_separatorNearMaximum() </p>
	 *
	 * <p> Description: UN-18, boundary value.  A 32-character name with a separator as its
	 * 31st character is valid. </p>
	 */
	@Test
	@DisplayName("UN-18 A separator just before the maximum is accepted (32 chars)")
	void un18_separatorNearMaximum() {
		expect("UN-18", "a" + "b".repeat(29) + "_c", "");		// 1 + 29 + 2 = 32 characters
	}

	/**********
	 * <p> Method: void un19_thirtyTwoEndingInSeparator() </p>
	 *
	 * <p> Description: UN-19, boundary value.  A 32-character name whose last character is a
	 * separator is rejected for the separator, not for its length. </p>
	 */
	@Test
	@DisplayName("UN-19 32 characters ending in a separator are rejected (index 32)")
	void un19_thirtyTwoEndingInSeparator() {
		expect("UN-19", "a" + "b".repeat(30) + "_", BAD_SEPARATOR);	// 1 + 30 + 1 = 32
		expectIndex("UN-19", 32);
	}

	/**********
	 * <p> Method: void un20_spaceInMiddle() </p>
	 *
	 * <p> Description: UN-20, negative test.  A space after four valid characters stops state 1
	 * with characters left over, which gives the invalid-character message (UN-R5). </p>
	 */
	@Test
	@DisplayName("UN-20 A space in the middle is rejected (index 4)")
	void un20_spaceInMiddle() {
		expect("UN-20", "user name", INVALID_CHAR);
		expectIndex("UN-20", 4);
	}

	/**********
	 * <p> Method: void un21_invalidLastCharacter() </p>
	 *
	 * <p> Description: UN-21, boundary value last position.  A trailing space is rejected. </p>
	 */
	@Test
	@DisplayName("UN-21 A trailing space is rejected (index 4)")
	void un21_invalidLastCharacter() {
		expect("UN-21", "abcd ", INVALID_CHAR);
		expectIndex("UN-21", 4);
	}

	/**********
	 * <p> Method: void un22_stateResetBetweenCalls() </p>
	 *
	 * <p> Description: UN-22, coverage of the reset at the start of every call.  The state
	 * machine keeps its state in static fields, so a rejected name must not affect the next
	 * call. </p>
	 */
	@Test
	@DisplayName("UN-22 A rejected name does not affect the next call")
	void un22_stateResetBetweenCalls() {
		expect("UN-22", "1abc", BAD_START);
		expect("UN-22", "abcd", "");
		expectIndex("UN-22", -1);
	}


	/**********
	 * <p> Method: void un28_justOutsideRangesInEveryState() </p>
	 *
	 * <p> Description: UN-28, boundary values just outside each character range, in each
	 * state.  @ comes just before A, [ just after Z, ` just before a, { just after z, / just
	 * before 0, and : just after 9.  None of them is a letter, digit, or separator, so each one
	 * must be rejected: as the first character (state 0), after four valid characters (state 1),
	 * and right after a separator (state 2).  Added after the coverage run showed that no other
	 * test sent a character just above z, or a character just outside the digit range in state
	 * 2. </p>
	 */
	@Test
	@DisplayName("UN-28 Characters just outside the ranges are rejected in states 0, 1, and 2")
	void un28_justOutsideRangesInEveryState() {
		List<Executable> checks = new ArrayList<Executable>();
		for (char c : "@[`{/:".toCharArray()) {
			// State 0: the character is first
			String first = c + "abc";
			String actualFirst = recognize(first);
			checks.add(() -> assertEquals(BAD_START, actualFirst, "UN-28 state 0 " + first));
			// State 1: four valid characters first, so the result is not the too-short message
			String middle = "abcd" + c;
			String actualMiddle = recognize(middle);
			checks.add(() -> assertEquals(INVALID_CHAR, actualMiddle, "UN-28 state 1 " + middle));
			// State 2: the character follows a separator
			String afterSeparator = "abc_" + c + "d";
			String actualAfter = recognize(afterSeparator);
			checks.add(() -> assertEquals(BAD_SEPARATOR, actualAfter,
					"UN-28 state 2 " + afterSeparator));
			System.out.println("UN-28  '" + c + "'  state 0 " + show(first) + " -> "
					+ (actualFirst.equals(BAD_START) ? "start message" : "WRONG")
					+ "  |  state 1 " + show(middle) + " -> "
					+ (actualMiddle.equals(INVALID_CHAR) ? "invalid-character message" : "WRONG")
					+ "  |  state 2 " + show(afterSeparator) + " -> "
					+ (actualAfter.equals(BAD_SEPARATOR) ? "separator message" : "WRONG"));
		}
		System.out.println();
		assertAll("UN-28", checks);
	}


	/*-*******************************************************************************************

	Expected failures: defects D5, D6, and D4

	 */

	/**********
	 * <p> Method: void un23_invalidCharacterEarly() </p>
	 *
	 * <p> Description: UN-23, EXPECTED FAIL (defect D5).  "ab@cd" contains an invalid character,
	 * so it should get the invalid-character message (UN-R5, UN-R9).  The machine stops at the @
	 * after counting only 2 characters, and the exit logic checks "fewer than 4" before
	 * "characters left over", so the user is wrongly told the name is too short. </p>
	 */
	@Test
	@DisplayName("UN-23 EXPECTED FAIL (D5): ab@cd is reported as too short")
	void un23_invalidCharacterEarly() {
		expect("UN-23", "ab@cd", INVALID_CHAR);
	}

	/**********
	 * <p> Method: void un24_invalidCharacterEarlyInLongName() </p>
	 *
	 * <p> Description: UN-24, EXPECTED FAIL (defect D5).  The same defect with an 8-character
	 * name, which makes the "at least 4 characters" message clearly wrong. </p>
	 */
	@Test
	@DisplayName("UN-24 EXPECTED FAIL (D5): ab!cdefg is reported as too short")
	void un24_invalidCharacterEarlyInLongName() {
		expect("UN-24", "ab!cdefg", INVALID_CHAR);
	}

	/**********
	 * <p> Method: void un25_tooLongAfterSeparator() </p>
	 *
	 * <p> Description: UN-25, EXPECTED FAIL (defect D6).  A 33-character name whose last
	 * character follows a separator should get the too-long message (UN-R4, UN-R9).  State 2
	 * stops the machine when the count passes 32, and the exit logic then reports a separator
	 * problem instead.  This test also covers the true branch of state 2's size check. </p>
	 */
	@Test
	@DisplayName("UN-25 EXPECTED FAIL (D6): 33 chars ending in _c get the separator message")
	void un25_tooLongAfterSeparator() {
		expect("UN-25", "a" + "b".repeat(30) + "_c", TOO_LONG);	// 1 + 30 + 2 = 33 characters
	}

	/**********
	 * <p> Method: void un26_nullInput() </p>
	 *
	 * <p> Description: UN-26, EXPECTED FAIL (defect D4).  A null input should be treated as
	 * empty without an exception (UN-R8, CWE-476).  The code calls input.length() first, which
	 * throws a NullPointerException. </p>
	 */
	@Test
	@DisplayName("UN-26 EXPECTED FAIL (D4): null input throws NullPointerException")
	void un26_nullInput() {
		System.out.println("UN-26  Input:    null");
		System.out.println("UN-26  Expected: \"" + EMPTY.replace("\n", "\\n")
				+ "\" with no exception");
		System.out.println();
		String actual = assertDoesNotThrow(() -> recognize(null), "UN-26 must not throw");
		assertEquals(EMPTY, actual, "UN-26");
	}


	/*-*******************************************************************************************

	Regression: the TP1 username tests

	 */

	/**********
	 * <p> Method: void un27_tp1Regression() </p>
	 *
	 * <p> Description: UN-27, regression.  Tests 9 to 15 from TP1 Test Cases.pdf must still pass
	 * or fail exactly as TP1 recorded. </p>
	 */
	@Test
	@DisplayName("UN-27 TP1 Tests 9 to 15 still give the TP1 results")
	void un27_tp1Regression() {
		// Each row: TP1 test number, input, true if TP1 expected the username to be accepted
		Object[][] tp1 = {
				{9, "ValidUser1", true},
				{10, "user-name_1", true},
				{11, "1InvalidStart", false},
				{12, "usr", false},
				{13, "testuser_", false},
				{14, "user name", false},
				{15, "", false}};
		List<Executable> checks = new ArrayList<Executable>();
		for (Object[] row : tp1) {
			String input = (String) row[1];
			boolean expectedPass = (Boolean) row[2];
			boolean actualPass = recognize(input).isEmpty();	// "" means the name was accepted
			System.out.println("UN-27  TP1 Test " + row[0] + "  " + show(input) + "  Expected: "
					+ (expectedPass ? "Pass" : "Fail") + "  Actual: " + (actualPass ? "Pass" : "Fail"));
			checks.add(() -> assertEquals(expectedPass, actualPass, "UN-27 TP1 Test " + row[0]));
		}
		System.out.println();
		assertAll("UN-27", checks);
	}
}
