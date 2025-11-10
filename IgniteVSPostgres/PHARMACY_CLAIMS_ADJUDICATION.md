# US Healthcare Pharmacy Claims Adjudication

## Overview
This document outlines the rules, processes, and volume metrics for pharmacy claims adjudication in the US healthcare system, specifically for Pharmacy Benefit Manager (PBM) systems.

## 1. Pharmacy Claims Rules (NCPDP Standards)

### 1.1 NCPDP Telecommunication Standard
The National Council for Prescription Drug Programs (NCPDP) defines the standard format for real-time pharmacy claims:

**Key Transaction Types:**
- **B1 (Billing)**: Standard claim submission
- **B2 (Reversal)**: Claim reversal/void
- **B3 (Rebill)**: Claim correction/resubmission
- **E1 (Eligibility)**: Member eligibility verification

### 1.2 Required Data Elements

#### Patient Information
- Member ID (Cardholder ID)
- Person Code (01=cardholder, 02+=dependents)
- Date of Birth
- Gender
- Patient Relationship Code

#### Prescription Information
- Prescription/Service Reference Number
- Product/Service ID (NDC - National Drug Code, 11 digits)
- Quantity Dispensed
- Days Supply
- Date Prescription Written
- Refill Number
- DAW (Dispense As Written) Code

#### Pharmacy Information
- Pharmacy NPI (National Provider Identifier)
- Pharmacy NCPDP Provider ID
- Pharmacy Service Type (01=Community, 02=Compounding, etc.)

#### Pricing Information
- Ingredient Cost Submitted
- Dispensing Fee Submitted
- Professional Service Fee
- Patient Pay Amount
- Tax Amount
- Usual and Customary Charge

#### Prescriber Information
- Prescriber NPI
- Prescriber ID
- DEA Number (for controlled substances)

### 1.3 Adjudication Rules

#### Step 1: Eligibility Verification
```
- Verify member is active on date of service
- Check coverage effective dates
- Validate member ID and person code
- Confirm plan is active
```

#### Step 2: Pharmacy Network Validation
```
- Verify pharmacy is in network for the plan
- Check pharmacy contract status
- Validate pharmacy credentials
```

#### Step 3: Drug Coverage Validation
```
- Check if drug is on formulary
- Verify tier placement
- Check for drug exclusions
- Validate quantity limits
```

#### Step 4: Clinical Edits
```
- Drug-Drug Interactions (DDI)
- Therapeutic Duplication
- Age/Gender restrictions
- Pregnancy/Lactation warnings
- Allergy checks
- Dosage validation
```

#### Step 5: Prior Authorization (PA)
```
- Check if PA required
- Validate PA approval on file
- Check PA expiration date
- Verify approved quantity/days supply
```

#### Step 6: Quantity/Refill Limits
```
- Maximum quantity per fill
- Days supply limits
- Early refill restrictions
- Maximum refills allowed
```

#### Step 7: Pricing Calculation
```
- Apply contracted pharmacy reimbursement
- Calculate ingredient cost (AWP, MAC, WAC based)
- Add dispensing fee
- Apply member cost-sharing (copay/coinsurance)
- Calculate plan payment
```

#### Step 8: Accumulator Updates
```
- Update deductible accumulator
- Update out-of-pocket maximum
- Update benefit period accumulators
- Track utilization metrics
```

## 2. Response Codes

### 2.1 Approval Codes
- **Approved (0)**: Claim paid
- **Approved with Warnings**: Paid but with informational messages

### 2.2 Rejection Codes (Common)
- **70**: Product/Service Not Covered
- **75**: Prior Authorization Required
- **76**: Plan Limitations Exceeded
- **79**: Refill Too Soon
- **85**: Patient Not Covered
- **88**: DUR Reject
- **99**: Host Processing Error
- **M0**: Missing/Invalid Patient ID
- **M1**: Missing/Invalid Date of Birth
- **MX**: Missing/Invalid Prescriber ID

## 3. Real-Time Adjudication Process

### 3.1 Transaction Flow
```
Pharmacy System → Switch/Router → PBM System → Response → Pharmacy System
```

### 3.2 Response Time Requirements
- **Target**: < 3 seconds for 95% of claims
- **Maximum**: < 10 seconds
- **Average**: 1-2 seconds

### 3.3 Processing Steps (Milliseconds)
```
1. Receive & Parse Request:        50-100ms
2. Eligibility Check:              100-200ms
3. Formulary Lookup:               50-150ms
4. Clinical Edits (DUR):           200-500ms
5. Pricing Calculation:            100-200ms
6. Accumulator Updates:            100-300ms
7. Generate Response:              50-100ms
-------------------------------------------
Total:                             650-1550ms
```

## 4. PBM System Volume Metrics

### 4.1 Industry Benchmarks

#### Large PBMs (CVS Caremark, Express Scripts, OptumRx)
- **Daily Claims**: 10-15 million claims/day
- **Peak Hours**: 8 AM - 8 PM local time
- **Peak TPS**: 3,000-5,000 transactions per second
- **Average TPS**: 1,000-2,000 transactions per second
- **Annual Volume**: 3-4 billion claims/year

#### Mid-Size PBMs
- **Daily Claims**: 500,000 - 2 million claims/day
- **Peak TPS**: 200-800 transactions per second
- **Average TPS**: 100-300 transactions per second
- **Annual Volume**: 200-700 million claims/year

#### Small PBMs
- **Daily Claims**: 50,000 - 500,000 claims/day
- **Peak TPS**: 20-200 transactions per second
- **Average TPS**: 10-80 transactions per second
- **Annual Volume**: 20-200 million claims/year

### 4.2 Volume Patterns

#### Daily Pattern
```
12 AM - 6 AM:   5-10% of daily volume
6 AM - 9 AM:    15-20% of daily volume (morning rush)
9 AM - 12 PM:   20-25% of daily volume (peak)
12 PM - 3 PM:   15-20% of daily volume
3 PM - 6 PM:    20-25% of daily volume (evening rush)
6 PM - 9 PM:    10-15% of daily volume
9 PM - 12 AM:   5-10% of daily volume
```

#### Weekly Pattern
```
Monday:         18-20% of weekly volume (highest)
Tuesday:        16-18% of weekly volume
Wednesday:      15-17% of weekly volume
Thursday:       15-17% of weekly volume
Friday:         16-18% of weekly volume
Saturday:       10-12% of weekly volume
Sunday:         6-8% of weekly volume (lowest)
```

#### Monthly Pattern
```
First 5 days:   Peak volume (new month, refills)
Days 6-25:      Normal volume
Last 5 days:    Increased volume (end of month rush)
```

### 4.3 Transaction Mix
```
New Prescriptions:      30-35%
Refills:               60-65%
Reversals:             2-3%
Rebills:               1-2%
```

### 4.4 Approval Rates
```
Approved on First Try:  85-90%
Rejected:              10-15%
  - Prior Auth Required: 3-5%
  - Not Covered:        2-3%
  - Refill Too Soon:    2-3%
  - Other:              3-4%
```

## 5. Simulation Recommendations

### 5.1 Test Scenarios

#### Volume Testing
```java
// Simulate realistic load
- Baseline: 100 TPS
- Peak Load: 500 TPS
- Stress Test: 1000+ TPS
- Duration: 1-4 hours
```

#### Scenario Distribution
```
- 85% Approved claims
- 5% Prior Authorization rejections
- 3% Refill Too Soon rejections
- 2% Not Covered rejections
- 5% Other rejections
```

### 5.2 Key Performance Indicators (KPIs)

#### Response Time
- P50 (Median): < 1 second
- P95: < 3 seconds
- P99: < 5 seconds
- P99.9: < 10 seconds

#### Throughput
- Sustained TPS without degradation
- Peak TPS capacity
- Concurrent user capacity

#### Accuracy
- Correct adjudication rate: > 99.9%
- False positive rate: < 0.1%
- False negative rate: < 0.1%

#### Availability
- Uptime: 99.9% (8.76 hours downtime/year)
- Mean Time Between Failures (MTBF)
- Mean Time To Recovery (MTTR)

## 6. Database Schema for Claims

### 6.1 Claims Table Structure
```sql
CREATE TABLE claims (
    claim_id BIGSERIAL PRIMARY KEY,
    claim_number VARCHAR(50) UNIQUE NOT NULL,
    transaction_type CHAR(2) NOT NULL, -- B1, B2, B3
    
    -- Timestamps
    received_timestamp TIMESTAMP NOT NULL,
    processed_timestamp TIMESTAMP,
    date_of_service DATE NOT NULL,
    
    -- Member Information
    member_id BIGINT NOT NULL,
    person_code CHAR(2) NOT NULL,
    
    -- Pharmacy Information
    pharmacy_id BIGINT NOT NULL,
    pharmacy_npi VARCHAR(10),
    
    -- Prescription Information
    prescription_number VARCHAR(50),
    ndc VARCHAR(11) NOT NULL,
    drug_id BIGINT,
    quantity_dispensed DECIMAL(10,3),
    days_supply INTEGER,
    refill_number INTEGER,
    daw_code CHAR(1),
    
    -- Prescriber Information
    prescriber_npi VARCHAR(10),
    prescriber_id VARCHAR(50),
    
    -- Pricing
    ingredient_cost_submitted DECIMAL(10,2),
    dispensing_fee_submitted DECIMAL(10,2),
    patient_pay_amount DECIMAL(10,2),
    plan_pay_amount DECIMAL(10,2),
    tax_amount DECIMAL(10,2),
    
    -- Adjudication Result
    status VARCHAR(20) NOT NULL, -- APPROVED, REJECTED, REVERSED
    response_code VARCHAR(10),
    response_message TEXT,
    
    -- Processing Metrics
    processing_time_ms INTEGER,
    
    -- Accumulators
    deductible_applied DECIMAL(10,2),
    oop_applied DECIMAL(10,2),
    
    FOREIGN KEY (member_id) REFERENCES members(member_id),
    FOREIGN KEY (pharmacy_id) REFERENCES pharmacies(pharmacy_id),
    FOREIGN KEY (drug_id) REFERENCES drugs(drug_id)
);

CREATE INDEX idx_claims_member ON claims(member_id);
CREATE INDEX idx_claims_pharmacy ON claims(pharmacy_id);
CREATE INDEX idx_claims_date_of_service ON claims(date_of_service);
CREATE INDEX idx_claims_received ON claims(received_timestamp);
CREATE INDEX idx_claims_status ON claims(status);
CREATE INDEX idx_claims_ndc ON claims(ndc);
```

### 6.2 Real-Time Metrics Table
```sql
CREATE TABLE claim_metrics (
    metric_id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    interval_minutes INTEGER NOT NULL, -- 1, 5, 15, 60
    
    -- Volume Metrics
    total_claims INTEGER,
    approved_claims INTEGER,
    rejected_claims INTEGER,
    reversed_claims INTEGER,
    
    -- Performance Metrics
    avg_response_time_ms INTEGER,
    p50_response_time_ms INTEGER,
    p95_response_time_ms INTEGER,
    p99_response_time_ms INTEGER,
    max_response_time_ms INTEGER,
    
    -- Throughput
    transactions_per_second DECIMAL(10,2),
    
    UNIQUE(timestamp, interval_minutes)
);
```

## 7. Implementation Example

### 7.1 Claim Adjudication Service
```java
public class ClaimAdjudicationService {
    
    public ClaimResponse adjudicateClaim(ClaimRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            // Step 1: Validate request format
            validateRequest(request);
            
            // Step 2: Check eligibility
            EligibilityResult eligibility = checkEligibility(
                request.getMemberId(), 
                request.getDateOfService()
            );
            if (!eligibility.isEligible()) {
                return reject("85", "Patient Not Covered");
            }
            
            // Step 3: Validate pharmacy network
            if (!isPharmacyInNetwork(request.getPharmacyId(), 
                                     eligibility.getPlanId())) {
                return reject("75", "Pharmacy Not In Network");
            }
            
            // Step 4: Check drug coverage
            FormularyResult formulary = checkFormulary(
                request.getNdc(), 
                eligibility.getFormularyId()
            );
            if (!formulary.isCovered()) {
                return reject("70", "Product Not Covered");
            }
            
            // Step 5: Apply clinical edits
            DURResult dur = performDUR(request, eligibility.getMemberId());
            if (dur.hasHardRejects()) {
                return reject("88", dur.getRejectMessage());
            }
            
            // Step 6: Check prior authorization
            if (formulary.requiresPriorAuth()) {
                if (!hasPriorAuth(request, eligibility.getMemberId())) {
                    return reject("75", "Prior Authorization Required");
                }
            }
            
            // Step 7: Check quantity limits
            if (!checkQuantityLimits(request, formulary)) {
                return reject("76", "Plan Limitations Exceeded");
            }
            
            // Step 8: Calculate pricing
            PricingResult pricing = calculatePricing(
                request, 
                formulary, 
                eligibility
            );
            
            // Step 9: Update accumulators
            updateAccumulators(eligibility.getMemberId(), pricing);
            
            // Step 10: Save claim
            saveClaim(request, pricing, "APPROVED");
            
            // Step 11: Build response
            return buildApprovedResponse(pricing, dur);
            
        } finally {
            long processingTime = System.currentTimeMillis() - startTime;
            recordMetrics(processingTime);
        }
    }
}
```

## 8. Testing Strategy

### 8.1 Load Testing Scenarios
```
Scenario 1: Normal Load
- Duration: 1 hour
- TPS: 100-200
- Expected: All claims < 3s response

Scenario 2: Peak Load
- Duration: 30 minutes
- TPS: 500-800
- Expected: 95% claims < 3s response

Scenario 3: Stress Test
- Duration: 15 minutes
- TPS: 1000-1500
- Expected: System remains stable

Scenario 4: Endurance Test
- Duration: 8 hours
- TPS: 200-400
- Expected: No memory leaks, stable performance
```

### 8.2 Data Volume for Testing
```
Members: 1,000,000
Pharmacies: 50,000
Drugs: 100,000
Formularies: 100
Plans: 500
Historical Claims: 10,000,000+
```

## 9. Regulatory Compliance

### 9.1 HIPAA Requirements
- Secure transmission (TLS 1.2+)
- Audit logging of all transactions
- Data encryption at rest
- Access controls and authentication

### 9.2 NCPDP Certification
- Compliance with NCPDP Telecommunication Standard
- Support for required transaction types
- Proper error handling and response codes

## 10. Monitoring and Alerting

### 10.1 Key Metrics to Monitor
```
- Response time (P50, P95, P99)
- Throughput (TPS)
- Error rate
- Rejection rate by code
- System resource utilization (CPU, Memory, Disk I/O)
- Database connection pool usage
- Queue depths
```

### 10.2 Alert Thresholds
```
Critical:
- Response time P95 > 5 seconds
- Error rate > 1%
- System availability < 99%

Warning:
- Response time P95 > 3 seconds
- Error rate > 0.5%
- CPU utilization > 80%
- Memory utilization > 85%
```

<<<<<<< HEAD
## 11. Implemented Claim Simulation System 🆕

### 11.1 Overview

The project includes a fully functional claim adjudication simulation that processes 1 million pharmacy claims per day, demonstrating realistic PBM workflows.

### 11.2 System Components

#### Claim Generation
- **Script**: `generate_claims.py`
- **Output**: 1 million NCPDP-compliant claims in CSV format
- **Features**:
  - Realistic hourly distribution (peak 9 AM - 6 PM)
  - Transaction type mix: 95% B1 (Billing), 3% B2 (Reversal), 2% B3 (Rebill)
  - Days supply distribution: 60% 30-day, 20% 90-day, 15% 60-day, 5% other
  - Realistic pricing based on quantity and tier

#### Data Access Layer
- **ClaimDAO**: Database operations with batch insert support (1000 records/batch)
- **ClaimConverter**: CSV to model object conversion with progress tracking
- **BaseDAO Interface**: Standardized CRUD operations

#### Service Layer
- **ClaimAdjudicationService**: Core adjudication logic
  - Eligibility verification
  - Formulary coverage checks
  - DUR (Drug Utilization Review)
  - Pricing calculations
  - Rejection code mapping
  
- **ClaimSimulationService**: Orchestration and workflow management
  - Loads 1M claims from CSV
  - Processes claims with realistic throughput
  - Batch processing (100 claims at a time)
  - Real-time progress reporting
  - Database persistence

#### Application Layer
- **ClaimSimulationApp**: Main entry point
  - Command-line interface
  - Speed multiplier support (1x to 1000x)
  - Database connection verification
  - Reference data validation
  - Comprehensive error handling

### 11.3 Running the Simulation

```bash
# Step 1: Generate claims data
cd database/scripts
python3 generate_claims.py

# Step 2: Ensure reference data is loaded
make load-all-data  # If not already done

# Step 3: Run simulation at 100x speed (~15 minutes)
make run-claim-simulation

# Or run at different speeds:
mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.ClaimSimulationApp" -Dexec.args="1"     # Real-time (~24 hours)
mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.ClaimSimulationApp" -Dexec.args="10"    # 10x speed (~2.4 hours)
mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.ClaimSimulationApp" -Dexec.args="100"   # 100x speed (~15 minutes)
mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.ClaimSimulationApp" -Dexec.args="1000"  # 1000x speed (~1.5 minutes)
```

### 11.4 Expected Results

At 100x speed multiplier:
- **Processing Time**: 12-15 minutes
- **Throughput**: 120-150 TPS
- **Total Claims**: 1,000,000
- **Approved**: ~870,000 (87%)
- **Rejected**: ~130,000 (13%)
- **Average Processing Time**: 3-5ms per claim

### 11.5 Rejection Distribution

The simulation produces realistic rejection patterns:

| Rejection Code | Description | Percentage |
|----------------|-------------|------------|
| REJECT_02 | Member not eligible | ~5% |
| REJECT_04 | Drug not covered | ~3% |
| REJECT_75 | Prior authorization required | ~2% |
| REJECT_88 | DUR rejection | ~2% |
| REJECT_05 | Quantity limit exceeded | ~1% |

### 11.6 Performance Metrics

The simulation tracks and reports:
- **Throughput**: Transactions per second (TPS)
- **Processing Time**: Average time per claim
- **Approval Rate**: Percentage of approved claims
- **Rejection Rate**: Percentage by rejection code
- **Batch Performance**: Time per batch of 100 claims
- **Database Performance**: Insert time and batch efficiency

### 11.7 Database Schema

The simulation uses the existing `claim` table with partitioning:

```sql
-- Claims are partitioned by service_date for performance
CREATE TABLE claim (
    claim_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    claim_number VARCHAR(50) UNIQUE NOT NULL,
    transaction_type VARCHAR(2) NOT NULL,
    member_id UUID NOT NULL,
    pharmacy_id UUID NOT NULL,
    drug_id UUID NOT NULL,
    plan_id UUID NOT NULL,
    service_date DATE NOT NULL,
    fill_date DATE NOT NULL,
    quantity_dispensed DECIMAL(10,3),
    days_supply INTEGER,
    ingredient_cost DECIMAL(10,2),
    dispensing_fee DECIMAL(10,2),
    total_cost DECIMAL(10,2),
    patient_pay DECIMAL(10,2),
    plan_pay DECIMAL(10,2),
    claim_status VARCHAR(20) NOT NULL,
    rejection_code VARCHAR(10),
    submitted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    FOREIGN KEY (member_id) REFERENCES member(member_id),
    FOREIGN KEY (pharmacy_id) REFERENCES pharmacy(pharmacy_id),
    FOREIGN KEY (drug_id) REFERENCES drug(drug_id),
    FOREIGN KEY (plan_id) REFERENCES plan(plan_id)
) PARTITION BY RANGE (service_date);
```

### 11.8 Analysis Queries

After running the simulation, analyze results with:

```sql
-- Overall statistics
SELECT
    COUNT(*) as total_claims,
    COUNT(*) FILTER (WHERE claim_status = 'APPROVED') as approved,
    COUNT(*) FILTER (WHERE claim_status = 'REJECTED') as rejected,
    ROUND(AVG(total_cost), 2) as avg_total_cost,
    ROUND(AVG(patient_pay), 2) as avg_patient_pay,
    ROUND(AVG(plan_pay), 2) as avg_plan_pay
FROM claim;

-- Rejection analysis
SELECT
    rejection_code,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER (), 2) as percentage
FROM claim
WHERE claim_status = 'REJECTED'
GROUP BY rejection_code
ORDER BY count DESC;

-- Hourly distribution
SELECT
    EXTRACT(HOUR FROM service_date) as hour,
    COUNT(*) as claim_count,
    COUNT(*) FILTER (WHERE claim_status = 'APPROVED') as approved,
    COUNT(*) FILTER (WHERE claim_status = 'REJECTED') as rejected
FROM claim
GROUP BY hour
ORDER BY hour;

-- Transaction type distribution
SELECT
    transaction_type,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER (), 2) as percentage
FROM claim
GROUP BY transaction_type
ORDER BY count DESC;

-- Days supply distribution
SELECT
    days_supply,
    COUNT(*) as count,
    ROUND(AVG(total_cost), 2) as avg_cost
FROM claim
WHERE claim_status = 'APPROVED'
GROUP BY days_supply
ORDER BY count DESC;
```

### 11.9 Documentation

For complete documentation, see:
- **[CLAIM_SIMULATION_README.md](./CLAIM_SIMULATION_README.md)**: Detailed setup and usage guide
- **[README.md](./README.md)**: Project overview and quick start

---

=======
>>>>>>> 4df88fe55d3c7255016426d7e7ee16d71d4ab2e7
## References

1. NCPDP Telecommunication Standard Implementation Guide
2. NCPDP SCRIPT Standard (for e-prescribing)
3. CMS Medicare Part D regulations
4. HIPAA Security and Privacy Rules
<<<<<<< HEAD
5. Industry benchmarks from PBM annual reports
6. **Implemented Claim Simulation System** (see Section 11) 🆕
=======
5. Industry benchmarks from PBM annual reports
>>>>>>> 4df88fe55d3c7255016426d7e7ee16d71d4ab2e7
