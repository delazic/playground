package dejanlazic.playground.inmemory.rdbms.converter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import dejanlazic.playground.inmemory.rdbms.model.Drug;

/**
 * Converter to read and parse drugs from CSV file
 * Reads from us_pharmacy_drugs.csv in classpath resources
 */
public class DrugConverter {
    
    private static final Logger LOGGER = Logger.getLogger(DrugConverter.class.getName());
    private static final String CSV_FILE = "data/us_pharmacy_drugs.csv";
    private static final String CSV_DELIMITER = ",";
    
    /**
     * Load all drugs from CSV file
     *
     * @return List of Drug objects
     * @throws IOException if file cannot be read
     */
    public List<Drug> loadAllDrugs() throws IOException {
        List<Drug> drugs = new ArrayList<>();
        
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
                        Drug drug = parseLine(line);
                        drugs.add(drug);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error parsing line {0}: {1}",
                            new Object[]{lineNumber, e.getMessage()});
                        // Continue processing other lines
                    }
                }
            }
        }
        
        LOGGER.log(Level.INFO, "Total drugs loaded: {0}", drugs.size());
        return drugs;
    }
    
    /**
     * Parse a single CSV line into a Drug object
     * 
     * CSV format: ndc_code,drug_name,generic_name,strength,dosage_form,route,manufacturer,
     *             drug_class,therapeutic_category,is_generic,is_brand,is_specialty,
     *             is_controlled,dea_schedule,awp_price,wac_price,mac_price,package_size,
     *             package_unit,fda_approval_date,is_active
     * 
     * @param line CSV line to parse
     * @return Drug object
     */
    public Drug parseLine(String line) {
        String[] fields = line.split(CSV_DELIMITER, -1); // -1 to keep trailing empty strings
        
        if (fields.length < 21) {
            throw new IllegalArgumentException("Invalid CSV line: expected 21 fields, got " + fields.length);
        }
        
        Drug drug = new Drug();
        
        // Basic drug information
        drug.setNdcCode(fields[0].trim());
        drug.setDrugName(fields[1].trim());
        drug.setGenericName(parseString(fields[2]));
        drug.setStrength(parseString(fields[3]));
        drug.setDosageForm(parseString(fields[4]));
        // Skip route (field 5) - not in database schema
        drug.setManufacturer(parseString(fields[6]));
        drug.setDrugClass(parseString(fields[7]));
        // Skip therapeutic_category (field 8) - not in database schema
        
        // Drug type flags
        drug.setGeneric(parseBoolean(fields[9]));
        drug.setBrand(parseBoolean(fields[10]));
        // Skip is_specialty (field 11) - calculated from price
        // Skip is_controlled (field 12) - not in database schema
        // Skip dea_schedule (field 13) - not in database schema
        
        // Pricing
        drug.setAwpPrice(parseBigDecimal(fields[14]));
        // Skip wac_price (field 15) - not in database schema
        drug.setMacPrice(parseBigDecimal(fields[16]));
        // Skip package_size (field 17) - not in database schema
        // Skip package_unit (field 18) - not in database schema
        // Skip fda_approval_date (field 19) - not in database schema
        // Skip is_active (field 20) - not in database schema
        
        return drug;
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
     * Parse boolean from string (true/false)
     */
    private boolean parseBoolean(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        return "true".equalsIgnoreCase(value.trim());
    }
    
    /**
     * Parse BigDecimal from string
     */
    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            LOGGER.log(Level.WARNING, "Invalid decimal value: {0}", value);
            return null;
        }
    }
    
    /**
     * Find a drug by NDC code
     * 
     * @param ndcCode NDC code to search for
     * @return Drug if found, null otherwise
     * @throws IOException if file cannot be read
     */
    public Drug findByNdcCode(String ndcCode) throws IOException {
        List<Drug> drugs = loadAllDrugs();
        return drugs.stream()
                .filter(drug -> drug.getNdcCode().equals(ndcCode))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Find drugs by drug name (case-insensitive partial match)
     * 
     * @param drugName Drug name to search for
     * @return List of matching drugs
     * @throws IOException if file cannot be read
     */
    public List<Drug> findByDrugName(String drugName) throws IOException {
        List<Drug> drugs = loadAllDrugs();
        String searchTerm = drugName.toLowerCase();
        return drugs.stream()
                .filter(drug -> drug.getDrugName().toLowerCase().contains(searchTerm))
                .toList();
    }
    
    /**
     * Find drugs by generic name (case-insensitive partial match)
     * 
     * @param genericName Generic name to search for
     * @return List of matching drugs
     * @throws IOException if file cannot be read
     */
    public List<Drug> findByGenericName(String genericName) throws IOException {
        List<Drug> drugs = loadAllDrugs();
        String searchTerm = genericName.toLowerCase();
        return drugs.stream()
                .filter(drug -> drug.getGenericName() != null && 
                               drug.getGenericName().toLowerCase().contains(searchTerm))
                .toList();
    }
    
    /**
     * Find drugs by drug class
     * 
     * @param drugClass Drug class to search for
     * @return List of matching drugs
     * @throws IOException if file cannot be read
     */
    public List<Drug> findByDrugClass(String drugClass) throws IOException {
        List<Drug> drugs = loadAllDrugs();
        return drugs.stream()
                .filter(drug -> drugClass.equalsIgnoreCase(drug.getDrugClass()))
                .toList();
    }
    
    /**
     * Find drugs by manufacturer
     * 
     * @param manufacturer Manufacturer to search for
     * @return List of matching drugs
     * @throws IOException if file cannot be read
     */
    public List<Drug> findByManufacturer(String manufacturer) throws IOException {
        List<Drug> drugs = loadAllDrugs();
        return drugs.stream()
                .filter(drug -> manufacturer.equalsIgnoreCase(drug.getManufacturer()))
                .toList();
    }
    
    /**
     * Find generic drugs only
     * 
     * @return List of generic drugs
     * @throws IOException if file cannot be read
     */
    public List<Drug> findGenericDrugs() throws IOException {
        List<Drug> drugs = loadAllDrugs();
        return drugs.stream()
                .filter(Drug::isGeneric)
                .toList();
    }
    
    /**
     * Find brand drugs only
     * 
     * @return List of brand drugs
     * @throws IOException if file cannot be read
     */
    public List<Drug> findBrandDrugs() throws IOException {
        List<Drug> drugs = loadAllDrugs();
        return drugs.stream()
                .filter(Drug::isBrand)
                .toList();
    }
    
    /**
     * Find specialty drugs (AWP > $1000)
     * 
     * @return List of specialty drugs
     * @throws IOException if file cannot be read
     */
    public List<Drug> findSpecialtyDrugs() throws IOException {
        List<Drug> drugs = loadAllDrugs();
        return drugs.stream()
                .filter(Drug::isSpecialty)
                .toList();
    }
    
    /**
     * Find drugs by price range
     * 
     * @param minPrice Minimum AWP price
     * @param maxPrice Maximum AWP price
     * @return List of matching drugs
     * @throws IOException if file cannot be read
     */
    public List<Drug> findByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) throws IOException {
        List<Drug> drugs = loadAllDrugs();
        return drugs.stream()
                .filter(drug -> drug.getAwpPrice() != null &&
                               drug.getAwpPrice().compareTo(minPrice) >= 0 &&
                               drug.getAwpPrice().compareTo(maxPrice) <= 0)
                .toList();
    }
    
    /**
     * Get count of drugs in CSV file
     * 
     * @return Total number of drugs
     * @throws IOException if file cannot be read
     */
    public int getDrugCount() throws IOException {
        return loadAllDrugs().size();
    }
    
    /**
     * Get statistics about loaded drugs
     * 
     * @return Statistics string
     * @throws IOException if file cannot be read
     */
    public String getStatistics() throws IOException {
        List<Drug> drugs = loadAllDrugs();
        
        long genericCount = drugs.stream().filter(Drug::isGeneric).count();
        long brandCount = drugs.stream().filter(Drug::isBrand).count();
        long specialtyCount = drugs.stream().filter(Drug::isSpecialty).count();
        
        BigDecimal avgAwp = drugs.stream()
                .filter(d -> d.getAwpPrice() != null)
                .map(Drug::getAwpPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(drugs.size()), 2, BigDecimal.ROUND_HALF_UP);
        
        BigDecimal minAwp = drugs.stream()
                .filter(d -> d.getAwpPrice() != null)
                .map(Drug::getAwpPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        
        BigDecimal maxAwp = drugs.stream()
                .filter(d -> d.getAwpPrice() != null)
                .map(Drug::getAwpPrice)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        
        return String.format("""
            Drug Statistics:
            ----------------
            Total Drugs: %,d
            Drug Type Distribution:
              Generic:   %,d (%.1f%%)
              Brand:     %,d (%.1f%%)
              Specialty: %,d (%.1f%%)
            AWP Price Statistics:
              Average: $%,.2f
              Minimum: $%,.2f
              Maximum: $%,.2f
            """,
            drugs.size(),
            genericCount, (genericCount * 100.0 / drugs.size()),
            brandCount, (brandCount * 100.0 / drugs.size()),
            specialtyCount, (specialtyCount * 100.0 / drugs.size()),
            avgAwp,
            minAwp,
            maxAwp
        );
    }
    
    /**
     * Print drug summary to console
     * 
     * @param drug Drug to print
     */
    public void printDrugSummary(Drug drug) {
        System.out.println("=".repeat(70));
        System.out.println("NDC Code: " + drug.getNdcCode());
        System.out.println("Drug Name: " + drug.getDrugName());
        System.out.println("Generic Name: " + (drug.getGenericName() != null ? drug.getGenericName() : "N/A"));
        System.out.println("-".repeat(70));
        System.out.println("Strength: " + (drug.getStrength() != null ? drug.getStrength() : "N/A"));
        System.out.println("Dosage Form: " + (drug.getDosageForm() != null ? drug.getDosageForm() : "N/A"));
        System.out.println("Manufacturer: " + (drug.getManufacturer() != null ? drug.getManufacturer() : "N/A"));
        System.out.println("Drug Class: " + (drug.getDrugClass() != null ? drug.getDrugClass() : "N/A"));
        System.out.println("-".repeat(70));
        System.out.println("Type: " + drug.getDrugType());
        System.out.println("AWP Price: $" + (drug.getAwpPrice() != null ? drug.getAwpPrice() : "N/A"));
        System.out.println("MAC Price: $" + (drug.getMacPrice() != null ? drug.getMacPrice() : "N/A"));
        System.out.println("=".repeat(70));
    }
}
