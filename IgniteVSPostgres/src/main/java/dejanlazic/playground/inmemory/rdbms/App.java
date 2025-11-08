package dejanlazic.playground.inmemory.rdbms;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import dejanlazic.playground.inmemory.rdbms.converter.BenefitPlanConverter;
import dejanlazic.playground.inmemory.rdbms.converter.DrugConverter;
import dejanlazic.playground.inmemory.rdbms.converter.EnrollmentConverter;
import dejanlazic.playground.inmemory.rdbms.converter.FormularyConverter;
import dejanlazic.playground.inmemory.rdbms.converter.FormularyDrugConverter;
import dejanlazic.playground.inmemory.rdbms.converter.MemberConverter;
import dejanlazic.playground.inmemory.rdbms.converter.PharmacyConverter;
import dejanlazic.playground.inmemory.rdbms.dao.BenefitPlanDAO;
import dejanlazic.playground.inmemory.rdbms.dao.DrugDAO;
import dejanlazic.playground.inmemory.rdbms.dao.EnrollmentDAO;
import dejanlazic.playground.inmemory.rdbms.dao.FormularyDAO;
import dejanlazic.playground.inmemory.rdbms.dao.FormularyDrugDAO;
import dejanlazic.playground.inmemory.rdbms.dao.MemberDAO;
import dejanlazic.playground.inmemory.rdbms.dao.PharmacyDAO;
import dejanlazic.playground.inmemory.rdbms.model.BenefitPlan;
import dejanlazic.playground.inmemory.rdbms.model.Drug;
import dejanlazic.playground.inmemory.rdbms.model.Enrollment;
import dejanlazic.playground.inmemory.rdbms.model.Formulary;
import dejanlazic.playground.inmemory.rdbms.model.FormularyDrug;
import dejanlazic.playground.inmemory.rdbms.model.Member;
import dejanlazic.playground.inmemory.rdbms.model.Pharmacy;

/**
 * Application to demonstrate CRUD operations with pure JDBC database connectivity
 *
 * Usage:
 *   java App [operation] [entity]
 *
 * Operations: CREATE, READ, UPDATE, DELETE, ALL
 * Entities: PLAN, DRUG, MEMBER, ENROLLMENT, FORMULARY, FORMULARY_DRUG, PHARMACY
 *
 * Examples:
 *   java App CREATE PLAN            - Insert benefit plans from CSV
 *   java App READ PLAN              - Read and display plans
 *   java App CREATE DRUG            - Insert drugs from CSV
 *   java App READ DRUG              - Read and display drugs
 *   java App CREATE PHARMACY        - Insert pharmacies from CSV
 *   java App READ PHARMACY          - Read and display pharmacies
 *   java App CREATE ENROLLMENT      - Insert enrollments from CSV
 *   java App READ ENROLLMENT        - Read and display enrollments
 *   java App CREATE FORMULARY       - Insert formularies from CSV
 *   java App READ FORMULARY         - Read and display formularies
 *   java App CREATE FORMULARY_DRUG  - Insert formulary-drug relationships from CSV
 *   java App READ FORMULARY_DRUG    - Read and display formulary-drug relationships
 *   java App ALL PHARMACY           - Run all CRUD operations for pharmacies
 *   java App                        - Run all operations for all entities (default)
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
            
            if ("ALL".equals(entity) || "DRUG".equals(entity)) {
                executeDrugOperations(connector, operation);
            }
            
            if ("ALL".equals(entity) || "PHARMACY".equals(entity)) {
                executePharmacyOperations(connector, operation);
            }
            
            if ("ALL".equals(entity) || "MEMBER".equals(entity)) {
                executeMemberOperations(connector, operation);
            }
            
            if ("ALL".equals(entity) || "ENROLLMENT".equals(entity)) {
                executeEnrollmentOperations(connector, operation);
            }
            
            if ("ALL".equals(entity) || "FORMULARY".equals(entity)) {
                executeFormularyOperations(connector, operation);
            }
            
            if ("ALL".equals(entity) || "FORMULARY_DRUG".equals(entity)) {
                executeFormularyDrugOperations(connector, operation);
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
     * Execute CRUD operations for Drug entity
     */
    private static void executeDrugOperations(DatabaseConnector connector, String operation) {
        switch (operation) {
            case "CREATE" -> createDrugs(connector);
            case "READ" -> readDrugs(connector);
            case "UPDATE" -> updateDrug(connector);
            case "DELETE" -> deleteDrug(connector);
            case "ALL" -> {
                createDrugs(connector);
                readDrugs(connector);
                updateDrug(connector);
                deleteDrug(connector);
            }
            default -> System.err.println("❌ Unknown operation: " + operation);
        }
    }
    
    /**
     * Execute CRUD operations for Pharmacy entity
     */
    private static void executePharmacyOperations(DatabaseConnector connector, String operation) {
        switch (operation) {
            case "CREATE" -> createPharmacies(connector);
            case "READ" -> readPharmacies(connector);
            case "UPDATE" -> updatePharmacy(connector);
            case "DELETE" -> deletePharmacy(connector);
            case "ALL" -> {
                createPharmacies(connector);
                readPharmacies(connector);
                updatePharmacy(connector);
                deletePharmacy(connector);
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
     * Execute CRUD operations for Enrollment entity
     */
    private static void executeEnrollmentOperations(DatabaseConnector connector, String operation) {
        switch (operation) {
            case "CREATE" -> createEnrollments(connector);
            case "READ" -> readEnrollments(connector);
            case "UPDATE" -> updateEnrollment(connector);
            case "DELETE" -> deleteEnrollment(connector);
            case "ALL" -> {
                createEnrollments(connector);
                readEnrollments(connector);
                updateEnrollment(connector);
                deleteEnrollment(connector);
            }
            default -> System.err.println("❌ Unknown operation: " + operation);
        }
    }
    
    /**
     * Execute CRUD operations for Formulary entity
     */
    private static void executeFormularyOperations(DatabaseConnector connector, String operation) {
        switch (operation) {
            case "CREATE" -> createFormularies(connector);
            case "READ" -> readFormularies(connector);
            case "UPDATE" -> updateFormulary(connector);
            case "DELETE" -> deleteFormulary(connector);
            case "ALL" -> {
                createFormularies(connector);
                readFormularies(connector);
                updateFormulary(connector);
                deleteFormulary(connector);
            }
            default -> System.err.println("❌ Unknown operation: " + operation);
        }
    }
    
    /**
     * Execute CRUD operations for FormularyDrug entity
     */
    private static void executeFormularyDrugOperations(DatabaseConnector connector, String operation) {
        switch (operation) {
            case "CREATE" -> createFormularyDrugs(connector);
            case "READ" -> readFormularyDrugs(connector);
            case "UPDATE" -> updateFormularyDrug(connector);
            case "DELETE" -> deleteFormularyDrug(connector);
            case "ALL" -> {
                createFormularyDrugs(connector);
                readFormularyDrugs(connector);
                updateFormularyDrug(connector);
                deleteFormularyDrug(connector);
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
     * CREATE operation for Drugs
     */
    private static void createDrugs(DatabaseConnector connector) {
        List<Drug> drugs = loadDrugs();
        if (drugs != null) {
            insertAndReportDrugs(connector, drugs);
        }
    }
    
    /**
     * READ operation for Drugs
     */
    private static void readDrugs(DatabaseConnector connector) {
        printHeader("Reading Drugs");
        DrugDAO dao = new DrugDAO(connector);
        
        try {
            long count = dao.count();
            System.out.println("📖 Found " + String.format("%,d", count) + " drugs in database");
            System.out.println();
            
            if (count > 0) {
                List<Drug> drugs = dao.findAll();
                
                // Display first 10 drugs
                int displayCount = Math.min(10, drugs.size());
                System.out.println("Displaying first " + displayCount + " drugs:");
                System.out.println("-".repeat(120));
                System.out.printf("%-15s | %-30s | %-25s | %-10s | %-15s | %-10s%n",
                    "NDC Code", "Drug Name", "Generic Name", "Strength", "Dosage Form", "Type");
                System.out.println("-".repeat(120));
                
                for (int i = 0; i < displayCount; i++) {
                    Drug drug = drugs.get(i);
                    System.out.printf("%-15s | %-30s | %-25s | %-10s | %-15s | %-10s%n",
                        drug.getNdcCode(),
                        truncate(drug.getDrugName(), 30),
                        truncate(drug.getGenericName(), 25),
                        truncate(drug.getStrength(), 10),
                        truncate(drug.getDosageForm(), 15),
                        drug.getDrugType());
                }
                System.out.println("-".repeat(120));
                
                // Display statistics
                long genericCount = drugs.stream().filter(Drug::isGeneric).count();
                long brandCount = drugs.stream().filter(Drug::isBrand).count();
                long specialtyCount = drugs.stream().filter(Drug::isSpecialty).count();
                
                System.out.println();
                System.out.println("Drug Type Distribution:");
                System.out.println("  Generic:   " + String.format("%,d", genericCount) +
                    " (" + String.format("%.1f%%", (genericCount * 100.0 / count)) + ")");
                System.out.println("  Brand:     " + String.format("%,d", brandCount) +
                    " (" + String.format("%.1f%%", (brandCount * 100.0 / count)) + ")");
                System.out.println("  Specialty: " + String.format("%,d", specialtyCount) +
                    " (" + String.format("%.1f%%", (specialtyCount * 100.0 / count)) + ")");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to read drugs", e);
            System.err.println("❌ Failed to read drugs: " + e.getMessage());
        }
        printSeparator();
    }
    
    /**
     * UPDATE operation for Drugs
     */
    private static void updateDrug(DatabaseConnector connector) {
        printHeader("Updating a Drug");
        DrugDAO dao = new DrugDAO(connector);
        
        try {
            // Find first drug to update
            List<Drug> drugs = dao.findAll();
            if (drugs.isEmpty()) {
                System.out.println("⚠️  No drugs found to update");
                printSeparator();
                return;
            }
            
            Drug drug = drugs.get(0);
            String originalName = drug.getDrugName();
            java.math.BigDecimal originalAwp = drug.getAwpPrice();
            
            // Update the drug
            drug.setDrugName(originalName + " (UPDATED)");
            if (originalAwp != null) {
                drug.setAwpPrice(originalAwp.add(java.math.BigDecimal.valueOf(10.00)));
            }
            
            boolean updated = dao.update(drug);
            if (updated) {
                System.out.println("✅ Successfully updated drug: " + drug.getNdcCode());
                System.out.println("   Old name: " + originalName);
                System.out.println("   New name: " + drug.getDrugName());
                if (originalAwp != null) {
                    System.out.println("   Old AWP: $" + originalAwp);
                    System.out.println("   New AWP: $" + drug.getAwpPrice());
                }
            } else {
                System.out.println("⚠️  Drug not found or not updated");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update drug", e);
            System.err.println("❌ Failed to update drug: " + e.getMessage());
        }
        printSeparator();
    }
    
    /**
     * DELETE operation for Drugs
     */
    private static void deleteDrug(DatabaseConnector connector) {
        printHeader("Deleting a Drug");
        DrugDAO dao = new DrugDAO(connector);
        
        try {
            // Find a drug to delete
            List<Drug> drugs = dao.findAll();
            if (drugs.isEmpty()) {
                System.out.println("⚠️  No drugs found to delete");
                printSeparator();
                return;
            }
            
            // Delete the last drug
            Drug drugToDelete = drugs.get(drugs.size() - 1);
            String ndcCode = drugToDelete.getNdcCode();
            
            boolean deleted = dao.delete(drugToDelete.getDrugId());
            if (deleted) {
                System.out.println("✅ Successfully deleted drug: " + ndcCode);
                System.out.println("   Drug name: " + drugToDelete.getDrugName());
            } else {
                System.out.println("⚠️  Drug not found or not deleted");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete drug", e);
            System.err.println("❌ Failed to delete drug: " + e.getMessage());
        }
        printSeparator();
    }
    
    /**
     * CREATE operation for Pharmacies
     */
    private static void createPharmacies(DatabaseConnector connector) {
        List<Pharmacy> pharmacies = loadPharmacies();
        if (pharmacies != null) {
            insertAndReportPharmacies(connector, pharmacies);
        }
    }
    
    /**
     * READ operation for Pharmacies
     */
    private static void readPharmacies(DatabaseConnector connector) {
        printHeader("Reading Pharmacies");
        PharmacyDAO dao = new PharmacyDAO(connector);
        
        try {
            long count = dao.count();
            System.out.println("📖 Found " + String.format("%,d", count) + " pharmacies in database");
            System.out.println();
            
            if (count > 0) {
                List<Pharmacy> pharmacies = dao.findAll();
                
                // Display first 10 pharmacies
                int displayCount = Math.min(10, pharmacies.size());
                System.out.println("Displaying first " + displayCount + " pharmacies:");
                System.out.println("-".repeat(120));
                System.out.printf("%-10s | %-40s | %-30s | %-15s | %-15s%n",
                    "NCPDP ID", "Pharmacy Name", "City, State", "Type", "Status");
                System.out.println("-".repeat(120));
                
                for (int i = 0; i < displayCount; i++) {
                    Pharmacy p = pharmacies.get(i);
                    String location = p.getCity() + ", " + p.getState();
                    System.out.printf("%-10s | %-40s | %-30s | %-15s | %-15s%n",
                        p.getNcpdpId(),
                        truncate(p.getPharmacyName(), 40),
                        truncate(location, 30),
                        p.getPharmacyType(),
                        p.getStatus());
                }
                System.out.println("-".repeat(120));
                
                // Display statistics
                long activeCount = pharmacies.stream().filter(Pharmacy::isActive).count();
                long retailCount = pharmacies.stream().filter(Pharmacy::isRetail).count();
                long mailOrderCount = pharmacies.stream().filter(Pharmacy::isMailOrder).count();
                long specialtyCount = pharmacies.stream().filter(Pharmacy::isSpecialty).count();
                long ltcCount = pharmacies.stream().filter(Pharmacy::isLongTermCare).count();
                
                System.out.println();
                System.out.println("Pharmacy Statistics:");
                System.out.println("  Active:        " + String.format("%,d", activeCount) +
                    " (" + String.format("%.1f%%", (activeCount * 100.0 / count)) + ")");
                System.out.println();
                System.out.println("Type Distribution:");
                System.out.println("  Retail:        " + String.format("%,d", retailCount) +
                    " (" + String.format("%.1f%%", (retailCount * 100.0 / count)) + ")");
                System.out.println("  Mail Order:    " + String.format("%,d", mailOrderCount) +
                    " (" + String.format("%.1f%%", (mailOrderCount * 100.0 / count)) + ")");
                System.out.println("  Specialty:     " + String.format("%,d", specialtyCount) +
                    " (" + String.format("%.1f%%", (specialtyCount * 100.0 / count)) + ")");
                System.out.println("  Long-Term Care:" + String.format("%,d", ltcCount) +
                    " (" + String.format("%.1f%%", (ltcCount * 100.0 / count)) + ")");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to read pharmacies", e);
            System.err.println("❌ Failed to read pharmacies: " + e.getMessage());
        }
        printSeparator();
    }
    
    /**
     * UPDATE operation for Pharmacies
     */
    private static void updatePharmacy(DatabaseConnector connector) {
        printHeader("Updating a Pharmacy");
        PharmacyDAO dao = new PharmacyDAO(connector);
        
        try {
            // Find first pharmacy to update
            List<Pharmacy> pharmacies = dao.findAll();
            if (pharmacies.isEmpty()) {
                System.out.println("⚠️  No pharmacies found to update");
                printSeparator();
                return;
            }
            
            Pharmacy pharmacy = pharmacies.get(0);
            String originalName = pharmacy.getPharmacyName();
            boolean originalActive = pharmacy.isActive();
            
            // Update the pharmacy
            pharmacy.setPharmacyName(originalName + " (UPDATED)");
            pharmacy.setActive(!originalActive);
            
            boolean updated = dao.update(pharmacy);
            if (updated) {
                System.out.println("✅ Successfully updated pharmacy: " + pharmacy.getNcpdpId());
                System.out.println("   Old name: " + originalName);
                System.out.println("   New name: " + pharmacy.getPharmacyName());
                System.out.println("   Old active status: " + originalActive);
                System.out.println("   New active status: " + pharmacy.isActive());
            } else {
                System.out.println("⚠️  Pharmacy not found or not updated");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update pharmacy", e);
            System.err.println("❌ Failed to update pharmacy: " + e.getMessage());
        }
        printSeparator();
    }
    
    /**
     * DELETE operation for Pharmacies
     */
    private static void deletePharmacy(DatabaseConnector connector) {
        printHeader("Deleting a Pharmacy");
        PharmacyDAO dao = new PharmacyDAO(connector);
        
        try {
            // Find a pharmacy to delete
            List<Pharmacy> pharmacies = dao.findAll();
            if (pharmacies.isEmpty()) {
                System.out.println("⚠️  No pharmacies found to delete");
                printSeparator();
                return;
            }
            
            // Delete the last pharmacy
            Pharmacy pharmacyToDelete = pharmacies.get(pharmacies.size() - 1);
            String ncpdpId = pharmacyToDelete.getNcpdpId();
            String pharmacyName = pharmacyToDelete.getPharmacyName();
            
            boolean deleted = dao.delete(pharmacyToDelete.getPharmacyId());
            if (deleted) {
                System.out.println("✅ Successfully deleted pharmacy: " + ncpdpId);
                System.out.println("   Pharmacy name: " + pharmacyName);
            } else {
                System.out.println("⚠️  Pharmacy not found or not deleted");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete pharmacy", e);
            System.err.println("❌ Failed to delete pharmacy: " + e.getMessage());
        }
        printSeparator();
    }
    
    /**
     * Load pharmacies from CSV file
     * @return List of pharmacies, or null if loading fails
     */
    private static List<Pharmacy> loadPharmacies() {
        printHeader("Loading and Inserting Pharmacies");
        
        PharmacyConverter pharmacyConverter = new PharmacyConverter();
        try {
            List<Pharmacy> pharmacies = pharmacyConverter.loadAllPharmacies();
            System.out.println("✅ Loaded " + String.format("%,d", pharmacies.size()) + " pharmacies from CSV file");
            return pharmacies;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Failed to load pharmacies", ex);
            System.err.println("❌ Failed to load pharmacies: " + ex.getMessage());
            return null;
        }
    }
    
    /**
     * Insert pharmacies into database and report results using DAO
     * @param connector Database connector
     * @param pharmacies List of pharmacies to insert
     */
    private static void insertAndReportPharmacies(DatabaseConnector connector, List<Pharmacy> pharmacies) {
        PharmacyDAO dao = new PharmacyDAO(connector);
        
        System.out.println("📝 Inserting " + String.format("%,d", pharmacies.size()) + " pharmacies into database...");
        System.out.println("⏳ This may take a moment...");
        
        long startTime = System.currentTimeMillis();
        try {
            int inserted = dao.insertBatch(pharmacies);
            long totalTime = System.currentTimeMillis() - startTime;
            double seconds = totalTime / 1000.0;
            double recordsPerSecond = inserted / seconds;
            
            System.out.println("✅ Successfully inserted " + String.format("%,d", inserted) + " pharmacies");
            System.out.println("⏱️  Total time: " + String.format("%.2f", seconds) + " seconds");
            System.out.println("🚀 Throughput: " + String.format("%,.0f", recordsPerSecond) + " records/sec");
            
            // Display count
            long totalCount = dao.count();
            System.out.println("📊 Total pharmacies in database: " + String.format("%,d", totalCount));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to insert pharmacies", e);
            System.err.println("❌ Failed to insert pharmacies: " + e.getMessage());
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
    
    /**
     * Load drugs from CSV file
     * @return List of drugs, or null if loading fails
     */
    private static List<Drug> loadDrugs() {
        printHeader("Loading and Inserting Drugs");
        
        DrugConverter drugConverter = new DrugConverter();
        try {
            List<Drug> drugs = drugConverter.loadAllDrugs();
            System.out.println("✅ Loaded " + String.format("%,d", drugs.size()) + " drugs from CSV file");
            return drugs;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Failed to load drugs", ex);
            System.err.println("❌ Failed to load drugs: " + ex.getMessage());
            return null;
        }
    }
    
    /**
     * Insert drugs into database and report results using DAO
     * @param connector Database connector
     * @param drugs List of drugs to insert
     */
    private static void insertAndReportDrugs(DatabaseConnector connector, List<Drug> drugs) {
        DrugDAO dao = new DrugDAO(connector);
        
        System.out.println("📝 Inserting " + String.format("%,d", drugs.size()) + " drugs into database...");
        System.out.println("⏳ This may take a moment...");
        
        long startTime = System.currentTimeMillis();
        try {
            int inserted = dao.insertBatch(drugs);
            long totalTime = System.currentTimeMillis() - startTime;
            double seconds = totalTime / 1000.0;
            double recordsPerSecond = inserted / seconds;
            
            System.out.println("✅ Successfully inserted " + String.format("%,d", inserted) + " drugs");
            System.out.println("⏱️  Total time: " + String.format("%.2f", seconds) + " seconds");
            System.out.println("🚀 Throughput: " + String.format("%,.0f", recordsPerSecond) + " records/sec");
            
            // Display count
            long totalCount = dao.count();
            System.out.println("📊 Total drugs in database: " + String.format("%,d", totalCount));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to insert drugs", e);
            System.err.println("❌ Failed to insert drugs: " + e.getMessage());
        }
        printSeparator();
    }
    
    /**
     * CREATE operation for Enrollments
     */
    private static void createEnrollments(DatabaseConnector connector) {
        List<Enrollment> enrollments = loadEnrollments();
        if (enrollments != null) {
            insertAndReportEnrollments(connector, enrollments);
        }
    }
    
    /**
     * READ operation for Enrollments
     */
    private static void readEnrollments(DatabaseConnector connector) {
        printHeader("Reading Enrollments");
        EnrollmentDAO dao = new EnrollmentDAO(connector);
        
        try {
            long totalCount = dao.count();
            long activeCount = dao.countActive();
            
            System.out.println("📖 Total enrollments: " + String.format("%,d", totalCount));
            System.out.println("✅ Active enrollments: " + String.format("%,d", activeCount));
            System.out.println("📊 Inactive enrollments: " + String.format("%,d", (totalCount - activeCount)));
            
            if (totalCount > 0) {
                System.out.println();
                System.out.println("Sample enrollments (first 5):");
                List<Enrollment> enrollments = dao.findAll();
                int displayCount = Math.min(5, enrollments.size());
                System.out.println("-".repeat(100));
                System.out.printf("%-15s | %-15s | %-15s | %-12s | %-12s | %-10s%n",
                    "Member", "Plan", "Group", "Effective", "Termination", "Active");
                System.out.println("-".repeat(100));
                
                for (int i = 0; i < displayCount; i++) {
                    Enrollment e = enrollments.get(i);
                    System.out.printf("%-15s | %-15s | %-15s | %-12s | %-12s | %-10s%n",
                        truncate(e.getMemberNumber(), 15),
                        truncate(e.getPlanCode(), 15),
                        truncate(e.getGroupNumber(), 15),
                        e.getEffectiveDate(),
                        e.getTerminationDate() != null ? e.getTerminationDate().toString() : "N/A",
                        e.isActive() ? "Yes" : "No");
                }
                System.out.println("-".repeat(100));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to read enrollments", e);
            System.err.println("❌ Failed to read enrollments: " + e.getMessage());
        }
        printSeparator();
    }
    
    /**
     * UPDATE operation for Enrollments
     */
    private static void updateEnrollment(DatabaseConnector connector) {
        printHeader("Updating an Enrollment");
        System.out.println("⚠️  Enrollment UPDATE operation not yet implemented");
        System.out.println("   (Would update termination date or active status)");
        printSeparator();
    }
    
    /**
     * DELETE operation for Enrollments
     */
    private static void deleteEnrollment(DatabaseConnector connector) {
        printHeader("Deleting an Enrollment");
        System.out.println("⚠️  Enrollment DELETE operation not yet implemented");
        System.out.println("   (Would delete a specific enrollment record)");
        printSeparator();
    }
    
    /**
     * Load enrollments from CSV files
     * @return List of enrollments, or null if loading fails
     */
    private static List<Enrollment> loadEnrollments() {
        printHeader("Loading and Inserting Enrollments");
        
        EnrollmentConverter enrollmentConverter = new EnrollmentConverter();
        try {
            System.out.println("📂 Scanning for enrollment CSV files...");
            int fileCount = enrollmentConverter.getAvailableFileCount();
            System.out.println("✅ Found " + fileCount + " enrollment CSV files");
            
            System.out.println("📖 Loading enrollments from all files...");
            System.out.println("⏳ This may take a few minutes for 10 million records...");
            
            List<Enrollment> enrollments = enrollmentConverter.loadAllEnrollments();
            System.out.println("✅ Loaded " + String.format("%,d", enrollments.size()) + " enrollments from CSV files");
            return enrollments;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Failed to load enrollments", ex);
            System.err.println("❌ Failed to load enrollments: " + ex.getMessage());
            return null;
        }
    }
    
    /**
     * Insert enrollments into database and report results using DAO
     * @param connector Database connector
     * @param enrollments List of enrollments to insert
     */
    private static void insertAndReportEnrollments(DatabaseConnector connector, List<Enrollment> enrollments) {
        EnrollmentDAO dao = new EnrollmentDAO(connector);
        
        System.out.println("📝 Inserting " + String.format("%,d", enrollments.size()) + " enrollments into database...");
        System.out.println("⏳ This will take several minutes for large datasets...");
        System.out.println("💡 Progress updates every 10,000 records");
        
        long startTime = System.currentTimeMillis();
        try {
            int inserted = dao.insertBatch(enrollments);
            long totalTime = System.currentTimeMillis() - startTime;
            double seconds = totalTime / 1000.0;
            double recordsPerSecond = inserted / seconds;
            
            System.out.println("✅ Successfully inserted " + String.format("%,d", inserted) + " enrollments");
            System.out.println("⏱️  Total time: " + String.format("%.2f", seconds) + " seconds");
            System.out.println("🚀 Throughput: " + String.format("%,.0f", recordsPerSecond) + " records/sec");
            
            // Display counts
            long totalCount = dao.count();
            long activeCount = dao.countActive();
            System.out.println("📊 Total enrollments in database: " + String.format("%,d", totalCount));
            System.out.println("✅ Active enrollments: " + String.format("%,d", activeCount));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to insert enrollments", e);
            System.err.println("❌ Failed to insert enrollments: " + e.getMessage());
            e.printStackTrace();
        }
        printSeparator();
    }
    
    /**
     * CREATE operation for Formularies
     */
    private static void createFormularies(DatabaseConnector connector) {
        List<Formulary> formularies = loadFormularies(connector);
        if (formularies != null) {
            insertAndReportFormularies(connector, formularies);
        }
    }
    
    /**
     * READ operation for Formularies
     */
    private static void readFormularies(DatabaseConnector connector) {
        printHeader("Reading Formularies");
        FormularyDAO dao = new FormularyDAO(connector);
        
        try {
            long count = dao.count();
            System.out.println("📖 Found " + String.format("%,d", count) + " formularies in database");
            System.out.println();
            
            if (count > 0) {
                List<Formulary> formularies = dao.findAll();
                
                // Display first 10 formularies
                int displayCount = Math.min(10, formularies.size());
                System.out.println("Displaying first " + displayCount + " formularies:");
                System.out.println("-".repeat(120));
                System.out.printf("%-50s | %-15s | %-12s | %-12s | %-10s%n",
                    "Formulary Name", "Plan ID", "Effective", "Termination", "Status");
                System.out.println("-".repeat(120));
                
                for (int i = 0; i < displayCount; i++) {
                    Formulary f = formularies.get(i);
                    System.out.printf("%-50s | %-15s | %-12s | %-12s | %-10s%n",
                        truncate(f.getFormularyName(), 50),
                        f.getPlanId().toString().substring(0, 13) + "...",
                        f.getEffectiveDate() != null ? f.getEffectiveDate().toString() : "N/A",
                        f.getTerminationDate() != null ? f.getTerminationDate().toString() : "N/A",
                        f.getStatus());
                }
                System.out.println("-".repeat(120));
                
                // Display statistics
                long activeCount = formularies.stream().filter(Formulary::isActive).count();
                long currentlyActiveCount = formularies.stream().filter(Formulary::isCurrentlyActive).count();
                long expiredCount = formularies.stream().filter(Formulary::isExpired).count();
                long futureCount = formularies.stream().filter(Formulary::isFutureDated).count();
                
                System.out.println();
                System.out.println("Formulary Status Distribution:");
                System.out.println("  Active (flag):     " + String.format("%,d", activeCount) +
                    " (" + String.format("%.1f%%", (activeCount * 100.0 / count)) + ")");
                System.out.println("  Currently Active:  " + String.format("%,d", currentlyActiveCount) +
                    " (" + String.format("%.1f%%", (currentlyActiveCount * 100.0 / count)) + ")");
                System.out.println("  Expired:           " + String.format("%,d", expiredCount) +
                    " (" + String.format("%.1f%%", (expiredCount * 100.0 / count)) + ")");
                System.out.println("  Future-dated:      " + String.format("%,d", futureCount) +
                    " (" + String.format("%.1f%%", (futureCount * 100.0 / count)) + ")");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to read formularies", e);
            System.err.println("❌ Failed to read formularies: " + e.getMessage());
        }
        printSeparator();
    }
    
    /**
     * UPDATE operation for Formularies
     */
    private static void updateFormulary(DatabaseConnector connector) {
        printHeader("Updating a Formulary");
        FormularyDAO dao = new FormularyDAO(connector);
        
        try {
            // Find first formulary to update
            List<Formulary> formularies = dao.findAll();
            if (formularies.isEmpty()) {
                System.out.println("⚠️  No formularies found to update");
                printSeparator();
                return;
            }
            
            Formulary formulary = formularies.get(0);
            String originalName = formulary.getFormularyName();
            boolean originalActive = formulary.isActive();
            
            // Update the formulary
            formulary.setFormularyName(originalName + " (UPDATED)");
            formulary.setActive(!originalActive);
            
            boolean updated = dao.update(formulary);
            if (updated) {
                System.out.println("✅ Successfully updated formulary");
                System.out.println("   Old name: " + originalName);
                System.out.println("   New name: " + formulary.getFormularyName());
                System.out.println("   Old active status: " + originalActive);
                System.out.println("   New active status: " + formulary.isActive());
            } else {
                System.out.println("⚠️  Formulary not found or not updated");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update formulary", e);
            System.err.println("❌ Failed to update formulary: " + e.getMessage());
        }
        printSeparator();
    }
    
    /**
     * DELETE operation for Formularies
     */
    private static void deleteFormulary(DatabaseConnector connector) {
        printHeader("Deleting a Formulary");
        FormularyDAO dao = new FormularyDAO(connector);
        
        try {
            // Find a formulary to delete
            List<Formulary> formularies = dao.findAll();
            if (formularies.isEmpty()) {
                System.out.println("⚠️  No formularies found to delete");
                printSeparator();
                return;
            }
            
            // Delete the last formulary
            Formulary formularyToDelete = formularies.get(formularies.size() - 1);
            String formularyName = formularyToDelete.getFormularyName();
            
            boolean deleted = dao.delete(formularyToDelete.getFormularyId());
            if (deleted) {
                System.out.println("✅ Successfully deleted formulary: " + formularyName);
            } else {
                System.out.println("⚠️  Formulary not found or not deleted");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete formulary", e);
            System.err.println("❌ Failed to delete formulary: " + e.getMessage());
        }
        printSeparator();
    }
    
    /**
     * Load formularies from CSV file
     * NOTE: Plans must be loaded first as formularies reference plan_id
     * @return List of formularies, or null if loading fails
     */
    private static List<Formulary> loadFormularies(DatabaseConnector connector) {
        printHeader("Loading and Inserting Formularies");
        
        // Check if plans exist first
        BenefitPlanDAO planDAO = new BenefitPlanDAO(connector);
        try {
            long planCount = planDAO.count();
            if (planCount == 0) {
                System.err.println("❌ ERROR: No plans found in database!");
                System.err.println("   Formularies require existing plans (foreign key constraint).");
                System.err.println("   Please run 'make run-create-plan' first to load plans.");
                return null;
            }
            System.out.println("✓ Found " + planCount + " plans in database");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to check plan count", e);
            System.err.println("❌ Failed to verify plans exist: " + e.getMessage());
            return null;
        }
        
        FormularyConverter formularyConverter = new FormularyConverter();
        try {
            List<Formulary> formularies = formularyConverter.loadAllFormularies();
            System.out.println("✅ Loaded " + String.format("%,d", formularies.size()) + " formularies from CSV file");
            System.out.println("💡 Plan references will be resolved via database JOIN during insert");
            
            return formularies;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Failed to load formularies", ex);
            System.err.println("❌ Failed to load formularies: " + ex.getMessage());
            return null;
        }
    }
    
    /**
     * Insert formularies into database and report results using DAO
     * @param connector Database connector
     * @param formularies List of formularies to insert
     */
    private static void insertAndReportFormularies(DatabaseConnector connector, List<Formulary> formularies) {
        FormularyDAO dao = new FormularyDAO(connector);
        
        System.out.println("📝 Inserting " + String.format("%,d", formularies.size()) + " formularies into database...");
        
        long startTime = System.currentTimeMillis();
        try {
            int inserted = dao.insertBatch(formularies);
            long totalTime = System.currentTimeMillis() - startTime;
            double seconds = totalTime / 1000.0;
            double recordsPerSecond = inserted / seconds;
            
            System.out.println("✅ Successfully inserted " + String.format("%,d", inserted) + " formularies");
            System.out.println("⏱️  Total time: " + String.format("%.2f", seconds) + " seconds");
            System.out.println("🚀 Throughput: " + String.format("%,.0f", recordsPerSecond) + " records/sec");
            
            // Display count
            long totalCount = dao.count();
            System.out.println("📊 Total formularies in database: " + String.format("%,d", totalCount));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to insert formularies", e);
            System.err.println("❌ Failed to insert formularies: " + e.getMessage());
        }
        printSeparator();
    }
    
    /**
     * CREATE operation for FormularyDrugs
     */
    private static void createFormularyDrugs(DatabaseConnector connector) {
        List<FormularyDrug> formularyDrugs = loadFormularyDrugs(connector);
        if (formularyDrugs != null) {
            insertAndReportFormularyDrugs(connector, formularyDrugs);
        }
    }
    
    /**
     * READ operation for FormularyDrugs
     */
    private static void readFormularyDrugs(DatabaseConnector connector) {
        printHeader("Reading Formulary-Drug Relationships");
        FormularyDrugDAO dao = new FormularyDrugDAO(connector);
        
        try {
            long count = dao.count();
            System.out.println("📖 Found " + String.format("%,d", count) + " formulary-drug relationships in database");
            System.out.println();
            
            if (count > 0) {
                // Display tier distribution
                System.out.println("Tier Distribution:");
                System.out.println("-".repeat(60));
                for (int tier = 1; tier <= 5; tier++) {
                    long tierCount = dao.countByTier(tier);
                    double pct = (tierCount * 100.0 / count);
                    System.out.printf("  Tier %d: %,12d (%5.1f%%)%n", tier, tierCount, pct);
                }
                System.out.println("-".repeat(60));
                
                // Display sample relationships
                System.out.println();
                System.out.println("Sample relationships (first 10):");
                List<FormularyDrug> formularyDrugs = dao.findAll();
                int displayCount = Math.min(10, formularyDrugs.size());
                System.out.println("-".repeat(120));
                System.out.printf("%-38s | %-38s | %-6s | %-15s | %-30s%n",
                    "Formulary ID", "Drug ID", "Tier", "Status", "Utilization Mgmt");
                System.out.println("-".repeat(120));
                
                for (int i = 0; i < displayCount; i++) {
                    FormularyDrug fd = formularyDrugs.get(i);
                    System.out.printf("%-38s | %-38s | %-6d | %-15s | %-30s%n",
                        fd.getFormularyId().toString().substring(0, 36) + "..",
                        fd.getDrugId().toString().substring(0, 36) + "..",
                        fd.getTier(),
                        fd.getStatus(),
                        truncate(fd.getUtilizationManagementSummary(), 30));
                }
                System.out.println("-".repeat(120));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to read formulary-drug relationships", e);
            System.err.println("❌ Failed to read formulary-drug relationships: " + e.getMessage());
        }
        printSeparator();
    }
    
    /**
     * UPDATE operation for FormularyDrugs
     */
    private static void updateFormularyDrug(DatabaseConnector connector) {
        printHeader("Updating a Formulary-Drug Relationship");
        FormularyDrugDAO dao = new FormularyDrugDAO(connector);
        
        try {
            // Find first formulary-drug relationship to update
            List<FormularyDrug> formularyDrugs = dao.findAll();
            if (formularyDrugs.isEmpty()) {
                System.out.println("⚠️  No formulary-drug relationships found to update");
                printSeparator();
                return;
            }
            
            FormularyDrug formularyDrug = formularyDrugs.get(0);
            int originalTier = formularyDrug.getTier();
            boolean originalPriorAuth = formularyDrug.isRequiresPriorAuth();
            
            // Update the relationship
            formularyDrug.setTier(Math.min(originalTier + 1, 5));
            formularyDrug.setRequiresPriorAuth(!originalPriorAuth);
            
            boolean updated = dao.update(formularyDrug);
            if (updated) {
                System.out.println("✅ Successfully updated formulary-drug relationship");
                System.out.println("   Old tier: " + originalTier);
                System.out.println("   New tier: " + formularyDrug.getTier());
                System.out.println("   Old prior auth: " + originalPriorAuth);
                System.out.println("   New prior auth: " + formularyDrug.isRequiresPriorAuth());
            } else {
                System.out.println("⚠️  Formulary-drug relationship not found or not updated");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update formulary-drug relationship", e);
            System.err.println("❌ Failed to update formulary-drug relationship: " + e.getMessage());
        }
        printSeparator();
    }
    
    /**
     * DELETE operation for FormularyDrugs
     */
    private static void deleteFormularyDrug(DatabaseConnector connector) {
        printHeader("Deleting a Formulary-Drug Relationship");
        FormularyDrugDAO dao = new FormularyDrugDAO(connector);
        
        try {
            // Find a formulary-drug relationship to delete
            List<FormularyDrug> formularyDrugs = dao.findAll();
            if (formularyDrugs.isEmpty()) {
                System.out.println("⚠️  No formulary-drug relationships found to delete");
                printSeparator();
                return;
            }
            
            // Delete the last relationship
            FormularyDrug formularyDrugToDelete = formularyDrugs.get(formularyDrugs.size() - 1);
            
            boolean deleted = dao.delete(formularyDrugToDelete.getFormularyDrugId());
            if (deleted) {
                System.out.println("✅ Successfully deleted formulary-drug relationship");
                System.out.println("   Tier: " + formularyDrugToDelete.getTier());
                System.out.println("   Status: " + formularyDrugToDelete.getStatus());
            } else {
                System.out.println("⚠️  Formulary-drug relationship not found or not deleted");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete formulary-drug relationship", e);
            System.err.println("❌ Failed to delete formulary-drug relationship: " + e.getMessage());
        }
        printSeparator();
    }
    
    /**
     * Load formulary-drug relationships from CSV files
     * NOTE: Both formularies and drugs must be loaded first (foreign key constraints)
     * @return List of formulary-drug relationships, or null if loading fails
     */
    private static List<FormularyDrug> loadFormularyDrugs(DatabaseConnector connector) {
        printHeader("Loading and Inserting Formulary-Drug Relationships");
        
        // Check if formularies exist first
        FormularyDAO formularyDAO = new FormularyDAO(connector);
        try {
            long formularyCount = formularyDAO.count();
            if (formularyCount == 0) {
                System.err.println("❌ ERROR: No formularies found in database!");
                System.err.println("   Formulary-drug relationships require existing formularies (foreign key constraint).");
                System.err.println("   Please run 'make run-create-formulary' first to load formularies.");
                return null;
            }
            System.out.println("✓ Found " + String.format("%,d", formularyCount) + " formularies in database");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to check formulary count", e);
            System.err.println("❌ Failed to verify formularies exist: " + e.getMessage());
            return null;
        }
        
        // Check if drugs exist
        DrugDAO drugDAO = new DrugDAO(connector);
        try {
            long drugCount = drugDAO.count();
            if (drugCount == 0) {
                System.err.println("❌ ERROR: No drugs found in database!");
                System.err.println("   Formulary-drug relationships require existing drugs (foreign key constraint).");
                System.err.println("   Please run 'make run-create-drug' first to load drugs.");
                return null;
            }
            System.out.println("✓ Found " + String.format("%,d", drugCount) + " drugs in database");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to check drug count", e);
            System.err.println("❌ Failed to verify drugs exist: " + e.getMessage());
            return null;
        }
        
        FormularyDrugConverter formularyDrugConverter = new FormularyDrugConverter();
        try {
            System.out.println("📂 Scanning for formulary-drug CSV files...");
            int fileCount = formularyDrugConverter.getAvailableFileCount();
            System.out.println("✅ Found " + fileCount + " formulary-drug CSV files");
            
            System.out.println("📖 Loading formulary-drug relationships from all files...");
            System.out.println("⏳ This may take several minutes for 10 million records...");
            
            List<FormularyDrug> formularyDrugs = formularyDrugConverter.loadAllFormularyDrugs();
            System.out.println("✅ Loaded " + String.format("%,d", formularyDrugs.size()) + " formulary-drug relationships from CSV files");
            System.out.println("💡 Foreign key references will be resolved via database JOINs during insert");
            
            return formularyDrugs;
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Failed to load formulary-drug relationships", ex);
            System.err.println("❌ Failed to load formulary-drug relationships: " + ex.getMessage());
            return null;
        }
    }
    
    /**
     * Insert formulary-drug relationships into database and report results using DAO
     * @param connector Database connector
     * @param formularyDrugs List of formulary-drug relationships to insert
     */
    private static void insertAndReportFormularyDrugs(DatabaseConnector connector, List<FormularyDrug> formularyDrugs) {
        FormularyDrugDAO dao = new FormularyDrugDAO(connector);
        
        System.out.println("📝 Inserting " + String.format("%,d", formularyDrugs.size()) + " formulary-drug relationships into database...");
        System.out.println("⏳ This will take several minutes for large datasets...");
        System.out.println("💡 Progress updates every 10,000 records");
        
        long startTime = System.currentTimeMillis();
        try {
            int inserted = dao.insertBatch(formularyDrugs);
            long totalTime = System.currentTimeMillis() - startTime;
            double seconds = totalTime / 1000.0;
            double recordsPerSecond = inserted / seconds;
            
            System.out.println("✅ Successfully inserted " + String.format("%,d", inserted) + " formulary-drug relationships");
            System.out.println("⏱️  Total time: " + String.format("%.2f", seconds) + " seconds");
            System.out.println("🚀 Throughput: " + String.format("%,.0f", recordsPerSecond) + " records/sec");
            
            // Display count
            long totalCount = dao.count();
            System.out.println("📊 Total formulary-drug relationships in database: " + String.format("%,d", totalCount));
            
            // Display tier distribution
            System.out.println();
            System.out.println("Tier Distribution:");
            for (int tier = 1; tier <= 5; tier++) {
                long tierCount = dao.countByTier(tier);
                double pct = (tierCount * 100.0 / totalCount);
                System.out.printf("  Tier %d: %,12d (%5.1f%%)%n", tier, tierCount, pct);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to insert formulary-drug relationships", e);
            System.err.println("❌ Failed to insert formulary-drug relationships: " + e.getMessage());
            e.printStackTrace();
        }
        printSeparator();
    }
}
