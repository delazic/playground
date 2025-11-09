# Pharmacy Claims Adjudication System - Documentation Index

## Overview
This directory contains a complete implementation of a US healthcare pharmacy claims adjudication simulation system, designed to replicate real-world PBM (Pharmacy Benefit Manager) operations.

## Documentation Files

### 1. [PHARMACY_CLAIMS_ADJUDICATION.md](./PHARMACY_CLAIMS_ADJUDICATION.md)
**Purpose**: Comprehensive reference guide for US pharmacy claims processing

**Contents**:
- NCPDP Telecommunication Standards (B1, B2, B3, E1 transactions)
- Required data elements for claim submission
- Complete adjudication workflow (8 steps)
- NCPDP response codes and rejection reasons
- Real-time processing requirements (<3 seconds)
- PBM system volume metrics and benchmarks
- Industry statistics and patterns
- Database schema for claims storage
- Regulatory compliance (HIPAA, NCPDP)
- Monitoring and alerting guidelines

**Target Audience**: Developers, architects, business analysts, QA engineers

**Key Sections**:
- Section 1-3: Claims rules and standards
- Section 4: PBM volume metrics (10-15M claims/day for large PBMs)
- Section 5: Simulation recommendations
- Section 6: Database schema
- Section 7: Implementation examples
- Section 8-10: Testing, compliance, and monitoring

### 2. [CLAIMS_SIMULATION_GUIDE.md](./CLAIMS_SIMULATION_GUIDE.md)
**Purpose**: Practical implementation and usage guide

**Contents**:
- System architecture overview
- Component documentation
- Adjudication workflow details
- Implementation code examples
- Usage patterns and best practices
- Performance testing scenarios
- Configuration options
- Monitoring and troubleshooting
- Integration points

**Target Audience**: Developers, DevOps engineers, performance testers

**Key Sections**:
- System Architecture: Core components and workflow
- Implementation Details: DAO integration and simulation logic
- Usage Examples: Basic and batch processing
- Performance Testing: Load scenarios (100-1200 TPS)
- Configuration: Adjustable parameters
- Monitoring: Real-time statistics and alerts

## Source Code Files

### Models

#### [Claim.java](./src/main/java/dejanlazic/playground/inmemory/rdbms/model/Claim.java)
**Purpose**: POJO representing a pharmacy claim

**Key Features**:
- Complete NCPDP field mapping
- Transaction identifiers (claim number, type)
- Member and pharmacy information
- Prescription details (NDC, quantity, days supply)
- Pricing breakdown (ingredient cost, dispensing fee, patient/plan pay)
- Adjudication results (status, response code, processing time)
- Accumulator tracking (deductible, OOP)

**Usage**:
```java
Claim claim = new Claim();
claim.setClaimNumber("CLM000000000001");
claim.setTransactionType("B1");
claim.setMemberId(12345L);
claim.setNdc("12345678901");
claim.setStatus("APPROVED");
```

### Services

#### [ClaimAdjudicationService.java](./src/main/java/dejanlazic/playground/inmemory/rdbms/service/ClaimAdjudicationService.java)
**Purpose**: Core service for real-time claim adjudication

**Key Features**:
- 8-step adjudication workflow
- Realistic processing delays (650-1550ms)
- Eligibility and enrollment validation
- Pharmacy network checking (95% in-network)
- Formulary coverage verification
- DUR (Drug Utilization Review) simulation
- Prior authorization validation
- Quantity limit enforcement
- Tier-based pricing calculation
- Real-time statistics tracking

**Inner Classes**:
- `ClaimRequest`: Input parameters for claim submission
- `ClaimResponse`: Adjudication result with pricing
- `PricingResult`: Detailed cost breakdown
- `AdjudicationStats`: Performance metrics

**Usage**:
```java
ClaimAdjudicationService service = new ClaimAdjudicationService(
    memberDAO, enrollmentDAO, pharmacyDAO, pharmacyNetworkDAO,
    drugDAO, formularyDrugDAO, benefitPlanDAO
);

ClaimRequest request = new ClaimRequest();
// ... set request parameters

ClaimResponse response = service.adjudicateClaim(request);
```

## Quick Start Guide

### 1. Prerequisites
- Java 17 or higher
- PostgreSQL database
- Maven for dependency management
- Existing member, enrollment, pharmacy, and drug data

### 2. Setup
```bash
# Navigate to project directory
cd IgniteVSPostgres

# Build the project
mvn clean install

# Initialize database (if not already done)
psql -U postgres -d pharmacy_db -f database/init/01-create-schema.sql
```

### 3. Basic Usage
```java
// Initialize service with DAOs
ClaimAdjudicationService adjudicationService = new ClaimAdjudicationService(
    memberDAO, enrollmentDAO, pharmacyDAO, pharmacyNetworkDAO,
    drugDAO, formularyDrugDAO, benefitPlanDAO
);

// Create claim request
ClaimRequest request = new ClaimRequest();
request.setMemberId(12345L);
request.setPharmacyId(67890L);
request.setNdc("12345678901");
request.setQuantityDispensed(new BigDecimal("30"));
request.setDaysSupply(30);
request.setDateOfService(LocalDate.now());
request.setIngredientCost(new BigDecimal("45.99"));
request.setDispensingFee(new BigDecimal("2.50"));

// Process claim
ClaimResponse response = adjudicationService.adjudicateClaim(request);

// Check result
if (response.isApproved()) {
    System.out.println("Approved! Patient pays: $" + 
        response.getPricing().getPatientPay());
} else {
    System.out.println("Rejected: " + 
        response.getClaim().getResponseMessage());
}
```

### 4. Performance Testing
```java
// Run load test
int targetTPS = 200;
int durationMinutes = 60;

for (int i = 0; i < targetTPS * 60 * durationMinutes; i++) {
    ClaimRequest request = generateRandomClaim();
    adjudicationService.adjudicateClaim(request);
    Thread.sleep(1000 / targetTPS);
}

// Get statistics
AdjudicationStats stats = adjudicationService.getStats();
System.out.println("Total: " + stats.getTotalClaims());
System.out.println("Approval Rate: " + stats.getApprovalRate() + "%");
System.out.println("Avg Time: " + stats.getAvgProcessingTimeMs() + "ms");
```

## Key Concepts

### NCPDP Transaction Types
- **B1 (Billing)**: Standard claim submission for payment
- **B2 (Reversal)**: Void a previously submitted claim
- **B3 (Rebill)**: Correct and resubmit a claim
- **E1 (Eligibility)**: Verify member eligibility only

### Adjudication Steps
1. **Request Validation**: Verify all required fields present
2. **Eligibility Check**: Confirm member coverage on date of service
3. **Network Validation**: Verify pharmacy is in plan network
4. **Drug Coverage**: Check if drug is on formulary
5. **Clinical Edits**: DUR checks for interactions, duplications
6. **Prior Authorization**: Verify PA for specialty drugs
7. **Quantity Limits**: Enforce maximum quantity/days supply
8. **Pricing**: Calculate patient and plan responsibility

### Response Codes
- **0**: Approved
- **70**: Product Not Covered
- **75**: Prior Authorization Required / Pharmacy Not In Network
- **76**: Plan Limitations Exceeded
- **79**: Refill Too Soon
- **85**: Patient Not Covered
- **88**: DUR Reject
- **99**: Host Processing Error
- **M0**: Missing/Invalid Request Data

### Formulary Tiers
- **Tier 1**: Generic drugs ($10 copay)
- **Tier 2**: Preferred brand ($25 copay)
- **Tier 3**: Non-preferred brand ($50 copay)
- **Tier 4**: Specialty drugs ($100 copay or 30% coinsurance)
- **Tier 5**: High-cost specialty ($150 copay or 30% coinsurance)

## Performance Benchmarks

### Response Time Targets
- **P50 (Median)**: < 1 second
- **P95**: < 3 seconds
- **P99**: < 5 seconds
- **P99.9**: < 10 seconds

### Throughput Targets
- **Small PBM**: 20-200 TPS
- **Mid-Size PBM**: 200-800 TPS
- **Large PBM**: 1,000-5,000 TPS

### Approval Rates
- **Target**: 85-90% approved on first submission
- **Rejection Distribution**:
  - Prior Auth Required: 3-5%
  - Not Covered: 2-3%
  - Refill Too Soon: 2-3%
  - Other: 3-4%

## Testing Scenarios

### Scenario 1: Normal Load
- **Duration**: 1 hour
- **TPS**: 100-200
- **Expected**: All claims < 3s response time

### Scenario 2: Peak Load
- **Duration**: 30 minutes
- **TPS**: 500-800
- **Expected**: 95% claims < 3s response time

### Scenario 3: Stress Test
- **Duration**: 15 minutes
- **TPS**: 1000-1500
- **Expected**: System remains stable, no crashes

### Scenario 4: Endurance Test
- **Duration**: 8 hours
- **TPS**: 200-400
- **Expected**: No memory leaks, stable performance

## Monitoring Metrics

### Key Metrics to Track
1. **Throughput**: Transactions per second (TPS)
2. **Response Time**: P50, P95, P99 latencies
3. **Approval Rate**: Percentage of approved claims
4. **Error Rate**: System errors and exceptions
5. **Resource Usage**: CPU, memory, database connections

### Alert Thresholds
- **Critical**: Response time P95 > 5s, Error rate > 1%
- **Warning**: Response time P95 > 3s, Error rate > 0.5%

## Integration Points

### Input Sources
- Pharmacy point-of-sale systems
- Mail-order pharmacy systems
- Specialty pharmacy systems
- E-prescribing systems

### Output Destinations
- Claims database for storage
- Reporting and analytics systems
- Financial systems for payment processing
- Audit and compliance systems

## Future Enhancements

### Planned Features
1. **Real Database Integration**: Store claims in PostgreSQL
2. **REST API**: HTTP endpoints for claim submission
3. **Async Processing**: Queue-based processing for high volume
4. **Advanced DUR**: Real drug interaction checking
5. **Accumulator Management**: Track deductibles and OOP max
6. **Reversal Processing**: Handle B2 transactions
7. **Rebill Support**: Handle B3 transactions
8. **Real-time Reporting**: Dashboard for live metrics

### Performance Optimizations
1. **Caching**: Cache frequently accessed data (formularies, networks)
2. **Connection Pooling**: Optimize database connections
3. **Parallel Processing**: Multi-threaded claim processing
4. **Batch Operations**: Bulk database operations

## Support and Troubleshooting

### Common Issues

**Issue**: High response times
- **Solution**: Check database connection pool, verify network latency

**Issue**: Low approval rates
- **Solution**: Review eligibility data, verify formulary completeness

**Issue**: Memory leaks
- **Solution**: Implement periodic statistics reset, monitor object creation

### Debug Mode
Enable detailed logging by setting log level to FINE or DEBUG:
```java
Logger.getLogger(ClaimAdjudicationService.class.getName()).setLevel(Level.FINE);
```

## References

### External Resources
- [NCPDP Standards](https://www.ncpdp.org/)
- [CMS Medicare Part D](https://www.cms.gov/Medicare/Prescription-Drug-Coverage)
- [HIPAA Compliance](https://www.hhs.gov/hipaa/)

### Related Documentation
- [DATABASE_INITIALIZATION.md](./DATABASE_INITIALIZATION.md): Database setup
- [BENEFIT_PLAN_MODEL.md](./BENEFIT_PLAN_MODEL.md): Plan structure
- [ENROLLMENT_SYSTEM.md](./ENROLLMENT_SYSTEM.md): Enrollment management

## License
See [LICENSE](../LICENSE) file for details.

## Contributors
Built with Bob - AI-powered development assistant

---

**Last Updated**: 2025-11-09
**Version**: 1.0.0
**Status**: Production-ready simulation system