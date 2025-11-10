package dejanlazic.playground.inmemory.rdbms.converter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import dejanlazic.playground.inmemory.rdbms.model.PlanRule;

/**
 * Converter to read and parse plan rules from CSV files
 * Reads from us_pharmacy_plan_rules_XX.csv files in classpath resources
 */
public class PlanRuleConverter {
    
    private static final Logger LOGGER = Logger.getLogger(PlanRuleConverter.class.getName());
    private static final String CSV_FILE_PREFIX = "data/us_pharmacy_plan_rules_";
    private static final String CSV_FILE_SUFFIX = ".csv";
    private static final String CSV_DELIMITER = ",";
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Load all plan rules from all CSV files
     *
     * @return List of PlanRule objects
     * @throws IOException if files cannot be read
     */
    public List<PlanRule> loadAllPlanRules() throws IOException {
        List<PlanRule> planRules = new ArrayList<>();
        
        // Try to load files sequentially (01, 02, 03, etc.)
        int fileNumber = 1;
        while (true) {
            String fileName = String.format("%s%02d%s", CSV_FILE_PREFIX, fileNumber, CSV_FILE_SUFFIX);
            try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
                if (inputStream == null) {
                    // No more files to load
                    break;
                }
                
                LOGGER.log(Level.INFO, "Loading plan rules from: {0}", fileName);
                List<PlanRule> rulesFromFile = loadFromStream(inputStream, fileName);
                planRules.addAll(rulesFromFile);
                fileNumber++;
            }
        }
        
        if (planRules.isEmpty()) {
            LOGGER.log(Level.WARNING, "No plan rule files found in classpath");
        } else {
            LOGGER.log(Level.INFO, "Total plan rules loaded: {0} from {1} files", 
                new Object[]{planRules.size(), fileNumber - 1});
        }
        
        return planRules;
    }
    
    /**
     * Load plan rules from a specific file number
     *
     * @param fileNumber File number (e.g., 1 for _01.csv)
     * @return List of PlanRule objects
     * @throws IOException if file cannot be read
     */
    public List<PlanRule> loadPlanRulesFromFile(int fileNumber) throws IOException {
        String fileName = String.format("%s%02d%s", CSV_FILE_PREFIX, fileNumber, CSV_FILE_SUFFIX);
        
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new IOException("CSV file not found in classpath: " + fileName);
            }
            
            return loadFromStream(inputStream, fileName);
        }
    }
    
    /**
     * Load plan rules from an input stream
     */
    private List<PlanRule> loadFromStream(InputStream inputStream, String fileName) throws IOException {
        List<PlanRule> planRules = new ArrayList<>();
        
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
                    PlanRule planRule = parseLine(line);
                    planRules.add(planRule);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error parsing line {0} in {1}: {2}",
                        new Object[]{lineNumber, fileName, e.getMessage()});
                    // Continue processing other lines
                }
            }
        }
        
        return planRules;
    }
    
    /**
     * Parse a single CSV line into a PlanRule object
     * 
     * CSV format: rule_id,plan_id,rule_type,rule_name,rule_criteria,rule_action,
     *             priority,is_active,created_at
     * 
     * @param line CSV line to parse
     * @return PlanRule object
     */
    public PlanRule parseLine(String line) {
        // Handle JSONB fields that may contain commas
        String[] fields = parseCSVLine(line);
        
        if (fields.length < 9) {
            throw new IllegalArgumentException("Invalid CSV line: expected 9 fields, got " + fields.length);
        }
        
        PlanRule planRule = new PlanRule();
        
        // Identification
        planRule.setRuleId(parseLong(fields[0]));
        planRule.setPlanId(parseLong(fields[1]));
        
        // Rule definition
        planRule.setRuleType(parseString(fields[2]));
        planRule.setRuleName(parseString(fields[3]));
        planRule.setRuleCriteria(parseString(fields[4]));  // JSONB as string
        planRule.setRuleAction(parseString(fields[5]));    // JSONB as string
        
        // Rule configuration
        planRule.setPriority(parseInteger(fields[6]));
        planRule.setIsActive(parseBoolean(fields[7]));
        
        // Audit trail
        planRule.setCreatedAt(parseDateTime(fields[8]));
        
        return planRule;
    }
    
    /**
     * Parse CSV line handling quoted fields (for JSONB)
     * This handles commas within JSON fields
     */
    private String[] parseCSVLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }
        
        // Add last field
        fields.add(currentField.toString());
        
        return fields.toArray(new String[0]);
    }
    
    /**
     * Parse string, returning null for empty strings
     */
    private String parseString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        // Remove surrounding quotes if present
        String trimmed = value.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }
    
    /**
     * Parse Long from string
     */
    private Long parseLong(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid long value: {0}", value);
            return null;
        }
    }
    
    /**
     * Parse Integer from string
     */
    private Integer parseInteger(String value) {
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
    private Boolean parseBoolean(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return "true".equalsIgnoreCase(value.trim());
    }
    
    /**
     * Parse LocalDateTime from string (yyyy-MM-dd HH:mm:ss format)
     */
    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim(), DATETIME_FORMATTER);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Invalid datetime value: {0}", value);
            return null;
        }
    }
    
    /**
     * Find plan rules by plan ID
     * 
     * @param planId Plan ID to search for
     * @return List of matching plan rules
     * @throws IOException if files cannot be read
     */
    public List<PlanRule> findByPlanId(Long planId) throws IOException {
        List<PlanRule> allRules = loadAllPlanRules();
        return allRules.stream()
                .filter(rule -> planId.equals(rule.getPlanId()))
                .toList();
    }
    
    /**
     * Find plan rules by rule type
     * 
     * @param ruleType Rule type to search for
     * @return List of matching plan rules
     * @throws IOException if files cannot be read
     */
    public List<PlanRule> findByRuleType(String ruleType) throws IOException {
        List<PlanRule> allRules = loadAllPlanRules();
        return allRules.stream()
                .filter(rule -> ruleType.equalsIgnoreCase(rule.getRuleType()))
                .toList();
    }
    
    /**
     * Find active plan rules only
     * 
     * @return List of active plan rules
     * @throws IOException if files cannot be read
     */
    public List<PlanRule> findActiveRules() throws IOException {
        List<PlanRule> allRules = loadAllPlanRules();
        return allRules.stream()
                .filter(PlanRule::isActive)
                .toList();
    }
    
    /**
     * Find high priority rules (priority >= 50)
     * 
     * @return List of high priority plan rules
     * @throws IOException if files cannot be read
     */
    public List<PlanRule> findHighPriorityRules() throws IOException {
        List<PlanRule> allRules = loadAllPlanRules();
        return allRules.stream()
                .filter(PlanRule::isHighPriority)
                .toList();
    }
    
    /**
     * Find plan rules by plan ID and rule type
     * 
     * @param planId Plan ID to search for
     * @param ruleType Rule type to search for
     * @return List of matching plan rules
     * @throws IOException if files cannot be read
     */
    public List<PlanRule> findByPlanIdAndRuleType(Long planId, String ruleType) throws IOException {
        List<PlanRule> allRules = loadAllPlanRules();
        return allRules.stream()
                .filter(rule -> planId.equals(rule.getPlanId()) 
                        && ruleType.equalsIgnoreCase(rule.getRuleType()))
                .toList();
    }
    
    /**
     * Get count of plan rules in all CSV files
     * 
     * @return Total number of plan rules
     * @throws IOException if files cannot be read
     */
    public int getPlanRuleCount() throws IOException {
        return loadAllPlanRules().size();
    }
    
    /**
     * Get statistics about loaded plan rules
     * 
     * @return Statistics string
     * @throws IOException if files cannot be read
     */
    public String getStatistics() throws IOException {
        List<PlanRule> rules = loadAllPlanRules();
        
        long activeCount = rules.stream().filter(PlanRule::isActive).count();
        long inactiveCount = rules.size() - activeCount;
        
        long highPriorityCount = rules.stream().filter(PlanRule::isHighPriority).count();
        long lowPriorityCount = rules.stream().filter(PlanRule::isLowPriority).count();
        long normalPriorityCount = rules.size() - highPriorityCount - lowPriorityCount;
        
        // Count by rule type
        Map<String, Long> ruleTypeCounts = new HashMap<>();
        rules.forEach(rule -> {
            String type = rule.getRuleType();
            ruleTypeCounts.put(type, ruleTypeCounts.getOrDefault(type, 0L) + 1);
        });
        
        // Count unique plans
        long uniquePlans = rules.stream()
                .map(PlanRule::getPlanId)
                .distinct()
                .count();
        
        double avgRulesPerPlan = uniquePlans > 0 ? (double) rules.size() / uniquePlans : 0;
        
        StringBuilder stats = new StringBuilder();
        stats.append("Plan Rule Statistics:\n");
        stats.append("--------------------\n");
        stats.append(String.format("Total Plan Rules: %,d\n", rules.size()));
        stats.append(String.format("Unique Plans: %,d\n", uniquePlans));
        stats.append(String.format("Average Rules per Plan: %.0f\n\n", avgRulesPerPlan));
        
        stats.append("Status Distribution:\n");
        stats.append(String.format("  Active:   %,d (%.1f%%)\n", activeCount, (activeCount * 100.0 / rules.size())));
        stats.append(String.format("  Inactive: %,d (%.1f%%)\n\n", inactiveCount, (inactiveCount * 100.0 / rules.size())));
        
        stats.append("Priority Distribution:\n");
        stats.append(String.format("  High (50-100):    %,d (%.1f%%)\n", highPriorityCount, (highPriorityCount * 100.0 / rules.size())));
        stats.append(String.format("  Normal (0-49):    %,d (%.1f%%)\n", normalPriorityCount, (normalPriorityCount * 100.0 / rules.size())));
        stats.append(String.format("  Low (-100 to -1): %,d (%.1f%%)\n\n", lowPriorityCount, (lowPriorityCount * 100.0 / rules.size())));
        
        stats.append("Rule Type Distribution:\n");
        ruleTypeCounts.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .forEach(entry -> {
                    stats.append(String.format("  %-25s %,d (%.1f%%)\n", 
                        entry.getKey() + ":", 
                        entry.getValue(), 
                        (entry.getValue() * 100.0 / rules.size())));
                });
        
        return stats.toString();
    }
    
    /**
     * Print plan rule summary to console
     * 
     * @param planRule Plan rule to print
     */
    public void printPlanRuleSummary(PlanRule planRule) {
        System.out.println("=".repeat(70));
        System.out.println("Rule ID: " + planRule.getRuleId());
        System.out.println("Plan ID: " + planRule.getPlanId());
        System.out.println("-".repeat(70));
        System.out.println("Rule Type: " + planRule.getRuleType());
        System.out.println("Rule Name: " + planRule.getRuleName());
        System.out.println("Category: " + planRule.getRuleCategory());
        System.out.println("-".repeat(70));
        System.out.println("Priority: " + planRule.getPriority() + " (" + planRule.getPriorityLevel() + ")");
        System.out.println("Active: " + (planRule.isActive() ? "Yes" : "No"));
        System.out.println("-".repeat(70));
        System.out.println("Rule Criteria:");
        System.out.println(formatJson(planRule.getRuleCriteria()));
        System.out.println("-".repeat(70));
        System.out.println("Rule Action:");
        System.out.println(formatJson(planRule.getRuleAction()));
        System.out.println("=".repeat(70));
    }
    
    /**
     * Format JSON string for better readability
     */
    private String formatJson(String json) {
        if (json == null || json.isEmpty()) {
            return "  (empty)";
        }
        // Simple indentation for display
        return "  " + json.replace("{", "{\n    ")
                          .replace("}", "\n  }")
                          .replace(",", ",\n    ");
    }
}

// Made with Bob
