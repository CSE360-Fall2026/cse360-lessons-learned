/**
 * <p> JUnit 5 tests for TP3 Aspects 3 and 4 (Epics E4 and E5): merging time and effort data
 * into a summary, and the Curator's control of whether Contributors and Viewers can see it.</p>
 *
 * <p> CuratorSummaryTests (SU-01 to SU-29) tests the rules: the statistics, which observations
 * are merged, preview, confirm, cancel, saving, and visibility, including the two acceptance
 * checks Alex Meyers added to US-C-E5-01 (SU-20 and SU-21).  CuratorSummaryPageTests (SP-01 to
 * SP-15) tests what the pages show: the Merge Statistics page's Model, the summary panel on the
 * Contributor's Lesson Details page, and the Viewer's lesson details.</p>
 *
 * <p> Every test uses its own new in-memory H2 database, so the tests do not touch
 * FoundationDatabase and can run while the application is open.</p>
 */
package curatorSummaryTests;
