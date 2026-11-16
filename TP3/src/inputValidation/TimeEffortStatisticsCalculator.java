package inputValidation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import entityClasses.LessonSummary;

/**
 * Utility class responsible for validating source lessons and computing aggregated
 * time and effort statistics across merged experience records (Epic E4 / Aspect 3).
 *
 * @author Alex Meyers
 * @version 3.0
 */
public class TimeEffortStatisticsCalculator {

    /** Maximum number of experience entries processed per merge to prevent resource exhaustion (CWE-770 / R10). */
    public static final int MAX_ENTRIES_LIMIT = 5000;

    /** Maximum number of distinct source lessons permitted in a single merge operation (CWE-770 / R10). */
    public static final int MAX_LESSONS_LIMIT = 500;

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private TimeEffortStatisticsCalculator() {
    }

    /**
     * Validates and deduplicates selected source lesson IDs so each source contributes at most once.
     *
     * @param rawSourceIds list of selected source lesson IDs
     * @return a deduplicated set of valid source lesson IDs
     * @throws IllegalArgumentException if fewer than two distinct valid lessons are provided or limits are exceeded
     */
    public static Set<Integer> validateAndDeduplicateSources(List<Integer> rawSourceIds) {
        if (rawSourceIds == null) {
            throw new IllegalArgumentException("Select at least two lessons");
        }

        Set<Integer> distinctIds = new LinkedHashSet<>();
        for (Integer id : rawSourceIds) {
            if (id != null && id > 0) {
                distinctIds.add(id);
            }
            if (distinctIds.size() > MAX_LESSONS_LIMIT) {
                throw new IllegalArgumentException("Selected lessons exceed maximum allowed limit.");
            }
        }

        if (distinctIds.size() < 2) {
            throw new IllegalArgumentException("Select at least two lessons");
        }

        return distinctIds;
    }

    /**
     * Computes comprehensive statistical metrics from a list of effort hour values.
     *
     * @param targetLessonId  the lesson ID receiving the summary
     * @param sourceLessonIds the deduplicated set of contributing source lesson IDs
     * @param effortHours     list of numerical effort hours extracted from selected source lessons
     * @return a LessonSummary object containing all computed metrics (hidden by default per E5)
     * @throws IllegalArgumentException if no valid, finite, non-negative experience hours exist
     */
    public static LessonSummary computeStatistics(int targetLessonId, Set<Integer> sourceLessonIds, List<Double> effortHours) {
        if (effortHours == null || effortHours.isEmpty()) {
            throw new IllegalArgumentException("No experience data to merge.");
        }

        List<Double> validHours = new ArrayList<>();
        int inspected = 0;
        for (Double val : effortHours) {
            if (inspected >= MAX_ENTRIES_LIMIT) {
                break;
            }
            inspected++;
            if (val != null && Double.isFinite(val) && val >= 0.0) {
                validHours.add(val);
            }
        }

        if (validHours.isEmpty()) {
            throw new IllegalArgumentException("No experience data to merge.");
        }

        int count = validHours.size();
        double total = 0.0;
        for (double val : validHours) {
            total += val;
        }

        double mean = total / count;

        Collections.sort(validHours);
        double min = validHours.get(0);
        double max = validHours.get(count - 1);

        double median;
        if (count % 2 == 0) {
            median = (validHours.get(count / 2 - 1) + validHours.get(count / 2)) / 2.0;
        } else {
            median = validHours.get(count / 2);
        }

        double stdDev = Double.NaN;
        if (count > 1) {
            double varianceSum = 0.0;
            for (double val : validHours) {
                double diff = val - mean;
                varianceSum += diff * diff;
            }
            stdDev = Math.sqrt(varianceSum / (count - 1));
        }

        return new LessonSummary(targetLessonId, sourceLessonIds, count, total, mean, median, min, max, stdDev);
    }
}