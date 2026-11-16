
package viewerTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import entityClasses.LessonsLearned;
import entityClasses.LessonsLearnedList;
import entityClasses.User;
import guiTools.LessonResult;
import guiViewerHome.ModelViewerHome;

/*******
 * <p> Title: ViewerHomeTests Class </p>
 *
 * <p> Description: Tests Viewer authorization and keyword
 * search behavior for TP3 Aspect 9. Uses in-memory lessons
 * to avoid modifying the application's H2 database. </p>
 *
 * @author David Nevarez
 *
 * @version 1.00 2026-11-16 Initial tests for TP3 Aspect 9
 */
public class ViewerHomeTests {

    /**********
     * <p> Method: createLesson(String title, String description,
     * String tags) </p>
     *
     * <p> Description: Creates a sample lesson for testing
     * without saving it to the database. </p>
     *
     * @param title the lesson title
     * @param description the lesson description
     * @param tags the lesson tags
     * @return a lesson for testing
     */
    private LessonsLearned createLesson(
            String title, String description, String tags) {

        return new LessonsLearned(
                "testContributor",
                title,
                description,
                tags,
                "Other");
    }

    /**********
     * <p> Method: testSearchMatchesTitle() </p>
     *
     * <p> Description: Verifies that keyword searches
     * can match lesson titles. </p>
     */
    @Test
    public void testSearchMatchesTitle() {

        LessonsLearned lesson = createLesson(
                "Debugging Java",
                "A lesson about software development.",
                "programming");

        LessonsLearnedList list =
                new LessonsLearnedList(List.of(lesson));

        LessonsLearnedList matches =
                list.filterByKeywords("debugging");

        assertEquals(1, matches.size());
        assertSame(lesson, matches.get(0));
    }

    /**********
     * <p> Method: testSearchMatchesDescription() </p>
     *
     * <p> Description: Verifies that searches also match
     * lesson descriptions. </p>
     */
    @Test
    public void testSearchMatchesDescription() {

        LessonsLearned lesson = createLesson(
                "Software Development",
                "Troubleshooting unexpected errors.",
                "programming");

        LessonsLearnedList list =
                new LessonsLearnedList(List.of(lesson));

        assertEquals(
                1,
                list.filterByKeywords("troubleshooting").size());
    }

    /**********
     * <p> Method: testSearchMatchesTags() </p>
     *
     * <p> Description: Verifies that lesson tags are
     * included in keyword searches. </p>
     */
    @Test
    public void testSearchMatchesTags() {

        LessonsLearned lesson = createLesson(
                "Java Project",
                "Working on a class assignment.",
                "testing, debugging");

        LessonsLearnedList list =
                new LessonsLearnedList(List.of(lesson));

        assertEquals(
                1,
                list.filterByKeywords("testing").size());
    }

    /**********
     * <p> Method: testSearchIsCaseInsensitive() </p>
     *
     * <p> Description: Verifies that capitalization
     * does not affect keyword matching. </p>
     */
    @Test
    public void testSearchIsCaseInsensitive() {

        LessonsLearned lesson = createLesson(
                "Java Programming",
                "Working with objects.",
                "software");

        LessonsLearnedList list =
                new LessonsLearnedList(List.of(lesson));

        assertEquals(
                1,
                list.filterByKeywords("JAVA").size());
    }

    /**********
     * <p> Method: testSearchRequiresAllKeywords() </p>
     *
     * <p> Description: Verifies that all entered keywords
     * must match the lesson's searchable fields. </p>
     */
    @Test
    public void testSearchRequiresAllKeywords() {

        LessonsLearned lesson = createLesson(
                "Java Programming",
                "Learning about objects.",
                "software");

        LessonsLearnedList list =
                new LessonsLearnedList(List.of(lesson));

        assertEquals(
                1,
                list.filterByKeywords("java objects").size());

        assertEquals(
                0,
                list.filterByKeywords("java python").size());
    }

    /**********
     * <p> Method: testNonViewerCannotListLessons() </p>
     *
     * <p> Description: Verifies that a user without
     * the Viewer role cannot access the Viewer lesson list. </p>
     */
    @Test
    public void testNonViewerCannotListLessons() {

        User contributor = new User(
                "contributor", "password", "", "", "", "", "",
                false, true, false, false);

        LessonResult result =
                ModelViewerHome.viewAllLessons(contributor);

        assertFalse(result.isSuccess());
    }

    /**********
     * <p> Method: testNonViewerCannotSearchLessons() </p>
     *
     * <p> Description: Verifies that a user without
     * Viewer access cannot use Viewer search operations. </p>
     */
    @Test
    public void testNonViewerCannotSearchLessons() {

        User curator = new User(
                "curator", "password", "", "", "", "", "",
                false, false, true, false);

        LessonResult result =
                ModelViewerHome.searchAllLessons(
                        curator, "debugging");

        assertFalse(result.isSuccess());
    }

    /**********
     * <p> Method: testNonViewerCannotReadLesson() </p>
     *
     * <p> Description: Verifies that unauthorized
     * users cannot retrieve lessons through the Viewer Model. </p>
     */
    @Test
    public void testNonViewerCannotReadLesson() {

        User contributor = new User(
                "contributor", "password", "", "", "", "", "",
                false, true, false, false);

        LessonResult result =
                ModelViewerHome.viewLesson(contributor, 1);

        assertFalse(result.isSuccess());
    }
}
