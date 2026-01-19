package data;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import database.DBConnection;

public class DashboardData {

	// Entity for leader boards
    public static class LeaderboardEntry {
        public String name; // full name for neighbors, company name for companies
        public String uniqueNo; // tckn for neighbors, tax number for companies
        public double score;

        public LeaderboardEntry(String name, String tckn, double score) {
            this.name = name;
            this.uniqueNo = tckn;
            this.score = score;
        }
    }

    public static List<LeaderboardEntry> getTopNeighbors(int count) throws Exception {
        List<LeaderboardEntry> list = new ArrayList<>();

        String selectSql = "SELECT fname, mname, lname, tckn, total_score FROM neighbor_leaderboard_view LIMIT ?";

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setInt(1, count);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String fname = rs.getString("fname");
                    String mname = rs.getString("mname");
                    String lname = rs.getString("lname");
                    
                    String fullName;
                    if (mname == null || mname.trim().isEmpty()) {
                        fullName = fname + " " + lname;
                    } else {
                        fullName = fname + " " + mname + " " + lname;
                    }
                    
                    String tckn = rs.getString("tckn");
                    double score = rs.getDouble("total_score");
                    
                    list.add(new LeaderboardEntry(fullName, tckn, score));
                }
            }
        }
        
        return list;
    }

    public static List<LeaderboardEntry> getTopCompanies(int count) throws Exception {
        List<LeaderboardEntry> list = new ArrayList<>();
        
        // Get all companies and their score with LEFT JOIN to prevent valueless entries
        String selectSql = "SELECT cname, taxnumber, totalScore FROM company_leaderboard_view LIMIT ?";

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(selectSql)) {
            	 ps.setInt(1, count);
            	 
            	 try (ResultSet rs = ps.executeQuery()) {
                     while (rs.next()) {
                    	 LeaderboardEntry e = new LeaderboardEntry(
                    			 rs.getString("cname"), rs.getString("taxnumber"), rs.getDouble("totalScore")
                    	 );
                         list.add(e);
                     }
            	 }
        }
        
        return list;
    }
    
    public static List<LeaderboardEntry> getNeighborhoodLeaderboard(String tckn, int limit) throws Exception {
        List<LeaderboardEntry> list = new ArrayList<>();
        
        // Get all neighbors information which has same neighborhood as this user
        String sql = "SELECT * FROM get_neighborhood_leaderboard(?, ?)";

        try (Connection conn = DBConnection.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, tckn);
            ps.setInt(2, limit);
            
            ResultSet rs = ps.executeQuery();
            
            while(rs.next()){
                String fname = rs.getString("fname");
                String lname = rs.getString("lname");
                String mname = rs.getString("mname");
                
                String fullName;
                if (mname == null || mname.trim().isEmpty()) {
                    fullName = fname + " " + lname;
                } else {
                    fullName = fname + " " + mname + " " + lname;
                }
                
                list.add(new LeaderboardEntry(fullName, rs.getString("out_tckn"), rs.getDouble("total_score")));
            }
        }
        
        return list;
    }
}