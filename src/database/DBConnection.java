package database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {

	// Static pooling object
    private static HikariDataSource ds;
    private static final String CLIENT_IDENTITY;

    // Only works once when class called
    static {
    	String rawUser = System.getProperty("user.name");
        CLIENT_IDENTITY = rawUser.replaceAll("[^a-zA-Z0-9_]", ""); 
    	
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(Variables.URL);
        config.setDriverClassName("org.postgresql.Driver");
        
        // Addition for debug
        config.setConnectionTimeout(5000); 
        config.setMinimumIdle(0);
        
        config.setMaximumPoolSize(5); // Maximum Concurrent Connection for One User
        //config.setMinimumIdle(5); // Number of Ready Connections Waiting to Serve
        config.setMaxLifetime(1800000); // Connection lifetime (ms)
        config.setKeepaliveTime(30000); // Checks connection in every x ms
        config.setIdleTimeout(600000); // Connection idle lifetime (ms)
        config.setLeakDetectionThreshold(3000); // Connection leak tolerance (ms)

        try {
        	ds = new HikariDataSource(config);
        }
        catch (Exception e) {
        	System.err.println("Database pool cannot be created!");
        }
    }
    
	public static Connection connect() throws SQLException {
        Connection conn = ds.getConnection();
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("SET LOCAL \"app.current_user\" = 'USER_" + CLIENT_IDENTITY + "'");
            
        } catch (SQLException e) {
            conn.close();
            throw e;
        }
        
        return conn;
    }
    
    public static void shutdown() {
        if (ds != null) ds.close();
    }
}
