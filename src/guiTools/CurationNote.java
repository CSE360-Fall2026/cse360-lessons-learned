package guiTools;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import entityClasses.LessonsLearned;

/*******
 * <p> Title: CurationNote Class </p>
 *
 * <p> Description: The "Edited by curator" note that tells a reader a Curator refined a lesson's
 * text (TP3 Aspect 6, US-C-E3-07: "the authorized lesson view displays an 'edited by curator'
 * note with the saved attribution").  The Curator Home Page, the Refine with AI page, and the
 * Contributor's Lesson Details page all show it with these same words.</p>
 *
 * <p> The note only formats what a page that has already checked access is showing; it never
 * reads a lesson itself, so it cannot give anyone access to one.  The date is written in
 * English whatever language the computer uses, so the note reads the same on every team
 * member's computer.  ModelCuratorRefine.curationNote() keeps Daniel's ISO-time version, which
 * his tests RF-11 and RF-12 check.</p>
 *
 * <p> Tested by curatorRefinementTests.RefinePageTests RP-08 and RP-09. </p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-11-18 Initial version for TP3 Aspect 6
 */
public class CurationNote {

	/** The date and time format, the same as the Viewer page's note. */
	private static final DateTimeFormatter FORMAT =
			DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a", Locale.ENGLISH);

	/*****
	 * <p> Method: CurationNote() </p>
	 *
	 * <p> Description: This default constructor is not used in this system; every member of
	 * this class is static. </p>
	 */
	public CurationNote() {

	}


	/*****
	 * <p> Method: String text(LessonsLearned lesson) </p>
	 *
	 * <p> Description: The note for a lesson, in this computer's time zone. </p>
	 *
	 * @param lesson	a lesson the page is already allowed to show; may be null
	 * @return			"Edited by curator: NAME on MMM d, yyyy at h:mm AM", or an empty String
	 * 					if no Curator has refined the lesson
	 */
	public static String text(LessonsLearned lesson) {
		return text(lesson, ZoneId.systemDefault());
	}


	/*****
	 * <p> Method: String text(LessonsLearned lesson, ZoneId zone) </p>
	 *
	 * <p> Description: The note for a lesson in a given time zone.  The tests use a fixed zone
	 * so the expected text does not depend on where they run. </p>
	 *
	 * @param lesson	a lesson the page is already allowed to show; may be null
	 * @param zone		the time zone for the date and time
	 * @return			the note, or an empty String if no Curator has refined the lesson
	 */
	public static String text(LessonsLearned lesson, ZoneId zone) {
		// Both parts are needed: a name without a time, or the reverse, is not a real refinement
		if (lesson == null || lesson.getCuratedAt() <= 0 || lesson.getCuratedBy() == null
				|| lesson.getCuratedBy().isBlank()) return "";
		return "Edited by curator: " + lesson.getCuratedBy() + " on "
				+ FORMAT.format(Instant.ofEpochMilli(lesson.getCuratedAt()).atZone(zone));
	}
}
