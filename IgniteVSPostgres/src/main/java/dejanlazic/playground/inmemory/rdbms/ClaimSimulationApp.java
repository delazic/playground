package dejanlazic.playground.inmemory.rdbms;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import dejanlazic.playground.inmemory.rdbms.dao.BenefitPlanDAO;
import dejanlazic.playground.inmemory.rdbms.dao.ClaimDAO;
import dejanlazic.playground.inmemory.rdbms.dao.DrugDAO;
import dejanlazic.playground.inmemory.rdbms.dao.EnrollmentDAO;
import dejanlazic.playground.inmemory.rdbms.dao.FormularyDrugDAO;
import dejanlazic.playground.inmemory.rdbms.dao.MemberDAO;
import dejanlazic.playground.inmemory.rdbms.dao.PharmacyDAO;
import dejanlazic.playground.inmemory.rdbms.dao.PharmacyNetworkDAO;
import dejanlazic.playground.inmemory.rdbms.service.ClaimAdjudicationService;
import dejanlazic.playground.inmemory.rdbms.service.ClaimSimulationService;

/**
 * Main application for PBM Claim Adjudication Simulation.
 * 
 * Simulates a mid-size PBM processing 1 million claims in a typical day.
 * 
 * Usage:
 *   java ClaimSimulationApp [speed_multiplier]
 * 
 * Examples:
 *   java ClaimSimulationApp           - Run at 1x speed (real-time simulation)
 *   java ClaimSimulationApp 10        - Run at 10x speed (10 times faster)
 *   java ClaimSimulationApp 100       - Run at 100x speed (100 times faster)
 * 
 * Prerequisites:
 *   1. Database must be running (docker-compose up)
 *   2. Schema must be initialized
 *   3. Reference data must be loaded (members, pharmacies, drugs, plans, etc.)
 *   4. Claims CSV file must be generated (run generate_1m_claims.py)
 */
public class ClaimSimulationApp {
    private static final Logger LOGGER = Logger.getLogger(ClaimSimulationApp.class.getName());
    
    public static void main(String[] args) {
        // Parse speed multiplier from command line
        double speedMultiplier = 1.0;
        if (args.length > 0) {
            try {
                speedMultiplier = Double.parseDouble(args[0]);
                if (speedMultiplier <= 0) {
                    System.err.println("Error: Speed multiplier must be positive");
                    System.exit(1);
                }
            } catch (NumberFormatException e) {
                System.err.println("Error: Invalid speed multiplier: " + args[0]);
                System.err.println("Usage: java ClaimSimulationApp [speed_multiplier]");
                System.exit(1);
            }
        }
        
        printHeader();
        
        // Initialize database connector
        DatabaseConnector connector = new DatabaseConnector();
        
        // Test database connection
        if (!testConnection(connector)) {
            System.err.println("❌ Database connection failed. Please ensure:");
            System.err.println("   1. Docker containers are running (docker-compose up)");
            System.err.println("   2. Database is accessible on localhost:5432");
            System.err.println("   3. Credentials in database.properties are correct");
            System.exit(1);
        }
        
        // Verify reference data exists
        if (!verifyReferenceData(connector)) {
            System.err.println("❌ Reference data missing. Please run:");
            System.err.println("   1. make run-create-plan");
            System.err.println("   2. make run-create-drug");
            System.err.println("   3. make run-create-pharmacy");
            System.err.println("   4. make run-create-member");
            System.err.println("   5. make run-create-enrollment");
            System.exit(1);
        }
        
        // Initialize DAOs
        MemberDAO memberDAO = new MemberDAO(connector);
        EnrollmentDAO enrollmentDAO = new EnrollmentDAO(connector);
        PharmacyDAO pharmacyDAO = new PharmacyDAO(connector);
        PharmacyNetworkDAO pharmacyNetworkDAO = new PharmacyNetworkDAO(connector);
        DrugDAO drugDAO = new DrugDAO(connector);
        FormularyDrugDAO formularyDrugDAO = new FormularyDrugDAO(connector);
        BenefitPlanDAO benefitPlanDAO = new BenefitPlanDAO(connector);
        ClaimDAO claimDAO = new ClaimDAO(connector);
        
        // Initialize services
        ClaimAdjudicationService adjudicationService = new ClaimAdjudicationService(
            memberDAO,
            enrollmentDAO,
            pharmacyDAO,
            pharmacyNetworkDAO,
            drugDAO,
            formularyDrugDAO,
            benefitPlanDAO
        );
        
        ClaimSimulationService simulationService = new ClaimSimulationService(
            adjudicationService,
            claimDAO
        );
        
        // Run simulation
        try {
            System.out.println("Starting simulation...");
            System.out.println();
            
            simulationService.runSimulation(speedMultiplier);
            
            System.out.println();
            System.out.println("✓ Simulation completed successfully!");
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading claims data", e);
            System.err.println("❌ Error loading claims data: " + e.getMessage());
            System.err.println();
            System.err.println("Please ensure the claims CSV files exist:");
            System.err.println("  src/main/resources/data/us_pharmacy_claims_simulation_1m_01.csv");
            System.err.println("  src/main/resources/data/us_pharmacy_claims_simulation_1m_02.csv");
            System.err.println("  ... (multiple files of ~30MB each)");
            System.err.println();
            System.err.println("Generate them by running:");
            System.err.println("  cd database/scripts");
            System.err.println("  python3 generate_1m_claims.py");
            System.exit(1);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database error during simulation", e);
            System.err.println("❌ Database error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during simulation", e);
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Print application header.
     */
    private static void printHeader() {
        System.out.println();
        System.out.println("╔" + "═".repeat(78) + "╗");
        System.out.println("║" + " ".repeat(78) + "║");
        System.out.println("║" + centerText("PBM CLAIM ADJUDICATION SIMULATION", 78) + "║");
        System.out.println("║" + centerText("Mid-Size PBM - 1 Million Claims/Day", 78) + "║");
        System.out.println("║" + " ".repeat(78) + "║");
        System.out.println("╚" + "═".repeat(78) + "╝");
        System.out.println();
    }
    
    /**
     * Center text within a given width.
     */
    private static String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        return " ".repeat(padding) + text + " ".repeat(width - padding - text.length());
    }
    
    /**
     * Test database connection.
     */
    private static boolean testConnection(DatabaseConnector connector) {
        System.out.println("Testing database connection...");
        
        if (!connector.testConnection()) {
            return false;
        }
        
        try {
            String dbInfo = connector.getDatabaseInfo();
            System.out.println("✓ Connected to: " + dbInfo);
            System.out.println();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to get database info", e);
            System.err.println("✗ Failed to get database info: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Verify that required reference data exists in the database.
     */
    private static boolean verifyReferenceData(DatabaseConnector connector) {
        System.out.println("Verifying reference data...");
        
        try {
            MemberDAO memberDAO = new MemberDAO(connector);
            PharmacyDAO pharmacyDAO = new PharmacyDAO(connector);
            DrugDAO drugDAO = new DrugDAO(connector);
            BenefitPlanDAO planDAO = new BenefitPlanDAO(connector);
            EnrollmentDAO enrollmentDAO = new EnrollmentDAO(connector);
            
            long memberCount = memberDAO.count();
            long pharmacyCount = pharmacyDAO.count();
            long drugCount = drugDAO.count();
            long planCount = planDAO.count();
            long enrollmentCount = enrollmentDAO.count();
            
            System.out.println("  Members:     " + String.format("%,d", memberCount));
            System.out.println("  Pharmacies:  " + String.format("%,d", pharmacyCount));
            System.out.println("  Drugs:       " + String.format("%,d", drugCount));
            System.out.println("  Plans:       " + String.format("%,d", planCount));
            System.out.println("  Enrollments: " + String.format("%,d", enrollmentCount));
            System.out.println();
            
            if (memberCount == 0 || pharmacyCount == 0 || drugCount == 0 || 
                planCount == 0 || enrollmentCount == 0) {
                return false;
            }
            
            System.out.println("✓ All reference data verified");
            System.out.println();
            return true;
            
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error verifying reference data", e);
            System.err.println("✗ Error verifying reference data: " + e.getMessage());
            return false;
        }
    }
}

// Made with Bob