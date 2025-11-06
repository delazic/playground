package dejanlazic.playground.inmemory.rdbms;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import dejanlazic.playground.inmemory.rdbms.converter.BenefitPlanConverter;
import dejanlazic.playground.inmemory.rdbms.model.BenefitPlan;

/**
 * Application to demonstrate pure JDBC database connectivity
 */
public class App {
    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
    
    // Batch size for database inserts - adjust based on memory constraints
    private static final int BATCH_SIZE = 1000;
    
    // SQL statement for inserting benefit plans
    private static final String INSERT_PLAN_SQL = """
        INSERT INTO plan (
            plan_code, plan_name, plan_type, plan_category, effective_date,
            annual_deductible, out_of_pocket_max,
            tier1_copay, tier2_copay, tier3_copay, tier4_copay, tier5_copay,
            tier1_coinsurance, tier2_coinsurance, tier3_coinsurance, tier4_coinsurance, tier5_coinsurance,
            mail_order_available, specialty_pharmacy_required, description
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
    
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
     * Insert benefit plans into database and report results
     * @param connector Database connector
     * @param plans List of benefit plans to insert
     */
    private static void insertAndReportPlans(DatabaseConnector connector, List<BenefitPlan> plans) {
        long startTime = System.currentTimeMillis();
        try {
            int inserted = insertBenefitPlans(connector, plans);
            long totalTime = System.currentTimeMillis() - startTime;
            System.out.println("✅ Successfully inserted " + inserted + " benefit plans");
            System.out.println("⏱️  Total time: " + totalTime + " ms");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to insert benefit plans", e);
            System.err.println("❌ Failed to insert benefit plans: " + e.getMessage());
        }
        printSeparator();
    }

    /**
     * Insert benefit plans into the database using batch operations
     * @param connector Database connector
     * @param plans List of benefit plans to insert
     * @return Number of plans inserted
     * @throws SQLException if database error occurs
     */
    private static int insertBenefitPlans(DatabaseConnector connector, List<BenefitPlan> plans) throws SQLException {
        if (plans == null || plans.isEmpty()) {
            System.out.println("⚠️  No plans to insert");
            return 0;
        }
        
        int insertedCount = 0;
        
        try (Connection conn = connector.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_PLAN_SQL)) {
            
            // Disable auto-commit for transaction management
            conn.setAutoCommit(false);
            
            try {
                for (int i = 0; i < plans.size(); i++) {
                    setBenefitPlanParameters(ps, plans.get(i));
                    ps.addBatch();
                    
                    // Execute batch every BATCH_SIZE records or at the end
                    if ((i + 1) % BATCH_SIZE == 0 || i == plans.size() - 1) {
                        int[] results = ps.executeBatch();
                        insertedCount += results.length;
                        ps.clearBatch();
                    }
                }
                
                // Commit the transaction
                conn.commit();
                
            } catch (SQLException e) {
                // Rollback on error
                conn.rollback();
                throw new SQLException(
                    String.format("Failed to insert benefit plans. Successfully inserted: %d of %d. Error: %s",
                        insertedCount, plans.size(), e.getMessage()), e);
            } finally {
                // Restore auto-commit
                conn.setAutoCommit(true);
            }
        }
        
        return insertedCount;
    }
    
    /**
     * Set PreparedStatement parameters from a BenefitPlan object
     * @param ps PreparedStatement to populate
     * @param plan BenefitPlan containing the data
     * @throws SQLException if parameter setting fails
     */
    private static void setBenefitPlanParameters(PreparedStatement ps, BenefitPlan plan) throws SQLException {
        ps.setString(1, plan.getPlanCode());
        ps.setString(2, plan.getPlanName());
        ps.setString(3, plan.getPlanType());
        ps.setString(4, plan.getPlanCategory());
        ps.setDate(5, plan.getEffectiveDate() != null ? Date.valueOf(plan.getEffectiveDate()) : null);
        
        // Financial fields
        ps.setBigDecimal(6, plan.getAnnualDeductible());
        ps.setBigDecimal(7, plan.getOutOfPocketMax());
        
        // Tier copays
        ps.setBigDecimal(8, plan.getTier1Copay());
        ps.setBigDecimal(9, plan.getTier2Copay());
        ps.setBigDecimal(10, plan.getTier3Copay());
        ps.setBigDecimal(11, plan.getTier4Copay());
        ps.setBigDecimal(12, plan.getTier5Copay());
        
        // Tier coinsurance
        ps.setBigDecimal(13, plan.getTier1Coinsurance());
        ps.setBigDecimal(14, plan.getTier2Coinsurance());
        ps.setBigDecimal(15, plan.getTier3Coinsurance());
        ps.setBigDecimal(16, plan.getTier4Coinsurance());
        ps.setBigDecimal(17, plan.getTier5Coinsurance());
        
        // Boolean fields
        ps.setBoolean(18, plan.isMailOrderAvailable());
        ps.setBoolean(19, plan.isSpecialtyPharmacyRequired());
        
        // Description
        ps.setString(20, plan.getDescription());
    }
}
