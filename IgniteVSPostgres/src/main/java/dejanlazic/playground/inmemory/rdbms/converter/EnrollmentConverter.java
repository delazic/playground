package dejanlazic.playground.inmemory.rdbms.converter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import dejanlazic.playground.inmemory.rdbms.model.Enrollment;

/**
 * Converter class to load Enrollment data from CSV files from classpath resources
 * Handles multiple CSV files with pattern: us_pharmacy_enrollments_##.csv
 */
public class EnrollmentConverter {
    
    private static final Logger LOGGER = Logger.getLogger(EnrollmentConverter.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String CSV_FILE_PATTERN = "data/us_pharmacy_enrollments_%02d.csv";
    private static final int TOTAL_FILES = 20; // us_pharmacy_enrollments_01.csv through us_pharmacy_enrollments_20.csv
    
    /**
     * Load all enrollments from all CSV files
     * @return List of all enrollments
     * @throws IOException if file reading fails
     */
    public List<Enrollment> loadAllEnrollments() throws IOException {
        List<Enrollment> allEnrollments = new ArrayList<>();
        
        for (int fileNum = 1; fileNum <= TOTAL_FILES; fileNum++) {
            String resourceName = String.format(CSV_FILE_PATTERN, fileNum);
            
            List<Enrollment> enrollments = loadEnrollmentsFromResource(resourceName);
            allEnrollments.addAll(enrollments);
            
            LOGGER.log(Level.INFO, "Loaded {0} enrollments from {1}",
                new Object[]{enrollments.size(), resourceName});
        }
        
        LOGGER.log(Level.INFO, "Total enrollments loaded: {0}", allEnrollments.size());
        return allEnrollments;
    }
    
    /**
     * Load enrollments from a specific file number
     * @param fileNumber File number (e.g., 1 for us_pharmacy_enrollments_01.csv)
     * @return List of enrollments from the file
     * @throws IOException if file reading fails
     */
    public List<Enrollment> loadEnrollmentsFromFileNumber(int fileNumber) throws IOException {
        String resourceName = String.format(CSV_FILE_PATTERN, fileNumber);
        return loadEnrollmentsFromResource(resourceName);
    }
    
    /**
     * Load enrollments from classpath resource
     * @param resourceName Resource name
     * @return List of enrollments
     * @throws IOException if resource reading fails
     */
    public List<Enrollment> loadEnrollmentsFromResource(String resourceName) throws IOException {
        List<Enrollment> enrollments = new ArrayList<>();
        
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            
            if (is == null) {
                throw new IOException("Resource not found: " + resourceName);
            }
            
            // Skip header
            reader.readLine();
            
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    Enrollment enrollment = parseEnrollmentLine(line);
                    if (enrollment != null) {
                        enrollments.add(enrollment);
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error parsing line: {0}", e.getMessage());
                }
            }
        }
        
        return enrollments;
    }
    
    /**
     * Parse a single CSV line into an Enrollment object
     * CSV format: member_number,plan_code,group_number,effective_date,termination_date,relationship,is_active
     */
    private Enrollment parseEnrollmentLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        
        String[] fields = line.split(",", -1);
        if (fields.length < 7) {
            LOGGER.log(Level.WARNING, "Invalid line format (expected 7 fields): {0}", line);
            return null;
        }
        
        Enrollment enrollment = new Enrollment();
        
        try {
            // member_number
            enrollment.setMemberNumber(fields[0].trim());
            
            // plan_code
            enrollment.setPlanCode(fields[1].trim());
            
            // group_number
            enrollment.setGroupNumber(fields[2].trim());
            
            // effective_date
            String effectiveDateStr = fields[3].trim();
            if (!effectiveDateStr.isEmpty()) {
                enrollment.setEffectiveDate(LocalDate.parse(effectiveDateStr, DATE_FORMATTER));
            }
            
            // termination_date (optional)
            String terminationDateStr = fields[4].trim();
            if (!terminationDateStr.isEmpty()) {
                enrollment.setTerminationDate(LocalDate.parse(terminationDateStr, DATE_FORMATTER));
            }
            
            // relationship
            enrollment.setRelationship(fields[5].trim());
            
            // is_active
            String isActiveStr = fields[6].trim();
            enrollment.setActive("true".equalsIgnoreCase(isActiveStr));
            
            return enrollment;
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error parsing enrollment data: {0}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Get count of available CSV files
     * @return Number of CSV files (fixed at 20)
     */
    public int getAvailableFileCount() {
        return TOTAL_FILES;
    }
    
    /**
     * Validate enrollment data
     * @param enrollment Enrollment to validate
     * @return true if valid, false otherwise
     */
    public boolean validateEnrollment(Enrollment enrollment) {
        if (enrollment == null) {
            return false;
        }
        
        // Required fields
        if (enrollment.getMemberNumber() == null || enrollment.getMemberNumber().isEmpty()) {
            return false;
        }
        if (enrollment.getPlanCode() == null || enrollment.getPlanCode().isEmpty()) {
            return false;
        }
        if (enrollment.getEffectiveDate() == null) {
            return false;
        }
        
        // Date validation
        if (enrollment.getTerminationDate() != null && 
            enrollment.getTerminationDate().isBefore(enrollment.getEffectiveDate())) {
            return false;
        }
        
        return true;
    }
}


