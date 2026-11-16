package guiContributorHome;

import java.util.Locale;
import database.Database;
import entityClasses.LessonSummary;

/**
 * Enforces read-boundary visibility rules and formats merged time and effort statistics
 * on the Contributor lesson view (Aspect 4 / Epic E5).
 *
 * <p> Contributor-facing pages read a summary only through readSummaryForContributor, so a
 * hidden summary is dropped at the read boundary rather than merely not drawn (US-C-E5-02).
 * Validated by: {@code curatorSummaryTests.CuratorSummaryTests}. </p>
 *
 * @author Alex Meyers
 * @author Emilia Davey
 * @version 3.0  2026-11-16 Alex Meyers: visibility filter and summary text for Aspect 4
 * @version 3.1  2026-11-16 Moved into src; added readSummaryForContributor, the database read
 *               that applies the visibility filter
 * @version 3.2  2026-11-16 EMPTY_STATE and renderSummaryPanelLines for the summary panel on the
 *               Lesson Details page and the Viewer's lesson details; numbers formatted with
 *               Locale.ROOT, so every computer shows "12.00", not "12,00"
 */
public class ContributorLessonView {

    /** What a lesson page shows when there is no summary to show.  The same words are used
     * whether the summary is hidden or does not exist, so a Contributor or Viewer cannot tell
     * that a hidden summary exists (US-C-E5-02: no statistics and no made-up zeros). */
    public static final String EMPTY_STATE = "No merged effort summary has been shared for "
            + "this lesson.";

    /**
     * Private constructor to prevent instantiation of this view helper class.
     */
    private ContributorLessonView() {
    }

    /**
     * Determines whether a merged summary should be displayed to a Contributor.
     *
     * @param summary the LessonSummary attached to the target lesson
     * @return true if the summary exists, has observations, and is marked visible; false otherwise
     */
    public static boolean shouldDisplaySummary(LessonSummary summary) {
        return summary != null && summary.getEntryCount() > 0 && summary.isVisibleToContributor();
    }

    /**
     * Filters a LessonSummary at the Contributor read boundary so hidden or empty summaries
     * are omitted completely (US-C-E5-02).
     *
     * @param summary the raw LessonSummary retrieved for the lesson
     * @return the LessonSummary if authorized for Contributor viewing, or null if hidden/missing
     */
    public static LessonSummary getSummaryForContributorRead(LessonSummary summary) {
        if (!shouldDisplaySummary(summary)) {
            return null;
        }
        return summary;
    }

    /**
     * Reads a lesson's merged summary for a Contributor-facing page.  The summary is returned
     * only if it exists, has observations, and the Curator made it visible; otherwise null, so
     * the page shows no statistics and no made-up zeros (US-C-E5-02).  Hiding a summary again
     * takes effect on the next read, because the flag is read from the database every time.
     *
     * @param database the connected application database
     * @param lessonId the lesson being shown
     * @return the visible summary, or null if it is hidden or missing
     */
    public static LessonSummary readSummaryForContributor(Database database, int lessonId) {
        if (database == null) {
            return null;
        }
        return getSummaryForContributorRead(database.getLessonSummary(lessonId));
    }

    /**
     * Renders a summary one labeled statistic per line, for the narrow summary panel on the
     * Contributor's Lesson Details page and for the Viewer's lesson details.  As in
     * renderSummaryPanelText, a hidden or missing summary gives no numbers: the result is then
     * EMPTY_STATE.  The count is labeled as experience records and every value is in hours, to
     * two decimal places (US-C-E4-03).
     *
     * @param summary the summary attached to the lesson, as read for a Contributor
     * @return the panel text, or EMPTY_STATE if the summary is hidden or missing
     */
    public static String renderSummaryPanelLines(LessonSummary summary) {
        LessonSummary authorizedSummary = getSummaryForContributorRead(summary);
        if (authorizedSummary == null) {
            return EMPTY_STATE;
        }
        return String.format(Locale.ROOT,
            "Experience records: %d\n" +
            "Total: %.2f hrs\n" +
            "Mean: %.2f hrs\n" +
            "Median: %.2f hrs\n" +
            "Minimum: %.2f hrs\n" +
            "Maximum: %.2f hrs\n" +
            "Std dev: %s",
            authorizedSummary.getEntryCount(),
            authorizedSummary.getTotalHours(),
            authorizedSummary.getMeanHours(),
            authorizedSummary.getMedianHours(),
            authorizedSummary.getMinHours(),
            authorizedSummary.getMaxHours(),
            authorizedSummary.getFormattedStandardDeviation()
        );
    }

    /**
     * Renders formatted statistics text for the Contributor UI without fabricated zeros
     * when a summary is hidden or absent (US-C-E5-02-V).
     *
     * @param summary the LessonSummary attached to the lesson
     * @return formatted summary text if visible, or an empty string if hidden/missing
     */
    public static String renderSummaryPanelText(LessonSummary summary) {
        LessonSummary authorizedSummary = getSummaryForContributorRead(summary);
        if (authorizedSummary == null) {
            return "";
        }

        return String.format(Locale.ROOT,
            "Merged Effort Summary (%d experience records)\n" +
            "Total Hours: %.2f hrs | Mean: %.2f hrs | Median: %.2f hrs\n" +
            "Min: %.2f hrs | Max: %.2f hrs | Std Dev: %s",
            authorizedSummary.getEntryCount(),
            authorizedSummary.getTotalHours(),
            authorizedSummary.getMeanHours(),
            authorizedSummary.getMedianHours(),
            authorizedSummary.getMinHours(),
            authorizedSummary.getMaxHours(),
            authorizedSummary.getFormattedStandardDeviation()
        );
    }
}