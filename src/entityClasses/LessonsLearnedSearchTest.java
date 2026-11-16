
package entityClasses;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/*******
 * <p>
 * Title: LessonsLearnedSearchTest Class
 * </p>
 *
 * <p>
 * Description: Tests the keyword search used by Viewers to find lessons. Checks
 * that saved Curator-approved search terms work, existing title searches still
 * work, and searches with no matches return an empty list.
 * </p>
 *
 * <p>
 * These tests also check that the changes made for TP3 don't break the search
 * functionality already implemented in TP2.
 * </p>
 *
 * @author David Nevarez
 *
 * @version 1.00 2026-11-09 Initial version for HW3
 */
class LessonsLearnedSearchTest {

	/*****
	 * <p>
	 * Method: searchMatchesCuratorApprovedTerms()
	 * </p>
	 *
	 * <p>
	 * Description: Checks that a lesson can be found using a saved Curator-approved
	 * search term, even when that term doesn't appear in the lesson's title,
	 * description, or tags.
	 * </p>
	 */
    @Test
    void searchMatchesCuratorApprovedTerms() {

		// Creates a lesson to use for this test.
        LessonsLearned lesson = new LessonsLearned(
                1, "testUser",
                "Database Connection Failure",
                "Application could not connect to H2",
                "database", "Technical",
                false, false, false, false, false,
                0L, 0L
        );

		// Adds search terms that aren't already in the lesson.
        lesson.setSearchTerms("troubleshooting, connectivity");

		// Adds the lesson to a list that can be searched.
        LessonsLearnedList lessons = new LessonsLearnedList();
        lessons.add(lesson);

		// Searches for one of the saved terms.
        LessonsLearnedList results =
                lessons.filterByKeywords("connectivity");

		// Checks that the search found the correct lesson.
        assertEquals(1, results.size());
        assertEquals(lesson.getId(), results.get(0).getId());
    }

	/*****
	 * <p>
	 * Method: searchMatchesLessonTitle()
	 * </p>
	 *
	 * <p>
	 * Description: Checks that searching for a word in a lesson's title still
	 * returns the correct lesson after the TP3 changes.
	 * </p>
	 */
    @Test
    void searchMatchesLessonTitle() {

		// Creates a lesson with a title to search for.
		LessonsLearned lesson = new LessonsLearned(2, "testUser", "Database Connection Failure",
				"Application could not connect to H2", "database", "Technical", false, false, false, false, false, 0L,
				0L);

		// Adds the lesson to the list.
        LessonsLearnedList lessons = new LessonsLearnedList();
        lessons.add(lesson);

		// Searches for a word from the lesson's title.
		LessonsLearnedList results = lessons.filterByKeywords("Database");

		// Checks that the lesson appears in the results.
        assertEquals(1, results.size());
    }

	/*****
	 * <p>
	 * Method: searchWithNoMatchesReturnsEmptyList()
	 * </p>
	 *
	 * <p>
	 * Description: Checks that searching for a word that doesn't match any lesson
	 * returns an empty list instead of unrelated results.
	 * </p>
	 */
    @Test
    void searchWithNoMatchesReturnsEmptyList() {

		// Creates a lesson to use for the search.
		LessonsLearned lesson = new LessonsLearned(3, "testUser", "Database Connection Failure",
				"Application could not connect to H2", "database", "Technical", false, false, false, false, false, 0L,
				0L);

		// Adds the lesson to the list.
        LessonsLearnedList lessons = new LessonsLearnedList();
        lessons.add(lesson);

		// Searches for a word that isn't in the lesson.
		LessonsLearnedList results = lessons.filterByKeywords("networking");

		// Checks that no lessons were returned.
        assertTrue(results.isEmpty());
    }
}
