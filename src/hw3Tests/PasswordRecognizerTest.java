package hw3Tests;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.function.Executable;

import inputValidation.PasswordRecognizer;

/*******
 * <p> Title: PasswordRecognizerTest Class. </p>
 *
 * <p> Description: JUnit 5 boundary value and coverage tests for
 * inputValidation.PasswordRecognizer.evaluatePassword() (HW3 Task 2.4).  Each test method is one
 * row of the test design in "TP2 Test Designs.pdf", section II.B, and its display name starts
 * with that row's id (PW-01 to PW-26), so the JUnit window lists the tests in the same order as
 * the design.</p>
 *
 * <p> <b>Requirements tested</b> (section II.A of the design): PW-R1 empty input, PW-R2 the
 * 32-character maximum (checked first), PW-R3 the 8-character minimum, PW-R4 the four character
 * classes, PW-R5 allowed characters only, PW-R6 a valid password returns "", PW-R7 the five
 * public flags describe the input just evaluated, PW-R8 no exception for any input (CWE-476),
 * and PW-R9 passwordErrorMessage holds the returned message.</p>
 *
 * <p> <b>How to read the output.</b>  Every test prints its id, the input, the expected result,
 * and the actual result on the console, then asserts that they are identical.  The recognizer
 * prints a trace of every character it reads; that trace is switched off while a test calls it,
 * so the console shows only the Expected and Actual lines.  A green test passed.  The three tests
 * marked "EXPECTED FAIL" in their display name (PW-23, PW-24, PW-25) are designed to fail
 * because they expose defects D2, D1, and D3 in TP2 Test Designs.pdf, section IV.  HW3 asks for
 * those defects to be listed, not fixed, so the tests are left in place to prove each one.</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-11-01 HW3 Task 2.4: tests for the TP1 password rules
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class PasswordRecognizerTest {

	/** The message for an empty password (PW-R1). */
	private static final String EMPTY = "*** Error *** The password is empty!";

	/** The message for a password longer than 32 characters (PW-R2). */
	private static final String TOO_LONG =
			"*** Error *** The password exceeds the maximum allowed length of 32 characters!";

	/** The message for a character that is not allowed (PW-R5). */
	private static final String INVALID_CHAR = "*** Error *** An invalid character has been found!";

	/** The ending of every "conditions not satisfied" message (PW-R3, PW-R4). */
	private static final String NOT_SATISFIED = "conditions were not satisfied";

	/** The 32 special characters PasswordRecognizer accepts, copied from its source code. */
	private static final String SPECIALS = "~`!@#$%^&*()_-+={}[]|\\:;\"'<>,.?/";

	/** The flags text for an input that satisfies all five conditions (PW-R7). */
	private static final String ALL_FLAGS_TRUE =
			"Upper=true, Lower=true, Digit=true, Special=true, LongEnough=true";

	/** The flags text when no condition has been evaluated as satisfied (PW-R7). */
	private static final String ALL_FLAGS_FALSE =
			"Upper=false, Lower=false, Digit=false, Special=false, LongEnough=false";

	/**********
	 * Default constructor is not used; JUnit creates an instance for each test.
	 */
	public PasswordRecognizerTest() {
	}


	/*-*******************************************************************************************

	Helper methods

	 */

	/**********
	 * <p> Method: String evaluate(String input) </p>
	 *
	 * <p> Description: Calls PasswordRecognizer.evaluatePassword(input) with the recognizer's own
	 * console trace switched off, and returns its result. </p>
	 *
	 * @param input the password to evaluate
	 *
	 * @return the recognizer's result: "" if the password is valid, otherwise its message
	 */
	private static String evaluate(String input) {
		return quietly(() -> PasswordRecognizer.evaluatePassword(input));
	}

	/**********
	 * <p> Method: String quietly(Supplier&lt;String&gt; call) </p>
	 *
	 * <p> Description: Runs a call with System.out sent nowhere, then restores System.out. </p>
	 *
	 * @param call the call to run
	 *
	 * @return whatever the call returns
	 */
	private static String quietly(Supplier<String> call) {
		// evaluatePassword() prints several lines for every character it reads.  Hiding that trace
		// keeps the Expected and Actual lines readable in the screencast.  The finally block
		// restores the console even when the call throws (PW-24), so later tests still print.
		PrintStream console = System.out;
		System.setOut(new PrintStream(OutputStream.nullOutputStream()));
		try {
			return call.get();
		} finally {
			System.setOut(console);
		}
	}

	/**********
	 * <p> Method: String flags() </p>
	 *
	 * <p> Description: Describes the five public requirement flags in one line. </p>
	 *
	 * @return the flags as "Upper=..., Lower=..., Digit=..., Special=..., LongEnough=..."
	 */
	private static String flags() {
		// One line instead of five separate checks, so a failure shows every flag at once
		return "Upper=" + PasswordRecognizer.foundUpperCase
				+ ", Lower=" + PasswordRecognizer.foundLowerCase
				+ ", Digit=" + PasswordRecognizer.foundNumericDigit
				+ ", Special=" + PasswordRecognizer.foundSpecialChar
				+ ", LongEnough=" + PasswordRecognizer.foundLongEnough;
	}

	/**********
	 * <p> Method: void check(String id, String input, String expected, String actual) </p>
	 *
	 * <p> Description: Prints the Expected and Actual lines for one check and then asserts that
	 * they are identical. </p>
	 *
	 * @param id the test id from the design, such as "PW-03"
	 *
	 * @param input a readable description of the input
	 *
	 * @param expected the required result
	 *
	 * @param actual the result the code produced
	 */
	private static void check(String id, String input, String expected, String actual) {
		System.out.println(id + "  Input:    " + input);
		System.out.println(id + "  Expected: \"" + expected + "\"");
		System.out.println(id + "  Actual:   \"" + actual + "\"");
		System.out.println();
		assertEquals(expected, actual, id);
	}

	/**********
	 * <p> Method: String show(String input) </p>
	 *
	 * <p> Description: Makes an input readable on the console: quoted, with its length, and with
	 * a tab or non-ASCII character written as a Java escape. </p>
	 *
	 * @param input the input to describe
	 *
	 * @return the readable form
	 */
	private static String show(String input) {
		if (input == null) return "null";
		StringBuilder text = new StringBuilder("\"");
		for (char c : input.toCharArray()) {
			if (c == '\t') text.append("\\t");
			// Non-ASCII characters can print as "?" on some consoles, so show the escape instead
			else if (c > 126) text.append(String.format("\\u%04x", (int) c));
			else text.append(c);
		}
		return text.append("\" (length ").append(input.length()).append(")").toString();
	}


	/*-*******************************************************************************************

	PW-R1 and PW-R3: length boundaries at the bottom (0, 7, 8, 9)

	 */

	/**********
	 * <p> Method: void pw01_emptyInput() </p>
	 *
	 * <p> Description: PW-01, boundary value length 0.  An empty password is rejected with the
	 * empty-password message (PW-R1).  This also takes the true branch of the method's first
	 * decision, "input is empty". </p>
	 */
	@Test
	@DisplayName("PW-01 Empty input is rejected (length 0)")
	void pw01_emptyInput() {
		check("PW-01", show(""), EMPTY, evaluate(""));
	}

	/**********
	 * <p> Method: void pw02_sevenCharacters() </p>
	 *
	 * <p> Description: PW-02, boundary value minimum - 1.  Seven characters with every
	 * character class present fail only the length rule (PW-R3), so the message names only
	 * "Long Enough". </p>
	 */
	@Test
	@DisplayName("PW-02 Seven characters fail only Long Enough (min - 1)")
	void pw02_sevenCharacters() {
		check("PW-02", show("Aa1!aaa"), "Long Enough; " + NOT_SATISFIED, evaluate("Aa1!aaa"));
	}

	/**********
	 * <p> Method: void pw03_eightCharacters() </p>
	 *
	 * <p> Description: PW-03, boundary value minimum.  Eight characters with every class is the
	 * shortest valid password and returns "" (PW-R3, PW-R6). </p>
	 */
	@Test
	@DisplayName("PW-03 Eight characters are accepted (min)")
	void pw03_eightCharacters() {
		check("PW-03", show("Aa1!aaaa"), "", evaluate("Aa1!aaaa"));
	}

	/**********
	 * <p> Method: void pw04_nineCharacters() </p>
	 *
	 * <p> Description: PW-04, boundary value minimum + 1.  Nine characters are accepted. </p>
	 */
	@Test
	@DisplayName("PW-04 Nine characters are accepted (min + 1)")
	void pw04_nineCharacters() {
		check("PW-04", show("Aa1!aaaaa"), "", evaluate("Aa1!aaaaa"));
	}


	/*-*******************************************************************************************

	PW-R2: length boundaries at the top (31, 32, 33) and the order of the checks

	 */

	/**********
	 * <p> Method: void pw05_thirtyOneCharacters() </p>
	 *
	 * <p> Description: PW-05, boundary value maximum - 1.  31 characters are accepted. </p>
	 */
	@Test
	@DisplayName("PW-05 31 characters are accepted (max - 1)")
	void pw05_thirtyOneCharacters() {
		String input = "Aa1!" + "a".repeat(27);		// 4 + 27 = 31 characters
		check("PW-05", show(input), "", evaluate(input));
	}

	/**********
	 * <p> Method: void pw06_thirtyTwoCharacters() </p>
	 *
	 * <p> Description: PW-06, boundary value maximum.  32 characters is the longest valid
	 * password (PW-R2, PW-R6). </p>
	 */
	@Test
	@DisplayName("PW-06 32 characters are accepted (max)")
	void pw06_thirtyTwoCharacters() {
		String input = "Aa1!" + "a".repeat(28);		// 4 + 28 = 32 characters
		check("PW-06", show(input), "", evaluate(input));
	}

	/**********
	 * <p> Method: void pw07_thirtyThreeCharacters() </p>
	 *
	 * <p> Description: PW-07, boundary value maximum + 1.  33 characters are rejected with the
	 * maximum-length message (PW-R2). </p>
	 */
	@Test
	@DisplayName("PW-07 33 characters are rejected (max + 1)")
	void pw07_thirtyThreeCharacters() {
		String input = "Aa1!" + "a".repeat(29);		// 4 + 29 = 33 characters
		check("PW-07", show(input), TOO_LONG, evaluate(input));
	}

	/**********
	 * <p> Method: void pw08_lengthCheckedFirst() </p>
	 *
	 * <p> Description: PW-08, order of the checks.  A 33-character input that also contains an
	 * invalid character (a space) must get the length message, which proves the length is
	 * checked before any character rule (PW-R2). </p>
	 */
	@Test
	@DisplayName("PW-08 Length is checked before characters (33 chars with a space)")
	void pw08_lengthCheckedFirst() {
		// The space at index 4 would give the invalid-character message if characters were
		// checked first, so getting TOO_LONG proves the order PW-R2 requires
		String input = "Aa1! " + "a".repeat(28);	// 5 + 28 = 33 characters
		check("PW-08", show(input), TOO_LONG, evaluate(input));
	}


	/*-*******************************************************************************************

	PW-R4: each character class, and the order of the missing-condition list

	 */

	/**********
	 * <p> Method: void pw09_missingUpperCase() </p>
	 *
	 * <p> Description: PW-09, negative test.  No upper-case letter. </p>
	 */
	@Test
	@DisplayName("PW-09 Missing upper case is named")
	void pw09_missingUpperCase() {
		check("PW-09", show("aa1!aaaa"), "Upper case; " + NOT_SATISFIED, evaluate("aa1!aaaa"));
	}

	/**********
	 * <p> Method: void pw10_missingLowerCase() </p>
	 *
	 * <p> Description: PW-10, negative test.  No lower-case letter. </p>
	 */
	@Test
	@DisplayName("PW-10 Missing lower case is named")
	void pw10_missingLowerCase() {
		check("PW-10", show("AA1!AAAA"), "Lower case; " + NOT_SATISFIED, evaluate("AA1!AAAA"));
	}

	/**********
	 * <p> Method: void pw11_missingDigit() </p>
	 *
	 * <p> Description: PW-11, negative test.  No digit. </p>
	 */
	@Test
	@DisplayName("PW-11 Missing digit is named")
	void pw11_missingDigit() {
		check("PW-11", show("Aaa!aaaa"), "Numeric digits; " + NOT_SATISFIED,
				evaluate("Aaa!aaaa"));
	}

	/**********
	 * <p> Method: void pw12_missingSpecial() </p>
	 *
	 * <p> Description: PW-12, negative test.  No special character. </p>
	 */
	@Test
	@DisplayName("PW-12 Missing special character is named")
	void pw12_missingSpecial() {
		check("PW-12", show("Aa1aaaaa"), "Special character; " + NOT_SATISFIED,
				evaluate("Aa1aaaaa"));
	}

	/**********
	 * <p> Method: void pw13_severalMissingInOrder() </p>
	 *
	 * <p> Description: PW-13, negative test.  Eight lower-case letters miss three conditions,
	 * which must be listed in the fixed order Upper case, Numeric digits, Special character
	 * (PW-R4). </p>
	 */
	@Test
	@DisplayName("PW-13 Several missing conditions are listed in order")
	void pw13_severalMissingInOrder() {
		check("PW-13", show("aaaaaaaa"),
				"Upper case; Numeric digits; Special character; " + NOT_SATISFIED,
				evaluate("aaaaaaaa"));
	}


	/*-*******************************************************************************************

	PW-R5: character-range edges, every special character, and invalid characters

	 */

	/**********
	 * <p> Method: void pw14_rangeEdgesAccepted() </p>
	 *
	 * <p> Description: PW-14, boundary values of the character ranges.  A, Z, a, z, 0, and 9
	 * are the first and last characters of each range the code tests with &gt;= and &lt;=, and
	 * ! and ~ are the first and last characters of the special list. </p>
	 */
	@Test
	@DisplayName("PW-14 Range edges A Z a z 0 9 and specials ! ~ are accepted")
	void pw14_rangeEdgesAccepted() {
		check("PW-14", show("AZaz09!~"), "", evaluate("AZaz09!~"));
	}

	/**********
	 * <p> Method: void pw15_justOutsideRangesAreSpecialsOnly() </p>
	 *
	 * <p> Description: PW-15, boundary values just outside each range.  @ comes just before A,
	 * [ just after Z, ` just before a, { just after z, / just before 0, and : just after 9.
	 * They must count only as special characters, so upper case, lower case, and digits are
	 * all still missing. </p>
	 */
	@Test
	@DisplayName("PW-15 Characters just outside the ranges count only as specials")
	void pw15_justOutsideRangesAreSpecialsOnly() {
		check("PW-15", show("@[`{/:~!"),
				"Upper case; Lower case; Numeric digits; " + NOT_SATISFIED,
				evaluate("@[`{/:~!"));
	}

	/**********
	 * <p> Method: void pw16_everySpecialAccepted() </p>
	 *
	 * <p> Description: PW-16, coverage of the special-character branch for each of the 32
	 * allowed specials.  "Aa1aaaa" has every class except a special, so each password is valid
	 * only if that one special character is recognized. </p>
	 */
	@Test
	@DisplayName("PW-16 Each of the 32 special characters is accepted")
	void pw16_everySpecialAccepted() {
		List<Executable> checks = new ArrayList<Executable>();
		int accepted = 0;
		for (char special : SPECIALS.toCharArray()) {
			String input = "Aa1aaaa" + special;
			String actual = evaluate(input);
			if (actual.isEmpty()) accepted++;
			// assertAll runs every check and reports all failures, not just the first one
			checks.add(() -> assertEquals("", actual, "PW-16 special " + special));
		}
		System.out.println("PW-16  Input:    \"Aa1aaaa\" + each of the " + SPECIALS.length()
				+ " specials " + SPECIALS);
		System.out.println("PW-16  Expected: \"\" (accepted) for all 32");
		System.out.println("PW-16  Actual:   accepted " + accepted + " of " + SPECIALS.length());
		System.out.println();
		assertEquals(32, SPECIALS.length(), "PW-16 the design lists 32 special characters");
		assertAll("PW-16", checks);
	}

	/**********
	 * <p> Method: void pw17_invalidFirstCharacter() </p>
	 *
	 * <p> Description: PW-17, boundary value first position.  A leading space is found at index
	 * 0. </p>
	 */
	@Test
	@DisplayName("PW-17 Invalid character in the first position (index 0)")
	void pw17_invalidFirstCharacter() {
		String input = " Aa1!aaaa";
		check("PW-17", show(input), INVALID_CHAR, evaluate(input));
		check("PW-17", "passwordIndexofError", "0",
				String.valueOf(PasswordRecognizer.passwordIndexofError));
	}

	/**********
	 * <p> Method: void pw18_invalidMiddleCharacter() </p>
	 *
	 * <p> Description: PW-18, negative test.  A space in the middle is found at index 4. </p>
	 */
	@Test
	@DisplayName("PW-18 Invalid character in the middle (index 4)")
	void pw18_invalidMiddleCharacter() {
		String input = "Aa1! aaaa";
		check("PW-18", show(input), INVALID_CHAR, evaluate(input));
		check("PW-18", "passwordIndexofError", "4",
				String.valueOf(PasswordRecognizer.passwordIndexofError));
	}

	/**********
	 * <p> Method: void pw19_invalidLastCharacter() </p>
	 *
	 * <p> Description: PW-19, boundary value last position.  A trailing space is found at index
	 * 8, even though the first eight characters already form a valid password. </p>
	 */
	@Test
	@DisplayName("PW-19 Invalid character in the last position (index 8)")
	void pw19_invalidLastCharacter() {
		String input = "Aa1!aaaa ";
		check("PW-19", show(input), INVALID_CHAR, evaluate(input));
		check("PW-19", "passwordIndexofError", "8",
				String.valueOf(PasswordRecognizer.passwordIndexofError));
	}

	/**********
	 * <p> Method: void pw20_controlCharacter() </p>
	 *
	 * <p> Description: PW-20, negative test.  A tab (a control character) is rejected. </p>
	 */
	@Test
	@DisplayName("PW-20 A tab character is rejected (index 4)")
	void pw20_controlCharacter() {
		String input = "Aa1!\taaa";
		check("PW-20", show(input), INVALID_CHAR, evaluate(input));
		check("PW-20", "passwordIndexofError", "4",
				String.valueOf(PasswordRecognizer.passwordIndexofError));
	}

	/**********
	 * <p> Method: void pw21_nonAsciiCharacter() </p>
	 *
	 * <p> Description: PW-21, negative test.  A non-ASCII letter (e with an acute accent) is not in any allowed
	 * range, so it is rejected. </p>
	 */
	@Test
	@DisplayName("PW-21 A non-ASCII letter is rejected (index 7)")
	void pw21_nonAsciiCharacter() {
		// Written as an escape so the source file means the same thing in any file encoding
		String input = "Aa1!aaa\u00e9";
		check("PW-21", show(input), INVALID_CHAR, evaluate(input));
		check("PW-21", "passwordIndexofError", "7",
				String.valueOf(PasswordRecognizer.passwordIndexofError));
	}


	/*-*******************************************************************************************

	PW-R7: the five public flags

	 */

	/**********
	 * <p> Method: void pw22_flagsDescribeTheInput() </p>
	 *
	 * <p> Description: PW-22, positive and negative.  After a valid password all five flags are
	 * true; after "aaaaaaaa" only Lower and LongEnough are true.  The New Account page colors
	 * its live checklist from these flags. </p>
	 */
	@Test
	@DisplayName("PW-22 The five flags describe the input just evaluated")
	void pw22_flagsDescribeTheInput() {
		evaluate("Aa1!aaaa");
		check("PW-22", "flags after " + show("Aa1!aaaa"), ALL_FLAGS_TRUE, flags());
		evaluate("aaaaaaaa");
		check("PW-22", "flags after " + show("aaaaaaaa"),
				"Upper=false, Lower=true, Digit=false, Special=false, LongEnough=true", flags());
	}

	/**********
	 * <p> Method: void pw23_flagsAfterEarlyExit() </p>
	 *
	 * <p> Description: PW-23, EXPECTED FAIL (defect D2).  A 33-character input is rejected before
	 * the flags are reset, so the flags should show nothing satisfied.  The code returns before
	 * resetting them, so they still describe the previous, valid password. </p>
	 */
	@Test
	@DisplayName("PW-23 EXPECTED FAIL (D2): flags are stale after an over-long input")
	void pw23_flagsAfterEarlyExit() {
		// First make every flag true, so a stale value is easy to see
		evaluate("Aa1!aaaa");
		evaluate("Aa1!" + "a".repeat(29));
		check("PW-23", "flags after a valid password, then a 33-character one",
				ALL_FLAGS_FALSE, flags());
	}

	/**********
	 * <p> Method: void pw24_nullInput() </p>
	 *
	 * <p> Description: PW-24, EXPECTED FAIL (defect D1).  A null input should be treated as
	 * empty without an exception (PW-R8, CWE-476).  The code calls input.length() first, which
	 * throws a NullPointerException. </p>
	 */
	@Test
	@DisplayName("PW-24 EXPECTED FAIL (D1): null input throws NullPointerException")
	void pw24_nullInput() {
		System.out.println("PW-24  Input:    null");
		System.out.println("PW-24  Expected: \"" + EMPTY + "\" with no exception");
		System.out.println();
		String actual = assertDoesNotThrow(() -> evaluate(null), "PW-24 must not throw");
		assertEquals(EMPTY, actual, "PW-24");
	}

	/**********
	 * <p> Method: void pw25_errorMessageField() </p>
	 *
	 * <p> Description: PW-25, EXPECTED FAIL (defect D3).  The public field passwordErrorMessage
	 * is documented as "The error message text", so after a failed check it should hold the
	 * returned message.  The code never assigns it a message. </p>
	 */
	@Test
	@DisplayName("PW-25 EXPECTED FAIL (D3): passwordErrorMessage is never set")
	void pw25_errorMessageField() {
		String returned = evaluate("Aa1!aaa");
		check("PW-25", "passwordErrorMessage after " + show("Aa1!aaa"), returned,
				PasswordRecognizer.passwordErrorMessage);
	}


	/*-*******************************************************************************************

	Regression: the TP1 password tests

	 */

	/**********
	 * <p> Method: void pw26_tp1Regression() </p>
	 *
	 * <p> Description: PW-26, regression.  Tests 1 to 8 from TP1 Test Cases.pdf must still pass
	 * or fail exactly as TP1 recorded. </p>
	 */
	@Test
	@DisplayName("PW-26 TP1 Tests 1 to 8 still give the TP1 results")
	void pw26_tp1Regression() {
		// Each row: TP1 test number, input, true if TP1 expected the password to be accepted
		Object[][] tp1 = {
				{1, "Aa!15678", true},
				{2, "A!1a", false},
				{3, "AA!15678", false},
				{4, "aa!15678", false},
				{5, "Aa!bcdef", false},
				{6, "Aa115678", false},
				{7, "Aa!15678Aa!15678Aa!15678Aa!15678X", false},
				{8, "", false}};
		List<Executable> checks = new ArrayList<Executable>();
		for (Object[] row : tp1) {
			String input = (String) row[1];
			boolean expectedPass = (Boolean) row[2];
			boolean actualPass = evaluate(input).isEmpty();	// "" means the password was accepted
			System.out.println("PW-26  TP1 Test " + row[0] + "  " + show(input) + "  Expected: "
					+ (expectedPass ? "Pass" : "Fail") + "  Actual: " + (actualPass ? "Pass" : "Fail"));
			checks.add(() -> assertEquals(expectedPass, actualPass, "PW-26 TP1 Test " + row[0]));
		}
		System.out.println();
		assertAll("PW-26", checks);
	}
}
