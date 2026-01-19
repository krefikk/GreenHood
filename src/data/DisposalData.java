package data;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import app.Localization;
import database.DBConnection;

public class DisposalData {
	
	public static class DisposalType {
        public int id;
        private String name; // English original name
        public double tCostCoef;
        public double scoreCoef;

        public DisposalType(int id, String name) {
            this(id, name, 0, 0);
        }

        public DisposalType(int id, String name, double tCostCoef, double scoreCoef) {
            this.id = id;
            this.name = name;
            this.tCostCoef = tCostCoef;
            this.scoreCoef = scoreCoef;
        }

        private String getName() {
            if (name == null) return "";
            return Localization.findLocalizedDisposalTypeName(name);
        }

        @Override
        public String toString() {
            return getName();
        }
    }
    
    public static class DisposalRecord {
        public int ddno;
        public String disposalTypeName;
        public double weight;
        public double volume;
        public double score;
        
        public Date discardDate; // Discard
        public Date resDate;     // Reservation
        public Date recDate;     // Recycle
        
        public boolean isReserved;
        public boolean isRecycled;
        
        public String actorName;   
        public String companyName; 

        // General constructor
        public DisposalRecord(int ddno, String name, double w, double v, double s, 
                              Date dDate, Date rDate, Date rcDate, 
                              boolean rStatus, boolean isRes, 
                              String actor, String company) {
            this.ddno = ddno; 
            this.disposalTypeName = name; 
            this.weight = w; 
            this.volume = v;
            this.score = s; 
            
            this.discardDate = dDate; 
            this.resDate = rDate;
            this.recDate = rcDate;
            
            this.isRecycled = rStatus; 
            this.isReserved = isRes;

            this.actorName = (actor != null) ? actor : Localization.get("unknown");
            this.companyName = (company != null) ? company : (isReserved ? Localization.get("reserved") : Localization.get("available"));
        }

        // Constructor for available disposals
        public DisposalRecord(int ddno, String name, double w, double v, double s, 
                              Date dDate, String actor) {
            // rStatus=false, isReserved=false, Company=null
            this(ddno, name, w, v, s, dDate, null, null, false, false, actor, null);
        }
        
        public String getName() {
            return new DisposalType(0, this.disposalTypeName).toString();
        }
    }
    
    public static class DisposalFilter {
        public List<String> types = null; 
        public Double minWeight, maxWeight, minVolume, maxVolume; 
        public Date startDate, endDate; 
        public boolean onlyAllowed = false;
        
        public DisposalFilter() {}
        
        public DisposalFilter(List<String> types, Double minW, Double maxW, Double minV, Double maxV, Date startD, Date endD, boolean onlyAllowed) {
        	this.types = types;
        	minWeight = minW;
        	maxWeight = maxW;
        	minVolume = minV;
        	maxVolume = maxV;
        	startDate = startD;
        	endDate = endD;
        	this.onlyAllowed = onlyAllowed;
        }
    }
    
    // Returns all disposal types
    public static List<DisposalType> getAllDisposalTypes() throws Exception {
        List<DisposalType> list = new ArrayList<>();
        String sql = "SELECT disposalID, disposalname FROM disposal ORDER BY disposalname";
        
        try (Connection conn = DBConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
             
            while(rs.next()) {
                list.add(new DisposalType(
                    rs.getInt("disposalID"),
                    rs.getString("disposalname")
                ));
            }
        } 
        
        return list;
    }
    
    public static List<DisposalType> getAllDisposalTypesWithCoefs() throws Exception {
        List<DisposalType> list = new ArrayList<>();
        String sql = "SELECT disposalID, disposalname, tcostcoef, scorecoef FROM disposal ORDER BY disposalname";
        
        try (Connection conn = DBConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while(rs.next()) {
                list.add(new DisposalType(
                    rs.getInt("disposalID"),
                    rs.getString("disposalname"),
                    rs.getDouble("tcostcoef"),
                    rs.getDouble("scorecoef")
                ));
            }
        }
        
        return list;
    }
    
    // Returns disposal types that company with given companyID can recycle
    public static List<DisposalType> getDisposalTypes(Connection conn, int companyID) throws Exception {
        List<DisposalType> list = new ArrayList<>();
        String sql = 
        		"SELECT d.disposalID, d.disposalname " +
        	    "FROM company_disposal cd " +
        	    "JOIN disposal d ON cd.dID = d.disposalID " +
        	    "WHERE cd.cID = ?";
        
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
        	ps.setInt(1, companyID);
        	ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                list.add(new DisposalType(
                    rs.getInt("disposalID"),
                    rs.getString("disposalname")
                ));
            }
        } 

        return list;
    }
    
    public static boolean deleteDisposal(int ddno) throws Exception {
        String sql = "DELETE FROM discarded_disposal " +
                     "WHERE ddno = ? " +
        		     "AND rstatus = FALSE " +
                     "AND ddno NOT IN (SELECT ddnumber FROM reservation_disposal)";
                     
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ddno);
            return ps.executeUpdate() > 0;
        }
    }
    
    public static List<DisposalRecord> getLastDisposals(int last) throws Exception {
        List<DisposalRecord> list = new ArrayList<>();
        
        // Returns last "last" discarded disposal information from database
        String sql = "SELECT * FROM get_current_discarded_disposals(?)";

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, last);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String fname = rs.getString("fname");
                    String mname = rs.getString("mname");
                    String lname = rs.getString("lname");

                    String fullName = fname + (mname != null ? " " + mname : "") + " " + lname;
                    
                    list.add(new DisposalRecord(
                        rs.getInt("ddno"),
                        rs.getString("disposalname"),
                        rs.getDouble("weight"),
                        rs.getDouble("volume"),
                        rs.getDouble("ddscore"),
                        rs.getDate("ddate"),           
                        rs.getDate("reservationdate"),  
                        rs.getDate("recycledate"),      
                        rs.getBoolean("rstatus"),
                        rs.getBoolean("is_reserved"),
                        fullName,           
                        rs.getString("cname") 
                    ));
                }
            }
        } 
        
        return list;
    }
    
    public static List<DisposalRecord> getLastRecycledItems(int last) throws Exception {
        List<DisposalRecord> list = new ArrayList<>();
        
        // Returns last "last" recycled disposals from database
        String sql = "SELECT * FROM get_current_recycled_disposals(?)";

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, last);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new DisposalRecord(
                        rs.getInt("ddno"),
                        rs.getString("disposalname"),
                        rs.getDouble("weight"),
                        rs.getDouble("volume"),
                        rs.getDouble("ddscore"),
                        rs.getDate("ddate"),
                        rs.getDate("reservationdate"),
                        rs.getDate("recycledate"), 
                        true, 
                        true, // if recycled, it has to be reserved
                        null, // no need for a neighbor
                        rs.getString("cname") 
                    ));
                }
            }
        } 
        
        return list;
    }
    
    public static List<DisposalRecord> getAvailableDisposals() throws Exception {
        List<DisposalRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM get_available_discarded_disposals_list()"; 

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while(rs.next()){
                list.add(new DisposalRecord(
                    rs.getInt("dd_no"),
                    rs.getString("waste_type"),
                    rs.getDouble("weight"),
                    rs.getDouble("volume"),
                    rs.getDouble("score"),
                    rs.getDate("discard_date"),
                    rs.getString("full_name")
                ));
            }
        } 
        
        return list;
    }
    
    public static boolean reserveDiscardedDisposal(String taxNumber, int ddNo) throws Exception {
        Connection conn = null;
        PreparedStatement pstmtRes = null;
        PreparedStatement pstmtLink = null;
        ResultSet rs = null;

        try {
            conn = DBConnection.connect();
            conn.setAutoCommit(false); // Transaction
            
            String sqlReservation = "INSERT INTO reservation (cID) " +
                                    "VALUES ((SELECT companyID FROM company WHERE taxNumber = ?))";
            
            // Use RETURN_GENERATED_KEYS to return ID
            pstmtRes = conn.prepareStatement(sqlReservation, Statement.RETURN_GENERATED_KEYS);
            pstmtRes.setString(1, taxNumber);
            
            int affected = pstmtRes.executeUpdate();
            if (affected == 0) {
                conn.rollback();
                return false;
            }

            int reservationNo = -1;
            rs = pstmtRes.getGeneratedKeys();
            if (rs.next()) {
                reservationNo = rs.getInt(1);
            } else {
                conn.rollback();
                return false;
            }

            String sqlLink = "INSERT INTO reservation_disposal (rnumber, ddnumber) VALUES (?, ?)";
            pstmtLink = conn.prepareStatement(sqlLink);
            pstmtLink.setInt(1, reservationNo);
            pstmtLink.setInt(2, ddNo);
            pstmtLink.executeUpdate();

            conn.commit();
            return true;

        } catch (SQLException e) {
        	if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
            }
            throw e;

        } finally {
            try { if(rs != null) rs.close(); } catch(Exception e){}
            try { if(pstmtRes != null) pstmtRes.close(); } catch(Exception e){}
            try { if(pstmtLink != null) pstmtLink.close(); } catch(Exception e){}
            try { if(conn != null) conn.setAutoCommit(true); conn.close(); } catch(Exception e){}
        }
    }
    
    public static boolean completeRecycling(int ddNo) throws Exception {
        // Since there is a trigger which automatically updates the date in reservation table, only updates the boolean
        String sql = "UPDATE discarded_disposal SET rstatus = TRUE WHERE ddno = ?";

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, ddNo);
            
            // Greater than 0 means successful
            return ps.executeUpdate() > 0;

        }
    }
    
    public static boolean addDisposalRecord(String tckn, int dID, double weight, 
    		double volume, double tCost, double ddScore) throws Exception {
        String sql = "INSERT INTO discarded_disposal (dID, neighbortckn, weight, volume, " + 
        			 "tcost, ddscore, rstatus, ddate) " +
                     "VALUES (?, ?, ?, ?, ?, ?, FALSE, CURRENT_DATE)";
        
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, dID);
            ps.setString(2, tckn);
            ps.setDouble(3, weight);
            ps.setDouble(4, volume);
            ps.setDouble(5, tCost);
            ps.setDouble(6, ddScore);
            
            return ps.executeUpdate() > 0;
            
        }
    }
    
    public static double calculateDisposalScore(int typeId, double weight, double volume) throws Exception {
        double score = 0.0;
        
        // Function to calculate score of a specific discarded disposal
        String sql = "SELECT calculate_disposal_score(?, ?, ?)";
        
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, typeId);
            ps.setDouble(2, weight);
            ps.setDouble(3, volume);
            
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                score = rs.getDouble(1); // Return value
            }
        }
        
        return score;
    }
    
    public static double calculateDisposalTransportationCost(int typeId, double weight, double volume) throws Exception {
        double cost = 0.0;
        
        // Function to calculate transportation cost of a specific discarded disposal
        String sql = "SELECT calculate_disposal_cost(?, ?, ?)";
        
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, typeId);
            ps.setDouble(2, weight);
            ps.setDouble(3, volume);
            
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                cost = rs.getDouble(1); // Return value
            }
        }
        
        return cost;
    }
    
    public static List<DisposalRecord> getAvailableDisposalsFiltered(DisposalFilter filter, String taxNumber) throws Exception {
        List<DisposalRecord> list = new ArrayList<>();
        
        // Function to get filtered disposals with given parameters
        String sql = "SELECT * FROM get_available_disposals_filtered(?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Convert java list to SQL array
            if (filter != null && filter.types != null && !filter.types.isEmpty()) {
                String[] typeArray = filter.types.toArray(new String[0]);
                java.sql.Array sqlArray = conn.createArrayOf("VARCHAR", typeArray);
                ps.setArray(1, sqlArray);
            } else {
                ps.setArray(1, null);
            }
            
            // With setObject, if filter value is null, all values go to database as null with their data types
            ps.setObject(2, (filter != null) ? filter.minWeight : null, java.sql.Types.NUMERIC);
            ps.setObject(3, (filter != null) ? filter.maxWeight : null, java.sql.Types.NUMERIC);
            ps.setObject(4, (filter != null) ? filter.minVolume : null, java.sql.Types.NUMERIC);
            ps.setObject(5, (filter != null) ? filter.maxVolume : null, java.sql.Types.NUMERIC);

            ps.setDate(6, (filter != null) ? filter.startDate : null);
            ps.setDate(7, (filter != null) ? filter.endDate : null);
            ps.setString(8, taxNumber);
            ps.setBoolean(9, (filter != null) ? filter.onlyAllowed : false);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String typeName = rs.getString("type_name");
                list.add(new DisposalRecord(
                    rs.getInt("dd_no"),
                    typeName,
                    rs.getDouble("weight"),
                    rs.getDouble("volume"),
                    rs.getDouble("score"),
                    rs.getDate("d_date"),
                    null, null, false, false, null, null
                ));
            }
        } 
        
        return list;
    }
}
