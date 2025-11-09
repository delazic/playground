# Pharmacy Claims Adjudication Simulation Guide

## Overview
This guide provides comprehensive documentation for the pharmacy claims adjudication simulation system built for US healthcare PBM (Pharmacy Benefit Manager) operations.

## Table of Contents
1. [System Architecture](#system-architecture)
2. [Implementation Details](#implementation-details)
3. [Usage Examples](#usage-examples)
4. [Performance Testing](#performance-testing)
5. [Configuration](#configuration)
6. [Monitoring](#monitoring)

## System Architecture

### Core Components

#### 1. ClaimAdjudicationService
The main service class that orchestrates the entire adjudication process.

**Location**: `src/main/java/dejanlazic/playground/inmemory/rdbms/service/ClaimAdjudicationService.java`

**Key Features**:
- Real-time claim processing
- NCPDP standard compliance
- Performance metrics tracking
- Thread-safe operations
- Configurable processing delays

#### 2. Claim Model
Represents a pharmacy claim with all required NCPDP fields.

**Location**: `src/main/java/dejanlazic/playground/inmemory/rdbms/model/Claim.java`

**Key Fields**:
- Transaction identifiers (claim number, transaction type)
- Member information (member ID, person code)
- Pharmacy information (pharmacy ID, NPI)
- Prescription details (NDC, quantity, days supply)
- Pricing information (ingredient cost, dispensing fee, patient pay)
- Adjudication results (status, response code, processing time)

### Adjudication Workflow

The system follows the standard 8-step PBM adjudication process:

```
1. Request Validation (50-100ms)
   ↓
2. Eligibility Check (100-200ms)
   ↓
3. Pharmacy Network Validation (50-100ms)
   ↓
4. Drug Coverage Check (50-150ms)
   ↓
5. Clinical Edits - DUR (200-500ms)
   ↓
6. Prior Authorization (100-200ms)
   ↓
7. Quantity Limits (50-100ms)
   ↓
8. Pricing Calculation (100-200ms)
```

**Total Processing Time**: 650-1550ms (realistic PBM response times)

## Implementation Details

### Data Access Layer Integration

The service integrates with existing DAO classes:
- `MemberDAO`: Member lookup and validation
- `EnrollmentDAO`: Enrollment status verification
- `PharmacyDAO`: Pharmacy information retrieval
- `DrugDAO`: Drug information and NDC validation
- `FormularyDrugDAO`: Formulary coverage checking
- `BenefitPlanDAO`: Plan benefit information

### Simulation Logic

#### Eligibility Checking
```java
private Enrollment checkEligibility(Long memberId, LocalDate dateOfService) {
    // Convert member ID to member number for lookup
    String memberNumber = String.valueOf(memberId);
    Optional<Member> memberOpt = memberDAO.findByMemberNumber(memberNumber);
    
    // Find active enrollment for date of service
    List<Enrollment> enrollments = enrollmentDAO.findByMemberNumber(memberNumber);
    // ... validation logic
}
```

#### Pharmacy Network Validation
```java
private boolean isPharmacyInNetwork(Long pharmacyId, String planCode) {
    // Simulation: 95% of pharmacies are in network
    return random.nextInt(100) < 95;
}
```

#### Formulary Checking
```java
private FormularyDrug checkFormulary(String ndc, String planCode) {
    // Create mock formulary drug with random tier assignment
    FormularyDrug formularyDrug = new FormularyDrug();
    formularyDrug.setTier(random.nextInt(5) + 1); // Tiers 1-5
    formularyDrug.setRequiresPriorAuth(tier >= 4); // PA for specialty drugs
    // ... additional logic
}
```

### Pricing Logic

#### Tier-Based Copays
```java
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
```

#### Coinsurance Calculation
For specialty drugs (tiers 4-5), the system uses 30% coinsurance instead of copays.

## Usage Examples

### Basic Claim Submission

```java
// Initialize the service
ClaimAdjudicationService adjudicationService = new ClaimAdjudicationService(
    memberDAO, enrollmentDAO, pharmacyDAO, pharmacyNetworkDAO,
    drugDAO, formularyDrugDAO, benefitPlanDAO
);

// Create a claim request
ClaimAdjudicationService.ClaimRequest request = new ClaimAdjudicationService.ClaimRequest();
request.setMemberId(12345L);
request.setPharmacyId(67890L);
request.setNdc("12345678901");
request.setQuantityDispensed(new BigDecimal("30"));
request.setDaysSupply(30);
request.setDateOfService(LocalDate.now());
request.setIngredientCost(new BigDecimal("45.99"));
request.setDispensingFee(new BigDecimal("2.50"));

// Process the claim
ClaimAdjudicationService.ClaimResponse response = adjudicationService.adjudicateClaim(request);

// Check results
if (response.isApproved()) {
    System.out.println("Claim approved!");
    System.out.println("Patient pays: $" + response.getPricing().getPatientPay());
    System.out.println("Plan pays: $" + response.getPricing().getPlanPay());
} else {
    System.out.println("Claim rejected: " + response.getClaim().getResponseMessage());
}
```

### Batch Processing Example

```java
List<ClaimAdjudicationService.ClaimRequest> requests = generateTestClaims(1000);
List<ClaimAdjudicationService.ClaimResponse> responses = new ArrayList<>();

for (ClaimAdjudicationService.ClaimRequest request : requests) {
    ClaimAdjudicationService.ClaimResponse response = adjudicationService.adjudicateClaim(request);
    responses.add(response);
}

// Get statistics
ClaimAdjudicationService.AdjudicationStats stats = adjudicationService.getStats();
System.out.println("Total claims: " + stats.getTotalClaims());
System.out.println("Approval rate: " + stats.getApprovalRate() + "%");
System.out.println("Avg processing time: " + stats.getAvgProcessingTimeMs() + "ms");
```

## Performance Testing

### Load Testing Scenarios

#### Scenario 1: Normal Load
```java
// Target: 100-200 TPS for 1 hour
int targetTPS = 150;
int durationMinutes = 60;
int totalClaims = targetTPS * 60 * durationMinutes;

ExecutorService executor = Executors.newFixedThreadPool(10);
CountDownLatch latch = new CountDownLatch(totalClaims);

long startTime = System.currentTimeMillis();
for (int i = 0; i < totalClaims; i++) {
    executor.submit(() -> {
        try {
            ClaimAdjudicationService.ClaimRequest request = generateRandomClaim();
            adjudicationService.adjudicateClaim(request);
        } finally {
            latch.countDown();
        }
    });
    
    // Control TPS
    Thread.sleep(1000 / targetTPS);
}

latch.await();
long endTime = System.currentTimeMillis();
```

#### Scenario 2: Peak Load
```java
// Target: 500-800 TPS for 30 minutes
int targetTPS = 650;
int durationMinutes = 30;
// ... similar implementation with higher TPS
```

#### Scenario 3: Stress Test
```java
// Target: 1000+ TPS for 15 minutes
int targetTPS = 1200;
int durationMinutes = 15;
// ... test system limits
```

### Performance Metrics

Monitor these key metrics during testing:

```java
ClaimAdjudicationService.AdjudicationStats stats = adjudicationService.getStats();

// Throughput metrics
double actualTPS = stats.getTotalClaims() / (durationSeconds);
System.out.println("Actual TPS: " + actualTPS);

// Response time metrics
System.out.println("Average response time: " + stats.getAvgProcessingTimeMs() + "ms");

// Quality metrics
System.out.println("Approval rate: " + stats.getApprovalRate() + "%");
System.out.println("Error rate: " + calculateErrorRate(responses) + "%");
```

## Configuration

### Processing Delays
Adjust simulation delays to match your testing requirements:

```java
// In simulateProcessingDelay method
private void simulateProcessingDelay(int minMs, int maxMs) {
    // Set to 0 for maximum throughput testing
    if (DISABLE_DELAYS) return;
    
    int delay = minMs + random.nextInt(maxMs - minMs + 1);
    Thread.sleep(delay);
}
```

### Approval Rates
Modify simulation logic to test different scenarios:

```java
// Adjust network participation rate
private boolean isPharmacyInNetwork(Long pharmacyId, String planCode) {
    return random.nextInt(100) < NETWORK_PARTICIPATION_RATE; // Default: 95%
}

// Adjust DUR rejection rate
private String performDUR(ClaimRequest request) {
    if (random.nextInt(100) < DUR_REJECTION_RATE) { // Default: 5%
        return "Drug-Drug Interaction Detected";
    }
    return null;
}
```

## Monitoring

### Real-Time Statistics

```java
// Get current statistics
ClaimAdjudicationService.AdjudicationStats stats = adjudicationService.getStats();

// Log metrics every minute
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
scheduler.scheduleAtFixedRate(() -> {
    ClaimAdjudicationService.AdjudicationStats currentStats = adjudicationService.getStats();
    
    System.out.println("=== Claims Processing Stats ===");
    System.out.println("Total Claims: " + currentStats.getTotalClaims());
    System.out.println("Approved: " + currentStats.getApprovedClaims());
    System.out.println("Rejected: " + currentStats.getRejectedClaims());
    System.out.println("Approval Rate: " + String.format("%.2f%%", currentStats.getApprovalRate()));
    System.out.println("Avg Processing Time: " + currentStats.getAvgProcessingTimeMs() + "ms");
    System.out.println("===============================");
    
}, 0, 60, TimeUnit.SECONDS);
```

### Performance Alerts

```java
// Set up performance monitoring
public void monitorPerformance(ClaimAdjudicationService.AdjudicationStats stats) {
    // Alert if average response time exceeds 3 seconds
    if (stats.getAvgProcessingTimeMs() > 3000) {
        System.err.println("ALERT: High response time - " + stats.getAvgProcessingTimeMs() + "ms");
    }
    
    // Alert if approval rate drops below 85%
    if (stats.getApprovalRate() < 85.0) {
        System.err.println("ALERT: Low approval rate - " + stats.getApprovalRate() + "%");
    }
    
    // Alert if error rate exceeds 1%
    double errorRate = calculateErrorRate();
    if (errorRate > 1.0) {
        System.err.println("ALERT: High error rate - " + errorRate + "%");
    }
}
```

## Database Schema

The system requires the following database tables (see `database/init/01-create-schema.sql`):

### Claims Table
```sql
CREATE TABLE claims (
    claim_id BIGSERIAL PRIMARY KEY,
    claim_number VARCHAR(50) UNIQUE NOT NULL,
    transaction_type CHAR(2) NOT NULL,
    received_timestamp TIMESTAMP NOT NULL,
    processed_timestamp TIMESTAMP,
    date_of_service DATE NOT NULL,
    member_id BIGINT NOT NULL,
    pharmacy_id BIGINT NOT NULL,
    ndc VARCHAR(11) NOT NULL,
    quantity_dispensed DECIMAL(10,3),
    days_supply INTEGER,
    status VARCHAR(20) NOT NULL,
    response_code VARCHAR(10),
    response_message TEXT,
    processing_time_ms INTEGER,
    patient_pay_amount DECIMAL(10,2),
    plan_pay_amount DECIMAL(10,2)
);
```

### Metrics Table
```sql
CREATE TABLE claim_metrics (
    metric_id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    interval_minutes INTEGER NOT NULL,
    total_claims INTEGER,
    approved_claims INTEGER,
    rejected_claims INTEGER,
    avg_response_time_ms INTEGER,
    p95_response_time_ms INTEGER,
    transactions_per_second DECIMAL(10,2)
);
```

## Troubleshooting

### Common Issues

1. **High Response Times**
   - Check database connection pool settings
   - Monitor CPU and memory usage
   - Verify network latency to database

2. **Low Approval Rates**
   - Review eligibility checking logic
   - Verify formulary data completeness
   - Check pharmacy network assignments

3. **Memory Issues**
   - Monitor statistics object growth
   - Implement periodic statistics reset
   - Use appropriate JVM heap settings

### Debug Mode

Enable detailed logging for troubleshooting:

```java
// Add logging to adjudication steps
private static final Logger LOGGER = Logger.getLogger(ClaimAdjudicationService.class.getName());

public ClaimResponse adjudicateClaim(ClaimRequest request) {
    LOGGER.info("Processing claim for member: " + request.getMemberId());
    
    // Log each step
    LOGGER.fine("Step 1: Validating request format");
    // ... processing logic
    
    LOGGER.info("Claim processed in " + processingTime + "ms with status: " + claim.getStatus());
    return response;
}
```

## Integration Points

### External Systems
- **Pharmacy Systems**: Receive claim requests via NCPDP format
- **Member Systems**: Eligibility and enrollment verification
- **Clinical Systems**: DUR and drug interaction checking
- **Pricing Systems**: AWP, MAC, and contract pricing
- **Reporting Systems**: Claims data and analytics

### API Endpoints (Future Enhancement)
```java
@RestController
@RequestMapping("/api/claims")
public class ClaimsController {
    
    @PostMapping("/adjudicate")
    public ResponseEntity<ClaimResponse> adjudicateClaim(@RequestBody ClaimRequest request) {
        ClaimResponse response = adjudicationService.adjudicateClaim(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<AdjudicationStats> getStats() {
        return ResponseEntity.ok(adjudicationService.getStats());
    }
}
```

This comprehensive simulation system provides a realistic foundation for testing PBM claim processing capabilities and understanding the complexities of pharmacy benefit management in the US healthcare system.