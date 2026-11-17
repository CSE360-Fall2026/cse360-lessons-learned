package guiCuratorSummary;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import database.Database;
import entityClasses.ExperienceEntry;
import entityClasses.LessonSummary;
import entityClasses.User;
import guiTools.CuratorAccess;
import inputValidation.TimeEffortStatisticsCalculator;

/**
 * Controller managing Curator workflows for merging statistics (E4)
 * and controlling visibility settings (E5).
 *
 * <p> <b>Merging (Aspect 3, Epic E4).</b> A Curator chooses a target lesson and at least two
 * source lessons, previews the summary (previewMergedSummary), and then explicitly confirms it
 * (confirmAndSaveSummary) or cancels (clearPreview).  Nothing is saved until the confirmation
 * (US-C-E4-01).  The hours are read from the database: each distinct source lesson, and each of
 * its experience records, contributes exactly once, so selecting a lesson twice cannot
 * double-count it (US-C-E4-02).  Confirming recomputes the summary; if the selection changed the
 * preview is discarded, and if the experience data changed the preview is refreshed and a new
 * confirmation is required (US-C-E4-02).  Saving replaces any earlier summary of the target and
 * leaves it hidden from Contributors.</p>
 *
 * <p> <b>Visibility (Aspect 4, Epic E5).</b> updateStatisticsVisibility shows or hides a saved
 * summary for Contributors.  It changes only the visibility flag, never the summary's numbers
 * or the raw experience records (US-C-E5-01, US-C-E5-02).</p>
 *
 * <p> Every method first checks that the acting user is currently a Curator
 * (guiTools.CuratorAccess), so the check cannot be skipped by a caller.  Problems are reported
 * by exceptions whose messages are the exact texts in the public constants below and in
 * inputValidation.TimeEffortStatisticsCalculator, so a page can show them directly.</p>
 *
 * <p> Validated by: {@code curatorSummaryTests.CuratorSummaryTests}.</p>
 *
 * @author Alex Meyers
 * @author Emilia Davey
 * @version 2.0  2026-11-16 Alex Meyers: preview, confirm, and visibility workflow (in guiRole2)
 * @version 3.0  2026-11-16 Moved into src/guiCuratorSummary.  Uses the application's Database
 *               (saveLessonSummary, getLessonSummary, setStatsVisibleToContributor) instead of
 *               its own SQL, so the statistics are saved and the summaryId is set; checks the
 *               acting User with CuratorAccess instead of a boolean from the caller; reads each
 *               source lesson's hours itself; recomputes on confirmation; removed
 *               createAndSaveSummary, which saved without a preview
 */
public class CuratorSummaryController {

    /** Message when the acting user is not a Curator (US-C-E5-02: only a Curator). */
    public static final String NOT_CURATOR =
            "Only a Curator can merge lesson statistics or change their visibility.";

    /** Message when the target lesson does not exist. */
    public static final String INVALID_TARGET = "Select a valid target lesson.";

    /** Message when a selected source lesson does not exist (US-C-E4-02: existing lessons). */
    public static final String MISSING_SOURCE = "A selected lesson no longer exists.";

    /** Message when Confirm is used without a preview (US-C-E4-01). */
    public static final String NO_PREVIEW = "Preview summary statistics before confirming.";

    /** Message when the selection changed since the preview (US-C-E4-02). */
    public static final String SELECTION_CHANGED =
            "Selected lessons changed since preview. Please preview again before confirming.";

    /** Message when the experience data changed since the preview (US-C-E4-02). */
    public static final String DATA_CHANGED = "The experience data changed since the preview. "
            + "Review the refreshed preview and confirm again.";

    /** Message when the summary could not be saved. */
    public static final String NOT_SAVED = "The summary could not be saved. Nothing was changed.";

    /** Message when a lesson has no saved summary to show or hide. */
    public static final String NO_SUMMARY = "This lesson has no merged summary to show or hide.";

    /** Message when the visibility flag could not be saved. */
    public static final String VISIBILITY_NOT_SAVED =
            "The visibility could not be saved. Please try again.";

    /** The application database every read and write goes through. */
    private final Database database;
    /** The preview waiting for confirmation, or null if there is none. */
    private LessonSummary pendingPreview;
    /** The distinct source lessons of the pending preview. */
    private Set<Integer> previewedSourceIds;
    /** The target lesson of the pending preview; -1 if there is none. */
    private int previewedTargetLessonId;

    /**
     * Constructs a CuratorSummaryController that works through the application's database.
     *
     * @param database the connected application database
     */
    public CuratorSummaryController(Database database) {
        this.database = database;
        this.previewedTargetLessonId = -1;
    }

    /**
     * Generates a preview of merged statistical metrics without persisting to the database
     * (US-C-E4-01).  Any earlier preview is discarded first, so a failed preview can never be
     * confirmed.
     *
     * @param curator              the acting user, who must be a Curator
     * @param targetLessonId       ID of the lesson receiving the summary (it may also be a source)
     * @param selectedLessonIds    list of selected source lesson IDs; repeats count once
     * @return the computed LessonSummary preview object (hidden by default)
     * @throws SecurityException        if the user is not a Curator
     * @throws IllegalArgumentException with the message to show, if the input is not valid
     */
    public LessonSummary previewMergedSummary(User curator, int targetLessonId,
            List<Integer> selectedLessonIds) {
        requireCurator(curator);
        clearPreview();
        Set<Integer> distinctSources = validateLessons(targetLessonId, selectedLessonIds);
        LessonSummary summary = TimeEffortStatisticsCalculator.computeStatistics(targetLessonId,
                distinctSources, collectHours(distinctSources));

        this.pendingPreview = summary;
        this.previewedSourceIds = new LinkedHashSet<>(distinctSources);
        this.previewedTargetLessonId = targetLessonId;
        return summary;
    }

    /**
     * Confirms a previously previewed statistical summary and persists it to the target lesson
     * (Epic E4).  The summary is recomputed from the database first.  If the selection changed
     * since the preview, the preview is discarded; if the experience data changed, the preview
     * is refreshed (see getPendingPreview) and a new confirmation is required (US-C-E4-02).
     *
     * @param curator                  the acting user, who must be a Curator
     * @param targetLessonId           ID of the lesson receiving the summary
     * @param currentSelectedLessonIds current list of selected source lesson IDs
     * @return the persisted LessonSummary object, with its summary ID set
     * @throws SecurityException        if the user is not a Curator
     * @throws IllegalArgumentException with the message to show, if the input is not valid
     * @throws IllegalStateException    with the message to show, if there is no current preview,
     *                                  the preview is out of date, or the save failed
     */
    public LessonSummary confirmAndSaveSummary(User curator, int targetLessonId,
            List<Integer> currentSelectedLessonIds) {
        requireCurator(curator);
        if (pendingPreview == null) {
            throw new IllegalStateException(NO_PREVIEW);
        }

        Set<Integer> currentDistinct =
                TimeEffortStatisticsCalculator.validateAndDeduplicateSources(currentSelectedLessonIds);
        if (targetLessonId != previewedTargetLessonId || !currentDistinct.equals(previewedSourceIds)) {
            clearPreview();
            throw new IllegalStateException(SELECTION_CHANGED);
        }

        // Recompute from the database, in case experience records changed after the preview
        validateLessons(targetLessonId, currentSelectedLessonIds);
        LessonSummary current = TimeEffortStatisticsCalculator.computeStatistics(targetLessonId,
                currentDistinct, collectHours(currentDistinct));
        if (!sameStatistics(current, pendingPreview)) {
            pendingPreview = current;
            throw new IllegalStateException(DATA_CHANGED);
        }

        LessonSummary confirmedSummary = pendingPreview;
        clearPreview();
        if (database.saveLessonSummary(confirmedSummary) < 0) {
            throw new IllegalStateException(NOT_SAVED);
        }
        return confirmedSummary;
    }

    /**
     * Returns the current preview, or null if there is none.  After confirmAndSaveSummary
     * reports DATA_CHANGED, this is the refreshed preview to show before the next confirmation.
     *
     * @return the pending preview, or null
     */
    public LessonSummary getPendingPreview() {
        return pendingPreview;
    }

    /**
     * Cancels any active merge preview without modifying existing database records (US-C-E4-01-V).
     */
    public void clearPreview() {
        this.pendingPreview = null;
        this.previewedSourceIds = null;
        this.previewedTargetLessonId = -1;
    }

    /**
     * Reads a lesson's saved summary for the Curator, whether it is visible or hidden.
     *
     * @param curator        the acting user, who must be a Curator
     * @param targetLessonId ID of the target lesson
     * @return the saved summary, or null if the lesson has none
     * @throws SecurityException if the user is not a Curator
     */
    public LessonSummary getSavedSummary(User curator, int targetLessonId) {
        requireCurator(curator);
        return database.getLessonSummary(targetLessonId);
    }

    /**
     * Sets a merged summary's visibility to visible or hidden (Aspect 4 / Epic E5).  Only the
     * visibility flag changes; the summary's numbers and the raw experience records do not.
     *
     * @param curator        the acting user, who must be a Curator
     * @param targetLessonId ID of the target lesson
     * @param isVisible      true to show on contributor view, false to hide
     * @throws SecurityException        if the user is not a Curator
     * @throws IllegalArgumentException if the lesson does not exist or has no saved summary
     * @throws IllegalStateException    if the flag could not be saved
     */
    public void updateStatisticsVisibility(User curator, int targetLessonId, boolean isVisible) {
        requireCurator(curator);
        if (targetLessonId <= 0 || !database.doesLessonExist(targetLessonId)) {
            throw new IllegalArgumentException(INVALID_TARGET);
        }
        if (database.getLessonSummary(targetLessonId) == null) {
            throw new IllegalArgumentException(NO_SUMMARY);
        }
        if (!database.setStatsVisibleToContributor(targetLessonId, isVisible)) {
            throw new IllegalStateException(VISIBILITY_NOT_SAVED);
        }
    }

    /**
     * Refuses the action unless the acting user is currently a Curator.
     *
     * @param user the acting user
     * @throws SecurityException if the user is not a Curator
     */
    private static void requireCurator(User user) {
        if (!CuratorAccess.isCurator(user)) {
            throw new SecurityException(NOT_CURATOR);
        }
    }

    /**
     * Checks the target and the selected source lessons: the target must exist, at least two
     * distinct lessons must be selected, and every selected lesson must exist (US-C-E4-02).
     *
     * @param targetLessonId    the target lesson
     * @param selectedLessonIds the selected source lessons
     * @return the distinct selected lesson IDs, in the order first selected
     */
    private Set<Integer> validateLessons(int targetLessonId, List<Integer> selectedLessonIds) {
        if (targetLessonId <= 0 || !database.doesLessonExist(targetLessonId)) {
            throw new IllegalArgumentException(INVALID_TARGET);
        }
        Set<Integer> distinctSources =
                TimeEffortStatisticsCalculator.validateAndDeduplicateSources(selectedLessonIds);
        for (int sourceId : distinctSources) {
            if (!database.doesLessonExist(sourceId)) {
                throw new IllegalArgumentException(MISSING_SOURCE);
            }
        }
        return distinctSources;
    }

    /**
     * Reads the recorded hours of every experience record of each distinct source lesson.  Each
     * lesson is read once and each record is listed once, so nothing is double-counted.
     *
     * @param distinctSources the distinct source lesson IDs
     * @return the hours, one value per experience record
     */
    private List<Double> collectHours(Set<Integer> distinctSources) {
        List<Double> hours = new ArrayList<>();
        for (int sourceId : distinctSources) {
            for (ExperienceEntry entry : database.getExperienceForLesson(sourceId)) {
                hours.add(entry.getTimeEffortHours());
            }
        }
        return hours;
    }

    /**
     * Do two summaries have the same statistics?  Double.compare treats two NaN standard
     * deviations (fewer than two observations) as equal.
     *
     * @param a one summary
     * @param b the other summary
     * @return true if every statistic is the same
     */
    private static boolean sameStatistics(LessonSummary a, LessonSummary b) {
        return a.getEntryCount() == b.getEntryCount()
                && Double.compare(a.getTotalHours(), b.getTotalHours()) == 0
                && Double.compare(a.getMeanHours(), b.getMeanHours()) == 0
                && Double.compare(a.getMedianHours(), b.getMedianHours()) == 0
                && Double.compare(a.getMinHours(), b.getMinHours()) == 0
                && Double.compare(a.getMaxHours(), b.getMaxHours()) == 0
                && Double.compare(a.getStandardDeviationHours(), b.getStandardDeviationHours()) == 0;
    }
}
