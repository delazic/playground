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

import dejanlazic.playground.inmemory.rdbms.model.Member;
import dejanlazic.playground.inmemory.rdbms.model.Member.Gender;

/**
 * Converter to read and parse members from CSV files
 * Reads from us_pharmacy_members_01.csv through us_pharmacy_members_10.csv from classpath resources
 */
public class MemberConverter {
    
    private static final Logger LOGGER = Logger.getLogger(MemberConverter.class.getName());
    private static final String CSV_FILE_PATTERN = "data/us_pharmacy_members_%02d.csv";
    private static final int TOTAL_FILES = 10;
    private static final String CSV_DELIMITER = ",";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * Load all members from all 10 CSV files
     *
     * @return List of Member objects
     * @throws IOException if files cannot be read
     */
    public List<Member> loadAllMembers() throws IOException {
        List<Member> allMembers = new ArrayList<>();
        
        for (int fileNum = 1; fileNum <= TOTAL_FILES; fileNum++) {
            String fileName = String.format(CSV_FILE_PATTERN, fileNum);
            
            List<Member> membersFromFile = loadMembersFromResource(fileName, fileNum);
            allMembers.addAll(membersFromFile);
            
            LOGGER.log(Level.INFO, "Loaded {0} members from {1}",
                new Object[]{membersFromFile.size(), fileName});
        }
        
        LOGGER.log(Level.INFO, "Total members loaded: {0}", allMembers.size());
        return allMembers;
    }
    
    /**
     * Load members from a single CSV file from classpath
     *
     * @param resourceName Resource name in classpath
     * @param fileNum File number for logging
     * @return List of Member objects from this file
     * @throws IOException if file cannot be read
     */
    private List<Member> loadMembersFromResource(String resourceName, int fileNum) throws IOException {
        List<Member> members = new ArrayList<>();
        
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                LOGGER.log(Level.WARNING, "CSV file not found in classpath: {0}", resourceName);
                return members;
            }
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                // Skip header line
                String headerLine = reader.readLine();
                if (headerLine == null) {
                    throw new IOException("CSV file is empty: " + resourceName);
                }
                
                String line;
                int lineNumber = 1;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    try {
                        Member member = parseLine(line);
                        members.add(member);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error parsing file {0}, line {1}: {2}",
                            new Object[]{fileNum, lineNumber, e.getMessage()});
                        // Continue processing other lines
                    }
                }
            }
        }
        
        return members;
    }
    
    /**
     * Parse a single CSV line into a Member object
     * 
     * CSV format: member_number,first_name,last_name,date_of_birth,gender,address,city,state,zip_code,phone,email
     * 
     * @param line CSV line to parse
     * @return Member object
     */
    public Member parseLine(String line) {
        String[] fields = line.split(CSV_DELIMITER, -1); // -1 to keep trailing empty strings
        
        if (fields.length < 11) {
            throw new IllegalArgumentException("Invalid CSV line: expected 11 fields, got " + fields.length);
        }
        
        Member member = new Member();
        
        // Basic member information
        member.setMemberNumber(fields[0].trim());
        member.setFirstName(fields[1].trim());
        member.setLastName(fields[2].trim());
        member.setDateOfBirth(parseDate(fields[3]));
        member.setGender(parseGender(fields[4]));
        
        // Address information
        member.setAddress(parseString(fields[5]));
        member.setCity(parseString(fields[6]));
        member.setState(parseString(fields[7]));
        member.setZipCode(parseString(fields[8]));
        
        // Contact information
        member.setPhone(parseString(fields[9]));
        member.setEmail(parseString(fields[10]));
        
        return member;
    }
    
    /**
     * Parse date string in yyyy-MM-dd format
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(dateStr.trim(), DATE_FORMATTER);
    }
    
    /**
     * Parse gender from string (M, F, U)
     */
    private Gender parseGender(String genderStr) {
        if (genderStr == null || genderStr.trim().isEmpty()) {
            return Gender.U;
        }
        String normalized = genderStr.trim().toUpperCase();
        return switch (normalized) {
            case "M" -> Gender.M;
            case "F" -> Gender.F;
            case "U" -> Gender.U;
            default -> throw new IllegalArgumentException("Invalid gender: " + genderStr);
        };
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
     * Find a member by member number
     * 
     * @param memberNumber Member number to search for
     * @return Member if found, null otherwise
     * @throws IOException if files cannot be read
     */
    public Member findByMemberNumber(String memberNumber) throws IOException {
        List<Member> members = loadAllMembers();
        return members.stream()
                .filter(member -> member.getMemberNumber().equals(memberNumber))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Find members by state
     * 
     * @param state State code (e.g., "CA", "TX")
     * @return List of matching members
     * @throws IOException if files cannot be read
     */
    public List<Member> findByState(String state) throws IOException {
        List<Member> members = loadAllMembers();
        return members.stream()
                .filter(member -> state.equalsIgnoreCase(member.getState()))
                .toList();
    }
    
    /**
     * Find members by gender
     * 
     * @param gender Gender to search for
     * @return List of matching members
     * @throws IOException if files cannot be read
     */
    public List<Member> findByGender(Gender gender) throws IOException {
        List<Member> members = loadAllMembers();
        return members.stream()
                .filter(member -> member.getGender() == gender)
                .toList();
    }
    
    /**
     * Find members by age range
     * 
     * @param minAge Minimum age (inclusive)
     * @param maxAge Maximum age (inclusive)
     * @return List of matching members
     * @throws IOException if files cannot be read
     */
    public List<Member> findByAgeRange(int minAge, int maxAge) throws IOException {
        List<Member> members = loadAllMembers();
        return members.stream()
                .filter(member -> {
                    int age = member.getAge();
                    return age >= minAge && age <= maxAge;
                })
                .toList();
    }
    
    /**
     * Get count of members across all CSV files
     * 
     * @return Total number of members
     * @throws IOException if files cannot be read
     */
    public int getMemberCount() throws IOException {
        return loadAllMembers().size();
    }
    
    /**
     * Get statistics about loaded members
     * 
     * @return Statistics string
     * @throws IOException if files cannot be read
     */
    public String getStatistics() throws IOException {
        List<Member> members = loadAllMembers();
        
        long maleCount = members.stream().filter(m -> m.getGender() == Gender.M).count();
        long femaleCount = members.stream().filter(m -> m.getGender() == Gender.F).count();
        long unknownCount = members.stream().filter(m -> m.getGender() == Gender.U).count();
        
        long withEmail = members.stream().filter(m -> m.getEmail() != null && !m.getEmail().isEmpty()).count();
        long withPhone = members.stream().filter(m -> m.getPhone() != null && !m.getPhone().isEmpty()).count();
        
        return String.format("""
            Member Statistics:
            ------------------
            Total Members: %,d
            Gender Distribution:
              Male:    %,d (%.1f%%)
              Female:  %,d (%.1f%%)
              Unknown: %,d (%.1f%%)
            Contact Information:
              With Email: %,d (%.1f%%)
              With Phone: %,d (%.1f%%)
            """,
            members.size(),
            maleCount, (maleCount * 100.0 / members.size()),
            femaleCount, (femaleCount * 100.0 / members.size()),
            unknownCount, (unknownCount * 100.0 / members.size()),
            withEmail, (withEmail * 100.0 / members.size()),
            withPhone, (withPhone * 100.0 / members.size())
        );
    }
    
    /**
     * Print member summary to console
     * 
     * @param member Member to print
     */
    public void printMemberSummary(Member member) {
        System.out.println("=".repeat(70));
        System.out.println("Member Number: " + member.getMemberNumber());
        System.out.println("Name: " + member.getFullName());
        System.out.println("Date of Birth: " + member.getDateOfBirth() + " (Age: " + member.getAge() + ")");
        System.out.println("Gender: " + member.getGender());
        System.out.println("-".repeat(70));
        System.out.println("Address: " + member.getFullAddress());
        System.out.println("-".repeat(70));
        System.out.println("Phone: " + (member.getPhone() != null ? member.getPhone() : "N/A"));
        System.out.println("Email: " + (member.getEmail() != null ? member.getEmail() : "N/A"));
        System.out.println("=".repeat(70));
    }
}


