package dejanlazic.playground.inmemory.rdbms.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * POJO representing a pharmacy benefit plan
 * Maps to data from us_pharmacy_plans.csv
 */
public class BenefitPlan {
    
    private String planCode;
    private String planName;
    private String planType;
    private String planCategory;
    private LocalDate effectiveDate;
    private BigDecimal annualDeductible;
    private BigDecimal outOfPocketMax;
    
    // Tier copays
    private BigDecimal tier1Copay;
    private BigDecimal tier2Copay;
    private BigDecimal tier3Copay;
    private BigDecimal tier4Copay;
    private BigDecimal tier5Copay;
    
    // Tier coinsurance (as decimal, e.g., 0.20 = 20%)
    private BigDecimal tier1Coinsurance;
    private BigDecimal tier2Coinsurance;
    private BigDecimal tier3Coinsurance;
    private BigDecimal tier4Coinsurance;
    private BigDecimal tier5Coinsurance;
    
    private boolean mailOrderAvailable;
    private boolean specialtyPharmacyRequired;
    private String description;
    
    // Constructors
    public BenefitPlan() {
    }
    
    public BenefitPlan(String planCode, String planName, String planType, String planCategory) {
        this.planCode = planCode;
        this.planName = planName;
        this.planType = planType;
        this.planCategory = planCategory;
    }
    
    // Getters and Setters
    public String getPlanCode() {
        return planCode;
    }
    
    public void setPlanCode(String planCode) {
        this.planCode = planCode;
    }
    
    public String getPlanName() {
        return planName;
    }
    
    public void setPlanName(String planName) {
        this.planName = planName;
    }
    
    public String getPlanType() {
        return planType;
    }
    
    public void setPlanType(String planType) {
        this.planType = planType;
    }
    
    public String getPlanCategory() {
        return planCategory;
    }
    
    public void setPlanCategory(String planCategory) {
        this.planCategory = planCategory;
    }
    
    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }
    
    public void setEffectiveDate(LocalDate effectiveDate) {
        this.effectiveDate = effectiveDate;
    }
    
    public BigDecimal getAnnualDeductible() {
        return annualDeductible;
    }
    
    public void setAnnualDeductible(BigDecimal annualDeductible) {
        this.annualDeductible = annualDeductible;
    }
    
    public BigDecimal getOutOfPocketMax() {
        return outOfPocketMax;
    }
    
    public void setOutOfPocketMax(BigDecimal outOfPocketMax) {
        this.outOfPocketMax = outOfPocketMax;
    }
    
    public BigDecimal getTier1Copay() {
        return tier1Copay;
    }
    
    public void setTier1Copay(BigDecimal tier1Copay) {
        this.tier1Copay = tier1Copay;
    }
    
    public BigDecimal getTier2Copay() {
        return tier2Copay;
    }
    
    public void setTier2Copay(BigDecimal tier2Copay) {
        this.tier2Copay = tier2Copay;
    }
    
    public BigDecimal getTier3Copay() {
        return tier3Copay;
    }
    
    public void setTier3Copay(BigDecimal tier3Copay) {
        this.tier3Copay = tier3Copay;
    }
    
    public BigDecimal getTier4Copay() {
        return tier4Copay;
    }
    
    public void setTier4Copay(BigDecimal tier4Copay) {
        this.tier4Copay = tier4Copay;
    }
    
    public BigDecimal getTier5Copay() {
        return tier5Copay;
    }
    
    public void setTier5Copay(BigDecimal tier5Copay) {
        this.tier5Copay = tier5Copay;
    }
    
    public BigDecimal getTier1Coinsurance() {
        return tier1Coinsurance;
    }
    
    public void setTier1Coinsurance(BigDecimal tier1Coinsurance) {
        this.tier1Coinsurance = tier1Coinsurance;
    }
    
    public BigDecimal getTier2Coinsurance() {
        return tier2Coinsurance;
    }
    
    public void setTier2Coinsurance(BigDecimal tier2Coinsurance) {
        this.tier2Coinsurance = tier2Coinsurance;
    }
    
    public BigDecimal getTier3Coinsurance() {
        return tier3Coinsurance;
    }
    
    public void setTier3Coinsurance(BigDecimal tier3Coinsurance) {
        this.tier3Coinsurance = tier3Coinsurance;
    }
    
    public BigDecimal getTier4Coinsurance() {
        return tier4Coinsurance;
    }
    
    public void setTier4Coinsurance(BigDecimal tier4Coinsurance) {
        this.tier4Coinsurance = tier4Coinsurance;
    }
    
    public BigDecimal getTier5Coinsurance() {
        return tier5Coinsurance;
    }
    
    public void setTier5Coinsurance(BigDecimal tier5Coinsurance) {
        this.tier5Coinsurance = tier5Coinsurance;
    }
    
    public boolean isMailOrderAvailable() {
        return mailOrderAvailable;
    }
    
    public void setMailOrderAvailable(boolean mailOrderAvailable) {
        this.mailOrderAvailable = mailOrderAvailable;
    }
    
    public boolean isSpecialtyPharmacyRequired() {
        return specialtyPharmacyRequired;
    }
    
    public void setSpecialtyPharmacyRequired(boolean specialtyPharmacyRequired) {
        this.specialtyPharmacyRequired = specialtyPharmacyRequired;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    // Utility methods
    
    /**
     * Get copay for a specific tier
     */
    public BigDecimal getCopayForTier(int tier) {
        return switch (tier) {
            case 1 -> tier1Copay;
            case 2 -> tier2Copay;
            case 3 -> tier3Copay;
            case 4 -> tier4Copay;
            case 5 -> tier5Copay;
            default -> throw new IllegalArgumentException("Invalid tier: " + tier);
        };
    }
    
    /**
     * Get coinsurance for a specific tier
     */
    public BigDecimal getCoinsuranceForTier(int tier) {
        return switch (tier) {
            case 1 -> tier1Coinsurance;
            case 2 -> tier2Coinsurance;
            case 3 -> tier3Coinsurance;
            case 4 -> tier4Coinsurance;
            case 5 -> tier5Coinsurance;
            default -> throw new IllegalArgumentException("Invalid tier: " + tier);
        };
    }
    
    /**
     * Check if plan uses copays (vs coinsurance) for a tier
     */
    public boolean usesCopayForTier(int tier) {
        BigDecimal copay = getCopayForTier(tier);
        return copay != null && copay.compareTo(BigDecimal.ZERO) > 0;
    }
    
    /**
     * Check if plan uses coinsurance (vs copay) for a tier
     */
    public boolean usesCoinsuranceForTier(int tier) {
        BigDecimal coinsurance = getCoinsuranceForTier(tier);
        return coinsurance != null && coinsurance.compareTo(BigDecimal.ZERO) > 0;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BenefitPlan that = (BenefitPlan) o;
        return Objects.equals(planCode, that.planCode);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(planCode);
    }
    
    @Override
    public String toString() {
        return "BenefitPlan{" +
                "planCode='" + planCode + '\'' +
                ", planName='" + planName + '\'' +
                ", planType='" + planType + '\'' +
                ", planCategory='" + planCategory + '\'' +
                ", effectiveDate=" + effectiveDate +
                ", annualDeductible=" + annualDeductible +
                ", outOfPocketMax=" + outOfPocketMax +
                ", mailOrderAvailable=" + mailOrderAvailable +
                ", specialtyPharmacyRequired=" + specialtyPharmacyRequired +
                '}';
    }
}
