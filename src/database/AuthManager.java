package database;

import org.mindrot.jbcrypt.BCrypt;

import app.AppUtils.DialogException;
import app.EmailSender;
import app.Localization;
import gui.GuiHelper;

import java.security.SecureRandom;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.time.Period;

public class AuthManager {

	// Limitations for registering a new field
	private static int minUserAge = 8;
	private static int maxUserAge = 100;
	private static int maxUserNameLength = 30;
	private static int minUserNameLength = 2;
	private static int maxCompanyNameLength = 100;
	private static int maxEmailLength = 100;
	private static int minContactNumberLength = 8;
	private static int maxContactNumberLength = 15;
	private static int minPasswordLength = 8;
	private static int maxPasswordLength = 50;
	private static int tcknLength = 11;
	private static int maxTaxNumberLength = 11;
	private static int minTaxNumberLength = 10;
	
	// Login Protocol
	public static boolean authenticate(String username, String rawPassword, String role) throws Exception {
        if(role.equals("GUEST")) return true;

        String idCol = role.equals("USER") ? "neighborid" : "companyid";
        String table = role.equals("USER") ? "neighbor" : "company";
        String userCol = role.equals("USER") ? "tckn" : "taxnumber";
        String passTable = role.equals("USER") ? "neighbor_passwords" : "company_passwords";
        String passIdCol = role.equals("USER") ? "nID" : "cID";

        String userId = null;
        String dbHash = null;

        try (Connection conn = DBConnection.connect()) {
            // Find the ID
            String sql1 = "SELECT " + idCol + " FROM " + table + " WHERE " + userCol + " = ?";
            try (PreparedStatement ps1 = conn.prepareStatement(sql1)) {
                ps1.setString(1, username);
                try (ResultSet rs1 = ps1.executeQuery()) {
                    if(rs1.next()) {
                        userId = rs1.getString(1);
                    } else {
                        throw new DialogException(Localization.get("nouserfound"));
                    }
                }
            }
            
            // Find the hashed password
            String sql2 = "SELECT passwordhash FROM " + passTable + " WHERE " + passIdCol + " = ?";
            try (PreparedStatement ps2 = conn.prepareStatement(sql2)) {
                ps2.setInt(1, Integer.parseInt(userId));
                try (ResultSet rs2 = ps2.executeQuery()) {
                    if(rs2.next()) {
                        dbHash = rs2.getString(1);
                    } else {
                        throw new DialogException(Localization.get("nopasswordfound"));
                    }
                }
            }
            
            // Check if the password is correct or not
            if(BCrypt.checkpw(rawPassword, dbHash)) {
            	return true;
            }
            else { 
            	throw new DialogException(Localization.get("nopasswordfound")); 
            }
            
        }
    }

    // Register the user
    public static boolean registerUser(String name, String mname, String surname, String tckn, String birth, 
    	String email, String phone, int addr, String gender, String rawPass) {
    	
    	// Format checks
		if (!isAValidDate(birth) || !isAValidAge(birth)) {
			return false;
		}
		if (!isAValidTCKN(tckn)) {
			GuiHelper.showMessage(Localization.get("invalidtrid")); 
			return false;
		}
		
		if (!isAValidName(name) || !isAValidName(surname)) {
			return false;
		}
		
		if (!isAValidEmail(email)) {
			return false;
		}
		
		if (!isAValidContactNumber(phone)) {
			return false;
		}
		
		if (!isPasswordStrong(rawPass)) {
			return false;
		}

    	try (Connection conn = DBConnection.connect()) {
    		conn.setAutoCommit(false); // Start a transaction to be able to revert applied changes on any error

    		// Uniqueness checks
    		if(!isUnique(conn, "neighbor", "tckn", tckn)) { 
    			GuiHelper.showMessage(Localization.get("alreadyregisteredtrid")); 
    			return false; 
    		}
    		
    		if(!isUnique(conn, "neighbor", "email", email)) { 
    			GuiHelper.showMessage(Localization.get("alreadyregisteredemail")); 
    			return false; 
    		}
    		
    		if(!isUnique(conn, "neighbor", "contactnumber", phone)) { 
    			GuiHelper.showMessage(Localization.get("alreadyregisteredphone")); 
    			return false; 
    		}
    		
    		try {
    			int newID = 0;
        		
        		// Add a new user (neighbor) to the neighbor table
        		String sqlUser = "INSERT INTO neighbor (fname, mname, lname, tckn, bdate, email, contactnumber, addressID, sex) " +
        				"VALUES (?, ?, ?, ?, CAST(? AS DATE), ?, ?, ?, ?) RETURNING neighborid";

        		try (PreparedStatement pstmtUser = conn.prepareStatement(sqlUser)) {
        			pstmtUser.setString(1, name);
            		pstmtUser.setString(2, mname.isEmpty() ? null : mname);
            		pstmtUser.setString(3, surname);
            		pstmtUser.setString(4, tckn);
            		pstmtUser.setString(5, birth);
            		pstmtUser.setString(6, email.isEmpty() ? null : email);
            		pstmtUser.setString(7, phone.isEmpty() ? null : clearNumber(phone));
            		pstmtUser.setInt(8, addr);
            		pstmtUser.setString(9, gender);
            		
            		try (ResultSet rs = pstmtUser.executeQuery()) {
            			if (rs.next()) {
                			newID = rs.getInt(1); // Get neighborid
                		} else {
                			throw new SQLException(Localization.get("errorusercreate"));
                		}
            		}
        		}

        		// Insert hashed password to the database
        		String salt = BCrypt.gensalt();
        		String hashed = BCrypt.hashpw(rawPass, salt);
        		String sqlPass = "INSERT INTO neighbor_passwords (nID, passwordhash) VALUES (?, ?)";
        		
        		try (PreparedStatement pstmtPass = conn.prepareStatement(sqlPass)) {
        			pstmtPass.setInt(1, newID);
            		pstmtPass.setString(2, hashed);
            		pstmtPass.executeUpdate();
        		}
        		
        		conn.commit(); // If there are no errors, commit all changes
        		return true;
    		}
    		catch (Exception e) {
    			conn.rollback(); 
                throw e;
    		}
    	} catch (Exception e) {
    		e.printStackTrace();
    		GuiHelper.showWarningMessage(e.getMessage());
    		return false;
    	}
    }

    // Register the company
    public static boolean registerCompany(String taxNumber, String name, String phone, String fax, int addr, String rawPass, List<Integer> selectedDisposalIDs) {
        // Format checks 
        if (!isAValidTaxNumber(taxNumber)) {
     		return false;
     	}
        
     	if (!isAValidCompanyName(name)) {
     		return false;
     	}
     	
     	if (!isAValidFaxNumber(fax)) {
     		return false;
     	}
     		
     	if (!isAValidContactNumber(phone)) {
     		return false;
     	}
     	
     	if (!isPasswordStrong(rawPass)) {
			return false;
		}
     	
     	if (selectedDisposalIDs == null || selectedDisposalIDs.isEmpty()) {
     		GuiHelper.showMessage(Localization.get("chooseatleastonedtype"));
            return false;
     	}
        
        try (Connection conn = DBConnection.connect()){
            conn.setAutoCommit(false); // Start a transaction to be able to revert applied changes on any error
            
            // Uniqueness checks
            if(!isUnique(conn, "company", "taxnumber", taxNumber)) { 
            	GuiHelper.showMessage(Localization.get("alreadyregisteredtaxno"));
            	return false; 
            }
            
            if(!isUnique(conn, "company", "cname", name)) { 
            	GuiHelper.showMessage(Localization.get("alreadyregisteredcname"));
            	return false; 
            }
            
            if(!isUnique(conn, "company", "contactnumber", phone)) { 
            	GuiHelper.showMessage(Localization.get("alreadyregisteredphone"));
            	return false; 
            }
            
            if(!isUnique(conn, "company", "faxnumber", fax)) { 
            	GuiHelper.showMessage(Localization.get("alreadyregisteredfax"));
            	return false; 
            }

            // Add a new company to the company table
            try {
                int newId = 0;

                // Add a new company to the company table
                String sqlComp = "INSERT INTO company (taxnumber, cname, contactnumber, faxnumber, addressID) VALUES (?, ?, ?, ?, ?) RETURNING companyid";

                try (PreparedStatement pstmtComp = conn.prepareStatement(sqlComp)) {
                    pstmtComp.setString(1, taxNumber);
                    pstmtComp.setString(2, name);
                    pstmtComp.setString(3, phone.isEmpty() ? null : clearNumber(phone));
                    pstmtComp.setString(4, fax.isEmpty() ? null : fax);
                    pstmtComp.setInt(5, addr);
                    
                    try (ResultSet rs = pstmtComp.executeQuery()) {
                        if (rs.next()) newId = rs.getInt(1);
                        else throw new DialogException(Localization.get("errorcompanycreate"));
                    }
                }

                // Insert disposal types company can recycle to company_disposal table
                String sqlRelation = "INSERT INTO company_disposal (cID, dID) VALUES (?, ?)";
                if (selectedDisposalIDs != null && !selectedDisposalIDs.isEmpty()) {
                    try (PreparedStatement pstmtRelation = conn.prepareStatement(sqlRelation)) {
                        for (int dID : selectedDisposalIDs) {
                            pstmtRelation.setInt(1, newId);
                            pstmtRelation.setInt(2, dID);
                            pstmtRelation.addBatch();
                        }
                        pstmtRelation.executeBatch();
                    }
                }

                // Hash, salt and insert the password
                String hashed = BCrypt.hashpw(rawPass, BCrypt.gensalt());
                String sqlPass = "INSERT INTO company_passwords (cID, passwordhash) VALUES (?, ?)";
                try (PreparedStatement pstmtPass = conn.prepareStatement(sqlPass)) {
                    pstmtPass.setInt(1, newId);
                    pstmtPass.setString(2, hashed);
                    pstmtPass.executeUpdate();
                }

                // If there are no errors, commit changes
                conn.commit();
                return true;

            } catch (Exception ex) {
                // Rollback if there is an error on insert
                conn.rollback();
                throw ex;
            }
        } catch (DialogException de) {
        	return false;
        } catch (Exception e) {
            e.printStackTrace();
            GuiHelper.showWarningMessage(e.getMessage());
            return false;
        }
    }
    
    // Change the password (both neighbor and company)  
    public static void changePassword(String role, int id, String oldPass, String newPass) throws Exception {
        String tableName;
        String idColumn;
        
        if ("USER".equals(role)) {
            tableName = "neighbor_passwords";
            idColumn = "nID";
        } else {
            tableName = "company_passwords";
            idColumn = "cID";
        }

        String sqlCheck = "SELECT passwordhash FROM " + tableName + " WHERE " + idColumn + " = ?";
        String sqlUpdate = "UPDATE " + tableName + " SET passwordhash = ? WHERE " + idColumn + " = ?";

        try (Connection conn = DBConnection.connect();
        	// Get current hash
            PreparedStatement psCheck = conn.prepareStatement(sqlCheck)) {
            psCheck.setInt(1, id);
            
            try (ResultSet rs = psCheck.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("passwordhash");
                    
                    // Check old password
                    if (!BCrypt.checkpw(oldPass, storedHash)) {
                        throw new DialogException(Localization.get("nopasswordfound")); 
                    }
                } else {
                    throw new DialogException(Localization.get("nouserfound"));
                }
            }
            
            // Hash and save the new password
            try (PreparedStatement psUpdate = conn.prepareStatement(sqlUpdate)) {
                String newHash = BCrypt.hashpw(newPass, BCrypt.gensalt());
                
                psUpdate.setString(1, newHash);
                psUpdate.setInt(2, id);
                
                psUpdate.executeUpdate();
            }
        }
    }
    
    public static boolean isPasswordStrong(String password) {
    	if (password.length() > maxPasswordLength || password.length() < minPasswordLength) {
    		GuiHelper.showMessage(Localization.get("lengthpass", minPasswordLength, maxPasswordLength));
    		return false;
    	}
        
        boolean hasUpper = !password.equals(password.toLowerCase());
        boolean hasLower = !password.equals(password.toUpperCase());
        boolean hasDigit = password.matches(".*\\d.*");
        
        if (!hasUpper || !hasLower || !hasDigit) {
            GuiHelper.showMessage(Localization.get("rulespass"));
            return false;
        }
        
        return true;
    }
    
    // For inserting new data
    public static boolean isUnique(Connection conn, String table, String col, String val) throws Exception {
        String sql = "SELECT 1 FROM " + table + " WHERE " + col + " = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
        	ps.setString(1, val);
            try (ResultSet rs = ps.executeQuery()) {
            	return !rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // For updating existing data
    public static boolean isUniqueOnUpdate(Connection conn, String tableName, String colName, String valueToCheck, String idColName, int currentId) {
        String sql = "SELECT 1 FROM " + tableName + " WHERE " + colName + " = ? AND " + idColName + " != ?";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, valueToCheck);
            ps.setInt(2, currentId);
            
            try (ResultSet rs = ps.executeQuery()) {
                return !rs.next(); 
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public static String createRandomPassword(int length) {
    	if (length < minPasswordLength) {
            length = minPasswordLength;
        }

        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String allChars = upper + lower + digits;

        SecureRandom random = new SecureRandom();
        List<Character> passwordChars = new ArrayList<>();

        // Add must-have characters
        passwordChars.add(upper.charAt(random.nextInt(upper.length())));
        passwordChars.add(lower.charAt(random.nextInt(lower.length())));
        passwordChars.add(digits.charAt(random.nextInt(digits.length())));

        // Fill remaining part with random characters
        for (int i = 3; i < length; i++) {
            passwordChars.add(allChars.charAt(random.nextInt(allChars.length())));
        }

        // Shuffle the list
        Collections.shuffle(passwordChars);

        // Make list a string
        StringBuilder sb = new StringBuilder();
        for (Character c : passwordChars) {
            sb.append(c);
        }

        return sb.toString();
    }
    
    public static boolean resetPasswordViaEmailForNeighbor(String email) throws Exception {       
        int neighborID = -1;
        String checkSql = 
                "SELECT n.neighborID, " +
                "EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - np.lastresetrequest)) as diff_seconds " +
                "FROM neighbor n " +
                "JOIN neighbor_passwords np ON n.neighborID = np.nID " +
                "WHERE n.email = ?";
        
        try (Connection conn = DBConnection.connect();
             PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
            
            psCheck.setString(1, email);
            ResultSet rs = psCheck.executeQuery();
            
            if (rs.next()) {
                neighborID = rs.getInt("neighborID");
                double diffSeconds = rs.getDouble("diff_seconds");
                
                // Time check (5 minutes)
                if (!rs.wasNull() && diffSeconds < 300) { 
                    long remaining = (long) (300 - diffSeconds);
                    throw new DialogException(Localization.get("waitbeforenewrequest", remaining));
                }
            }
            else {
            	// True because of security reasons, user cannot learn an email is in the system or not by using this request
            	return true;
            }
        }

        // Create password and update the database
        String newPass = createRandomPassword(10);
        boolean dbUpdated = false;
        String updateSql = "UPDATE neighbor_passwords " +
                           "SET passwordhash = ?, lastresetrequest = CURRENT_TIMESTAMP " +
                           "WHERE nID = ?";
        
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(updateSql)) {
            
            String hashedPass = BCrypt.hashpw(newPass, BCrypt.gensalt());
            ps.setString(1, hashedPass);
            ps.setInt(2, neighborID);
            
            int affected = ps.executeUpdate();
            if (affected > 0) dbUpdated = true;
        }

        // Send mail if everything is correct
        if (dbUpdated) {
            EmailSender.sendNewPassword(email, newPass); 
            return true;
        }
        
        throw new DialogException(Localization.get("errordb"));
    }
    
    private static boolean isAValidDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return false;

        try {
            LocalDate.parse(dateStr);
            return true;
        } catch (DateTimeParseException e) {
        	GuiHelper.showMessage(Localization.get("formatdate"));
            return false;
        }
    }
    
    public static boolean isAValidAge(String dateStr) {
    	if (dateStr == null || dateStr.isEmpty()) return false;

        try {
            LocalDate birthDate = LocalDate.parse(dateStr);
            LocalDate today = LocalDate.now();

            // Reject dates after than current date as birth date
            if (birthDate.isAfter(today)) {
            	GuiHelper.showMessage(Localization.get("invalidbirthdate"));
                return false;
            }

            // Calculate the age
            int age = Period.between(birthDate, today).getYears();

            // If age is smaller than minimum age, reject it
            if (age < minUserAge) {
            	GuiHelper.showMessage(Localization.get("errorbirthdatelow"));
                return false;
            }
            
            // If age is greater than maximum age, reject it
            if (age > maxUserAge) {
            	GuiHelper.showMessage(Localization.get("errorbirthdatehigh"));
                return false;
            }

            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
    
    private static boolean isAValidTCKN(String tckn) {
    	if (tckn == null || tckn.isEmpty()) {
    		return false;
    	}
    	
    	if (!tckn.matches("[0-9]+")) {
    		return false;
    	}
    	
    	if (tckn.length() != tcknLength) {
    		return false;
    	}
    	else {
    		return true;
    	}
    }
    
    public static boolean isAValidName(String name) {
    	if (name == null || name.isEmpty()) {
    		GuiHelper.showMessage(Localization.get("emptyname")); 
    		return false;
    	}
    	
    	if (!name.matches("[a-zA-ZçÇğĞıİöÖşŞüÜ]+")) {
    		GuiHelper.showMessage(Localization.get("invalidname"));
    		return false;
    	}
    	
    	if (name.length() > maxUserNameLength || name.length() < minUserNameLength) {
    		GuiHelper.showMessage(Localization.get("rulesname", minUserNameLength, maxUserNameLength)); 
    		return false;
    	}
    	else {
    		return true;
    	}
    }
    
    public static boolean isAValidEmail(String email) {
    	// email can be null
    	if (email == null || email.isEmpty()) {
    		return true;
    	}
    	
    	if (!email.contains("@")) {
    		GuiHelper.showMessage(Localization.get("invalidemail"));
    		return false;
    	}
    	
    	if (email.length() > maxEmailLength) {
    		GuiHelper.showMessage(Localization.get("rulesemail", maxEmailLength));
    		return false;
    	}
    	
    	return true;
    }
    
    private static String clearNumber(String number) {
    	return number.replaceAll("[^0-9+]", "");
    }
    
    public static boolean isAValidContactNumber(String number) {
    	// contact number can be null
    	if (number == null || number.isEmpty()) {
    		return true;
    	}
    	
    	String cleanNumber = clearNumber(number);
    	
    	if (!cleanNumber.startsWith("+")) {
    		GuiHelper.showMessage(Localization.get("rulecountrycode"));
            return false; 
        }
    	
    	int length = cleanNumber.length() - 1; // ignore '+'
        if (length < minContactNumberLength || length > maxContactNumberLength) {
        	GuiHelper.showMessage(Localization.get("rulesphone", minContactNumberLength, maxContactNumberLength));
            return false;
        }
        
        if (!cleanNumber.matches("^\\+[0-9]+$")) {
        	GuiHelper.showMessage(Localization.get("formatphone"));
            return false;
        }
        
        return true;
    }
    
    private static boolean isAValidCompanyName(String cname) {
    	if (cname == null || cname.isEmpty()) {
    		GuiHelper.showMessage(Localization.get("emptycname"));
    		return false;
    	}
    	
    	if (cname.length() > maxCompanyNameLength) {
    		GuiHelper.showMessage(Localization.get("rulescname", maxCompanyNameLength));
    		return false;
    	}
    	
    	return true;
    }
    
    public static boolean isAValidFaxNumber(String fax) {
    	// fax can be null
    	if (fax == null || fax.isEmpty()) {
    		return true;
    	}
        
        if (!fax.matches("^\\d{3}-\\d{3}-\\d{4}$")) {
        	GuiHelper.showMessage(Localization.get("formatfax"));
        	return false;
        }
        
        return true;
    }
    
    private static boolean isAValidTaxNumber(String taxNumber) {
    	if (taxNumber == null || taxNumber.isEmpty()) {
    		GuiHelper.showMessage(Localization.get("emptytaxno"));
    		return false;
    	}
    	
    	if (!taxNumber.matches("[0-9]+")) {
    		GuiHelper.showMessage(Localization.get("formattaxno"));
    		return false;
    	}
    	
    	if (taxNumber.length() > maxTaxNumberLength || taxNumber.length() < minTaxNumberLength) {
    		GuiHelper.showMessage(Localization.get("rulestaxno", minTaxNumberLength, maxTaxNumberLength));
    		return false;
    	}
    	else {
    		return true;
    	}
    }
}
