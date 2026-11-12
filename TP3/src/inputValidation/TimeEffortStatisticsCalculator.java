package inputValidation;

import java.util.Collections;
import java.util.List;
import entityClasses.LessonSummary;

/**
 * Utility engine for computing statistical aggregates across multiple 
 * lesson experience entries and member effort hours (Aspect 3 / Epic E4).
 * <p>
 * Validated by: {@code TimeEffortStatisticsCalculatorTest}.
 * 
 * @author Alex Meyers
 * @version 1.0
 */
public class TimeEffortStatisticsCalculator {

    // Maximum threshold limit to prevent resource exhaustion on oversized collections (CWE-770 mitigation)
    private static final int MAX_ENTRIES_LIMIT = 5000;

    /**
     * Computes comprehensive statistical metrics from a list of effort hour values.
     * 
     * @param targetLessonId the lesson ID receiving the summary
     * @param effortHours list of numerical effort hours extracted from selected source lessons
     * @return a LessonSummary object containing all computed metrics
     */
    public static LessonSummary computeStatistics(int targetLessonId, List<Double> effortHours) {
        // Defensive check: Handle null or empty inputs gracefully without throwing NullPointerExceptions (CWE-476 / R9)
        if (effortHours == null || effortHours.isEmpty()) {
            return new LessonSummary(0, targetLessonId, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, false);
        }

        // Resource constraint check (CWE-770 / R10): Throttling or capping collection size if unbounded
        int count = effortHours.size();
        if (count > MAX_ENTRIES_LIMIT) {
            effortHours = effortHours.subList(0, MAX_ENTRIES_LIMIT);
            count = MAX_ENTRIES_LIMIT;
        }

        double total = 0.0;
        for (double val : effortHours) {
            total += val;
        }

        double mean = total / count;

        // Sort collection to accurately determine median, min, and max values.
        Collections.sort(effortHours);
        double min = effortHours.get(0);
        double max = effortHours.get(count - 1);
        
        double median;
        if (count % 2 == 0) {
            median = (effortHours.get(count / 2 - 1) + effortHours.get(count / 2)) / 2.0;
        } else {
            median = effortHours.get(count / 2);
        }

        // Calculate sample standard deviation (n-1 degrees of freedom)
        double varianceSum = 0.0;
        for (double val : effortHours) {
            varianceSum += Math.pow(val - mean, 2);
        }
        
        // Internal comment: If count is 1, variance denominator (count - 1) would be 0; 
        // handle gracefully to prevent NaN or arithmetic exceptions.
        double stdDev = (count > 1) ? Math.sqrt(varianceSum / (count - 1)) : 0.0;

        // Default visibility to false (hidden by default per E5 requirements)
        return new LessonSummary(0, targetLessonId, count, total, mean, median, min, max, stdDev, false);
    }
}