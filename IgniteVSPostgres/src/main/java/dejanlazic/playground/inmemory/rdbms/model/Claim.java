package dejanlazic.playground.inmemory.rdbms.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a pharmacy claim in the PBM system.
 * Based on NCPDP Telecommunication Standard.
 */
public class Claim {
    private Long claimId;
    private String claimNumber;
    private String transactionType; // B1, B2, B3
    
    // Timestamps
    private LocalDateTime receivedTimestamp;
    private LocalDateTime processedTimestamp;
    private LocalDate dateOfService;
    
    // Member Information
    private Long memberId;
    private String personCode;
    
    // Pharmacy Information
    private Long pharmacyId;
    private String pharmacyNpi;
    
    // Prescription Information
    private String prescriptionNumber;
    private String ndc;
    private Long drugId;
    private BigDecimal quantityDispensed;
    private Integer daysSupply;
    private Integer refillNumber;
    private String dawCode;
    
    // Prescriber Information
    private String prescriberNpi;
    private String prescriberId;
    
    // Pricing
    private BigDecimal ingredientCostSubmitted;
    private BigDecimal dispensingFeeSubmitted;
    private BigDecimal patientPayAmount;
    private BigDecimal planPayAmount;
    private BigDecimal taxAmount;
    
    // Adjudication Result
    private String status; // APPROVED, REJECTED, REVERSED
    private String responseCode;
    private String responseMessage;
    
    // Processing Metrics
    private Integer processingTimeMs;
    
    // Accumulators
    private BigDecimal deductibleApplied;
    private BigDecimal oopApplied;
    
    // Constructors
    public Claim() {
        this.receivedTimestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getClaimId() { return claimId; }
    public void setClaimId(Long claimId) { this.claimId = claimId; }
    
    public String getClaimNumber() { return claimNumber; }
    public void setClaimNumber(String claimNumber) { this.claimNumber = claimNumber; }
    
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
    
    public LocalDateTime getReceivedTimestamp() { return receivedTimestamp; }
    public void setReceivedTimestamp(LocalDateTime receivedTimestamp) { this.receivedTimestamp = receivedTimestamp; }
    
    public LocalDateTime getProcessedTimestamp() { return processedTimestamp; }
    public void setProcessedTimestamp(LocalDateTime processedTimestamp) { this.processedTimestamp = processedTimestamp; }
    
    public LocalDate getDateOfService() { return dateOfService; }
    public void setDateOfService(LocalDate dateOfService) { this.dateOfService = dateOfService; }
    
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    
    public String getPersonCode() { return personCode; }
    public void setPersonCode(String personCode) { this.personCode = personCode; }
    
    public Long getPharmacyId() { return pharmacyId; }
    public void setPharmacyId(Long pharmacyId) { this.pharmacyId = pharmacyId; }
    
    public String getPharmacyNpi() { return pharmacyNpi; }
    public void setPharmacyNpi(String pharmacyNpi) { this.pharmacyNpi = pharmacyNpi; }
    
    public String getPrescriptionNumber() { return prescriptionNumber; }
    public void setPrescriptionNumber(String prescriptionNumber) { this.prescriptionNumber = prescriptionNumber; }
    
    public String getNdc() { return ndc; }
    public void setNdc(String ndc) { this.ndc = ndc; }
    
    public Long getDrugId() { return drugId; }
    public void setDrugId(Long drugId) { this.drugId = drugId; }
    
    public BigDecimal getQuantityDispensed() { return quantityDispensed; }
    public void setQuantityDispensed(BigDecimal quantityDispensed) { this.quantityDispensed = quantityDispensed; }
    
    public Integer getDaysSupply() { return daysSupply; }
    public void setDaysSupply(Integer daysSupply) { this.daysSupply = daysSupply; }
    
    public Integer getRefillNumber() { return refillNumber; }
    public void setRefillNumber(Integer refillNumber) { this.refillNumber = refillNumber; }
    
    public String getDawCode() { return dawCode; }
    public void setDawCode(String dawCode) { this.dawCode = dawCode; }
    
    public String getPrescriberNpi() { return prescriberNpi; }
    public void setPrescriberNpi(String prescriberNpi) { this.prescriberNpi = prescriberNpi; }
    
    public String getPrescriberId() { return prescriberId; }
    public void setPrescriberId(String prescriberId) { this.prescriberId = prescriberId; }
    
    public BigDecimal getIngredientCostSubmitted() { return ingredientCostSubmitted; }
    public void setIngredientCostSubmitted(BigDecimal ingredientCostSubmitted) { this.ingredientCostSubmitted = ingredientCostSubmitted; }
    
    public BigDecimal getDispensingFeeSubmitted() { return dispensingFeeSubmitted; }
    public void setDispensingFeeSubmitted(BigDecimal dispensingFeeSubmitted) { this.dispensingFeeSubmitted = dispensingFeeSubmitted; }
    
    public BigDecimal getPatientPayAmount() { return patientPayAmount; }
    public void setPatientPayAmount(BigDecimal patientPayAmount) { this.patientPayAmount = patientPayAmount; }
    
    public BigDecimal getPlanPayAmount() { return planPayAmount; }
    public void setPlanPayAmount(BigDecimal planPayAmount) { this.planPayAmount = planPayAmount; }
    
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getResponseCode() { return responseCode; }
    public void setResponseCode(String responseCode) { this.responseCode = responseCode; }
    
    public String getResponseMessage() { return responseMessage; }
    public void setResponseMessage(String responseMessage) { this.responseMessage = responseMessage; }
    
    public Integer getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(Integer processingTimeMs) { this.processingTimeMs = processingTimeMs; }
    
    public BigDecimal getDeductibleApplied() { return deductibleApplied; }
    public void setDeductibleApplied(BigDecimal deductibleApplied) { this.deductibleApplied = deductibleApplied; }
    
    public BigDecimal getOopApplied() { return oopApplied; }
    public void setOopApplied(BigDecimal oopApplied) { this.oopApplied = oopApplied; }
    
    @Override
    public String toString() {
        return "Claim{" +
                "claimId=" + claimId +
                ", claimNumber='" + claimNumber + '\'' +
                ", status='" + status + '\'' +
                ", memberId=" + memberId +
                ", pharmacyId=" + pharmacyId +
                ", ndc='" + ndc + '\'' +
                ", dateOfService=" + dateOfService +
                ", planPayAmount=" + planPayAmount +
                ", patientPayAmount=" + patientPayAmount +
                '}';
    }
}


