package dejanlazic.playground.inmemory.rdbms.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * POJO representing an enrollment record
 * Maps to the enrollment table in the database
 */
public class Enrollment {
    
    private UUID enrollmentId;
    private String memberNumber;
    private String planCode;
    private String groupNumber;
    private LocalDate effectiveDate;
    private LocalDate terminationDate;
    private String relationship;
    private boolean isActive;
    
    // Constructors
    public Enrollment() {
    }
    
    public Enrollment(String memberNumber, String planCode, String groupNumber, 
                     LocalDate effectiveDate, String relationship) {
        this.memberNumber = memberNumber;
        this.planCode = planCode;
        this.groupNumber = groupNumber;
        this.effectiveDate = effectiveDate;
        this.relationship = relationship;
        this.isActive = true;
    }
    
    // Getters and Setters
    public UUID getEnrollmentId() {
        return enrollmentId;
    }
    
    public void setEnrollmentId(UUID enrollmentId) {
        this.enrollmentId = enrollmentId;
    }
    
    public String getMemberNumber() {
        return memberNumber;
    }
    
    public void setMemberNumber(String memberNumber) {
        this.memberNumber = memberNumber;
    }
    
    public String getPlanCode() {
        return planCode;
    }
    
    public void setPlanCode(String planCode) {
        this.planCode = planCode;
    }
    
    public String getGroupNumber() {
        return groupNumber;
    }
    
    public void setGroupNumber(String groupNumber) {
        this.groupNumber = groupNumber;
    }
    
    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }
    
    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }
    
    public LocalDate getTerminationDate() {
        return terminationDate;
    }
    
    public void setTerminationDate(LocalDate terminationDate) {
        this.terminationDate = terminationDate;
    }
    
    public String getRelationship() {
        return relationship;
    }
    
    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    // Utility methods
    
    /**
     * Check if enrollment is currently active based on dates
     */
    public boolean isCurrentlyActive() {
        LocalDate now = LocalDate.now();
        return effectiveDate != null && 
               !effectiveDate.isAfter(now) && 
               (terminationDate == null || terminationDate.isAfter(now));
    }
    
    /**
     * Get enrollment duration in days
     */
    public long getEnrollmentDurationDays() {
        if (effectiveDate == null) {
            return 0;
        }
        LocalDate endDate = terminationDate != null ? terminationDate : LocalDate.now();
        return java.time.temporal.ChronoUnit.DAYS.between(effectiveDate, endDate);
    }
    
    /**
     * Check if this is a primary enrollment (SELF relationship)
     */
    public boolean isPrimaryEnrollment() {
        return "SELF".equalsIgnoreCase(relationship);
    }
    
    /**
     * Check if this is a dependent enrollment
     */
    public boolean isDependentEnrollment() {
        return relationship != null && 
               (relationship.equalsIgnoreCase("SPOUSE") || 
                relationship.equalsIgnoreCase("CHILD") || 
                relationship.equalsIgnoreCase("DEPENDENT"));
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Enrollment that = (Enrollment) o;
        return Objects.equals(enrollmentId, that.enrollmentId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(enrollmentId);
    }
    
    @Override
    public String toString() {
        return "Enrollment{" +
                "enrollmentId=" + enrollmentId +
                ", memberNumber='" + memberNumber + '\'' +
                ", planCode='" + planCode + '\'' +
                ", groupNumber='" + groupNumber + '\'' +
                ", effectiveDate=" + effectiveDate +
                ", terminationDate=" + terminationDate +
                ", relationship='" + relationship + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}


