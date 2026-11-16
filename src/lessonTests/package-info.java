/*******
 * <p> The TP2 tests for the Contributor User Stories and the lessons-learned entity classes
 * (TP2 Tasks 6 and 7). </p>
 *
 * <p> <b>Requirements.</b>  The requirements tested are the fourteen Contributor User Stories
 * and seven entity-class requirements drawn from TP2 Task 3 (decision D19): </p>
 * <ul>
 * <li> US-C1, US-C1-V: create a lesson; invalid core information is refused. </li>
 * <li> US-R1, US-R2, US-R2-V, US-R3, US-R3-V, US-R4: view my list, search my titles, open one
 * 		lesson, fail safely for a lesson that is missing or not mine, and see only my own
 * 		lessons. </li>
 * <li> US-U1, US-U1-V, US-U2, US-U2-V: update an unlocked lesson and add experience
 * 		information; locked fields and invalid values are refused. </li>
 * <li> US-D1, US-D1-V: delete only after "Yes"; locked lessons cannot be deleted. </li>
 * <li> E1: LessonsLearned implements Create, Read, Update, and Delete. </li>
 * <li> E2: LessonsLearned validates its fields and refuses to store invalid data. </li>
 * <li> E3: LessonsLearnedList loads, adds, finds, replaces, and removes lessons. </li>
 * <li> E4: LessonsLearnedList returns subsets (title, keywords, category, tag) as new lists. </li>
 * <li> E5: lock flags are saved and reported for each field and the whole lesson. </li>
 * <li> E6: the TP3 and AICoaching attributes are saved, read back, and validated. </li>
 * <li> E7: the team-member effort list: valid names and hours, members' hours no more than the
 * 		total, at most 20 members. </li>
 * </ul>
 *
 * <p> <b>Tests.</b>  Every requirement has at least two tests, positive and negative.  Each
 * test is one method whose Javadoc names the requirement, explains how the code satisfies it,
 * and explains how to read the output.  The test ids combine the requirement and the kind of
 * test: "C1V-N2" is the second negative test of US-C1-V; "E7-P1" is the first positive test of
 * E7. </p>
 * <ul>
 * <li> CreateLessonTests, ReadLessonTests, UpdateLessonTests, AddExperienceTests, and
 * 		DeleteLessonTests test the user stories through each page's Model. </li>
 * <li> LessonsLearnedEntityTests and LessonsLearnedListEntityTests test the entity classes
 * 		directly. </li>
 * <li> AllContributorTests runs all seven classes as one console program; TestSupport holds
 * 		the shared output format and test data helpers. </li>
 * <li> SharedAiServiceTests holds the JUnit tests for the shared AI service (TP3 Aspect 8). </li>
 * <li> ManualGuiTests walks a tester through the GUI checks M1 to M7 and adds the sample
 * 		lessons they use. </li>
 * <li> inputValidation.LessonRecognizerTest holds the field-by-field boundary tests (Tests
 * 		1-58). </li>
 * </ul>
 *
 * <p> In TP2 the tests were console programs with a main method, in the style of the
 * Foundations PasswordEvaluationTestingAutomation class (decision D17).  In TP3 (Task 5.4) the
 * seven test classes were converted to JUnit 5: run the package with Run As &gt; JUnit Test to run
 * all 66 tests, listed under their TP2 ids.  The console programs still work and print the same
 * output as in TP2.  The tests use the real H2 database, so FoundationsMain must be closed while
 * they run.  ManualGuiTests stays a manual walkthrough, because what a page shows can only be
 * judged by looking at it. </p>
 */
package lessonTests;
