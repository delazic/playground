package dejanlazic.playground.inmemory.rdbms.converter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import dejanlazic.playground.inmemory.rdbms.model.ClaimLine;

/**
 * Converter to read and parse claim lines from CSV files
 * Reads from us_pharmacy_claim_lines_XX.csv files in classpath resources
 */
public class ClaimLineConverter {
    
    private static final Logger LOGGER = Logger.getLogger(ClaimLineConverter.class.getName());
    private static final String CSV_FILE_PREFIX = "data/us_pharmacy_claim_lines_";
    private static final String CSV_FILE_SUFFIX = ".csv";
    private static final String CSV_DELIMITER = ",";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Load all claim lines from all CSV files
     *
     * @return List of ClaimLine objects
     * @throws IOException if files cannot be read
     */
    public List<ClaimLine> loadAllClaimLines() throws IOException {
        List<ClaimLine> claimLines = new ArrayList<>();
        
        // Try to load files sequentially (01, 02, 03, etc.)
        int fileNumber = 1;
        while (true) {
            String fileName = String.format("%s%02d%s", CSV_FILE_PREFIX, fileNumber, CSV_FILE_SUFFIX);
            try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
                if (inputStream == null) {
                    // No more files to load
                    break;
                }
                
                LOGGER.log(Level.INFO, "Loading claim lines from: {0}", fileName);
                List<ClaimLine> linesFromFile = loadFromStream(inputStream, fileName);
                claimLines.addAll(linesFromFile);
                fileNumber++;
            }
        }
        
        if (claimLines.isEmpty()) {
            LOGGER.log(Level.WARNING, "No claim line files found in classpath");
        } else {
            LOGGER.log(Level.INFO, "Total claim lines loaded: {0} from {1} files", 
                new Object[]{claimLines.size(), fileNumber - 1});
        }
        
        return claimLines;
    }
    
    /**
     * Load claim lines from a specific file number
     *
     * @param fileNumber File number (e.g., 1 for _01.csv)
     * @return List of ClaimLine objects
     * @throws IOException if file cannot be read
     */
    public List<ClaimLine> loadClaimLinesFromFile(int fileNumber) throws IOException {
        String fileName = String.format("%s%02d%s", CSV_FILE_PREFIX, fileNumber, CSV_FILE_SUFFIX);
        
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName)) {
            if (inputStream == null) {
                throw new IOException("CSV file not found in classpath: " + fileName);
            }
            
            return loadFromStream(inputStream, fileName);
        }
    }
    
    /**
     * Load claim lines from an input stream
     */
    private List<ClaimLine> loadFromStream(InputStream inputStream, String fileName) throws IOException {
        List<ClaimLine> claimLines = new ArrayList<>();
        
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
                    ClaimLine claimLine = parseLine(line);
                    claimLines.add(claimLine);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error parsing line {0} in {1}: {2}",
                        new Object[]{lineNumber, fileName, e.getMessage()});
                    // Continue processing other lines
                }
            }
        }
        
        return claimLines;
    }
    
    /**
     * Parse a single CSV line into a ClaimLine object
     * 
     * CSV format: claim_line_id,claim_number,line_number,service_date,ndc,drug_name,
     *             quantity_dispensed,days_supply,unit_of_measure,rendering_provider_npi,
     *             service_facility_npi,place_of_service,billed_amount,allowed_amount,
     *             paid_amount,patient_responsibility,copay_amount,coinsurance_amount,
     *             deductible_amount,ingredient_cost,dispensing_fee,sales_tax,line_status,
     *             denial_code,denial_reason,adjustment_reason,prior_auth_number,
     *             formulary_status,tier_level,daw_code,generic_indicator,brand_indicator,
     *             prescription_number,refill_number,date_written,prescriber_npi,
     *             processing_time_ms,created_at
     * 
     * @param line CSV line to parse
     * @return ClaimLine object
     */
    public ClaimLine parseLine(String line) {
        String[] fields = line.split(CSV_DELIMITER, -1); // -1 to keep trailing empty strings
        
        if (fields.length < 38) {
            throw new IllegalArgumentException("Invalid CSV line: expected 38 fields, got " + fields.length);
        }
        
        ClaimLine claimLine = new ClaimLine();
        
        // Identification
        claimLine.setClaimLineId(parseLong(fields[0]));
        claimLine.setClaimNumber(parseString(fields[1]));
        claimLine.setLineNumber(parseInteger(fields[2]));
        
        // Service Information
        claimLine.setServiceDate(parseDate(fields[3]));
        claimLine.setNdc(parseString(fields[4]));
        claimLine.setDrugName(parseString(fields[5]));
        claimLine.setQuantityDispensed(parseBigDecimal(fields[6]));
        claimLine.setDaysSupply(parseInteger(fields[7]));
        claimLine.setUnitOfMeasure(parseString(fields[8]));
        
        // Provider Information
        claimLine.setRenderingProviderNpi(parseString(fields[9]));
        claimLine.setServiceFacilityNpi(parseString(fields[10]));
        claimLine.setPlaceOfService(parseString(fields[11]));
        
        // Financial Information
        claimLine.setBilledAmount(parseBigDecimal(fields[12]));
        claimLine.setAllowedAmount(parseBigDecimal(fields[13]));
        claimLine.setPaidAmount(parseBigDecimal(fields[14]));
        claimLine.setPatientResponsibility(parseBigDecimal(fields[15]));
        claimLine.setCopayAmount(parseBigDecimal(fields[16]));
        claimLine.setCoinsuranceAmount(parseBigDecimal(fields[17]));
        claimLine.setDeductibleAmount(parseBigDecimal(fields[18]));
        claimLine.setIngredientCost(parseBigDecimal(fields[19]));
        claimLine.setDispensingFee(parseBigDecimal(fields[20]));
        claimLine.setSalesTax(parseBigDecimal(fields[21]));
        
        // Adjudication Information
        claimLine.setLineStatus(parseString(fields[22]));
        claimLine.setDenialCode(parseString(fields[23]));
        claimLine.setDenialReason(parseString(fields[24]));
        claimLine.setAdjustmentReason(parseString(fields[25]));
        claimLine.setPriorAuthNumber(parseString(fields[26]));
        
        // Formulary & Coverage
        claimLine.setFormularyStatus(parseString(fields[27]));
        claimLine.setTierLevel(parseInteger(fields[28]));
        claimLine.setDawCode(parseString(fields[29]));
        claimLine.setGenericIndicator(parseBoolean(fields[30]));
        claimLine.setBrandIndicator(parseBoolean(fields[31]));
        
        // Clinical Information
        claimLine.setPrescriptionNumber(parseString(fields[32]));
        claimLine.setRefillNumber(parseInteger(fields[33]));
        claimLine.setDateWritten(parseDate(fields[34]));
        claimLine.setPrescriberNpi(parseString(fields[35]));
        
        // Audit Trail
        claimLine.setProcessingTimeMs(parseInteger(fields[36]));
        claimLine.setCreatedAt(parseDateTime(fields[37]));
        
        return claimLine;
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
     * Parse LocalDate from string (yyyy-MM-dd format)
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
     * Find claim lines by claim number
     * 
     * @param claimNumber Claim number to search for
     * @return List of matching claim lines
     * @throws IOException if files cannot be read
     */
    public List<ClaimLine> findByClaimNumber(String claimNumber) throws IOException {
        List<ClaimLine> allLines = loadAllClaimLines();
        return allLines.stream()
                .filter(line -> claimNumber.equals(line.getClaimNumber()))
                .toList();
    }
    
    /**
     * Find claim lines by status
     * 
     * @param status Line status to search for
     * @return List of matching claim lines
     * @throws IOException if files cannot be read
     */
    public List<ClaimLine> findByStatus(String status) throws IOException {
        List<ClaimLine> allLines = loadAllClaimLines();
        return allLines.stream()
                .filter(line -> status.equalsIgnoreCase(line.getLineStatus()))
                .toList();
    }
    
    /**
     * Find claim lines by tier level
     * 
     * @param tierLevel Tier level to search for
     * @return List of matching claim lines
     * @throws IOException if files cannot be read
     */
    public List<ClaimLine> findByTierLevel(Integer tierLevel) throws IOException {
        List<ClaimLine> allLines = loadAllClaimLines();
        return allLines.stream()
                .filter(line -> tierLevel.equals(line.getTierLevel()))
                .toList();
    }
    
    /**
     * Find approved claim lines only
     * 
     * @return List of approved claim lines
     * @throws IOException if files cannot be read
     */
    public List<ClaimLine> findApprovedLines() throws IOException {
        return findByStatus("APPROVED");
    }
    
    /**
     * Find denied claim lines only
     * 
     * @return List of denied claim lines
     * @throws IOException if files cannot be read
     */
    public List<ClaimLine> findDeniedLines() throws IOException {
        return findByStatus("DENIED");
    }
    
    /**
     * Get count of claim lines in all CSV files
     * 
     * @return Total number of claim lines
     * @throws IOException if files cannot be read
     */
    public int getClaimLineCount() throws IOException {
        return loadAllClaimLines().size();
    }
    
    /**
     * Get statistics about loaded claim lines
     * 
     * @return Statistics string
     * @throws IOException if files cannot be read
     */
    public String getStatistics() throws IOException {
        List<ClaimLine> lines = loadAllClaimLines();
        
        long approvedCount = lines.stream().filter(ClaimLine::isApproved).count();
        long deniedCount = lines.stream().filter(ClaimLine::isDenied).count();
        long pendingCount = lines.stream().filter(ClaimLine::isPending).count();
        long adjustedCount = lines.stream().filter(ClaimLine::isAdjusted).count();
        
        long tier1Count = lines.stream().filter(l -> Integer.valueOf(1).equals(l.getTierLevel())).count();
        long tier2Count = lines.stream().filter(l -> Integer.valueOf(2).equals(l.getTierLevel())).count();
        long tier3Count = lines.stream().filter(l -> Integer.valueOf(3).equals(l.getTierLevel())).count();
        long tier4Count = lines.stream().filter(l -> Integer.valueOf(4).equals(l.getTierLevel())).count();
        long tier5Count = lines.stream().filter(l -> Integer.valueOf(5).equals(l.getTierLevel())).count();
        
        BigDecimal totalBilled = lines.stream()
                .filter(l -> l.getBilledAmount() != null)
                .map(ClaimLine::getBilledAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalPaid = lines.stream()
                .filter(l -> l.getPaidAmount() != null)
                .map(ClaimLine::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalPatientResp = lines.stream()
                .filter(l -> l.getPatientResponsibility() != null)
                .map(ClaimLine::getPatientResponsibility)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return String.format("""
            Claim Line Statistics:
            ----------------------
            Total Claim Lines: %,d
            
            Status Distribution:
              Approved:  %,d (%.1f%%)
              Denied:    %,d (%.1f%%)
              Pending:   %,d (%.1f%%)
              Adjusted:  %,d (%.1f%%)
            
            Tier Distribution:
              Tier 1 (Generic):           %,d (%.1f%%)
              Tier 2 (Preferred Brand):   %,d (%.1f%%)
              Tier 3 (Non-Preferred):     %,d (%.1f%%)
              Tier 4 (Specialty):         %,d (%.1f%%)
              Tier 5 (High-Cost):         %,d (%.1f%%)
            
            Financial Summary:
              Total Billed:              $%,.2f
              Total Paid:                $%,.2f
              Total Patient Responsibility: $%,.2f
            """,
            lines.size(),
            approvedCount, (approvedCount * 100.0 / lines.size()),
            deniedCount, (deniedCount * 100.0 / lines.size()),
            pendingCount, (pendingCount * 100.0 / lines.size()),
            adjustedCount, (adjustedCount * 100.0 / lines.size()),
            tier1Count, (tier1Count * 100.0 / lines.size()),
            tier2Count, (tier2Count * 100.0 / lines.size()),
            tier3Count, (tier3Count * 100.0 / lines.size()),
            tier4Count, (tier4Count * 100.0 / lines.size()),
            tier5Count, (tier5Count * 100.0 / lines.size()),
            totalBilled,
            totalPaid,
            totalPatientResp
        );
    }
    
    /**
     * Print claim line summary to console
     * 
     * @param claimLine Claim line to print
     */
    public void printClaimLineSummary(ClaimLine claimLine) {
        System.out.println("=".repeat(70));
        System.out.println("Claim Line ID: " + claimLine.getClaimLineId());
        System.out.println("Claim Number: " + claimLine.getClaimNumber());
        System.out.println("Line Number: " + claimLine.getLineNumber());
        System.out.println("-".repeat(70));
        System.out.println("Service Date: " + claimLine.getServiceDate());
        System.out.println("NDC: " + claimLine.getFormattedNdc());
        System.out.println("Drug Name: " + claimLine.getDrugName());
        System.out.println("Quantity: " + claimLine.getQuantityDispensed() + " " + claimLine.getUnitOfMeasure());
        System.out.println("Days Supply: " + claimLine.getDaysSupply());
        System.out.println("-".repeat(70));
        System.out.println("Status: " + claimLine.getLineStatus());
        System.out.println("Tier: " + claimLine.getTierDescription());
        System.out.println("-".repeat(70));
        System.out.println("Billed Amount: $" + claimLine.getBilledAmount());
        System.out.println("Allowed Amount: $" + claimLine.getAllowedAmount());
        System.out.println("Paid Amount: $" + claimLine.getPaidAmount());
        System.out.println("Patient Responsibility: $" + claimLine.getPatientResponsibility());
        System.out.println("  Copay: $" + claimLine.getCopayAmount());
        System.out.println("  Coinsurance: $" + claimLine.getCoinsuranceAmount());
        System.out.println("  Deductible: $" + claimLine.getDeductibleAmount());
        System.out.println("=".repeat(70));
    }
}

// Made with Bob
