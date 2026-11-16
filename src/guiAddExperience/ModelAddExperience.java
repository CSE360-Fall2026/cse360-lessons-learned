package guiAddExperience;

import java.util.ArrayList;
import java.util.List;

import entityClasses.ExperienceEntry;
import entityClasses.LessonsLearned;
import entityClasses.MemberEffort;
import guiTools.LessonAccess;
import guiTools.LessonResult;
import inputValidation.LessonRecognizer;

/*******
 * <p> Title: ModelAddExperience Class. </p>
 *
 * <p> Description: The Add Experience Page Model.  It performs the data work behind recording
 * experience information on a lesson after a new attempt to leverage it (US-U2): what was done
 * and how, the time/effort in hours, and, as the Lessons Learned System V1 Contributor story
 * adds, "the effort used by the various team members."  Invalid values are refused with a
 * specific message and nothing is saved (US-U2-V).</p>
 *
 * <p> Locks are deliberately NOT checked here.  Locks protect a lesson's core fields from being
 * changed; they were never meant to stop a Contributor from recording that the lesson was
 * reused.  The ownership rule (US-R3-V) does apply, because US-R3-V names "experience entry" as
 * one of the actions it protects.</p>
 *
 * <p> The methods are public so the automated tests can call them without starting JavaFX.</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-10-14 Initial version for TP2 (from HW2's ModelRole1Home, with the
 * 							per-member effort added in TP2 Phase 1)
 */

public class ModelAddExperience {

	/*****
	 * <p> Method: ModelAddExperience() </p>
	 *
	 * <p> Description: This default constructor is not used in this system; every method is
	 * static. </p>
	 */
	public ModelAddExperience() {

	}


	/**********
	 * <p> Method: LessonResult loadLesson(String requestingUser, int lessonId) </p>
	 *
	 * <p> Description: READ.  Loads the lesson the experience will be added to, so the page can
	 * show which lesson it is, applying the ownership rule (US-R3-V, US-R4). </p>
	 *
	 * @param requestingUser	the logged-in Contributor's username
	 * @param lessonId			the lesson
	 * @return					on success the lesson; otherwise "Lesson not found."
	 */
	public static LessonResult loadLesson(String requestingUser, int lessonId) {
		LessonsLearned lesson = LessonAccess.findOwnedLesson(requestingUser, lessonId);
		if (lesson == null) return LessonResult.failed(LessonAccess.NOT_FOUND, "");	// US-R3-V
		return LessonResult.passed("", lessonId, lesson);
	}


	/**********
	 * <p> Method: LessonResult addExperience(String requestingUser, int lessonId,
	 * 		String timeEffortText, String description) </p>
	 *
	 * <p> Description: UPDATE (experience).  US-U2 and US-U2-V for an entry without a
	 * per-member breakdown; it calls the full version with no team members. </p>
	 *
	 * @param requestingUser	the logged-in Contributor's username
	 * @param lessonId			the lesson the entry belongs to
	 * @param timeEffortText	the total hours, exactly as typed
	 * @param description		what was done and how
	 * @return					on success "Experience added"; otherwise the specific message
	 */
	public static LessonResult addExperience(String requestingUser, int lessonId,
			String timeEffortText, String description) {
		return addExperience(requestingUser, lessonId, timeEffortText, description, null, null);
	}


	/**********
	 * <p> Method: LessonResult addExperience(String requestingUser, int lessonId,
	 * 		String timeEffortText, String description, List&lt;String&gt; memberNames,
	 * 		List&lt;String&gt; memberHoursTexts) </p>
	 *
	 * <p> Description: UPDATE (experience).  Implements US-U2 and US-U2-V.  Order of checks:
	 * <br> 1. The lesson exists and belongs to requestingUser (US-R3-V).
	 * <br> 2. The total hours and the description, as typed.
	 * <br> 3. Each team member's name and hours, as typed, row by row.
	 * <br> 4. The entry as a whole: no more than ExperienceEntry.MAX_MEMBERS members, and the
	 * members' hours do not add up to more than the total.
	 * <br> 5. One transaction saves the entry and every member row, or nothing is saved.</p>
	 *
	 * @param requestingUser	the logged-in Contributor's username
	 * @param lessonId			the lesson the entry belongs to
	 * @param timeEffortText	the total hours, exactly as typed
	 * @param description		what was done and how
	 * @param memberNames		the team members' names, one per row; null means none
	 * @param memberHoursTexts	each member's hours exactly as typed, in the same order as the
	 * 							names; null means none
	 * @return					on success "Experience added"; otherwise the specific message and
	 * 							the field that caused it
	 */
	public static LessonResult addExperience(String requestingUser, int lessonId,
			String timeEffortText, String description, List<String> memberNames,
			List<String> memberHoursTexts) {

		LessonsLearned lesson = LessonAccess.findOwnedLesson(requestingUser, lessonId);
		if (lesson == null) return LessonResult.failed(LessonAccess.NOT_FOUND, "");	// US-R3-V

		String error = LessonRecognizer.checkForValidExperienceValue(timeEffortText);
		if (!error.isEmpty()) return LessonResult.failed(error, "experienceValue");	// US-U2-V

		error = LessonRecognizer.checkForValidExperienceDescription(description);
		if (!error.isEmpty()) return LessonResult.failed(error, "experienceDescription");

		// Each member row must have both a name and hours.  The page always sends the two lists
		// with the same length, so a mismatch is reported rather than guessed at.
		List<String> names = (memberNames == null) ? new ArrayList<String>() : memberNames;
		List<String> hoursTexts = (memberHoursTexts == null)
				? new ArrayList<String>() : memberHoursTexts;
		if (names.size() != hoursTexts.size())
			return LessonResult.failed("Each team member needs both a name and hours.",
					"memberEfforts");

		List<MemberEffort> shares = new ArrayList<MemberEffort>();
		for (int ndx = 0; ndx < names.size(); ndx++) {
			error = LessonRecognizer.checkForValidMemberName(names.get(ndx));
			if (!error.isEmpty()) return LessonResult.failed(error, "memberName");
			error = LessonRecognizer.checkForValidExperienceValue(hoursTexts.get(ndx));
			if (!error.isEmpty()) return LessonResult.failed(error, "memberHours");
			// The recognizer proved the hours are a plain number, so parsing is safe
			shares.add(new MemberEffort(names.get(ndx).trim(),
					Double.parseDouble(hoursTexts.get(ndx).trim())));
		}

		// The recognizer proved the total is a plain number, so parsing is safe.  The rules about
		// the entry as a whole (and the description after trimming) are checked by the entity.
		ExperienceEntry entry = new ExperienceEntry(lessonId,
				Double.parseDouble(timeEffortText.trim()), description.trim(), shares);
		String field = entry.findInvalidField();
		if (!field.isEmpty()) return LessonResult.failed(entry.validate(), field);

		if (entry.create() < 0)													// US-U2
			return LessonResult.failed("Could not save the experience due to a database error.",
					"");
		return LessonResult.passed("Experience added", lessonId, lesson);
	}
}
