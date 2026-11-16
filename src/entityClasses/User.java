package entityClasses;


/*******
 * <p> Title: User Class </p>
 *
 * <p> Description: This User class represents a user entity in the system.  It contains the user's
 *  details such as userName, password, and roles being played. </p>
 *
 * <p> Copyright: Lynn Robert Carter © 2025 </p>
 *
 * @author Lynn Robert Carter
 * @author David Nevarez
 *
 * @version 1.01 2026-11-15 TP3 Aspect 9: Added Viewer role support
 */

public class User {
	
	/*
	 * These are the private attributes for this entity object
	 */
    /** The account username. */
    private String userName;
    /** The account password. */
    private String password;
    /** The user's first name. */
    private String firstName;
    /** The user's middle name. */
    private String middleName;
    /** The user's last name. */
    private String lastName;
    /** The user's display name (preferred first name). */
    private String preferredFirstName;
    /** The user's email address. */
    private String emailAddress;
    /** Whether the user has the Admin role. */
    private boolean adminRole;
    /** Whether the user has Role1, which TP2 shows as the Contributor role (design decision D12). */
    private boolean role1;
    /** Whether the user has Role2, which TP3 shows as the Curator role (TP3 Aspect 1; the rule
     * that reads it is guiTools.CuratorAccess). */
    private boolean role2;
	/** Whether the user has Role3, which TP3 uses for the Viewer role (Aspect 9, US-V-01 through US-V-04). */
	private boolean role3;
    
    
    /*****
     * <p> Method: User() </p>
     * 
     * <p> Description: This default constructor is not used in this system. </p>
     */
    public User() {
    	
    }

    
    /*****
     * <p> Method: User(String userName, String password, boolean r1, boolean r2,
     * 		boolean r3, boolean r4, boolean r5) </p>
     * 
     * <p> Description: This constructor is used to establish user entity objects. </p>
     * 
     * @param userName specifies the account userName for this user
     * 
     * @param password specifies the account password for this user
     * 
     * @param fn specifies the first name for this user
     * 
     * @param mn specifies the middle name for this user
     * 
     * @param ln specifies the last name for this user
     * 
     * @param pfn specifies the display name (preferred first name) for this user
     * 
     * @param ea specifies the email address for this user
     * 
     * @param r1 specifies the the Admin attribute (TRUE or FALSE) for this user
     * 
     * @param r2 specifies the the Role1 attribute, the Contributor role in TP2 (TRUE or FALSE)
     * 
     * @param r3 specifies the the Role2 attribute, the Curator role in TP3 (TRUE or FALSE)
     * 
     */
    // Constructor to initialize a new User object with userName, password, and role.
    public User(String userName, String password, String fn, String mn, String ln, String pfn, 
    		String ea, boolean r1, boolean r2, boolean r3) {
        this.userName = userName;
        this.password = password;
        this.firstName = fn;
        this.middleName = mn;
        this.lastName = ln;
        this.preferredFirstName = pfn;
        this.emailAddress = ea;
        this.adminRole = r1;
        this.role1 = r2;
        this.role2 = r3;
    }

	/**
	 * Creates a user with Admin, Contributor, Curator, and Viewer roles.
	 *
	 * @param userName account username
	 * @param password account password
	 * @param fn       first name
	 * @param mn       middle name
	 * @param ln       last name
	 * @param pfn      preferred first name
	 * @param ea       email address
	 * @param r1       Admin role
	 * @param r2       Contributor role
	 * @param r3       Curator role
	 * @param r4       Viewer role
	 */
	public User(String userName, String password, String fn, String mn, String ln, String pfn, String ea, boolean r1,
			boolean r2, boolean r3, boolean r4) {

		this(userName, password, fn, mn, ln, pfn, ea, r1, r2, r3);
		this.role3 = r4;
	}

    
    /*****
     * <p> Method: void setAdminRole(boolean role) </p>
     * 
     * <p> Description: This setter defines the Admin role attribute. </p>
     * 
     * @param role is a boolean that specifies if this user in playing the Admin role.
     * 
     */
    // Sets the role of the Admin user.
    public void setAdminRole(boolean role) {
    	this.adminRole=role;
    }

    
    /*****
     * <p> Method: void setRole1User(boolean role) </p>
     * 
     * <p> Description: This setter defines the role1 attribute. </p>
     * 
     * @param role is a boolean that specifies if this user in playing role1.
     * 
     */
    // Sets the role1 user.
    public void setRole1User(boolean role) {
    	this.role1=role;
    }

    
    /*****
     * <p> Method: void setRole2User(boolean role) </p>
     * 
     * <p> Description: This setter defines the role2 attribute. </p>
     * 
     * @param role is a boolean that specifies if this user in playing role2.
     * 
     */
    // Sets the role2 user.
    public void setRole2User(boolean role) {
    	this.role2=role;
    }

	/**
	 * Sets the Viewer role.
	 *
	 * @param role true to grant Viewer access, false to remove it
	 */
	public void setRole3User(boolean role) {
		this.role3 = role;
	}

    
    /*****
     * <p> Method: String getUserName() </p>
     * 
     * <p> Description: This getter returns the UserName. </p>
     * 
     * @return a String of the UserName
     * 
     */
    // Gets the current value of the UserName.
    public String getUserName() { return userName; }

    
    /*****
     * <p> Method: String getPassword() </p>
     * 
     * <p> Description: This getter returns the Password. </p>
     * 
     * @return a String of the password
	 *
     */
    // Gets the current value of the Password.
    public String getPassword() { return password; }

    
    /*****
     * <p> Method: String getFirstName() </p>
     * 
     * <p> Description: This getter returns the FirstName. </p>
     * 
     * @return a String of the FirstName
	 *
     */
    // Gets the current value of the FirstName.
    public String getFirstName() { return firstName; }

    
    /*****
     * <p> Method: String getMiddleName() </p>
     * 
     * <p> Description: This getter returns the MiddleName. </p>
     * 
     * @return a String of the MiddleName
	 *
     */
    // Gets the current value of the Student role attribute.
    public String getMiddleName() { return middleName; }

    
    /*****
     * <p> Method: String getLasteName() </p>
     * 
     * <p> Description: This getter returns the LastName. </p>
     * 
     * @return a String of the LastName
	 *
     */
    // Gets the current value of the Student role attribute.
    public String getLastName() { return lastName; }

    
    /*****
     * <p> Method: String getPreferredFirstName() </p>
     * 
     * <p> Description: This getter returns the PreferredFirstName. </p>
     * 
     * @return a String of the PreferredFirstName
	 *
     */
    // Gets the current value of the Student role attribute.
    public String getPreferredFirstName() { return preferredFirstName; }

    
    /*****
     * <p> Method: String getEmailAddress() </p>
     * 
     * <p> Description: This getter returns the EmailAddress. </p>
     * 
     * @return a String of the EmailAddress
	 *
     */
    // Gets the current value of the Student role attribute.
    public String getEmailAddress() { return emailAddress; }

    /**
     * Sets the username.
     * @param s the new username
     */
    public void setUserName(String s) { userName = s; }
    /**
     * Sets the password.
     * @param s the new password
     */
    public void setPassword(String s) { password = s; }
    /**
     * Sets the first name.
     * @param s the new first name
     */
    public void setFirstName(String s) { firstName = s; }
    /**
     * Sets the middle name.
     * @param s the new middle name
     */
    public void setMiddleName(String s) { middleName = s; }
    /**
     * Sets the last name.
     * @param s the new last name
     */
    public void setLastName(String s) { lastName = s; }
    /**
     * Sets the display name (preferred first name).
     * @param s the new display name (preferred first name)
     */
    public void setPreferredFirstName(String s) { preferredFirstName = s; }
    /**
     * Sets the email address.
     * @param s the new email address
     */
    public void setEmailAddress(String s) { emailAddress = s; }

    
    /*****
     * <p> Method: String getAdminRole() </p>
     * 
     * <p> Description: This getter returns the value of the Admin role attribute. </p>
     * 
     * @return a String of "TRUE" or "FALSE" based on state of the attribute
	 *
     */
    // Gets the current value of the Admin role attribute.
    public boolean getAdminRole() { return adminRole; }

    
    /*****
     * <p> Method: String getRole1() </p>
     * 
     * <p> Description: This getter returns the value of the role1 attribute. </p>
     * 
     * @return a String of "TRUE" or "FALSE" based on state of the attribute
	 *
     */
    // Gets the current value of the role1 attribute.
	public boolean getNewRole1() { return role1; }

    
    /*****
     * <p> Method: String getRole2() </p>
     * 
     * <p> Description: This getter returns the value of the role2 attribute. </p>
     * 
     * @return a String of "TRUE" or "FALSE" based on state of the attribute
     *
     */
    // Gets the current value of the role2 attribute.
    public boolean getNewRole2() { return role2; }

	/**
	 * Returns whether the user has the Viewer role.
	 *
	 * @return true if the user has the Viewer role
	 */
	public boolean getNewRole3() {
		return role3;
	}

        
	/*****
	 * <p> Method: int getNumRoles() </p>
	 *
	 * <p> Description: This getter returns the number of roles this user plays
	 * (0 - 4), including the Viewer role introduced in TP3 Aspect 9. </p>
	 *
	 * @return the number of roles assigned to this user, from 0 to 4
	 */
	// Returns the total number of roles assigned to the user.
	public int getNumRoles() {
	    int numRoles = 0;
	    if (adminRole) numRoles++;
	    if (role1) numRoles++;
	    if (role2) numRoles++;
	    if (role3) numRoles++;
	    return numRoles;
	}
}
