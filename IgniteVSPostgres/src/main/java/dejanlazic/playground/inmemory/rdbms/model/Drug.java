package dejanlazic.playground.inmemory.rdbms.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * POJO representing a drug product
 * Maps to the drug table in the database
 */
public class Drug {
    
    private UUID drugId;
    private String ndcCode;
    private String drugName;
    private String genericName;
    private String strength;
    private String dosageForm;
    private String manufacturer;
    private String drugClass;
    private boolean isGeneric;
    private boolean isBrand;
    private BigDecimal awpPrice;
    private BigDecimal macPrice;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public Drug() {
    }
    
    public Drug(String ndcCode, String drugName, String genericName) {
        this.ndcCode = ndcCode;
        this.drugName = drugName;
        this.genericName = genericName;
    }
    
    public Drug(String ndcCode, String drugName, String genericName, String strength, 
                String dosageForm, boolean isGeneric, boolean isBrand) {
        this.ndcCode = ndcCode;
        this.drugName = drugName;
        this.genericName = genericName;
        this.strength = strength;
        this.dosageForm = dosageForm;
        this.isGeneric = isGeneric;
        this.isBrand = isBrand;
    }
    
    // Getters and Setters
    public UUID getDrugId() {
        return drugId;
    }
    
    public void setDrugId(UUID drugId) {
        this.drugId = drugId;
    }
    
    public String getNdcCode() {
        return ndcCode;
    }
    
    public void setNdcCode(String ndcCode) {
        this.ndcCode = ndcCode;
    }
    
    public String getDrugName() {
        return drugName;
    }
    
    public void setDrugName(String drugName) {
        this.drugName = drugName;
    }
    
    public String getGenericName() {
        return genericName;
    }
    
    public void setGenericName(String genericName) {
        this.genericName = genericName;
    }
    
    public String getStrength() {
        return strength;
    }
    
    public void setStrength(String strength) {
        this.strength = strength;
    }
    
    public String getDosageForm() {
        return dosageForm;
    }
    
    public void setDosageForm(String dosageForm) {
        this.dosageForm = dosageForm;
    }
    
    public String getManufacturer() {
        return manufacturer;
    }
    
    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }
    
    public String getDrugClass() {
        return drugClass;
    }
    
    public void setDrugClass(String drugClass) {
        this.drugClass = drugClass;
    }
    
    public boolean isGeneric() {
        return isGeneric;
    }
    
    public void setGeneric(boolean generic) {
        isGeneric = generic;
    }
    
    public boolean isBrand() {
        return isBrand;
    }
    
    public void setBrand(boolean brand) {
        isBrand = brand;
    }
    
    public BigDecimal getAwpPrice() {
        return awpPrice;
    }
    
    public void setAwpPrice(BigDecimal awpPrice) {
        this.awpPrice = awpPrice;
    }
    
    public BigDecimal getMacPrice() {
        return macPrice;
    }
    
    public void setMacPrice(BigDecimal macPrice) {
        this.macPrice = macPrice;
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
     * Get full drug description
     */
    public String getFullDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(drugName);
        if (genericName != null && !genericName.equals(drugName)) {
            sb.append(" (").append(genericName).append(")");
        }
        if (strength != null) {
            sb.append(" ").append(strength);
        }
        if (dosageForm != null) {
            sb.append(" ").append(dosageForm);
        }
        return sb.toString();
    }
    
    /**
     * Check if drug is a specialty medication
     * Typically defined as AWP > $1000
     */
    public boolean isSpecialty() {
        return awpPrice != null && awpPrice.compareTo(new BigDecimal("1000.00")) > 0;
    }
    
    /**
     * Get drug type as string
     */
    public String getDrugType() {
        if (isSpecialty()) {
            return "SPECIALTY";
        } else if (isBrand) {
            return "BRAND";
        } else if (isGeneric) {
            return "GENERIC";
        }
        return "UNKNOWN";
    }
    
    /**
     * Calculate MAC to AWP ratio (for generics)
     */
    public BigDecimal getMacToAwpRatio() {
        if (macPrice == null || awpPrice == null || awpPrice.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return macPrice.divide(awpPrice, 4, BigDecimal.ROUND_HALF_UP);
    }
    
    /**
     * Check if drug has complete pricing information
     */
    public boolean hasCompletePricing() {
        if (awpPrice == null) {
            return false;
        }
        // MAC price is only required for generics
        if (isGeneric && macPrice == null) {
            return false;
        }
        return true;
    }
    
    /**
     * Format NDC code with hyphens if not already formatted
     */
    public String getFormattedNdcCode() {
        if (ndcCode == null) {
            return null;
        }
        // If already formatted (contains hyphens), return as is
        if (ndcCode.contains("-")) {
            return ndcCode;
        }
        // Format as XXXXX-XXXX-XX
        if (ndcCode.length() == 11) {
            return ndcCode.substring(0, 5) + "-" + 
                   ndcCode.substring(5, 9) + "-" + 
                   ndcCode.substring(9, 11);
        }
        return ndcCode;
    }
    
    /**
     * Check if this is an oral medication
     */
    public boolean isOralMedication() {
        if (dosageForm == null) {
            return false;
        }
        String form = dosageForm.toUpperCase();
        return form.contains("TABLET") || 
               form.contains("CAPSULE") || 
               form.contains("SOLUTION") ||
               form.contains("SUSPENSION") ||
               form.contains("SYRUP");
    }
    
    /**
     * Check if this is an injectable medication
     */
    public boolean isInjectableMedication() {
        if (dosageForm == null) {
            return false;
        }
        String form = dosageForm.toUpperCase();
        return form.contains("INJECTION") || 
               form.contains("VIAL") ||
               form.contains("SYRINGE");
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Drug drug = (Drug) o;
        return Objects.equals(ndcCode, drug.ndcCode);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(ndcCode);
    }
    
    @Override
    public String toString() {
        return "Drug{" +
                "drugId=" + drugId +
                ", ndcCode='" + ndcCode + '\'' +
                ", drugName='" + drugName + '\'' +
                ", genericName='" + genericName + '\'' +
                ", strength='" + strength + '\'' +
                ", dosageForm='" + dosageForm + '\'' +
                ", manufacturer='" + manufacturer + '\'' +
                ", drugClass='" + drugClass + '\'' +
                ", isGeneric=" + isGeneric +
                ", isBrand=" + isBrand +
                ", awpPrice=" + awpPrice +
                ", macPrice=" + macPrice +
                '}';
    }
}
