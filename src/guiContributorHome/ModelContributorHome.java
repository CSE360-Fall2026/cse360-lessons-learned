package guiContributorHome;

import entityClasses.LessonsLearnedList;
import guiTools.LessonResult;
import inputValidation.LessonRecognizer;

/*******
 * <p> Title: ModelContributorHome Class. </p>
 *
 * <p> Description: The Contributor Home Page Model.  It performs the data work behind the
 * Contributor's list of lessons: loading every lesson the logged-in Contributor has added
 * (US-R1) and searching their titles (US-R2, US-R2-V).  Following the MVC pattern of the
 * Foundations code, the View only draws widgets, the Controller only reacts to them, and this
 * Model does the work, here by calling the LessonsLearnedList entity class.</p>
 *
 * <p> Privacy (US-R4) is enforced by always loading through LessonsLearnedList.loadByOwner()
 * with the logged-in user's name.  A Contributor's list and search results can therefore never
 * contain another Contributor's lesson, whatever is typed into the search box.</p>
 *
 * <p> The methods are public, unlike the Controller's protected methods, so that the automated
 * tests can call them directly without starting JavaFX.</p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-10-14 Initial version for TP2 (list and search from HW2's
 * 							ModelRole1Home)
 */

public class ModelContributorHome {

	/*****
	 * <p> Method: ModelContributorHome() </p>
	 *
	 * <p> Description: This default constructor is not used in this system; every method is
	 * static. </p>
	 */
	public ModelContributorHome() {

	}


	/**********
	 * <p> Method: LessonsLearnedList viewMyLessons(String requestingUser) </p>
	 *
	 * <p> Description: READ (list).  Implements US-R1 and US-R4: every lesson the requesting
	 * Contributor has added, newest first.  It cannot fail in a way the user needs to hear about
	 * (a Contributor with no lessons simply gets an empty list), so it returns the list directly
	 * rather than a LessonResult.</p>
	 *
	 * @param requestingUser	the logged-in Contributor's username
	 * @return					that Contributor's lessons; empty, never null, if there are none
	 */
	public static LessonsLearnedList viewMyLessons(String requestingUser) {
		return LessonsLearnedList.loadByOwner(requestingUser);					// US-R1, US-R4
	}


	/**********
	 * <p> Method: LessonResult searchMyLessons(String requestingUser, String searchText) </p>
	 *
	 * <p> Description: READ (subset).  Implements US-R2 and US-R2-V.  The search text is
	 * validated first; text that is too long or contains a control character is refused with a
	 * specific message and no search is run.  Otherwise the Contributor's own lessons are
	 * filtered by title, ignoring case.  Empty text is valid but finds nothing (design decision
	 * D6, following the team's Users search rule); the Clear button brings back the full
	 * list.</p>
	 *
	 * @param requestingUser	the logged-in Contributor's username
	 * @param searchText		the text to look for in the titles
	 * @return					on success the matching list (possibly empty); otherwise the
	 * 							specific search-text error, with failedField "searchText"
	 */
	public static LessonResult searchMyLessons(String requestingUser, String searchText) {
		String error = LessonRecognizer.checkForValidSearchText(searchText);	// US-R2-V
		if (!error.isEmpty()) return LessonResult.failed(error, "searchText");

		LessonsLearnedList matches = LessonsLearnedList.loadByOwner(requestingUser)	// US-R4
				.filterByTitleContains(searchText);									// US-R2
		return LessonResult.passedList("Search complete", matches);
	}
}
