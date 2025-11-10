package dejanlazic.playground.inmemory.rdbms.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * POJO representing a claim line (claim detail)
 * Maps to the claim_line table in the database
 * Each claim line represents an individual service or product within a claim
 */
public class ClaimLine {
    
    // Identification
    private Long claimLineId;
    private Long claimId;
    private String claimNumber;
    private Integer lineNumber;
    
    // Service Information
    private LocalDate serviceDate;
    private String procedureCode;
    private String ndc;
    private String drugName;
    private BigDecimal quantityDispensed;
    private Integer daysSupply;
    private String unitOfMeasure;
    
    // Provider Information
    private String renderingProviderNpi;
    private String serviceFacilityNpi;
    private String placeOfService;
    
    // Financial Information
    private BigDecimal billedAmount;
    private BigDecimal allowedAmount;
    private BigDecimal paidAmount;
    private BigDecimal patientResponsibility;
    private BigDecimal copayAmount;
    private BigDecimal coinsuranceAmount;
    private BigDecimal deductibleAmount;
    private BigDecimal ingredientCost;
    private BigDecimal dispensingFee;
    private BigDecimal salesTax;
    
    // Adjudication Information
    private String lineStatus;
    private String denialCode;
    private String denialReason;
    private String adjustmentReason;
    private String priorAuthNumber;
    
    // Formulary & Coverage
    private String formularyStatus;
    private Integer tierLevel;
    private String dawCode;
    private Boolean genericIndicator;
    private Boolean brandIndicator;
    
    // Clinical Information
    private String diagnosisPointer;
    private String prescriberNpi;
    private String prescriptionNumber;
    private Integer refillNumber;
    private LocalDate dateWritten;
    
    // Audit Trail
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String processedBy;
    private Integer processingTimeMs;
    
    // Constructors
    public ClaimLine() {
        this.createdAt = LocalDateTime.now();
    }
    
    public ClaimLine(Long claimId, Integer lineNumber) {
        this();
        this.claimId = claimId;
        this.lineNumber = lineNumber;
    }
    
    // Getters and Setters
    public Long getClaimLineId() {
        return claimLineId;
    }
    
    public void setClaimLineId(Long claimLineId) {
        this.claimLineId = claimLineId;
    }
    
    public Long getClaimId() {
        return claimId;
    }
    
    public void setClaimId(Long claimId) {
        this.claimId = claimId;
    }
    
    public String getClaimNumber() {
        return claimNumber;
    }
    
    public void setClaimNumber(String claimNumber) {
        this.claimNumber = claimNumber;
    }
    
    public Integer getLineNumber() {
        return lineNumber;
    }
    
    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }
    
    public LocalDate getServiceDate() {
        return serviceDate;
    }
    
    public void setServiceDate(LocalDate serviceDate) {
        this.serviceDate = serviceDate;
    }
    
    public String getProcedureCode() {
        return procedureCode;
    }
    
    public void setProcedureCode(String procedureCode) {
        this.procedureCode = procedureCode;
    }
    
    public String getNdc() {
        return ndc;
    }
    
    public void setNdc(String ndc) {
        this.ndc = ndc;
    }
    
    public String getDrugName() {
        return drugName;
    }
    
    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }
    
    public BigDecimal getQuantityDispensed() {
        return quantityDispensed;
    }
    
    public void setQuantityDispensed(BigDecimal quantityDispensed) {
        this.quantityDispensed = quantityDispensed;
    }
    
    public Integer getDaysSupply() {
        return daysSupply;
    }
    
    public void setDaysSupply(Integer daysSupply) {
        this.daysSupply = daysSupply;
    }
    
    public String getUnitOfMeasure() {
        return unitOfMeasure;
    }
    
    public void setUnitOfMeasure(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }
    
    public String getRenderingProviderNpi() {
        return renderingProviderNpi;
    }
    
    public void setRenderingProviderNpi(String renderingProviderNpi) {
        this.renderingProviderNpi = renderingProviderNpi;
    }
    
    public String getServiceFacilityNpi() {
        return serviceFacilityNpi;
    }
    
    public void setServiceFacilityNpi(String serviceFacilityNpi) {
        this.serviceFacilityNpi = serviceFacilityNpi;
    }
    
    public String getPlaceOfService() {
        return placeOfService;
    }
    
    public void setPlaceOfService(String placeOfService) {
        this.placeOfService = placeOfService;
    }
    
    public BigDecimal getBilledAmount() {
        return billedAmount;
    }
    
    public void setBilledAmount(BigDecimal billedAmount) {
        this.billedAmount = billedAmount;
    }
    
    public BigDecimal getAllowedAmount() {
        return allowedAmount;
    }
    
    public void setAllowedAmount(BigDecimal allowedAmount) {
        this.allowedAmount = allowedAmount;
    }
    
    public BigDecimal getPaidAmount() {
        return paidAmount;
    }
    
    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }
    
    public BigDecimal getPatientResponsibility() {
        return patientResponsibility;
    }
    
    public void setPatientResponsibility(BigDecimal patientResponsibility) {
        this.patientResponsibility = patientResponsibility;
    }
    
    public BigDecimal getCopayAmount() {
        return copayAmount;
    }
    
    public void setCopayAmount(BigDecimal copayAmount) {
        this.copayAmount = copayAmount;
    }
    
    public BigDecimal getCoinsuranceAmount() {
        return coinsuranceAmount;
    }
    
    public void setCoinsuranceAmount(BigDecimal coinsuranceAmount) {
        this.coinsuranceAmount = coinsuranceAmount;
    }
    
    public BigDecimal getDeductibleAmount() {
        return deductibleAmount;
    }
    
    public void setDeductibleAmount(BigDecimal deductibleAmount) {
        this.deductibleAmount = deductibleAmount;
    }
    
    public BigDecimal getIngredientCost() {
        return ingredientCost;
    }
    
    public void setIngredientCost(BigDecimal ingredientCost) {
        this.ingredientCost = ingredientCost;
    }
    
    public BigDecimal getDispensingFee() {
        return dispensingFee;
    }
    
    public void setDispensingFee(BigDecimal dispensingFee) {
        this.dispensingFee = dispensingFee;
    }
    
    public BigDecimal getSalesTax() {
        return salesTax;
    }
    
    public void setSalesTax(BigDecimal salesTax) {
        this.salesTax = salesTax;
    }
    
    public String getLineStatus() {
        return lineStatus;
    }
    
    public void setLineStatus(String lineStatus) {
        this.lineStatus = lineStatus;
    }
    
    public String getDenialCode() {
        return denialCode;
    }
    
    public void setDenialCode(String denialCode) {
        this.denialCode = denialCode;
    }
    
    public String getDenialReason() {
        return denialReason;
    }
    
    public void setDenialReason(String denialReason) {
        this.denialReason = denialReason;
    }
    
    public String getAdjustmentReason() {
        return adjustmentReason;
    }
    
    public void setAdjustmentReason(String adjustmentReason) {
        this.adjustmentReason = adjustmentReason;
    }
    
    public String getPriorAuthNumber() {
        return priorAuthNumber;
    }
    
    public void setPriorAuthNumber(String priorAuthNumber) {
        this.priorAuthNumber = priorAuthNumber;
    }
    
    public String getFormularyStatus() {
        return formularyStatus;
    }
    
    public void setFormularyStatus(String formularyStatus) {
        this.formularyStatus = formularyStatus;
    }
    
    public Integer getTierLevel() {
        return tierLevel;
    }
    
    public void setTierLevel(Integer tierLevel) {
        this.tierLevel = tierLevel;
    }
    
    public String getDawCode() {
        return dawCode;
    }
    
    public void setDawCode(String dawCode) {
        this.dawCode = dawCode;
    }
    
    public Boolean getGenericIndicator() {
        return genericIndicator;
    }
    
    public void setGenericIndicator(Boolean genericIndicator) {
        this.genericIndicator = genericIndicator;
    }
    
    public Boolean getBrandIndicator() {
        return brandIndicator;
    }
    
    public void setBrandIndicator(Boolean brandIndicator) {
        this.brandIndicator = brandIndicator;
    }
    
    public String getDiagnosisPointer() {
        return diagnosisPointer;
    }
    
    public void setDiagnosisPointer(String diagnosisPointer) {
        this.diagnosisPointer = diagnosisPointer;
    }
    
    public String getPrescriberNpi() {
        return prescriberNpi;
    }
    
    public void setPrescriberNpi(String prescriberNpi) {
        this.prescriberNpi = prescriberNpi;
    }
    
    public String getPrescriptionNumber() {
        return prescriptionNumber;
    }
    
    public void setPrescriptionNumber(String prescriptionNumber) {
        this.prescriptionNumber = prescriptionNumber;
    }
    
    public Integer getRefillNumber() {
        return refillNumber;
    }
    
    public void setRefillNumber(Integer refillNumber) {
        this.refillNumber = refillNumber;
    }
    
    public LocalDate getDateWritten() {
        return dateWritten;
    }
    
    public void setDateWritten(LocalDate dateWritten) {
        this.dateWritten = dateWritten;
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
    
    public String getProcessedBy() {
        return processedBy;
    }
    
    public void setProcessedBy(String processedBy) {
        this.processedBy = processedBy;
    }
    
    public Integer getProcessingTimeMs() {
        return processingTimeMs;
    }
    
    public void setProcessingTimeMs(Integer processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }
    
    // Utility methods
    
    /**
     * Check if line is approved
     */
    public boolean isApproved() {
        return "APPROVED".equalsIgnoreCase(lineStatus);
    }
    
    /**
     * Check if line is denied
     */
    public boolean isDenied() {
        return "DENIED".equalsIgnoreCase(lineStatus);
    }
    
    /**
     * Check if line is pending
     */
    public boolean isPending() {
        return "PENDING".equalsIgnoreCase(lineStatus);
    }
    
    /**
     * Check if line has been adjusted
     */
    public boolean isAdjusted() {
        return "ADJUSTED".equalsIgnoreCase(lineStatus);
    }
    
    /**
     * Get total line cost (billed amount)
     */
    public BigDecimal getTotalLineCost() {
        if (billedAmount != null) {
            return billedAmount;
        }
        // Calculate from components if billed amount not set
        BigDecimal total = BigDecimal.ZERO;
        if (ingredientCost != null) {
            total = total.add(ingredientCost);
        }
        if (dispensingFee != null) {
            total = total.add(dispensingFee);
        }
        return total;
    }
    
    /**
     * Validate financial reconciliation
     * allowed_amount should equal paid_amount + patient_responsibility
     */
    public boolean isFinanciallyReconciled() {
        if (allowedAmount == null || paidAmount == null || patientResponsibility == null) {
            return false;
        }
        BigDecimal calculated = paidAmount.add(patientResponsibility);
        return allowedAmount.compareTo(calculated) == 0;
    }
    
    /**
     * Check if line requires prior authorization
     */
    public boolean requiresPriorAuth() {
        return priorAuthNumber != null && !priorAuthNumber.isEmpty();
    }
    
    /**
     * Check if this is a generic drug
     */
    public boolean isGeneric() {
        return genericIndicator != null && genericIndicator;
    }
    
    /**
     * Check if this is a brand drug
     */
    public boolean isBrand() {
        return brandIndicator != null && brandIndicator;
    }
    
    /**
     * Check if this is a specialty tier medication
     */
    public boolean isSpecialtyTier() {
        return tierLevel != null && tierLevel >= 4;
    }
    
    /**
     * Get tier description
     */
    public String getTierDescription() {
        if (tierLevel == null) {
            return "UNKNOWN";
        }
        switch (tierLevel) {
            case 1: return "TIER 1 - GENERIC";
            case 2: return "TIER 2 - PREFERRED BRAND";
            case 3: return "TIER 3 - NON-PREFERRED BRAND";
            case 4: return "TIER 4 - SPECIALTY";
            case 5: return "TIER 5 - HIGH-COST SPECIALTY";
            default: return "TIER " + tierLevel;
        }
    }
    
    /**
     * Calculate patient cost share percentage
     */
    public BigDecimal getPatientCostSharePercentage() {
        if (allowedAmount == null || allowedAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (patientResponsibility == null) {
            return BigDecimal.ZERO;
        }
        return patientResponsibility.divide(allowedAmount, 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
    }
    
    /**
     * Check if DAW (Dispense As Written) was used
     */
    public boolean isDawUsed() {
        return dawCode != null && !"0".equals(dawCode);
    }
    
    /**
     * Get formatted NDC with hyphens
     */
    public String getFormattedNdc() {
        if (ndc == null) {
            return null;
        }
        if (ndc.contains("-")) {
            return ndc;
        }
        if (ndc.length() == 11) {
            return ndc.substring(0, 5) + "-" + 
                   ndc.substring(5, 9) + "-" + 
                   ndc.substring(9, 11);
        }
        return ndc;
    }
    
    /**
     * Check if line has complete required data
     */
    public boolean isComplete() {
        return claimId != null && 
               lineNumber != null && 
               serviceDate != null && 
               ndc != null && 
               quantityDispensed != null && 
               daysSupply != null &&
               billedAmount != null;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClaimLine claimLine = (ClaimLine) o;
        return Objects.equals(claimLineId, claimLine.claimLineId) &&
               Objects.equals(claimId, claimLine.claimId) &&
               Objects.equals(lineNumber, claimLine.lineNumber);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(claimLineId, claimId, lineNumber);
    }
    
    @Override
    public String toString() {
        return "ClaimLine{" +
                "claimLineId=" + claimLineId +
                ", claimId=" + claimId +
                ", claimNumber='" + claimNumber + '\'' +
                ", lineNumber=" + lineNumber +
                ", serviceDate=" + serviceDate +
                ", ndc='" + ndc + '\'' +
                ", drugName='" + drugName + '\'' +
                ", quantityDispensed=" + quantityDispensed +
                ", daysSupply=" + daysSupply +
                ", lineStatus='" + lineStatus + '\'' +
                ", tierLevel=" + tierLevel +
                ", billedAmount=" + billedAmount +
                ", allowedAmount=" + allowedAmount +
                ", paidAmount=" + paidAmount +
                ", patientResponsibility=" + patientResponsibility +
                '}';
    }
}

// Made with Bob
