package dejanlazic.playground.inmemory;

import dejanlazic.playground.inmemory.rdbms.DatabaseConnector;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test class for database connectivity using pure JDBC
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseConnectionTest {
    
    private static DatabaseConnector connector;
    
    @BeforeAll
    static void setUp() {
        connector = new DatabaseConnector();
        System.out.println("=".repeat(60));
        System.out.println("PBM Database Connection Test - Pure JDBC with JUnit 5");
        System.out.println("=".repeat(60));
    }
    
    @AfterAll
    static void tearDown() {
        System.out.println("=".repeat(60));
        System.out.println("All tests completed!");
        System.out.println("=".repeat(60));
    }
    
    @Test
    @Order(1)
    @DisplayName("Test 1: Basic database connectivity")
    void testConnection() {
        System.out.println("\nTest 1: Testing database connectivity...");
        boolean connected = connector.testConnection();
        assertTrue(connected, "Database connection should be successful");
        System.out.println("✓ Connection successful!");
    }
    
    @Test
    @Order(2)
    @DisplayName("Test 2: Retrieve database information")
    void testGetDatabaseInfo() {
        System.out.println("\nTest 2: Retrieving database information...");
        assertDoesNotThrow(() -> {
            String dbInfo = connector.getDatabaseInfo();
            assertNotNull(dbInfo, "Database info should not be null");
            assertTrue(dbInfo.contains("PostgreSQL"), "Should be PostgreSQL database");
            System.out.println("✓ " + dbInfo);
        });
    }
    
    @Test
    @Order(3)
    @DisplayName("Test 3: Query database tables")
    void testQueryTables() {
        System.out.println("\nTest 3: Querying database tables...");
        String query = "SELECT table_name FROM information_schema.tables " +
                      "WHERE table_schema = 'public' ORDER BY table_name";
        
        assertDoesNotThrow(() -> {
            try (Connection conn = connector.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                
                System.out.println("Tables in database:");
                int count = 0;
                while (rs.next()) {
                    count++;
                    System.out.println("  " + count + ". " + rs.getString("table_name"));
                }
                assertTrue(count > 0, "Should have at least one table");
                System.out.println("✓ Found " + count + " tables");
            }
        });
    }
    
    @Test
    @Order(4)
    @DisplayName("Test 4: Query sample plan data")
    void testQueryPlans() {
        System.out.println("\nTest 4: Querying sample plan data...");
        String planQuery = "SELECT plan_code, plan_name, plan_type, plan_category " +
                           "FROM plan LIMIT 5";
        
        assertDoesNotThrow(() -> {
            try (Connection conn = connector.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(planQuery)) {
                
                System.out.println("Sample plans:");
                System.out.println(String.format("%-20s %-35s %-15s %-15s",
                        "Plan Code", "Plan Name", "Type", "Category"));
                System.out.println("-".repeat(85));
                
                int count = 0;
                while (rs.next()) {
                    count++;
                    System.out.println(String.format("%-20s %-35s %-15s %-15s",
                            rs.getString("plan_code"),
                            rs.getString("plan_name"),
                            rs.getString("plan_type"),
                            rs.getString("plan_category")));
                }
                assertTrue(count > 0, "Should have at least one plan");
                System.out.println("✓ Query successful - found " + count + " plans");
            }
        });
    }
    
    @Test
    @Order(5)
    @DisplayName("Test 5: Count records in key tables")
    void testCountRecords() {
        System.out.println("\nTest 5: Counting records in key tables...");
        String[] tables = {"member", "plan", "drug", "pharmacy", "enrollment", "formulary"};
        
        assertDoesNotThrow(() -> {
            try (Connection conn = connector.getConnection();
                 Statement stmt = conn.createStatement()) {
                
                for (String table : tables) {
                    try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
                        if (rs.next()) {
                            int count = rs.getInt(1);
                            System.out.println(String.format("  %-15s: %d records", table, count));
                            assertTrue(count >= 0, "Count should be non-negative for " + table);
                        }
                    }
                }
                System.out.println("✓ Count queries successful");
            }
        });
    }
    
    @Test
    @Order(6)
    @DisplayName("Test 6: Verify connection can be closed properly")
    void testConnectionClose() {
        System.out.println("\nTest 6: Testing connection close...");
        assertDoesNotThrow(() -> {
            Connection conn = connector.getConnection();
            assertNotNull(conn, "Connection should not be null");
            assertFalse(conn.isClosed(), "Connection should be open");
            
            conn.close();
            assertTrue(conn.isClosed(), "Connection should be closed");
            System.out.println("✓ Connection closed successfully");
        });
    }
}
