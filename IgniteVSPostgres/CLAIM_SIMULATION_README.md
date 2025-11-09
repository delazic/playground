# PBM Claim Adjudication Simulation

## Overview

This simulation demonstrates a complete mid-size Pharmacy Benefit Manager (PBM) system processing **1 million pharmacy claims** in a typical day. It includes:

- **Real-time claim adjudication** following NCPDP standards
- **Realistic throughput patterns** (varying TPS throughout the day)
- **Complete adjudication logic** (eligibility, formulary, DUR, pricing)
- **Database persistence** of all processed claims
- **Performance metrics** and statistics

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Claim Simulation Flow                     │
└─────────────────────────────────────────────────────────────┘

1. Generate Claims (Python)
   └─> 1M claims in CSV format (NCPDP-style)

2. Load Claims (ClaimConverter)
   └─> Parse CSV into Claim objects

3. Adjudicate Claims (ClaimAdjudicationService)
   ├─> Eligibility verification
   ├─> Pharmacy network validation
   ├─> Formulary coverage check
   ├─> Clinical edits (DUR)
   ├─> Prior authorization check
   ├─> Quantity limits validation
   ├─> Pricing calculation
   └─> Accumulator updates

4. Save to Database (ClaimDAO)
   └─> Batch insert adjudicated claims

5. Report Statistics
   └─> Display performance metrics
```

## Prerequisites

### 1. Database Setup

Ensure PostgreSQL is running:

```bash
cd IgniteVSPostgres
docker-compose up -d
```

Verify database is accessible:
```bash
docker-compose ps
```

### 2. Load Reference Data

The simulation requires existing reference data:

```bash
# Load benefit plans
make run-create-plan

# Load drugs
make run-create-drug

# Load pharmacies
make run-create-pharmacy

# Load members
make run-create-member

# Load enrollments
make run-create-enrollment

# Load formularies
make run-create-formulary

# Load formulary-drug relationships
make run-create-formulary-drug

# Load pharmacy networks
make run-create-pharmacy-network
```

Verify data is loaded:
```bash
make run-read-plan
make run-read-drug
make run-read-pharmacy
make run-read-member
```

### 3. Generate Claims Data

Generate 1 million claims for simulation:

```bash
cd database/scripts
python3 generate_1m_claims.py
```

**This creates multiple files (~30MB each):**
- `src/main/resources/data/us_pharmacy_claims_simulation_1m_01.csv` (~23 MB)
- `src/main/resources/data/us_pharmacy_claims_simulation_1m_02.csv` (~23 MB)
- `src/main/resources/data/us_pharmacy_claims_simulation_1m_03.csv` (~23 MB)
- ... (9 files total, ~197 MB combined)

**Expected output:**
- 1,000,000 claims split across 9 files
- Each file under GitHub's 100MB limit
- Distributed across 24 hours
- Realistic hourly patterns (peak during business hours)
- NCPDP-compliant format
- Member IDs in string format (e.g., "MBR000466742")

**Configuration:**
You can modify these settings in `generate_1m_claims.py`:
- `TARGET_FILE_SIZE_MB = 30` - Target size for each file
- `TOTAL_CLAIMS = 1_000_000` - Total number of claims

## Running the Simulation

### Using Make Commands (Recommended)

**Quick Test (1-2 minutes):**
```bash
make run-claim-simulation-1000x
```

**Standard Testing (12-15 minutes):**
```bash
make run-claim-simulation-100x
# or simply:
make run-claim-simulation
```

**Real-Time Simulation (24 hours):**
```bash
make run-claim-simulation-1x
```

### Using Maven Directly

**Real-time simulation (1x speed):**
```bash
mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.ClaimSimulationApp" -Dexec.args="1"
```
- Simulates actual PBM throughput
- Takes ~24 hours to complete
- Use for realistic performance testing

**100x speed:**
```bash
mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.ClaimSimulationApp" -Dexec.args="100"
```
- Completes in ~15 minutes
- Recommended for development/testing

**1000x speed:**
```bash
mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.ClaimSimulationApp" -Dexec.args="1000"
```
- Completes in ~1-2 minutes
- Maximum speed for quick validation

### What Happens

1. **File Detection:** Application automatically scans for all `us_pharmacy_claims_simulation_1m_*.csv` files
2. **Multi-File Loading:** Loads from all 9 split files sequentially
3. **Member ID Parsing:** Handles both numeric and string member IDs (e.g., "MBR000466742")
4. **Sequential Processing:** Processes claims with realistic throughput patterns
5. **Progress Reporting:** Shows real-time statistics for each batch
6. **Database Persistence:** Saves all adjudicated claims to PostgreSQL
7. **Final Summary:** Displays overall statistics and performance metrics

## Expected Output

### During Simulation

```
╔══════════════════════════════════════════════════════════════════════════════╗
║                                                                              ║
║              PBM CLAIM ADJUDICATION SIMULATION                               ║
║              Mid-Size PBM - 1 Million Claims/Day                             ║
║                                                                              ║
╚══════════════════════════════════════════════════════════════════════════════╝

Testing database connection...
✓ Connected to: PostgreSQL 15.3

Verifying reference data...
  Members:     1,000,000
  Pharmacies:  50,000
  Drugs:       100,000
  Plans:       500
  Enrollments: 10,000,000

✓ All reference data verified

Starting simulation...

============================================================
PBM CLAIM ADJUDICATION SIMULATION
============================================================
Speed multiplier: 100.0x
Target: 1,000,000 claims
============================================================

Step 1: Loading claims from CSV...
Loading from file 1: /data/us_pharmacy_claims_simulation_1m_01.csv
  Loaded 125,829 claims from file 1
Loading from file 2: /data/us_pharmacy_claims_simulation_1m_02.csv
  Loaded 125,829 claims from file 2
...
Loading from file 9: /data/us_pharmacy_claims_simulation_1m_09.csv
  Loaded 83,886 claims from file 9
Total claims loaded: 1,000,000
✓ Loaded 1,000,000 claims in 45.2 seconds

Step 2: Processing claims with adjudication...
Simulating realistic PBM throughput patterns

Progress: 100,000 / 1,000,000 (10.0%) | TPS: 125.3 | Avg Time: 8ms | Approved: 87,234 | Rejected: 12,766
Progress: 200,000 / 1,000,000 (20.0%) | TPS: 128.7 | Avg Time: 7ms | Approved: 174,123 | Rejected: 25,877
...
Progress: 1,000,000 / 1,000,000 (100.0%) | TPS: 132.1 | Avg Time: 7ms | Approved: 870,456 | Rejected: 129,544

✓ Completed processing all claims
```

### Final Statistics

```
============================================================
SIMULATION COMPLETE
============================================================

Duration: 12m 35s

Claims Processed:
  Total:    1,000,000
  Approved: 870,456 (87.0%)
  Rejected: 129,544 (13.0%)

Performance:
  Overall TPS: 132.5 transactions/second
  Avg Processing Time: 7ms

Database:
  Total claims in DB: 1,000,000
  Approved in DB: 870,456
  Rejected in DB: 129,544
============================================================

✓ Simulation completed successfully!
```

## Understanding the Results

### Approval Rate (~87%)

The simulation targets an 87% approval rate, matching industry benchmarks:
- **Approved (87%)**: Claims that pass all validation checks
- **Rejected (13%)**: Claims that fail one or more checks

### Common Rejection Reasons

| Code | Reason | % of Rejections |
|------|--------|----------------|
| 75 | Prior Authorization Required | 30% |
| 70 | Product Not Covered | 25% |
| 79 | Refill Too Soon | 15% |
| 76 | Plan Limitations Exceeded | 15% |
| 85 | Patient Not Covered | 10% |
| 88 | DUR Reject | 5% |

### Performance Metrics

**Typical Results (100x speed):**
- **Duration**: 12-15 minutes
- **TPS**: 120-150 transactions/second
- **Avg Processing Time**: 5-10ms per claim
- **Database Inserts**: 1,000,000 records

**Real-time (1x speed):**
- **Duration**: ~24 hours
- **TPS**: 10-15 transactions/second
- **Matches**: Real mid-size PBM throughput

## Database Schema

Claims are stored in the `claims` table:

```sql
CREATE TABLE claims (
    claim_id BIGSERIAL PRIMARY KEY,
    claim_number VARCHAR(50) UNIQUE NOT NULL,
    transaction_type CHAR(2) NOT NULL,
    
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
    status VARCHAR(20) NOT NULL,
    response_code VARCHAR(10),
    response_message TEXT,
    
    -- Processing Metrics
    processing_time_ms INTEGER,
    
    -- Accumulators
    deductible_applied DECIMAL(10,2),
    oop_applied DECIMAL(10,2)
);
```

## Querying Results

### View Processed Claims

```sql
-- Total claims by status
SELECT status, COUNT(*), 
       ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER(), 2) as percentage
FROM claims
GROUP BY status;

-- Average processing time
SELECT AVG(processing_time_ms) as avg_ms,
       PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY processing_time_ms) as p50_ms,
       PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY processing_time_ms) as p95_ms,
       PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY processing_time_ms) as p99_ms
FROM claims
WHERE processing_time_ms IS NOT NULL;

-- Rejection reasons
SELECT response_code, response_message, COUNT(*)
FROM claims
WHERE status = 'REJECTED'
GROUP BY response_code, response_message
ORDER BY COUNT(*) DESC;

-- Claims by hour
SELECT EXTRACT(HOUR FROM received_timestamp) as hour,
       COUNT(*) as claim_count
FROM claims
GROUP BY hour
ORDER BY hour;

-- Top drugs by volume
SELECT d.drug_name, d.ndc_code, COUNT(*) as claim_count
FROM claims c
JOIN drugs d ON c.ndc = d.ndc_code
GROUP BY d.drug_name, d.ndc_code
ORDER BY claim_count DESC
LIMIT 10;

-- Financial summary
SELECT 
    COUNT(*) as total_claims,
    SUM(ingredient_cost_submitted) as total_ingredient_cost,
    SUM(patient_pay_amount) as total_patient_pay,
    SUM(plan_pay_amount) as total_plan_pay
FROM claims
WHERE status = 'APPROVED';
```

## Troubleshooting

### Issue: "Claims file not found"

**Solution:**
```bash
cd database/scripts
python3 generate_1m_claims.py
```

Ensure the files exist at:
- `src/main/resources/data/us_pharmacy_claims_simulation_1m_01.csv`
- `src/main/resources/data/us_pharmacy_claims_simulation_1m_02.csv`
- ... (9 files total)

The application will automatically detect and load all files matching the pattern `us_pharmacy_claims_simulation_1m_*.csv`

### Issue: "Reference data missing"

**Solution:** Load all reference data first:
```bash
make run-create-plan
make run-create-drug
make run-create-pharmacy
make run-create-member
make run-create-enrollment
```

### Issue: "Database connection failed"

**Solution:**
```bash
# Check if containers are running
docker-compose ps

# Restart if needed
docker-compose down
docker-compose up -d

# Check logs
docker-compose logs postgres
```

### Issue: Out of Memory

**Solution:** Increase JVM heap size:
```bash
export MAVEN_OPTS="-Xmx4g"
mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.ClaimSimulationApp" -Dexec.args="100"
```

### Issue: Slow Performance

**Recommendations:**
1. Use higher speed multiplier (100x or 1000x)
2. Ensure database has adequate resources
3. Check system load (CPU, memory, disk I/O)
4. Reduce batch size in ClaimSimulationService if needed

## Performance Tuning

### Database Optimization

```sql
-- Add indexes for better query performance
CREATE INDEX idx_claims_status_date ON claims(status, date_of_service);
CREATE INDEX idx_claims_member_date ON claims(member_id, date_of_service DESC);
CREATE INDEX idx_claims_pharmacy_date ON claims(pharmacy_id, date_of_service DESC);

-- Analyze tables
ANALYZE claims;
```

### JVM Tuning

```bash
# For large simulations
export MAVEN_OPTS="-Xmx8g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

## Next Steps

1. **Analyze Results**: Query the database to understand claim patterns
2. **Performance Testing**: Run at different speeds to measure system capacity
3. **Extend Simulation**: Modify adjudication rules or add new scenarios
4. **Integration**: Connect to real pharmacy systems or switches

## Related Documentation

- [PHARMACY_CLAIMS_ADJUDICATION.md](PHARMACY_CLAIMS_ADJUDICATION.md) - Detailed adjudication rules
- [CLAIMS_SIMULATION_GUIDE.md](CLAIMS_SIMULATION_GUIDE.md) - Advanced simulation scenarios
- [DATABASE_INITIALIZATION.md](DATABASE_INITIALIZATION.md) - Database setup guide

---

**Made with Bob** 🤖