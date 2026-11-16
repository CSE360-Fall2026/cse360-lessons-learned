package lessonTests;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Scanner;

import applicationMain.FoundationsMain;
import entityClasses.LessonsLearned;
import entityClasses.LessonsLearnedList;
import guiAddExperience.ModelAddExperience;
import guiCreateLesson.ModelCreateLesson;
import guiTools.LessonResult;

/*******
 * <p> Title: ManualGuiTests Class. </p>
 *
 * <p> Description: The manual tests for the parts of the Contributor User Stories that can
 * only be judged by looking at the screens: messages shown beneath the right field, buttons that
 * are enabled or disabled, the "Are you sure?" dialog, and locked fields shown as read-only.
 * The semi-automated test classes prove that each page's Model behaves correctly; these tests
 * prove that the pages built on those Models show that behavior to the Contributor.</p>
 *
 * <p> <b>How to run the manual tests.</b></p>
 * <ol>
 * <li> Make sure FoundationsMain is not running, then run this class with the arguments
 * 		<code>setup USERNAME</code>, where USERNAME is an account that has the Contributor role.
 * 		This adds five sample lessons to that account, two of them locked as a Phase Three
 * 		Curator would lock them (decision D9: no Contributor screen can set a lock). </li>
 * <li> Start FoundationsMain and log in as USERNAME. </li>
 * <li> Run this class again with no arguments.  It prints each test's steps and expected
 * 		results, one at a time.  Perform the steps in the application, compare the screen with
 * 		the expected results, and type P (pass), F (fail), or S (skip). </li>
 * <li> When finished, close FoundationsMain and run this class with <code>cleanup
 * 		USERNAME</code> to remove that account's lessons. </li>
 * </ol>
 *
 * <p> <b>How to read the output:</b> after the last test the class prints how many tests the
 * tester recorded as passed, failed, and skipped.  A test passes only if every expected result
 * listed for it was seen on the screen.</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-10-16 TP2 Task 6: manual GUI tests and their sample data
 */
public class ManualGuiTests {

	/** How many manual tests the tester recorded as passed. */
	private static int passed = 0;

	/** How many manual tests the tester recorded as failed. */
	private static int failed = 0;

	/** How many manual tests the tester skipped. */
	private static int skipped = 0;

	/** Reads the tester's answers from the console. */
	private static Scanner input;

	/**********
	 * Default constructor is not used; every member is static.
	 */
	public ManualGuiTests() {
	}

	/**********
	 * With "setup USERNAME", adds the sample lessons; with "cleanup USERNAME", removes that
	 * account's lessons; with no arguments, walks the tester through manual tests M1 to M7.
	 *
	 * @param args	nothing, or "setup USERNAME", or "cleanup USERNAME"
	 */
	public static void main(String[] args) {
		if (args.length == 2 && args[0].equalsIgnoreCase("setup")) {
			setUpSampleLessons(args[1]);
			return;
		}
		if (args.length == 2 && args[0].equalsIgnoreCase("cleanup")) {
			cleanUpSampleLessons(args[1]);
			return;
		}

		input = new Scanner(System.in);
		System.out.println("TP2 Manual GUI Tests - Contributor pages");
		System.out.println("Before starting: run \"ManualGuiTests setup USERNAME\" with the "
				+ "application closed,\nthen start FoundationsMain and log in as USERNAME.");
		testM1CreateLessonPage();
		testM2HomeListAndSearch();
		testM3OpenLessonFromList();
		testM4LockedLessonDisplay();
		testM5EditLessonPage();
		testM6AddExperienceWithTeamMembers();
		testM7DeleteWithConfirmation();

		System.out.println("____________________________________________________________________________");
		System.out.println();
		System.out.println("Manual tests recorded as passed:  " + passed);
		System.out.println("Manual tests recorded as failed:  " + failed);
		System.out.println("Manual tests skipped:             " + skipped);
	}


	/*-*******************************************************************************************

	The manual tests

	 */

	/**********
	 * <p> Test M1: Create Lesson Page - Errors Beneath Fields, Save Only When Valid. </p>
	 *
	 * <p> <b>Requirements:</b> US-C1 (a new lesson is saved and can be retrieved) and US-C1-V
	 * (each problem gets a specific message, and nothing is saved until it is fixed). </p>
	 *
	 * <p> <b>How the code satisfies them:</b> ControllerCreateLesson re-checks the form on
	 * every keystroke and disables "Create Lesson" while anything is invalid; when a field loses
	 * focus its message appears beneath it.  "Create Lesson" calls ModelCreateLesson.createLesson
	 * and opens the new lesson's details with "Lesson created". </p>
	 *
	 * <p> <b>How to interpret the result:</b> record P only if every expected result is seen;
	 * in particular, no lesson may be created while a message is showing. </p>
	 */
	private static void testM1CreateLessonPage() {
		manualTest("M1", "Create Lesson Page - Errors Beneath Fields, Save Only When Valid",
				"US-C1, US-C1-V",
				new String[] {
					"On the Contributor Home Page, press New Lesson.",
					"Without typing anything, look at the Create Lesson button.",
					"Type \"short\" in Description, then click in Tags.",
					"Type the title \"Manual test lesson\", replace the description with "
						+ "\"Created during manual test M1.\", and choose the category Technical.",
					"Press Create Lesson." },
				new String[] {
					"Create Lesson is disabled on the blank form.",
					"\"Description must be between 10 and 5000 characters.\" appears beneath "
						+ "Description, and Create Lesson stays disabled.",
					"The message disappears once the description is fixed, and Create Lesson "
						+ "becomes enabled.",
					"The Lesson Details page opens for \"Manual test lesson\" with the message "
						+ "\"Lesson created\"." });
	}

	/**********
	 * <p> Test M2: Home Page - My List, Title Search, Invalid Search, and Clear. </p>
	 *
	 * <p> <b>Requirements:</b> US-R1 (my list), US-R2 (search my titles and see the subset),
	 * US-R2-V (too-long search text gets a message and the list does not change), and US-R4
	 * (only my lessons appear). </p>
	 *
	 * <p> <b>How the code satisfies them:</b> ControllerContributorHome fills the list from
	 * ModelContributorHome.viewMyLessons and runs searches through searchMyLessons; on a refused
	 * search it shows the message and leaves the list as it was; Clear reloads the full
	 * list. </p>
	 *
	 * <p> <b>How to interpret the result:</b> record P only if the counts and messages match
	 * exactly. </p>
	 */
	private static void testM2HomeListAndSearch() {
		manualTest("M2", "Home Page - My List, Title Search, Invalid Search, and Clear",
				"US-R1, US-R2, US-R2-V, US-R4",
				new String[] {
					"Return to the Contributor Home Page and look at the list.",
					"Type \"Sprint\" in the search box and press Search.",
					"Select all the text in the search box, paste the 255-character line "
						+ "below, and press Search:\n             " + TestSupport.rep("a", 255),
					"Press Clear." },
				new String[] {
					"The list shows the six lessons of this account (the five sample lessons "
						+ "and \"Manual test lesson\"), and the two locked ones are marked as "
						+ "locked.  No other Contributor's lessons appear.",
					"Only \"Sprint retro notes\" is listed.",
					"\"Search text is too long.\" is shown and the list still shows only "
						+ "\"Sprint retro notes\".",
					"All six lessons are listed again." });
	}

	/**********
	 * <p> Test M3: Open a Lesson From My List and See Its Details. </p>
	 *
	 * <p> <b>Requirement:</b> US-R3, select a lesson from the list to view its full details,
	 * including its experience entries. </p>
	 *
	 * <p> <b>How the code satisfies it:</b> "Open Lesson" is enabled only when a lesson is
	 * selected; it, or a double-click, opens ViewLessonDetails with the selected lesson's id,
	 * and ControllerLessonDetails shows the fields and the experience log loaded by
	 * ModelLessonDetails.viewLesson. </p>
	 *
	 * <p> <b>How to interpret the result:</b> record P only if both ways of opening show the
	 * lesson that was selected. </p>
	 */
	private static void testM3OpenLessonFromList() {
		manualTest("M3", "Open a Lesson From My List and See Its Details", "US-R3",
				new String[] {
					"On the Home Page, with nothing selected, look at Open Lesson.",
					"Select \"Sprint retro notes\" and press Open Lesson.",
					"Press Return, then double-click \"Deploy pipeline fix\"." },
				new String[] {
					"Open Lesson is disabled until a lesson is selected.",
					"The details of \"Sprint retro notes\" are shown: description, tags "
						+ "\"retro, teamwork\", category Teamwork, and an experience log with one "
						+ "2-hour entry listing Ana and Ben.",
					"The details of \"Deploy pipeline fix\" are shown, with \"No experience "
						+ "entries yet.\"" });
	}

	/**********
	 * <p> Test M4: Locked Fields Are Shown and Enforced. </p>
	 *
	 * <p> <b>Requirements:</b> US-R3 (see which fields are locked), US-U1-V (a locked field
	 * cannot be changed), and US-D1-V (a lesson with a locked field cannot be deleted, with a
	 * clear message and no confirmation prompt). </p>
	 *
	 * <p> <b>How the code satisfies them:</b> ControllerLessonDetails shows a banner naming the
	 * locked fields and asks ModelLessonDetails.deleteLesson for the lock check before showing
	 * any dialog (decision D30); ControllerUpdateLesson disables the inputs of locked fields; a
	 * whole-lesson lock disables Edit Lesson but leaves Add Experience available. </p>
	 *
	 * <p> <b>How to interpret the result:</b> record P only if no locked value can be changed
	 * and no deletion dialog appears for a locked lesson. </p>
	 */
	private static void testM4LockedLessonDisplay() {
		manualTest("M4", "Locked Fields Are Shown and Enforced", "US-R3, US-U1-V, US-D1-V",
				new String[] {
					"Return to the Home Page and open \"Title-locked lesson\".",
					"Press Edit Lesson, look at the inputs, then press Return.",
					"Press Delete Lesson.",
					"Return to the Home Page and open \"Fully locked lesson\"." },
				new String[] {
					"A banner says \"A curator has locked: title.\" and explains that locked "
						+ "fields cannot be edited and the lesson cannot be deleted.",
					"The Title input is disabled and labeled (locked); Description, Tags, and "
						+ "Category can be edited.",
					"\"This lesson is locked and cannot be deleted.\" is shown and no \"Are you "
						+ "sure?\" dialog appears.",
					"The banner says the whole lesson is locked; Edit Lesson is disabled and Add "
						+ "Experience is still enabled." });
	}

	/**********
	 * <p> Test M5: Edit Lesson Page - Save Only a Valid Change. </p>
	 *
	 * <p> <b>Requirements:</b> US-U1 (update an unlocked field and the change is saved) and
	 * US-U1-V (invalid new information is refused with a clear message). </p>
	 *
	 * <p> <b>How the code satisfies them:</b> ControllerUpdateLesson enables Save Changes only
	 * when something has changed and every editable field is valid, shows each field's message
	 * beneath it, and saves through ModelUpdateLesson.updateLesson. </p>
	 *
	 * <p> <b>How to interpret the result:</b> record P only if the invalid value could not be
	 * saved and the valid change is shown on the details page. </p>
	 */
	private static void testM5EditLessonPage() {
		manualTest("M5", "Edit Lesson Page - Save Only a Valid Change", "US-U1, US-U1-V",
				new String[] {
					"Open \"Deploy pipeline fix\" and press Edit Lesson.  Look at Save Changes.",
					"Delete all of the Description text, then click in Tags.",
					"Type the description \"Rolled back and fixed the pipeline config.\" and "
						+ "change the title to \"Deploy pipeline fix (edited)\".",
					"Press Save Changes." },
				new String[] {
					"Save Changes is disabled because nothing has changed yet.",
					"\"Description is required.\" appears beneath Description and Save Changes "
						+ "stays disabled.",
					"The message disappears and Save Changes becomes enabled.",
					"The details page shows the new title and description with the message "
						+ "\"Lesson updated\"." });
	}

	/**********
	 * <p> Test M6: Add Experience With Team Members. </p>
	 *
	 * <p> <b>Requirements:</b> US-U2 (add experience information), US-U2-V (invalid values
	 * get a specific message and nothing is saved), and E7 (members' hours may not add up to
	 * more than the total). </p>
	 *
	 * <p> <b>How the code satisfies them:</b> ControllerAddExperience checks the total, the
	 * description, and each member row as the tester types, names the row that is wrong,
	 * enforces the sum rule, and enables Add Experience only when everything is valid; it saves
	 * through ModelAddExperience.addExperience. </p>
	 *
	 * <p> <b>How to interpret the result:</b> record P only if the entry could not be saved
	 * while the sum rule was broken, and the saved entry lists both members. </p>
	 */
	private static void testM6AddExperienceWithTeamMembers() {
		manualTest("M6", "Add Experience With Team Members", "US-U2, US-U2-V, E7",
				new String[] {
					"On the details page of \"Deploy pipeline fix (edited)\", press Add Experience.",
					"Enter 3 in the total hours and \"Reused the fix on the release branch.\" "
						+ "in the description.",
					"Press Add Team Member twice.  Enter Ana with 2 hours and Ben with 2 hours.",
					"Change Ben's hours to 1, then press Add Experience." },
				new String[] {
					"The page names the lesson the entry is for.",
					"Add Experience becomes enabled once both required fields are valid.",
					"\"Team member hours cannot add up to more than the total time/effort.\" is "
						+ "shown and Add Experience is disabled.",
					"The details page shows \"Experience added\" and the experience log lists a "
						+ "3-hour entry with Ana (2 h) and Ben (1 h)." });
	}

	/**********
	 * <p> Test M7: Delete a Lesson Only After "Yes". </p>
	 *
	 * <p> <b>Requirements:</b> US-D1 (delete after confirming "Are you sure?" with an
	 * explicit "Yes") and US-D1-V (anything but "Yes" cancels and nothing changes). </p>
	 *
	 * <p> <b>How the code satisfies them:</b> ControllerLessonDetails.performDelete shows a
	 * confirmation dialog with only Yes and No, and calls ModelLessonDetails.deleteLesson with
	 * confirmed = true only for Yes; it then returns to the Home Page with "Lesson
	 * deleted". </p>
	 *
	 * <p> <b>How to interpret the result:</b> record P only if No left the lesson in place and
	 * Yes removed it. </p>
	 */
	private static void testM7DeleteWithConfirmation() {
		manualTest("M7", "Delete a Lesson Only After \"Yes\"", "US-D1, US-D1-V",
				new String[] {
					"Return to the Home Page, open \"Lesson to delete\", and press Delete Lesson.",
					"Press No.",
					"Press Delete Lesson again and press Yes." },
				new String[] {
					"A dialog titled \"Delete Lesson\" asks \"Are you sure?\" and offers only Yes "
						+ "and No.",
					"The dialog closes; the details of \"Lesson to delete\" are still shown and "
						+ "no error is shown.",
					"The Home Page opens with \"Lesson deleted\", and \"Lesson to delete\" is no "
						+ "longer in the list." });
	}


	/*-*******************************************************************************************

	Running a manual test

	 */

	/**********
	 * Prints one manual test's steps and expected results, then records the tester's verdict.
	 *
	 * @param id			the test id, for example "M1"
	 * @param name			the descriptive test name
	 * @param requirements	the requirements the test demonstrates
	 * @param steps			what the tester does, in order
	 * @param expected		what the tester must see
	 */
	private static void manualTest(String id, String name, String requirements, String[] steps,
			String[] expected) {
		System.out.println();
		System.out.println("=== " + id + ": " + name + "   (" + requirements + ")");
		System.out.println("  Steps:");
		for (int i = 0; i < steps.length; i++) System.out.println("    " + (i + 1) + ". " + steps[i]);
		System.out.println("  Expected:");
		for (String result : expected) System.out.println("    - " + result);
		System.out.print("  Result (P = pass, F = fail, S = skip): ");

		String answer = input.hasNextLine() ? input.nextLine().trim().toUpperCase() : "S";
		if (answer.startsWith("P")) {
			passed++;
			System.out.println("  Recorded: PASS");
		} else if (answer.startsWith("F")) {
			failed++;
			System.out.println("  Recorded: FAIL");
		} else {
			skipped++;
			System.out.println("  Recorded: skipped");
		}
	}


	/*-*******************************************************************************************

	Sample data

	 */

	/**********
	 * Adds the five sample lessons the manual tests use to one Contributor's account, locking
	 * two of them the way a Phase Three Curator will.  Run with FoundationsMain closed.
	 *
	 * @param user	the Contributor's username
	 */
	private static void setUpSampleLessons(String user) {
		if (!connect()) return;
		try {
			int retro = sample(user, "Sprint retro notes", "Teamwork", "retro, teamwork");
			ModelAddExperience.addExperience(user, retro, "2",
					"Used the retro format again in sprint 4.", Arrays.asList("Ana", "Ben"),
					Arrays.asList("1", "1"));
			sample(user, "Deploy pipeline fix", "Technical", "ci, release");
			int titleLocked = sample(user, "Title-locked lesson", "General", "");
			FoundationsMain.database.setLessonLock(titleLocked, LessonsLearned.FIELD_TITLE, true);
			int fullyLocked = sample(user, "Fully locked lesson", "General", "");
			FoundationsMain.database.setLessonLock(fullyLocked, LessonsLearned.LOCK_ENTIRE, true);
			sample(user, "Lesson to delete", "General", "");
			System.out.println("Added 5 sample lessons for \"" + user + "\" (one title-locked, one "
					+ "fully locked).");
		} finally {
			FoundationsMain.database.closeConnection();
		}
	}

	/**********
	 * Creates one sample lesson through the Create Lesson Model.
	 *
	 * @param user		the owner
	 * @param title		the title
	 * @param category	the category
	 * @param tags		the tags
	 * @return the new lesson's id
	 */
	private static int sample(String user, String title, String category, String tags) {
		LessonResult result = ModelCreateLesson.createLesson(user, title,
				"Sample lesson for the TP2 manual GUI tests.", tags, category);
		if (!result.isSuccess())
			throw new IllegalStateException("Could not add \"" + title + "\": " + result.getMessage());
		return result.getLessonId();
	}

	/**********
	 * Deletes every lesson owned by one account, including locked ones, by going straight to
	 * the Database.  Run with FoundationsMain closed.
	 *
	 * @param user	the Contributor's username
	 */
	private static void cleanUpSampleLessons(String user) {
		if (!connect()) return;
		try {
			LessonsLearnedList lessons = LessonsLearnedList.loadByOwner(user);
			for (LessonsLearned lesson : lessons.getLessons())
				FoundationsMain.database.deleteLesson(lesson.getId());
			System.out.println("Removed " + lessons.size() + " lesson(s) owned by \"" + user + "\".");
		} finally {
			FoundationsMain.database.closeConnection();
		}
	}

	/**********
	 * Opens the database connection for setup or clean-up.
	 *
	 * @return true if connected; false (after printing why) if not
	 */
	private static boolean connect() {
		try {
			FoundationsMain.database.connectToDatabase();
			return true;
		} catch (SQLException e) {
			System.out.println("*** ERROR *** Could not connect to the database.  Close "
					+ "FoundationsMain first.  " + e.getMessage());
			return false;
		}
	}
}
