package dejanlazic.playground.inmemory.rdbms;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import dejanlazic.playground.inmemory.rdbms.converter.BenefitPlanConverter;
import dejanlazic.playground.inmemory.rdbms.converter.MemberConverter;
import dejanlazic.playground.inmemory.rdbms.dao.BenefitPlanDAO;
import dejanlazic.playground.inmemory.rdbms.dao.MemberDAO;
import dejanlazic.playground.inmemory.rdbms.model.BenefitPlan;
import dejanlazic.playground.inmemory.rdbms.model.Member;

/**
 * Application to demonstrate CRUD operations with pure JDBC database connectivity
 *
 * Usage:
 *   java App [operation] [entity]
 *
 * Operations: CREATE, READ, UPDATE, DELETE, ALL
 * Entities: PLAN, MEMBER
 *
 * Examples:
 *   java App CREATE PLAN    - Insert benefit plans from CSV
 *   java App READ PLAN      - Read and display plans
 *   java App UPDATE PLAN    - Update a sample plan
 *   java App DELETE PLAN    - Delete a sample plan
 *   java App ALL PLAN       - Run all CRUD operations for plans
 *   java App                - Run all operations for all entities (default)
 */
public class App {
    private static final Logger LOGGER = Logger.getLogger(App.class.getName());
    
    public static void main(String[] args) {
        DatabaseConnector connector = new DatabaseConnector();
        
        // Parse command-line arguments
        String operation = args.length > 0 ? args[0].toUpperCase() : "ALL";
        String entity = args.length > 1 ? args[1].toUpperCase() : "ALL";
        
        printHeader("PBM Database CRUD Operations - Pure JDBC");
        System.out.println("Operation: " + operation + " | Entity: " + entity);
        System.out.println();
        
        if (!testAndDisplayConnection(connector)) {
            return;
        }
        
        printSeparator();
        
        // Execute requested operations
        try {
            if ("ALL".equals(entity) || "PLAN".equals(entity)) {
                executePlanOperations(connector, operation);
            }
            
            if ("ALL".equals(entity) || "MEMBER".equals(entity)) {
                executeMemberOperations(connector, operation);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error executing operations", e);
            System.err.println("❌ Error: " + e.getMessage());
        }
    }
    
    /**
     * Execute CRUD operations for Plan entity
     */
    private static void executePlanOperations(DatabaseConnector connector, String operation) {
        switch (operation) {
            case "CREATE" -> createPlans(connector);
            case "READ" -> readPlans(connector);
            case "UPDATE" -> updatePlan(connector);
            case "DELETE" -> deletePlan(connector);
            case "ALL" -> {
                createPlans(connector);
                readPlans(connector);
                updatePlan(connector);
                deletePlan(connector);
            }
            default -> System.err.println("❌ Unknown operation: " + operation);
        }
    }
    
    /**
     * Execute CRUD operations for Member entity
     */
    private static void executeMemberOperations(DatabaseConnector connector, String operation) {
        switch (operation) {
            case "CREATE" -> createMembers(connector);
            case "READ" -> readMembers(connector);
            case "UPDATE" -> updateMember(connector);
            case "DELETE" -> deleteMember(connector);
            case "ALL" -> {
                createMembers(connector);
                readMembers(connector);
                updateMember(connector);
                deleteMember(connector);
            }
            default -> System.err.println("❌ Unknown operation: " + operation);
        }
    }
    
    /**
     * CREATE operation for Plans
     */
    private static void createPlans(DatabaseConnector connector) {
        List<BenefitPlan> plans = loadBenefitPlans();
        if (plans != null) {
            insertAndReportPlans(connector, plans);
        }
    }
    
    /**
     * READ operation for Plans
     */
    private static void readPlans(DatabaseConnector connector) {
        printHeader("Reading Benefit Plans");
        BenefitPlanDAO dao = new BenefitPlanDAO(connector);
        
        try {
            List<BenefitPlan> plans = dao.findAll();
            System.out.println("📖 Found " + plans.size() + " plans in database");
            System.out.println();
            
            // Display first 5 plans
            int displayCount = Math.min(5, plans.size());
            System.out.println("Displaying first " + displayCount + " plans:");
            System.out.println("-".repeat(80));
            
            for (int i = 0; i < displayCount; i++) {
                BenefitPlan plan = plans.get(i);
                System.out.printf("%-15s | %-40s | %-15s%n",
                    plan.getPlanCode(),
                    truncate(plan.getPlanName(), 40),
                    plan.getPlanType());
            }
            System.out.println("-".repeat(80));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to read plans", e);
            System.err.println("❌ Failed to read plans: " + e.getMessage());
        }
        printSeparator();
    }
    
    /**
     * UPDATE operation for Plans
     */
    private static void updatePlan(DatabaseConnector connector) {
        printHeader("Updating a Benefit Plan");
        BenefitPlanDAO dao = new BenefitPlanDAO(connector);
        
        try {
            // Find first plan to update
            List<BenefitPlan> plans = dao.findAll();
            if (plans.isEmpty()) {
                System.out.println("⚠️  No plans found to update");
                printSeparator();
                return;
            }
            
            BenefitPlan plan = plans.get(0);
            String originalName = plan.getPlanName();
            
            // Update the plan
            plan.setPlanName(originalName + " (UPDATED)");
            plan.setAnnualDeductible(plan.getAnnualDeductible().add(java.math.BigDecimal.valueOf(100)));
            
            boolean updated = dao.update(plan);
            if (updated) {
                System.out.println("✅ Successfully updated plan: " + plan.getPlanCode());
                System.out.println("   Old name: " + originalName);
                System.out.println("   New name: " + plan.getPlanName());
            } else {
                System.out.println("⚠️  Plan not found or not updated");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update plan", e);
            System.err.println("❌ Failed to update plan: " + e.getMessage());
        }
        printSeparator();
    }
    
    /**
     * DELETE operation for Plans
     */
    private static void deletePlan(DatabaseConnector connector) {
        printHeader("Deleting a Benefit Plan");
        BenefitPlanDAO dao = new BenefitPlanDAO(connector);
        
        try {
            // Find a plan to delete
            List<BenefitPlan> plans = dao.findAll();
            if (plans.isEmpty()) {
                System.out.println("⚠️  No plans found to delete");
                printSeparator();
                return;
            }
            
            // Delete the last plan
            BenefitPlan planToDelete = plans.get(plans.size() - 1);
            String planCode = planToDelete.getPlanCode();
            
            // Note: We need to get the plan_id first
            Optional<BenefitPlan> foundPlan = dao.findByPlanCode(planCode);
            if (foundPlan.isEmpty()) {
                System.out.println("⚠️  Plan not found");
                printSeparator();
                return;
            }
            
            System.out.println("⚠️  DELETE operation requires plan_id which is not available in current model");
            System.out.println("   Plan code: " + planCode);
            System.out.println("   Skipping delete operation");
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete plan", e);
            System.err.println("❌ Failed to delete plan: " + e.getMessage());
        }
        printSeparator();
    }
    
    /**
     * CREATE operation for Members
     */
    private static void createMembers(DatabaseConnector connector) {
        List<Member> members = loadMembers();
        if (members != null) {
            insertAndReportMembers(connector, members);
        }
    }
    
    /**
     * READ operation for Members
     */
    private static void readMembers(DatabaseConnector connector) {
        printHeader("Reading Members");
        MemberDAO dao = new MemberDAO(connector);
        
        try {
            long count = dao.count();
            System.out.println("📖 Found " + String.format("%,d", count) + " members in database");
            
            if (count > 0) {
                System.out.println("   (Use specific queries to view member details)");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to read members", e);
            System.err.println("❌ Failed to read members: " + e.getMessage());
        }
        printSeparator();
    }
    
    /**
     * UPDATE operation for Members
     */
    private static void updateMember(DatabaseConnector connector) {
        printHeader("Updating a Member");
        System.out.println("⚠️  Member UPDATE operation not yet implemented");
        printSeparator();
    }
    
    /**
     * DELETE operation for Members
     */
    private static void deleteMember(DatabaseConnector connector) {
        printHeader("Deleting a Member");
        System.out.println("⚠️  Member DELETE operation not yet implemented");
        printSeparator();
    }
    
    /**
     * Truncate string to specified length
     */
    private static String truncate(String str, int maxLength) {
        if (str == null) return "";
        return str.length() <= maxLength ? str : str.substring(0, maxLength - 3) + "...";
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
    
    /**
     * Load members from CSV files
     * @return List of members, or null if loading fails
     */
    private static List<Member> loadMembers() {
        printHeader("Loading and Inserting Members");
        
        MemberConverter memberConverter = new MemberConverter();
        try {
            List<Member> members = memberConverter.loadAllMembers();
            System.out.println("✅ Loaded " + String.format("%,d", members.size()) + " members from CSV files");
            return members;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Failed to load members", ex);
            System.err.println("❌ Failed to load members: " + ex.getMessage());
            return null;
        }
    }
    
    /**
     * Insert members into database and report results using DAO
     * @param connector Database connector
     * @param members List of members to insert
     */
    private static void insertAndReportMembers(DatabaseConnector connector, List<Member> members) {
        MemberDAO dao = new MemberDAO(connector);
        
        System.out.println("📝 Inserting " + String.format("%,d", members.size()) + " members into database...");
        System.out.println("⏳ This may take a while for large datasets...");
        
        long startTime = System.currentTimeMillis();
        try {
            int inserted = dao.insertBatch(members);
            long totalTime = System.currentTimeMillis() - startTime;
            double seconds = totalTime / 1000.0;
            double recordsPerSecond = inserted / seconds;
            
            System.out.println("✅ Successfully inserted " + String.format("%,d", inserted) + " members");
            System.out.println("⏱️  Total time: " + String.format("%.2f", seconds) + " seconds");
            System.out.println("🚀 Throughput: " + String.format("%,.0f", recordsPerSecond) + " records/sec");
            
            // Display count
            long totalCount = dao.count();
            System.out.println("📊 Total members in database: " + String.format("%,d", totalCount));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to insert members", e);
            System.err.println("❌ Failed to insert members: " + e.getMessage());
        }
        printSeparator();
    }
}
