package guiContributorHome;

import entityClasses.LessonSummary;

/**
 * Enforces read-boundary visibility rules and formats merged time and effort statistics
 * on the Contributor lesson view (Aspect 4 / Epic E5).
 *
 * @author Alex Meyers
 * @version 3.0
 */
public class ContributorLessonView {

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

        return String.format(
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