# Pharmacy Claims Data Generation and Rules Documentation

## Overview
This document provides comprehensive documentation of the pharmacy claims data generation process, business rules, and adjudication logic used in the PBM (Pharmacy Benefit Manager) simulation system.

## Table of Contents
1. [Claims Data Generation](#claims-data-generation)
2. [Claim Status Rules](#claim-status-rules)
3. [Pricing Calculation Rules](#pricing-calculation-rules)
4. [Adjudication Business Rules](#adjudication-business-rules)
5. [Data Distributions](#data-distributions)
6. [Rejection Codes](#rejection-codes)
7. [Data Model](#data-model)
8. [Generation Process](#generation-process)

---

## Claims Data Generation

### Generation Script
**Location**: `database/scripts/generate_claims.py`

### Configuration Parameters
```python
TOTAL_CLAIMS = 10_000_000          # Total claims to generate
TARGET_FILE_SIZE_MB = 30           # Target size per CSV file
OUTPUT_DIR = "../src/main/resources/data"
FILE_PREFIX = "us_pharmacy_claims"
```

### Data Sources
The claim generator loads reference data from existing CSV files:
- **Members**: `us_pharmacy_members_*.csv` (member IDs)
- **Pharmacies**: `us_pharmacy_pharmacies.csv` (pharmacy IDs)
- **Drugs**: `us_pharmacy_drugs.csv` (drug IDs and NDC codes)
- **Plans**: `us_pharmacy_plans.csv` (plan IDs)

---

## Claim Status Rules

### Status Distribution (Industry Benchmarks)
```python
CLAIM_STATUS_DISTRIBUTION = {
    'APPROVED': 87,      # 87% approved
    'REJECTED': 10,      # 10% rejected
    'PENDING': 2,        # 2% pending
    'REVERSED': 0.5,     # 0.5% reversed
    'REBILLED': 0.5      # 0.5% rebilled
}
```

### Status Definitions

#### APPROVED (87%)
- Claim passes all validation checks
- Member is eligible on date of service
- Drug is covered on formulary
- No clinical edit rejections
- Pricing calculated successfully
- Patient and plan pay amounts determined

#### REJECTED (10%)
- Fails one or more validation checks
- Assigned a rejection code (see Rejection Codes section)
- No payment amounts calculated
- Requires correction and resubmission

#### PENDING (2%)
- Awaiting additional information
- Manual review required
- Prior authorization pending
- Not yet processed
- No processed_at timestamp

#### REVERSED (0.5%)
- Previously approved claim
- Voided after initial approval
- Processed after submission
- Used for claim corrections

#### REBILLED (0.5%)
- Resubmitted after initial rejection
- Corrected claim information
- Processed days after initial submission (1-7 days)

---

## Pricing Calculation Rules

### Base Pricing Components

#### 1. Ingredient Cost
```python
unit_cost = random.uniform(0.50, 150.00)  # Per unit cost
ingredient_cost = unit_cost * quantity_dispensed
```
- Varies by drug type and manufacturer
- Range: $0.50 to $150.00 per unit
- Multiplied by quantity dispensed

#### 2. Dispensing Fee
```python
dispensing_fee = random.uniform(1.00, 5.00)  # Typical range
```
- Standard pharmacy fee for filling prescription
- Range: $1.00 to $5.00
- Fixed per prescription (not per unit)

#### 3. Total Cost
```python
total_cost = ingredient_cost + dispensing_fee
```

### Patient Cost-Sharing (Approved Claims Only)

#### Tier-Based Copays
```python
if tier == 1:  # Generic
    patient_pay = random.uniform(5.00, 15.00)
elif tier == 2:  # Preferred Brand
    patient_pay = random.uniform(15.00, 35.00)
elif tier == 3:  # Non-Preferred Brand
    patient_pay = random.uniform(35.00, 70.00)
elif tier == 4:  # Specialty
    patient_pay = total_cost * 0.30  # 30% coinsurance
else:  # tier == 5, High-Cost Specialty
    patient_pay = total_cost * 0.30  # 30% coinsurance
```

#### Plan Payment
```python
plan_pay = total_cost - patient_pay

# Ensure patient pay doesn't exceed total cost
if patient_pay > total_cost:
    patient_pay = total_cost
    plan_pay = 0.00
```

### Rejected/Pending Claims
```python
# No payment for rejected or pending claims
patient_pay = 0.00
plan_pay = 0.00
```

---

## Adjudication Business Rules

### Step-by-Step Adjudication Process

#### Step 1: Request Validation (50-100ms)
**Location**: `ClaimAdjudicationService.validateRequest()`

**Validation Rules**:
```java
- Member ID must not be null
- Pharmacy ID must not be null
- NDC must be exactly 11 digits
- Quantity dispensed must be > 0
- Days supply must be > 0
- Date of service must not be null
```

**Rejection**: Code "M0" - Invalid Request Format

#### Step 2: Eligibility Check (100-200ms)
**Location**: `ClaimAdjudicationService.checkEligibility()`

**Eligibility Rules**:
```java
1. Member must exist in system
2. Member must have active enrollment
3. Enrollment effective date <= date of service
4. Enrollment termination date >= date of service (or null)
5. Enrollment status must be 'ACTIVE'
```

**Rejection**: Code "85" - Patient Not Covered

#### Step 3: Pharmacy Network Validation (50-100ms)
**Location**: `ClaimAdjudicationService.isPharmacyInNetwork()`

**Network Rules**:
```java
- Pharmacy must be in network for member's plan
- Simulation: 95% of pharmacies are in network
- In production: Lookup pharmacy_network table
```

**Rejection**: Code "75" - Pharmacy Not In Network

#### Step 4: Drug Coverage Check (50-150ms)
**Location**: `ClaimAdjudicationService.checkFormulary()`

**Formulary Rules**:
```java
1. Drug must exist in drug table (by NDC)
2. Drug must be on plan's formulary
3. Tier assignment (1-5) determines cost-sharing
4. Status must be 'PREFERRED' or 'NON-PREFERRED'
```

**Tier Assignment**:
- Tier 1: Generic drugs
- Tier 2: Preferred brand drugs
- Tier 3: Non-preferred brand drugs
- Tier 4: Specialty drugs (requires PA)
- Tier 5: High-cost specialty drugs (requires PA)

**Rejection**: Code "70" - Product Not Covered

#### Step 5: Clinical Edits - DUR (200-500ms)
**Location**: `ClaimAdjudicationService.performDUR()`

**DUR (Drug Utilization Review) Checks**:
```java
- Drug-drug interactions
- Therapeutic duplication
- Age/gender restrictions
- Pregnancy/lactation warnings
- Simulation: 5% rejection rate
```

**Rejection**: Code "88" - DUR Reject
- "Drug-Drug Interaction Detected"
- "Therapeutic Duplication"
- "Age Restriction"
- "Gender Restriction"

#### Step 6: Prior Authorization (100-200ms)
**Location**: `ClaimAdjudicationService.requiresPriorAuth()`

**Prior Authorization Rules**:
```java
- Required for Tier 4 and Tier 5 drugs
- Must have PA approval on file
- PA must not be expired
- Simulation: 80% have PA when required
```

**Rejection**: Code "75" - Prior Authorization Required

#### Step 7: Quantity Limits (50-100ms)
**Location**: `ClaimAdjudicationService.checkQuantityLimits()`

**Quantity Limit Rules**:
```java
1. Quantity must not exceed formulary limit
2. Days supply maximum: 90 days
3. Specialty drugs (Tier 4-5): 30-day limit
```

**Rejection**: Code "76" - Plan Limitations Exceeded

#### Step 8: Pricing Calculation (100-200ms)
**Location**: `ClaimAdjudicationService.calculatePricing()`

**Pricing Rules**:
```java
1. Get ingredient cost from submission
2. Add dispensing fee
3. Calculate total cost
4. Apply tier-based copay or coinsurance
5. Calculate plan payment
6. Update accumulators (deductible, OOP)
```

---

## Data Distributions

### Days Supply Distribution
```python
DAYS_SUPPLY_DISTRIBUTION = {
    30: 60,   # 60% are 30-day supplies
    60: 15,   # 15% are 60-day supplies
    90: 20,   # 20% are 90-day supplies
    7: 3,     # 3% are 7-day supplies
    14: 2     # 2% are 14-day supplies
}
```

### Quantity Ranges by Days Supply
```python
QUANTITY_RANGES = {
    7: (7, 14),        # 7-14 units for 7-day supply
    14: (14, 28),      # 14-28 units for 14-day supply
    30: (30, 90),      # 30-90 units for 30-day supply
    60: (60, 180),     # 60-180 units for 60-day supply
    90: (90, 270)      # 90-270 units for 90-day supply
}
```

### Date Generation
```python
# Service dates span 2024-2025
start_date = datetime(2024, 1, 1)
end_date = datetime(2025, 12, 31)

# Fill date is 0-2 days after service date
fill_date = service_date + timedelta(days=random.randint(0, 2))

# Submission is 0-48 hours after service date
submitted_at = service_date + timedelta(hours=random.randint(0, 48))
```

### Processing Timestamps
```python
if status in ['APPROVED', 'REJECTED']:
    # Processed within seconds to 1 hour of submission
    processed_at = submitted_at + timedelta(seconds=random.randint(1, 3600))
    
elif status == 'REVERSED':
    # Reversed after being approved
    processed_at = submitted_at + timedelta(seconds=random.randint(1, 3600))
    
elif status == 'REBILLED':
    # Rebilled 1-7 days after initial rejection
    processed_at = submitted_at + timedelta(days=random.randint(1, 7))
    
else:  # PENDING
    # Not yet processed
    processed_at = None
```

---

## Rejection Codes

### Rejection Code Distribution (for rejected claims)
```python
REJECTION_CODES = {
    '70': 25,   # Product Not Covered (25%)
    '75': 30,   # Prior Authorization Required (30%)
    '76': 15,   # Plan Limitations Exceeded (15%)
    '79': 15,   # Refill Too Soon (15%)
    '85': 10,   # Patient Not Covered (10%)
    '88': 5     # DUR Reject (5%)
}
```

### Detailed Rejection Code Definitions

#### Code 70: Product Not Covered
- Drug is not on the plan's formulary
- NDC is not in the formulary_drug table
- Drug may be excluded from coverage
- **Resolution**: Use alternative drug or request formulary exception

#### Code 75: Prior Authorization Required
- Drug requires prior authorization
- No PA approval on file
- PA may be expired
- **Resolution**: Submit PA request to plan

#### Code 76: Plan Limitations Exceeded
- Quantity exceeds formulary limit
- Days supply exceeds maximum (typically 90 days)
- Refill limit reached
- **Resolution**: Reduce quantity or request override

#### Code 79: Refill Too Soon
- Attempting to refill before allowed date
- Based on days supply of previous fill
- **Resolution**: Wait until refill date or request override

#### Code 85: Patient Not Covered
- Member not found in system
- Enrollment not active on date of service
- Coverage terminated
- **Resolution**: Verify member ID and coverage dates

#### Code 88: DUR Reject
- Clinical edit failure
- Drug-drug interaction detected
- Therapeutic duplication
- Age/gender restriction
- **Resolution**: Review clinical information, request override

#### Code M0: Invalid Request Format
- Missing required fields
- Invalid data format
- NDC not 11 digits
- **Resolution**: Correct request format and resubmit

#### Code 99: Host Processing Error
- System error during processing
- Database connection failure
- Unexpected exception
- **Resolution**: Retry submission

---

## Data Model

### Claim Fields

#### Identifiers
```python
claim_id: UUID                    # Unique claim identifier
claim_number: String              # Sequential claim number (CLM000000000000001)
```

#### Member Information
```python
member_id: UUID                   # Foreign key to member table
```

#### Pharmacy Information
```python
pharmacy_id: UUID                 # Foreign key to pharmacy table
```

#### Drug Information
```python
drug_id: UUID                     # Foreign key to drug table
ndc_code: String (11 digits)      # National Drug Code
```

#### Plan Information
```python
plan_id: UUID                     # Foreign key to plan table
```

#### Prescription Details
```python
service_date: Date                # Date prescription filled
fill_date: Date                   # Date prescription dispensed
quantity_dispensed: Decimal       # Number of units dispensed
days_supply: Integer              # Number of days supply
```

#### Pricing Information
```python
ingredient_cost: Decimal(10,2)    # Cost of drug ingredient
dispensing_fee: Decimal(10,2)     # Pharmacy dispensing fee
total_cost: Decimal(10,2)         # Total cost (ingredient + fee)
patient_pay: Decimal(10,2)        # Patient responsibility
plan_pay: Decimal(10,2)           # Plan payment amount
```

#### Adjudication Results
```python
claim_status: String              # APPROVED, REJECTED, PENDING, REVERSED, REBILLED
rejection_code: String            # Rejection code if rejected
```

#### Timestamps
```python
submitted_at: Timestamp           # When claim was submitted
processed_at: Timestamp           # When claim was processed (null if pending)
created_at: Timestamp             # Record creation timestamp
updated_at: Timestamp             # Record update timestamp
```

---

## Generation Process

### Step-by-Step Generation Flow

#### 1. Initialize Generator
```python
generator = ClaimsDataGenerator()
generator.load_reference_data()  # Load members, pharmacies, drugs, plans
```

#### 2. Generate Single Claim
```python
def generate_claim():
    # Select random foreign keys
    member_id = random.choice(member_ids)
    pharmacy_id = random.choice(pharmacy_ids)
    drug_id = random.choice(drug_ids)
    plan_id = random.choice(plan_ids)
    
    # Generate dates
    service_date = generate_service_date()  # Random date in 2024-2025
    fill_date = service_date + timedelta(days=random.randint(0, 2))
    
    # Generate claim number
    claim_number = f"CLM{claim_counter:015d}"
    
    # Determine status (weighted random)
    status = weighted_choice(CLAIM_STATUS_DISTRIBUTION)
    
    # Determine rejection code if rejected
    rejection_code = weighted_choice(REJECTION_CODES) if status == 'REJECTED' else ''
    
    # Generate days supply and quantity
    days_supply = weighted_choice(DAYS_SUPPLY_DISTRIBUTION)
    quantity_min, quantity_max = QUANTITY_RANGES[days_supply]
    quantity_dispensed = random.randint(quantity_min, quantity_max)
    
    # Calculate pricing
    ingredient_cost, dispensing_fee, total_cost, patient_pay, plan_pay = 
        calculate_pricing(days_supply, quantity_dispensed, status)
    
    # Generate timestamps
    submitted_at = service_date + timedelta(hours=random.randint(0, 48))
    processed_at = calculate_processed_at(status, submitted_at)
    
    return claim_dict
```

#### 3. Write to CSV Files
```python
# Write claims to multiple CSV files (~30MB each)
# Automatically splits into new file when size limit reached
# Progress tracking every 100,000 claims
```

#### 4. Output Statistics
```python
Total claims generated: 10,000,000
Number of files: ~334 files
Average claims per file: ~30,000

Expected distribution:
  Approved: ~8,700,000 (87%)
  Rejected: ~1,000,000 (10%)
  Pending: ~200,000 (2%)
  Reversed: ~50,000 (0.5%)
  Rebilled: ~50,000 (0.5%)
```

---

## Real-Time Adjudication Simulation

### ClaimAdjudicationService

#### Processing Time Simulation
```java
// Each step has realistic processing delay
Step 1: Request Validation        50-100ms
Step 2: Eligibility Check         100-200ms
Step 3: Network Validation         50-100ms
Step 4: Formulary Check            50-150ms
Step 5: DUR Processing            200-500ms
Step 6: Prior Authorization       100-200ms
Step 7: Quantity Limits            50-100ms
Step 8: Pricing Calculation       100-200ms
Step 9: Accumulator Updates       100-300ms
----------------------------------------
Total Processing Time:            650-1550ms
```

#### Performance Metrics
```java
// Service tracks real-time statistics
- Total claims processed
- Approved claims count
- Rejected claims count
- Average processing time
- Approval rate percentage
```

#### Thread Safety
```java
// Atomic counters for concurrent processing
private final AtomicLong totalClaims = new AtomicLong(0);
private final AtomicLong approvedClaims = new AtomicLong(0);
private final AtomicLong rejectedClaims = new AtomicLong(0);
```

---

## Usage Examples

### Generate Claims Data
```bash
cd database/scripts
python3 generate_claims.py
```

### Load Claims into Database
```bash
# Using provided scripts
make load-claims-data

# Or manually
psql -U postgres -d pbm_db -f load_claims.sql
```

### Run Adjudication Simulation
```bash
# Compile and run
mvn clean compile
mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.ClaimSimulationApp"
```

### Query Claims Data
```sql
-- Overall statistics
SELECT
    claim_status,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER (), 2) as percentage,
    ROUND(AVG(total_cost), 2) as avg_total_cost,
    ROUND(AVG(patient_pay), 2) as avg_patient_pay
FROM claim
GROUP BY claim_status
ORDER BY count DESC;

-- Rejection analysis
SELECT
    rejection_code,
    COUNT(*) as count,
    ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER (), 2) as percentage
FROM claim
WHERE claim_status = 'REJECTED'
GROUP BY rejection_code
ORDER BY count DESC;

-- Days supply distribution
SELECT
    days_supply,
    COUNT(*) as count,
    ROUND(AVG(quantity_dispensed), 2) as avg_quantity,
    ROUND(AVG(total_cost), 2) as avg_cost
FROM claim
WHERE claim_status = 'APPROVED'
GROUP BY days_supply
ORDER BY count DESC;
```

---

## Validation Rules Summary

### Data Quality Checks
1. **Foreign Key Integrity**: All member, pharmacy, drug, and plan IDs must exist
2. **Date Logic**: fill_date >= service_date, processed_at >= submitted_at
3. **Pricing Logic**: total_cost = ingredient_cost + dispensing_fee
4. **Payment Logic**: patient_pay + plan_pay = total_cost (for approved claims)
5. **Status Logic**: Rejected claims have rejection_code, approved claims don't
6. **Timestamp Logic**: Pending claims have no processed_at timestamp

### Business Rule Validation
1. **Quantity Ranges**: Quantity must match days supply ranges
2. **Days Supply Limits**: Maximum 90 days for most drugs, 30 for specialty
3. **Pricing Ranges**: Ingredient cost $0.50-$150 per unit, dispensing fee $1-$5
4. **Status Distribution**: Matches industry benchmarks (87% approved)
5. **Rejection Distribution**: Matches typical PBM rejection patterns

---

## Performance Considerations

### Generation Performance
- **Speed**: ~100,000 claims per minute
- **Memory**: Processes claims in batches to manage memory
- **File Size**: Automatically splits into ~30MB files for easier handling
- **Progress**: Reports progress every 100,000 claims

### Database Loading Performance
- **Batch Inserts**: 1,000 records per batch for optimal performance
- **Indexes**: Created after data load for faster insertion
- **Partitioning**: Claims partitioned by service_date for query performance
- **Constraints**: Foreign keys validated during load

### Query Performance
- **Indexes**: On member_id, pharmacy_id, drug_id, service_date, claim_status
- **Partitioning**: By service_date (monthly partitions)
- **Statistics**: Updated after data load for optimal query plans

---

## References

1. **NCPDP Telecommunication Standard**: Industry standard for pharmacy claims
2. **Industry Benchmarks**: Based on actual PBM processing statistics
3. **Medicare Part D**: CMS regulations for prescription drug coverage
4. **HIPAA**: Security and privacy requirements for healthcare data

---

## Document Version
- **Version**: 1.0
- **Last Updated**: 2025-11-09
- **Author**: PBM Simulation System
- **Related Documents**:
  - [PHARMACY_CLAIMS_ADJUDICATION.md](./PHARMACY_CLAIMS_ADJUDICATION.md)
  - [CLAIMS_SIMULATION_GUIDE.md](./CLAIMS_SIMULATION_GUIDE.md)
  - [CLAIM_SIMULATION_README.md](./CLAIM_SIMULATION_README.md)