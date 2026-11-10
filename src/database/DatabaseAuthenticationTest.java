
package database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import entityClasses.User;

/**
 * Tests the authentication methods in Database.java.
 *
 * These tests check that Admin users can log in with valid credentials
 * and that invalid login attempts are rejected. This includes incorrect
 * usernames, passwords, roles, and empty or null inputs.
 */
class DatabaseAuthenticationTest {

    private Database database;
    private final List<String> testUsernames = new ArrayList<>();

    /**
     * Connects to the test database before each test.
     *
     * @throws Exception if the database connection fails
     */
    @BeforeEach
    void setUp() throws Exception {
        database = new Database();
        database.connectToDatabase();
    }

    /**
     * Removes test accounts and closes the database connection after each test.
     */
    @AfterEach
    void tearDown() {

        // Remove only the accounts created during this test.
        for (String username : testUsernames) {
            database.deleteUser(username);
    }

        database.closeConnection();
    }

    /**
     * Tests whether an Admin can log in with the correct credentials.
     * The login should succeed because the account exists and has
     * the Admin role.
     *
     * @throws Exception if the test account cannot be created
     */
    @Test
    void testValidAdminLogin() throws Exception {

        // Use a unique username so the test doesn't conflict with existing accounts.
        String username = "hw3admin_" + System.nanoTime();
        String password = "TestPassword123";

        User admin = new User(username, password, "Test", "", "Admin", "Test",
                "test@example.com", true, false, false);

        database.register(admin);
        testUsernames.add(username);

        // The account has valid credentials and the Admin role, so login should work.
        boolean result = database.loginAdmin(admin);

        assertTrue(result, "Admin login should succeed with valid credentials");
    }

    /**
     * Tests whether an Admin login fails when the password is incorrect.
     * The username and role are valid, but the password does not match
     * the one stored in the database.
     *
     * @throws Exception if the test account cannot be created
     */
    @Test
    void testInvalidAdminPassword() throws Exception {

        // Create a valid Admin account to test against.
        String username = "hw3admin_" + System.nanoTime();
        String password = "TestPassword123";

        User admin = new User(username, password, "Test", "", "Admin", "Test",
                "test@example.com", true, false, false);

        database.register(admin);
        testUsernames.add(username);

        // Keep the username the same but use a different password.
        User invalidLogin = new User(username, "WrongPassword123", "Test", "",
                "Admin", "Test", "test@example.com", true, false, false);

        boolean result = database.loginAdmin(invalidLogin);

        // The database should reject the login because the passwords don't match.
        assertFalse(result, "Admin login should fail with an incorrect password");
    }

    /**
     * Tests whether a Contributor can log in as an Admin.
     * Even with the correct username and password, the login should
     * fail because the account does not have the Admin role.
     *
     * @throws Exception if the test account cannot be created
     */
    @Test
    void testAdminLoginWithoutAdminRole() throws Exception {

        // This account only has the Contributor role.
        String username = "hw3contributor_" + System.nanoTime();
        String password = "TestPassword123";

        User contributor = new User(username, password, "Test", "",
                "Contributor", "Test", "test@example.com", false, true, false);

        database.register(contributor);
        testUsernames.add(username);

        // Having valid credentials shouldn't give the user Admin access.
        boolean result = database.loginAdmin(contributor);

        assertFalse(result, "A Contributor should not be able to log in as an Admin");
    }

    /**
     * Tests whether an Admin login fails when the username is incorrect.
     * The password is correct, but the username does not match
     * an account in the database.
     *
     * @throws Exception if the test account cannot be created
     */
    @Test
    void testInvalidAdminUsername() throws Exception {

        // Create an Admin account with credentials we know are valid.
        String username = "hw3admin_" + System.nanoTime();
        String password = "TestPassword123";

        User admin = new User(username, password, "Test", "", "Admin", "Test",
                "test@example.com", true, false, false);

        database.register(admin);
        testUsernames.add(username);

        // Change the username while keeping the correct password.
        User invalidLogin = new User(username + "_wrong", password, "Test", "",
                "Admin", "Test", "test@example.com", true, false, false);

        boolean result = database.loginAdmin(invalidLogin);

        // The login should fail because that username isn't registered.
        assertFalse(result, "Admin login should fail with an incorrect username");
    }

    /**
     * Tests whether the system rejects empty usernames and passwords.
     * Both login attempts should fail because they don't match
     * the credentials of the registered Admin account.
     *
     * @throws Exception if the test account cannot be created
     */
    @Test
    void testEmptyAdminCredentials() throws Exception {

        // Create a valid account so we can compare the login attempts.
        String username = "hw3admin_" + System.nanoTime();
        String password = "TestPassword123";

        User admin = new User(username, password, "Test", "", "Admin", "Test",
                "test@example.com", true, false, false);

        database.register(admin);
        testUsernames.add(username);

        // An empty username shouldn't match the account we created.
        User emptyUsername = new User("", password, "Test", "", "Admin",
                "Test", "test@example.com", true, false, false);

        assertFalse(database.loginAdmin(emptyUsername),
                "Login should fail with an empty username");

        // An empty password shouldn't match the stored password either.
        User emptyPassword = new User(username, "", "Test", "", "Admin",
                "Test", "test@example.com", true, false, false);

        assertFalse(database.loginAdmin(emptyPassword),
                "Login should fail with an empty password");
    }

    /**
     * Tests how the login method handles null usernames and passwords.
     * The system should reject these inputs without crashing.
     */
    @Test
    void testNullAdminCredentials() {

        // Test a missing username with a password provided.
        User nullUsername = new User(null, "TestPassword123", "Test", "",
                "Admin", "Test", "test@example.com", true, false, false);

        assertFalse(database.loginAdmin(nullUsername),
                "Login should fail with a null username");

        // Test a missing password with a username provided.
        User nullPassword = new User("testadmin", null, "Test", "",
                "Admin", "Test", "test@example.com", true, false, false);

        assertFalse(database.loginAdmin(nullPassword),
                "Login should fail with a null password");
    }

    /**
     * Tests whether a Contributor can log in with valid credentials. The login
     * should succeed because the account exists and has the Contributor role.
     *
     * @throws Exception if the test account cannot be created
     */
    @Test
    void testValidContributorLogin() throws Exception {

        // Use a unique username to avoid conflicts with existing accounts.
        String username = "hw3contributor_" + System.nanoTime();
        String password = "TestPassword123";

        User contributor = new User(username, password, "Test", "", "Contributor", "Test", "test@example.com", false,
                true, false);

        database.register(contributor);
        testUsernames.add(username);

        // The account has valid credentials and the Contributor role.
        boolean result = database.loginRole1(contributor);

        assertTrue(result, "Contributor login should succeed with valid credentials");
    }

    /**
     * Tests whether a Contributor login fails with an incorrect password. The
     * username and role are valid, but the password does not match the one stored
     * in the database.
     *
     * @throws Exception if the test account cannot be created
     */
    @Test
    void testInvalidContributorPassword() throws Exception {

        // Create a valid Contributor account.
        String username = "hw3contributor_" + System.nanoTime();
        String password = "TestPassword123";

        User contributor = new User(username, password, "Test", "", "Contributor", "Test", "test@example.com", false,
                true, false);

        database.register(contributor);
        testUsernames.add(username);

        // Try logging in with the correct username but a different password.
        User invalidLogin = new User(username, "WrongPassword123", "Test", "", "Contributor", "Test",
                "test@example.com", false, true, false);

        boolean result = database.loginRole1(invalidLogin);

        // The login should fail because the passwords don't match.
        assertFalse(result, "Contributor login should fail with an incorrect password");
    }

    /**
     * Tests whether an Admin without the Contributor role can log in through the
     * Contributor login method. The login should fail because the account does not
     * have that role.
     *
     * @throws Exception if the test account cannot be created
     */
    @Test
    void testContributorLoginWithoutContributorRole() throws Exception {

        // Create an account that only has the Admin role.
        String username = "hw3admin_" + System.nanoTime();
        String password = "TestPassword123";

        User admin = new User(username, password, "Test", "", "Admin", "Test", "test@example.com", true, false, false);

        database.register(admin);
        testUsernames.add(username);

        // Try using the Admin credentials to log in as a Contributor.
        boolean result = database.loginRole1(admin);

        // The login should fail because the Contributor role isn't assigned.
        assertFalse(result, "An Admin without the Contributor role should not log in as a Contributor");
    }

    /**
     * Tests whether a Contributor login fails with an incorrect username. The
     * password is correct, but the username does not match an account in the
     * database.
     *
     * @throws Exception if the test account cannot be created
     */
    @Test
    void testInvalidContributorUsername() throws Exception {

        // Create a valid Contributor account.
        String username = "hw3contributor_" + System.nanoTime();
        String password = "TestPassword123";

        User contributor = new User(username, password, "Test", "", "Contributor", "Test", "test@example.com", false,
                true, false);

        database.register(contributor);
        testUsernames.add(username);

        // Keep the password the same but use a username that isn't registered.
        User invalidLogin = new User(username + "_wrong", password, "Test", "", "Contributor", "Test",
                "test@example.com", false, true, false);

        boolean result = database.loginRole1(invalidLogin);

        // The login should fail because the username doesn't exist.
        assertFalse(result, "Contributor login should fail with an incorrect username");
    }

    /**
     * Tests whether Contributor login rejects empty usernames and passwords. Both
     * attempts should fail because the credentials don't match the registered
     * Contributor account.
     *
     * @throws Exception if the test account cannot be created
     */
    @Test
    void testEmptyContributorCredentials() throws Exception {

        // Create a valid Contributor account to test against.
        String username = "hw3contributor_" + System.nanoTime();
        String password = "TestPassword123";

        User contributor = new User(username, password, "Test", "", "Contributor", "Test", "test@example.com", false,
                true, false);

        database.register(contributor);
        testUsernames.add(username);

        // Try logging in without a username.
        User emptyUsername = new User("", password, "Test", "", "Contributor", "Test", "test@example.com", false, true,
                false);

        assertFalse(database.loginRole1(emptyUsername), "Contributor login should fail with an empty username");

        // Try logging in without a password.
        User emptyPassword = new User(username, "", "Test", "", "Contributor", "Test", "test@example.com", false, true,
                false);

        assertFalse(database.loginRole1(emptyPassword), "Contributor login should fail with an empty password");
    }

    /**
     * Tests how Contributor login handles null usernames and passwords. Both
     * attempts should fail without causing the application to crash.
     */
    @Test
    void testNullContributorCredentials() {

        // Try logging in without a username.
        User nullUsername = new User(null, "TestPassword123", "Test", "", "Contributor", "Test", "test@example.com",
                false, true, false);

        assertFalse(database.loginRole1(nullUsername), "Contributor login should fail with a null username");

        // Try logging in without a password.
        User nullPassword = new User("testcontributor", null, "Test", "", "Contributor", "Test", "test@example.com",
                false, true, false);

        assertFalse(database.loginRole1(nullPassword), "Contributor login should fail with a null password");
    }

    /**
     * Tests whether the system generates a six-character invitation code and saves
     * it with the correct role in the database.
     */
    @Test
    void testGenerateInvitationCode() {

        // Generate an invitation code for a Contributor account.
        String email = "hw3test_" + System.nanoTime() + "@example.com";
        String role = "Contributor";

        int invitationsBefore = database.getNumberOfInvitations();
        String code = database.generateInvitationCode(email, role);

        // The generated code should exist and contain six characters.
        assertNotNull(code, "The invitation code should not be null");
        assertEquals(6, code.length(), "The invitation code should have six characters");

        int invitationsAfter = database.getNumberOfInvitations();

        assertEquals(invitationsBefore + 1, invitationsAfter,
                "Generating an invitation code should add one record to the database");

        // Check that the code is linked to the correct role.
        String storedRole = database.getRoleGivenAnInvitationCode(code);

        assertEquals(role, storedRole, "The invitation code should have the correct role");

        // Remove the test invitation so it doesn't stay in the database.
        database.removeInvitationAfterUse(code);
    }

    /**
     * Tests whether an invitation code is removed from the database after use. Once
     * removed, the code should no longer have an associated role.
     */
    @Test
    void testRemoveInvitationAfterUse() {

        // Generate an invitation code for a Contributor.
        String email = "hw3test_" + System.nanoTime() + "@example.com";
        String code = database.generateInvitationCode(email, "Contributor");

        // Make sure the invitation exists before removing it.
        assertEquals("Contributor", database.getRoleGivenAnInvitationCode(code),
                "The invitation should exist before being removed");

        // Remove the invitation and check that it is no longer valid.
        database.removeInvitationAfterUse(code);

        assertEquals("", database.getRoleGivenAnInvitationCode(code),
                "The invitation should no longer exist after being removed");
    }

    /**
     * Tests whether the system generates an eight-character one-time password and
     * saves it for the correct user.
     */
    @Test
    void testGenerateOneTimePassword() {

        // Use a unique username so the test doesn't conflict with existing codes.
        String username = "hw3user_" + System.nanoTime();

        String code = database.generateOneTimePassword(username);

        // Check that the generated code has the expected length.
        assertNotNull(code, "The one-time password should not be null");
        assertEquals(8, code.length(), "The one-time password should have eight characters");

        // The code should be valid for the user it was generated for.
        assertTrue(database.isOneTimePasswordValid(username, code),
                "The one-time password should be valid for the correct user");

        // Remove the test code so it doesn't stay in the database.
        database.clearOneTimePassword(code);
    }

    /**
     * Tests whether a one-time password can be used by a different user. The code
     * should only be valid for the username it was generated for.
     */
    @Test
    void testOneTimePasswordWithWrongUsername() {

        // Generate a password reset code for one user.
        String username = "hw3user_" + System.nanoTime();
        String code = database.generateOneTimePassword(username);

        // Make sure the code works for the correct username.
        assertTrue(database.isOneTimePasswordValid(username, code), "The code should be valid for the correct user");

        // Try using the same code with a different username.
        String wrongUsername = username + "_wrong";

        assertFalse(database.isOneTimePasswordValid(wrongUsername, code),
                "The code should not be valid for a different user");

        // Remove the test code from the database.
        database.clearOneTimePassword(code);
    }

    /**
     * Tests whether a one-time password becomes invalid after it is removed. The
     * code should work before removal and fail afterward.
     */
    @Test
    void testOneTimePasswordAfterRemoval() {

        // Generate a password reset code for a test user.
        String username = "hw3user_" + System.nanoTime();
        String code = database.generateOneTimePassword(username);

        // Check that the code is valid before removing it.
        assertTrue(database.isOneTimePasswordValid(username, code), "The code should be valid before removal");

        // Remove the code after use.
        database.clearOneTimePassword(code);

        // The same code should no longer work.
        assertFalse(database.isOneTimePasswordValid(username, code), "The code should be invalid after removal");
    }


    /**
     * Tests whether passwords are protected before being saved to the database. The
     * stored password should not match the original plaintext password.
     *
     * @throws Exception if the account cannot be created or the database cannot be
     *                   queried
     */
    @Test
    void testPasswordNotStoredAsPlaintext() throws Exception {

        // Create an account with a password we can compare against.
        String username = "hw3password_" + System.nanoTime();
        String password = "TestPassword123";

        User user = new User(username, password, "Test", "", "User", "Test", "test@example.com", false, true, false);

        database.register(user);
        testUsernames.add(username);

        // Read the actual password value saved in the H2 database.
        try (Connection connection = DriverManager.getConnection("jdbc:h2:~/HW3TestDatabase", "sa", "");
                PreparedStatement statement = connection
                        .prepareStatement("SELECT password FROM userDB WHERE userName = ?")) {

            statement.setString(1, username);

            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next(), "The test account should exist");

                String storedPassword = result.getString("password");

                // The database should not contain the original plaintext password.
                assertNotEquals(password, storedPassword, "Passwords should not be stored as plaintext");
            }
    }
    }

    /**
     * Tests whether an updated password is protected before being saved. The
     * database should not store the new password as plaintext.
     *
     * @throws Exception if the account cannot be created or queried
     */
    @Test
    void testUpdatedPasswordNotStoredAsPlaintext() throws Exception {

        // Create an account with an initial password.
        String username = "hw3update_" + System.nanoTime();
        String originalPassword = "OriginalPassword123";
        String newPassword = "UpdatedPassword456";

        User user = new User(username, originalPassword, "Test", "", "User", "Test", "test@example.com", false, true,
                false);

        database.register(user);
        testUsernames.add(username);

        // Update the password using the existing database method.
        database.updatePassword(username, newPassword);

        // Read the updated password directly from the H2 database.
        try (Connection connection = DriverManager.getConnection("jdbc:h2:~/HW3TestDatabase", "sa", "");
                PreparedStatement statement = connection
                        .prepareStatement("SELECT password FROM userDB WHERE userName = ?")) {

            statement.setString(1, username);

            try (ResultSet result = statement.executeQuery()) {
                assertTrue(result.next(), "The test account should exist");

                String storedPassword = result.getString("password");

                // Check that the database isn't storing the new password as plaintext.
                assertNotEquals(newPassword, storedPassword, "Updated passwords should not be stored as plaintext");
            }
        }
    }

    /**
     * Tests whether the system rejects an incorrect one-time password. A code that
     * was not generated for the user should not be accepted.
     */
    @Test
    void testInvalidOneTimePassword() {

        // Generate a valid password reset code.
        String username = "hw3user_" + System.nanoTime();
        String code = database.generateOneTimePassword(username);

        try {
            // Make sure the original code works.
            assertTrue(database.isOneTimePasswordValid(username, code), "The generated code should be valid");

            // Change the code so it no longer matches the stored value.
            String invalidCode = code + "_wrong";

            assertFalse(database.isOneTimePasswordValid(username, invalidCode),
                    "An incorrect one-time password should be rejected");
        } finally {
            // Remove the code even if an assertion fails.
            database.clearOneTimePassword(code);
        }
    }

    /**
     * Tests whether the system rejects empty and null one-time passwords. Missing
     * reset codes should not be accepted.
     */
    @Test
    void testEmptyAndNullOneTimePasswords() {

        // Generate a valid reset code for a test user.
        String username = "hw3user_" + System.nanoTime();
        String code = database.generateOneTimePassword(username);

        try {
            // Make sure the generated code is valid.
            assertTrue(database.isOneTimePasswordValid(username, code), "The generated code should be valid");

            // An empty code should not be accepted.
            assertFalse(database.isOneTimePasswordValid(username, ""), "An empty one-time password should be rejected");

            // A null code should not be accepted.
            assertFalse(database.isOneTimePasswordValid(username, null), "A null one-time password should be rejected");
        } finally {
            // Remove the generated code after testing.
            database.clearOneTimePassword(code);
        }
    }

}
