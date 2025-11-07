package dejanlazic.playground.inmemory.rdbms.converter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import dejanlazic.playground.inmemory.rdbms.model.FormularyDrug;

/**
 * Converter to read and parse formulary-drug relationships from CSV files
 * Reads from us_pharmacy_formularies_drugs_*.csv files in classpath resources
 * 
 * Similar to EnrollmentConverter, handles multiple CSV files (64 files)
 * Stores formularyCode and ndcCode as business keys
 * The DAO will resolve these to formulary_id and drug_id UUIDs via database JOINs
 */
public class FormularyDrugConverter {
    
    private static final Logger LOGGER = Logger.getLogger(FormularyDrugConverter.class.getName());
    private static final String CSV_FILE_PREFIX = "data/us_pharmacy_formularies_drugs_";
    private static final String CSV_FILE_SUFFIX = ".csv";
    private static final String CSV_DELIMITER = ",";
    private static final int TOTAL_FILES = 64;  // Total number of CSV files
    
    /**
     * Load all formulary-drug relationships from all CSV files
     *
     * @return List of FormularyDrug objects
     * @throws IOException if files cannot be read
     */
    public List<FormularyDrug> loadAllFormularyDrugs() throws IOException {
        List<FormularyDrug> allFormularyDrugs = new ArrayList<>();
        
        LOGGER.log(Level.INFO, "Loading formulary-drug relationships from {0} CSV files...", TOTAL_FILES);
        
        for (int fileNum = 1; fileNum <= TOTAL_FILES; fileNum++) {
            String fileName = String.format("%s%02d%s", CSV_FILE_PREFIX, fileNum, CSV_FILE_SUFFIX);
            
            try {
                List<FormularyDrug> formularyDrugs = loadFromFile(fileName);
                allFormularyDrugs.addAll(formularyDrugs);
                
                // Log progress every 10 files
                if (fileNum % 10 == 0) {
                    LOGGER.log(Level.INFO, "Loaded {0} files, total records: {1}",
                        new Object[]{fileNum, allFormularyDrugs.size()});
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to load file {0}: {1}",
                    new Object[]{fileName, e.getMessage()});
                // Continue with other files
            }
        }
        
        LOGGER.log(Level.INFO, "Total formulary-drug relationships loaded: {0}", allFormularyDrugs.size());
        return allFormularyDrugs;
    }
    
    /**
     * Load formulary-drug relationships from a single CSV file
     *
     * @param fileName Name of the CSV file to load
     * @return List of FormularyDrug objects from the file
     * @throws IOException if file cannot be read
     */
    public List<FormularyDrug> loadFromFile(String fileName) throws IOException {
        List<FormularyDrug> formularyDrugs = new ArrayList<>();
        
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new IOException("CSV file not found in classpath: " + fileName);
            }
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                // Skip header line
                String headerLine = reader.readLine();
                if (headerLine == null) {
                    throw new IOException("CSV file is empty: " + fileName);
                }
                
                String line;
                int lineNumber = 1;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    try {
                        FormularyDrug formularyDrug = parseLine(line);
                        formularyDrugs.add(formularyDrug);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error parsing line {0} in {1}: {2}",
                            new Object[]{lineNumber, fileName, e.getMessage()});
                        // Continue processing other lines
                    }
                }
            }
        }
        
        return formularyDrugs;
    }
    
    /**
     * Parse a single CSV line into a FormularyDrug object
     *
     * CSV format: formulary_drug_id,formulary_code,ndc_code,tier,status,
     *             requires_prior_auth,requires_step_therapy,quantity_limit,days_supply_limit
     *
     * @param line CSV line to parse
     * @return FormularyDrug object
     */
    public FormularyDrug parseLine(String line) {
        String[] fields = line.split(CSV_DELIMITER, -1); // -1 to keep trailing empty strings
        
        if (fields.length < 9) {
            throw new IllegalArgumentException("Invalid CSV line: expected 9 fields, got " + fields.length);
        }
        
        FormularyDrug formularyDrug = new FormularyDrug();
        
        // Skip formulary_drug_id (field 0) - will be generated by database
        
        // Set formulary_code and ndc_code (business keys) - will be resolved to UUIDs by DAO
        formularyDrug.setFormularyCode(fields[1].trim());
        formularyDrug.setNdcCode(fields[2].trim());
        
        // Parse tier
        formularyDrug.setTier(parseInt(fields[3]));
        
        // Parse status
        formularyDrug.setStatus(fields[4].trim());
        
        // Parse boolean flags
        formularyDrug.setRequiresPriorAuth(parseBoolean(fields[5]));
        formularyDrug.setRequiresStepTherapy(parseBoolean(fields[6]));
        
        // Parse optional integer fields
        formularyDrug.setQuantityLimit(parseOptionalInt(fields[7]));
        formularyDrug.setDaysSupplyLimit(parseOptionalInt(fields[8]));
        
        return formularyDrug;
    }
    
    /**
     * Parse integer from string
     */
    private int parseInt(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid integer value: {0}", value);
            return 0;
        }
    }
    
    /**
     * Parse optional integer from string (returns null if empty)
     */
    private Integer parseOptionalInt(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid integer value: {0}", value);
            return null;
        }
    }
    
    /**
     * Parse boolean from string (true/false)
     */
    private boolean parseBoolean(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        return "true".equalsIgnoreCase(value.trim());
    }
    
    /**
     * Get count of available CSV files
     *
     * @return Number of CSV files found
     */
    public int getAvailableFileCount() {
        int count = 0;
        for (int fileNum = 1; fileNum <= TOTAL_FILES; fileNum++) {
            String fileName = String.format("%s%02d%s", CSV_FILE_PREFIX, fileNum, CSV_FILE_SUFFIX);
            try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
                if (inputStream != null) {
                    count++;
                }
            } catch (IOException e) {
                // Ignore
            }
        }
        return count;
    }
    
    /**
     * Get statistics about loaded formulary-drug relationships
     *
     * @return Statistics string
     * @throws IOException if files cannot be read
     */
    public String getStatistics() throws IOException {
        List<FormularyDrug> formularyDrugs = loadAllFormularyDrugs();
        
        long tier1Count = formularyDrugs.stream().filter(fd -> fd.getTier() == 1).count();
        long tier2Count = formularyDrugs.stream().filter(fd -> fd.getTier() == 2).count();
        long tier3Count = formularyDrugs.stream().filter(fd -> fd.getTier() == 3).count();
        long tier4Count = formularyDrugs.stream().filter(fd -> fd.getTier() == 4).count();
        long tier5Count = formularyDrugs.stream().filter(fd -> fd.getTier() == 5).count();
        
        long preferredCount = formularyDrugs.stream().filter(FormularyDrug::isPreferred).count();
        long nonPreferredCount = formularyDrugs.stream().filter(FormularyDrug::isNonPreferred).count();
        long specialtyCount = formularyDrugs.stream().filter(FormularyDrug::isSpecialty).count();
        
        long priorAuthCount = formularyDrugs.stream().filter(FormularyDrug::isRequiresPriorAuth).count();
        long stepTherapyCount = formularyDrugs.stream().filter(FormularyDrug::isRequiresStepTherapy).count();
        long quantityLimitCount = formularyDrugs.stream().filter(FormularyDrug::hasQuantityLimits).count();
        
        return String.format("""
            Formulary-Drug Relationship Statistics:
            ---------------------------------------
            Total Relationships: %,d
            
            Tier Distribution:
              Tier 1 (Generic):           %,d (%.1f%%)
              Tier 2 (Preferred Brand):   %,d (%.1f%%)
              Tier 3 (Non-Preferred):     %,d (%.1f%%)
              Tier 4 (Specialty):         %,d (%.1f%%)
              Tier 5 (High-Cost):         %,d (%.1f%%)
            
            Status Distribution:
              Preferred:                  %,d (%.1f%%)
              Non-Preferred:              %,d (%.1f%%)
              Specialty:                  %,d (%.1f%%)
            
            Utilization Management:
              Prior Authorization:        %,d (%.1f%%)
              Step Therapy:               %,d (%.1f%%)
              Quantity Limits:            %,d (%.1f%%)
            """,
            formularyDrugs.size(),
            tier1Count, (tier1Count * 100.0 / formularyDrugs.size()),
            tier2Count, (tier2Count * 100.0 / formularyDrugs.size()),
            tier3Count, (tier3Count * 100.0 / formularyDrugs.size()),
            tier4Count, (tier4Count * 100.0 / formularyDrugs.size()),
            tier5Count, (tier5Count * 100.0 / formularyDrugs.size()),
            preferredCount, (preferredCount * 100.0 / formularyDrugs.size()),
            nonPreferredCount, (nonPreferredCount * 100.0 / formularyDrugs.size()),
            specialtyCount, (specialtyCount * 100.0 / formularyDrugs.size()),
            priorAuthCount, (priorAuthCount * 100.0 / formularyDrugs.size()),
            stepTherapyCount, (stepTherapyCount * 100.0 / formularyDrugs.size()),
            quantityLimitCount, (quantityLimitCount * 100.0 / formularyDrugs.size())
        );
    }
    
    /**
     * Print formulary-drug summary to console
     *
     * @param formularyDrug FormularyDrug to print
     */
    public void printFormularyDrugSummary(FormularyDrug formularyDrug) {
        System.out.println("=".repeat(70));
        System.out.println("Formulary Code: " + formularyDrug.getFormularyCode());
        System.out.println("NDC Code: " + formularyDrug.getNdcCode());
        System.out.println("-".repeat(70));
        System.out.println("Tier: " + formularyDrug.getTier() + " - " + formularyDrug.getTierDescription());
        System.out.println("Status: " + formularyDrug.getStatus());
        System.out.println("Utilization Management: " + formularyDrug.getUtilizationManagementSummary());
        System.out.println("=".repeat(70));
    }
}
