package guiContributorHome;

import entityClasses.LessonSummary;

/**
 * Evaluates whether statistical summaries should be rendered on the Contributor view (Aspect 4 / Epic E5).
 * 
 * @author Alex Meyers
 * @version 1.0
 */
public class ContributorLessonView {

    /**
     * Determines if the statistical summary is authorized for display to a Contributor.
     * 
     * @param summary the LessonSummary associated with the lesson
     * @return true if visible, false if hidden (default)
     */
    public boolean shouldDisplaySummary(LessonSummary summary) {
        if (summary == null) {
            return false; // Defensive null check (CWE-476 / R9)
        }
        
        // Per Epic E5 requirements, summaries are hidden by default and only display 
        // when the Curator explicitly toggles statsVisibleToContributor to true.
        return summary.isStatsVisibleToContributor();
    }
}