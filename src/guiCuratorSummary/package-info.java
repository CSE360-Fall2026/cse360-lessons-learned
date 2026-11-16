/**
 * <p> The Curator Merge Statistics page (TP3 Aspects 3 and 4, Epics E4 and E5): a Curator
 * merges the recorded time and effort hours of two or more lessons into a summary on a target
 * lesson (count, total, mean, median, minimum, maximum, and sample standard deviation), previews
 * it, saves it only after confirming, and chooses whether Contributors can see it.  The Curator
 * opens it for the lesson selected on the Curator Home Page (package guiCuratorHome).</p>
 *
 * <p> Like every Foundations GUI package, it follows Model-View-Controller:
 * ViewCuratorSummary declares and places the widgets, ControllerCuratorSummary reacts to the
 * user, and ModelCuratorSummary does the page's data work.  The merge and visibility rules are
 * in CuratorSummaryController, written by Alex Meyers, which the Model calls; the statistics
 * are computed by inputValidation.TimeEffortStatisticsCalculator and saved by
 * database.Database.saveLessonSummary.  Every action checks the Curator role first, using the
 * shared rule in guiTools.CuratorAccess.  Contributors and Viewers see a summary only through
 * guiContributorHome.ContributorLessonView, which drops hidden summaries.</p>
 *
 * <p> The rules are tested by curatorSummaryTests.CuratorSummaryTests (JUnit, SU-01 to SU-29),
 * and the page Model and the summary panels by curatorSummaryTests.CuratorSummaryPageTests
 * (JUnit, SP-01 to SP-15).  The View and Controller are tested by the manual tests M-CS1 to
 * M-CS8 in Manual Tests.pdf.</p>
 */
package guiCuratorSummary;
