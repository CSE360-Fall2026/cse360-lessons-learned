package entityClasses;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Entity model representing aggregated time and effort statistics merged
 * across multiple lessons-learned instances (Aspects 3 and 4 / Epics E4 and E5).
 *
 * @author Alex Meyers
 * @version 3.0
 */
public class LessonSummary {
    private int summaryId;
    private int targetLessonId;
    private Set<Integer> sourceLessonIds;
    private int entryCount;
    private double totalHours;
    private double meanHours;
    private double medianHours;
    private double minHours;
    private double maxHours;
    private double standardDeviationHours;
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
        return String.format("%.2f hrs", standardDeviationHours);
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