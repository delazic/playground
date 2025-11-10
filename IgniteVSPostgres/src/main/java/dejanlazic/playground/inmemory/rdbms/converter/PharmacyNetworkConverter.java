package dejanlazic.playground.inmemory.rdbms.converter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import dejanlazic.playground.inmemory.rdbms.model.PharmacyNetwork;
import dejanlazic.playground.inmemory.rdbms.model.PharmacyNetwork.ContractType;
import dejanlazic.playground.inmemory.rdbms.model.PharmacyNetwork.NetworkStatus;
import dejanlazic.playground.inmemory.rdbms.model.PharmacyNetwork.NetworkTier;
import dejanlazic.playground.inmemory.rdbms.model.PharmacyNetwork.NetworkType;

/**
 * Converter to read and parse pharmacy networks from CSV files
 * Reads from us_pharmacy_pharmacy_networks_*.csv files in classpath resources
 */
public class PharmacyNetworkConverter {
    
    private static final Logger LOGGER = Logger.getLogger(PharmacyNetworkConverter.class.getName());
    private static final String CSV_FILE_PREFIX = "data/us_pharmacy_pharmacy_networks_";
    private static final String CSV_FILE_SUFFIX = ".csv";
    private static final String PHARMACY_CSV_FILE = "data/us_pharmacy_pharmacies.csv";
    private static final String CSV_DELIMITER = ",";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    // Cache for pharmacy ID to NCPDP ID mapping
    private Map<String, String> pharmacyIdToNcpdpMap = null;
    
    /**
     * Load pharmacy ID to NCPDP ID mapping from pharmacy CSV
     */
    private void loadPharmacyMapping() throws IOException {
        if (pharmacyIdToNcpdpMap != null) {
            return; // Already loaded
        }
        
        pharmacyIdToNcpdpMap = new HashMap<>();
        
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(PHARMACY_CSV_FILE)) {
            if (inputStream == null) {
                LOGGER.log(Level.WARNING, "Pharmacy CSV file not found: {0}", PHARMACY_CSV_FILE);
                return;
            }
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                // Skip header
                reader.readLine();
                
                String line;
                int index = 1;
                while ((line = reader.readLine()) != null) {
                    String[] fields = line.split(",", 2); // Split only first field (ncpdp_id)
                    if (fields.length > 0) {
                        String ncpdpId = fields[0].trim();
                        String pharmacyId = String.format("PHARM%08d", index);
                        pharmacyIdToNcpdpMap.put(pharmacyId, ncpdpId);
                        index++;
                    }
                }
                
                LOGGER.log(Level.INFO, "Loaded {0} pharmacy ID mappings", pharmacyIdToNcpdpMap.size());
            }
        }
    }
    
    /**
     * Load all pharmacy networks from all CSV files
     *
     * @return List of PharmacyNetwork objects
     * @throws IOException if files cannot be read
     */
    public List<PharmacyNetwork> loadAllPharmacyNetworks() throws IOException {
        // Load pharmacy mapping first
        loadPharmacyMapping();
        
        List<PharmacyNetwork> allNetworks = new ArrayList<>();
        
        // Try to load files numbered 01, 02, 03, etc.
        int fileNumber = 1;
        boolean filesFound = false;
        
        while (true) {
            String fileName = String.format("%s%02d%s", CSV_FILE_PREFIX, fileNumber, CSV_FILE_SUFFIX);
            
            try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
                if (inputStream == null) {
                    // No more files found
                    break;
                }
                
                filesFound = true;
                LOGGER.log(Level.INFO, "Loading pharmacy networks from: {0}", fileName);
                
                List<PharmacyNetwork> networks = loadFromStream(inputStream, fileName);
                allNetworks.addAll(networks);
                
                fileNumber++;
            }
        }
        
        if (!filesFound) {
            throw new IOException("No pharmacy network CSV files found with prefix: " + CSV_FILE_PREFIX);
        }
        
        LOGGER.log(Level.INFO, "Total pharmacy networks loaded from {0} file(s): {1}", 
            new Object[]{fileNumber - 1, allNetworks.size()});
        return allNetworks;
    }
    
    /**
     * Load pharmacy networks from a specific file number
     *
     * @param fileNumber File number (e.g., 1 for _01.csv)
     * @return List of PharmacyNetwork objects
     * @throws IOException if file cannot be read
     */
    public List<PharmacyNetwork> loadFromFile(int fileNumber) throws IOException {
        String fileName = String.format("%s%02d%s", CSV_FILE_PREFIX, fileNumber, CSV_FILE_SUFFIX);
        
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new IOException("CSV file not found in classpath: " + fileName);
            }
            
            return loadFromStream(inputStream, fileName);
        }
    }
    
    /**
     * Load pharmacy networks from an input stream
     */
    private List<PharmacyNetwork> loadFromStream(InputStream inputStream, String fileName) throws IOException {
        List<PharmacyNetwork> networks = new ArrayList<>();
        
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
                    PharmacyNetwork network = parseLine(line);
                    networks.add(network);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error parsing line {0} in {1}: {2}",
                        new Object[]{lineNumber, fileName, e.getMessage()});
                    // Continue processing other lines
                }
            }
        }
        
        LOGGER.log(Level.INFO, "Loaded {0} pharmacy networks from {1}", 
            new Object[]{networks.size(), fileName});
        return networks;
    }
    
    /**
     * Parse a single CSV line into a PharmacyNetwork object
     *
     * CSV format: network_id,pharmacy_id,network_name,network_type,network_tier,
     *             contract_type,effective_date,termination_date,status,
     *             reimbursement_rate,dispensing_fee,is_preferred,is_mail_order,
     *             is_specialty,created_at,updated_at
     *
     * @param line CSV line to parse
     * @return PharmacyNetwork object
     */
    public PharmacyNetwork parseLine(String line) {
        // Handle quoted fields that may contain commas
        String[] fields = parseCSVLine(line);
        
        if (fields.length < 16) {
            throw new IllegalArgumentException("Invalid CSV line: expected 16 fields, got " + fields.length);
        }
        
        PharmacyNetwork network = new PharmacyNetwork();
        
        // Note: network_id will be generated by database
        // fields[0] = network_id (will be generated by database)
        // fields[1] = pharmacy_id (synthetic ID like PHARM00000001)
        
        // Map synthetic pharmacy_id to actual ncpdp_id
        String pharmacyId = fields[1].trim();
        String ncpdpId = pharmacyIdToNcpdpMap != null ? pharmacyIdToNcpdpMap.get(pharmacyId) : null;
        if (ncpdpId == null) {
            // Skip records with invalid pharmacy IDs (beyond available pharmacies)
            throw new IllegalArgumentException("Could not find NCPDP ID for pharmacy ID: " + pharmacyId + " (pharmacy may not exist in database)");
        }
        network.setNcpdpId(ncpdpId);
        
        // Validate dates before setting them
        LocalDate effectiveDate = parseDate(fields[6]);
        LocalDate terminationDate = parseDate(fields[7]);
        
        // Fix invalid date ranges: if termination_date < effective_date, swap them or set termination to null
        if (terminationDate != null && effectiveDate != null && terminationDate.isBefore(effectiveDate)) {
            LOGGER.log(Level.WARNING, "Invalid date range for {0}: effective={1}, termination={2}. Setting termination_date to null.",
                new Object[]{fields[2].trim(), effectiveDate, terminationDate});
            terminationDate = null;
        }
        
        // Set network name
        network.setNetworkName(fields[2].trim());
        
        // Parse network type
        network.setNetworkType(parseNetworkType(fields[3]));
        
        // Parse network tier
        network.setNetworkTier(parseNetworkTier(fields[4]));
        
        // Parse contract type
        network.setContractType(parseContractType(fields[5]));
        
        // Set the validated dates
        network.setEffectiveDate(effectiveDate);
        network.setTerminationDate(terminationDate);
        
        // Parse status
        network.setStatus(parseNetworkStatus(fields[8]));
        
        // Set reimbursement rate
        network.setReimbursementRate(fields[9].trim());
        
        // Parse dispensing fee
        network.setDispensingFee(parseBigDecimal(fields[10]));
        
        // Parse boolean flags
        network.setPreferred(parseBoolean(fields[11]));
        network.setMailOrder(parseBoolean(fields[12]));
        network.setSpecialty(parseBoolean(fields[13]));
        
        // created_at and updated_at are handled by database triggers
        
        return network;
    }
    
    /**
     * Parse CSV line handling quoted fields
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
        
        // Add the last field
        fields.add(currentField.toString());
        
        return fields.toArray(new String[0]);
    }
    
    /**
     * Parse network type from string
     */
    private NetworkType parseNetworkType(String value) {
        if (value == null || value.trim().isEmpty()) {
            return NetworkType.RETAIL; // Default
        }
        
        try {
            return NetworkType.valueOf(value.trim().toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Invalid network type: {0}, defaulting to RETAIL", value);
            return NetworkType.RETAIL;
        }
    }
    
    /**
     * Parse network tier from string
     */
    private NetworkTier parseNetworkTier(String value) {
        if (value == null || value.trim().isEmpty()) {
            return NetworkTier.STANDARD; // Default
        }
        
        try {
            return NetworkTier.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Invalid network tier: {0}, defaulting to STANDARD", value);
            return NetworkTier.STANDARD;
        }
    }
    
    /**
     * Parse contract type from string
     */
    private ContractType parseContractType(String value) {
        if (value == null || value.trim().isEmpty()) {
            return ContractType.DIRECT; // Default
        }
        
        try {
            return ContractType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Invalid contract type: {0}, defaulting to DIRECT", value);
            return ContractType.DIRECT;
        }
    }
    
    /**
     * Parse network status from string
     */
    private NetworkStatus parseNetworkStatus(String value) {
        if (value == null || value.trim().isEmpty()) {
            return NetworkStatus.ACTIVE; // Default
        }
        
        try {
            return NetworkStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Invalid network status: {0}, defaulting to ACTIVE", value);
            return NetworkStatus.ACTIVE;
        }
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
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Invalid date format: {0}", value);
            return null;
        }
    }
    
    /**
     * Parse BigDecimal from string
     */
    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid decimal value: {0}, defaulting to 0", value);
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Parse boolean from string (true/false)
     */
    private boolean parseBoolean(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false; // Default
        }
        return "true".equalsIgnoreCase(value.trim());
    }
    
    /**
     * Get count of pharmacy networks in CSV files
     *
     * @return Total number of pharmacy networks
     * @throws IOException if files cannot be read
     */
    public int getPharmacyNetworkCount() throws IOException {
        return loadAllPharmacyNetworks().size();
    }
    
    /**
     * Get statistics about loaded pharmacy networks
     *
     * @return Statistics string
     * @throws IOException if files cannot be read
     */
    public String getStatistics() throws IOException {
        List<PharmacyNetwork> networks = loadAllPharmacyNetworks();
        
        // Count by status
        long activeCount = networks.stream().filter(n -> n.getStatus() == NetworkStatus.ACTIVE).count();
        long inactiveCount = networks.stream().filter(n -> n.getStatus() == NetworkStatus.INACTIVE).count();
        long pendingCount = networks.stream().filter(n -> n.getStatus() == NetworkStatus.PENDING).count();
        
        // Count by type
        long pbmCount = networks.stream().filter(PharmacyNetwork::isPBMNetwork).count();
        long retailCount = networks.stream().filter(PharmacyNetwork::isRetailNetwork).count();
        long specialtyCount = networks.stream().filter(PharmacyNetwork::isSpecialtyNetwork).count();
        long mailOrderCount = networks.stream().filter(PharmacyNetwork::isMailOrderNetwork).count();
        long regionalCount = networks.stream().filter(PharmacyNetwork::isRegionalNetwork).count();
        long independentCount = networks.stream().filter(PharmacyNetwork::isIndependentNetwork).count();
        
        // Count by tier
        long preferredCount = networks.stream().filter(PharmacyNetwork::isPreferredTier).count();
        long standardCount = networks.stream().filter(PharmacyNetwork::isStandardTier).count();
        
        // Count flags
        long preferredFlagCount = networks.stream().filter(PharmacyNetwork::isPreferred).count();
        long mailOrderFlagCount = networks.stream().filter(PharmacyNetwork::isMailOrder).count();
        long specialtyFlagCount = networks.stream().filter(PharmacyNetwork::isSpecialty).count();
        
        StringBuilder stats = new StringBuilder();
        stats.append(String.format("""
            Pharmacy Network Statistics:
            ----------------------------
            Total Network Assignments: %,d
            
            Status Distribution:
              Active:              %,d (%.1f%%)
              Inactive:            %,d (%.1f%%)
              Pending:             %,d (%.1f%%)
            
            Network Type Distribution:
              PBM:                 %,d (%.1f%%)
              Retail:              %,d (%.1f%%)
              Specialty:           %,d (%.1f%%)
              Mail-Order:          %,d (%.1f%%)
              Regional:            %,d (%.1f%%)
              Independent:         %,d (%.1f%%)
            
            Network Tier Distribution:
              Preferred:           %,d (%.1f%%)
              Standard:            %,d (%.1f%%)
            
            Network Characteristics:
              Preferred Flag:      %,d (%.1f%%)
              Mail-Order Flag:     %,d (%.1f%%)
              Specialty Flag:      %,d (%.1f%%)
            """,
            networks.size(),
            activeCount, (activeCount * 100.0 / networks.size()),
            inactiveCount, (inactiveCount * 100.0 / networks.size()),
            pendingCount, (pendingCount * 100.0 / networks.size()),
            pbmCount, (pbmCount * 100.0 / networks.size()),
            retailCount, (retailCount * 100.0 / networks.size()),
            specialtyCount, (specialtyCount * 100.0 / networks.size()),
            mailOrderCount, (mailOrderCount * 100.0 / networks.size()),
            regionalCount, (regionalCount * 100.0 / networks.size()),
            independentCount, (independentCount * 100.0 / networks.size()),
            preferredCount, (preferredCount * 100.0 / networks.size()),
            standardCount, (standardCount * 100.0 / networks.size()),
            preferredFlagCount, (preferredFlagCount * 100.0 / networks.size()),
            mailOrderFlagCount, (mailOrderFlagCount * 100.0 / networks.size()),
            specialtyFlagCount, (specialtyFlagCount * 100.0 / networks.size())
        ));
        
        return stats.toString();
    }
    
    /**
     * Print pharmacy network summary to console
     *
     * @param network PharmacyNetwork to print
     */
    public void printPharmacyNetworkSummary(PharmacyNetwork network) {
        System.out.println("=".repeat(70));
        System.out.println("Network: " + network.getNetworkName());
        System.out.println("Type: " + network.getNetworkTypeDisplay());
        System.out.println("-".repeat(70));
        System.out.println("Tier: " + network.getNetworkTierDisplay());
        System.out.println("Contract: " + network.getContractTypeDisplay());
        System.out.println("Status: " + network.getStatusDisplay());
        System.out.println("Reimbursement: " + network.getReimbursementRate());
        System.out.println("Dispensing Fee: $" + network.getDispensingFee());
        System.out.println("Effective Date: " + network.getEffectiveDate());
        if (network.getTerminationDate() != null) {
            System.out.println("Termination Date: " + network.getTerminationDate());
        }
        System.out.println("Preferred: " + (network.isPreferred() ? "Yes" : "No"));
        System.out.println("Mail-Order: " + (network.isMailOrder() ? "Yes" : "No"));
        System.out.println("Specialty: " + (network.isSpecialty() ? "Yes" : "No"));
        System.out.println("=".repeat(70));
    }
}


