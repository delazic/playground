package dejanlazic.playground.inmemory.rdbms.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import dejanlazic.playground.inmemory.rdbms.dao.BenefitPlanDAO;
import dejanlazic.playground.inmemory.rdbms.dao.DrugDAO;
import dejanlazic.playground.inmemory.rdbms.dao.EnrollmentDAO;
import dejanlazic.playground.inmemory.rdbms.dao.FormularyDrugDAO;
import dejanlazic.playground.inmemory.rdbms.dao.MemberDAO;
import dejanlazic.playground.inmemory.rdbms.dao.PharmacyDAO;
import dejanlazic.playground.inmemory.rdbms.dao.PharmacyNetworkDAO;
import dejanlazic.playground.inmemory.rdbms.model.Claim;
import dejanlazic.playground.inmemory.rdbms.model.Drug;
import dejanlazic.playground.inmemory.rdbms.model.Enrollment;
import dejanlazic.playground.inmemory.rdbms.model.FormularyDrug;
import dejanlazic.playground.inmemory.rdbms.model.Member;

/**
 * Service for adjudicating pharmacy claims in real-time.
 * Simulates PBM claim processing following NCPDP standards.
 */
public class ClaimAdjudicationService {
    private static final Logger LOGGER = Logger.getLogger(ClaimAdjudicationService.class.getName());
    
    private final MemberDAO memberDAO;
    private final EnrollmentDAO enrollmentDAO;
    private final PharmacyDAO pharmacyDAO;
    private final PharmacyNetworkDAO pharmacyNetworkDAO;
    private final DrugDAO drugDAO;
    private final FormularyDrugDAO formularyDrugDAO;
    private final BenefitPlanDAO benefitPlanDAO;
    
    private final Random random = new Random();
    private final AtomicLong claimCounter = new AtomicLong(1);
    
    // Adjudication statistics
    private final AtomicLong totalClaims = new AtomicLong(0);
    private final AtomicLong approvedClaims = new AtomicLong(0);
    private final AtomicLong rejectedClaims = new AtomicLong(0);
    private long totalProcessingTimeMs = 0;
    
    public ClaimAdjudicationService(
            MemberDAO memberDAO,
            EnrollmentDAO enrollmentDAO,
            PharmacyDAO pharmacyDAO,
            PharmacyNetworkDAO pharmacyNetworkDAO,
            DrugDAO drugDAO,
            FormularyDrugDAO formularyDrugDAO,
            BenefitPlanDAO benefitPlanDAO) {
        this.memberDAO = memberDAO;
        this.enrollmentDAO = enrollmentDAO;
        this.pharmacyDAO = pharmacyDAO;
        this.pharmacyNetworkDAO = pharmacyNetworkDAO;
        this.drugDAO = drugDAO;
        this.formularyDrugDAO = formularyDrugDAO;
        this.benefitPlanDAO = benefitPlanDAO;
    }
    
    /**
     * Adjudicate a pharmacy claim in real-time.
     * Follows the standard PBM adjudication workflow.
     */
    public ClaimResponse adjudicateClaim(ClaimRequest request) {
        long startTime = System.currentTimeMillis();
        Claim claim = new Claim();
        
        try {
            // Generate claim number
            claim.setClaimNumber(generateClaimNumber());
            claim.setTransactionType("B1"); // Billing
            claim.setDateOfService(request.getDateOfService());
            claim.setMemberId(request.getMemberId());
            claim.setPharmacyId(request.getPharmacyId());
            claim.setNdc(request.getNdc());
            claim.setQuantityDispensed(request.getQuantityDispensed());
            claim.setDaysSupply(request.getDaysSupply());
            claim.setRefillNumber(request.getRefillNumber());
            claim.setIngredientCostSubmitted(request.getIngredientCost());
            claim.setDispensingFeeSubmitted(request.getDispensingFee());
            
            // Step 1: Validate request format (50-100ms)
            simulateProcessingDelay(50, 100);
            if (!validateRequest(request)) {
                return rejectClaim(claim, "M0", "Invalid Request Format", startTime);
            }
            
            // Step 2: Check eligibility (100-200ms)
            simulateProcessingDelay(100, 200);
            Enrollment enrollment = checkEligibility(request.getMemberId(), request.getDateOfService());
            if (enrollment == null) {
                return rejectClaim(claim, "85", "Patient Not Covered", startTime);
            }
            
            // Step 3: Validate pharmacy network (50-100ms)
            simulateProcessingDelay(50, 100);
            if (!isPharmacyInNetwork(request.getPharmacyId(), enrollment.getPlanCode())) {
                return rejectClaim(claim, "75", "Pharmacy Not In Network", startTime);
            }
            
            // Step 4: Check drug coverage (50-150ms)
            simulateProcessingDelay(50, 150);
            FormularyDrug formularyDrug = checkFormulary(request.getNdc(), enrollment.getPlanCode());
            if (formularyDrug == null) {
                return rejectClaim(claim, "70", "Product Not Covered", startTime);
            }
            
            // Step 5: Clinical edits - DUR (200-500ms)
            simulateProcessingDelay(200, 500);
            String durResult = performDUR(request);
            if (durResult != null) {
                return rejectClaim(claim, "88", durResult, startTime);
            }
            
            // Step 6: Check prior authorization (if required) (100-200ms)
            simulateProcessingDelay(100, 200);
            if (requiresPriorAuth(formularyDrug)) {
                if (!hasPriorAuth(request.getMemberId(), request.getNdc())) {
                    return rejectClaim(claim, "75", "Prior Authorization Required", startTime);
                }
            }
            
            // Step 7: Check quantity limits (50-100ms)
            simulateProcessingDelay(50, 100);
            if (!checkQuantityLimits(request, formularyDrug)) {
                return rejectClaim(claim, "76", "Plan Limitations Exceeded", startTime);
            }
            
            // Step 8: Calculate pricing (100-200ms)
            simulateProcessingDelay(100, 200);
            PricingResult pricing = calculatePricing(request, formularyDrug, enrollment);
            
            // Step 9: Update accumulators (100-300ms)
            simulateProcessingDelay(100, 300);
            claim.setDeductibleApplied(pricing.getDeductibleApplied());
            claim.setOopApplied(pricing.getOopApplied());
            
            // Step 10: Build approved response
            claim.setStatus("APPROVED");
            claim.setResponseCode("0");
            claim.setResponseMessage("Approved");
            claim.setPatientPayAmount(pricing.getPatientPay());
            claim.setPlanPayAmount(pricing.getPlanPay());
            
            long processingTime = System.currentTimeMillis() - startTime;
            claim.setProcessingTimeMs((int) processingTime);
            claim.setProcessedTimestamp(LocalDateTime.now());
            
            // Update statistics
            totalClaims.incrementAndGet();
            approvedClaims.incrementAndGet();
            synchronized (this) {
                totalProcessingTimeMs += processingTime;
            }
            
            return new ClaimResponse(claim, true, pricing);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error adjudicating claim: " + claim.getClaimNumber(), e);
            return rejectClaim(claim, "99", "Host Processing Error: " + e.getMessage(), startTime);
        }
    }
    
    /**
     * Validate the claim request format.
     */
    private boolean validateRequest(ClaimRequest request) {
        if (request.getMemberId() == null) return false;
        if (request.getPharmacyId() == null) return false;
        if (request.getNdc() == null || request.getNdc().length() != 11) return false;
        if (request.getQuantityDispensed() == null || request.getQuantityDispensed().compareTo(BigDecimal.ZERO) <= 0) return false;
        if (request.getDaysSupply() == null || request.getDaysSupply() <= 0) return false;
        if (request.getDateOfService() == null) return false;
        return true;
    }
    
    /**
     * Check member eligibility on date of service.
     */
    private Enrollment checkEligibility(Long memberId, LocalDate dateOfService) {
        try {
            // Find member by member number (convert Long to String for lookup)
            String memberNumber = String.valueOf(memberId);
            Optional<Member> memberOpt = memberDAO.findByMemberNumber(memberNumber);
            if (memberOpt.isEmpty()) return null;
            
            // Find enrollment by member number
            List<Enrollment> enrollments = enrollmentDAO.findByMemberNumber(memberNumber);
            
            // Find active enrollment for the date of service
            for (Enrollment enrollment : enrollments) {
                if (enrollment.isActive() &&
                    enrollment.getEffectiveDate() != null &&
                    !enrollment.getEffectiveDate().isAfter(dateOfService) &&
                    (enrollment.getTerminationDate() == null ||
                     !enrollment.getTerminationDate().isBefore(dateOfService))) {
                    return enrollment;
                }
            }
            
            return null;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error checking eligibility for member: " + memberId, e);
            return null;
        }
    }
    
    /**
     * Check if pharmacy is in network for the plan.
     */
    private boolean isPharmacyInNetwork(Long pharmacyId, String planCode) {
        try {
            // For simulation, assume most pharmacies are in network
            // In real system, you'd lookup actual pharmacy network relationships
            return random.nextInt(100) < 95; // 95% are in network
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error checking pharmacy network for pharmacy: " + pharmacyId, e);
            return false;
        }
    }
    
    /**
     * Check if drug is on formulary for the plan.
     */
    private FormularyDrug checkFormulary(String ndc, String planCode) {
        try {
            // Get drug by NDC
            Optional<Drug> drugOpt = drugDAO.findByNdcCode(ndc);
            if (drugOpt.isEmpty()) return null;
            
            // For simulation, create a mock formulary drug
            // In real system, you'd lookup actual formulary relationships
            FormularyDrug formularyDrug = new FormularyDrug();
            formularyDrug.setNdcCode(ndc);
            formularyDrug.setTier(random.nextInt(5) + 1); // Random tier 1-5
            formularyDrug.setStatus("PREFERRED");
            formularyDrug.setRequiresPriorAuth(formularyDrug.getTier() >= 4);
            formularyDrug.setQuantityLimit(formularyDrug.getTier() >= 4 ? 30 : null);
            
            return formularyDrug;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error checking formulary for NDC: " + ndc, e);
            return null;
        }
    }
    
    /**
     * Perform Drug Utilization Review (DUR).
     * Simulates clinical edit checks.
     */
    private String performDUR(ClaimRequest request) {
        // Simulate DUR checks - in real system would check:
        // - Drug-drug interactions
        // - Therapeutic duplication
        // - Age/gender restrictions
        // - Pregnancy/lactation warnings
        
        // 5% chance of DUR reject for simulation
        if (random.nextInt(100) < 5) {
            String[] durRejects = {
                "Drug-Drug Interaction Detected",
                "Therapeutic Duplication",
                "Age Restriction",
                "Gender Restriction"
            };
            return durRejects[random.nextInt(durRejects.length)];
        }
        
        return null; // No DUR issues
    }
    
    /**
     * Check if drug requires prior authorization.
     */
    private boolean requiresPriorAuth(FormularyDrug formularyDrug) {
        // Tier 4 and 5 drugs typically require PA
        return formularyDrug.getTier() >= 4;
    }
    
    /**
     * Check if member has prior authorization on file.
     */
    private boolean hasPriorAuth(Long memberId, String ndc) {
        // Simulate PA check - 80% have PA if required
        return random.nextInt(100) < 80;
    }
    
    /**
     * Check quantity limits.
     */
    private boolean checkQuantityLimits(ClaimRequest request, FormularyDrug formularyDrug) {
        // Check if quantity exceeds limits
        if (formularyDrug.getQuantityLimit() != null) {
            BigDecimal quantityLimit = new BigDecimal(formularyDrug.getQuantityLimit());
            if (request.getQuantityDispensed().compareTo(quantityLimit) > 0) {
                return false;
            }
        }
        
        // Check days supply (typically max 90 days)
        if (request.getDaysSupply() > 90) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Calculate pricing for the claim.
     */
    private PricingResult calculatePricing(ClaimRequest request, FormularyDrug formularyDrug, Enrollment enrollment) {
        PricingResult result = new PricingResult();
        
        // Get ingredient cost (AWP-based pricing)
        BigDecimal ingredientCost = request.getIngredientCost();
        BigDecimal dispensingFee = request.getDispensingFee();
        BigDecimal totalCost = ingredientCost.add(dispensingFee);
        
        // Get copay based on tier
        BigDecimal copay = getCopayForTier(formularyDrug.getTier());
        
        // Calculate patient pay (copay or coinsurance)
        BigDecimal patientPay;
        if (formularyDrug.getTier() <= 3) {
            // Copay for lower tiers
            patientPay = copay;
        } else {
            // Coinsurance for higher tiers (e.g., 30%)
            BigDecimal coinsurance = new BigDecimal("0.30");
            patientPay = totalCost.multiply(coinsurance).setScale(2, RoundingMode.HALF_UP);
        }
        
        // Ensure patient pay doesn't exceed total cost
        if (patientPay.compareTo(totalCost) > 0) {
            patientPay = totalCost;
        }
        
        // Calculate plan pay
        BigDecimal planPay = totalCost.subtract(patientPay);
        
        result.setPatientPay(patientPay);
        result.setPlanPay(planPay);
        result.setTotalCost(totalCost);
        result.setDeductibleApplied(BigDecimal.ZERO); // Simplified
        result.setOopApplied(patientPay);
        
        return result;
    }
    
    /**
     * Get copay amount based on formulary tier.
     */
    private BigDecimal getCopayForTier(Integer tier) {
        switch (tier) {
            case 1: return new BigDecimal("10.00");  // Generic
            case 2: return new BigDecimal("25.00");  // Preferred Brand
            case 3: return new BigDecimal("50.00");  // Non-Preferred Brand
            case 4: return new BigDecimal("100.00"); // Specialty
            case 5: return new BigDecimal("150.00"); // High-Cost Specialty
            default: return new BigDecimal("25.00");
        }
    }
    
    /**
     * Reject a claim with specified code and message.
     */
    private ClaimResponse rejectClaim(Claim claim, String code, String message, long startTime) {
        claim.setStatus("REJECTED");
        claim.setResponseCode(code);
        claim.setResponseMessage(message);
        
        long processingTime = System.currentTimeMillis() - startTime;
        claim.setProcessingTimeMs((int) processingTime);
        claim.setProcessedTimestamp(LocalDateTime.now());
        
        // Update statistics
        totalClaims.incrementAndGet();
        rejectedClaims.incrementAndGet();
        synchronized (this) {
            totalProcessingTimeMs += processingTime;
        }
        
        return new ClaimResponse(claim, false, null);
    }
    
    /**
     * Generate unique claim number.
     */
    private String generateClaimNumber() {
        return String.format("CLM%015d", claimCounter.getAndIncrement());
    }
    
    /**
     * Simulate processing delay to mimic real system latency.
     */
    private void simulateProcessingDelay(int minMs, int maxMs) {
        try {
            int delay = minMs + random.nextInt(maxMs - minMs + 1);
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            LOGGER.log(Level.FINE, "Processing delay interrupted", e);
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Get adjudication statistics.
     */
    public AdjudicationStats getStats() {
        long total = totalClaims.get();
        long approved = approvedClaims.get();
        long rejected = rejectedClaims.get();
        long avgProcessingTime = total > 0 ? totalProcessingTimeMs / total : 0;
        
        return new AdjudicationStats(total, approved, rejected, avgProcessingTime);
    }
    
    /**
     * Reset statistics.
     */
    public void resetStats() {
        totalClaims.set(0);
        approvedClaims.set(0);
        rejectedClaims.set(0);
        totalProcessingTimeMs = 0;
    }
    
    // Inner classes for request/response
    
    public static class ClaimRequest {
        private Long memberId;
        private Long pharmacyId;
        private String ndc;
        private BigDecimal quantityDispensed;
        private Integer daysSupply;
        private Integer refillNumber;
        private LocalDate dateOfService;
        private BigDecimal ingredientCost;
        private BigDecimal dispensingFee;
        
        // Getters and setters
        public Long getMemberId() { return memberId; }
        public void setMemberId(Long memberId) { this.memberId = memberId; }
        
        public Long getPharmacyId() { return pharmacyId; }
        public void setPharmacyId(Long pharmacyId) { this.pharmacyId = pharmacyId; }
        
        public String getNdc() { return ndc; }
        public void setNdc(String ndc) { this.ndc = ndc; }
        
        public BigDecimal getQuantityDispensed() { return quantityDispensed; }
        public void setQuantityDispensed(BigDecimal quantityDispensed) { this.quantityDispensed = quantityDispensed; }
        
        public Integer getDaysSupply() { return daysSupply; }
        public void setDaysSupply(Integer daysSupply) { this.daysSupply = daysSupply; }
        
        public Integer getRefillNumber() { return refillNumber; }
        public void setRefillNumber(Integer refillNumber) { this.refillNumber = refillNumber; }
        
        public LocalDate getDateOfService() { return dateOfService; }
        public void setDateOfService(LocalDate dateOfService) { this.dateOfService = dateOfService; }
        
        public BigDecimal getIngredientCost() { return ingredientCost; }
        public void setIngredientCost(BigDecimal ingredientCost) { this.ingredientCost = ingredientCost; }
        
        public BigDecimal getDispensingFee() { return dispensingFee; }
        public void setDispensingFee(BigDecimal dispensingFee) { this.dispensingFee = dispensingFee; }
    }
    
    public static class ClaimResponse {
        private Claim claim;
        private boolean approved;
        private PricingResult pricing;
        
        public ClaimResponse(Claim claim, boolean approved, PricingResult pricing) {
            this.claim = claim;
            this.approved = approved;
            this.pricing = pricing;
        }
        
        public Claim getClaim() { return claim; }
        public boolean isApproved() { return approved; }
        public PricingResult getPricing() { return pricing; }
    }
    
    public static class PricingResult {
        private BigDecimal patientPay;
        private BigDecimal planPay;
        private BigDecimal totalCost;
        private BigDecimal deductibleApplied;
        private BigDecimal oopApplied;
        
        public BigDecimal getPatientPay() { return patientPay; }
        public void setPatientPay(BigDecimal patientPay) { this.patientPay = patientPay; }
        
        public BigDecimal getPlanPay() { return planPay; }
        public void setPlanPay(BigDecimal planPay) { this.planPay = planPay; }
        
        public BigDecimal getTotalCost() { return totalCost; }
        public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
        
        public BigDecimal getDeductibleApplied() { return deductibleApplied; }
        public void setDeductibleApplied(BigDecimal deductibleApplied) { this.deductibleApplied = deductibleApplied; }
        
        public BigDecimal getOopApplied() { return oopApplied; }
        public void setOopApplied(BigDecimal oopApplied) { this.oopApplied = oopApplied; }
    }
    
    public static class AdjudicationStats {
        private long totalClaims;
        private long approvedClaims;
        private long rejectedClaims;
        private long avgProcessingTimeMs;
        
        public AdjudicationStats(long totalClaims, long approvedClaims, long rejectedClaims, long avgProcessingTimeMs) {
            this.totalClaims = totalClaims;
            this.approvedClaims = approvedClaims;
            this.rejectedClaims = rejectedClaims;
            this.avgProcessingTimeMs = avgProcessingTimeMs;
        }
        
        public long getTotalClaims() { return totalClaims; }
        public long getApprovedClaims() { return approvedClaims; }
        public long getRejectedClaims() { return rejectedClaims; }
        public long getAvgProcessingTimeMs() { return avgProcessingTimeMs; }
        public double getApprovalRate() { 
            return totalClaims > 0 ? (double) approvedClaims / totalClaims * 100 : 0; 
        }
    }
}

// Made with Bob
