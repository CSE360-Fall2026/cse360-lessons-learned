
package viewerTests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import entityClasses.LessonsLearned;
import guiViewerHome.ControllerViewerHome;

/*******
 * <p> Title: ViewerAttributionTests Class </p>
 *
 * <p> Description: Tests the display of Curator-edit
 * attribution in the read-only Viewer lesson details
 * for TP3 US-V-04. </p>
 *
 * @author David Nevarez
 *
 * @version 1.00 2026-11-16 Initial attribution tests
 */
public class ViewerAttributionTests {

    /**********
     * <p> Method: createLesson() </p>
     *
     * <p> Description: Creates an unsaved lesson for
     * testing the Viewer details display. </p>
     *
     * @return a sample lesson
     */
    private LessonsLearned createLesson() {
        return new LessonsLearned(
                "contributor",
                "Java Exception Handling",
                "Learning how to handle errors in Java.",
                "java, exceptions",
                "Other");
    }

    /**********
     * <p> Method: formatDetails(LessonsLearned lesson) </p>
     *
     * <p> Description: Invokes the private Viewer
     * formatting method for testing purposes. </p>
     *
     * @param lesson the lesson to format
     * @return formatted lesson details
     * @throws Exception if the method cannot be invoked
     */
    private String formatDetails(LessonsLearned lesson)
            throws Exception {

        Method method = ControllerViewerHome.class
                .getDeclaredMethod(
                        "formatLessonDetails",
                        LessonsLearned.class);

        method.setAccessible(true);

        return (String) method.invoke(null, lesson);
    }

    /**********
     * <p> Method: testUnrefinedLessonHasNoAttribution() </p>
     *
     * <p> Description: Verifies that an unrefined lesson
     * does not display Curator-edit attribution. </p>
     */
    @Test
    public void testUnrefinedLessonHasNoAttribution()
            throws Exception {

        LessonsLearned lesson = createLesson();

        String details = formatDetails(lesson);

        assertTrue(details.contains("Java Exception Handling"));
        assertTrue(details.contains(
                "Learning how to handle errors in Java."));

        assertFalse(details.contains("Edited by curator:"));
        assertFalse(details.contains("Last curated:"));
    }

    /**********
     * <p> Method: testRefinedLessonShowsAttribution() </p>
     *
     * <p> Description: Verifies that a refined lesson
     * displays the Curator identity and edit date. </p>
     */
    @Test
    public void testRefinedLessonShowsAttribution()
            throws Exception {

        LessonsLearned lesson = createLesson();

        lesson.setCuratedBy("testCurator");
        lesson.setCuratedAt(1731772800000L);

        String details = formatDetails(lesson);

        assertTrue(details.contains(
                "Edited by curator: testCurator"));
        assertTrue(details.contains("Last curated:"));
    }

    /**********
     * <p> Method: testIncompleteAttributionIsHidden() </p>
     *
     * <p> Description: Verifies that an incomplete
     * attribution record is not shown as a completed edit. </p>
     */
    @Test
    public void testIncompleteAttributionIsHidden()
            throws Exception {

        LessonsLearned lesson = createLesson();

        lesson.setCuratedBy("testCurator");
        lesson.setCuratedAt(0);

        String details = formatDetails(lesson);

        assertFalse(details.contains("Edited by curator:"));
        assertFalse(details.contains("Last curated:"));
    }
}
