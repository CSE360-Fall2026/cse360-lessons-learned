package guiTools;

/*******
 * <p> Title: RoleLabels Class. </p>
 *
 * <p> Description: Translates between the role names the database stores and the role names the
 * user sees.  In TP2 the Foundations "Role1" is the Contributor role (design decision D12), so
 * every screen shows "Contributor", while the database, the invitation codes, and the account
 * set-up code keep using "Role1".  In TP3 the Foundations "Role2" becomes the Curator role in
 * exactly the same way (TP3 Aspect 1; Initial ConOps assumption A1): every screen shows
 * "Curator" and the stored name stays "Role2".</p>
 *
 * <p> The stored names are kept as "Role1" and "Role2" on purpose:</p>
 * <ul>
 * <li> the InvitationCodes table stores the role in a VARCHAR(10) column, and "Contributor" is 11
 * 		characters long, so it would not fit; </li>
 * <li> ControllerNewAccount.doCreateUser and Database.updateUserRole compare the stored value
 * 		with "Role1" and "Role2", and an unknown role makes doCreateUser end the program; </li>
 * <li> invitations already sent and databases already in use stay valid, so an account that
 * 		had the unused Role2 before TP3 is simply a Curator now. </li>
 * </ul>
 * <p> So the translation happens only at the edge of the screens: a role is turned into its
 * label just before it is shown (toLabel), and a label chosen by the user is turned back into
 * the stored name just before it is given to the database (toStoredName).</p>
 *
 * <p> Tested by curatorHomeTests.CuratorHomeTests, CH-01 to CH-04 and CH-10 (JUnit). </p>
 *
 * <p> Copyright: Lynn Robert Carter © 2026 </p>
 *
 * @author Emilia Davey
 *
 * @version 1.00		2026-10-16 TP2: Role1 is shown as "Contributor" on the admin pages
 * @version 1.01		2026-11-10 TP3 Aspect 1: Role2 is shown as "Curator"
 */
public class RoleLabels {

	/** The name the database and the Foundations code use for the Contributor role. */
	public static final String ROLE1_STORED_NAME = "Role1";

	/** The name every screen shows for that role (design decision D12). */
	public static final String ROLE1_LABEL = "Contributor";

	/** The name the database and the Foundations code use for the Curator role. */
	public static final String ROLE2_STORED_NAME = "Role2";

	/** The name every screen shows for that role (TP3 Aspect 1; ConOps assumption A1). */
	public static final String ROLE2_LABEL = "Curator";

	/**********
	 * Default constructor is not used; every member is static.
	 */
	public RoleLabels() {
	}

	/**********
	 * <p> Method: String toLabel(String storedName) </p>
	 *
	 * <p> Description: Returns the name to show on the screen for a stored role name.  "Role1"
	 * becomes "Contributor" and "Role2" becomes "Curator"; every other name ("Admin") is shown
	 * unchanged. </p>
	 *
	 * <p> Tested by CuratorHomeTests CH-01, CH-03, and CH-04. </p>
	 *
	 * @param storedName	the role name as the database stores it; may be null
	 * @return the role name to show the user
	 */
	public static String toLabel(String storedName) {
		// The constant is on the left of equals() so a null name is returned, not dereferenced
		if (ROLE1_STORED_NAME.equals(storedName)) return ROLE1_LABEL;
		if (ROLE2_STORED_NAME.equals(storedName)) return ROLE2_LABEL;
		return storedName;
	}

	/**********
	 * <p> Method: String toStoredName(String label) </p>
	 *
	 * <p> Description: Returns the stored role name for a name chosen on the screen.
	 * "Contributor" becomes "Role1" and "Curator" becomes "Role2"; every other name, including
	 * the "&lt;Select a role&gt;" prompt, is returned unchanged. </p>
	 *
	 * <p> Tested by CuratorHomeTests CH-02, CH-03, CH-04, and CH-10. </p>
	 *
	 * @param label	the role name as the user saw and chose it; may be null
	 * @return the role name the database expects
	 */
	public static String toStoredName(String label) {
		// An exact, case-sensitive match: the labels only ever come from this class's own
		// constants, so "curator" typed some other way is deliberately not a role (CH-04)
		if (ROLE1_LABEL.equals(label)) return ROLE1_STORED_NAME;
		if (ROLE2_LABEL.equals(label)) return ROLE2_STORED_NAME;
		return label;
	}
}
