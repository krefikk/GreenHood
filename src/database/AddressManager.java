package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import app.AppUtils.*;
import app.Localization;
import gui.GuiHelper.ComboItem;

public class AddressManager {

    public static class AddressDetails {
        public int provinceID, districtID, neighborhoodID, streetID;
        public int buildingNo, floorNo, doorNo;
    }
    
    public static AddressDetails getAddressDetails(int addressID) throws Exception {
        AddressDetails details = new AddressDetails();
        String sql = 
            "SELECT a.buildingno, a.floorno, a.doorno, a.streetID, " +
            "       s.neighborhoodID, n.districtID, d.provinceID " +
            "FROM address a " +
            "JOIN street s ON a.streetID = s.streetid " +
            "JOIN neighborhood n ON s.neighborhoodID = n.neighborhoodid " +
            "JOIN district d ON n.districtID = d.districtid " +
            "WHERE a.addressid = ?";

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, addressID);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    details.buildingNo = rs.getInt("buildingno");
                    details.floorNo = rs.getInt("floorno");
                    details.doorNo = rs.getInt("doorno");
                    details.streetID = rs.getInt("streetID");
                    details.neighborhoodID = rs.getInt("neighborhoodID");
                    details.districtID = rs.getInt("districtID");
                    details.provinceID = rs.getInt("provinceID");
                }
            }
        }
        
        return details;
    }
    
    // Get all provinces
    public static List<ComboItem> getProvinces() throws Exception {
        List<ComboItem> list = new ArrayList<>();
        String sql = "SELECT provinceid, provincename FROM province ORDER BY provincename ASC";

        try (Connection conn = DBConnection.connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new ComboItem(rs.getInt("provinceid"), rs.getString("provincename")));
            }
        } 
        
        return list;
    }

    // Get all districts in given province
    public static List<ComboItem> getDistricts(int cityId) throws Exception {
        List<ComboItem> list = new ArrayList<>();
        String sql = "SELECT districtid, districtname FROM district WHERE provinceID = ? ORDER BY districtname ASC";

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, cityId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ComboItem(rs.getInt("districtid"), rs.getString("districtname")));
                }
            }
        } 
        
        return list;
    }

    // Get all neighborhoods in given district
    public static List<ComboItem> getNeighborhoods(int districtId) throws Exception {
        List<ComboItem> list = new ArrayList<>();
        String sql = "SELECT neighborhoodid, neighborhoodname FROM neighborhood WHERE districtID = ? ORDER BY neighborhoodname ASC";

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, districtId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ComboItem(rs.getInt("neighborhoodid"), rs.getString("neighborhoodname")));
                }
            }
        }
        
        return list;
    }

    // Get all streets in given neighborhood
    public static List<ComboItem> getStreets(int neighborhoodId) throws Exception {
        List<ComboItem> list = new ArrayList<>();
        String sql = "SELECT streetid, streetname FROM street WHERE neighborhoodID = ? ORDER BY streetname ASC";

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setInt(1, neighborhoodId);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ComboItem(rs.getInt("streetid"), rs.getString("streetname")));
                }
            }
        }
        
        return list;
    }

    // Register given address into database
    public static int registerAddress(int streetId, int buildingNo, int floorNo, int doorNo) throws Exception {
        // Check uniqueness of given address
        String checkSql = "SELECT addressid FROM address WHERE streetID=? AND buildingno=? AND floorno =? AND doorno=?";
        
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(checkSql)) {
            
            ps.setInt(1, streetId);
            ps.setInt(2, buildingNo);
            ps.setInt(3, floorNo);
            ps.setInt(4, doorNo);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("addressid"); // If not unique, return the same address' ID
                }
            }
        }

        // If unique, insert into database
        String insertSql = "INSERT INTO address (streetID, buildingno, floorno, doorno) VALUES (?, ?, ?, ?) RETURNING addressid";
        
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(insertSql)) {
            
            ps.setInt(1, streetId);
            ps.setInt(2, buildingNo);
            ps.setInt(3, floorNo);
            ps.setInt(4, doorNo);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        
        throw new DialogException(Localization.get("failedaddressregister"));
    }
    
    // Get address as a string both for companies and neighbors
    public static String getAddressStr(Connection conn, int ID, boolean isNeighbor) {
        int addressID = -1;
        String addressIdSql;
        
        if (isNeighbor) {
            addressIdSql = "SELECT addressID FROM neighbor WHERE neighborid = ?";
        } else {
            addressIdSql = "SELECT addressID FROM company WHERE companyid = ?";
        }
        
        try (PreparedStatement ps = conn.prepareStatement(addressIdSql)) {
            ps.setInt(1, ID);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    addressID = rs.getInt("addressID");
                    if (rs.wasNull()) addressID = -1;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(); 
        }
        
        if (addressID == -1) {
            return Localization.get("addressnotfound");
        }
        
        StringBuilder sb = new StringBuilder();
        String addressStrSql = "SELECT adr.buildingno, adr.floorno, adr.doorno, st.streetname, ng.neighborhoodname, d.districtname, p.provincename "
                            + "FROM address adr "
                            + "JOIN street st ON st.streetid = adr.streetID "
                            + "JOIN neighborhood ng ON ng.neighborhoodid = st.neighborhoodID "
                            + "JOIN district d ON d.districtid = ng.districtID "
                            + "JOIN province p ON p.provinceid = d.provinceID "
                            + "WHERE addressid = ?";
        
        try (PreparedStatement ps = conn.prepareStatement(addressStrSql)) {
            ps.setInt(1, addressID);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    sb.append(rs.getString("neighborhoodname")).append(" Mah. ");
                    sb.append(rs.getString("streetname")).append(" Sok. ");
                    
                    int bNo = rs.getInt("buildingno");
                    if (bNo > 0) {
                        sb.append("No: ").append(bNo).append(", ");
                    }
                    
                    int fNo = rs.getInt("floorno");
                    if (fNo > 0) {
                        sb.append(" Kat: ").append(fNo).append(", ");
                    }
                    
                    int dNo = rs.getInt("doorno");
                    if (dNo > 0) {
                        sb.append(" Daire: ").append(dNo).append(", ");
                    }
                    
                    sb.append(rs.getString("districtname")).append("/");
                    sb.append(rs.getString("provincename"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return Localization.get("errordb");
        }
        
        return sb.toString();
    }
}