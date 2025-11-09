package dejanlazic.playground.inmemory.ignite;

import dejanlazic.playground.inmemory.ignite.dao.MemberDAO;
import dejanlazic.playground.inmemory.ignite.model.Member;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Apache Ignite Application Demo
 * Demonstrates CRUD operations using Ignite as an in-memory database
 */
public class IgniteApp {
    private static final Logger LOGGER = Logger.getLogger(IgniteApp.class.getName());
    
    public static void main(String[] args) {
        printHeader("Apache Ignite In-Memory Database Demo");
        
        // Initialize Ignite connector
        IgniteConnector connector = new IgniteConnector();
        
        try {
            // Test connection
            if (!testConnection(connector)) {
                System.err.println("❌ Failed to connect to Ignite cluster");
                return;
            }
            
            // Activate cluster (required for persistence)
            connector.activateCluster();
            
            // Create DAO
            MemberDAO memberDAO = new MemberDAO(connector);
            
            // Run CRUD operations
            System.out.println("\n" + "=".repeat(60));
            System.out.println("Running CRUD Operations");
            System.out.println("=".repeat(60) + "\n");
            
            // CREATE - Insert sample members
            createSampleMembers(memberDAO);
            
            // READ - Query members
            readMembers(memberDAO);
            
            // UPDATE - Update a member
            updateMember(memberDAO);
            
            // DELETE - Delete a member
            deleteMember(memberDAO);
            
            // Final count
            System.out.println("\n" + "=".repeat(60));
            System.out.println("Final Statistics");
            System.out.println("=".repeat(60));
            long finalCount = memberDAO.count();
            System.out.println("Total members in cache: " + finalCount);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error running Ignite application", e);
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Close connection
            System.out.println("\n" + "=".repeat(60));
            System.out.println("Closing Ignite Connection");
            System.out.println("=".repeat(60));
            connector.close();
            System.out.println("✅ Connection closed successfully");
        }
    }
    
    private static boolean testConnection(IgniteConnector connector) {
        System.out.println("Testing Ignite connectivity...");
        
        if (!connector.testConnection()) {
            System.out.println("✗ Connection failed!");
            return false;
        }
        
        System.out.println("✓ Connection successful!");
        System.out.println("✓ " + connector.getClusterInfo());
        return true;
    }
    
    private static void createSampleMembers(MemberDAO memberDAO) {
        printHeader("CREATE - Inserting Sample Members");
        
        List<Member> members = new ArrayList<>();
        
        // Create sample members
        for (int i = 1; i <= 10; i++) {
            Member member = new Member();
            member.setMemberId((long) i);
            member.setMemberNumber("MEM" + String.format("%06d", i));
            member.setFirstName("John" + i);
            member.setLastName("Doe" + i);
            member.setDateOfBirth(Date.valueOf("1980-01-15"));
            member.setGender(i % 2 == 0 ? "M" : "F");
            member.setAddress(i + " Main Street");
            member.setCity("New York");
            member.setState("NY");
            member.setZipCode("10001");
            member.setPhone("555-000-" + String.format("%04d", i));
            member.setEmail("john.doe" + i + "@example.com");
            member.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            member.setUpdatedAt(new Timestamp(System.currentTimeMillis()));
            
            members.add(member);
        }
        
        int inserted = memberDAO.insertBatch(members);
        System.out.println("✅ Inserted " + inserted + " members");
        
        long count = memberDAO.count();
        System.out.println("📊 Total members in cache: " + count);
    }
    
    private static void readMembers(MemberDAO memberDAO) {
        printHeader("READ - Querying Members");
        
        // Find all members
        List<Member> allMembers = memberDAO.findAll();
        System.out.println("📖 Found " + allMembers.size() + " members");
        
        // Display first 5
        System.out.println("\nFirst 5 members:");
        System.out.println("-".repeat(80));
        for (int i = 0; i < Math.min(5, allMembers.size()); i++) {
            Member m = allMembers.get(i);
            System.out.printf("%-12s | %-20s | %-15s | %-10s%n",
                m.getMemberNumber(),
                m.getFullName(),
                m.getCity() + ", " + m.getState(),
                m.getGender());
        }
        System.out.println("-".repeat(80));
        
        // Find by member number
        System.out.println("\nFinding member by number 'MEM000001':");
        Optional<Member> member = memberDAO.findByMemberNumber("MEM000001");
        if (member.isPresent()) {
            System.out.println("✅ Found: " + member.get().getFullName());
        } else {
            System.out.println("⚠️  Member not found");
        }
        
        // Find by state
        System.out.println("\nFinding members in NY:");
        List<Member> nyMembers = memberDAO.findByState("NY");
        System.out.println("✅ Found " + nyMembers.size() + " members in NY");
    }
    
    private static void updateMember(MemberDAO memberDAO) {
        printHeader("UPDATE - Updating a Member");
        
        Optional<Member> memberOpt = memberDAO.findByMemberNumber("MEM000001");
        if (memberOpt.isEmpty()) {
            System.out.println("⚠️  Member not found");
            return;
        }
        
        Member member = memberOpt.get();
        String oldCity = member.getCity();
        String oldEmail = member.getEmail();
        
        // Update member
        member.setCity("Los Angeles");
        member.setState("CA");
        member.setEmail("updated.email@example.com");
        
        boolean updated = memberDAO.update(member);
        if (updated) {
            System.out.println("✅ Successfully updated member: " + member.getMemberNumber());
            System.out.println("   Old city: " + oldCity);
            System.out.println("   New city: " + member.getCity());
            System.out.println("   Old email: " + oldEmail);
            System.out.println("   New email: " + member.getEmail());
        } else {
            System.out.println("⚠️  Update failed");
        }
    }
    
    private static void deleteMember(MemberDAO memberDAO) {
        printHeader("DELETE - Deleting a Member");
        
        long countBefore = memberDAO.count();
        System.out.println("Members before delete: " + countBefore);
        
        // Delete member with ID 10
        boolean deleted = memberDAO.delete(10L);
        if (deleted) {
            System.out.println("✅ Successfully deleted member with ID: 10");
        } else {
            System.out.println("⚠️  Member not found or delete failed");
        }
        
        long countAfter = memberDAO.count();
        System.out.println("Members after delete: " + countAfter);
    }
    
    private static void printHeader(String title) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println(title);
        System.out.println("=".repeat(60) + "\n");
    }
}

// Made with Bob
