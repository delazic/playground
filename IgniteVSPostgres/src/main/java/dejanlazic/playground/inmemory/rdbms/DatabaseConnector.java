package dejanlazic.playground.inmemory.rdbms;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Pure JDBC Database Connector for PostgreSQL
 * No connection pooling, no ORM - just raw JDBC
 */
public class DatabaseConnector {
    private final String url;
    private final String username;
    private final String password;
    private final String driver;
    
    public DatabaseConnector() {
        Properties props = loadProperties();

        this.url = props.getProperty("db.url");
        this.username = props.getProperty("db.username");
        this.password = props.getProperty("db.password");
        this.driver = props.getProperty("db.driver");
        
        loadDriver();
    }
    
    public DatabaseConnector(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.driver = "org.postgresql.Driver";
        
        loadDriver();
    }
    
    private Properties loadProperties() {
        Properties props = new Properties();

        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("database.properties")) {
            if (input == null) {
                throw new RuntimeException("Unable to find database.properties");
            }
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load database properties", e);
        }
        return props;
    }
    
    private void loadDriver() {
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("PostgreSQL JDBC Driver not found", e);
        }
    }
    
    /**
     * Get a new database connection
     * Caller is responsible for closing the connection
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
    
    /**
     * Test database connectivity
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Get database metadata information
     */
    public String getDatabaseInfo() throws SQLException {
        try (Connection conn = getConnection()) {
            var metaData = conn.getMetaData();
            return String.format("Database: %s %s, Driver: %s %s",
                    metaData.getDatabaseProductName(),
                    metaData.getDatabaseProductVersion(),
                    metaData.getDriverName(),
                    metaData.getDriverVersion());
        }
    }
    
    public String getUrl() {
        return url;
    }
    
    public String getUsername() {
        return username;
    }
}
