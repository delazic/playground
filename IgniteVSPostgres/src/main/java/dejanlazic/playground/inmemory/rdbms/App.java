package dejanlazic.playground.inmemory.rdbms;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import dejanlazic.playground.inmemory.rdbms.converter.BenefitPlanConverter;
import dejanlazic.playground.inmemory.rdbms.dao.BenefitPlanDAO;
import dejanlazic.playground.inmemory.rdbms.model.BenefitPlan;

/**
 * Application to demonstrate pure JDBC database connectivity
 */
public class App {
    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
    
    public static void main(String[] args) {
        DatabaseConnector connector = new DatabaseConnector();
        
        printHeader("PBM Database Connection Test - Pure JDBC");
        
        if (!testAndDisplayConnection(connector)) {
            return;
        }
        
        printSeparator();
        
        List<BenefitPlan> plans = loadBenefitPlans();
        if (plans == null) {
            return;
        }
        
        insertAndReportPlans(connector, plans);
    }
    
    /**
     * Print a formatted header with title
     * @param title Header title to display
     */
    private static void printHeader(String title) {
        System.out.println("=".repeat(60));
        System.out.println(title);
        System.out.println("=".repeat(60));
        System.out.println();
    }
    
    /**
     * Print a separator line
     */
    private static void printSeparator() {
        System.out.println();
        System.out.println("=".repeat(60));
    }
    
    /**
     * Test database connection and display results
     * @param connector Database connector to test
     * @return true if connection successful, false otherwise
     */
    private static boolean testAndDisplayConnection(DatabaseConnector connector) {
        System.out.println("Testing database connectivity...");
        if (!connector.testConnection()) {
            System.out.println("✗ Connection failed!");
            return false;
        }
        System.out.println("✓ Connection successful!");
        System.out.println();
        
        System.out.println("Retrieving database information...");
        try {
            String dbInfo = connector.getDatabaseInfo();
            System.out.println("✓ " + dbInfo);
        } catch (SQLException e) {
            System.out.println("✗ Failed to get database info: " + e.getMessage());
            return false;
        }
        System.out.println();
        
        System.out.println("=".repeat(60));
        System.out.println("Database connection test completed successfully!");
        System.out.println("=".repeat(60));
        return true;
    }
    
    /**
     * Load benefit plans from CSV file
     * @return List of benefit plans, or null if loading fails
     */
    private static List<BenefitPlan> loadBenefitPlans() {
        printHeader("Loading and Inserting Benefit Plans");
        
        BenefitPlanConverter planConverter = new BenefitPlanConverter();
        try {
            List<BenefitPlan> plans = planConverter.loadAllPlans();
            System.out.println("✅ Loaded " + plans.size() + " benefit plans from CSV file");
            return plans;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Failed to load benefit plans", ex);
            System.err.println("❌ Failed to load benefit plans: " + ex.getMessage());
            return null;
        }
    }
    
    /**
     * Insert benefit plans into database and report results using DAO
     * @param connector Database connector
     * @param plans List of benefit plans to insert
     */
    private static void insertAndReportPlans(DatabaseConnector connector, List<BenefitPlan> plans) {
        BenefitPlanDAO dao = new BenefitPlanDAO(connector);
        
        long startTime = System.currentTimeMillis();
        try {
            int inserted = dao.insertBatch(plans);
            long totalTime = System.currentTimeMillis() - startTime;
            System.out.println("✅ Successfully inserted " + inserted + " benefit plans");
            System.out.println("⏱️  Total time: " + totalTime + " ms");
            
            // Display count
            long totalCount = dao.count();
            System.out.println("📊 Total plans in database: " + totalCount);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to insert benefit plans", e);
            System.err.println("❌ Failed to insert benefit plans: " + e.getMessage());
        }
        printSeparator();
    }
}
