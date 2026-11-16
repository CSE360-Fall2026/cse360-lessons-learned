package guiRole2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import entityClasses.LessonSummary;
import inputValidation.TimeEffortStatisticsCalculator;

/**
 * Controller managing Curator workflows for merging statistics (E4)
 * and controlling visibility settings (E5).
 * <p>
 * Validated by: {@code CuratorSummaryControllerTest}.
 *
 * @author Alex Meyers
 * @version 2.0
 */
public class CuratorSummaryController {

    private Connection dbConnection;
    private LessonSummary pendingPreview;
    private Set<Integer> previewedSourceIds;
    private int previewedTargetLessonId;

    /**
     * Constructs a CuratorSummaryController bound to the active database connection.
     *
     * @param dbConnection active JDBC connection to the H2 database
     */
    public CuratorSummaryController(Connection dbConnection) {
        this.dbConnection = dbConnection;
        this.previewedTargetLessonId = -1;
    }

    /**
     * Generates a preview of merged statistical metrics without persisting to the database (US-C-E4-01).
     *
     * @param targetLessonId       ID of the lesson receiving the summary
     * @param selectedLessonIds    list of selected source lesson IDs
     * @param combinedEffortHours  collected effort hours from selected source lessons
     * @return the computed LessonSummary preview object
     */
    public LessonSummary previewMergedSummary(int targetLessonId, List<Integer> selectedLessonIds, List<Double> combinedEffortHours) {
        if (targetLessonId <= 0) {
            throw new IllegalArgumentException("Select a valid target lesson.");
        }

        Set<Integer> distinctSources = TimeEffortStatisticsCalculator.validateAndDeduplicateSources(selectedLessonIds);
        LessonSummary summary = TimeEffortStatisticsCalculator.computeStatistics(targetLessonId, distinctSources, combinedEffortHours);

        this.pendingPreview = summary;
        this.previewedSourceIds = new LinkedHashSet<>(distinctSources);
        this.previewedTargetLessonId = targetLessonId;
        return summary;
    }

    /**
     * Confirms a previously previewed statistical summary and persists it to the target lesson (Epic E4).
     *
     * @param targetLessonId           ID of the lesson receiving the summary
     * @param currentSelectedLessonIds current list of selected source lesson IDs
     * @return the persisted LessonSummary object
     * @throws SQLException if a database update error occurs
     */
    public LessonSummary confirmAndSaveSummary(int targetLessonId, List<Integer> currentSelectedLessonIds) throws SQLException {
        if (pendingPreview == null) {
            throw new IllegalStateException("Preview summary statistics before confirming.");
        }

        Set<Integer> currentDistinct = TimeEffortStatisticsCalculator.validateAndDeduplicateSources(currentSelectedLessonIds);
        if (targetLessonId != previewedTargetLessonId || !currentDistinct.equals(previewedSourceIds)) {
            clearPreview();
            throw new IllegalStateException("Selected lessons changed since preview. Please preview again before confirming.");
        }

        LessonSummary confirmedSummary = pendingPreview;
        clearPreview();

        persistSummaryToDatabase(confirmedSummary);
        return confirmedSummary;
    }

    /**
     * Direct convenience method to compute and persist a merged statistical summary in one step.
     *
     * @param targetLessonId      ID of the lesson receiving the summary
     * @param selectedLessonIds   list of selected source lesson IDs
     * @param rawEffortHours      collected effort hours from selected source lessons
     * @return the persisted LessonSummary object
     * @throws SQLException if a database update error occurs
     */
    public LessonSummary createAndSaveSummary(int targetLessonId, List<Integer> selectedLessonIds, List<Double> rawEffortHours) throws SQLException {
        if (targetLessonId <= 0) {
            throw new IllegalArgumentException("Select a valid target lesson.");
        }

        Set<Integer> distinctSources = TimeEffortStatisticsCalculator.validateAndDeduplicateSources(selectedLessonIds);
        LessonSummary summary = TimeEffortStatisticsCalculator.computeStatistics(targetLessonId, distinctSources, rawEffortHours);

        persistSummaryToDatabase(summary);
        return summary;
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
     * Persists summary linkage and default hidden visibility in the H2 database.
     */
    private void persistSummaryToDatabase(LessonSummary summary) throws SQLException {
        if (dbConnection == null) {
            return;
        }

        String query = "UPDATE lessonsLearnedDB SET summaryId = ?, statsVisibleToContributor = ? WHERE id = ?";

        try (PreparedStatement pstmt = dbConnection.prepareStatement(query)) {
            // Parameterized query safeguards against SQL injection (R2 / CWE-89)
            pstmt.setInt(1, summary.getSummaryId());
            pstmt.setBoolean(2, summary.isVisibleToContributor());
            pstmt.setInt(3, summary.getTargetLessonId());
            pstmt.executeUpdate();
        }
    }

    /**
     * Sets a merged summary's visibility to visible or hidden (Aspect 4 / Epic E5).
     *
     * @param targetLessonId      ID of the target lesson
     * @param isVisible           true to show on contributor view, false to hide
     * @param isAuthorizedCurator true if the active user holds Curator authorization
     * @throws SQLException if a database update error occurs
     */
    public void updateStatisticsVisibility(int targetLessonId, boolean isVisible, boolean isAuthorizedCurator) throws SQLException {
        if (!isAuthorizedCurator) {
            throw new SecurityException("Only an authorized Curator can change summary visibility.");
        }
        if (targetLessonId <= 0) {
            throw new IllegalArgumentException("Select a valid target lesson.");
        }

        if (dbConnection != null) {
            String query = "UPDATE lessonsLearnedDB SET statsVisibleToContributor = ? WHERE id = ?";

            try (PreparedStatement pstmt = dbConnection.prepareStatement(query)) {
                pstmt.setBoolean(1, isVisible);
                pstmt.setInt(2, targetLessonId);
                pstmt.executeUpdate();
            }
        }
    }
}