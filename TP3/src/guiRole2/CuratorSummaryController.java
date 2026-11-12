package guiRole2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import entityClasses.LessonSummary;
import inputValidation.TimeEffortStatisticsCalculator;

/**
 * Controller managing Curator workflows for merging statistics (E4) 
 * and controlling visibility settings (E5).
 * <p>
 * Validated by: {@code CuratorSummaryControllerTest}.
 * 
 * @author Alex Meyers
 * @version 1.0
 */
public class CuratorSummaryController {

    private Connection dbConnection;

    public CuratorSummaryController(Connection dbConnection) {
        this.dbConnection = dbConnection;
    }

    /**
     * Previews and persists a merged statistical summary for a target lesson (Epic E4).
     * 
     * @param targetLessonId ID of the lesson receiving the summary
     * @param rawEffortHours collected effort hours from selected source lessons
     * @return the persisted LessonSummary object
     */
    public LessonSummary createAndSaveSummary(int targetLessonId, List<Double> rawEffortHours) throws SQLException {
        LessonSummary summary = TimeEffortStatisticsCalculator.computeStatistics(targetLessonId, rawEffortHours);
        
        // Persist summary and set default hidden visibility in H2 database
        String query = "UPDATE lessonsLearnedDB SET summaryId = ?, statsVisibleToContributor = ? WHERE id = ?";
        
        try (PreparedStatement pstmt = dbConnection.prepareStatement(query)) {
            // Parameterized query safeguards against SQL injection (R2 / CWE-89)
            pstmt.setInt(1, summary.getSummaryId());
            pstmt.setBoolean(2, summary.isStatsVisibleToContributor());
            pstmt.setInt(3, summary.getTargetLessonId());
            pstmt.executeUpdate();
        }
        
        return summary;
    }

    /**
     * Sets a merged summary's visibility to visible or hidden (Aspect 4 / Epic E5).
     * 
     * @param targetLessonId ID of the target lesson
     * @param isVisible true to show on contributor view, false to hide
     */
    public void updateStatisticsVisibility(int targetLessonId, boolean isVisible) throws SQLException {
        String query = "UPDATE lessonsLearnedDB SET statsVisibleToContributor = ? WHERE id = ?";
        
        try (PreparedStatement pstmt = dbConnection.prepareStatement(query)) {
            pstmt.setBoolean(1, isVisible);
            pstmt.setInt(2, targetLessonId);
            pstmt.executeUpdate();
        }
    }
}