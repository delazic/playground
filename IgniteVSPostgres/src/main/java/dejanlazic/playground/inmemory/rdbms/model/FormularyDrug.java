package dejanlazic.playground.inmemory.rdbms.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * POJO representing a formulary-drug relationship
 * Maps to the formulary_drug table in the database
 * 
 * This is a junction table that links formularies to drugs with specific coverage rules:
 * - Tier assignment (1-5)
 * - Status (PREFERRED, NON_PREFERRED, SPECIALTY)
 * - Utilization management requirements (prior auth, step therapy, quantity limits)
 */
public class FormularyDrug {
    
    private UUID formularyDrugId;
    private UUID formularyId;
    private UUID drugId;
    private String formularyCode;  // Business key from CSV, used to lookup formularyId
    private String ndcCode;        // Business key from CSV, used to lookup drugId
    private int tier;
    private String status;
    private boolean requiresPriorAuth;
    private boolean requiresStepTherapy;
    private Integer quantityLimit;
    private Integer daysSupplyLimit;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public FormularyDrug() {
    }
    
    public FormularyDrug(UUID formularyId, UUID drugId, int tier) {
        this.formularyId = formularyId;
        this.drugId = drugId;
        this.tier = tier;
        this.status = "PREFERRED";
        this.requiresPriorAuth = false;
        this.requiresStepTherapy = false;
    }
    
    public FormularyDrug(UUID formularyId, UUID drugId, int tier, String status) {
        this.formularyId = formularyId;
        this.drugId = drugId;
        this.tier = tier;
        this.status = status;
        this.requiresPriorAuth = false;
        this.requiresStepTherapy = false;
    }
    
    // Getters and Setters
    public UUID getFormularyDrugId() {
        return formularyDrugId;
    }
    
    public void setFormularyDrugId(UUID formularyDrugId) {
        this.formularyDrugId = formularyDrugId;
    }
    
    public UUID getFormularyId() {
        return formularyId;
    }
    
    public void setFormularyId(UUID formularyId) {
        this.formularyId = formularyId;
    }
    
    public UUID getDrugId() {
        return drugId;
    }
    
    public void setDrugId(UUID drugId) {
        this.drugId = drugId;
    }
    
    public String getFormularyCode() {
        return formularyCode;
    }
    
    public void setFormularyCode(String formularyCode) {
        this.formularyCode = formularyCode;
    }
    
    public String getNdcCode() {
        return ndcCode;
    }
    
    public void setNdcCode(String ndcCode) {
        this.ndcCode = ndcCode;
    }
    
    public int getTier() {
        return tier;
    }
    
    public void setTier(int tier) {
        this.tier = tier;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public boolean isRequiresPriorAuth() {
        return requiresPriorAuth;
    }
    
    public void setRequiresPriorAuth(boolean requiresPriorAuth) {
        this.requiresPriorAuth = requiresPriorAuth;
    }
    
    public boolean isRequiresStepTherapy() {
        return requiresStepTherapy;
    }
    
    public void setRequiresStepTherapy(boolean requiresStepTherapy) {
        this.requiresStepTherapy = requiresStepTherapy;
    }
    
    public Integer getQuantityLimit() {
        return quantityLimit;
    }
    
    public void setQuantityLimit(Integer quantityLimit) {
        this.quantityLimit = quantityLimit;
    }
    
    public Integer getDaysSupplyLimit() {
        return daysSupplyLimit;
    }
    
    public void setDaysSupplyLimit(Integer daysSupplyLimit) {
        this.daysSupplyLimit = daysSupplyLimit;
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
     * Check if this drug has any utilization management requirements
     */
    public boolean hasUtilizationManagement() {
        return requiresPriorAuth || requiresStepTherapy || quantityLimit != null;
    }
    
    /**
     * Check if this is a preferred drug
     */
    public boolean isPreferred() {
        return "PREFERRED".equalsIgnoreCase(status);
    }
    
    /**
     * Check if this is a specialty drug
     */
    public boolean isSpecialty() {
        return "SPECIALTY".equalsIgnoreCase(status);
    }
    
    /**
     * Check if this is a non-preferred drug
     */
    public boolean isNonPreferred() {
        return "NON_PREFERRED".equalsIgnoreCase(status);
    }
    
    /**
     * Get tier description
     */
    public String getTierDescription() {
        return switch (tier) {
            case 1 -> "Tier 1 (Generic)";
            case 2 -> "Tier 2 (Preferred Brand)";
            case 3 -> "Tier 3 (Non-Preferred Brand)";
            case 4 -> "Tier 4 (Specialty)";
            case 5 -> "Tier 5 (High-Cost Specialty)";
            default -> "Unknown Tier";
        };
    }
    
    /**
     * Check if tier is valid (1-5)
     */
    public boolean hasValidTier() {
        return tier >= 1 && tier <= 5;
    }
    
    /**
     * Get summary of utilization management requirements
     */
    public String getUtilizationManagementSummary() {
        if (!hasUtilizationManagement()) {
            return "No restrictions";
        }
        
        StringBuilder sb = new StringBuilder();
        if (requiresPriorAuth) {
            sb.append("Prior Auth");
        }
        if (requiresStepTherapy) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("Step Therapy");
        }
        if (quantityLimit != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("Qty Limit: ").append(quantityLimit);
            if (daysSupplyLimit != null) {
                sb.append(" (").append(daysSupplyLimit).append(" days)");
            }
        }
        return sb.toString();
    }
    
    /**
     * Check if quantity limits are defined
     */
    public boolean hasQuantityLimits() {
        return quantityLimit != null || daysSupplyLimit != null;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FormularyDrug that = (FormularyDrug) o;
        return Objects.equals(formularyDrugId, that.formularyDrugId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(formularyDrugId);
    }
    
    @Override
    public String toString() {
        return "FormularyDrug{" +
                "formularyDrugId=" + formularyDrugId +
                ", formularyId=" + formularyId +
                ", drugId=" + drugId +
                ", tier=" + tier +
                ", status='" + status + '\'' +
                ", requiresPriorAuth=" + requiresPriorAuth +
                ", requiresStepTherapy=" + requiresStepTherapy +
                ", quantityLimit=" + quantityLimit +
                ", daysSupplyLimit=" + daysSupplyLimit +
                '}';
    }
}
