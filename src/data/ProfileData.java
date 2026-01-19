package data;

import app.AppUtils.*;
import data.DisposalData.*;
import database.AuthManager;
import java.sql.*;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import app.Localization;
import database.AddressManager;
import database.DBConnection;
import gui.GuiHelper;

public class ProfileData {
    
	// Data Transportation Classes (DTO)
    public static class NeighborProfile {
        public int id;
        public int addressID;
        public String tckn, fname, mname, lname, email, phone, address;
        public Date bdate;
        public String sex;
        public int age;
        public double totalWeight, totalVolume;
        public double recycledWeight, recycledVolume;
        public double totalScore;
    }

    public static class CompanyProfile {
        public int id;
        public int addressID;
        public String taxNumber, cname, phone, fax, address;
        public boolean isGov;
        public double reservedWeight, reservedVolume;
        public double recycledWeight, recycledVolume;
        public String supportedTypes; // separated with comma
    }
    
    public static class CompanyStats {
        public int reservedCount = 0;
        public double reservedWeight = 0;
        public double reservedVolume = 0;
        public int recycledCount = 0;
        public double recycledWeight = 0;
        public double recycledVolume = 0;
        public double totalScore = 0;
    }
    
    public static class UserStats {
        public int totalCount = 0;
        public double totalWeight = 0;
        public double totalVolume = 0;
        public double totalScore = 0;
        public int recycledCount = 0;
        public int reservedCount = 0;
    }
    
    // Get all necessary information about neighbor
    public static NeighborProfile getNeighborProfile(String tckn) throws Exception {
        String sql = "SELECT * FROM get_neighbor_profile(?)";

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, tckn);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
            	NeighborProfile p = new NeighborProfile();
            	
                p.id = rs.getInt("neighborid");
                p.tckn = rs.getString("tckn");
                p.fname = rs.getString("fname");
                p.mname = rs.getString("mname");
                p.lname = rs.getString("lname");
                p.email = rs.getString("email");
                p.phone = rs.getString("contactnumber");
                p.addressID = rs.getInt("addressID");
                p.bdate = rs.getDate("bdate");
                p.sex = rs.getString("sex");
                p.address = AddressManager.getAddressStr(conn, p.id, true);
                
                // Age Calculation
                if (p.bdate != null) {
                    p.age = Period.between(p.bdate.toLocalDate(), LocalDate.now()).getYears();
                }

                p.totalWeight = rs.getDouble("tot_w");
                p.totalVolume = rs.getDouble("tot_v");
                p.recycledWeight = rs.getDouble("rec_w");
                p.recycledVolume = rs.getDouble("rec_v");
                p.totalScore = rs.getDouble("score");
                return p;
            }
        }

        return null;
    }

    // Get all necessary information about company
    public static CompanyProfile getCompanyProfile(String taxNumber) throws Exception {
        CompanyProfile p = new CompanyProfile();

        // Company information + all reservations + all recycles
        String sql = 
            "SELECT c.*, " +
            "   COALESCE(SUM(dd.weight), 0) as res_w, " +
            "   COALESCE(SUM(dd.volume), 0) as res_v, " +
            "   COALESCE(SUM(CASE WHEN dd.rstatus = TRUE THEN dd.weight ELSE 0 END), 0) as rec_w, " +
            "   COALESCE(SUM(CASE WHEN dd.rstatus = TRUE THEN dd.volume ELSE 0 END), 0) as rec_v " +
            "FROM company c " +
            "LEFT JOIN reservation r ON c.companyid = r.cID " +
            "LEFT JOIN reservation_disposal rd ON r.reservationNo = rd.rnumber " +
            "LEFT JOIN discarded_disposal dd ON rd.ddnumber = dd.ddno " +
            "WHERE c.taxnumber = ? " +
            "GROUP BY c.companyid";

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, taxNumber);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                p.id = rs.getInt("companyID");
                p.taxNumber = rs.getString("taxnumber");
                p.cname = rs.getString("cname");
                p.phone = rs.getString("contactnumber");
                p.fax = rs.getString("faxnumber");
                p.addressID = rs.getInt("addressID");
                p.isGov = rs.getBoolean("governmentservice");
                p.address = AddressManager.getAddressStr(conn, p.id, false);
                
                p.reservedWeight = rs.getDouble("res_w");
                p.reservedVolume = rs.getDouble("res_v");
                p.recycledWeight = rs.getDouble("rec_w");
                p.recycledVolume = rs.getDouble("rec_v");
                
                String supportedDisposalTypesStr = "";
                List<DisposalType> supportedDisposalTypes = DisposalData.getDisposalTypes(conn, p.id);
                for (int i = 0; i < supportedDisposalTypes.size(); i++) {
                	supportedDisposalTypesStr = supportedDisposalTypesStr + supportedDisposalTypes.get(i).toString() + ", ";
                }
                p.supportedTypes = supportedDisposalTypesStr.substring(0, supportedDisposalTypesStr.length() - 2);
                
                return p;
            }
        }
        
        return null;
    }
    
    // 0: All Time, 1: Last 1 Year, 2: Last 1 Month
    public static CompanyStats getCompanyStats(String taxNumber, int rangeMode) throws Exception {
        CompanyStats stats = new CompanyStats();
        
        // Determine date interval
        String interval = "100 years"; // Default
        if (rangeMode == 1) interval = "1 year";
        else if (rangeMode == 2) interval = "1 month";

        String sql = 
            "SELECT " +
            // Reservations
            "COUNT(CASE WHEN r.reservationdate >= CURRENT_DATE - INTERVAL '" + interval + "' THEN 1 END) as res_cnt, " +
            "SUM(CASE WHEN r.reservationdate >= CURRENT_DATE - INTERVAL '" + interval + "' THEN dd.weight ELSE 0 END) as res_w, " +
            "SUM(CASE WHEN r.reservationdate >= CURRENT_DATE - INTERVAL '" + interval + "' THEN dd.volume ELSE 0 END) as res_v, " +
            // Recycles
            "COUNT(CASE WHEN dd.rstatus = TRUE AND r.recycledate >= CURRENT_DATE - INTERVAL '" + interval + "' THEN 1 END) as rec_cnt, " +
            "SUM(CASE WHEN dd.rstatus = TRUE AND r.recycledate >= CURRENT_DATE - INTERVAL '" + interval + "' THEN dd.weight ELSE 0 END) as rec_w, " +
            "SUM(CASE WHEN dd.rstatus = TRUE AND r.recycledate >= CURRENT_DATE - INTERVAL '" + interval + "' THEN dd.volume ELSE 0 END) as rec_v, " +
            // Score
            "SUM(CASE WHEN dd.rstatus = TRUE AND r.recycledate >= CURRENT_DATE - INTERVAL '" + interval + "' THEN dd.ddscore ELSE 0 END) as total_score " +
            "FROM company c " +
            "JOIN reservation r ON c.companyID = r.cID " +
            "JOIN reservation_disposal rd ON r.reservationNo = rd.rnumber " +
            "JOIN discarded_disposal dd ON rd.ddnumber = dd.ddno " +
            // Filtering
            "WHERE c.taxnumber = ? AND " +
            "(r.reservationdate >= CURRENT_DATE - INTERVAL '" + interval + "' " +
            " OR (dd.rstatus = TRUE AND r.recycledate >= CURRENT_DATE - INTERVAL '" + interval + "'))";

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, taxNumber);
            
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                stats.reservedCount = rs.getInt("res_cnt");
                stats.reservedWeight = rs.getDouble("res_w");
                stats.reservedVolume = rs.getDouble("res_v");
                
                stats.recycledCount = rs.getInt("rec_cnt");
                stats.recycledWeight = rs.getDouble("rec_w");
                stats.recycledVolume = rs.getDouble("rec_v");
                
                stats.totalScore = rs.getDouble("total_score"); 
            }
        }
        
        return stats;
    }
    
    public static List<String> getCompanyAllowedDisposalTypes(String taxNumber) throws Exception {
        List<String> allowedTypes = new ArrayList<>();

        String query = "SELECT d.disposalname " +
                       "FROM disposal d " +
                       "JOIN company_disposal cd ON d.disposalID = cd.dID " +
                       "JOIN company c ON cd.cID = c.companyID " +
                       "WHERE c.taxNumber = ?";

        try (Connection conn = DBConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, taxNumber);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    allowedTypes.add(rs.getString("disposalname"));
                }
            }
            
        }
        
        return allowedTypes;
    }
    
    public static boolean updateNeighbor(int id, String fname, String mname, String lname, Date bdate, String email, String phone, 
                                         int provinceID, int districtID, int neighborhoodID, int streetID, 
                                         String buildingNoStr, String floorNoStr, String flatNoStr) throws Exception {
        
        // Validation
        if (!AuthManager.isAValidName(fname) || !AuthManager.isAValidName(lname)) return false;
        if (!AuthManager.isAValidAge(bdate.toString())) return false;
        if (!AuthManager.isAValidEmail(email)) return false;
        if (!AuthManager.isAValidContactNumber(phone)) return false;

        // Casting
        int buildingNo, floorNo, doorNo;
        try {
            buildingNo = Integer.parseInt(buildingNoStr);
            floorNo = Integer.parseInt(floorNoStr);
            doorNo = Integer.parseInt(flatNoStr);
        } catch (NumberFormatException e) {
            throw new DialogException(Localization.get("invalibuildinginfo"));
        }

        Connection conn = null;
        PreparedStatement psCheckAddr = null;
        PreparedStatement psCreateAddr = null;
        PreparedStatement psNeighbor = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.connect();
            
            // Uniqueness checks
            if (!AuthManager.isUniqueOnUpdate(conn, "neighbor", "email", email, "neighborid", id)) {
            	throw new DialogException(Localization.get("alreadyregisteredemail"));
            }
            if (!AuthManager.isUniqueOnUpdate(conn, "neighbor", "contactnumber", phone, "neighborid", id)) {
                throw new DialogException(Localization.get("alreadyregisteredphone"));
            }
            
            conn.setAutoCommit(false); // Transaction

            int finalAddressID = -1;

            // Check if selected address is already existing
            String sqlCheckAddr = "SELECT addressid FROM address WHERE streetID=? AND buildingno=? AND floorno=? AND doorno=?";
            psCheckAddr = conn.prepareStatement(sqlCheckAddr);
            psCheckAddr.setInt(1, streetID);
            psCheckAddr.setInt(2, buildingNo);
            psCheckAddr.setInt(3, floorNo);
            psCheckAddr.setInt(4, doorNo);
            
            rs = psCheckAddr.executeQuery();
            
            if (rs.next()) {
                finalAddressID = rs.getInt("addressid");
            } else {
            	// If there is no address like that, create new one
                rs.close(); 
                
                String sqlCreateAdress = "INSERT INTO address (streetID, buildingno, floorno, doorno) " +
                                         "VALUES (?, ?, ?, ?) RETURNING addressid";
                psCreateAddr = conn.prepareStatement(sqlCreateAdress);
                psCreateAddr.setInt(1, streetID);
                psCreateAddr.setInt(2, buildingNo);
                psCreateAddr.setInt(3, floorNo);
                psCreateAddr.setInt(4, doorNo);
                
                rs = psCreateAddr.executeQuery();
                if (rs.next()) {
                    finalAddressID = rs.getInt(1);
                }
            }

            if (finalAddressID == -1) {
                conn.rollback();
                throw new DialogException(Localization.get("erroraddress"));
            }

            // Update neighbor table
            String sqlNeighbor = "UPDATE neighbor SET fname=?, mname=?, lname=?, bdate=?, email=?, contactnumber=?, addressID=? WHERE neighborid=?";
            psNeighbor = conn.prepareStatement(sqlNeighbor);
            
            psNeighbor.setString(1, fname);
            psNeighbor.setString(2, (mname.equals("") ? null : mname));
            psNeighbor.setString(3, lname);
            psNeighbor.setDate(4, bdate);
            psNeighbor.setString(5, email);
            psNeighbor.setString(6, phone);
            psNeighbor.setInt(7, finalAddressID);
            psNeighbor.setInt(8, id);

            int affectedNeighborRows = psNeighbor.executeUpdate();
            
            if (affectedNeighborRows == 0) {
                conn.rollback(); 
                throw new DialogException(Localization.get("errordb"));
            }
            
            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            throw e;
            
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (psCheckAddr != null) psCheckAddr.close(); } catch (Exception e) {}
            try { if (psCreateAddr != null) psCreateAddr.close(); } catch (Exception e) {}
            try { if (psNeighbor != null) psNeighbor.close(); } catch (Exception e) {}
            try { if (conn != null) { conn.setAutoCommit(true); conn.close(); } } catch (Exception e) {}
        }
    }

    public static boolean updateCompany(int id, String cname, String phone, String fax, 
                                        int provinceID, int districtID, int neighborhoodID, int streetID, 
                                        String buildingNoStr, String floorNoStr, String flatNoStr) throws Exception {
        
        // Validation
        if (cname == null || cname.trim().length() < 2) {
             GuiHelper.showMessage(Localization.get("invalidcname"));
             return false;
        }
        if (!AuthManager.isAValidContactNumber(phone)) return false;
        if (!AuthManager.isAValidFaxNumber(fax)) return false;

        // Casting
        int buildingNo, floorNo, doorNo;
        try {
            buildingNo = Integer.parseInt(buildingNoStr);
            floorNo = Integer.parseInt(floorNoStr);
            doorNo = Integer.parseInt(flatNoStr);
        } catch (NumberFormatException e) {
        	throw new DialogException(Localization.get("invalibuildinginfo"));
        }

        Connection conn = null;
        PreparedStatement psCheckUnique = null;
        PreparedStatement psCheckAddr = null;
        PreparedStatement psCreateAddr = null;
        PreparedStatement psCompany = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.connect();
            
            // Uniqueness checks
            if (!AuthManager.isUniqueOnUpdate(conn, "company", "cname", cname, "companyid", id)) {
                throw new DialogException(Localization.get("alreadyregisteredcname"));
            }
            if (!AuthManager.isUniqueOnUpdate(conn, "company", "contactnumber", phone, "companyid", id)) {
                throw new DialogException(Localization.get("alreadyregisteredphone"));
            }
            if (!AuthManager.isUniqueOnUpdate(conn, "company", "faxnumber", fax, "companyid", id)) {
                throw new DialogException(Localization.get("alreadyregisteredfax"));
            }

            // Transaction
            conn.setAutoCommit(false); 

            int finalAddressID = -1;

            // Address check
            String sqlCheckAddr = "SELECT addressid FROM address WHERE streetID=? AND buildingno=? AND floorno=? AND doorno=?";
            psCheckAddr = conn.prepareStatement(sqlCheckAddr);
            psCheckAddr.setInt(1, streetID);
            psCheckAddr.setInt(2, buildingNo);
            psCheckAddr.setInt(3, floorNo);
            psCheckAddr.setInt(4, doorNo);
            
            rs = psCheckAddr.executeQuery();
            
            if (rs.next()) {
                finalAddressID = rs.getInt("addressid");
            } else {
                // If no address found, create new one
                rs.close(); 
                
                String sqlCreateAdress = "INSERT INTO address (streetID, buildingno, floorno, doorno) " +
                                         "VALUES (?, ?, ?, ?) RETURNING addressid";
                psCreateAddr = conn.prepareStatement(sqlCreateAdress);
                psCreateAddr.setInt(1, streetID);
                psCreateAddr.setInt(2, buildingNo);
                psCreateAddr.setInt(3, floorNo);
                psCreateAddr.setInt(4, doorNo);
                
                rs = psCreateAddr.executeQuery();
                if (rs.next()) {
                    finalAddressID = rs.getInt(1);
                }
            }

            if (finalAddressID == -1) {
                conn.rollback();
                throw new DialogException(Localization.get("erroraddress"));
            }

            // Update company table
            String sqlCompany = "UPDATE company SET cname=?, contactnumber=?, faxnumber=?, addressID=? WHERE companyid=?";
            psCompany = conn.prepareStatement(sqlCompany);
            
            psCompany.setString(1, cname);
            psCompany.setString(2, phone);
            psCompany.setString(3, fax);
            psCompany.setInt(4, finalAddressID);
            psCompany.setInt(5, id);

            int affectedCompanyRows = psCompany.executeUpdate();
            
            if (affectedCompanyRows == 0) {
                conn.rollback(); 
                throw new DialogException(Localization.get("errordb"));
            }
            
            // Commit if everything is allright
            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            throw e;
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (psCheckUnique != null) psCheckUnique.close(); } catch (Exception e) {}
            try { if (psCheckAddr != null) psCheckAddr.close(); } catch (Exception e) {}
            try { if (psCreateAddr != null) psCreateAddr.close(); } catch (Exception e) {}
            try { if (psCompany != null) psCompany.close(); } catch (Exception e) {}
            try { if (conn != null) { conn.setAutoCommit(true); conn.close(); } } catch (Exception e) {}
        }
    }
    
    // Delete neighbor
    public static boolean deleteNeighbor(String tckn) throws Exception {
        String sql = "DELETE FROM neighbor WHERE tckn = ?";
        
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, tckn);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        }
    }
    
    // Delete company
    public static boolean deleteCompany(String taxnumber) throws Exception {
        String sql = "DELETE FROM company WHERE taxnumber = ?";
        
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, taxnumber);
            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;  
        }
    }
    
    public static List<DisposalRecord> getCompanyReservedDisposals(String taxNumber) throws Exception {
        List<DisposalRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM get_company_reserved_discarded_disposals_list(?)";

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, taxNumber);
            ResultSet rs = ps.executeQuery();
            
            while(rs.next()){
                list.add(new DisposalRecord(
                    rs.getInt("dd_no"),
                    rs.getString("waste_type"),
                    rs.getDouble("weight"),
                    rs.getDouble("volume"),
                    rs.getDouble("score"),
                    rs.getDate("discard_date"),           
                    rs.getDate("res_date"), 
                    null,
                    false, // isRecycled
                    true,  // isReserved
                    rs.getString("full_name"),
                    rs.getString("company_name")
                ));
            }
        } 
        
        return list;
    }
    
    public static List<DisposalRecord> getCompanyRecycledDisposals(String taxNumber) throws Exception {
        List<DisposalRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM get_company_recycled_discarded_disposals_list(?)";

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, taxNumber);
            ResultSet rs = ps.executeQuery();
            
            while(rs.next()){
                list.add(new DisposalRecord(
                    rs.getInt("dd_no"),
                    rs.getString("waste_type"),
                    rs.getDouble("weight"),
                    rs.getDouble("volume"),
                    rs.getDouble("score"),
                    rs.getDate("discard_date"),           
                    rs.getDate("res_date"),
                    rs.getDate("rec_date"),
                    true, // isRecycled
                    true, // isReserved
                    rs.getString("full_name"),
                    rs.getString("company_name")
                ));
            }
        }
        
        return list;
    }
    
    public static String getUserNeighborhoodName(String tckn) throws Exception {
        String sql = "SELECT nh.neighborhoodname " + 
                     "FROM neighbor n " +
                     "JOIN address a ON n.addressID = a.addressid " +
                     "JOIN street s ON a.streetID = s.streetid " +
                     "JOIN neighborhood nh ON s.neighborhoodID = nh.neighborhoodid " +
                     "WHERE n.tckn = ?";
                     
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, tckn);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getString("neighborhoodname");
            }
        }
        
        return Localization.get("unknown"); // If cannot found, use default
    }
    
    // 0: All Time, 1: Last 1 Year, 2: Last 1 Month
    public static UserStats getUserStats(String tckn, int rangeMode) throws Exception {
        UserStats stats = new UserStats();
        
        String dateFilter = "";
        if (rangeMode == 1) {
            dateFilter = " AND dd.ddate >= CURRENT_DATE - INTERVAL '1 year' ";
        } else if (rangeMode == 2) {
            dateFilter = " AND dd.ddate >= CURRENT_DATE - INTERVAL '1 month' ";
        }

        String sql = 
            "SELECT " +
            "COUNT(*) as cnt, " +
            "SUM(dd.weight) as sum_w, " +
            "SUM(dd.volume) as sum_v, " +
            "SUM(CASE WHEN dd.rstatus = TRUE THEN dd.ddscore ELSE 0 END) as sum_s, " +
            "COUNT(CASE WHEN dd.rstatus = TRUE THEN 1 END) as recycled_cnt, " +
            "COUNT(rd.ddnumber) as reserved_cnt " +
            "FROM discarded_disposal dd " +
            "LEFT JOIN reservation_disposal rd ON dd.ddno = rd.ddnumber " +
            "WHERE dd.neighbortckn = ?" + dateFilter;

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tckn);
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                stats.totalCount = rs.getInt("cnt");
                stats.totalWeight = rs.getDouble("sum_w");
                stats.totalVolume = rs.getDouble("sum_v");
                stats.totalScore = rs.getDouble("sum_s");
                stats.recycledCount = rs.getInt("recycled_cnt");
                stats.reservedCount = rs.getInt("reserved_cnt");
            }
        }
        
        return stats;
    }
    
    public static List<DisposalRecord> getFilteredDisposalHistory(String tckn, int daysLimit) throws Exception {
        List<DisposalRecord> list = new java.util.ArrayList<>();
        
        // Function to get disposals a specific neighbor discarded
        String sql = "SELECT * FROM get_user_history_filtered(?, ?)";
        
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {  
        	
            ps.setString(1, tckn);
            ps.setInt(2, daysLimit);
            
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int ddNo = rs.getInt("dd_no");
                String typeName = rs.getString("waste_type");
                double weight = rs.getDouble("weight");
                double volume = rs.getDouble("volume");
                double score = rs.getDouble("score");
                Date dDate = rs.getDate("d_date");
                String statusText = rs.getString("status_text");

                // not localizable texts
                boolean isRecycled = "Dönüştürüldü".equals(statusText);
                boolean isReserved = "Rezerve".equals(statusText) || isRecycled; 
                
                // Find localized version of disposal type name
                String localizedTypeName = Localization.findLocalizedDisposalTypeName(typeName);

                list.add(new DisposalRecord(
                    ddNo,              // ddno
                    localizedTypeName, // name
                    weight,            // w
                    volume,            // v
                    score,             // s
                    dDate,             // dDate
                    null,              // resDate
                    null,              // recDate
                    isRecycled,        // rStatus
                    isReserved,        // isRes
                    null,              // actor (No need since we already call it for a specific user)
                    null               // company (If isReserved == "Reserved", else == "Available")
                ));
            }
        }
        
        return list;
    }
}