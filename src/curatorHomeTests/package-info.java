/**
 * <p> JUnit 5 tests for TP3 Aspect 1, the Curator role and the Curator Home Page (Epics.pdf
 * dependency D1, needed by Epics E2 to E6).  The tests are CH-01 to CH-36 and cover the
 * requirements CR-1 to CR-8 listed in the CuratorHomeTests class description, which come from
 * the user stories US-CR1, US-CR2, US-CH1, US-CH2, US-CH2-V, US-CH3, and US-CH-V in Staff User
 * Stories.pdf.</p>
 *
 * <p> The tests use the application's H2 database, exactly like the tests in lessonTests and
 * curatorLockTests, so FoundationsMain must not be running while they run.  Which page opens
 * after a Curator logs in can only be seen on the screen, so it is covered by the manual tests
 * M-CH1 to M-CH6 in Manual Tests.pdf.</p>
 */
package curatorHomeTests;
