package lessonTests;

import static lessonTests.TestSupport.check;
import static lessonTests.TestSupport.describe;
import static lessonTests.TestSupport.makeLesson;
import static lessonTests.TestSupport.requirement;
import static lessonTests.TestSupport.run;
import static lessonTests.TestSupport.uniqueUser;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import entityClasses.ExperienceEntry;
import entityClasses.LessonsLearned;
import entityClasses.MemberEffort;
import guiAddExperience.ModelAddExperience;
import guiTools.LessonResult;

/*******
 * <p> Title: AddExperienceTests Class. </p>
 *
 * <p> Description: JUnit 5 tests for US-U2 (a Contributor can add experience
 * information, a description of what was done and the time/effort in hours, to their lesson),
 * US-U2-V (invalid experience values get a specific message and nothing is saved), and E7, the
 * entity requirement for the optional per-member effort list (each member needs a valid name
 * and hours, the members' hours may not add up to more than the total, and at most 20 members
 * may be listed).</p>
 *
 * <p> Every test calls ModelAddExperience.addExperience, the method the Add Experience page's
 * Controller calls, and then reads the entries back with ExperienceEntry.readForLesson to prove
 * what was stored.  See TestSupport for how to read the output.</p>
 *
 * <p> <b>Running the tests.</b>  These are JUnit 5 tests: run the class, or the whole
 * lessonTests package, with Run As &gt; JUnit Test.  Each test's display name is its TP2 id, its
 * name, its requirement, and P or N, for example "C1-P1 Create Lesson - Valid Input (US-C1, P)",
 * and the tests run in the order of the Requirements-Test Traceability list.  The same tests
 * still run as a console program through main() and AllContributorTests, with the same output
 * as in TP2.  FoundationsMain must not be running.</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-10-16 TP2 Task 6: Add Experience tests, one method per test
 * @version 1.01		2026-11-14 TP3 Task 5.4: converted to JUnit 5; the console runner is kept
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AddExperienceTests {

	/**********
	 * Default constructor.  JUnit creates an instance for each test, and runAll creates one
	 * to run the same test methods as a console program.
	 */
	public AddExperienceTests() {
	}

	/**********
	 * Runs the Add Experience tests as their own suite.
	 *
	 * @param args not used
	 */
	public static void main(String[] args) {
		TestSupport.runSuite("TP2 Contributor Tests - Add Experience (US-U2, US-U2-V, E7)",
				AddExperienceTests::runAll);
	}

	/**********
	 * <p> Method: void connectForJUnit() </p>
	 *
	 * <p> Description: Opens the database once before the first JUnit test of this class.
	 * FoundationsMain must not be running, because H2 allows one connection. </p>
	 */
	@BeforeAll
	static void connectForJUnit() {
		TestSupport.startJUnit();
	}

	/**********
	 * <p> Method: void cleanUpForJUnit() </p>
	 *
	 * <p> Description: After the last JUnit test of this class, deletes every lesson the tests
	 * created and closes the database. </p>
	 */
	@AfterAll
	static void cleanUpForJUnit() {
		TestSupport.finishJUnit();
	}

	/**********
	 * Runs every test in this class, grouped under the requirement each one demonstrates.
	 */
	public static void runAll() {
		AddExperienceTests tests = new AddExperienceTests();
		requirement("US-U2", "A Contributor can add experience information (what was done, "
				+ "time/effort in hours) to their lesson");
		run(tests::testAddExperienceValidInput);
		run(tests::testSecondAttemptAddsSecondEntry);

		requirement("US-U2-V", "Invalid experience information gets a specific message and "
				+ "nothing is saved");
		run(tests::testAddExperienceSmallestValidValuesAccepted);
		run(tests::testAddExperienceRejectedNegativeValue);
		run(tests::testAddExperienceRejectedNonNumericValue);
		run(tests::testAddExperienceRejectedMissingDescription);

		requirement("E7", "Team-member effort: valid name and hours each, total of members' "
				+ "hours <= total, at most 20 members");
		run(tests::testTeamMemberSharesSaved);
		run(tests::testTwentyMembersWithHoursEqualToTotalAccepted);
		run(tests::testMemberHoursExceedingTotalRejected);
		run(tests::testMoreThanTwentyMembersRejected);
		run(tests::testBlankMemberNameRejected);
	}


	/**********
	 * Describes the experience entries stored for a lesson, read fresh from the database.
	 *
	 * @param lessonId	the lesson whose entries to read
	 * @return for example: 2 entries stored: 2.0 h, 1.5 h
	 */
	private static String storedEntries(int lessonId) {
		List<ExperienceEntry> entries = ExperienceEntry.readForLesson(lessonId);
		List<String> hours = new ArrayList<String>();
		for (ExperienceEntry entry : entries) hours.add(entry.getTimeEffortHours() + " h");
		return entries.size() + " entries stored" + (hours.isEmpty() ? "" : ": "
				+ String.join(", ", hours));
	}


	/**********
	 * Makes a list of the same value repeated, for building long member lists.
	 *
	 * @param value	the value
	 * @param count	how many copies
	 * @return the list
	 */
	private static List<String> repeated(String value, int count) {
		List<String> list = new ArrayList<String>();
		for (int i = 0; i < count; i++) list.add(value);
		return list;
	}


	/**********
	 * Makes a list of distinct member names: "Member 1", "Member 2", and so on.
	 *
	 * @param count	how many names
	 * @return the names
	 */
	private static List<String> memberNames(int count) {
		List<String> names = new ArrayList<String>();
		for (int i = 1; i <= count; i++) names.add("Member " + i);
		return names;
	}


	/*-*******************************************************************************************

	US-U2: add experience

	 */

	/**********
	 * <p> Test U2-P1 (Positive): Add Experience Information - Valid Input. </p>
	 *
	 * <p> <b>Requirement:</b> US-U2, a Contributor can add a description of what was done and
	 * the time/effort in hours to their lesson. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> addExperience checks ownership, validates the
	 * hours and the description, builds an ExperienceEntry linked to the lesson id, and saves it
	 * with ExperienceEntry.create().  The lesson's core fields are not touched. </p>
	 *
	 * <p> <b>Steps:</b> add a 3.5-hour entry; read the entries and the lesson back. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means "Experience added" was returned, one
	 * 3.5-hour entry is stored for this lesson, and the lesson title is unchanged. </p>
	 *
	 * <p> Validates: ModelAddExperience.addExperience(). </p>
	 */
	@Test
	@Order(1)
	@DisplayName("U2-P1 Add Experience Information - Valid Input (US-U2, P)")
	void testAddExperienceValidInput() {
		String user = uniqueUser("expValid");
		int id = makeLesson(user, "Reusable lesson", "Research");

		LessonResult result = ModelAddExperience.addExperience(user, id, "3.5",
				"Reused the checklist from the original lesson and it saved us time.");
		check("U2-P1", true, "Add Experience Information - Valid Input",
				"success \"Experience added\"; 1 entries stored: 3.5 h; lesson title still: "
				+ "Reusable lesson",
				describe(result) + "; " + storedEntries(id) + "; lesson title still: "
				+ LessonsLearned.read(id).getTitle());
	}


	/**********
	 * <p> Test U2-P2 (Positive): Add Experience - A Second Attempt Adds a Second Entry. </p>
	 *
	 * <p> <b>Requirement:</b> US-U2, experience is added "after a new attempt" each time, so the
	 * lesson keeps a history rather than overwriting the last entry. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> each call inserts a new row in experienceDB, and
	 * readForLesson returns the rows oldest first. </p>
	 *
	 * <p> <b>Steps:</b> add a 2-hour entry, then a 1.5-hour entry. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means both entries are stored, in the order they
	 * were added. </p>
	 *
	 * <p> Validates: ModelAddExperience.addExperience(). </p>
	 */
	@Test
	@Order(2)
	@DisplayName("U2-P2 Add Experience - A Second Attempt Adds a Second Entry (US-U2, P)")
	void testSecondAttemptAddsSecondEntry() {
		String user = uniqueUser("expTwice");
		int id = makeLesson(user, "Lesson reused twice", "General");

		ModelAddExperience.addExperience(user, id, "2", "First reuse of this lesson.");
		LessonResult second = ModelAddExperience.addExperience(user, id, "1.5",
				"Second reuse, faster this time.");
		check("U2-P2", true, "Add Experience - A Second Attempt Adds a Second Entry",
				"success \"Experience added\"; 2 entries stored: 2.0 h, 1.5 h",
				describe(second) + "; " + storedEntries(id));
	}


	/*-*******************************************************************************************

	US-U2-V: invalid experience information

	 */

	/**********
	 * <p> Test U2V-P1 (Positive): Add Experience - Smallest Valid Values Accepted. </p>
	 *
	 * <p> <b>Requirement:</b> US-U2-V.  Rejecting values that are too small also means
	 * accepting the smallest values that are allowed; otherwise a Contributor with a short
	 * attempt could not record it. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> checkForValidExperienceValue accepts any plain
	 * number greater than 0, and checkForValidExperienceDescription accepts 5 characters. </p>
	 *
	 * <p> <b>Steps:</b> submit 0.1 hours with the 5-character description "Fixed". </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the entry is saved with 0.1 hours. </p>
	 *
	 * <p> Validates: ModelAddExperience.addExperience(). </p>
	 */
	@Test
	@Order(3)
	@DisplayName("U2V-P1 Add Experience - Smallest Valid Values Accepted (US-U2-V, P)")
	void testAddExperienceSmallestValidValuesAccepted() {
		String user = uniqueUser("expSmallest");
		int id = makeLesson(user, "Lesson for smallest values", "General");

		LessonResult result = ModelAddExperience.addExperience(user, id, "0.1", "Fixed");
		check("U2V-P1", true, "Add Experience - Smallest Valid Values Accepted",
				"success \"Experience added\"; 1 entries stored: 0.1 h",
				describe(result) + "; " + storedEntries(id));
	}


	/**********
	 * <p> Test U2V-N1 (Negative): Add Experience Information - Rejected on Invalid Input
	 * (Negative Hours). </p>
	 *
	 * <p> <b>Requirements:</b> US-U2-V, a negative time/effort gets a specific message and
	 * nothing is saved; and US-U2, which saves only valid experience information. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> LessonRecognizer.checkForValidExperienceValue
	 * rejects anything that is not a plain number greater than 0, and addExperience stops
	 * before creating the entry. </p>
	 *
	 * <p> <b>Steps:</b> submit "-2" hours. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the time/effort message is returned for the
	 * hours field and 0 entries are stored. </p>
	 *
	 * <p> Validates: ModelAddExperience.addExperience(). </p>
	 */
	@Test
	@Order(4)
	@DisplayName("U2V-N1 Add Experience Information - Rejected on Invalid Input (US-U2-V, N)")
	void testAddExperienceRejectedNegativeValue() {
		String user = uniqueUser("expNegative");
		int id = makeLesson(user, "Lesson for negative hours", "General");

		LessonResult result = ModelAddExperience.addExperience(user, id, "-2",
				"Tried the same approach again.");
		check("U2V-N1", false, "Add Experience Information - Rejected on Invalid Input",
				"refused \"Time/effort must be a positive number.\" (field: experienceValue); "
				+ "0 entries stored",
				describe(result) + "; " + storedEntries(id));
	}


	/**********
	 * <p> Test U2V-N2 (Negative): Add Experience - Non-Numeric Hours Rejected. </p>
	 *
	 * <p> <b>Requirement:</b> US-U2-V names "a negative or non-numeric time/effort". </p>
	 *
	 * <p> <b>How the code satisfies it:</b> checkForValidExperienceValue checks every character
	 * is a digit or a single decimal point before the text is ever parsed, so "abc" cannot cause
	 * an exception. </p>
	 *
	 * <p> <b>Steps:</b> submit "abc" hours. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the time/effort message is returned and 0
	 * entries are stored. </p>
	 *
	 * <p> Validates: ModelAddExperience.addExperience(). </p>
	 */
	@Test
	@Order(5)
	@DisplayName("U2V-N2 Add Experience - Non-Numeric Hours Rejected (US-U2-V, N)")
	void testAddExperienceRejectedNonNumericValue() {
		String user = uniqueUser("expNonNumeric");
		int id = makeLesson(user, "Lesson for non-numeric hours", "General");

		LessonResult result = ModelAddExperience.addExperience(user, id, "abc",
				"Tried the same approach again.");
		check("U2V-N2", false, "Add Experience - Non-Numeric Hours Rejected",
				"refused \"Time/effort must be a positive number.\" (field: experienceValue); "
				+ "0 entries stored",
				describe(result) + "; " + storedEntries(id));
	}


	/**********
	 * <p> Test U2V-N3 (Negative): Add Experience - Missing Description Rejected. </p>
	 *
	 * <p> <b>Requirement:</b> US-U2-V names "a missing description". </p>
	 *
	 * <p> <b>How the code satisfies it:</b> checkForValidExperienceDescription returns "Please
	 * describe what was done." for an empty description. </p>
	 *
	 * <p> <b>Steps:</b> submit 2 hours with an empty description. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the description message is returned for the
	 * description field and 0 entries are stored. </p>
	 *
	 * <p> Validates: ModelAddExperience.addExperience(). </p>
	 */
	@Test
	@Order(6)
	@DisplayName("U2V-N3 Add Experience - Missing Description Rejected (US-U2-V, N)")
	void testAddExperienceRejectedMissingDescription() {
		String user = uniqueUser("expNoDescription");
		int id = makeLesson(user, "Lesson for missing description", "General");

		LessonResult result = ModelAddExperience.addExperience(user, id, "2", "");
		check("U2V-N3", false, "Add Experience - Missing Description Rejected",
				"refused \"Please describe what was done.\" (field: experienceDescription); "
				+ "0 entries stored",
				describe(result) + "; " + storedEntries(id));
	}


	/*-*******************************************************************************************

	E7: team-member effort

	 */

	/**********
	 * <p> Test E7-P1 (Positive): Team Member Shares Are Saved With the Entry. </p>
	 *
	 * <p> <b>Requirement:</b> E7, an experience entry may list the team members who did the
	 * work and the hours each spent (the Experience field in AICoaching.pdf). </p>
	 *
	 * <p> <b>How the code satisfies it:</b> addExperience turns each name/hours pair into a
	 * MemberEffort, and ExperienceEntry.create() saves the entry and its shares in one database
	 * transaction. </p>
	 *
	 * <p> <b>Steps:</b> add a 4-hour entry with Ana (1.5 h) and Ben (2 h). </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the stored entry has exactly those two
	 * members with those hours. </p>
	 *
	 * <p> Validates: ModelAddExperience.addExperience(). </p>
	 */
	@Test
	@Order(7)
	@DisplayName("E7-P1 Team Member Shares Are Saved With the Entry (E7, P)")
	void testTeamMemberSharesSaved() {
		String user = uniqueUser("membersValid");
		int id = makeLesson(user, "Lesson with team members", "Teamwork");

		LessonResult result = ModelAddExperience.addExperience(user, id, "4",
				"Paired on the fix and reused the earlier checklist.",
				Arrays.asList("Ana", "Ben"), Arrays.asList("1.5", "2"));

		String shares = "no entry stored";
		List<ExperienceEntry> entries = ExperienceEntry.readForLesson(id);
		if (!entries.isEmpty()) {
			List<String> parts = new ArrayList<String>();
			for (MemberEffort share : entries.get(0).getMemberEfforts())
				parts.add(share.getMemberName() + " " + share.getHours() + " h");
			shares = "members stored: " + String.join(", ", parts);
		}
		check("E7-P1", true, "Team Member Shares Are Saved With the Entry",
				"success \"Experience added\"; members stored: Ana 1.5 h, Ben 2.0 h",
				describe(result) + "; " + shares);
	}


	/**********
	 * <p> Test E7-P2 (Positive): Twenty Members Whose Hours Equal the Total Are Accepted. </p>
	 *
	 * <p> <b>Requirement:</b> E7, both limits are inclusive: exactly 20 members may be listed,
	 * and the members' hours may add up to exactly the total. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> ExperienceEntry.findInvalidField refuses only more
	 * than MAX_MEMBERS (20) shares, and only a sum greater than the total (with a tiny tolerance
	 * for decimal rounding). </p>
	 *
	 * <p> <b>Steps:</b> add a 20-hour entry with 20 members of 1 hour each. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the entry is saved with 20 member
	 * shares. </p>
	 *
	 * <p> Validates: ModelAddExperience.addExperience(). </p>
	 */
	@Test
	@Order(8)
	@DisplayName("E7-P2 Twenty Members Whose Hours Equal the Total Are Accepted (E7, P)")
	void testTwentyMembersWithHoursEqualToTotalAccepted() {
		String user = uniqueUser("membersTwenty");
		int id = makeLesson(user, "Lesson with twenty members", "Teamwork");

		LessonResult result = ModelAddExperience.addExperience(user, id, "20",
				"The whole class reused this lesson.", memberNames(20), repeated("1", 20));
		List<ExperienceEntry> entries = ExperienceEntry.readForLesson(id);
		String stored = entries.isEmpty() ? "no entry stored"
				: entries.get(0).getMemberEfforts().size() + " member shares stored";
		check("E7-P2", true, "Twenty Members Whose Hours Equal the Total Are Accepted",
				"success \"Experience added\"; 20 member shares stored",
				describe(result) + "; " + stored);
	}


	/**********
	 * <p> Test E7-N1 (Negative): Members' Hours Exceeding the Total Are Rejected. </p>
	 *
	 * <p> <b>Requirement:</b> E7, the members' hours may not add up to more than the entry's
	 * total time/effort (decision D31). </p>
	 *
	 * <p> <b>How the code satisfies it:</b> ExperienceEntry.memberHoursExceedTotal() sums the
	 * shares, and validate() returns the sum message before anything is saved. </p>
	 *
	 * <p> <b>Steps:</b> a 3-hour entry with two members of 2 hours each (4 hours). </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the sum message is returned for the member
	 * list and 0 entries are stored. </p>
	 *
	 * <p> Validates: ModelAddExperience.addExperience(). </p>
	 */
	@Test
	@Order(9)
	@DisplayName("E7-N1 Members' Hours Exceeding the Total Are Rejected (E7, N)")
	void testMemberHoursExceedingTotalRejected() {
		String user = uniqueUser("membersOver");
		int id = makeLesson(user, "Lesson with too many hours", "Teamwork");

		LessonResult result = ModelAddExperience.addExperience(user, id, "3",
				"Paired on the fix.", Arrays.asList("Ana", "Ben"), Arrays.asList("2", "2"));
		check("E7-N1", false, "Members' Hours Exceeding the Total Are Rejected",
				"refused \"Team member hours cannot add up to more than the total time/effort.\" "
				+ "(field: memberEfforts); 0 entries stored",
				describe(result) + "; " + storedEntries(id));
	}


	/**********
	 * <p> Test E7-N2 (Negative): More Than Twenty Members Are Rejected. </p>
	 *
	 * <p> <b>Requirement:</b> E7, at most 20 members may be listed (decision D31). </p>
	 *
	 * <p> <b>How the code satisfies it:</b> ExperienceEntry.findInvalidField checks the size of
	 * the member list against MAX_MEMBERS.  The Add Experience page also disables "Add Team
	 * Member" at 20 rows, so this is the safety net behind the screen. </p>
	 *
	 * <p> <b>Steps:</b> a 20-hour entry with 21 members of 0.5 hours each (10.5 hours, under
	 * the total, so only the count is wrong). </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the 20-member message is returned and 0
	 * entries are stored. </p>
	 *
	 * <p> Validates: ModelAddExperience.addExperience(). </p>
	 */
	@Test
	@Order(10)
	@DisplayName("E7-N2 More Than Twenty Members Are Rejected (E7, N)")
	void testMoreThanTwentyMembersRejected() {
		String user = uniqueUser("membersTooMany");
		int id = makeLesson(user, "Lesson with too many members", "Teamwork");

		LessonResult result = ModelAddExperience.addExperience(user, id, "20",
				"The whole class reused this lesson.", memberNames(21), repeated("0.5", 21));
		check("E7-N2", false, "More Than Twenty Members Are Rejected",
				"refused \"No more than 20 team members can be listed.\" (field: memberEfforts); "
				+ "0 entries stored",
				describe(result) + "; " + storedEntries(id));
	}


	/**********
	 * <p> Test E7-N3 (Negative): A Blank Member Name Is Rejected. </p>
	 *
	 * <p> <b>Requirement:</b> E7, each listed member needs a name. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> addExperience checks each name with
	 * LessonRecognizer.checkForValidMemberName before building the shares. </p>
	 *
	 * <p> <b>Steps:</b> a 2-hour entry with one member whose name is only spaces. </p>
	 *
	 * <p> <b>How to read the output:</b> PASS means the name message is returned for the member
	 * name field and 0 entries are stored. </p>
	 *
	 * <p> Validates: ModelAddExperience.addExperience(). </p>
	 */
	@Test
	@Order(11)
	@DisplayName("E7-N3 A Blank Member Name Is Rejected (E7, N)")
	void testBlankMemberNameRejected() {
		String user = uniqueUser("membersBlank");
		int id = makeLesson(user, "Lesson with a blank member", "Teamwork");

		LessonResult result = ModelAddExperience.addExperience(user, id, "2",
				"Paired on the fix.", Arrays.asList("   "), Arrays.asList("1"));
		check("E7-N3", false, "A Blank Member Name Is Rejected",
				"refused \"Team member name is required.\" (field: memberName); 0 entries stored",
				describe(result) + "; " + storedEntries(id));
	}
}
