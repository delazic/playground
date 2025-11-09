package dejanlazic.playground.inmemory.rdbms.converter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import dejanlazic.playground.inmemory.rdbms.model.Claim;

/**
 * Converter for loading Claim data from CSV files.
 * Handles the simulation CSV format with NCPDP-style fields.
 * Supports loading from multiple split files.
 */
public class ClaimConverter {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String FILE_PREFIX = "/data/us_pharmacy_claims_simulation_1m_";
    private static final String FILE_SUFFIX = ".csv";
    
    /**
     * Load all claims from simulation CSV files.
     * Automatically detects and loads from multiple files if they exist,
     * otherwise falls back to single file.
     *
     * @return List of Claim objects from all files
     * @throws IOException if files cannot be read
     */
    public List<Claim> loadAllClaims() throws IOException {
        List<Claim> allClaims = new ArrayList<>();
        
        // Try to load from multiple files first
        int fileNumber = 1;
        boolean foundFiles = false;
        
        while (true) {
            String filename = String.format("%s%02d%s", FILE_PREFIX, fileNumber, FILE_SUFFIX);
            InputStream is = getClass().getResourceAsStream(filename);
            
            if (is == null) {
                // No more files found
                break;
            }
            
            foundFiles = true;
            System.out.println("Loading from file " + fileNumber + ": " + filename);
            
            try {
                List<Claim> claims = loadClaimsFromStream(is);
                allClaims.addAll(claims);
                System.out.println("  Loaded " + claims.size() + " claims from file " + fileNumber);
            } finally {
                is.close();
            }
            
            fileNumber++;
        }
        
        // If no split files found, try single file (backward compatibility)
        if (!foundFiles) {
            String singleFilename = "/data/us_pharmacy_claims_simulation_1m.csv";
            System.out.println("No split files found, trying single file: " + singleFilename);
            
            try (InputStream is = getClass().getResourceAsStream(singleFilename)) {
                if (is == null) {
                    throw new IOException("No claims files found. Expected either:\n" +
                        "  - Multiple files: " + FILE_PREFIX + "01" + FILE_SUFFIX + ", " +
                        FILE_PREFIX + "02" + FILE_SUFFIX + ", etc.\n" +
                        "  - Single file: " + singleFilename + "\n\n" +
                        "Please run: cd database/scripts && python3 generate_1m_claims.py");
                }
                
                allClaims = loadClaimsFromStream(is);
            }
        }
        
        System.out.println("Total claims loaded: " + allClaims.size());
        return allClaims;
    }
    
    /**
     * Load claims from an input stream.
     * 
     * @param is Input stream containing CSV data
     * @return List of Claim objects
     * @throws IOException if stream cannot be read
     */
    private List<Claim> loadClaimsFromStream(InputStream is) throws IOException {
        List<Claim> claims = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            
            // Skip header line
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IOException("Empty CSV file");
            }
            
            String line;
            int lineNumber = 1;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                try {
                    Claim claim = parseCsvLine(line);
                    claims.add(claim);
                    
                    // Progress indicator for large files
                    if (claims.size() % 100000 == 0) {
                        System.out.println("Loaded " + claims.size() + " claims...");
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing line " + lineNumber + ": " + e.getMessage());
                    // Continue processing other lines
                }
            }
        }
        
        return claims;
    }
    
    /**
     * Parse a single CSV line into a Claim object.
     * 
     * CSV Format:
     * claim_number,transaction_type,date_of_service,received_timestamp,
     * member_id,person_code,pharmacy_id,pharmacy_npi,
     * prescription_number,ndc,quantity_dispensed,days_supply,
     * refill_number,daw_code,date_written,
     * prescriber_npi,prescriber_id,
     * ingredient_cost_submitted,dispensing_fee_submitted,
     * patient_pay_amount,plan_pay_amount,tax_amount
     * 
     * @param line CSV line to parse
     * @return Claim object
     */
    private Claim parseCsvLine(String line) {
        String[] fields = line.split(",", -1);
        
        if (fields.length < 22) {
            throw new IllegalArgumentException("Invalid CSV line: expected 22 fields, got " + fields.length);
        }
        
        Claim claim = new Claim();
        
        int idx = 0;
        
        // Basic claim info
        claim.setClaimNumber(fields[idx++].trim());
        claim.setTransactionType(fields[idx++].trim());
        
        // Dates
        String dateOfService = fields[idx++].trim();
        if (!dateOfService.isEmpty()) {
            claim.setDateOfService(LocalDate.parse(dateOfService, DATE_FORMATTER));
        }
        
        String receivedTimestamp = fields[idx++].trim();
        if (!receivedTimestamp.isEmpty()) {
            claim.setReceivedTimestamp(LocalDateTime.parse(receivedTimestamp, DATETIME_FORMATTER));
        }
        
        // Member info
        String memberId = fields[idx++].trim();
        if (!memberId.isEmpty()) {
            // Convert member number string to Long for lookup
            claim.setMemberId(Long.parseLong(memberId));
        }
        
        claim.setPersonCode(fields[idx++].trim());
        
        // Pharmacy info
        String pharmacyId = fields[idx++].trim();
        if (!pharmacyId.isEmpty()) {
            // For simulation, we'll use a hash of NCPDP ID
            claim.setPharmacyId((long) pharmacyId.hashCode());
        }
        
        claim.setPharmacyNpi(fields[idx++].trim());
        
        // Prescription info
        claim.setPrescriptionNumber(fields[idx++].trim());
        claim.setNdc(fields[idx++].trim());
        
        String quantityDispensed = fields[idx++].trim();
        if (!quantityDispensed.isEmpty()) {
            claim.setQuantityDispensed(new BigDecimal(quantityDispensed));
        }
        
        String daysSupply = fields[idx++].trim();
        if (!daysSupply.isEmpty()) {
            claim.setDaysSupply(Integer.parseInt(daysSupply));
        }
        
        String refillNumber = fields[idx++].trim();
        if (!refillNumber.isEmpty()) {
            claim.setRefillNumber(Integer.parseInt(refillNumber));
        }
        
        String dawCode = fields[idx++].trim();
        if (!dawCode.isEmpty()) {
            claim.setDawCode(String.valueOf(dawCode));
        }
        
        // Skip date_written (idx++)
        idx++;
        
        // Prescriber info
        claim.setPrescriberNpi(fields[idx++].trim());
        claim.setPrescriberId(fields[idx++].trim());
        
        // Pricing
        String ingredientCost = fields[idx++].trim();
        if (!ingredientCost.isEmpty()) {
            claim.setIngredientCostSubmitted(new BigDecimal(ingredientCost));
        }
        
        String dispensingFee = fields[idx++].trim();
        if (!dispensingFee.isEmpty()) {
            claim.setDispensingFeeSubmitted(new BigDecimal(dispensingFee));
        }
        
        String patientPay = fields[idx++].trim();
        if (!patientPay.isEmpty()) {
            claim.setPatientPayAmount(new BigDecimal(patientPay));
        }
        
        String planPay = fields[idx++].trim();
        if (!planPay.isEmpty()) {
            claim.setPlanPayAmount(new BigDecimal(planPay));
        }
        
        String taxAmount = fields[idx++].trim();
        if (!taxAmount.isEmpty()) {
            claim.setTaxAmount(new BigDecimal(taxAmount));
        }
        
        // Set initial status as PENDING (will be adjudicated)
        claim.setStatus("PENDING");
        
        return claim;
    }
    
    /**
     * Get the count of available claims across all CSV files.
     *
     * @return Number of claims (excluding headers)
     * @throws IOException if files cannot be read
     */
    public long getClaimCount() throws IOException {
        long totalCount = 0;
        int fileNumber = 1;
        boolean foundFiles = false;
        
        // Try multiple files first
        while (true) {
            String filename = String.format("%s%02d%s", FILE_PREFIX, fileNumber, FILE_SUFFIX);
            InputStream is = getClass().getResourceAsStream(filename);
            
            if (is == null) {
                break;
            }
            
            foundFiles = true;
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                
                // Skip header
                reader.readLine();
                
                long count = 0;
                while (reader.readLine() != null) {
                    count++;
                }
                
                totalCount += count;
            }
            
            fileNumber++;
        }
        
        // If no split files, try single file
        if (!foundFiles) {
            String singleFilename = "/data/us_pharmacy_claims_simulation_1m.csv";
            try (InputStream is = getClass().getResourceAsStream(singleFilename)) {
                if (is == null) {
                    return 0;
                }
                
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    
                    // Skip header
                    reader.readLine();
                    
                    while (reader.readLine() != null) {
                        totalCount++;
                    }
                }
            }
        }
        
        return totalCount;
    }
}

// Made with Bob