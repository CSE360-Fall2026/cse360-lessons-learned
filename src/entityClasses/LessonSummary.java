package entityClasses;

import java.util.Locale;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Entity model representing aggregated time and effort statistics merged
 * across multiple lessons-learned instances (Aspects 3 and 4 / Epics E4 and E5).
 *
 * <p> Tested by curatorSummaryTests.CuratorSummaryTests SU-01 to SU-05 (the statistics and
 * their formatting) and CuratorSummaryPageTests SP-05, SP-12, and SP-15 (the table and panel
 * lines, with a decimal point on every computer) (JUnit), and by M-CS3 and M-CS8 (Manual
 * Tests.pdf). </p>
 *
 * @author Alex Meyers
 * @author Emilia Davey
 * @version 3.0  2026-11-16 Alex Meyers: summary entity for Aspects 3 and 4
 * @version 3.1  2026-11-16 Numbers are formatted with Locale.ROOT, so they show "2.00" on every
 *               computer (a Polish or German locale would otherwise show "2,00")
 */
public class LessonSummary {
    /** The id of this summary's row in lessonSummaryDB, set when it is saved. */
    private int summaryId;
    /** The id of the lesson this summary is saved on (the Curator's target lesson). */
    private int targetLessonId;
    /** The distinct ids of the lessons whose experience records were merged. */
    private Set<Integer> sourceLessonIds;
    /** The number of experience records (observations) that were merged. */
    private int entryCount;
    /** The sum of the merged effort hours. */
    private double totalHours;
    /** The mean of the merged effort hours. */
    private double meanHours;
    /** The median of the merged effort hours. */
    private double medianHours;
    /** The smallest merged effort value, in hours. */
    private double minHours;
    /** The largest merged effort value, in hours. */
    private double maxHours;
    /** The sample standard deviation of the merged hours; NaN when there is only one observation. */
    private double standardDeviationHours;
    /** Whether the Curator has made this summary visible to Contributors (and Viewers). */
    private boolean visibleToContributor;

    /**
     * Constructs an empty LessonSummary with visibility hidden by default.
     */
    public LessonSummary() {
        this.sourceLessonIds = new LinkedHashSet<>();
        this.visibleToContributor = false;
    }

    /**
     * Constructs a populated LessonSummary with computed statistical metrics and hidden default visibility.
     *
     * @param targetLessonId           the lesson ID receiving the merged summary
     * @param sourceLessonIds          set of contributing source lesson IDs
     * @param entryCount               total number of valid contributing experience observations
     * @param totalHours               sum of all valid effort hours
     * @param meanHours                arithmetic mean of valid effort hours
     * @param medianHours              median of valid effort hours
     * @param minHours                 minimum recorded effort hours
     * @param maxHours                 maximum recorded effort hours
     * @param standardDeviationHours   sample standard deviation of effort hours (NaN if fewer than 2 observations)
     */
    public LessonSummary(int targetLessonId, Set<Integer> sourceLessonIds, int entryCount,
                         double totalHours, double meanHours, double medianHours,
                         double minHours, double maxHours, double standardDeviationHours) {
        this.targetLessonId = targetLessonId;
        this.sourceLessonIds = (sourceLessonIds != null)
                ? new LinkedHashSet<>(sourceLessonIds)
                : new LinkedHashSet<>();
        this.entryCount = entryCount;
        this.totalHours = totalHours;
        this.meanHours = meanHours;
        this.medianHours = medianHours;
        this.minHours = minHours;
        this.maxHours = maxHours;
        this.standardDeviationHours = standardDeviationHours;
        this.visibleToContributor = false;
    }

    /**
     * Formats the sample standard deviation for display, returning an explanatory label
     * when fewer than two observations exist (US-C-E4-03-V).
     *
     * @return formatted standard deviation string or "N/A: fewer than two observations"
     */
    public String getFormattedStandardDeviation() {
        if (entryCount < 2 || Double.isNaN(standardDeviationHours)) {
            return "N/A: fewer than two observations";
        }
        return String.format(Locale.ROOT, "%.2f hrs", standardDeviationHours);
    }

    /**
     * Gets the unique summary identifier.
     *
     * @return the summary ID
     */
    public int getSummaryId() { return summaryId; }

    /**
     * Sets the unique summary identifier.
     *
     * @param summaryId the summary ID to assign
     */
    public void setSummaryId(int summaryId) { this.summaryId = summaryId; }

    /**
     * Gets the target lesson ID associated with this summary.
     *
     * @return the target lesson ID
     */
    public int getTargetLessonId() { return targetLessonId; }

    /**
     * Sets the target lesson ID associated with this summary.
     *
     * @param targetLessonId the target lesson ID to assign
     */
    public void setTargetLessonId(int targetLessonId) { this.targetLessonId = targetLessonId; }

    /**
     * Gets an unmodifiable set of contributing source lesson IDs.
     *
     * @return unmodifiable set of source lesson IDs
     */
    public Set<Integer> getSourceLessonIds() { return Collections.unmodifiableSet(sourceLessonIds); }

    /**
     * Sets the contributing source lesson IDs.
     *
     * @param sourceLessonIds set of source lesson IDs to assign
     */
    public void setSourceLessonIds(Set<Integer> sourceLessonIds) {
        this.sourceLessonIds = (sourceLessonIds != null)
                ? new LinkedHashSet<>(sourceLessonIds)
                : new LinkedHashSet<>();
    }

    /**
     * Gets the count of valid experience observations included in the merge.
     *
     * @return number of experience observations
     */
    public int getEntryCount() { return entryCount; }

    /**
     * Sets the count of valid experience observations included in the merge.
     *
     * @param entryCount number of experience observations
     */
    public void setEntryCount(int entryCount) { this.entryCount = entryCount; }

    /**
     * Gets the sum of all valid effort hours.
     *
     * @return total effort hours
     */
    public double getTotalHours() { return totalHours; }

    /**
     * Sets the sum of all valid effort hours.
     *
     * @param totalHours total effort hours
     */
    public void setTotalHours(double totalHours) { this.totalHours = totalHours; }

    /**
     * Gets the arithmetic mean of valid effort hours.
     *
     * @return mean effort hours
     */
    public double getMeanHours() { return meanHours; }

    /**
     * Sets the arithmetic mean of valid effort hours.
     *
     * @param meanHours mean effort hours
     */
    public void setMeanHours(double meanHours) { this.meanHours = meanHours; }

    /**
     * Gets the median of valid effort hours.
     *
     * @return median effort hours
     */
    public double getMedianHours() { return medianHours; }

    /**
     * Sets the median of valid effort hours.
     *
     * @param medianHours median effort hours
     */
    public void setMedianHours(double medianHours) { this.medianHours = medianHours; }

    /**
     * Gets the minimum recorded effort hours.
     *
     * @return minimum effort hours
     */
    public double getMinHours() { return minHours; }

    /**
     * Sets the minimum recorded effort hours.
     *
     * @param minHours minimum effort hours
     */
    public void setMinHours(double minHours) { this.minHours = minHours; }

    /**
     * Gets the maximum recorded effort hours.
     *
     * @return maximum effort hours
     */
    public double getMaxHours() { return maxHours; }

    /**
     * Sets the maximum recorded effort hours.
     *
     * @param maxHours maximum effort hours
     */
    public void setMaxHours(double maxHours) { this.maxHours = maxHours; }

    /**
     * Gets the sample standard deviation of valid effort hours.
     *
     * @return sample standard deviation in hours
     */
    public double getStandardDeviationHours() { return standardDeviationHours; }

    /**
     * Sets the sample standard deviation of valid effort hours.
     *
     * @param standardDeviationHours sample standard deviation in hours
     */
    public void setStandardDeviationHours(double standardDeviationHours) {
        this.standardDeviationHours = standardDeviationHours;
    }

    /**
     * Checks whether the summary is currently visible to Contributors.
     *
     * @return true if visible to Contributors; false if hidden
     */
    public boolean isVisibleToContributor() { return visibleToContributor; }

    /**
     * Sets whether the summary is visible to Contributors.
     *
     * @param visibleToContributor true to make visible; false to hide
     */
    public void setVisibleToContributor(boolean visibleToContributor) {
        this.visibleToContributor = visibleToContributor;
    }
}