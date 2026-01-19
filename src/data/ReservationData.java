package data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import data.DisposalData.DisposalRecord;
import database.DBConnection;

public class ReservationData {
	
	public static List<DisposalRecord> getLastReservations(int last) throws Exception {
        List<DisposalRecord> list = new ArrayList<>();
        String sql = 
            "SELECT dd.ddno, d.disposalname, dd.weight, dd.volume, dd.ddscore, dd.ddate, r.reservationdate, r.recycledate, dd.rstatus, " +
                   "c.cname " +
            "FROM reservation r " +
            "JOIN reservation_disposal rd ON r.reservationNo = rd.rnumber " +
            "JOIN discarded_disposal dd ON rd.ddnumber = dd.ddno " +
            "JOIN disposal d ON dd.dID = d.disposalID " +
            "JOIN company c ON r.cID = c.companyID " +
            "ORDER BY r.reservationdate DESC LIMIT " + last;

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
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
                    rs.getBoolean("rstatus"),
                    true,
                    null,
                    rs.getString("cname") 
                ));
            }
        }
        
        return list;
    }
	
	public static boolean cancelReservation(int wasteID) throws Exception {    
        String sql = 
            "DELETE FROM reservation WHERE reservationNo = (" +
            "   SELECT rnumber FROM reservation_disposal WHERE ddnumber = ? LIMIT 1" +
            ")";
            
        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, wasteID);
            return ps.executeUpdate() > 0;
        }
    }
}
