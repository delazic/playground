package dejanlazic.playground.inmemory.rdbms.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * POJO representing a pharmacy
 * Maps to the pharmacy table in the database
 */
public class Pharmacy {
    
    // Enum for pharmacy types matching database enum
    public enum PharmacyType {
        RETAIL,
        MAIL_ORDER,
        SPECIALTY,
        LONG_TERM_CARE
    }
    
    private UUID pharmacyId;
    private String ncpdpId;
    private String pharmacyName;
    private String npi;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String phone;
    private PharmacyType pharmacyType;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public Pharmacy() {
    }
    
    public Pharmacy(String ncpdpId, String pharmacyName, String npi) {
        this.ncpdpId = ncpdpId;
        this.pharmacyName = pharmacyName;
        this.npi = npi;
        this.pharmacyType = PharmacyType.RETAIL;
        this.isActive = true;
    }
    
    // Getters and Setters
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
    
    public String getPharmacyName() {
        return pharmacyName;
    }
    
    public void setPharmacyName(String pharmacyName) {
        this.pharmacyName = pharmacyName;
    }
    
    public String getNpi() {
        return npi;
    }
    
    public void setNpi(String npi) {
        this.npi = npi;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    
    public String getZipCode() {
        return zipCode;
    }
    
    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public PharmacyType getPharmacyType() {
        return pharmacyType;
    }
    
    public void setPharmacyType(PharmacyType pharmacyType) {
        this.pharmacyType = pharmacyType;
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
     * Get full address as a single string
     */
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (address != null && !address.isEmpty()) {
            sb.append(address);
        }
        if (city != null && !city.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(city);
        }
        if (state != null && !state.isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(state);
        }
        if (zipCode != null && !zipCode.isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(zipCode);
        }
        return sb.toString();
    }
    
    /**
     * Check if pharmacy is a retail location
     */
    public boolean isRetail() {
        return pharmacyType == PharmacyType.RETAIL;
    }
    
    /**
     * Check if pharmacy is mail order
     */
    public boolean isMailOrder() {
        return pharmacyType == PharmacyType.MAIL_ORDER;
    }
    
    /**
     * Check if pharmacy is specialty
     */
    public boolean isSpecialty() {
        return pharmacyType == PharmacyType.SPECIALTY;
    }
    
    /**
     * Check if pharmacy is long-term care
     */
    public boolean isLongTermCare() {
        return pharmacyType == PharmacyType.LONG_TERM_CARE;
    }
    
    /**
     * Get pharmacy type as display string
     */
    public String getPharmacyTypeDisplay() {
        if (pharmacyType == null) return "UNKNOWN";
        
        switch (pharmacyType) {
            case RETAIL:
                return "Retail Pharmacy";
            case MAIL_ORDER:
                return "Mail Order Pharmacy";
            case SPECIALTY:
                return "Specialty Pharmacy";
            case LONG_TERM_CARE:
                return "Long-Term Care Pharmacy";
            default:
                return pharmacyType.toString();
        }
    }
    
    /**
     * Check if pharmacy has complete contact information
     */
    public boolean hasCompleteContactInfo() {
        return address != null && !address.isEmpty() &&
               city != null && !city.isEmpty() &&
               state != null && !state.isEmpty() &&
               zipCode != null && !zipCode.isEmpty() &&
               phone != null && !phone.isEmpty();
    }
    
    /**
     * Get pharmacy status as string
     */
    public String getStatus() {
        return isActive ? "ACTIVE" : "INACTIVE";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pharmacy pharmacy = (Pharmacy) o;
        return Objects.equals(pharmacyId, pharmacy.pharmacyId) ||
               Objects.equals(ncpdpId, pharmacy.ncpdpId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(pharmacyId, ncpdpId);
    }
    
    @Override
    public String toString() {
        return "Pharmacy{" +
                "pharmacyId=" + pharmacyId +
                ", ncpdpId='" + ncpdpId + '\'' +
                ", pharmacyName='" + pharmacyName + '\'' +
                ", npi='" + npi + '\'' +
                ", city='" + city + '\'' +
                ", state='" + state + '\'' +
                ", pharmacyType=" + pharmacyType +
                ", isActive=" + isActive +
                '}';
    }
}

// Made with Bob
