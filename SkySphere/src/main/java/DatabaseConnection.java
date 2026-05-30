// ================================================================
//  DatabaseConnection.java
//  Package: (default package — put in src/main/java)
//  Purpose: Provides a single reusable MySQL JDBC connection.
//
//  How it works:
//    - Loads the MySQL JDBC driver class (com.mysql.cj.jdbc.Driver)
//    - Reads DB credentials from constants (change these to match yours)
//    - Returns a java.sql.Connection object used by the servlets
//
//  REQUIRED JAR: mysql-connector-j-8.x.x.jar
//    → Right-click project > Build Path > Add External JARs…
// ================================================================

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Utility class that creates and returns a MySQL JDBC connection.
 * All servlets call DatabaseConnection.getConnection() to talk to MySQL.
 */
public class DatabaseConnection {

    // ── Database configuration ─────────────────────────────────
    // ⚠️  Change these values to match your local MySQL setup.

    /** JDBC URL format: jdbc:mysql://<host>:<port>/<database>?options */
	private static final String URL =
		    "jdbc:mysql://"
		    + System.getenv("DB_HOST") + ":"
		    + System.getenv("DB_PORT") + "/"
		    + System.getenv("DB_NAME")
		    + "?useSSL=true&serverTimezone=Asia/Kolkata&allowPublicKeyRetrieval=true";

		private static final String USER     = System.getenv("DB_USER");
		private static final String PASSWORD = System.getenv("DB_PASSWORD");

    // ── Static initializer: load the driver once ───────────────
    static {
        try {
            // Load MySQL JDBC driver into JVM
            // MySQL Connector/J 8.x uses com.mysql.cj.jdbc.Driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("[SkySphere] MySQL JDBC Driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            // This happens if mysql-connector-j JAR is missing from the classpath
            System.err.println("[SkySphere] ERROR: MySQL Driver not found! " +
                               "Add mysql-connector-j JAR to your project build path.");
            e.printStackTrace();
        }
    }

    /**
     * Returns a live JDBC Connection to the 'skiesphere' MySQL database.
     *
     * Usage example:
     *   Connection conn = DatabaseConnection.getConnection();
     *   PreparedStatement ps = conn.prepareStatement("SELECT ...");
     *
     * Always close the connection in a finally block or try-with-resources!
     *
     * @return  java.sql.Connection object
     * @throws  SQLException if the connection cannot be established
     */
    public static Connection getConnection() throws SQLException {
        // DriverManager creates a new connection each time this is called.
        // For production, consider using a Connection Pool (e.g. HikariCP).
        Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
        System.out.println("[SkySphere] Database connection established.");
        return conn;
    }
}
