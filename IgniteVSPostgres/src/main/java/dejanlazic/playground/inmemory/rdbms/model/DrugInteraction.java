package dejanlazic.playground.inmemory.rdbms.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * POJO representing a drug interaction
 * Maps to the drug_interaction table in the database
 */
public class DrugInteraction {
    
    private Long interactionId;
    private String interactionCode;
    private String drug1Name;
    private String drug1Ndc;
    private String drug2Name;
    private String drug2Ndc;
    private String severityLevel;
    private String interactionMechanism;
    private String clinicalEffects;
    private String managementRecommendation;
    private String evidenceLevel;
    private String onsetTiming;
    private String documentationSource;
    private boolean requiresAlert;
    private boolean requiresIntervention;
    private boolean patientCounselingRequired;
    private boolean prescriberNotificationRequired;
    private LocalDate lastReviewedDate;
    private LocalDate lastUpdatedDate;
    private String activeStatus;
    private String referenceId;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public DrugInteraction() {
    }
    
    public DrugInteraction(String interactionCode, String drug1Name, String drug1Ndc, 
                          String drug2Name, String drug2Ndc, String severityLevel) {
        this.interactionCode = interactionCode;
        this.drug1Name = drug1Name;
        this.drug1Ndc = drug1Ndc;
        this.drug2Name = drug2Name;
        this.drug2Ndc = drug2Ndc;
        this.severityLevel = severityLevel;
    }
    
    // Getters and Setters
    public Long getInteractionId() {
        return interactionId;
    }
    
    public void setInteractionId(Long interactionId) {
        this.interactionId = interactionId;
    }
    
    public String getInteractionCode() {
        return interactionCode;
    }
    
    public void setInteractionCode(String interactionCode) {
        this.interactionCode = interactionCode;
    }
    
    public String getDrug1Name() {
        return drug1Name;
    }
    
    public void setDrug1Name(String drug1Name) {
        this.drug1Name = drug1Name;
    }
    
    public String getDrug1Ndc() {
        return drug1Ndc;
    }
    
    public void setDrug1Ndc(String drug1Ndc) {
        this.drug1Ndc = drug1Ndc;
    }
    
    public String getDrug2Name() {
        return drug2Name;
    }
    
    public void setDrug2Name(String drug2Name) {
        this.drug2Name = drug2Name;
    }
    
    public String getDrug2Ndc() {
        return drug2Ndc;
    }
    
    public void setDrug2Ndc(String drug2Ndc) {
        this.drug2Ndc = drug2Ndc;
    }
    
    public String getSeverityLevel() {
        return severityLevel;
    }
    
    public void setSeverityLevel(String severityLevel) {
        this.severityLevel = severityLevel;
    }
    
    public String getInteractionMechanism() {
        return interactionMechanism;
    }
    
    public void setInteractionMechanism(String interactionMechanism) {
        this.interactionMechanism = interactionMechanism;
    }
    
    public String getClinicalEffects() {
        return clinicalEffects;
    }
    
    public void setClinicalEffects(String clinicalEffects) {
        this.clinicalEffects = clinicalEffects;
    }
    
    public String getManagementRecommendation() {
        return managementRecommendation;
    }
    
    public void setManagementRecommendation(String managementRecommendation) {
        this.managementRecommendation = managementRecommendation;
    }
    
    public String getEvidenceLevel() {
        return evidenceLevel;
    }
    
    public void setEvidenceLevel(String evidenceLevel) {
        this.evidenceLevel = evidenceLevel;
    }
    
    public String getOnsetTiming() {
        return onsetTiming;
    }
    
    public void setOnsetTiming(String onsetTiming) {
        this.onsetTiming = onsetTiming;
    }
    
    public String getDocumentationSource() {
        return documentationSource;
    }
    
    public void setDocumentationSource(String documentationSource) {
        this.documentationSource = documentationSource;
    }
    
    public boolean isRequiresAlert() {
        return requiresAlert;
    }
    
    public void setRequiresAlert(boolean requiresAlert) {
        this.requiresAlert = requiresAlert;
    }
    
    public boolean isRequiresIntervention() {
        return requiresIntervention;
    }
    
    public void setRequiresIntervention(boolean requiresIntervention) {
        this.requiresIntervention = requiresIntervention;
    }
    
    public boolean isPatientCounselingRequired() {
        return patientCounselingRequired;
    }
    
    public void setPatientCounselingRequired(boolean patientCounselingRequired) {
        this.patientCounselingRequired = patientCounselingRequired;
    }
    
    public boolean isPrescriberNotificationRequired() {
        return prescriberNotificationRequired;
    }
    
    public void setPrescriberNotificationRequired(boolean prescriberNotificationRequired) {
        this.prescriberNotificationRequired = prescriberNotificationRequired;
    }
    
    public LocalDate getLastReviewedDate() {
        return lastReviewedDate;
    }
    
    public void setLastReviewedDate(LocalDate lastReviewedDate) {
        this.lastReviewedDate = lastReviewedDate;
    }
    
    public LocalDate getLastUpdatedDate() {
        return lastUpdatedDate;
    }
    
    public void setLastUpdatedDate(LocalDate lastUpdatedDate) {
        this.lastUpdatedDate = lastUpdatedDate;
    }
    
    public String getActiveStatus() {
        return activeStatus;
    }
    
    public void setActiveStatus(String activeStatus) {
        this.activeStatus = activeStatus;
    }
    
    public String getReferenceId() {
        return referenceId;
    }
    
    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
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
     * Get full interaction description
     */
    public String getFullDescription() {
        return String.format("%s + %s: %s (%s severity)",
            drug1Name, drug2Name, clinicalEffects, severityLevel);
    }
    
    /**
     * Check if this is a severe interaction
     */
    public boolean isSevere() {
        return "Severe".equalsIgnoreCase(severityLevel) || 
               "Major".equalsIgnoreCase(severityLevel);
    }
    
    /**
     * Check if this is a moderate interaction
     */
    public boolean isModerate() {
        return "Moderate".equalsIgnoreCase(severityLevel);
    }
    
    /**
     * Check if this is a minor interaction
     */
    public boolean isMinor() {
        return "Minor".equalsIgnoreCase(severityLevel);
    }
    
    /**
     * Check if any action is required
     */
    public boolean requiresAction() {
        return requiresAlert || requiresIntervention || 
               patientCounselingRequired || prescriberNotificationRequired;
    }
    
    /**
     * Get interaction summary for display
     */
    public String getInteractionSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Interaction: ").append(drug1Name).append(" + ").append(drug2Name).append("\n");
        sb.append("Severity: ").append(severityLevel).append("\n");
        sb.append("Mechanism: ").append(interactionMechanism).append("\n");
        sb.append("Clinical Effects: ").append(clinicalEffects).append("\n");
        sb.append("Management: ").append(managementRecommendation);
        return sb.toString();
    }
    
    /**
     * Check if interaction involves a specific drug by NDC
     */
    public boolean involvesDrug(String ndcCode) {
        return (drug1Ndc != null && drug1Ndc.equals(ndcCode)) ||
               (drug2Ndc != null && drug2Ndc.equals(ndcCode));
    }
    
    /**
     * Check if interaction involves both drugs by NDC
     */
    public boolean involvesBothDrugs(String ndc1, String ndc2) {
        return (involvesDrug(ndc1) && involvesDrug(ndc2));
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DrugInteraction that = (DrugInteraction) o;
        return Objects.equals(interactionCode, that.interactionCode);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(interactionCode);
    }
    
    @Override
    public String toString() {
        return "DrugInteraction{" +
                "interactionId=" + interactionId +
                ", interactionCode='" + interactionCode + '\'' +
                ", drug1Name='" + drug1Name + '\'' +
                ", drug1Ndc='" + drug1Ndc + '\'' +
                ", drug2Name='" + drug2Name + '\'' +
                ", drug2Ndc='" + drug2Ndc + '\'' +
                ", severityLevel='" + severityLevel + '\'' +
                ", requiresAlert=" + requiresAlert +
                ", requiresIntervention=" + requiresIntervention +
                '}';
    }
}


