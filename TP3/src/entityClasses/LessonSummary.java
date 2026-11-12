package entityClasses;

/**
 * Represents a merged statistical summary of time and effort metrics 
 * derived from experience entries across multiple selected lessons (Aspect 3 / Epic E4).
 * <p>
 * Validated by: {@code TimeEffortStatisticsCalculatorTest}, {@code LessonSummaryDaoTest}.
 * 
 * @author Alex Meyers
 * @version 1.0
 */
public class LessonSummary {
    private int summaryId;
    private int targetLessonId;
    private int entryCount;
    private double totalHours;
    private double meanHours;
    private double medianHours;
    private double minHours;
    private double maxHours;
    private double standardDeviation;
    private boolean statsVisibleToContributor;

    /**
     * Constructs a new LessonSummary with computed statistical values.
     * 
     * @param summaryId unique identifier for the summary record
     * @param targetLessonId the lesson ID where this summary is attached
     * @param entryCount total number of contributing experience entries
     * @param totalHours sum of all effort hours
     * @param meanHours arithmetic mean of effort hours
     * @param medianHours median effort hours
     * @param minHours minimum recorded effort hours
     * @param maxHours maximum recorded effort hours
     * @param standardDeviation sample standard deviation of effort hours
     * @param statsVisibleToContributor flag indicating visibility to contributors (default false)
     */
    public LessonSummary(int summaryId, int targetLessonId, int entryCount, double totalHours,
                         double meanHours, double medianHours, double minHours, double maxHours,
                         double standardDeviation, boolean statsVisibleToContributor) {
        this.summaryId = summaryId;
        this.targetLessonId = targetLessonId;
        this.entryCount = entryCount;
        this.totalHours = totalHours;
        this.meanHours = meanHours;
        this.medianHours = medianHours;
        this.minHours = minHours;
        this.maxHours = maxHours;
        this.standardDeviation = standardDeviation;
        this.statsVisibleToContributor = statsVisibleToContributor;
    }

    public int getSummaryId() { return summaryId; }
    public int getTargetLessonId() { return targetLessonId; }
    public int getEntryCount() { return entryCount; }
    public double getTotalHours() { return totalHours; }
    public double getMeanHours() { return meanHours; }
    public double getMedianHours() { return medianHours; }
    public double getMinHours() { return minHours; }
    public double getMaxHours() { return maxHours; }
    public double getStandardDeviation() { return standardDeviation; }
    public boolean isStatsVisibleToContributor() { return statsVisibleToContributor; }
    
    public void setStatsVisibleToContributor(boolean statsVisibleToContributor) {
        this.statsVisibleToContributor = statsVisibleToContributor;
    }
}