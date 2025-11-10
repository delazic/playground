package dejanlazic.playground.inmemory.rdbms.converter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import dejanlazic.playground.inmemory.rdbms.model.DrugInteraction;

/**
 * Converter to read and parse drug interactions from CSV file
 * Reads from us_pharmacy_drug_interactions_01.csv in classpath resources
 */
public class DrugInteractionConverter {
    
    private static final Logger LOGGER = Logger.getLogger(DrugInteractionConverter.class.getName());
    private static final String CSV_FILE = "data/us_pharmacy_drug_interactions_01.csv";
    private static final String CSV_DELIMITER = ",";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * Load all drug interactions from CSV file
     *
     * @return List of DrugInteraction objects
     * @throws IOException if file cannot be read
     */
    public List<DrugInteraction> loadAllDrugInteractions() throws IOException {
        List<DrugInteraction> interactions = new ArrayList<>();
        
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
                        DrugInteraction interaction = parseLine(line);
                        interactions.add(interaction);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error parsing line {0}: {1}",
                            new Object[]{lineNumber, e.getMessage()});
                        // Continue processing other lines
                    }
                }
            }
        }
        
        LOGGER.log(Level.INFO, "Total drug interactions loaded: {0}", interactions.size());
        return interactions;
    }
    
    /**
     * Parse a single CSV line into a DrugInteraction object
     * 
     * CSV format: interaction_id,drug_1_name,drug_1_ndc,drug_2_name,drug_2_ndc,severity_level,
     *             interaction_mechanism,clinical_effects,management_recommendation,evidence_level,
     *             onset_timing,documentation_source,requires_alert,requires_intervention,
     *             patient_counseling_required,prescriber_notification_required,last_reviewed_date,
     *             last_updated_date,active_status,reference_id,notes
     * 
     * @param line CSV line to parse
     * @return DrugInteraction object
     */
    public DrugInteraction parseLine(String line) {
        String[] fields = line.split(CSV_DELIMITER, -1); // -1 to keep trailing empty strings
        
        if (fields.length < 21) {
            throw new IllegalArgumentException("Invalid CSV line: expected 21 fields, got " + fields.length);
        }
        
        DrugInteraction interaction = new DrugInteraction();
        
        // Basic interaction information
        interaction.setInteractionCode(fields[0].trim());
        interaction.setDrug1Name(parseString(fields[1]));
        interaction.setDrug1Ndc(parseString(fields[2]));
        interaction.setDrug2Name(parseString(fields[3]));
        interaction.setDrug2Ndc(parseString(fields[4]));
        interaction.setSeverityLevel(parseString(fields[5]));
        interaction.setInteractionMechanism(parseString(fields[6]));
        interaction.setClinicalEffects(parseString(fields[7]));
        interaction.setManagementRecommendation(parseString(fields[8]));
        interaction.setEvidenceLevel(parseString(fields[9]));
        interaction.setOnsetTiming(parseString(fields[10]));
        interaction.setDocumentationSource(parseString(fields[11]));
        
        // Boolean flags
        interaction.setRequiresAlert(parseBoolean(fields[12]));
        interaction.setRequiresIntervention(parseBoolean(fields[13]));
        interaction.setPatientCounselingRequired(parseBoolean(fields[14]));
        interaction.setPrescriberNotificationRequired(parseBoolean(fields[15]));
        
        // Dates
        interaction.setLastReviewedDate(parseDate(fields[16]));
        interaction.setLastUpdatedDate(parseDate(fields[17]));
        
        // Status and reference
        interaction.setActiveStatus(parseString(fields[18]));
        interaction.setReferenceId(parseString(fields[19]));
        interaction.setNotes(parseString(fields[20]));
        
        return interaction;
    }
    
    /**
     * Parse string, returning null for empty strings
     */
    private String parseString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }
    
    /**
     * Parse boolean from string (Yes/No or true/false)
     */
    private boolean parseBoolean(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        String trimmed = value.trim();
        return "Yes".equalsIgnoreCase(trimmed) || "true".equalsIgnoreCase(trimmed);
    }
    
    /**
     * Parse LocalDate from string
     */
    private LocalDate parseDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DATE_FORMATTER);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Invalid date value: {0}", value);
            return null;
        }
    }
    
    /**
     * Find interactions by interaction code
     * 
     * @param interactionCode Interaction code to search for
     * @return DrugInteraction if found, null otherwise
     * @throws IOException if file cannot be read
     */
    public DrugInteraction findByInteractionCode(String interactionCode) throws IOException {
        List<DrugInteraction> interactions = loadAllDrugInteractions();
        return interactions.stream()
                .filter(interaction -> interaction.getInteractionCode().equals(interactionCode))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Find interactions involving a specific drug by NDC
     * 
     * @param ndcCode NDC code to search for
     * @return List of matching interactions
     * @throws IOException if file cannot be read
     */
    public List<DrugInteraction> findByDrugNdc(String ndcCode) throws IOException {
        List<DrugInteraction> interactions = loadAllDrugInteractions();
        return interactions.stream()
                .filter(interaction -> interaction.involvesDrug(ndcCode))
                .toList();
    }
    
    /**
     * Find interactions by severity level
     * 
     * @param severityLevel Severity level to search for
     * @return List of matching interactions
     * @throws IOException if file cannot be read
     */
    public List<DrugInteraction> findBySeverityLevel(String severityLevel) throws IOException {
        List<DrugInteraction> interactions = loadAllDrugInteractions();
        return interactions.stream()
                .filter(interaction -> severityLevel.equalsIgnoreCase(interaction.getSeverityLevel()))
                .toList();
    }
    
    /**
     * Find severe interactions only
     * 
     * @return List of severe interactions
     * @throws IOException if file cannot be read
     */
    public List<DrugInteraction> findSevereInteractions() throws IOException {
        List<DrugInteraction> interactions = loadAllDrugInteractions();
        return interactions.stream()
                .filter(DrugInteraction::isSevere)
                .toList();
    }
    
    /**
     * Find interactions that require alerts
     * 
     * @return List of interactions requiring alerts
     * @throws IOException if file cannot be read
     */
    public List<DrugInteraction> findInteractionsRequiringAlerts() throws IOException {
        List<DrugInteraction> interactions = loadAllDrugInteractions();
        return interactions.stream()
                .filter(DrugInteraction::isRequiresAlert)
                .toList();
    }
    
    /**
     * Find interactions that require intervention
     * 
     * @return List of interactions requiring intervention
     * @throws IOException if file cannot be read
     */
    public List<DrugInteraction> findInteractionsRequiringIntervention() throws IOException {
        List<DrugInteraction> interactions = loadAllDrugInteractions();
        return interactions.stream()
                .filter(DrugInteraction::isRequiresIntervention)
                .toList();
    }
    
    /**
     * Get count of drug interactions in CSV file
     * 
     * @return Total number of interactions
     * @throws IOException if file cannot be read
     */
    public int getDrugInteractionCount() throws IOException {
        return loadAllDrugInteractions().size();
    }
    
    /**
     * Get statistics about loaded drug interactions
     * 
     * @return Statistics string
     * @throws IOException if file cannot be read
     */
    public String getStatistics() throws IOException {
        List<DrugInteraction> interactions = loadAllDrugInteractions();
        
        long severeCount = interactions.stream().filter(DrugInteraction::isSevere).count();
        long moderateCount = interactions.stream().filter(DrugInteraction::isModerate).count();
        long minorCount = interactions.stream().filter(DrugInteraction::isMinor).count();
        long requiresAlertCount = interactions.stream().filter(DrugInteraction::isRequiresAlert).count();
        long requiresInterventionCount = interactions.stream().filter(DrugInteraction::isRequiresIntervention).count();
        
        return String.format("""
            Drug Interaction Statistics:
            ----------------------------
            Total Interactions: %,d
            Severity Distribution:
              Severe/Major:  %,d (%.1f%%)
              Moderate:      %,d (%.1f%%)
              Minor:         %,d (%.1f%%)
            Action Required:
              Requires Alert:        %,d (%.1f%%)
              Requires Intervention: %,d (%.1f%%)
            """,
            interactions.size(),
            severeCount, (severeCount * 100.0 / interactions.size()),
            moderateCount, (moderateCount * 100.0 / interactions.size()),
            minorCount, (minorCount * 100.0 / interactions.size()),
            requiresAlertCount, (requiresAlertCount * 100.0 / interactions.size()),
            requiresInterventionCount, (requiresInterventionCount * 100.0 / interactions.size())
        );
    }
    
    /**
     * Print interaction summary to console
     * 
     * @param interaction DrugInteraction to print
     */
    public void printInteractionSummary(DrugInteraction interaction) {
        System.out.println("=".repeat(70));
        System.out.println("Interaction Code: " + interaction.getInteractionCode());
        System.out.println("Drug 1: " + interaction.getDrug1Name() + " (NDC: " + interaction.getDrug1Ndc() + ")");
        System.out.println("Drug 2: " + interaction.getDrug2Name() + " (NDC: " + interaction.getDrug2Ndc() + ")");
        System.out.println("-".repeat(70));
        System.out.println("Severity: " + interaction.getSeverityLevel());
        System.out.println("Mechanism: " + interaction.getInteractionMechanism());
        System.out.println("Clinical Effects: " + interaction.getClinicalEffects());
        System.out.println("Management: " + interaction.getManagementRecommendation());
        System.out.println("-".repeat(70));
        System.out.println("Requires Alert: " + (interaction.isRequiresAlert() ? "Yes" : "No"));
        System.out.println("Requires Intervention: " + (interaction.isRequiresIntervention() ? "Yes" : "No"));
        System.out.println("Patient Counseling: " + (interaction.isPatientCounselingRequired() ? "Yes" : "No"));
        System.out.println("Prescriber Notification: " + (interaction.isPrescriberNotificationRequired() ? "Yes" : "No"));
        System.out.println("=".repeat(70));
    }
}


