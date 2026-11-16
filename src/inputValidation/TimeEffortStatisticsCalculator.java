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
 * @author Emilia Davey
 * @version 3.0  2026-11-16 Alex Meyers: validation and statistics for Aspect 3
 * @version 3.1  2026-11-16 Moved into src; error messages as constants for the page and tests;
 *               a merge over MAX_ENTRIES_LIMIT entries is rejected instead of computed on only
 *               the first entries
 */
public class TimeEffortStatisticsCalculator {

    /** Maximum number of experience entries processed per merge to prevent resource exhaustion (CWE-770 / R10). */
    public static final int MAX_ENTRIES_LIMIT = 5000;

    /** Maximum number of distinct source lessons permitted in a single merge operation (CWE-770 / R10). */
    public static final int MAX_LESSONS_LIMIT = 500;

    /** Message when fewer than two distinct lessons are selected (US-C-E4-01). */
    public static final String TOO_FEW_LESSONS = "Select at least two lessons";

    /** Message when more than MAX_LESSONS_LIMIT distinct lessons are selected. */
    public static final String TOO_MANY_LESSONS = "Selected lessons exceed maximum allowed limit.";

    /** Message when the selected lessons have no valid experience hours (US-C-E4-02). */
    public static final String NO_DATA = "No experience data to merge.";

    /** Message when the selected lessons have more than MAX_ENTRIES_LIMIT experience records. */
    public static final String TOO_MANY_ENTRIES = "The selected lessons have more than "
            + MAX_ENTRIES_LIMIT + " experience records. Select fewer lessons.";

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
            throw new IllegalArgumentException(TOO_FEW_LESSONS);
        }

        Set<Integer> distinctIds = new LinkedHashSet<>();
        for (Integer id : rawSourceIds) {
            if (id != null && id > 0) {
                distinctIds.add(id);
            }
            if (distinctIds.size() > MAX_LESSONS_LIMIT) {
                throw new IllegalArgumentException(TOO_MANY_LESSONS);
            }
        }

        if (distinctIds.size() < 2) {
            throw new IllegalArgumentException(TOO_FEW_LESSONS);
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
            throw new IllegalArgumentException(NO_DATA);
        }

        // Reject an over-limit merge instead of computing on only the first entries: statistics
        // of an arbitrary part of the data would be shown as if they described all of it
        if (effortHours.size() > MAX_ENTRIES_LIMIT) {
            throw new IllegalArgumentException(TOO_MANY_ENTRIES);
        }

        List<Double> validHours = new ArrayList<>();
        for (Double val : effortHours) {
            // Invalid, negative, and non-finite hours are left out rather than counted as zero
            // (US-C-E4-02); the count in the summary is the number of valid observations
            if (val != null && Double.isFinite(val) && val >= 0.0) {
                validHours.add(val);
            }
        }

        if (validHours.isEmpty()) {
            throw new IllegalArgumentException(NO_DATA);
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