package dejanlazic.playground.inmemory.rdbms.service;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import dejanlazic.playground.inmemory.rdbms.converter.ClaimConverter;
import dejanlazic.playground.inmemory.rdbms.dao.ClaimDAO;
import dejanlazic.playground.inmemory.rdbms.model.Claim;
import dejanlazic.playground.inmemory.rdbms.service.ClaimAdjudicationService.ClaimRequest;
import dejanlazic.playground.inmemory.rdbms.service.ClaimAdjudicationService.ClaimResponse;

/**
 * Service for simulating real-time PBM claim processing.
 * Processes 1 million claims over a simulated day with realistic throughput patterns.
 */
public class ClaimSimulationService {
    private static final Logger LOGGER = Logger.getLogger(ClaimSimulationService.class.getName());
    
    private final ClaimAdjudicationService adjudicationService;
    private final ClaimDAO claimDAO;
    private final ClaimConverter claimConverter;
    
    private final AtomicLong totalProcessed = new AtomicLong(0);
    private final AtomicLong totalApproved = new AtomicLong(0);
    private final AtomicLong totalRejected = new AtomicLong(0);
    private final AtomicLong totalProcessingTimeMs = new AtomicLong(0);
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    private LocalDateTime simulationStartTime;
    private List<Claim> allClaims;
    
    public ClaimSimulationService(
            ClaimAdjudicationService adjudicationService,
            ClaimDAO claimDAO) {
        this.adjudicationService = adjudicationService;
        this.claimDAO = claimDAO;
        this.claimConverter = new ClaimConverter();
    }
    
    /**
     * Run the full simulation: load claims, process them, and save to database.
     * Simulates a typical day of PBM processing with 1 million claims.
     * 
     * @param speedMultiplier Speed up factor (1.0 = real-time, 10.0 = 10x faster)
     * @throws IOException if claims cannot be loaded
     * @throws SQLException if database operations fail
     */
    public void runSimulation(double speedMultiplier) throws IOException, SQLException {
        if (running.get()) {
            throw new IllegalStateException("Simulation is already running");
        }
        
        running.set(true);
        simulationStartTime = LocalDateTime.now();
        
        try {
            System.out.println("=".repeat(60));
            System.out.println("PBM CLAIM ADJUDICATION SIMULATION");
            System.out.println("=".repeat(60));
            System.out.println("Speed multiplier: " + speedMultiplier + "x");
            System.out.println("Target: 1,000,000 claims");
            System.out.println("=".repeat(60));
            System.out.println();
            
            // Step 1: Load claims from CSV
            System.out.println("Step 1: Loading claims from CSV...");
            long loadStart = System.currentTimeMillis();
            LOGGER.info("Loading claims from CSV files");
            allClaims = claimConverter.loadAllClaims();
            long loadTime = System.currentTimeMillis() - loadStart;
            LOGGER.info("Loaded " + allClaims.size() + " claims in " + (loadTime / 1000.0) + " seconds");
            System.out.println("✓ Loaded " + String.format("%,d", allClaims.size()) +
                " claims in " + (loadTime / 1000.0) + " seconds");
            System.out.println();
            
            // Step 2: Process claims with realistic throughput
            System.out.println("Step 2: Processing claims with adjudication...");
            System.out.println("Simulating realistic PBM throughput patterns");
            System.out.println();
            
            LOGGER.info("Starting claim processing with speed multiplier: " + speedMultiplier);
            processClaimsWithThroughput(speedMultiplier);
            LOGGER.info("Completed claim processing");
            
            // Step 3: Display final statistics
            displayFinalStatistics();
            
        } finally {
            running.set(false);
        }
    }
    
    /**
     * Process claims with realistic throughput patterns.
     * Simulates varying TPS throughout the day.
     */
    private void processClaimsWithThroughput(double speedMultiplier) throws SQLException {
        // Mid-size PBM: ~1M claims/day
        // Average TPS: ~12 TPS (1M / 86400 seconds)
        // Peak TPS: ~50-100 TPS during business hours
        
        int totalClaims = allClaims.size();
        int claimsProcessed = 0;
        
        // Process in batches to simulate realistic throughput
        int batchSize = 100; // Process 100 claims at a time
        long batchDelayMs = (long) (1000.0 / speedMultiplier); // Delay between batches
        
        long startTime = System.currentTimeMillis();
        long lastReportTime = startTime;
        
        while (claimsProcessed < totalClaims) {
            int batchEnd = Math.min(claimsProcessed + batchSize, totalClaims);
            List<Claim> batch = allClaims.subList(claimsProcessed, batchEnd);
            
            // Process batch
            processBatch(batch);
            
            claimsProcessed = batchEnd;
            
            // Report progress every 10 seconds
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastReportTime >= 10000) {
                reportProgress(claimsProcessed, totalClaims, startTime);
                lastReportTime = currentTime;
            }
            
            // Simulate realistic processing delay
            if (claimsProcessed < totalClaims) {
                try {
                    Thread.sleep(batchDelayMs);
                } catch (InterruptedException e) {
                    LOGGER.log(Level.WARNING, "Simulation interrupted", e);
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        System.out.println();
        System.out.println("✓ Completed processing all claims");
    }
    
    /**
     * Process a batch of claims through adjudication and save to database.
     */
    private void processBatch(List<Claim> batch) throws SQLException {
        List<Claim> adjudicatedClaims = new ArrayList<>();
        
        for (Claim claim : batch) {
            // Convert to ClaimRequest
            ClaimRequest request = convertToRequest(claim);
            
            // Adjudicate
            long adjStart = System.currentTimeMillis();
            ClaimResponse response = adjudicationService.adjudicateClaim(request);
            long adjTime = System.currentTimeMillis() - adjStart;
            
            // Update claim with adjudication results
            Claim adjudicatedClaim = response.getClaim();
            adjudicatedClaims.add(adjudicatedClaim);
            
            // Update statistics
            totalProcessed.incrementAndGet();
            totalProcessingTimeMs.addAndGet(adjTime);
            
            if (response.isApproved()) {
                totalApproved.incrementAndGet();
            } else {
                totalRejected.incrementAndGet();
            }
        }
        
        // Save batch to database
        if (!adjudicatedClaims.isEmpty()) {
            try {
                claimDAO.insertBatch(adjudicatedClaims);
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "Failed to insert batch of " + adjudicatedClaims.size() + " claims", e);
                throw e;
            }
        }
    }
    
    /**
     * Convert Claim to ClaimRequest for adjudication.
     */
    private ClaimRequest convertToRequest(Claim claim) {
        ClaimRequest request = new ClaimRequest();
        request.setMemberId(claim.getMemberId());
        request.setPharmacyId(claim.getPharmacyId());
        request.setNdc(claim.getNdc());
        request.setQuantityDispensed(claim.getQuantityDispensed());
        request.setDaysSupply(claim.getDaysSupply());
        request.setRefillNumber(claim.getRefillNumber());
        request.setDateOfService(claim.getDateOfService());
        request.setIngredientCost(claim.getIngredientCostSubmitted());
        request.setDispensingFee(claim.getDispensingFeeSubmitted());
        return request;
    }
    
    /**
     * Report progress during simulation.
     */
    private void reportProgress(int processed, int total, long startTime) {
        long elapsed = System.currentTimeMillis() - startTime;
        double progress = (processed * 100.0) / total;
        double tps = processed / (elapsed / 1000.0);
        long avgProcessingTime = totalProcessed.get() > 0 ? 
            totalProcessingTimeMs.get() / totalProcessed.get() : 0;
        
        System.out.printf("Progress: %,d / %,d (%.1f%%) | TPS: %.1f | Avg Time: %dms | " +
            "Approved: %,d | Rejected: %,d%n",
            processed, total, progress, tps, avgProcessingTime,
            totalApproved.get(), totalRejected.get());
    }
    
    /**
     * Display final simulation statistics.
     */
    private void displayFinalStatistics() {
        LocalDateTime endTime = LocalDateTime.now();
        Duration duration = Duration.between(simulationStartTime, endTime);
        
        long total = totalProcessed.get();
        long approved = totalApproved.get();
        long rejected = totalRejected.get();
        double approvalRate = total > 0 ? (approved * 100.0) / total : 0;
        double avgProcessingTime = total > 0 ? 
            (double) totalProcessingTimeMs.get() / total : 0;
        double overallTps = total / (duration.toMillis() / 1000.0);
        
        System.out.println();
        System.out.println("=".repeat(60));
        System.out.println("SIMULATION COMPLETE");
        System.out.println("=".repeat(60));
        System.out.println();
        System.out.println("Duration: " + formatDuration(duration));
        System.out.println();
        System.out.println("Claims Processed:");
        System.out.println("  Total:    " + String.format("%,d", total));
        System.out.println("  Approved: " + String.format("%,d", approved) + 
            String.format(" (%.1f%%)", approvalRate));
        System.out.println("  Rejected: " + String.format("%,d", rejected) + 
            String.format(" (%.1f%%)", 100 - approvalRate));
        System.out.println();
        System.out.println("Performance:");
        System.out.println("  Overall TPS: " + String.format("%.1f", overallTps) + 
            " transactions/second");
        System.out.println("  Avg Processing Time: " + String.format("%.0f", avgProcessingTime) + "ms");
        System.out.println();
        System.out.println("Database:");
        try {
            long dbCount = claimDAO.count();
            System.out.println("  Total claims in DB: " + String.format("%,d", dbCount));
            
            long approvedCount = claimDAO.countByStatus("APPROVED");
            long rejectedCount = claimDAO.countByStatus("REJECTED");
            System.out.println("  Approved in DB: " + String.format("%,d", approvedCount));
            System.out.println("  Rejected in DB: " + String.format("%,d", rejectedCount));
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error querying database for final statistics", e);
            System.out.println("  Error querying database: " + e.getMessage());
        }
        System.out.println("=".repeat(60));
    }
    
    /**
     * Format duration for display.
     */
    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    /**
     * Get current simulation statistics.
     */
    public SimulationStats getStats() {
        return new SimulationStats(
            totalProcessed.get(),
            totalApproved.get(),
            totalRejected.get(),
            totalProcessed.get() > 0 ? totalProcessingTimeMs.get() / totalProcessed.get() : 0
        );
    }
    
    /**
     * Check if simulation is currently running.
     */
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * Statistics holder class.
     */
    public static class SimulationStats {
        private final long totalProcessed;
        private final long totalApproved;
        private final long totalRejected;
        private final long avgProcessingTimeMs;
        
        public SimulationStats(long totalProcessed, long totalApproved, 
                             long totalRejected, long avgProcessingTimeMs) {
            this.totalProcessed = totalProcessed;
            this.totalApproved = totalApproved;
            this.totalRejected = totalRejected;
            this.avgProcessingTimeMs = avgProcessingTimeMs;
        }
        
        public long getTotalProcessed() { return totalProcessed; }
        public long getTotalApproved() { return totalApproved; }
        public long getTotalRejected() { return totalRejected; }
        public long getAvgProcessingTimeMs() { return avgProcessingTimeMs; }
        public double getApprovalRate() { 
            return totalProcessed > 0 ? (totalApproved * 100.0) / totalProcessed : 0; 
        }
    }
}

