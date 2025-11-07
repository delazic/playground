package dejanlazic.playground.inmemory.rdbms.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * POJO representing a formulary
 * Maps to the formulary table in the database
 */
public class Formulary {
    
    private UUID formularyId;
    private UUID planId;
    private String planCode;  // Business key from CSV, used to lookup planId
    private String formularyCode;  // Unique formulary code from CSV
    private String formularyName;
    private LocalDate effectiveDate;
    private LocalDate terminationDate;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public Formulary() {
    }
    
    public Formulary(UUID planId, String formularyName, LocalDate effectiveDate) {
        this.planId = planId;
        this.formularyName = formularyName;
        this.effectiveDate = effectiveDate;
        this.isActive = true;
    }
    
    public Formulary(UUID planId, String formularyName, LocalDate effectiveDate, LocalDate terminationDate) {
        this.planId = planId;
        this.formularyName = formularyName;
        this.effectiveDate = effectiveDate;
        this.terminationDate = terminationDate;
        this.isActive = true;
    }
    
    // Getters and Setters
    public UUID getFormularyId() {
        return formularyId;
    }
    
    public void setFormularyId(UUID formularyId) {
        this.formularyId = formularyId;
    }
    
    public UUID getPlanId() {
        return planId;
    }
    
    public void setPlanId(UUID planId) {
        this.planId = planId;
    }
    
    public String getPlanCode() {
        return planCode;
    }
    
    public void setPlanCode(String planCode) {
        this.planCode = planCode;
    }
    
    public String getFormularyCode() {
        return formularyCode;
    }
    
    public void setFormularyCode(String formularyCode) {
        this.formularyCode = formularyCode;
    }
    
    public String getFormularyName() {
        return formularyName;
    }
    
    public void setFormularyName(String formularyName) {
        this.formularyName = formularyName;
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
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Utility methods
    
    /**
     * Check if formulary is currently active based on dates
     */
    public boolean isCurrentlyActive() {
        LocalDate now = LocalDate.now();
        boolean afterEffective = effectiveDate == null || !now.isBefore(effectiveDate);
        boolean beforeTermination = terminationDate == null || now.isBefore(terminationDate);
        return isActive && afterEffective && beforeTermination;
    }
    
    /**
     * Check if formulary is expired
     */
    public boolean isExpired() {
        if (terminationDate == null) {
            return false;
        }
        return LocalDate.now().isAfter(terminationDate);
    }
    
    /**
     * Check if formulary is future-dated (not yet effective)
     */
    public boolean isFutureDated() {
        if (effectiveDate == null) {
            return false;
        }
        return LocalDate.now().isBefore(effectiveDate);
    }
    
    /**
     * Get the number of days until effective date
     * Returns 0 if already effective or no effective date
     */
    public long getDaysUntilEffective() {
        if (effectiveDate == null || !isFutureDated()) {
            return 0;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), effectiveDate);
    }
    
    /**
     * Get the number of days until termination
     * Returns -1 if no termination date or already terminated
     */
    public long getDaysUntilTermination() {
        if (terminationDate == null) {
            return -1;
        }
        if (isExpired()) {
            return -1;
        }
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), terminationDate);
    }
    
    /**
     * Get formulary status as string
     */
    public String getStatus() {
        if (!isActive) {
            return "INACTIVE";
        }
        if (isFutureDated()) {
            return "FUTURE";
        }
        if (isExpired()) {
            return "EXPIRED";
        }
        return "ACTIVE";
    }
    
    /**
     * Check if dates are valid (termination after effective)
     */
    public boolean hasValidDates() {
        if (effectiveDate == null) {
            return false;
        }
        if (terminationDate == null) {
            return true;
        }
        return !terminationDate.isBefore(effectiveDate);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Formulary formulary = (Formulary) o;
        return Objects.equals(formularyId, formulary.formularyId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(formularyId);
    }
    
    @Override
    public String toString() {
        return "Formulary{" +
                "formularyId=" + formularyId +
                ", planId=" + planId +
                ", formularyName='" + formularyName + '\'' +
                ", effectiveDate=" + effectiveDate +
                ", terminationDate=" + terminationDate +
                ", isActive=" + isActive +
                ", status=" + getStatus() +
                '}';
    }
}
