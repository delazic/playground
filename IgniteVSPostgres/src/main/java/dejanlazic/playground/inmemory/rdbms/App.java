package dejanlazic.playground.inmemory.rdbms;

/**
 * Application to demonstrate pure JDBC database connectivity
 */
public class App {
    public static void main(String[] args) {
        DatabaseConnector connector = new DatabaseConnector();
        
        System.out.println("=".repeat(60));
        System.out.println("PBM Database Connection Test - Pure JDBC");
        System.out.println("=".repeat(60));
        System.out.println();
        
        // Test: Basic connectivity
        System.out.println("Testing database connectivity...");
        if (connector.testConnection()) {
            System.out.println("✓ Connection successful!");
        } else {
            System.out.println("✗ Connection failed!");
            return;
        }
        System.out.println();
        
        // Test: Get database info
        System.out.println("Retrieving database information...");
        try {
            String dbInfo = connector.getDatabaseInfo();
            System.out.println("✓ " + dbInfo);
        } catch (Exception e) {
            System.out.println("✗ Failed to get database info: " + e.getMessage());
        }
        System.out.println();
        
        System.out.println("=".repeat(60));
        System.out.println("Database connection test completed successfully!");
        System.out.println("=".repeat(60));
    }
}
