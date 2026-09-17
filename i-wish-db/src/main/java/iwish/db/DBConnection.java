package iwish.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Central place that opens JDBC connections to MySQL.
 *
 * IMPORTANT: this returns a NEW connection every time getConnection()
 * is called. That is intentional for a multi-threaded server — each
 * client thread should get its own Connection object and close it
 * (via try-with-resources) when it's done with a single query/transaction.
 * Do NOT share one Connection object across threads; MySQL's JDBC
 * Connection is not thread-safe.
 */
public final class DBConnection {

    private static final String CONFIG_FILE = "/db.properties";
    private static String url;
    private static String user;
    private static String password;

    static {
        loadConfig();
        try {
            // Explicit driver load — not strictly required on modern
            // JDBC 4+ drivers, but keeps startup errors clear if the
            // driver JAR is missing from the classpath.
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                "MySQL JDBC driver not found on classpath. " +
                "Check that mysql-connector-j is in pom.xml.", e);
        }
    }

    private DBConnection() {
        // utility class — never instantiated
    }

    private static void loadConfig() {
        try (InputStream in = DBConnection.class.getResourceAsStream(CONFIG_FILE)) {
            if (in == null) {
                throw new RuntimeException(
                    "db.properties not found on classpath at " + CONFIG_FILE +
                    ". Make sure it's in src/main/resources/.");
            }
            Properties props = new Properties();
            props.load(in);
            url = props.getProperty("db.url");
            user = props.getProperty("db.user");
            password = props.getProperty("db.password");

            if (url == null || user == null || password == null) {
                throw new RuntimeException("db.properties is missing one of: db.url, db.user, db.password");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read db.properties", e);
        }
    }

    /**
     * Opens and returns a new JDBC connection.
     * Caller is responsible for closing it — always use try-with-resources:
     *
     *   try (Connection conn = DBConnection.getConnection()) {
     *       ...
     *   }
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
