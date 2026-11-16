package guiCuratorHome;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import entityClasses.ExperienceEntry;
import entityClasses.LessonsLearned;
import entityClasses.LessonsLearnedList;
import entityClasses.MemberEffort;
import entityClasses.User;
import guiTools.CuratorAccess;
import guiTools.LessonAccess;
import guiTools.LessonResult;
import inputValidation.LessonRecognizer;

/*******
 * <p> Title: ModelCuratorHome Class. </p>
 *
 * <p> Description: The Curator Home Page Model (TP3 Aspect 1; Epics.pdf dependency D1).  It
 * performs the data work behind the Curator's list of lessons: loading every lesson from every
 * Contributor (US-CH1), searching them by keyword (US-CH2, US-CH2-V), and reading one lesson in
 * full (US-CH3).  Following the MVC pattern of the Foundations code, the View only draws
 * widgets, the Controller only reacts to them, and this Model does the work by calling the
 * LessonsLearnedList and LessonsLearned entity classes.</p>
 *
 * <p> <b>The role check comes first.</b>  A Contributor may see only their own lessons
 * (US-R4), and this Model reads everyone's.  Every method that returns lesson data therefore
 * takes the acting User and calls CuratorAccess.isCurator() before doing anything else
 * (US-CH-V; Initial ConOps objective OB-4).  The check is here, in the Model, and not only in
 * the page's navigation, so the data stays protected even if some later page calls these
 * methods for a user who should not have reached them.</p>
 *
 * <p> <b>Results.</b>  Like the TP2 Contributor Models, every outcome is a
 * guiTools.LessonResult, so the page can show getMessage() directly.  A refusal carries no
 * list and no lesson.</p>
 *
 * <p> The methods are public, unlike the Controller's protected methods, so that the automated
 * tests can call them directly without starting JavaFX.</p>
 *
 * <p> Tested by curatorHomeTests.CuratorHomeTests (JUnit): viewAllLessons() by CH-16 to CH-20
 * and CH-34; searchAllLessons() by CH-21 to CH-28; viewLesson() by CH-29 to CH-31; lockSummary()
 * by CH-32; detailsText() by CH-33; rowText() by CH-36. </p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-11-10 Initial version for TP3 Aspect 1, written to pass
 * 							CuratorHomeTests
 */

public class ModelCuratorHome {

	/** Shown when the acting user is not a Curator (US-CH-V).  It names the action without
	 * saying anything about which lessons exist. */
	public static final String NOT_CURATOR = "Only a curator can view all lessons.";

	/*****
	 * <p> Method: ModelCuratorHome() </p>
	 *
	 * <p> Description: This default constructor is not used in this system; every method is
	 * static. </p>
	 */
	public ModelCuratorHome() {

	}


	/**********
	 * <p> Method: LessonResult viewAllLessons(User actingUser) </p>
	 *
	 * <p> Description: READ (list).  Implements US-CH1 and US-CH-V: every lesson in the system,
	 * from every Contributor, newest first. </p>
	 *
	 * <p> Tested by CuratorHomeTests CH-16, CH-17, CH-20 (positive), CH-18, CH-19 (negative),
	 * and CH-34 (regression of US-R4). </p>
	 *
	 * @param actingUser	the logged-in user; may be null
	 * @return				for a Curator, a successful result whose list holds every lesson
	 * 						(empty, never null, if there are none); otherwise a failed result
	 * 						with NOT_CURATOR and no list
	 */
	public static LessonResult viewAllLessons(User actingUser) {
		// Unlike ModelContributorHome.viewMyLessons(), this returns a LessonResult rather than
		// the bare list, because here there IS a failure the user must be told about: the
		// refusal of someone who is not a Curator
		if (!CuratorAccess.isCurator(actingUser))
			return LessonResult.failed(NOT_CURATOR, "");						// US-CH-V

		return LessonResult.passedList("All lessons", LessonsLearnedList.loadAll());	// US-CH1
	}


	/**********
	 * <p> Method: LessonResult searchAllLessons(User actingUser, String searchText) </p>
	 *
	 * <p> Description: READ (subset).  Implements US-CH2, US-CH2-V, and US-CH-V.  Order of
	 * checks:
	 * <br> 1. The acting user is a Curator.
	 * <br> 2. The search text is valid: no more than 254 characters and no control character.
	 * <br> 3. Every lesson is filtered by keyword, ignoring case.  A lesson matches only if
	 * every keyword appears somewhere in its title, description, or tags.</p>
	 *
	 * <p> Empty text is valid but finds nothing (design decision D6, the same rule as the
	 * Contributor search); the Clear button brings back the full list.</p>
	 *
	 * <p> Tested by CuratorHomeTests CH-21 to CH-23 (positive) and CH-24 to CH-28 (negative and
	 * boundary). </p>
	 *
	 * @param actingUser	the logged-in user; may be null
	 * @param searchText	one or more keywords separated by spaces or commas
	 * @return				on success the matching list (possibly empty); otherwise NOT_CURATOR,
	 * 						or the specific search-text error with failedField "searchText"
	 */
	public static LessonResult searchAllLessons(User actingUser, String searchText) {
		// The role is checked BEFORE the text, so someone who is not a Curator always gets the
		// same answer and cannot use different error messages to probe the page (CH-28)
		if (!CuratorAccess.isCurator(actingUser))
			return LessonResult.failed(NOT_CURATOR, "");						// US-CH-V

		String error = LessonRecognizer.checkForValidSearchText(searchText);	// US-CH2-V
		if (!error.isEmpty()) return LessonResult.failed(error, "searchText");

		// A keyword search, not the Contributor's title search: a Curator looks through other
		// people's lessons and cannot know their titles, so the description and tags are
		// searched too.  Epic E2 will add the saved search terms to the same filter.
		LessonsLearnedList matches = LessonsLearnedList.loadAll()
				.filterByKeywords(searchText);									// US-CH2
		return LessonResult.passedList("Search complete", matches);
	}


	/**********
	 * <p> Method: LessonResult viewLesson(User actingUser, int lessonId) </p>
	 *
	 * <p> Description: READ (one).  Implements US-CH3 and US-CH-V: one lesson with its
	 * experience entries loaded, whoever owns it.  Order of checks:
	 * <br> 1. The acting user is a Curator.
	 * <br> 2. The lesson exists.  There is no ownership check: ownership is a Contributor rule,
	 * and a Curator reviews every Contributor's lessons.</p>
	 *
	 * <p> Tested by CuratorHomeTests CH-29 (positive), CH-30, and CH-31 (negative). </p>
	 *
	 * @param actingUser	the logged-in user; may be null
	 * @param lessonId		the lesson to read
	 * @return				on success the lesson with its experience loaded; otherwise
	 * 						NOT_CURATOR or "Lesson not found."
	 */
	public static LessonResult viewLesson(User actingUser, int lessonId) {
		// Role first, so someone who is not a Curator cannot learn which lesson ids exist by
		// comparing "not a curator" with "not found" (CWE-200; CH-31)
		if (!CuratorAccess.isCurator(actingUser))
			return LessonResult.failed(NOT_CURATOR, "");						// US-CH-V

		// The lesson can be missing even though it was just listed: its Contributor may have
		// deleted it while the Curator's list was on the screen
		LessonsLearned lesson = LessonsLearned.read(lessonId);
		if (lesson == null) return LessonResult.failed(LessonAccess.NOT_FOUND, "");

		lesson.loadExperienceEntries();											// US-CH3
		return LessonResult.passed("", lessonId, lesson);
	}


	/**********
	 * <p> Method: String rowText(LessonsLearned lesson) </p>
	 *
	 * <p> Description: The text of one row of the Curator's list: the title, the category, and
	 * the owner, followed by "(locked)" when the lesson or any of its fields is locked
	 * (US-CH1). </p>
	 *
	 * <p> Tested by CuratorHomeTests CH-36. </p>
	 *
	 * @param lesson	the lesson to describe
	 * @return			for example "Set up CI   [Technical]   by jsmith   (locked)"
	 */
	public static String rowText(LessonsLearned lesson) {
		// The Contributor's rows show no owner because every row is the Contributor's own.
		// Here the owner is the one fact that tells two similar lessons apart.  The text is
		// built in the Model, not the Controller, so a JUnit test can check it without JavaFX.
		return lesson.getTitle() + "   [" + lesson.getCategory() + "]   by "
				+ lesson.getOwnerUserName() + (lesson.isLocked() ? "   (locked)" : "");
	}


	/**********
	 * <p> Method: String lockSummary(LessonsLearned lesson) </p>
	 *
	 * <p> Description: Names what is locked on a lesson, in the fixed order entire lesson,
	 * title, description, tags, category, which is the order the lock action uses in its own
	 * messages (ModelCuratorLock). </p>
	 *
	 * <p> Tested by CuratorHomeTests CH-32. </p>
	 *
	 * @param lesson	the lesson to describe
	 * @return			"nothing" if no lock is set, otherwise for example "title, tags" or
	 * 					"entire lesson, title"
	 */
	public static String lockSummary(LessonsLearned lesson) {
		// The five stored flags are read one by one.  isFieldLocked() is not used, because it
		// answers true for every field once the whole lesson is locked, and a Curator who is
		// about to unlock needs to see which flags are really set.
		List<String> locked = new ArrayList<String>();
		if (lesson.isLockedEntire()) locked.add("entire lesson");
		if (lesson.isLockedTitle()) locked.add(LessonsLearned.FIELD_TITLE);
		if (lesson.isLockedDescription()) locked.add(LessonsLearned.FIELD_DESCRIPTION);
		if (lesson.isLockedTags()) locked.add(LessonsLearned.FIELD_TAGS);
		if (lesson.isLockedCategory()) locked.add(LessonsLearned.FIELD_CATEGORY);
		return locked.isEmpty() ? "nothing" : String.join(", ", locked);
	}


	/**********
	 * <p> Method: String detailsText(LessonsLearned lesson) </p>
	 *
	 * <p> Description: The full text the page shows for the selected lesson (US-CH3): title,
	 * owner, category, tags, what is locked, the description, and the experience entries,
	 * oldest first, each with its hours and, when there are any, its team members' shares. </p>
	 *
	 * <p> Tested by CuratorHomeTests CH-33. </p>
	 *
	 * @param lesson	the lesson to show, with its experience entries already loaded (as
	 * 					returned by viewLesson()); may be null
	 * @return			the details, one fact per line; an empty String for null
	 */
	public static String detailsText(LessonsLearned lesson) {
		if (lesson == null) return "";

		StringBuilder text = new StringBuilder();
		text.append("Title: ").append(lesson.getTitle()).append("\n");
		text.append("Owner: ").append(lesson.getOwnerUserName()).append("\n");
		text.append("Category: ").append(lesson.getCategory()).append("\n");
		// Tags are optional, so an empty line is replaced by words that say so
		String tags = (lesson.getTags() == null) ? "" : lesson.getTags().trim();
		text.append("Tags: ").append(tags.isEmpty() ? "(none)" : tags).append("\n");
		text.append("Locked: ").append(lockSummary(lesson)).append("\n\n");
		text.append("Description:\n").append(lesson.getDescription()).append("\n\n");

		List<ExperienceEntry> entries = lesson.getExperienceEntries();
		text.append("Experience entries: ").append(entries.size());
		for (ExperienceEntry entry : entries) {
			text.append("\n").append(formatHours(entry.getTimeEffortHours())).append(" h   ")
				.append(entry.getDescription());
			List<String> shares = new ArrayList<String>();
			for (MemberEffort share : entry.getMemberEfforts())
				shares.add(share.getMemberName() + " " + formatHours(share.getHours()) + " h");
			if (!shares.isEmpty()) text.append("\n   Team: ").append(String.join(", ", shares));
		}
		return text.toString();
	}


	/**********
	 * <p> Method: String formatHours(double hours) </p>
	 *
	 * <p> Description: Hours without a needless ".0", so 4.0 shows as "4" and 3.5 as "3.5",
	 * matching how the Contributor's Lesson Details page shows hours. </p>
	 *
	 * @param hours	the number of hours
	 * @return		the hours as text
	 */
	private static String formatHours(double hours) {
		// BigDecimal.valueOf, not new BigDecimal(double), so 3.5 stays "3.5" instead of
		// becoming a long binary fraction
		return BigDecimal.valueOf(hours).stripTrailingZeros().toPlainString();
	}
}
