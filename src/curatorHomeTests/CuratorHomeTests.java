package curatorHomeTests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import applicationMain.FoundationsMain;
import database.Database;
import entityClasses.LessonsLearned;
import entityClasses.LessonsLearnedList;
import entityClasses.User;
import guiAddExperience.ModelAddExperience;
import guiContributorHome.ModelContributorHome;
import guiCreateLesson.ModelCreateLesson;
import guiCuratorHome.ModelCuratorHome;
import guiCuratorLock.ModelCuratorLock;
import guiTools.CuratorAccess;
import guiTools.LessonAccess;
import guiTools.LessonResult;
import guiTools.RoleLabels;

/*******
 * <p> Title: CuratorHomeTests Class. </p>
 *
 * <p> Description: JUnit 5 tests for TP3 Aspect 1, the Curator role and the Curator Home Page
 * (Epics.pdf dependency D1: "A Curator role and home page, built like the Role1 and Role2
 * pattern").  Each test's display name starts with its id (CH-01 to CH-36) and ends with (P)
 * for a positive test or (N) for a negative test, so the JUnit window lists the tests in the
 * order of the requirements below.</p>
 *
 * <p> <b>Requirements tested.</b></p>
 * <ul>
 * <li> CR-1 (US-CR1): the stored role name "Role2" is shown as "Curator" and a chosen "Curator"
 * 		is stored as "Role2"; every other role name is unchanged.  Tests CH-01 to CH-04,
 * 		guiTools.RoleLabels. </li>
 * <li> CR-2 (US-CR1): an Admin can give and remove the Curator role, and no other role
 * 		changes.  Tests CH-08 to CH-11, Database.updateUserRole(). </li>
 * <li> CR-3 (US-CR2): a Curator can log in as a Curator, a user without the role cannot, and
 * 		the number of roles decides between the home page and the role chooser.  Tests CH-12
 * 		to CH-15, Database.loginRole2() and Database.getNumberOfRoles(). </li>
 * <li> CR-4 (US-CH-V): one shared rule says who is a Curator.  Tests CH-05 to CH-07,
 * 		guiTools.CuratorAccess.isCurator(). </li>
 * <li> CR-5 (US-CH1, US-CH-V): a Curator sees every Contributor's lessons, newest first;
 * 		anyone else is refused and receives no lessons.  Tests CH-16 to CH-20 and CH-36,
 * 		ModelCuratorHome.viewAllLessons() and rowText(). </li>
 * <li> CR-6 (US-CH2, US-CH2-V, US-CH-V): a Curator can search every lesson by keyword;
 * 		invalid search text is refused with a specific message; empty or non-matching text
 * 		finds nothing.  Tests CH-21 to CH-28, ModelCuratorHome.searchAllLessons(). </li>
 * <li> CR-7 (US-CH3, US-CH-V): a Curator can read one lesson in full, with its owner, its
 * 		experience entries, and what is locked.  Tests CH-29 to CH-33,
 * 		ModelCuratorHome.viewLesson(), lockSummary(), and detailsText(). </li>
 * <li> CR-8: the existing Contributor privacy rule (US-R4) and the Curator lock action (Epic
 * 		E6) still behave as before, and share the same Curator rule.  Tests CH-34 and
 * 		CH-35. </li>
 * </ul>
 *
 * <p> <b>Test data.</b>  The tests use the application's real database, which may already hold
 * other people's lessons, so no test assumes the database is empty.  Every title and tag a test
 * creates contains a token that is unique to this run, and searches use that token, so the
 * counts are exact whatever else is stored.  Every lesson and user account a test creates is
 * removed after that test, even when the test fails.</p>
 *
 * <p> <b>How to read the output.</b>  Every check prints the test id, the input, the expected
 * result, and the actual result, then asserts that the Expected and Actual lines are identical.
 * A result is written as "Success: message" or "Refused: message".</p>
 *
 * <p> FoundationsMain must not be running, because H2 allows one connection.</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-11-10 TP3 Aspect 1: tests written before the Curator role and home
 * 							page code
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class CuratorHomeTests {

	/** The message for anyone who is not a Curator (CR-5 to CR-7). */
	private static final String NOT_CURATOR = "Only a curator can view all lessons.";

	/** A description longer than the 10-character minimum. */
	private static final String DESCRIPTION = "Use GitHub Actions to build and test every push.";

	/** A token no other lesson in the database contains, so search counts are exact. */
	private static final String TOKEN = "chk" + System.currentTimeMillis();

	/** A Curator only (the Role2 flag). */
	private static final User CURATOR = new User("tp3curator", "", "", "", "", "", "",
			false, false, true);

	/** An Admin only. */
	private static final User ADMIN = new User("tp3admin", "", "", "", "", "", "",
			true, false, false);

	/** A user with both the Contributor and the Curator roles. */
	private static final User BOTH = new User("tp3both", "", "", "", "", "", "",
			false, true, true);

	/** A user with no role at all. */
	private static final User NO_ROLE = new User("tp3norole", "", "", "", "", "", "",
			false, false, false);

	/** The first Contributor's username; unique for every run. */
	private static final String OWNER_A = "tp3ownerA" + System.currentTimeMillis();

	/** The second Contributor's username; unique for every run. */
	private static final String OWNER_B = "tp3ownerB" + System.currentTimeMillis();

	/** The first Contributor as a User. */
	private static final User CONTRIBUTOR = new User(OWNER_A, "", "", "", "", "", "",
			false, true, false);

	/** A password that satisfies the account rules, for the accounts the tests register. */
	private static final String PASSWORD = "Abcdef1!";

	/** Every lesson id created by the current test, deleted after the test. */
	private static final List<Integer> createdLessons = new ArrayList<Integer>();

	/** Every username registered by the current test, deleted after the test. */
	private static final List<String> createdUsers = new ArrayList<String>();

	/** The application's database, used directly by the role tests. */
	private static final Database db = FoundationsMain.database;

	/**********
	 * Default constructor is not used; JUnit creates an instance for each test.
	 */
	public CuratorHomeTests() {
	}


	/*-*******************************************************************************************

	Setup and cleanup

	 */

	/**********
	 * <p> Method: void connect() </p>
	 *
	 * <p> Description: Opens the application's database once, before the first test. </p>
	 *
	 * @throws SQLException if the database cannot be opened (for example, the application is
	 * 		still running)
	 */
	@BeforeAll
	static void connect() throws SQLException {
		// The tests use the real database through FoundationsMain.database, as the TP2 and lock
		// tests do, so they exercise the same code the application runs
		FoundationsMain.database.connectToDatabase();
	}

	/**********
	 * <p> Method: void disconnect() </p>
	 *
	 * <p> Description: Closes the database after the last test. </p>
	 */
	@AfterAll
	static void disconnect() {
		// Closing releases H2's single connection, so the application can be started again
		FoundationsMain.database.closeConnection();
	}

	/**********
	 * <p> Method: void deleteCreatedData() </p>
	 *
	 * <p> Description: Deletes every lesson and user account the test created, even if the test
	 * failed. </p>
	 */
	@AfterEach
	void deleteCreatedData() {
		// Delete through the Database, not the Contributor Model, because the Model correctly
		// refuses to delete a locked lesson and CH-35 leaves one locked
		for (int id : createdLessons) db.deleteLesson(id);
		createdLessons.clear();
		for (String username : createdUsers) db.deleteUser(username);
		createdUsers.clear();
	}


	/*-*******************************************************************************************

	Helper methods

	 */

	/**********
	 * <p> Method: int makeLesson(String owner, String title, String tags) </p>
	 *
	 * <p> Description: Creates a lesson exactly as a Contributor would, through the TP2
	 * ModelCreateLesson, and remembers it for cleanup. </p>
	 *
	 * @param owner the Contributor who owns the lesson
	 *
	 * @param title the lesson's title
	 *
	 * @param tags the lesson's tags
	 *
	 * @return the new lesson's id
	 */
	private static int makeLesson(String owner, String title, String tags) {
		// Creating through the real Contributor Model guarantees a valid TP2 lesson
		LessonResult result = ModelCreateLesson.createLesson(owner, title, DESCRIPTION, tags,
				"Technical");
		if (!result.isSuccess())
			throw new IllegalStateException("Test setup failed: " + result.getMessage());
		createdLessons.add(result.getLessonId());
		return result.getLessonId();
	}

	/**********
	 * <p> Method: int missingLessonId() </p>
	 *
	 * <p> Description: Returns the id of a lesson that existed and has been deleted. </p>
	 *
	 * @return an id that no lesson has any more
	 */
	private static int missingLessonId() {
		// An id that really existed is more realistic than one that never did
		int id = makeLesson(OWNER_A, "Deleted lesson " + TOKEN, "");
		db.deleteLesson(id);
		return id;
	}

	/**********
	 * <p> Method: String register(String base, boolean admin, boolean contributor,
	 * 		boolean curator) </p>
	 *
	 * <p> Description: Adds a user account to the database and remembers it for cleanup. </p>
	 *
	 * @param base a readable start for the username
	 *
	 * @param admin true to give the Admin role
	 *
	 * @param contributor true to give the Contributor role
	 *
	 * @param curator true to give the Curator role
	 *
	 * @return the new, unique username
	 */
	private static String register(String base, boolean admin, boolean contributor,
			boolean curator) {
		// nanoTime keeps the username unique even when two tests run in the same millisecond
		String username = base + System.nanoTime();
		try {
			db.register(new User(username, PASSWORD, "", "", "", "", "", admin, contributor,
					curator));
		} catch (SQLException e) {
			throw new IllegalStateException("Test setup failed: " + e.getMessage());
		}
		createdUsers.add(username);
		return username;
	}

	/**********
	 * <p> Method: String storedRoles(String username) </p>
	 *
	 * <p> Description: Reads a user's three role flags back from the database, in the order
	 * Admin, Contributor, Curator. </p>
	 *
	 * @param username the user to read
	 *
	 * @return for example "F,T,T" for a Contributor who is also a Curator
	 */
	private static String storedRoles(String username) {
		// Reading from the database, not from the User object, proves what was really saved
		if (!db.getUserAccountDetails(username)) return "no user";
		return tf(db.getCurrentAdminRole()) + "," + tf(db.getCurrentNewRole1()) + ","
				+ tf(db.getCurrentNewRole2());
	}

	/**********
	 * <p> Method: String tf(boolean value) </p>
	 *
	 * <p> Description: Writes a flag as T or F. </p>
	 *
	 * @param value the flag
	 *
	 * @return "T" for true, "F" for false
	 */
	private static String tf(boolean value) {
		// One letter per flag keeps the three flags readable on one line
		return value ? "T" : "F";
	}

	/**********
	 * <p> Method: String outcome(LessonResult result) </p>
	 *
	 * <p> Description: Describes a result as "Success: message" or "Refused: message". </p>
	 *
	 * @param result the result to describe
	 *
	 * @return the description
	 */
	private static String outcome(LessonResult result) {
		// Including success or failure catches a result with the right message but the wrong
		// outcome, such as a refusal reported as a success
		return (result.isSuccess() ? "Success: " : "Refused: ") + result.getMessage();
	}

	/**********
	 * <p> Method: String listed(LessonResult result) </p>
	 *
	 * <p> Description: Describes the list a result carries. </p>
	 *
	 * @param result the result to describe
	 *
	 * @return "no list" if the result carries none, otherwise for example "2 lesson(s)"
	 */
	private static String listed(LessonResult result) {
		// "no list" proves a refusal hands back no lessons at all, not just an empty list
		return (result.getList() == null) ? "no list" : result.getList().size() + " lesson(s)";
	}

	/**********
	 * <p> Method: void check(String id, String input, String expected, String actual) </p>
	 *
	 * <p> Description: Prints the Expected and Actual lines for one check and then asserts that
	 * they are identical. </p>
	 *
	 * @param id the test id, such as "CH-01"
	 *
	 * @param input a readable description of the input
	 *
	 * @param expected the required result
	 *
	 * @param actual the result the code produced
	 */
	private static void check(String id, String input, String expected, String actual) {
		// Printing before asserting means the screencast shows both lines even for a failure
		System.out.println(id + "  Input:    " + input);
		System.out.println(id + "  Expected: " + expected);
		System.out.println(id + "  Actual:   " + actual);
		System.out.println();
		assertEquals(expected, actual, id);
	}


	/*-*******************************************************************************************

	CR-1: the stored role "Role2" is shown as "Curator"

	 */

	/**********
	 * <p> Method: void ch01_role2ShownAsCurator() </p>
	 *
	 * <p> Description: CH-01, positive.  The stored name "Role2" becomes the label "Curator",
	 * which is what every screen shows (US-CR1; CR-1). </p>
	 */
	@Test
	@DisplayName("CH-01 Stored Role2 is shown as Curator (P)")
	void ch01_role2ShownAsCurator() {
		check("CH-01", "RoleLabels.toLabel(\"Role2\")", "Curator", RoleLabels.toLabel("Role2"));
	}

	/**********
	 * <p> Method: void ch02_curatorStoredAsRole2() </p>
	 *
	 * <p> Description: CH-02, positive.  The label "Curator" chosen on a screen becomes the
	 * stored name "Role2", which is what the database and the invitation code expect (CR-1). </p>
	 */
	@Test
	@DisplayName("CH-02 Chosen Curator is stored as Role2 (P)")
	void ch02_curatorStoredAsRole2() {
		check("CH-02", "RoleLabels.toStoredName(\"Curator\")", "Role2",
				RoleLabels.toStoredName("Curator"));
	}

	/**********
	 * <p> Method: void ch03_contributorNamesUnchanged() </p>
	 *
	 * <p> Description: CH-03, positive.  Adding the Curator names did not change the TP2
	 * Contributor names (design decision D12; CR-1). </p>
	 */
	@Test
	@DisplayName("CH-03 Contributor names still translate both ways (P)")
	void ch03_contributorNamesUnchanged() {
		check("CH-03", "toLabel(\"Role1\") | toStoredName(\"Contributor\")",
				"Contributor | Role1",
				RoleLabels.toLabel("Role1") + " | " + RoleLabels.toStoredName("Contributor"));
	}

	/**********
	 * <p> Method: void ch04_otherNamesUnchanged() </p>
	 *
	 * <p> Description: CH-04, negative.  Names that are not a role label are returned
	 * unchanged: "Admin", the "&lt;Select a role&gt;" prompt, a lower-case "curator", and null
	 * (CR-1). </p>
	 */
	@Test
	@DisplayName("CH-04 Other names are not translated (N)")
	void ch04_otherNamesUnchanged() {
		check("CH-04", "toLabel(\"Admin\") | toStoredName(\"Admin\") | "
				+ "toStoredName(\"<Select a role>\") | toStoredName(\"curator\") | "
				+ "toLabel(null) | toStoredName(null)",
				"Admin | Admin | <Select a role> | curator | null | null",
				RoleLabels.toLabel("Admin") + " | " + RoleLabels.toStoredName("Admin") + " | "
				+ RoleLabels.toStoredName("<Select a role>") + " | "
				+ RoleLabels.toStoredName("curator") + " | " + RoleLabels.toLabel(null) + " | "
				+ RoleLabels.toStoredName(null));
	}


	/*-*******************************************************************************************

	CR-4: one shared rule says who is a Curator

	 */

	/**********
	 * <p> Method: void ch05_curatorFlagMakesACurator() </p>
	 *
	 * <p> Description: CH-05, positive.  A user with the Curator role is a Curator, whether or
	 * not the user also has the Contributor role (US-CH-V; CR-4). </p>
	 */
	@Test
	@DisplayName("CH-05 A user with the Curator role is a Curator (P)")
	void ch05_curatorFlagMakesACurator() {
		check("CH-05", "isCurator(Curator only) | isCurator(Contributor and Curator)",
				"true | true",
				CuratorAccess.isCurator(CURATOR) + " | " + CuratorAccess.isCurator(BOTH));
	}

	/**********
	 * <p> Method: void ch06_otherRolesAreNotCurators() </p>
	 *
	 * <p> Description: CH-06, negative.  A Contributor and an Admin are not Curators; being an
	 * Admin does not include the Curator role (CR-4). </p>
	 */
	@Test
	@DisplayName("CH-06 A Contributor or an Admin is not a Curator (N)")
	void ch06_otherRolesAreNotCurators() {
		check("CH-06", "isCurator(Contributor only) | isCurator(Admin only)", "false | false",
				CuratorAccess.isCurator(CONTRIBUTOR) + " | " + CuratorAccess.isCurator(ADMIN));
	}

	/**********
	 * <p> Method: void ch07_nobodyIsNotACurator() </p>
	 *
	 * <p> Description: CH-07, negative.  No user at all (null) and a user with no role are not
	 * Curators, and the check does not crash on null (CWE-476; CR-4). </p>
	 */
	@Test
	@DisplayName("CH-07 Null and a user with no role are not Curators (N)")
	void ch07_nobodyIsNotACurator() {
		check("CH-07", "isCurator(null) | isCurator(user with no role)", "false | false",
				CuratorAccess.isCurator(null) + " | " + CuratorAccess.isCurator(NO_ROLE));
	}


	/*-*******************************************************************************************

	CR-2: an Admin can give and remove the Curator role

	 */

	/**********
	 * <p> Method: void ch08_adminGivesCuratorRole() </p>
	 *
	 * <p> Description: CH-08, positive.  Giving the Curator role to a Contributor sets the
	 * Curator flag and leaves the Contributor flag alone (US-CR1; CR-2). </p>
	 */
	@Test
	@DisplayName("CH-08 An Admin gives the Curator role (P)")
	void ch08_adminGivesCuratorRole() {
		String user = register("tp3give", false, true, false);
		boolean saved = db.updateUserRole(user, "Role2", "true");
		check("CH-08", "Contributor; updateUserRole(user, \"Role2\", \"true\")",
				"returned true | roles F,T,T", "returned " + saved + " | roles "
				+ storedRoles(user));
	}

	/**********
	 * <p> Method: void ch09_adminRemovesCuratorRole() </p>
	 *
	 * <p> Description: CH-09, positive.  Removing the Curator role clears the Curator flag and
	 * leaves the Contributor flag alone (US-CR1; CR-2). </p>
	 */
	@Test
	@DisplayName("CH-09 An Admin removes the Curator role (P)")
	void ch09_adminRemovesCuratorRole() {
		String user = register("tp3remove", false, true, true);
		boolean saved = db.updateUserRole(user, "Role2", "false");
		check("CH-09", "Contributor and Curator; updateUserRole(user, \"Role2\", \"false\")",
				"returned true | roles F,T,F", "returned " + saved + " | roles "
				+ storedRoles(user));
	}

	/**********
	 * <p> Method: void ch10_roleChosenByItsScreenName() </p>
	 *
	 * <p> Description: CH-10, positive.  The Add/Remove Roles page shows "Curator" and passes
	 * the chosen label through RoleLabels.toStoredName(); that path gives the role to a user who
	 * had no role, and the Admin and Contributor flags stay false.  The database also accepts
	 * the word "Curator" itself, as it accepts "Contributor" for Role1 (CR-1 and CR-2). </p>
	 */
	@Test
	@DisplayName("CH-10 The role chosen as Curator on the screen is saved (P)")
	void ch10_roleChosenByItsScreenName() {
		String user = register("tp3label", false, false, false);
		String other = register("tp3word", false, false, false);
		boolean saved = db.updateUserRole(user, RoleLabels.toStoredName("Curator"), "true");
		boolean savedByWord = db.updateUserRole(other, "Curator", "true");
		check("CH-10", "No roles; updateUserRole(user, toStoredName(\"Curator\"), \"true\") / "
				+ "updateUserRole(user, \"Curator\", \"true\")",
				"returned true | roles F,F,T / returned true | roles F,F,T",
				"returned " + saved + " | roles " + storedRoles(user) + " / returned "
				+ savedByWord + " | roles " + storedRoles(other));
	}

	/**********
	 * <p> Method: void ch11_unknownRoleChangesNothing() </p>
	 *
	 * <p> Description: CH-11, negative.  A role name the system does not have is refused and no
	 * flag changes (CR-2). </p>
	 */
	@Test
	@DisplayName("CH-11 An unknown role name changes nothing (N)")
	void ch11_unknownRoleChangesNothing() {
		String user = register("tp3unknown", false, true, false);
		boolean saved = db.updateUserRole(user, "Reviewer", "true");
		check("CH-11", "Contributor; updateUserRole(user, \"Reviewer\", \"true\")",
				"returned false | roles F,T,F", "returned " + saved + " | roles "
				+ storedRoles(user));
	}


	/*-*******************************************************************************************

	CR-3: logging in as a Curator

	 */

	/**********
	 * <p> Method: void ch12_curatorLogsIn() </p>
	 *
	 * <p> Description: CH-12, positive.  A user whose only role is Curator is accepted by the
	 * Curator login check and has exactly one role, so the login page sends the user straight
	 * to the Curator Home Page (US-CR2; CR-3). </p>
	 */
	@Test
	@DisplayName("CH-12 A Curator logs in and has one role (P)")
	void ch12_curatorLogsIn() {
		String name = register("tp3login", false, false, true);
		User user = new User(name, PASSWORD, "", "", "", "", "", false, false, true);
		check("CH-12", "Curator only, correct password",
				"loginRole2 true | 1 role(s)",
				"loginRole2 " + db.loginRole2(user) + " | " + db.getNumberOfRoles(user)
				+ " role(s)");
	}

	/**********
	 * <p> Method: void ch13_nonCuratorCannotLogInAsCurator() </p>
	 *
	 * <p> Description: CH-13, negative.  The Curator login check refuses a Contributor who is
	 * not a Curator, and refuses a Curator with the wrong password (CR-3). </p>
	 */
	@Test
	@DisplayName("CH-13 A non-Curator or a wrong password is refused (N)")
	void ch13_nonCuratorCannotLogInAsCurator() {
		String contributor = register("tp3notcur", false, true, false);
		String curator = register("tp3wrongpw", false, false, true);
		// The User objects claim the Curator role, so only the database can refuse them
		User claims = new User(contributor, PASSWORD, "", "", "", "", "", false, false, true);
		User wrongPassword = new User(curator, "Wrong999!", "", "", "", "", "", false, false,
				true);
		check("CH-13", "Contributor only | Curator with the wrong password",
				"loginRole2 false | loginRole2 false",
				"loginRole2 " + db.loginRole2(claims) + " | loginRole2 "
				+ db.loginRole2(wrongPassword));
	}

	/**********
	 * <p> Method: void ch14_twoRolesMeansAChoice() </p>
	 *
	 * <p> Description: CH-14, positive.  A user who is both a Contributor and a Curator has two
	 * roles, so the login page shows the role chooser, and both login checks accept the user
	 * (US-CR2; CR-3). </p>
	 */
	@Test
	@DisplayName("CH-14 A Contributor who is also a Curator has two roles (P)")
	void ch14_twoRolesMeansAChoice() {
		String name = register("tp3two", false, true, true);
		User user = new User(name, PASSWORD, "", "", "", "", "", false, true, true);
		check("CH-14", "Contributor and Curator, correct password",
				"2 role(s) | loginRole1 true | loginRole2 true",
				db.getNumberOfRoles(user) + " role(s) | loginRole1 " + db.loginRole1(user)
				+ " | loginRole2 " + db.loginRole2(user));
	}

	/**********
	 * <p> Method: void ch15_removedCuratorCannotLogIn() </p>
	 *
	 * <p> Description: CH-15, negative.  After an Admin removes the Curator role, the Curator
	 * login check refuses that user, even though the user was accepted before (US-CR1;
	 * CR-2 and CR-3). </p>
	 */
	@Test
	@DisplayName("CH-15 After the role is removed the Curator login is refused (N)")
	void ch15_removedCuratorCannotLogIn() {
		String name = register("tp3removed", false, false, true);
		User user = new User(name, PASSWORD, "", "", "", "", "", false, false, true);
		boolean before = db.loginRole2(user);
		db.updateUserRole(name, "Role2", "false");
		check("CH-15", "Curator; login, remove the Curator role, login again",
				"before true | after false",
				"before " + before + " | after " + db.loginRole2(user));
	}


	/*-*******************************************************************************************

	CR-5: the Curator's list of every lesson

	 */

	/**********
	 * <p> Method: void ch16_curatorSeesEveryContributorsLessons() </p>
	 *
	 * <p> Description: CH-16, positive.  A Curator's list holds lessons from two different
	 * Contributors and is as long as the number of lessons in the database (US-CH1; CR-5). </p>
	 */
	@Test
	@DisplayName("CH-16 A Curator sees every Contributor's lessons (P)")
	void ch16_curatorSeesEveryContributorsLessons() {
		int a = makeLesson(OWNER_A, "Set up CI " + TOKEN, "ci");
		int b = makeLesson(OWNER_B, "Plan the release " + TOKEN, "release");
		LessonResult result = ModelCuratorHome.viewAllLessons(CURATOR);
		LessonsLearnedList list = result.getList();
		check("CH-16", "Curator; one lesson from each of two Contributors",
				"Success | has A's lesson true | has B's lesson true | every lesson listed true",
				(result.isSuccess() ? "Success" : "Refused") + " | has A's lesson "
				+ list.contains(a) + " | has B's lesson " + list.contains(b)
				+ " | every lesson listed " + (list.size() == db.getNumberOfLessons()));
	}

	/**********
	 * <p> Method: void ch17_newestLessonFirst() </p>
	 *
	 * <p> Description: CH-17, positive.  The list is newest first, so a lesson just added is
	 * the first row and the one added before it is the second (US-CH1; CR-5). </p>
	 */
	@Test
	@DisplayName("CH-17 The list is newest first (P)")
	void ch17_newestLessonFirst() {
		int older = makeLesson(OWNER_A, "Older lesson " + TOKEN, "");
		int newer = makeLesson(OWNER_B, "Newer lesson " + TOKEN, "");
		LessonsLearnedList list = ModelCuratorHome.viewAllLessons(CURATOR).getList();
		check("CH-17", "Curator; an older lesson, then a newer lesson",
				"first row is the newer lesson true | second row is the older lesson true",
				"first row is the newer lesson " + (list.get(0).getId() == newer)
				+ " | second row is the older lesson " + (list.get(1).getId() == older));
	}

	/**********
	 * <p> Method: void ch18_contributorIsRefusedTheFullList() </p>
	 *
	 * <p> Description: CH-18, negative.  A Contributor who asks for the full list is refused
	 * and receives no list, which protects the privacy story US-R4 (US-CH-V; CR-5). </p>
	 */
	@Test
	@DisplayName("CH-18 A Contributor is refused the full list (N)")
	void ch18_contributorIsRefusedTheFullList() {
		makeLesson(OWNER_B, "Private to B " + TOKEN, "");
		LessonResult result = ModelCuratorHome.viewAllLessons(CONTRIBUTOR);
		check("CH-18", "Contributor only; viewAllLessons",
				"Refused: " + NOT_CURATOR + " | no list", outcome(result) + " | "
				+ listed(result));
	}

	/**********
	 * <p> Method: void ch19_adminAndNobodyAreRefused() </p>
	 *
	 * <p> Description: CH-19, negative.  An Admin without the Curator role, a user with no
	 * role, and no user at all are each refused and receive no list (US-CH-V; CR-5). </p>
	 */
	@Test
	@DisplayName("CH-19 An Admin, a user with no role, and null are refused (N)")
	void ch19_adminAndNobodyAreRefused() {
		String refused = "Refused: " + NOT_CURATOR + " | no list";
		LessonResult admin = ModelCuratorHome.viewAllLessons(ADMIN);
		LessonResult noRole = ModelCuratorHome.viewAllLessons(NO_ROLE);
		LessonResult nobody = ModelCuratorHome.viewAllLessons(null);
		check("CH-19", "Admin only / no role / null; viewAllLessons",
				refused + " / " + refused + " / " + refused,
				outcome(admin) + " | " + listed(admin) + " / " + outcome(noRole) + " | "
				+ listed(noRole) + " / " + outcome(nobody) + " | " + listed(nobody));
	}

	/**********
	 * <p> Method: void ch20_userWithBothRolesSeesTheFullList() </p>
	 *
	 * <p> Description: CH-20, positive.  A user who is a Contributor and a Curator may use the
	 * Curator's list, and it includes another Contributor's lesson (US-CH1; CR-5). </p>
	 */
	@Test
	@DisplayName("CH-20 A user with both roles sees the full list (P)")
	void ch20_userWithBothRolesSeesTheFullList() {
		int b = makeLesson(OWNER_B, "Owned by B " + TOKEN, "");
		LessonResult result = ModelCuratorHome.viewAllLessons(BOTH);
		check("CH-20", "Contributor and Curator; viewAllLessons",
				"Success | has B's lesson true",
				(result.isSuccess() ? "Success" : "Refused") + " | has B's lesson "
				+ (result.getList() != null && result.getList().contains(b)));
	}


	/*-*******************************************************************************************

	CR-6: the Curator's keyword search over every lesson

	 */

	/**********
	 * <p> Method: void ch21_searchFindsOneLesson() </p>
	 *
	 * <p> Description: CH-21, positive.  A keyword that only one lesson contains finds exactly
	 * that lesson, although another Contributor owns it (US-CH2; CR-6). </p>
	 */
	@Test
	@DisplayName("CH-21 A search finds a single matching lesson (P)")
	void ch21_searchFindsOneLesson() {
		int a = makeLesson(OWNER_A, "Rollback " + TOKEN, "");
		makeLesson(OWNER_B, "Something else entirely", "");
		LessonResult result = ModelCuratorHome.searchAllLessons(CURATOR, TOKEN);
		check("CH-21", "Curator; search for the run's unique token",
				"Success: Search complete | 1 lesson(s) | the matching lesson true",
				outcome(result) + " | " + listed(result) + " | the matching lesson "
				+ result.getList().contains(a));
	}

	/**********
	 * <p> Method: void ch22_searchFindsManyLessons() </p>
	 *
	 * <p> Description: CH-22, positive.  A keyword two Contributors both used finds both
	 * lessons (US-CH2; CR-6). </p>
	 */
	@Test
	@DisplayName("CH-22 A search finds lessons from several Contributors (P)")
	void ch22_searchFindsManyLessons() {
		makeLesson(OWNER_A, "Deploy checklist " + TOKEN, "");
		makeLesson(OWNER_B, "Deploy rollback " + TOKEN, "");
		makeLesson(OWNER_B, "Unrelated lesson", "");
		LessonResult result = ModelCuratorHome.searchAllLessons(CURATOR, "deploy " + TOKEN);
		check("CH-22", "Curator; search \"deploy <token>\" with two matching lessons",
				"Success: Search complete | 2 lesson(s)", outcome(result) + " | "
				+ listed(result));
	}

	/**********
	 * <p> Method: void ch23_searchLooksBeyondTheTitle() </p>
	 *
	 * <p> Description: CH-23, positive.  The search ignores case and also matches a keyword
	 * that appears only in the tags, not in the title (US-CH2; CR-6). </p>
	 */
	@Test
	@DisplayName("CH-23 A search matches tags and ignores case (P)")
	void ch23_searchLooksBeyondTheTitle() {
		makeLesson(OWNER_A, "Set up CI", "pipeline, " + TOKEN);
		LessonResult result = ModelCuratorHome.searchAllLessons(CURATOR, TOKEN.toUpperCase());
		check("CH-23", "Curator; token only in the tags, searched in upper case",
				"Success: Search complete | 1 lesson(s)", outcome(result) + " | "
				+ listed(result));
	}

	/**********
	 * <p> Method: void ch24_searchWithNoMatch() </p>
	 *
	 * <p> Description: CH-24, negative.  A keyword no lesson contains is not an error; it finds
	 * nothing (US-CH2-V; CR-6). </p>
	 */
	@Test
	@DisplayName("CH-24 A search that matches nothing finds no lessons (N)")
	void ch24_searchWithNoMatch() {
		makeLesson(OWNER_A, "Set up CI " + TOKEN, "");
		LessonResult result = ModelCuratorHome.searchAllLessons(CURATOR, "nomatch" + TOKEN);
		check("CH-24", "Curator; search for text no lesson contains",
				"Success: Search complete | 0 lesson(s)", outcome(result) + " | "
				+ listed(result));
	}

	/**********
	 * <p> Method: void ch25_emptySearchFindsNothing() </p>
	 *
	 * <p> Description: CH-25, negative.  Empty search text, or only spaces, is valid but finds
	 * nothing, the same rule as the Contributor search (design decision D6; US-CH2-V;
	 * CR-6). </p>
	 */
	@Test
	@DisplayName("CH-25 An empty search finds no lessons (N)")
	void ch25_emptySearchFindsNothing() {
		makeLesson(OWNER_A, "Set up CI " + TOKEN, "");
		LessonResult empty = ModelCuratorHome.searchAllLessons(CURATOR, "");
		LessonResult spaces = ModelCuratorHome.searchAllLessons(CURATOR, "   ");
		check("CH-25", "Curator; search \"\" / search \"   \"",
				"Success: Search complete | 0 lesson(s) / Success: Search complete | 0 lesson(s)",
				outcome(empty) + " | " + listed(empty) + " / " + outcome(spaces) + " | "
				+ listed(spaces));
	}

	/**********
	 * <p> Method: void ch26_searchTextLengthBoundary() </p>
	 *
	 * <p> Description: CH-26, boundary.  Search text of 254 characters, the maximum, is
	 * accepted; 255 characters is refused with a specific message and no search is run
	 * (US-CH2-V; CR-6). </p>
	 */
	@Test
	@DisplayName("CH-26 Search text of 254 characters is accepted and 255 is refused (N)")
	void ch26_searchTextLengthBoundary() {
		char[] letters = new char[255];
		Arrays.fill(letters, 'a');
		String tooLong = new String(letters);
		LessonResult atLimit = ModelCuratorHome.searchAllLessons(CURATOR,
				tooLong.substring(0, 254));
		LessonResult overLimit = ModelCuratorHome.searchAllLessons(CURATOR, tooLong);
		check("CH-26", "Curator; 254 letters / 255 letters",
				"Success: Search complete | 0 lesson(s) / Refused: Search text is too long. "
				+ "| no list",
				outcome(atLimit) + " | " + listed(atLimit) + " / " + outcome(overLimit) + " | "
				+ listed(overLimit));
	}

	/**********
	 * <p> Method: void ch27_searchTextWithControlCharacter() </p>
	 *
	 * <p> Description: CH-27, negative.  Search text containing a control character (a tab) is
	 * refused with a specific message (US-CH2-V; CR-6). </p>
	 */
	@Test
	@DisplayName("CH-27 Search text with a control character is refused (N)")
	void ch27_searchTextWithControlCharacter() {
		LessonResult result = ModelCuratorHome.searchAllLessons(CURATOR, "set\tup");
		check("CH-27", "Curator; search text containing a tab",
				"Refused: Search text contains an invalid character. | no list",
				outcome(result) + " | " + listed(result));
	}

	/**********
	 * <p> Method: void ch28_roleIsCheckedBeforeTheSearchText() </p>
	 *
	 * <p> Description: CH-28, negative.  A Contributor who searches is refused, and the role is
	 * checked before the text: with invalid text the Contributor still receives the role
	 * message, not the text message (US-CH-V; CR-6). </p>
	 */
	@Test
	@DisplayName("CH-28 A Contributor's search is refused before the text is checked (N)")
	void ch28_roleIsCheckedBeforeTheSearchText() {
		makeLesson(OWNER_B, "Private to B " + TOKEN, "");
		LessonResult valid = ModelCuratorHome.searchAllLessons(CONTRIBUTOR, TOKEN);
		LessonResult invalid = ModelCuratorHome.searchAllLessons(CONTRIBUTOR, "set\tup");
		String refused = "Refused: " + NOT_CURATOR + " | no list";
		check("CH-28", "Contributor only; valid search text / search text containing a tab",
				refused + " / " + refused,
				outcome(valid) + " | " + listed(valid) + " / " + outcome(invalid) + " | "
				+ listed(invalid));
	}


	/*-*******************************************************************************************

	CR-7: reading one lesson in full

	 */

	/**********
	 * <p> Method: void ch29_curatorReadsAnyLesson() </p>
	 *
	 * <p> Description: CH-29, positive.  A Curator opens a lesson another user owns and
	 * receives the lesson, its owner, and its experience entries (US-CH3; CR-7). </p>
	 */
	@Test
	@DisplayName("CH-29 A Curator reads a Contributor's lesson with its experience (P)")
	void ch29_curatorReadsAnyLesson() {
		int a = makeLesson(OWNER_A, "Set up CI " + TOKEN, "ci");
		ModelAddExperience.addExperience(OWNER_A, a, "3.5", "Reused it on the mobile app.");
		LessonResult result = ModelCuratorHome.viewLesson(CURATOR, a);
		LessonsLearned lesson = result.getLesson();
		check("CH-29", "Curator; a lesson owned by Contributor A with one experience entry",
				"Success | title Set up CI " + TOKEN + " | owner " + OWNER_A
				+ " | 1 experience entry of 3.5 h",
				(result.isSuccess() ? "Success" : "Refused") + " | title " + lesson.getTitle()
				+ " | owner " + lesson.getOwnerUserName() + " | "
				+ lesson.getExperienceEntries().size() + " experience entry of "
				+ lesson.getExperienceEntries().get(0).getTimeEffortHours() + " h");
	}

	/**********
	 * <p> Method: void ch30_missingLessonIsNotFound() </p>
	 *
	 * <p> Description: CH-30, negative.  A lesson that has been deleted gives "Lesson not
	 * found." and no lesson (US-CH-V; CR-7). </p>
	 */
	@Test
	@DisplayName("CH-30 A Curator opening a deleted lesson gets Lesson not found (N)")
	void ch30_missingLessonIsNotFound() {
		LessonResult result = ModelCuratorHome.viewLesson(CURATOR, missingLessonId());
		check("CH-30", "Curator; a lesson that was deleted",
				"Refused: " + LessonAccess.NOT_FOUND + " | lesson null",
				outcome(result) + " | lesson " + result.getLesson());
	}

	/**********
	 * <p> Method: void ch31_roleIsCheckedBeforeTheLesson() </p>
	 *
	 * <p> Description: CH-31, negative.  A Contributor is refused by the Curator's read, even
	 * for the Contributor's own lesson, and receives the same message for a lesson that does
	 * not exist, so the refusal reveals nothing about which lessons exist (CWE-200; US-CH-V;
	 * CR-7). </p>
	 */
	@Test
	@DisplayName("CH-31 A Contributor is refused before the lesson is looked up (N)")
	void ch31_roleIsCheckedBeforeTheLesson() {
		int own = makeLesson(OWNER_A, "My own lesson " + TOKEN, "");
		LessonResult existing = ModelCuratorHome.viewLesson(CONTRIBUTOR, own);
		LessonResult missing = ModelCuratorHome.viewLesson(CONTRIBUTOR, missingLessonId());
		String refused = "Refused: " + NOT_CURATOR + " | lesson null";
		check("CH-31", "Contributor only; own lesson / deleted lesson",
				refused + " / " + refused,
				outcome(existing) + " | lesson " + existing.getLesson() + " / "
				+ outcome(missing) + " | lesson " + missing.getLesson());
	}

	/**********
	 * <p> Method: void ch32_lockSummary() </p>
	 *
	 * <p> Description: CH-32, positive.  The lock summary names exactly the locks that are set,
	 * in the fixed order used by the lock action: nothing, two fields, the whole lesson together
	 * with those fields, and, on a second lesson, the whole lesson alone.  The last case proves
	 * the summary does not list every field just because the whole lesson is locked (US-CH3;
	 * CR-7). </p>
	 */
	@Test
	@DisplayName("CH-32 The lock summary names what is locked (P)")
	void ch32_lockSummary() {
		int a = makeLesson(OWNER_A, "Set up CI " + TOKEN, "ci");
		String none = ModelCuratorHome.lockSummary(LessonsLearned.read(a));
		db.setLessonLocks(a, Arrays.asList("tags", "title"), true);
		String fields = ModelCuratorHome.lockSummary(LessonsLearned.read(a));
		db.setLessonLocks(a, Arrays.asList("entire"), true);
		String entireAndFields = ModelCuratorHome.lockSummary(LessonsLearned.read(a));
		int b = makeLesson(OWNER_B, "Plan the release " + TOKEN, "");
		db.setLessonLocks(b, Arrays.asList("entire"), true);
		String entireOnly = ModelCuratorHome.lockSummary(LessonsLearned.read(b));
		check("CH-32", "nothing locked / tags and title locked / entire lesson locked as well "
				+ "/ another lesson with only the entire lesson locked",
				"nothing / title, tags / entire lesson, title, tags / entire lesson",
				none + " / " + fields + " / " + entireAndFields + " / " + entireOnly);
	}

	/**********
	 * <p> Method: void ch33_detailsText() </p>
	 *
	 * <p> Description: CH-33, positive.  The details shown for a selected lesson are its title,
	 * owner, category, tags, locks, description, and experience entries, one fact per line
	 * (US-CH3; CR-7). </p>
	 */
	@Test
	@DisplayName("CH-33 The details text shows every part of the lesson (P)")
	void ch33_detailsText() {
		int a = makeLesson(OWNER_A, "Set up CI " + TOKEN, "ci, build");
		ModelAddExperience.addExperience(OWNER_A, a, "3.5", "Reused it on the mobile app.");
		ModelAddExperience.addExperience(OWNER_A, a, "4", "Reused it on the web app.");
		db.setLessonLocks(a, Arrays.asList("title"), true);
		String expected = "Title: Set up CI " + TOKEN + "\n"
				+ "Owner: " + OWNER_A + "\n"
				+ "Category: Technical\n"
				+ "Tags: ci, build\n"
				+ "Locked: title\n"
				+ "\n"
				+ "Description:\n"
				+ DESCRIPTION + "\n"
				+ "\n"
				+ "Experience entries: 2\n"
				+ "3.5 h   Reused it on the mobile app.\n"
				+ "4 h   Reused it on the web app.";
		String actual = ModelCuratorHome.detailsText(
				ModelCuratorHome.viewLesson(CURATOR, a).getLesson());
		check("CH-33", "Curator; a lesson with two tags, a locked title, and two experience "
				+ "entries", expected, actual);
	}


	/*-*******************************************************************************************

	CR-8: the Contributor rule and the lock action are unchanged, and CR-5's row text

	 */

	/**********
	 * <p> Method: void ch34_contributorPrivacyUnchanged() </p>
	 *
	 * <p> Description: CH-34, regression.  With the Curator's list in place, a Contributor's
	 * own list still never includes another Contributor's lesson (US-R4), while the Curator's
	 * list includes both (CR-8). </p>
	 */
	@Test
	@DisplayName("CH-34 A Contributor's own list still shows only that Contributor's lessons (P)")
	void ch34_contributorPrivacyUnchanged() {
		int a = makeLesson(OWNER_A, "Owned by A " + TOKEN, "");
		int b = makeLesson(OWNER_B, "Owned by B " + TOKEN, "");
		LessonsLearnedList mine = ModelContributorHome.viewMyLessons(OWNER_A);
		LessonsLearnedList all = ModelCuratorHome.viewAllLessons(CURATOR).getList();
		check("CH-34", "Contributor A's list / the Curator's list",
				"A's list: has A's true, has B's false / Curator's list: has A's true, "
				+ "has B's true",
				"A's list: has A's " + mine.contains(a) + ", has B's " + mine.contains(b)
				+ " / Curator's list: has A's " + all.contains(a) + ", has B's "
				+ all.contains(b));
	}

	/**********
	 * <p> Method: void ch35_lockActionSharesTheCuratorRule() </p>
	 *
	 * <p> Description: CH-35, integration.  The lock action (Epic E6) still refuses a
	 * Contributor and accepts a Curator, and a lesson the Curator locked is shown as locked in
	 * the Curator's list (CR-8). </p>
	 */
	@Test
	@DisplayName("CH-35 The lock action uses the same Curator rule and its lock shows (P)")
	void ch35_lockActionSharesTheCuratorRule() {
		int a = makeLesson(OWNER_A, "Set up CI " + TOKEN, "");
		List<String> title = Arrays.asList("title");
		LessonResult refused = ModelCuratorLock.changeLocks(CONTRIBUTOR, a, title, true, true);
		LessonResult locked = ModelCuratorLock.changeLocks(CURATOR, a, title, true, true);
		LessonsLearned row = ModelCuratorHome.viewAllLessons(CURATOR).getList().getById(a);
		check("CH-35", "Contributor locks the title / Curator locks the title / the list row",
				"Refused: Only a curator can lock or unlock lessons. / Success: Locked: title. "
				+ "/ locked true",
				outcome(refused) + " / " + outcome(locked) + " / locked " + row.isLocked());
	}

	/**********
	 * <p> Method: void ch36_rowText() </p>
	 *
	 * <p> Description: CH-36, positive.  A row of the Curator's list shows the title, the
	 * category, and the owner, and adds "(locked)" once any part of the lesson is locked
	 * (US-CH1; CR-5). </p>
	 */
	@Test
	@DisplayName("CH-36 A list row shows the title, category, owner, and locked state (P)")
	void ch36_rowText() {
		int a = makeLesson(OWNER_A, "Set up CI " + TOKEN, "");
		String unlocked = ModelCuratorHome.rowText(LessonsLearned.read(a));
		db.setLessonLocks(a, Arrays.asList("description"), true);
		String locked = ModelCuratorHome.rowText(LessonsLearned.read(a));
		String row = "Set up CI " + TOKEN + "   [Technical]   by " + OWNER_A;
		check("CH-36", "a lesson with nothing locked / the same lesson with a locked field",
				row + " / " + row + "   (locked)", unlocked + " / " + locked);
	}
}
