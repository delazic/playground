package dejanlazic.playground.inmemory.rdbms.converter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import dejanlazic.playground.inmemory.rdbms.model.Pharmacy;
import dejanlazic.playground.inmemory.rdbms.model.Pharmacy.PharmacyType;

/**
 * Converter to read and parse pharmacies from CSV file
 * Reads from us_pharmacy_pharmacies.csv in classpath resources
 */
public class PharmacyConverter {
    
    private static final Logger LOGGER = Logger.getLogger(PharmacyConverter.class.getName());
    private static final String CSV_FILE = "data/us_pharmacy_pharmacies.csv";
    private static final String CSV_DELIMITER = ",";
    
    /**
     * Load all pharmacies from CSV file
     *
     * @return List of Pharmacy objects
     * @throws IOException if file cannot be read
     */
    public List<Pharmacy> loadAllPharmacies() throws IOException {
        List<Pharmacy> pharmacies = new ArrayList<>();
        
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
                        Pharmacy pharmacy = parseLine(line);
                        pharmacies.add(pharmacy);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error parsing line {0}: {1}",
                            new Object[]{lineNumber, e.getMessage()});
                        // Continue processing other lines
                    }
                }
            }
        }
        
        LOGGER.log(Level.INFO, "Total pharmacies loaded: {0}", pharmacies.size());
        return pharmacies;
    }
    
    /**
     * Parse a single CSV line into a Pharmacy object
     *
     * CSV format: ncpdp_id,pharmacy_name,npi,address,city,state,zip_code,phone,pharmacy_type,is_active
     *
     * @param line CSV line to parse
     * @return Pharmacy object
     */
    public Pharmacy parseLine(String line) {
        // Handle quoted fields that may contain commas
        String[] fields = parseCSVLine(line);
        
        if (fields.length < 10) {
            throw new IllegalArgumentException("Invalid CSV line: expected 10 fields, got " + fields.length);
        }
        
        Pharmacy pharmacy = new Pharmacy();
        
        // Set NCPDP ID (unique identifier)
        pharmacy.setNcpdpId(fields[0].trim());
        
        // Set pharmacy name
        pharmacy.setPharmacyName(fields[1].trim());
        
        // Set NPI
        pharmacy.setNpi(fields[2].trim());
        
        // Set address
        pharmacy.setAddress(fields[3].trim());
        
        // Set city
        pharmacy.setCity(fields[4].trim());
        
        // Set state
        pharmacy.setState(fields[5].trim());
        
        // Set ZIP code
        pharmacy.setZipCode(fields[6].trim());
        
        // Set phone
        pharmacy.setPhone(fields[7].trim());
        
        // Parse pharmacy type
        pharmacy.setPharmacyType(parsePharmacyType(fields[8]));
        
        // Parse is_active flag
        pharmacy.setActive(parseBoolean(fields[9]));
        
        return pharmacy;
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
     * Parse pharmacy type from string
     */
    private PharmacyType parsePharmacyType(String value) {
        if (value == null || value.trim().isEmpty()) {
            return PharmacyType.RETAIL; // Default
        }
        
        try {
            return PharmacyType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Invalid pharmacy type: {0}, defaulting to RETAIL", value);
            return PharmacyType.RETAIL;
        }
    }
    
    /**
     * Parse boolean from string (true/false)
     */
    private boolean parseBoolean(String value) {
        if (value == null || value.trim().isEmpty()) {
            return true; // Default to active
        }
        return "true".equalsIgnoreCase(value.trim());
    }
    
    /**
     * Find pharmacies by state
     *
     * @param state Two-letter state code
     * @return List of matching pharmacies
     * @throws IOException if file cannot be read
     */
    public List<Pharmacy> findByState(String state) throws IOException {
        List<Pharmacy> pharmacies = loadAllPharmacies();
        return pharmacies.stream()
                .filter(pharmacy -> state.equalsIgnoreCase(pharmacy.getState()))
                .toList();
    }
    
    /**
     * Find pharmacies by type
     *
     * @param type Pharmacy type
     * @return List of matching pharmacies
     * @throws IOException if file cannot be read
     */
    public List<Pharmacy> findByType(PharmacyType type) throws IOException {
        List<Pharmacy> pharmacies = loadAllPharmacies();
        return pharmacies.stream()
                .filter(pharmacy -> type.equals(pharmacy.getPharmacyType()))
                .toList();
    }
    
    /**
     * Find active pharmacies only
     *
     * @return List of active pharmacies
     * @throws IOException if file cannot be read
     */
    public List<Pharmacy> findActivePharmacies() throws IOException {
        List<Pharmacy> pharmacies = loadAllPharmacies();
        return pharmacies.stream()
                .filter(Pharmacy::isActive)
                .toList();
    }
    
    /**
     * Find pharmacies by city
     *
     * @param city City name (case-insensitive)
     * @return List of matching pharmacies
     * @throws IOException if file cannot be read
     */
    public List<Pharmacy> findByCity(String city) throws IOException {
        List<Pharmacy> pharmacies = loadAllPharmacies();
        String searchCity = city.toLowerCase();
        return pharmacies.stream()
                .filter(pharmacy -> pharmacy.getCity() != null && 
                                   pharmacy.getCity().toLowerCase().contains(searchCity))
                .toList();
    }
    
    /**
     * Get count of pharmacies in CSV file
     *
     * @return Total number of pharmacies
     * @throws IOException if file cannot be read
     */
    public int getPharmacyCount() throws IOException {
        return loadAllPharmacies().size();
    }
    
    /**
     * Get statistics about loaded pharmacies
     *
     * @return Statistics string
     * @throws IOException if file cannot be read
     */
    public String getStatistics() throws IOException {
        List<Pharmacy> pharmacies = loadAllPharmacies();
        
        long activeCount = pharmacies.stream().filter(Pharmacy::isActive).count();
        long inactiveCount = pharmacies.size() - activeCount;
        
        // Count by type
        long retailCount = pharmacies.stream().filter(Pharmacy::isRetail).count();
        long mailOrderCount = pharmacies.stream().filter(Pharmacy::isMailOrder).count();
        long specialtyCount = pharmacies.stream().filter(Pharmacy::isSpecialty).count();
        long ltcCount = pharmacies.stream().filter(Pharmacy::isLongTermCare).count();
        
        // Count by state (top 10)
        var stateGroups = pharmacies.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    Pharmacy::getState,
                    java.util.stream.Collectors.counting()
                ));
        
        var topStates = stateGroups.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .limit(10)
                .toList();
        
        StringBuilder stats = new StringBuilder();
        stats.append(String.format("""
            Pharmacy Statistics:
            --------------------
            Total Pharmacies: %,d
            Status Distribution:
              Active:              %,d (%.1f%%)
              Inactive:            %,d (%.1f%%)
            
            Type Distribution:
              Retail:              %,d (%.1f%%)
              Mail Order:          %,d (%.1f%%)
              Specialty:           %,d (%.1f%%)
              Long-Term Care:      %,d (%.1f%%)
            
            Top 10 States:
            """,
            pharmacies.size(),
            activeCount, (activeCount * 100.0 / pharmacies.size()),
            inactiveCount, (inactiveCount * 100.0 / pharmacies.size()),
            retailCount, (retailCount * 100.0 / pharmacies.size()),
            mailOrderCount, (mailOrderCount * 100.0 / pharmacies.size()),
            specialtyCount, (specialtyCount * 100.0 / pharmacies.size()),
            ltcCount, (ltcCount * 100.0 / pharmacies.size())
        ));
        
        for (var entry : topStates) {
            stats.append(String.format("  %s: %,d (%.1f%%)\n",
                entry.getKey(),
                entry.getValue(),
                (entry.getValue() * 100.0 / pharmacies.size())
            ));
        }
        
        return stats.toString();
    }
    
    /**
     * Print pharmacy summary to console
     *
     * @param pharmacy Pharmacy to print
     */
    public void printPharmacySummary(Pharmacy pharmacy) {
        System.out.println("=".repeat(70));
        System.out.println("Pharmacy: " + pharmacy.getPharmacyName());
        System.out.println("NCPDP ID: " + pharmacy.getNcpdpId());
        System.out.println("NPI: " + pharmacy.getNpi());
        System.out.println("-".repeat(70));
        System.out.println("Address: " + pharmacy.getFullAddress());
        System.out.println("Phone: " + pharmacy.getPhone());
        System.out.println("Type: " + pharmacy.getPharmacyTypeDisplay());
        System.out.println("Status: " + pharmacy.getStatus());
        System.out.println("=".repeat(70));
    }
}

// Made with Bob
