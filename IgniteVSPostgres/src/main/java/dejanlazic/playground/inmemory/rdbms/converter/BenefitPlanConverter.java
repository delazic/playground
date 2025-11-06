package dejanlazic.playground.inmemory.rdbms.converter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import dejanlazic.playground.inmemory.rdbms.model.BenefitPlan;

/**
 * Converter to read and parse benefit plans from CSV file
 * Reads from us_pharmacy_plans.csv in classpath resources
 */
public class BenefitPlanConverter {
    
    private static final String CSV_FILE = "us_pharmacy_plans.csv";
    private static final String CSV_DELIMITER = ",";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * Load all benefit plans from CSV file
     * 
     * @return List of BenefitPlan objects
     * @throws IOException if file cannot be read
     */
    public List<BenefitPlan> loadAllPlans() throws IOException {
        List<BenefitPlan> plans = new ArrayList<>();
        
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CSV_FILE)) {
            if (inputStream == null) {
                throw new IOException("CSV file not found: " + CSV_FILE);
            }
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                // Skip header line
                String headerLine = reader.readLine();
                if (headerLine == null) {
                    throw new IOException("CSV file is empty");
                }
                
                String line;
                int lineNumber = 1;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    try {
                        BenefitPlan plan = parseLine(line);
                        plans.add(plan);
                    } catch (Exception e) {
                        throw new IOException("Error parsing line " + lineNumber + ": " + e.getMessage(), e);
                    }
                }
            }
        }
        
        return plans;
    }
    
    /**
     * Parse a single CSV line into a BenefitPlan object
     * 
     * @param line CSV line to parse
     * @return BenefitPlan object
     */
    public BenefitPlan parseLine(String line) {
        String[] fields = line.split(CSV_DELIMITER, -1); // -1 to keep trailing empty strings
        
        if (fields.length < 20) {
            throw new IllegalArgumentException("Invalid CSV line: expected at least 20 fields, got " + fields.length);
        }
        
        BenefitPlan plan = new BenefitPlan();
        
        // Basic plan information
        plan.setPlanCode(fields[0].trim());
        plan.setPlanName(fields[1].trim());
        plan.setPlanType(fields[2].trim());
        plan.setPlanCategory(fields[3].trim());
        plan.setEffectiveDate(parseDate(fields[4]));
        
        // Financial information
        plan.setAnnualDeductible(parseBigDecimal(fields[5]));
        plan.setOutOfPocketMax(parseBigDecimal(fields[6]));
        
        // Tier copays
        plan.setTier1Copay(parseBigDecimal(fields[7]));
        plan.setTier2Copay(parseBigDecimal(fields[8]));
        plan.setTier3Copay(parseBigDecimal(fields[9]));
        plan.setTier4Copay(parseBigDecimal(fields[10]));
        plan.setTier5Copay(parseBigDecimal(fields[11]));
        
        // Tier coinsurance
        plan.setTier1Coinsurance(parseBigDecimal(fields[12]));
        plan.setTier2Coinsurance(parseBigDecimal(fields[13]));
        plan.setTier3Coinsurance(parseBigDecimal(fields[14]));
        plan.setTier4Coinsurance(parseBigDecimal(fields[15]));
        plan.setTier5Coinsurance(parseBigDecimal(fields[16]));
        
        // Boolean flags
        plan.setMailOrderAvailable(parseBoolean(fields[17]));
        plan.setSpecialtyPharmacyRequired(parseBoolean(fields[18]));
        
        // Description
        plan.setDescription(fields[19].trim());
        
        return plan;
    }
    
    /**
     * Parse date string in yyyy-MM-dd format
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(dateStr.trim(), DATE_FORMATTER);
    }
    
    /**
     * Parse BigDecimal from string
     * Returns BigDecimal.ZERO for empty or "0" values
     */
    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format: " + value, e);
        }
    }
    
    /**
     * Parse boolean from string
     * Accepts: TRUE/FALSE, true/false, 1/0, yes/no
     */
    private boolean parseBoolean(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        String normalized = value.trim().toUpperCase();
        return normalized.equals("TRUE") || 
               normalized.equals("1") || 
               normalized.equals("YES");
    }
    
    /**
     * Find a plan by plan code
     * 
     * @param planCode Plan code to search for
     * @return BenefitPlan if found, null otherwise
     * @throws IOException if file cannot be read
     */
    public BenefitPlan findByPlanCode(String planCode) throws IOException {
        List<BenefitPlan> plans = loadAllPlans();
        return plans.stream()
                .filter(plan -> plan.getPlanCode().equals(planCode))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Find plans by plan type
     * 
     * @param planType Plan type to search for (e.g., COMMERCIAL, MEDICARE)
     * @return List of matching plans
     * @throws IOException if file cannot be read
     */
    public List<BenefitPlan> findByPlanType(String planType) throws IOException {
        List<BenefitPlan> plans = loadAllPlans();
        return plans.stream()
                .filter(plan -> plan.getPlanType().equalsIgnoreCase(planType))
                .toList();
    }
    
    /**
     * Find plans by plan category
     * 
     * @param planCategory Plan category to search for (e.g., GOLD, SILVER)
     * @return List of matching plans
     * @throws IOException if file cannot be read
     */
    public List<BenefitPlan> findByPlanCategory(String planCategory) throws IOException {
        List<BenefitPlan> plans = loadAllPlans();
        return plans.stream()
                .filter(plan -> plan.getPlanCategory().equalsIgnoreCase(planCategory))
                .toList();
    }
    
    /**
     * Get count of plans in CSV file
     * 
     * @return Number of plans
     * @throws IOException if file cannot be read
     */
    public int getPlanCount() throws IOException {
        return loadAllPlans().size();
    }
    
    /**
     * Print plan summary to console
     * 
     * @param plan Plan to print
     */
    public void printPlanSummary(BenefitPlan plan) {
        System.out.println("=".repeat(70));
        System.out.println("Plan Code: " + plan.getPlanCode());
        System.out.println("Plan Name: " + plan.getPlanName());
        System.out.println("Type: " + plan.getPlanType() + " / Category: " + plan.getPlanCategory());
        System.out.println("Effective Date: " + plan.getEffectiveDate());
        System.out.println("-".repeat(70));
        System.out.println("Annual Deductible: $" + plan.getAnnualDeductible());
        System.out.println("Out-of-Pocket Max: $" + plan.getOutOfPocketMax());
        System.out.println("-".repeat(70));
        System.out.println("Tier Copays:");
        System.out.println("  Tier 1 (Generic):           $" + plan.getTier1Copay());
        System.out.println("  Tier 2 (Preferred Brand):   $" + plan.getTier2Copay());
        System.out.println("  Tier 3 (Non-Preferred):     $" + plan.getTier3Copay());
        System.out.println("  Tier 4 (Specialty):         $" + plan.getTier4Copay());
        System.out.println("  Tier 5 (Specialty Biologic): $" + plan.getTier5Copay());
        System.out.println("-".repeat(70));
        System.out.println("Tier Coinsurance:");
        System.out.println("  Tier 1: " + formatPercentage(plan.getTier1Coinsurance()));
        System.out.println("  Tier 2: " + formatPercentage(plan.getTier2Coinsurance()));
        System.out.println("  Tier 3: " + formatPercentage(plan.getTier3Coinsurance()));
        System.out.println("  Tier 4: " + formatPercentage(plan.getTier4Coinsurance()));
        System.out.println("  Tier 5: " + formatPercentage(plan.getTier5Coinsurance()));
        System.out.println("-".repeat(70));
        System.out.println("Mail Order Available: " + (plan.isMailOrderAvailable() ? "Yes" : "No"));
        System.out.println("Specialty Pharmacy Required: " + (plan.isSpecialtyPharmacyRequired() ? "Yes" : "No"));
        System.out.println("-".repeat(70));
        System.out.println("Description: " + plan.getDescription());
        System.out.println("=".repeat(70));
    }
    
    /**
     * Format coinsurance as percentage
     */
    private String formatPercentage(BigDecimal coinsurance) {
        if (coinsurance == null || coinsurance.compareTo(BigDecimal.ZERO) == 0) {
            return "0%";
        }
        return coinsurance.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString() + "%";
    }
}
