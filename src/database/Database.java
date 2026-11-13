package database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import entityClasses.ExperienceEntry;
import entityClasses.LessonsLearned;
import entityClasses.LessonsLearnedList;
import entityClasses.MemberEffort;
import entityClasses.RefinementDraft;
import entityClasses.User;

/*******
 * <p> Title: Database Class. </p>
 *
 * <p> Description: This is an in-memory database built on H2.  Detailed documentation of H2 can
 * be found at https://www.h2database.com/html/main.html (Click on "PDF (2MB)" on the l3ft side
 * of the page under the heading "Reference" for a PDF of 438 pages.)  This class leverages H2
 * and provides numerous special supporting methods.
 * </p>
 *
 * <p> Copyright: Lynn Robert Carter © 2025 </p>
 *
 * @author Lynn Robert Carter
 *
 * @version 2.00		2025-04-29 Updated and expanded from the version produce by Pravalika
 * 							Mukkiri and Ishwarya Hidkimath Basavaraj
 * @version 2.01		2025-12-17 Minor updates for Spring 2026
 * @version 2.02		2026-09-29 HW2: added the lessonsLearnedDB and experienceDB tables and the
 * 							CRUD methods the Contributor role uses for lessons learned
 * @version 2.03		2026-10-14 TP2: in-place migration of lessonsLearnedDB for the Phase Three
 * 							attributes; experienceMemberEffortDB table; all-lessons query;
 * 							Curator-field update; transactional experience insert
 * @version 2.04		2026-11-01 HW3 Task 5: setLessonLocks() saves several lock flags in one
 * 							UPDATE for the Curator lock prototype; lockColumn() shared with
 * 							setLessonLock()
 * @version 2.05		2026-11-10 TP3 Aspect 1: updateUserRole() also accepts "Curator" for the
 * 							Role2 flag
 */

/*
 * The Database class is responsible for establishing and managing the connection to the database,
 * and performing operations such as user registration, login validation, handling invitation
 * codes, and numerous other database related functions.
 */
public class Database {

	/** JDBC driver name. */
	static final String JDBC_DRIVER = "org.h2.Driver";
	/** Database URL: an H2 file named FoundationDatabase in the user's home folder. */
    static final String DB_URL = "jdbc:h2:~/FoundationDatabase";

	/** Database user name (H2 default). */
	static final String USER = "sa";
	/** Database password (H2 default, empty). */
	static final String PASS = "";

	/** Singleton connection used to access the database. */
	private Connection connection = null;
	/** The H2 Statement used to construct queries. */
	private Statement statement = null;

	// These are the easily accessible attributes of the currently logged-in user
	// This is only useful for single user applications
	/** Username of the current user. */
	private String currentUsername;
	/** Password of the current user. */
	private String currentPassword;
	/** First name of the current user. */
	private String currentFirstName;
	/** Middle name of the current user. */
	private String currentMiddleName;
	/** Last name of the current user. */
	private String currentLastName;
	/** Preferred first name of the current user. */
	private String currentPreferredFirstName;
	/** Email address of the current user. */
	private String currentEmailAddress;
	/** Whether the current user has the Admin role. */
	private boolean currentAdminRole;
	/** Whether the current user has Role1 (shown as Contributor). */
	private boolean currentNewRole1;
	/** Whether the current user has Role2 (shown as Curator since TP3 Aspect 1). */
	private boolean currentNewRole2;

	/*******
	 * <p> Method: Database </p>
	 *
	 * <p> Description: The default constructor used to establish this singleton object.</p>
	 *
	 */

	public Database () {

	}


/*******
 * <p> Method: connectToDatabase </p>
 *
 * <p> Description: Used to establish the in-memory instance of the H2 database from secondary
 *		storage.</p>
 *
 * @throws SQLException when the DriverManager is unable to establish a connection
 *
 */
	public void connectToDatabase() throws SQLException {
		try {
			Class.forName(JDBC_DRIVER); // Load the JDBC driver
			connection = DriverManager.getConnection(DB_URL, USER, PASS);
			statement = connection.createStatement();
			// You can use this command to clear the database and restart from fresh.
			//statement.execute("DROP ALL OBJECTS");

			createTables();  // Create the necessary tables if they don't exist
		} catch (ClassNotFoundException e) {
			System.err.println("JDBC Driver not found: " + e.getMessage());
		}
	}


/*******
 * <p> Method: createTables </p>
 *
 * <p> Description: Used to create new instances of the two database tables used by this class.</p>
 *
 * @throws SQLException if a table cannot be created
 */
	private void createTables() throws SQLException {
		// Create the user database
		String userTable = "CREATE TABLE IF NOT EXISTS userDB ("
				+ "id INT AUTO_INCREMENT PRIMARY KEY, "
				+ "userName VARCHAR(255) UNIQUE, "
				+ "password VARCHAR(255), "
				+ "firstName VARCHAR(255), "
				+ "middleName VARCHAR(255), "
				+ "lastName VARCHAR (255), "
				+ "preferredFirstName VARCHAR(255), "
				+ "emailAddress VARCHAR(255), "
				+ "adminRole BOOL DEFAULT FALSE, "
				+ "newRole1 BOOL DEFAULT FALSE, "
				+ "newRole2 BOOL DEFAULT FALSE)";
		statement.execute(userTable);

		// Create the invitation codes table
	    String invitationCodesTable = "CREATE TABLE IF NOT EXISTS InvitationCodes ("
	            + "code VARCHAR(10) PRIMARY KEY, "
	    		+ "emailAddress VARCHAR(255), "
                + "role VARCHAR(15))";
	    statement.execute(invitationCodesTable);

        // Update the role column for databases that already exist.
        statement.execute("ALTER TABLE InvitationCodes ALTER COLUMN role VARCHAR(15)");

	    // Create the one-time passwords table
 		String otpTable = "CREATE TABLE IF NOT EXISTS OneTimePasswords ("
 				+ "code VARCHAR(10) PRIMARY KEY, "
 				+ "userName VARCHAR(255))";
 		statement.execute(otpTable);

		// HW2: Create the lessons learned table.  The column sizes match the maximum lengths the
		// LessonRecognizer enforces (title 100, description 5000, tags 200), so anything that gets
		// past the recognizer is guaranteed to fit in the column.  ownerUserName deliberately has NO
		// foreign key to userDB: deleteUser() swallows SQL errors, so a foreign key would make
		// deleting a user who owns lessons fail without telling anybody.
		String lessonsTable = "CREATE TABLE IF NOT EXISTS lessonsLearnedDB ("
				+ "id INT AUTO_INCREMENT PRIMARY KEY, "
				+ "ownerUserName VARCHAR(255) NOT NULL, "
				+ "title VARCHAR(100) NOT NULL, "
				+ "description VARCHAR(5000) NOT NULL, "
				+ "tags VARCHAR(200) DEFAULT '', "
				+ "category VARCHAR(50) NOT NULL, "
				+ "lockedTitle BOOL DEFAULT FALSE, "
				+ "lockedDescription BOOL DEFAULT FALSE, "
				+ "lockedTags BOOL DEFAULT FALSE, "
				+ "lockedCategory BOOL DEFAULT FALSE, "
				+ "lockedEntire BOOL DEFAULT FALSE, "
				+ "createdAt BIGINT NOT NULL, "
				+ "updatedAt BIGINT NOT NULL)";
		statement.execute(lessonsTable);

		// HW2: Create the experience table.  ON DELETE CASCADE means deleting a lesson also removes
		// its experience rows, so no orphan rows are left behind (Test Case: Delete Lesson).
		String experienceTable = "CREATE TABLE IF NOT EXISTS experienceDB ("
				+ "id INT AUTO_INCREMENT PRIMARY KEY, "
				+ "lessonId INT NOT NULL, "
				+ "timeEffortHours DOUBLE NOT NULL, "
				+ "description VARCHAR(2000) NOT NULL, "
				+ "createdAt BIGINT NOT NULL, "
				+ "FOREIGN KEY (lessonId) REFERENCES lessonsLearnedDB(id) ON DELETE CASCADE)";
		statement.execute(experienceTable);

		// TP2: add the Phase Three attributes to lessonsLearnedDB (see the method's description)
		migrateLessonsLearnedTable();

		// TP2: Create the per-member effort table (V1: "the effort used by the various team
		// members").  ON DELETE CASCADE chains with the one above: deleting a lesson deletes its
		// experience rows, and deleting an experience row deletes its member rows, so a single
		// DELETE of a lesson leaves no orphan rows anywhere.  memberName matches the
		// 100-character limit LessonRecognizer.checkForValidMemberName() enforces.
		String memberEffortTable = "CREATE TABLE IF NOT EXISTS experienceMemberEffortDB ("
				+ "id INT AUTO_INCREMENT PRIMARY KEY, "
				+ "experienceId INT NOT NULL, "
				+ "memberName VARCHAR(100) NOT NULL, "
				+ "hours DOUBLE NOT NULL, "
				+ "FOREIGN KEY (experienceId) REFERENCES experienceDB(id) ON DELETE CASCADE)";
		statement.execute(memberEffortTable);
	}


/*******
 * <p> Method: migrateLessonsLearnedTable </p>
 *
 * <p> Description: TP2.  Adds the columns for the LessonsLearned attributes introduced in TP2
 * (the AI coaching fields and the Curator fields for Phase Three) to lessonsLearnedDB.</p>
 *
 * <p> The columns are added with ALTER TABLE ... ADD COLUMN IF NOT EXISTS rather than being
 * written into the CREATE TABLE statement above.  CREATE TABLE IF NOT EXISTS does nothing when
 * the table already exists, so a database file created in HW2 would never receive new columns
 * and every query that reads them would fail.  ADD COLUMN IF NOT EXISTS works in both cases: on
 * a new database it adds each column once, on an existing database it adds only the columns that
 * are missing, and on every later start it does nothing.  Existing rows receive the DEFAULT
 * value, which matches the "not set" value of the corresponding LessonsLearned attribute, and
 * NOT NULL means a read never has to handle a null.</p>
 *
 * <p> Column sizes match the maximum lengths LessonRecognizer enforces (root cause and
 * recommendation 2000, search terms 500), and the original title and description are sized
 * like the title and description they copy.</p>
 *
 * @throws SQLException if a column cannot be added
 */
	private void migrateLessonsLearnedTable() throws SQLException {
		String[] newColumns = {
				"rootCause VARCHAR(2000) DEFAULT '' NOT NULL",					// AICoaching
				"recommendation VARCHAR(2000) DEFAULT '' NOT NULL",				// AICoaching
				"aiAssisted BOOL DEFAULT FALSE NOT NULL",						// Epic E1
				"searchTerms VARCHAR(500) DEFAULT '' NOT NULL",					// Epic E2
				"curatedBy VARCHAR(255) DEFAULT '' NOT NULL",					// Epic E3
				"curatedAt BIGINT DEFAULT 0 NOT NULL",							// Epic E3
				"originalTitle VARCHAR(100) DEFAULT '' NOT NULL",				// Epic E3
				"originalDescription VARCHAR(5000) DEFAULT '' NOT NULL",		// Epic E3
				"summaryId INT DEFAULT -1 NOT NULL",							// Epic E4
				"statsVisibleToContributor BOOL DEFAULT FALSE NOT NULL"			// Epic E5
		};
		// The column definitions are fixed strings in this method, never user input, so building
		// the statement by concatenation is safe here
		for (String column : newColumns)
			statement.execute("ALTER TABLE lessonsLearnedDB ADD COLUMN IF NOT EXISTS " + column);
	}


/*******
 * <p> Method: isDatabaseEmpty </p>
 *
 * <p> Description: If the user database has no rows, true is returned, else false.</p>
 *
 * @return true if the database is empty, else it returns false
 *
 */
	public boolean isDatabaseEmpty() {
		String query = "SELECT COUNT(*) AS count FROM userDB";
		try {
			ResultSet resultSet = statement.executeQuery(query);
			if (resultSet.next()) {
				return resultSet.getInt("count") == 0;
			}
		}  catch (SQLException e) {
	        return false;
	    }
		return true;
	}


/*******
 * <p> Method: getNumberOfUsers </p>
 *
 * <p> Description: Returns an integer .of the number of users currently in the user database. </p>
 *
 * @return the number of user records in the database.
 *
 */
	public int getNumberOfUsers() {
		String query = "SELECT COUNT(*) AS count FROM userDB";
		try {
			ResultSet resultSet = statement.executeQuery(query);
			if (resultSet.next()) {
				return resultSet.getInt("count");
			}
		} catch (SQLException e) {
	        return 0;
	    }
		return 0;
	}

/*******
 * <p> Method: register(User user) </p>
 *
 * <p> Description: Creates a new row in the database using the user parameter. </p>
 *
 * @throws SQLException when there is an issue creating the SQL command or executing it.
 *
 * @param user specifies a user object to be added to the database.
 *
 */
	public void register(User user) throws SQLException {
		String insertUser = "INSERT INTO userDB (userName, password, firstName, middleName, "
				+ "lastName, preferredFirstName, emailAddress, adminRole, newRole1, newRole2) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement pstmt = connection.prepareStatement(insertUser)) {
			currentUsername = user.getUserName();
			pstmt.setString(1, currentUsername);

			currentPassword = user.getPassword();
			pstmt.setString(2, currentPassword);

			currentFirstName = user.getFirstName();
			pstmt.setString(3, currentFirstName);

			currentMiddleName = user.getMiddleName();
			pstmt.setString(4, currentMiddleName);

			currentLastName = user.getLastName();
			pstmt.setString(5, currentLastName);

			currentPreferredFirstName = user.getPreferredFirstName();
			pstmt.setString(6, currentPreferredFirstName);

			currentEmailAddress = user.getEmailAddress();
			pstmt.setString(7, currentEmailAddress);

			currentAdminRole = user.getAdminRole();
			pstmt.setBoolean(8, currentAdminRole);

			currentNewRole1 = user.getNewRole1();
			pstmt.setBoolean(9, currentNewRole1);

			currentNewRole2 = user.getNewRole2();
			pstmt.setBoolean(10, currentNewRole2);

			pstmt.executeUpdate();
		}

	}

/*******
 *  <p> Method: List getUserList() </p>
 *
 *  <P> Description: Generate an List of Strings, one for each user in the database,
 *  starting with "&lt;Select User&gt;" at the start of the list. </p>
 *
 *  @return a list of userNames found in the database.
 */
	public List<String> getUserList () {
		List<String> userList = new ArrayList<String>();
		userList.add("<Select a User>");
		String query = "SELECT userName FROM userDB";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				userList.add(rs.getString("userName"));
			}
		} catch (SQLException e) {
	        return null;
	    }
//		System.out.println(userList);
		return userList;
	}

/*******
 * <p> Method: boolean loginAdmin(User user) </p>
 *
 * <p> Description: Check to see that a user with the specified username, password, and role
 * 		is the same as a row in the table for the username, password, and role. </p>
 *
 * @param user specifies the specific user that should be logged in playing the Admin role.
 *
 * @return true if the specified user has been logged in as an Admin else false.
 *
 */
	public boolean loginAdmin(User user){
		// Validates an admin user's login credentials so the user can login in as an Admin.
		String query = "SELECT * FROM userDB WHERE userName = ? AND password = ? AND "
				+ "adminRole = TRUE";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, user.getUserName());
			pstmt.setString(2, user.getPassword());
			ResultSet rs = pstmt.executeQuery();
			return rs.next();	// If a row is returned, rs.next() will return true
		} catch  (SQLException e) {
	        e.printStackTrace();
	    }
		return false;
	}


/*******
 * <p> Method: boolean loginRole1(User user) </p>
 *
 * <p> Description: Check to see that a user with the specified username, password, and role
 * 		is the same as a row in the table for the username, password, and role. </p>
 *
 * @param user specifies the specific user that should be logged in playing the Student role.
 *
 * @return true if the specified user has been logged in as an Student else false.
 *
 */
	public boolean loginRole1(User user) {
		// Validates a student user's login credentials.
		String query = "SELECT * FROM userDB WHERE userName = ? AND password = ? AND "
				+ "newRole1 = TRUE";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, user.getUserName());
			pstmt.setString(2, user.getPassword());
			ResultSet rs = pstmt.executeQuery();
			return rs.next();
		} catch  (SQLException e) {
		       e.printStackTrace();
		}
		return false;
	}

	/*******
	 * <p> Method: boolean loginRole2(User user) </p>
	 *
	 * <p> Description: Check to see that a user with the specified username, password, and role
	 * 		is the same as a row in the table for the username, password, and role. </p>
	 *
	 * <p> Since TP3 Aspect 1 the Role2 flag is the Curator role, so this is the Curator login
	 * 		check (US-CR2).  Because it reads the stored flag, a user whose Curator role was
	 * 		removed by an Admin is refused at the next login.
	 * 		Tested by curatorHomeTests.CuratorHomeTests CH-12 to CH-15 (JUnit). </p>
	 *
	 * @param user specifies the specific user that should be logged in playing the Curator role.
	 *
	 * @return true if the specified user has been logged in as a Curator else false.
	 *
	 */
	// Validates a Curator's login credentials.
	public boolean loginRole2(User user) {
		String query = "SELECT * FROM userDB WHERE userName = ? AND password = ? AND "
				+ "newRole2 = TRUE";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, user.getUserName());
			pstmt.setString(2, user.getPassword());
			ResultSet rs = pstmt.executeQuery();
			return rs.next();
		} catch  (SQLException e) {
		       e.printStackTrace();
		}
		return false;
	}


	/*******
	 * <p> Method: boolean doesUserExist(User user) </p>
	 *
	 * <p> Description: Check to see that a user with the specified username is  in the table. </p>
	 *
	 * @param userName specifies the specific user that we want to determine if it is in the table.
	 *
	 * @return true if the specified user is in the table else false.
	 *
	 */
	// Checks if a user already exists in the database based on their userName.
	public boolean doesUserExist(String userName) {
	    String query = "SELECT COUNT(*) FROM userDB WHERE userName = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {

	        pstmt.setString(1, userName);
	        ResultSet rs = pstmt.executeQuery();

	        if (rs.next()) {
	            // If the count is greater than 0, the user exists
	            return rs.getInt(1) > 0;
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return false; // If an error occurs, assume user doesn't exist
	}


	/*******
	 * <p> Method: int getNumberOfRoles(User user) </p>
	 *
	 * <p> Description: Determine the number of roles a specified user plays. </p>
	 *
	 * @param user specifies the specific user that we want to determine if it is in the table.
	 *
	 * @return the number of roles this user plays (0 - 5).
	 *
	 */
	// Get the number of roles that this user plays
	public int getNumberOfRoles (User user) {
		int numberOfRoles = 0;
		if (user.getAdminRole()) numberOfRoles++;
		if (user.getNewRole1()) numberOfRoles++;
		if (user.getNewRole2()) numberOfRoles++;
		return numberOfRoles;
	}


	/*******
	 * <p> Method: String generateInvitationCode(String emailAddress, String role) </p>
	 *
	 * <p> Description: Given an email address and a roles, this method establishes and invitation
	 * code and adds a record to the InvitationCodes table.  When the invitation code is used, the
	 * stored email address is used to establish the new user and the record is removed from the
	 * table.</p>
	 *
	 * @param emailAddress specifies the email address for this new user.
	 *
	 * @param role specified the role that this new user will play.
	 *
	 * @return the code of six characters so the new user can use it to securely setup an account.
	 *
	 */
	// Generates a new invitation code and inserts it into the database.
	public String generateInvitationCode(String emailAddress, String role) {
	    String code = UUID.randomUUID().toString().substring(0, 6); // Generate a random 6-character code
	    String query = "INSERT INTO InvitationCodes (code, emailaddress, role) VALUES (?, ?, ?)";

	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, code);
	        pstmt.setString(2, emailAddress);
	        pstmt.setString(3, role);
	        pstmt.executeUpdate();
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return code;
	}

	/*******
	 * <p> Method: String generateOneTimePassword(String username) </p>
	 *
	 * <p> Description: Generates a temporary 8-character one-time password. </p>
	 *
	 * @param username specifies the user that needs the temporary password
	 *
	 * @return the code of 8 characters for the user to use to reset password
	 */
	// Generates a new temporary one-time password
	public String generateOneTimePassword(String username) {
		String code = UUID.randomUUID().toString().substring(0, 8);
		String query = "INSERT INTO OneTimePasswords (code, userName) VALUES (?, ?)";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, code);
			pstmt.setString(2, username);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return code;
	}

	/*******
	 * <p> Method: boolean isOneTimePasswordValid(String username, String code) </p>
	 *
	 * <p> Description: Checks if an active one-time password exists for the user. </p>
	 *
	 * @param username	the user the one-time password was issued to
	 * @param code		the one-time password entered
	 * @return true if the code is an active one-time password for that user
	 */
	public boolean isOneTimePasswordValid(String username, String code) {
		String query = "SELECT COUNT(*) AS count FROM OneTimePasswords WHERE code = ? AND userName = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, code);
			pstmt.setString(2, username);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				return rs.getInt("count") > 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	/*******
	 * <p> Method: void clearOneTimePassword(String code) </p>
	 *
	 * <p> Description: Deletes the one-time password once used.</p>
	 *
	 * @param code	the one-time password to delete
	 */
	public void clearOneTimePassword(String code) {
		String query = "DELETE FROM OneTimePasswords WHERE code = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, code);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}


	/*******
	 * <p> Method: int getNumberOfInvitations() </p>
	 *
	 * <p> Description: Determine the number of outstanding invitations in the table.</p>
	 *
	 * @return the number of invitations in the table.
	 *
	 */
	// Number of invitations in the database
	public int getNumberOfInvitations() {
		String query = "SELECT COUNT(*) AS count FROM InvitationCodes";
		try {
			ResultSet resultSet = statement.executeQuery(query);
			if (resultSet.next()) {
				return resultSet.getInt("count");
			}
		} catch  (SQLException e) {
	        e.printStackTrace();
	    }
		return 0;
	}


	/*******
	 * <p> Method: boolean emailaddressHasBeenUsed(String emailAddress) </p>
	 *
	 * <p> Description: Determine if an email address has been user to establish a user.</p>
	 *
	 * @param emailAddress is a string that identifies a user in the table
	 *
	 * @return true if the email address is in the table, else return false.
	 *
	 */
	// Check to see if an email address is already in the database
	public boolean emailaddressHasBeenUsed(String emailAddress) {
	    String query = "SELECT COUNT(*) AS count FROM InvitationCodes WHERE emailAddress = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, emailAddress);
	        ResultSet rs = pstmt.executeQuery();
	 //     System.out.println(rs);
	        if (rs.next()) {
	            // Mark the code as used
	        	return rs.getInt("count")>0;
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
		return false;
	}


	/*******
	 * <p> Method: String getRoleGivenAnInvitationCode(String code) </p>
	 *
	 * <p> Description: Get the role associated with an invitation code.</p>
	 *
	 * @param code is the 6 character String invitation code
	 *
	 * @return the role for the code or an empty string.
	 *
	 */
	// Obtain the roles associated with an invitation code.
	public String getRoleGivenAnInvitationCode(String code) {
	    String query = "SELECT * FROM InvitationCodes WHERE code = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, code);
	        ResultSet rs = pstmt.executeQuery();
	        if (rs.next()) {
	            return rs.getString("role");
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	    return "";
	}


	/*******
	 * <p> Method: String getEmailAddressUsingCode (String code ) </p>
	 *
	 * <p> Description: Get the email addressed associated with an invitation code.</p>
	 *
	 * @param code is the 6 character String invitation code
	 *
	 * @return the email address for the code or an empty string.
	 *
	 */
	// For a given invitation code, return the associated email address of an empty string
	public String getEmailAddressUsingCode (String code ) {
	    String query = "SELECT emailAddress FROM InvitationCodes WHERE code = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, code);
	        ResultSet rs = pstmt.executeQuery();
	        if (rs.next()) {
	            return rs.getString("emailAddress");
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
		return "";
	}


	/*******
	 * <p> Method: void removeInvitationAfterUse(String code) </p>
	 *
	 * <p> Description: Remove an invitation record once it is used.</p>
	 *
	 * @param code is the 6 character String invitation code
	 *
	 */
	// Remove an invitation using an email address once the user account has been setup
	public void removeInvitationAfterUse(String code) {
	    String query = "SELECT COUNT(*) AS count FROM InvitationCodes WHERE code = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, code);
	        ResultSet rs = pstmt.executeQuery();
	        if (rs.next()) {
	        	int counter = rs.getInt(1);
	            // Only do the remove if the code is still in the invitation table
	        	if (counter > 0) {
        			query = "DELETE FROM InvitationCodes WHERE code = ?";
	        		try (PreparedStatement pstmt2 = connection.prepareStatement(query)) {
	        			pstmt2.setString(1, code);
	        			pstmt2.executeUpdate();
	        		}catch (SQLException e) {
	        	        e.printStackTrace();
	        	    }
	        	}
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
		return;
	}

	/*******
	 * <p> Method: boolean deleteUser(String username) </p>
	 *
	 * <p> Description: Delete a user from the database.</p>
	 *
	 * @param username of the user to be deleted
	 *
	 */
	// Delete user from database using their username
	public void deleteUser(String username) {
		String query = "DELETE FROM userDB WHERE userName = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, username);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}


	/*******
	 * <p> Method: String getFirstName(String username) </p>
	 *
	 * <p> Description: Get the first name of a user given that user's username.</p>
	 *
	 * @param username is the username of the user
	 *
	 * @return the first name of a user given that user's username
	 *
	 */
	// Get the First Name
	public String getFirstName(String username) {
		String query = "SELECT firstName FROM userDB WHERE userName = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, username);
	        ResultSet rs = pstmt.executeQuery();

	        if (rs.next()) {
	            return rs.getString("firstName"); // Return the first name if user exists
	        }

	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
		return null;
	}


	/*******
	 * <p> Method: void updateFirstName(String username, String firstName) </p>
	 *
	 * <p> Description: Update the first name of a user given that user's username and the new
	 *		first name.</p>
	 *
	 * @param username is the username of the user
	 *
	 * @param firstName is the new first name for the user
	 *
	 */
	// update the first name
	public void updateFirstName(String username, String firstName) {
	    String query = "UPDATE userDB SET firstName = ? WHERE username = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, firstName);
	        pstmt.setString(2, username);
	        pstmt.executeUpdate();
	        currentFirstName = firstName;
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}


	/*******
	 * <p> Method: String getMiddleName(String username) </p>
	 *
	 * <p> Description: Get the middle name of a user given that user's username.</p>
	 *
	 * @param username is the username of the user
	 *
	 * @return the middle name of a user given that user's username
	 *
	 */
	// get the middle name
	public String getMiddleName(String username) {
		String query = "SELECT MiddleName FROM userDB WHERE userName = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, username);
	        ResultSet rs = pstmt.executeQuery();

	        if (rs.next()) {
	            return rs.getString("middleName"); // Return the middle name if user exists
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
		return null;
	}


	/*******
	 * <p> Method: void updateMiddleName(String username, String middleName) </p>
	 *
	 * <p> Description: Update the middle name of a user given that user's username and the new
	 * 		middle name.</p>
	 *
	 * @param username is the username of the user
	 *
	 * @param middleName is the new middle name for the user
	 *
	 */
	// update the middle name
	public void updateMiddleName(String username, String middleName) {
	    String query = "UPDATE userDB SET middleName = ? WHERE username = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, middleName);
	        pstmt.setString(2, username);
	        pstmt.executeUpdate();
	        currentMiddleName = middleName;
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}


	/*******
	 * <p> Method: String getLastName(String username) </p>
	 *
	 * <p> Description: Get the last name of a user given that user's username.</p>
	 *
	 * @param username is the username of the user
	 *
	 * @return the last name of a user given that user's username
	 *
	 */
	// get he last name
	public String getLastName(String username) {
		String query = "SELECT LastName FROM userDB WHERE userName = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, username);
	        ResultSet rs = pstmt.executeQuery();

	        if (rs.next()) {
	            return rs.getString("lastName"); // Return last name role if user exists
	        }
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
		return null;
	}


	/*******
	 * <p> Method: void updateLastName(String username, String lastName) </p>
	 *
	 * <p> Description: Update the middle name of a user given that user's username and the new
	 * 		middle name.</p>
	 *
	 * @param username is the username of the user
	 *
	 * @param lastName is the new last name for the user
	 *
	 */
	// update the last name
	public void updateLastName(String username, String lastName) {
	    String query = "UPDATE userDB SET lastName = ? WHERE username = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, lastName);
	        pstmt.setString(2, username);
	        pstmt.executeUpdate();
	        currentLastName = lastName;
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}


	/*******
	 * <p> Method: String getPreferredFirstName(String username) </p>
	 *
	 * <p> Description: Get the preferred first name of a user given that user's username.</p>
	 *
	 * @param username is the username of the user
	 *
	 * @return the preferred first name of a user given that user's username
	 *
	 */
	// get the preferred first name
	public String getPreferredFirstName(String username) {
		String query = "SELECT preferredFirstName FROM userDB WHERE userName = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, username);
	        ResultSet rs = pstmt.executeQuery();

	        if (rs.next()) {
	            return rs.getString("firstName"); // Return the preferred first name if user exists
	        }

	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
		return null;
	}


	/*******
	 * <p> Method: void updatePreferredFirstName(String username, String preferredFirstName) </p>
	 *
	 * <p> Description: Update the preferred first name of a user given that user's username and
	 * 		the new preferred first name.</p>
	 *
	 * @param username is the username of the user
	 *
	 * @param preferredFirstName is the new preferred first name for the user
	 *
	 */
	// update the preferred first name of the user
	public void updatePreferredFirstName(String username, String preferredFirstName) {
	    String query = "UPDATE userDB SET preferredFirstName = ? WHERE username = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, preferredFirstName);
	        pstmt.setString(2, username);
	        pstmt.executeUpdate();
	        currentPreferredFirstName = preferredFirstName;
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}


	/*******
	 * <p> Method: String getEmailAddress(String username) </p>
	 *
	 * <p> Description: Get the email address of a user given that user's username.</p>
	 *
	 * @param username is the username of the user
	 *
	 * @return the email address of a user given that user's username
	 *
	 */
	// get the email address
	public String getEmailAddress(String username) {
		String query = "SELECT emailAddress FROM userDB WHERE userName = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, username);
	        ResultSet rs = pstmt.executeQuery();

	        if (rs.next()) {
	            return rs.getString("emailAddress"); // Return the email address if user exists
	        }

	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
		return null;
	}


	/*******
	 * <p> Method: void updateEmailAddress(String username, String emailAddress) </p>
	 *
	 * <p> Description: Update the email address name of a user given that user's username and
	 * 		the new email address.</p>
	 *
	 * @param username is the username of the user
	 *
	 * @param emailAddress is the new preferred first name for the user
	 *
	 */
	// update the email address
	public void updateEmailAddress(String username, String emailAddress) {
	    String query = "UPDATE userDB SET emailAddress = ? WHERE username = ?";
	    try (PreparedStatement pstmt = connection.prepareStatement(query)) {
	        pstmt.setString(1, emailAddress);
	        pstmt.setString(2, username);
	        pstmt.executeUpdate();
	        currentEmailAddress = emailAddress;
	    } catch (SQLException e) {
	        e.printStackTrace();
	    }
	}


	/*******
	 * <p> Method: boolean getUserAccountDetails(String username) </p>
	 *
	 * <p> Description: Get all the attributes of a user given that user's username.</p>
	 *
	 * @param username is the username of the user
	 *
	 * @return true of the get is successful, else false
	 *
	 */
	// get the attributes for a specified user
	public boolean getUserAccountDetails(String username) {
		String query = "SELECT * FROM userDB WHERE username = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, username);
	        ResultSet rs = pstmt.executeQuery();
			rs.next();
	    	currentUsername = rs.getString(2);
	    	currentPassword = rs.getString(3);
	    	currentFirstName = rs.getString(4);
	    	currentMiddleName = rs.getString(5);
	    	currentLastName = rs.getString(6);
	    	currentPreferredFirstName = rs.getString(7);
	    	currentEmailAddress = rs.getString(8);
	    	currentAdminRole = rs.getBoolean(9);
	    	currentNewRole1 = rs.getBoolean(10);
	    	currentNewRole2 = rs.getBoolean(11);
			return true;
	    } catch (SQLException e) {
			return false;
	    }
	}


	/*******
	 * <p> Method: boolean updateUserRole(String username, String role, String value) </p>
	 *
	 * <p> Description: Update a specified role for a specified user's and set and update all the
	 * 		current user attributes.  This is how an Admin gives or removes the Contributor role
	 * 		("Role1" or "Contributor") and, in TP3, the Curator role ("Role2" or "Curator";
	 * 		US-CR1).  A role name the system does not have changes nothing.</p>
	 *
	 * <p> For the Curator role, tested by curatorHomeTests.CuratorHomeTests CH-08 to CH-11 and
	 * 		CH-15 (JUnit). </p>
	 *
	 * @param username is the username of the user
	 *
	 * @param role is string that specifies the role to update: "Admin", "Role1" or
	 * 		"Contributor", "Role2" or "Curator"
	 *
	 * @param value is the string that specified TRUE or FALSE for the role
	 *
	 * @return true if the update was successful, else false
	 *
	 */
	// Update a users role
	public boolean updateUserRole(String username, String role, String value) {
		if (role.compareTo("Admin") == 0) {
			String query = "UPDATE userDB SET adminRole = ? WHERE username = ?";
			try (PreparedStatement pstmt = connection.prepareStatement(query)) {
				pstmt.setString(1, value);
				pstmt.setString(2, username);
				pstmt.executeUpdate();
				if (value.compareTo("true") == 0)
					currentAdminRole = true;
				else
					currentAdminRole = false;
				return true;
			} catch (SQLException e) {
				return false;
			}
		}
		if (role.compareTo("Role1") == 0 || role.compareTo("Contributor") == 0) {
			String query = "UPDATE userDB SET newRole1 = ? WHERE username = ?";
			try (PreparedStatement pstmt = connection.prepareStatement(query)) {
				pstmt.setString(1, value);
				pstmt.setString(2, username);
				pstmt.executeUpdate();
				if (value.compareTo("true") == 0)
					currentNewRole1 = true;
				else
					currentNewRole1 = false;
				return true;
			} catch (SQLException e) {
				return false;
			}
		}
		// TP3 Aspect 1: Role2 is the Curator role.  The pages pass the stored name "Role2";
		// "Curator" is accepted as well, the same way "Contributor" is accepted for Role1 above.
		if (role.compareTo("Role2") == 0 || role.compareTo("Curator") == 0) {
			String query = "UPDATE userDB SET newRole2 = ? WHERE username = ?";
			try (PreparedStatement pstmt = connection.prepareStatement(query)) {
				pstmt.setString(1, value);
				pstmt.setString(2, username);
				pstmt.executeUpdate();
				if (value.compareTo("true") == 0)
					currentNewRole2 = true;
				else
					currentNewRole2 = false;
				return true;
			} catch (SQLException e) {
				return false;
			}
		}
		return false;
	}

	/*******
	 * <p> Method: void updatePassword(String username, String newPassword) </p>
	 *
	 * <p> Description: Update the password a user. </p>
	 *
	 * @param username		the user whose password changes
	 * @param newPassword	the new password
	 */
	public void updatePassword(String username, String newPassword) {
		String query = "UPDATE userDB SET password = ? WHERE userName = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, newPassword);
			pstmt.setString(2, username);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	// ------------------------------------------------------------------------------------------
	// Lessons learned.  Everything from here down to the "Attribute getters for the current user"
	// comment holds the Create, Read, Update, and Delete methods for lessons learned, their
	// experience entries, and the members' shares of effort (added in HW2 and extended in TP2).
	// Everything above this point, apart from the table creation and migration, is the
	// Foundations and TP1 code.
	//
	// These methods follow the conventions the rest of this class already uses: a
	// PreparedStatement in a try-with-resources block, and "catch the SQLException and hand back
	// a safe default" for reads and updates.  The one exception is createLesson(), which throws
	// its SQLException just as register() does, because both are the Create half of a CRUD pair
	// and the caller should decide what a failed save means.
	//
	// These methods do not check ownership, locks, or confirmations; the entity classes and the
	// GUI Models do that before calling them.  Keeping SQL here and rules elsewhere is the
	// layering the Foundations code established.
	// ------------------------------------------------------------------------------------------

	/*******
	 * <p> Method: int createLesson(LessonsLearned lesson) throws SQLException </p>
	 *
	 * <p> Description: CREATE.  Inserts one row into lessonsLearnedDB and returns the id the
	 * database gave it.  It is modeled after register(User) and throws SQLException for the same
	 * reason register() does.  Unlike register(), it asks for the generated key back
	 * (Statement.RETURN_GENERATED_KEYS), because the new lesson's id is needed at once so the
	 * screen can select it, while a User row is looked up by username and never needs its id.
	 * Every column is written, including the TP2 attributes, so the stored row always matches the
	 * object exactly.</p>
	 *
	 * @param lesson the (already validated) lesson to save
	 *
	 * @return the auto-generated id of the new row, or -1 if no key came back
	 *
	 * @throws SQLException when the insert cannot be built or executed
	 */
	public int createLesson(LessonsLearned lesson) throws SQLException {
		String insertLesson = "INSERT INTO lessonsLearnedDB (ownerUserName, title, description, "
				+ "tags, category, lockedTitle, lockedDescription, lockedTags, lockedCategory, "
				+ "lockedEntire, createdAt, updatedAt, rootCause, recommendation, aiAssisted, "
				+ "searchTerms, curatedBy, curatedAt, originalTitle, originalDescription, "
				+ "summaryId, statsVisibleToContributor) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement pstmt = connection.prepareStatement(insertLesson,
				Statement.RETURN_GENERATED_KEYS)) {
			pstmt.setString(1, lesson.getOwnerUserName());
			pstmt.setString(2, lesson.getTitle());
			pstmt.setString(3, lesson.getDescription());
			pstmt.setString(4, lesson.getTags());
			pstmt.setString(5, lesson.getCategory());
			pstmt.setBoolean(6, lesson.isLockedTitle());
			pstmt.setBoolean(7, lesson.isLockedDescription());
			pstmt.setBoolean(8, lesson.isLockedTags());
			pstmt.setBoolean(9, lesson.isLockedCategory());
			pstmt.setBoolean(10, lesson.isLockedEntire());
			pstmt.setLong(11, lesson.getCreatedAt());
			pstmt.setLong(12, lesson.getUpdatedAt());
			pstmt.setString(13, lesson.getRootCause());
			pstmt.setString(14, lesson.getRecommendation());
			pstmt.setBoolean(15, lesson.isAiAssisted());
			pstmt.setString(16, lesson.getSearchTerms());
			pstmt.setString(17, lesson.getCuratedBy());
			pstmt.setLong(18, lesson.getCuratedAt());
			pstmt.setString(19, lesson.getOriginalTitle());
			pstmt.setString(20, lesson.getOriginalDescription());
			pstmt.setInt(21, lesson.getSummaryId());
			pstmt.setBoolean(22, lesson.isStatsVisibleToContributor());
			pstmt.executeUpdate();
			try (ResultSet keys = pstmt.getGeneratedKeys()) {
				if (keys.next()) return keys.getInt(1);
			}
		}
		return -1;	// should not happen with an AUTO_INCREMENT column, but -1 makes it obvious
	}


	/*******
	 * <p> Method: LessonsLearned getLessonById(int lessonId) </p>
	 *
	 * <p> Description: READ (one).  Loads a single lesson.  Like getFirstName() and the other
	 * single-value reads, a database problem is caught and reported as null, which is safe
	 * because a null is treated exactly like a missing row ("Lesson not found.").</p>
	 *
	 * @param lessonId the id of the lesson to load
	 *
	 * @return the lesson, or null if there is no such row (or the query failed)
	 */
	public LessonsLearned getLessonById(int lessonId) {
		String query = "SELECT * FROM lessonsLearnedDB WHERE id = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setInt(1, lessonId);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) return buildLessonFromRow(rs);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}


	/*******
	 * <p> Method: LessonsLearnedList getAllLessons() </p>
	 *
	 * <p> Description: TP2 READ (list).  Loads every lesson in the system, newest first.  This
	 * backs LessonsLearnedList.loadAll(), the persistent list of all lessons.  No Contributor
	 * screen uses it, because a Contributor may see only their own lessons (US-R4).</p>
	 *
	 * @return every lesson; empty (never null) if there are none
	 */
	public LessonsLearnedList getAllLessons() {
		List<LessonsLearned> lessons = new ArrayList<LessonsLearned>();
		String query = "SELECT * FROM lessonsLearnedDB ORDER BY createdAt DESC, id DESC";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) lessons.add(buildLessonFromRow(rs));
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return new LessonsLearnedList(lessons);
	}


	/*******
	 * <p> Method: LessonsLearnedList getLessonsByOwner(String ownerUserName) </p>
	 *
	 * <p> Description: READ (list).  Loads every lesson one user owns, newest first.  Because of
	 * the WHERE clause, a Contributor's query cannot return another user's rows, which enforces
	 * the privacy story (US-R4) at the database level.</p>
	 *
	 * @param ownerUserName the user whose lessons should be loaded
	 *
	 * @return that user's lessons; empty (never null) if there are none
	 */
	public LessonsLearnedList getLessonsByOwner(String ownerUserName) {
		List<LessonsLearned> lessons = new ArrayList<LessonsLearned>();
		String query = "SELECT * FROM lessonsLearnedDB WHERE ownerUserName = ? "
				+ "ORDER BY createdAt DESC, id DESC";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, ownerUserName);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) lessons.add(buildLessonFromRow(rs));
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return new LessonsLearnedList(lessons);
	}


	/*******
	 * <p> Method: LessonsLearned buildLessonFromRow(ResultSet rs) throws SQLException </p>
	 *
	 * <p> Description: Private helper that turns the current row of a lessonsLearnedDB result
	 * set into a LessonsLearned.  Every query that returns lessons needs this exact mapping, so
	 * it is written once.  The identity, core information, locks, and timestamps go through the
	 * constructor; the TP2 attributes are set afterwards.</p>
	 *
	 * @param rs a result set already positioned on a lessonsLearnedDB row
	 *
	 * @return the LessonsLearned for that row
	 *
	 * @throws SQLException if a column cannot be read
	 */
	private LessonsLearned buildLessonFromRow(ResultSet rs) throws SQLException {
		LessonsLearned lesson = new LessonsLearned(rs.getInt("id"), rs.getString("ownerUserName"),
				rs.getString("title"), rs.getString("description"), rs.getString("tags"),
				rs.getString("category"), rs.getBoolean("lockedTitle"),
				rs.getBoolean("lockedDescription"), rs.getBoolean("lockedTags"),
				rs.getBoolean("lockedCategory"), rs.getBoolean("lockedEntire"),
				rs.getLong("createdAt"), rs.getLong("updatedAt"));
		lesson.setRootCause(rs.getString("rootCause"));
		lesson.setRecommendation(rs.getString("recommendation"));
		lesson.setAiAssisted(rs.getBoolean("aiAssisted"));
		lesson.setSearchTerms(rs.getString("searchTerms"));
		lesson.setCuratedBy(rs.getString("curatedBy"));
		lesson.setCuratedAt(rs.getLong("curatedAt"));
		lesson.setOriginalTitle(rs.getString("originalTitle"));
		lesson.setOriginalDescription(rs.getString("originalDescription"));
		lesson.setSummaryId(rs.getInt("summaryId"));
		lesson.setStatsVisibleToContributor(rs.getBoolean("statsVisibleToContributor"));
		return lesson;
	}


	/*******
	 * <p> Method: boolean updateLesson(LessonsLearned lesson) </p>
	 *
	 * <p> Description: UPDATE.  Writes every field a Contributor may change (the four core
	 * fields, root cause, recommendation, and the AI-assisted flag) in ONE UPDATE statement, so
	 * a lesson can never end up half-updated (the "commit all changes or none" rule in
	 * InputValidation.pdf).  The lock flags and the Curator fields are intentionally not written
	 * here, so a Contributor's save can never change them.</p>
	 *
	 * @param lesson the lesson to save; its id selects the row
	 *
	 * @return true if a row was updated, false if no row matched or something went wrong
	 */
	public boolean updateLesson(LessonsLearned lesson) {
		String query = "UPDATE lessonsLearnedDB SET title = ?, description = ?, tags = ?, "
				+ "category = ?, updatedAt = ?, rootCause = ?, recommendation = ?, "
				+ "aiAssisted = ? WHERE id = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, lesson.getTitle());
			pstmt.setString(2, lesson.getDescription());
			pstmt.setString(3, lesson.getTags());
			pstmt.setString(4, lesson.getCategory());
			pstmt.setLong(5, lesson.getUpdatedAt());
			pstmt.setString(6, lesson.getRootCause());
			pstmt.setString(7, lesson.getRecommendation());
			pstmt.setBoolean(8, lesson.isAiAssisted());
			pstmt.setInt(9, lesson.getId());
			return pstmt.executeUpdate() > 0;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}


	/*******
	 * <p> Method: boolean updateLessonCuratorFields(LessonsLearned lesson) </p>
	 *
	 * <p> Description: TP2 UPDATE.  Writes the Curator-only attributes (Phase Three Epics E2 to
	 * E5) in one statement: search terms, curated-by and curated-at, original title and
	 * description, the statistics summary id, and its visibility.  It is separate from
	 * updateLesson() so that the two roles' saves can never overwrite each other's fields.</p>
	 *
	 * @param lesson the lesson to save; its id selects the row
	 *
	 * @return true if a row was updated, false if no row matched or something went wrong
	 */
	public boolean updateLessonCuratorFields(LessonsLearned lesson) {
		String query = "UPDATE lessonsLearnedDB SET searchTerms = ?, curatedBy = ?, "
				+ "curatedAt = ?, originalTitle = ?, originalDescription = ?, summaryId = ?, "
				+ "statsVisibleToContributor = ? WHERE id = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, lesson.getSearchTerms());
			pstmt.setString(2, lesson.getCuratedBy());
			pstmt.setLong(3, lesson.getCuratedAt());
			pstmt.setString(4, lesson.getOriginalTitle());
			pstmt.setString(5, lesson.getOriginalDescription());
			pstmt.setInt(6, lesson.getSummaryId());
			pstmt.setBoolean(7, lesson.isStatsVisibleToContributor());
			pstmt.setInt(8, lesson.getId());
			return pstmt.executeUpdate() > 0;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}


	/**
	 * Reads the current stored Curator role without changing current-user attributes.
	 * E3 checks this before an AI request/save, including after role revocation.
	 * Tested by CuratorRefinementTests RF-02, RF-28/29, and RF-34.
	 * @param username authenticated session username
	 * @return true only for an existing account with the Curator flag
	 */
	public boolean hasCuratorRole(String username) {
		if (username == null || username.isBlank()) return false;
		try (PreparedStatement pstmt = connection.prepareStatement(
				"SELECT newRole2 FROM userDB WHERE userName = ?")) {
			pstmt.setString(1, username);
			try (ResultSet rs = pstmt.executeQuery()) {
				return rs.next() && rs.getBoolean(1);
			}
		} catch (SQLException e) {
			return false;
		}
	}

	/**
	 * Atomically stores E3 text, first originals, attribution, and the updated timestamp.
	 * The Model validates text first. One prepared UPDATE rechecks the stored Curator
	 * role and the review's revision, closing the read/check/write gap. It updates no
	 * locks, ownership, E2 search terms, E4/E5 summaries, or Contributor coaching fields.
	 * Tested by CuratorRefinementTests RF-09/10/13/14/16/17/28/30/31/32/33/36/38.
	 * @param expected review snapshot
	 * @param curator authenticated Curator username
	 * @param title validated final title
	 * @param description validated final description
	 * @param when positive epoch-millisecond save time
	 * @return true for exactly one saved lesson; false on stale review, role loss, or SQL failure
	 */
	public boolean saveCuratorRefinement(RefinementDraft expected, String curator,
			String title, String description, long when) {
		if (expected == null || curator == null || when <= 0) return false;
		String query = "UPDATE lessonsLearnedDB SET "
				+ "originalTitle = CASE WHEN originalTitle = '' THEN title ELSE originalTitle END, "
				+ "originalDescription = CASE WHEN originalDescription = '' THEN description ELSE originalDescription END, "
				+ "title = ?, description = ?, curatedBy = ?, curatedAt = ?, updatedAt = ? "
				+ "WHERE id = ? AND title = ? AND description = ? AND updatedAt = ? "
				+ "AND curatedAt = ? AND curatedBy = ? "
				+ "AND EXISTS (SELECT 1 FROM userDB WHERE userName = ? AND newRole2 = TRUE)";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setString(1, title);
			pstmt.setString(2, description);
			pstmt.setString(3, curator);
			pstmt.setLong(4, when);
			pstmt.setLong(5, when);
			pstmt.setInt(6, expected.lessonId());
			pstmt.setString(7, expected.title());
			pstmt.setString(8, expected.description());
			pstmt.setLong(9, expected.updatedAt());
			pstmt.setLong(10, expected.curatedAt());
			pstmt.setString(11, expected.curatedBy());
			pstmt.setString(12, curator);
			return pstmt.executeUpdate() == 1;
		} catch (SQLException e) {
			return false;
		}
	}


	/*******
	 * <p> Method: boolean setLessonLock(int lessonId, String field, boolean locked) </p>
	 *
	 * <p> Description: Sets or clears one lock flag (Epic E6).  The Lessons Learned System V1
	 * document schedules the Curator role for Phase Three, so no Contributor action calls this.
	 * Until the Curator page exists it is used, through LessonsLearned.setLock(), by the tests
	 * and the demonstration data to create the locked lessons that the locked-field and
	 * locked-delete test cases need.</p>
	 *
	 * @param lessonId the lesson to change
	 *
	 * @param field "title", "description", "tags", "category", or "entire"
	 *
	 * @param locked true to lock, false to unlock
	 *
	 * @return true if a row was updated, false otherwise (including an unknown field name)
	 */
	public boolean setLessonLock(int lessonId, String field, boolean locked) {
		// HW3 Task 5 refactor: the name-to-column switch moved to lockColumn() so this method and
		// setLessonLocks() share one fixed list of lock columns and can never disagree
		String column = lockColumn(field);
		if (column == null) return false;
		// JDBC can only parameterize values, not column names, so the column name is placed in
		// the query text.  That is safe here because it only ever comes from lockColumn().
		String query = "UPDATE lessonsLearnedDB SET " + column + " = ? WHERE id = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setBoolean(1, locked);
			pstmt.setInt(2, lessonId);
			return pstmt.executeUpdate() > 0;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}

	/*******
	 * <p> Method: boolean setLessonLocks(int lessonId, Collection&lt;String&gt; fields,
	 * 		boolean locked) </p>
	 *
	 * <p> Description: HW3 Task 5 prototype for TP3 Aspect 2 (Epic E6).  Sets or clears several
	 * lock flags of one lesson in ONE UPDATE statement, so the flags a Curator chose are saved
	 * together or not at all (Epic E6, action 4: "System saves all lock flags in one write";
	 * ConOps objective OB-7).  setLessonLock() above changes one flag per call, so a request for
	 * several flags could be left half-saved if a later call failed; ModelCuratorLock uses this
	 * method instead.</p>
	 *
	 * <p> Every name is checked against the five known field names BEFORE the statement is built.
	 * An unknown name, an empty or null list, or a lesson that does not exist changes nothing
	 * and returns false (requirement TR-7).  A repeated name is used once.</p>
	 *
	 * @param lessonId the lesson to change
	 *
	 * @param fields any of "title", "description", "tags", "category", and "entire" (the
	 * 		LessonsLearned FIELD_ and LOCK_ENTIRE constants)
	 *
	 * @param locked true to lock, false to unlock
	 *
	 * @return true if the lesson's row was updated; false if nothing was changed
	 */
	public boolean setLessonLocks(int lessonId, Collection<String> fields, boolean locked) {
		if (fields == null || fields.isEmpty()) return false;

		// Turn every name into its column before building anything.  One unknown name stops
		// the whole request, so a valid name in the same list is never saved on its own.
		// A LinkedHashSet drops repeated names, because H2 rejects a column listed twice in SET.
		Set<String> columns = new LinkedHashSet<String>();
		for (String field : fields) {
			String column = lockColumn(field);
			if (column == null) return false;
			columns.add(column);
		}

		// JDBC can only parameterize values, not column names, so the column names are placed
		// in the query text.  That is safe because every one came from lockColumn()'s fixed
		// list, never from the caller's text (Top Defects R2, CWE-89).
		StringBuilder query = new StringBuilder("UPDATE lessonsLearnedDB SET ");
		query.append(String.join(" = ?, ", columns)).append(" = ? WHERE id = ?");
		try (PreparedStatement pstmt = connection.prepareStatement(query.toString())) {
			int index = 1;
			for (int i = 0; i < columns.size(); i++) pstmt.setBoolean(index++, locked);
			pstmt.setInt(index, lessonId);
			// No matching row (for example, a deleted lesson) means nothing was saved
			return pstmt.executeUpdate() > 0;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}


	/*******
	 * <p> Method: String lockColumn(String field) </p>
	 *
	 * <p> Description: Returns the lessonsLearnedDB column that holds the lock flag for a field
	 * name, or null if the name is not one of the five lockable names.  This fixed list is the
	 * only source of column names in the lock UPDATE statements. </p>
	 *
	 * @param field "title", "description", "tags", "category", or "entire"
	 *
	 * @return the column name, or null for any other value, including null
	 */
	private static String lockColumn(String field) {
		// A switch on a null String throws an exception, so null is handled first
		if (field == null) return null;
		switch (field) {
			case "title": return "lockedTitle";
			case "description": return "lockedDescription";
			case "tags": return "lockedTags";
			case "category": return "lockedCategory";
			case "entire": return "lockedEntire";
			default: return null;
		}
	}


	/*******
	 * <p> Method: boolean deleteLesson(int lessonId) </p>
	 *
	 * <p> Description: DELETE.  Removes one lesson row.  Because of the ON DELETE CASCADE rules
	 * on experienceDB and experienceMemberEffortDB, this one statement also removes the lesson's
	 * experience entries and their members' shares.  Checking ownership, locks, and the "Are you
	 * sure?" answer happens before this is called.</p>
	 *
	 * @param lessonId the lesson to delete
	 *
	 * @return true if a row was deleted, false otherwise
	 */
	public boolean deleteLesson(int lessonId) {
		String query = "DELETE FROM lessonsLearnedDB WHERE id = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setInt(1, lessonId);
			return pstmt.executeUpdate() > 0;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}


	/*******
	 * <p> Method: int createExperience(ExperienceEntry entry) </p>
	 *
	 * <p> Description: CREATE (experience).  Adds one experience row to a lesson together with
	 * one row per team member's share, all inside a single transaction.  If any insert fails,
	 * the whole transaction is rolled back, so an entry is never stored without its member
	 * shares or with only some of them ("commit all changes or none," InputValidation.pdf).</p>
	 *
	 * <p> The connection is shared by the whole application and normally runs in auto-commit
	 * mode, so the previous mode is saved first and always restored in the finally block, even
	 * when the insert fails.</p>
	 *
	 * @param entry the (already validated) experience entry; its lessonId says where it goes
	 *
	 * @return the id the database gave the new entry, or -1 if nothing was saved
	 */
	public int createExperience(ExperienceEntry entry) {
		String insertEntry = "INSERT INTO experienceDB (lessonId, timeEffortHours, description, "
				+ "createdAt) VALUES (?, ?, ?, ?)";
		String insertShare = "INSERT INTO experienceMemberEffortDB (experienceId, memberName, "
				+ "hours) VALUES (?, ?, ?)";
		boolean previousAutoCommit = true;
		try {
			previousAutoCommit = connection.getAutoCommit();
			connection.setAutoCommit(false);

			int newId = -1;
			try (PreparedStatement pstmt = connection.prepareStatement(insertEntry,
					Statement.RETURN_GENERATED_KEYS)) {
				pstmt.setInt(1, entry.getLessonId());
				pstmt.setDouble(2, entry.getTimeEffortHours());
				pstmt.setString(3, entry.getDescription());
				pstmt.setLong(4, entry.getCreatedAt());
				pstmt.executeUpdate();
				try (ResultSet keys = pstmt.getGeneratedKeys()) {
					if (keys.next()) newId = keys.getInt(1);
				}
			}
			if (newId < 0) throw new SQLException("No id was generated for the experience entry.");

			if (!entry.getMemberEfforts().isEmpty()) {
				try (PreparedStatement pstmt = connection.prepareStatement(insertShare)) {
					for (MemberEffort share : entry.getMemberEfforts()) {
						share.setExperienceId(newId);
						pstmt.setInt(1, newId);
						pstmt.setString(2, share.getMemberName());
						pstmt.setDouble(3, share.getHours());
						pstmt.addBatch();
					}
					pstmt.executeBatch();
				}
			}

			connection.commit();
			return newId;
		} catch (SQLException e) {
			e.printStackTrace();
			try {
				connection.rollback();
			} catch (SQLException rollbackError) {
				rollbackError.printStackTrace();
			}
			return -1;
		} finally {
			try {
				connection.setAutoCommit(previousAutoCommit);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}


	/*******
	 * <p> Method: List&lt;ExperienceEntry&gt; getExperienceForLesson(int lessonId) </p>
	 *
	 * <p> Description: READ (experience).  Loads a lesson's experience entries, oldest first,
	 * each with its members' shares.  Unlike getUserList(), which returns null on a problem, this
	 * returns an empty list, so the caller never has to check for null.</p>
	 *
	 * @param lessonId the lesson whose experience should be loaded
	 *
	 * @return the entries, oldest first; empty if there are none
	 */
	public List<ExperienceEntry> getExperienceForLesson(int lessonId) {
		List<ExperienceEntry> entries = new ArrayList<ExperienceEntry>();
		String query = "SELECT * FROM experienceDB WHERE lessonId = ? ORDER BY createdAt ASC, id ASC";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setInt(1, lessonId);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				entries.add(new ExperienceEntry(rs.getInt("id"), rs.getInt("lessonId"),
						rs.getDouble("timeEffortHours"), rs.getString("description"),
						rs.getLong("createdAt")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// The shares are loaded after the entries' result set is closed, one query per entry
		for (ExperienceEntry entry : entries)
			entry.setMemberEfforts(getMemberEffortsForExperience(entry.getId()));
		return entries;
	}


	/*******
	 * <p> Method: List&lt;MemberEffort&gt; getMemberEffortsForExperience(int experienceId) </p>
	 *
	 * <p> Description: TP2 READ.  Loads the team members' shares for one experience entry, in
	 * the order they were entered.</p>
	 *
	 * @param experienceId the experience entry whose shares should be loaded
	 *
	 * @return the shares; empty (never null) if there are none
	 */
	private List<MemberEffort> getMemberEffortsForExperience(int experienceId) {
		List<MemberEffort> shares = new ArrayList<MemberEffort>();
		String query = "SELECT * FROM experienceMemberEffortDB WHERE experienceId = ? ORDER BY id ASC";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setInt(1, experienceId);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				shares.add(new MemberEffort(rs.getInt("id"), rs.getInt("experienceId"),
						rs.getString("memberName"), rs.getDouble("hours")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return shares;
	}


	/*******
	 * <p> Method: boolean doesLessonExist(int lessonId) </p>
	 *
	 * <p> Description: Modeled directly after doesUserExist().  True if a lesson with that id
	 * exists, without loading the whole row.</p>
	 *
	 * @param lessonId the id to look for
	 *
	 * @return true if the lesson exists, else false
	 */
	public boolean doesLessonExist(int lessonId) {
		String query = "SELECT COUNT(*) FROM lessonsLearnedDB WHERE id = ?";
		try (PreparedStatement pstmt = connection.prepareStatement(query)) {
			pstmt.setInt(1, lessonId);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) return rs.getInt(1) > 0;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}


	/*******
	 * <p> Method: int getNumberOfLessons() </p>
	 *
	 * <p> Description: Modeled directly after getNumberOfUsers().  The total number of lesson
	 * rows across every contributor.</p>
	 *
	 * @return the number of lessons in the database
	 */
	public int getNumberOfLessons() {
		String query = "SELECT COUNT(*) AS count FROM lessonsLearnedDB";
		try {
			ResultSet resultSet = statement.executeQuery(query);
			if (resultSet.next()) return resultSet.getInt("count");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	// Attribute getters for the current user
	/*******
	 * <p> Method: String getCurrentUsername() </p>
	 *
	 * <p> Description: Get the current user's username.</p>
	 *
	 * @return the username value is returned
	 *
	 */
	public String getCurrentUsername() { return currentUsername;};


	/*******
	 * <p> Method: String getCurrentPassword() </p>
	 *
	 * <p> Description: Get the current user's password.</p>
	 *
	 * @return the password value is returned
	 *
	 */
	public String getCurrentPassword() { return currentPassword;};


	/*******
	 * <p> Method: String getCurrentFirstName() </p>
	 *
	 * <p> Description: Get the current user's first name.</p>
	 *
	 * @return the first name value is returned
	 *
	 */
	public String getCurrentFirstName() { return currentFirstName;};


	/*******
	 * <p> Method: String getCurrentMiddleName() </p>
	 *
	 * <p> Description: Get the current user's middle name.</p>
	 *
	 * @return the middle name value is returned
	 *
	 */
	public String getCurrentMiddleName() { return currentMiddleName;};


	/*******
	 * <p> Method: String getCurrentLastName() </p>
	 *
	 * <p> Description: Get the current user's last name.</p>
	 *
	 * @return the last name value is returned
	 *
	 */
	public String getCurrentLastName() { return currentLastName;};


	/*******
	 * <p> Method: String getCurrentPreferredFirstName( </p>
	 *
	 * <p> Description: Get the current user's preferred first name.</p>
	 *
	 * @return the preferred first name value is returned
	 *
	 */
	public String getCurrentPreferredFirstName() { return currentPreferredFirstName;};


	/*******
	 * <p> Method: String getCurrentEmailAddress() </p>
	 *
	 * <p> Description: Get the current user's email address name.</p>
	 *
	 * @return the email address value is returned
	 *
	 */
	public String getCurrentEmailAddress() { return currentEmailAddress;};


	/*******
	 * <p> Method: boolean getCurrentAdminRole() </p>
	 *
	 * <p> Description: Get the current user's Admin role attribute.</p>
	 *
	 * @return true if this user plays an Admin role, else false
	 *
	 */
	public boolean getCurrentAdminRole() { return currentAdminRole;};


	/*******
	 * <p> Method: boolean getCurrentNewRole1() </p>
	 *
	 * <p> Description: Get the current user's Student role attribute.</p>
	 *
	 * @return true if this user plays a Student role, else false
	 *
	 */
	public boolean getCurrentNewRole1() { return currentNewRole1;};


	/*******
	 * <p> Method: boolean getCurrentNewRole2() </p>
	 *
	 * <p> Description: Get the current user's Reviewer role attribute.</p>
	 *
	 * @return true if this user plays a Reviewer role, else false
	 *
	 */
	public boolean getCurrentNewRole2() { return currentNewRole2;};


	/*******
	 * <p> Debugging method</p>
	 *
	 * <p> Description: Debugging method that dumps the database of the console.</p>
	 *
	 * @throws SQLException if there is an issues accessing the database.
	 *
	 */
	// Dumps the database.
	public void dump() throws SQLException {
		String query = "SELECT * FROM userDB";
		ResultSet resultSet = statement.executeQuery(query);
		ResultSetMetaData meta = resultSet.getMetaData();
		while (resultSet.next()) {
		for (int i = 0; i < meta.getColumnCount(); i++) {
		System.out.println(
		meta.getColumnLabel(i + 1) + ": " +
				resultSet.getString(i + 1));
		}
		System.out.println();
		}
		resultSet.close();
	}


	/*******
	 * <p> Method: void closeConnection()</p>
	 *
	 * <p> Description: Closes the database statement and connection.</p>
	 *
	 */
	// Closes the database statement and connection.
	public void closeConnection() {
		try{
			if(statement!=null) statement.close();
		} catch(SQLException se2) {
			se2.printStackTrace();
		}
		try {
			if(connection!=null) connection.close();
		} catch(SQLException se){
			se.printStackTrace();
		}
	}
}
