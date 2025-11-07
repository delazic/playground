package dejanlazic.playground.inmemory.rdbms.converter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import dejanlazic.playground.inmemory.rdbms.model.Formulary;

/**
 * Converter to read and parse formularies from CSV file
 * Reads from us_pharmacy_formularies.csv in classpath resources
 * Similar to EnrollmentConverter, stores planCode as business key
 * The DAO will resolve planCode to plan_id UUID via database JOIN
 */
public class FormularyConverter {
    
    private static final Logger LOGGER = Logger.getLogger(FormularyConverter.class.getName());
    private static final String CSV_FILE = "data/us_pharmacy_formularies.csv";
    private static final String CSV_DELIMITER = ",";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    
    /**
     * Load all formularies from CSV file
     *
     * @return List of Formulary objects
     * @throws IOException if file cannot be read
     */
    public List<Formulary> loadAllFormularies() throws IOException {
        List<Formulary> formularies = new ArrayList<>();
        
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CSV_FILE)) {
            if (inputStream == null) {
                throw new IOException("CSV file not found in classpath: " + CSV_FILE);
            }
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                // Skip header line
                String headerLine = reader.readLine();
                if (headerLine == null) {
                    throw new IOException("CSV file is empty: " + CSV_FILE);
                }
                
                String line;
                int lineNumber = 1;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    try {
                        Formulary formulary = parseLine(line);
                        formularies.add(formulary);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error parsing line {0}: {1}",
                            new Object[]{lineNumber, e.getMessage()});
                        // Continue processing other lines
                    }
                }
            }
        }
        
        LOGGER.log(Level.INFO, "Total formularies loaded: {0}", formularies.size());
        return formularies;
    }
    
    /**
     * Parse a single CSV line into a Formulary object
     *
     * CSV format: formulary_code,formulary_name,plan_code,market_segment,carrier,pbm,
     *             formulary_type,tier_count,coverage_level,effective_date,termination_date,
     *             region,drug_count,prior_auth_pct,step_therapy_pct,quantity_limit_pct,is_active
     *
     * @param line CSV line to parse
     * @return Formulary object
     */
    public Formulary parseLine(String line) {
        String[] fields = line.split(CSV_DELIMITER, -1); // -1 to keep trailing empty strings
        
        if (fields.length < 17) {
            throw new IllegalArgumentException("Invalid CSV line: expected 17 fields, got " + fields.length);
        }
        
        Formulary formulary = new Formulary();
        
        // Set formulary_code (unique identifier)
        formulary.setFormularyCode(fields[0].trim());
        
        // Set plan_code (business key) - will be resolved to plan_id by DAO
        String planCode = fields[2].trim();
        formulary.setPlanCode(planCode);
        
        // Set formulary name
        formulary.setFormularyName(fields[1].trim());
        
        // Parse dates
        formulary.setEffectiveDate(parseDate(fields[9]));
        formulary.setTerminationDate(parseDate(fields[10]));
        
        // Parse is_active flag
        formulary.setActive(parseBoolean(fields[16]));
        
        return formulary;
    }
    
    /**
     * Parse date from string (yyyy-MM-dd format)
     */
    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            LOGGER.log(Level.WARNING, "Invalid date value: {0}", value);
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
     * Find formularies by plan ID
     *
     * @param planId Plan ID to search for
     * @return List of matching formularies
     * @throws IOException if file cannot be read
     */
    public List<Formulary> findByPlanId(UUID planId) throws IOException {
        List<Formulary> formularies = loadAllFormularies();
        return formularies.stream()
                .filter(formulary -> formulary.getPlanId().equals(planId))
                .toList();
    }
    
    /**
     * Find formularies by name (case-insensitive partial match)
     *
     * @param name Formulary name to search for
     * @return List of matching formularies
     * @throws IOException if file cannot be read
     */
    public List<Formulary> findByName(String name) throws IOException {
        List<Formulary> formularies = loadAllFormularies();
        String searchTerm = name.toLowerCase();
        return formularies.stream()
                .filter(formulary -> formulary.getFormularyName().toLowerCase().contains(searchTerm))
                .toList();
    }
    
    /**
     * Find active formularies only
     *
     * @return List of active formularies
     * @throws IOException if file cannot be read
     */
    public List<Formulary> findActiveFormularies() throws IOException {
        List<Formulary> formularies = loadAllFormularies();
        return formularies.stream()
                .filter(Formulary::isActive)
                .toList();
    }
    
    /**
     * Find currently active formularies (based on dates)
     *
     * @return List of currently active formularies
     * @throws IOException if file cannot be read
     */
    public List<Formulary> findCurrentlyActiveFormularies() throws IOException {
        List<Formulary> formularies = loadAllFormularies();
        return formularies.stream()
                .filter(Formulary::isCurrentlyActive)
                .toList();
    }
    
    /**
     * Find formularies effective on a specific date
     *
     * @param date Date to check
     * @return List of formularies effective on the date
     * @throws IOException if file cannot be read
     */
    public List<Formulary> findEffectiveOnDate(LocalDate date) throws IOException {
        List<Formulary> formularies = loadAllFormularies();
        return formularies.stream()
                .filter(f -> f.getEffectiveDate() != null &&
                            !date.isBefore(f.getEffectiveDate()) &&
                            (f.getTerminationDate() == null || date.isBefore(f.getTerminationDate())))
                .toList();
    }
    
    /**
     * Get count of formularies in CSV file
     *
     * @return Total number of formularies
     * @throws IOException if file cannot be read
     */
    public int getFormularyCount() throws IOException {
        return loadAllFormularies().size();
    }
    
    /**
     * Get statistics about loaded formularies
     *
     * @return Statistics string
     * @throws IOException if file cannot be read
     */
    public String getStatistics() throws IOException {
        List<Formulary> formularies = loadAllFormularies();
        
        long activeCount = formularies.stream().filter(Formulary::isActive).count();
        long currentlyActiveCount = formularies.stream().filter(Formulary::isCurrentlyActive).count();
        long expiredCount = formularies.stream().filter(Formulary::isExpired).count();
        long futureCount = formularies.stream().filter(Formulary::isFutureDated).count();
        
        long uniquePlans = formularies.stream()
                .map(Formulary::getPlanId)
                .distinct()
                .count();
        
        return String.format("""
            Formulary Statistics:
            --------------------
            Total Formularies: %,d
            Unique Plans: %,d
            Status Distribution:
              Active (flag):        %,d (%.1f%%)
              Currently Active:     %,d (%.1f%%)
              Expired:              %,d (%.1f%%)
              Future-dated:         %,d (%.1f%%)
            """,
            formularies.size(),
            uniquePlans,
            activeCount, (activeCount * 100.0 / formularies.size()),
            currentlyActiveCount, (currentlyActiveCount * 100.0 / formularies.size()),
            expiredCount, (expiredCount * 100.0 / formularies.size()),
            futureCount, (futureCount * 100.0 / formularies.size())
        );
    }
    
    /**
     * Print formulary summary to console
     *
     * @param formulary Formulary to print
     */
    public void printFormularySummary(Formulary formulary) {
        System.out.println("=".repeat(70));
        System.out.println("Formulary Name: " + formulary.getFormularyName());
        System.out.println("Plan ID: " + formulary.getPlanId());
        System.out.println("-".repeat(70));
        System.out.println("Effective Date: " + (formulary.getEffectiveDate() != null ? formulary.getEffectiveDate() : "N/A"));
        System.out.println("Termination Date: " + (formulary.getTerminationDate() != null ? formulary.getTerminationDate() : "N/A"));
        System.out.println("Status: " + formulary.getStatus());
        System.out.println("Is Active: " + formulary.isActive());
        if (formulary.isFutureDated()) {
            System.out.println("Days Until Effective: " + formulary.getDaysUntilEffective());
        }
        if (formulary.isCurrentlyActive() && formulary.getTerminationDate() != null) {
            System.out.println("Days Until Termination: " + formulary.getDaysUntilTermination());
        }
        System.out.println("=".repeat(70));
    }
    
}
