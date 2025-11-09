package dejanlazic.playground.inmemory.rdbms.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * POJO representing a pharmacy network assignment
 * Maps to the pharmacy_network table in the database
 * Links pharmacies to pharmacy benefit networks (PBMs, insurance networks, etc.)
 */
public class PharmacyNetwork {
    
    // Enum for network types matching US healthcare pharmacy networks
    public enum NetworkType {
        PBM,              // Pharmacy Benefit Manager
        RETAIL,           // Retail pharmacy chain network
        SPECIALTY,        // Specialty pharmacy network
        MAIL_ORDER,       // Mail-order pharmacy network
        REGIONAL,         // Regional network
        INDEPENDENT       // Independent pharmacy network
    }
    
    // Enum for network tiers
    public enum NetworkTier {
        PREFERRED,        // Preferred tier (lower copays)
        STANDARD          // Standard tier (higher copays)
    }
    
    // Enum for contract types
    public enum ContractType {
        DIRECT,           // Direct contract with PBM/payer
        INDIRECT,         // Indirect contract through intermediary
        PSAO,             // Pharmacy Services Administrative Organization
        AGGREGATOR        // Third-party aggregator
    }
    
    // Enum for network status
    public enum NetworkStatus {
        ACTIVE,           // Currently active
        INACTIVE,         // Terminated/inactive
        PENDING           // Pending activation
    }
    
    private UUID networkId;
    private UUID pharmacyId;
    private String ncpdpId;  // Business key for pharmacy lookup
    private String networkName;
    private NetworkType networkType;
    private NetworkTier networkTier;
    private ContractType contractType;
    private LocalDate effectiveDate;
    private LocalDate terminationDate;
    private NetworkStatus status;
    private String reimbursementRate;
    private BigDecimal dispensingFee;
    private boolean isPreferred;
    private boolean isMailOrder;
    private boolean isSpecialty;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public PharmacyNetwork() {
    }
    
    public PharmacyNetwork(UUID pharmacyId, String networkName, NetworkType networkType) {
        this.pharmacyId = pharmacyId;
        this.networkName = networkName;
        this.networkType = networkType;
        this.networkTier = NetworkTier.STANDARD;
        this.contractType = ContractType.DIRECT;
        this.status = NetworkStatus.ACTIVE;
        this.isPreferred = false;
        this.isMailOrder = false;
        this.isSpecialty = false;
    }
    
    // Getters and Setters
    public UUID getNetworkId() {
        return networkId;
    }
    
    public void setNetworkId(UUID networkId) {
        this.networkId = networkId;
    }
    
    public UUID getPharmacyId() {
        return pharmacyId;
    }
    
    public void setPharmacyId(UUID pharmacyId) {
        this.pharmacyId = pharmacyId;
    }
    
    public String getNcpdpId() {
        return ncpdpId;
    }
    
    public void setNcpdpId(String ncpdpId) {
        this.ncpdpId = ncpdpId;
    }
    
    public String getNetworkName() {
        return networkName;
    }
    
    public void setNetworkName(String networkName) {
        this.networkName = networkName;
    }
    
    public NetworkType getNetworkType() {
        return networkType;
    }
    
    public void setNetworkType(NetworkType networkType) {
        this.networkType = networkType;
    }
    
    public NetworkTier getNetworkTier() {
        return networkTier;
    }
    
    public void setNetworkTier(NetworkTier networkTier) {
        this.networkTier = networkTier;
    }
    
    public ContractType getContractType() {
        return contractType;
    }
    
    public void setContractType(ContractType contractType) {
        this.contractType = contractType;
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
    
    public NetworkStatus getStatus() {
        return status;
    }
    
    public void setStatus(NetworkStatus status) {
        this.status = status;
    }
    
    public String getReimbursementRate() {
        return reimbursementRate;
    }
    
    public void setReimbursementRate(String reimbursementRate) {
        this.reimbursementRate = reimbursementRate;
    }
    
    public BigDecimal getDispensingFee() {
        return dispensingFee;
    }
    
    public void setDispensingFee(BigDecimal dispensingFee) {
        this.dispensingFee = dispensingFee;
    }
    
    public boolean isPreferred() {
        return isPreferred;
    }
    
    public void setPreferred(boolean preferred) {
        isPreferred = preferred;
    }
    
    public boolean isMailOrder() {
        return isMailOrder;
    }
    
    public void setMailOrder(boolean mailOrder) {
        isMailOrder = mailOrder;
    }
    
    public boolean isSpecialty() {
        return isSpecialty;
    }
    
    public void setSpecialty(boolean specialty) {
        isSpecialty = specialty;
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
     * Check if network assignment is currently active
     */
    public boolean isCurrentlyActive() {
        if (status != NetworkStatus.ACTIVE) {
            return false;
        }
        
        LocalDate today = LocalDate.now();
        
        if (effectiveDate != null && today.isBefore(effectiveDate)) {
            return false;
        }
        
        if (terminationDate != null && today.isAfter(terminationDate)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if network assignment has been terminated
     */
    public boolean isTerminated() {
        return status == NetworkStatus.INACTIVE || 
               (terminationDate != null && LocalDate.now().isAfter(terminationDate));
    }
    
    /**
     * Check if network assignment is pending activation
     */
    public boolean isPending() {
        return status == NetworkStatus.PENDING ||
               (effectiveDate != null && LocalDate.now().isBefore(effectiveDate));
    }
    
    /**
     * Get network type as display string
     */
    public String getNetworkTypeDisplay() {
        if (networkType == null) return "UNKNOWN";
        
        switch (networkType) {
            case PBM:
                return "Pharmacy Benefit Manager";
            case RETAIL:
                return "Retail Network";
            case SPECIALTY:
                return "Specialty Network";
            case MAIL_ORDER:
                return "Mail-Order Network";
            case REGIONAL:
                return "Regional Network";
            case INDEPENDENT:
                return "Independent Network";
            default:
                return networkType.toString();
        }
    }
    
    /**
     * Get network tier as display string
     */
    public String getNetworkTierDisplay() {
        if (networkTier == null) return "UNKNOWN";
        
        switch (networkTier) {
            case PREFERRED:
                return "Preferred Tier";
            case STANDARD:
                return "Standard Tier";
            default:
                return networkTier.toString();
        }
    }
    
    /**
     * Get contract type as display string
     */
    public String getContractTypeDisplay() {
        if (contractType == null) return "UNKNOWN";
        
        switch (contractType) {
            case DIRECT:
                return "Direct Contract";
            case INDIRECT:
                return "Indirect Contract";
            case PSAO:
                return "PSAO Contract";
            case AGGREGATOR:
                return "Aggregator Contract";
            default:
                return contractType.toString();
        }
    }
    
    /**
     * Get status as display string
     */
    public String getStatusDisplay() {
        if (status == null) return "UNKNOWN";
        return status.toString();
    }
    
    /**
     * Get full network description
     */
    public String getFullDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(networkName);
        sb.append(" (").append(getNetworkTypeDisplay()).append(")");
        sb.append(" - ").append(getNetworkTierDisplay());
        return sb.toString();
    }
    
    /**
     * Check if this is a PBM network
     */
    public boolean isPBMNetwork() {
        return networkType == NetworkType.PBM;
    }
    
    /**
     * Check if this is a retail network
     */
    public boolean isRetailNetwork() {
        return networkType == NetworkType.RETAIL;
    }
    
    /**
     * Check if this is a specialty network
     */
    public boolean isSpecialtyNetwork() {
        return networkType == NetworkType.SPECIALTY;
    }
    
    /**
     * Check if this is a mail-order network
     */
    public boolean isMailOrderNetwork() {
        return networkType == NetworkType.MAIL_ORDER;
    }
    
    /**
     * Check if this is a regional network
     */
    public boolean isRegionalNetwork() {
        return networkType == NetworkType.REGIONAL;
    }
    
    /**
     * Check if this is an independent network
     */
    public boolean isIndependentNetwork() {
        return networkType == NetworkType.INDEPENDENT;
    }
    
    /**
     * Check if this is a preferred tier network
     */
    public boolean isPreferredTier() {
        return networkTier == NetworkTier.PREFERRED;
    }
    
    /**
     * Check if this is a standard tier network
     */
    public boolean isStandardTier() {
        return networkTier == NetworkTier.STANDARD;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PharmacyNetwork that = (PharmacyNetwork) o;
        return Objects.equals(networkId, that.networkId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(networkId);
    }
    
    @Override
    public String toString() {
        return "PharmacyNetwork{" +
                "networkId=" + networkId +
                ", pharmacyId=" + pharmacyId +
                ", networkName='" + networkName + '\'' +
                ", networkType=" + networkType +
                ", networkTier=" + networkTier +
                ", contractType=" + contractType +
                ", status=" + status +
                ", reimbursementRate='" + reimbursementRate + '\'' +
                ", dispensingFee=" + dispensingFee +
                ", isPreferred=" + isPreferred +
                ", effectiveDate=" + effectiveDate +
                ", terminationDate=" + terminationDate +
                '}';
    }
}

// Made with Bob
