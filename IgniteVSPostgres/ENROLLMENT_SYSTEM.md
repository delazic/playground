# Enrollment System Documentation

**Last Updated:** 2025-11-07 00:47 UTC

## Overview

The Enrollment System manages the relationship between members and benefit plans in the PBM system. It implements US healthcare enrollment rules and handles 10 million enrollment records with realistic scenarios including dual coverage, plan transitions, and historical enrollments.

---

## Table of Contents

1. [System Architecture](#system-architecture)
2. [Data Model](#data-model)
3. [US Healthcare Enrollment Rules](#us-healthcare-enrollment-rules)
4. [Implementation Details](#implementation-details)
5. [Usage Guide](#usage-guide)
6. [Performance Metrics](#performance-metrics)
7. [Testing](#testing)

---

## System Architecture

### Components

```
┌─────────────────────────────────────────────────────────────┐
│                    Enrollment System                         │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐    ┌──────────────────┐    ┌───────────┐ │
│  │ Enrollment   │───▶│ Enrollment       │───▶│ Enrollment│ │
│  │ Model (POJO) │    │ Converter (CSV)  │    │ DAO       │ │
│  └──────────────┘    └──────────────────┘    └───────────┘ │
│         │                     │                      │       │
│         │                     │                      │       │
│         ▼                     ▼                      ▼       │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              PostgreSQL Database                      │  │
│  │  - enrollment table (10M records)                    │  │
│  │  - Foreign keys to member and plan tables           │  │
│  │  - Indexes on member_id, plan_id, dates             │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

### Data Flow

1. **Data Generation** (Python script)
   - Generates 10M realistic enrollment records
   - Follows US healthcare enrollment rules
   - Outputs 20 CSV files (~30MB each)

2. **Data Loading** (EnrollmentConverter)
   - Reads CSV files from classpath or file system
   - Parses and validates enrollment data
   - Creates Enrollment objects

3. **Data Persistence** (EnrollmentDAO)
   - Resolves foreign keys (member_number → member_id, plan_code → plan_id)
   - Batch inserts with progress tracking
   - Performance metrics logging

---

## Data Model

### Enrollment POJO

```java
public class Enrollment {
    private String memberNumber;      // Business key for member lookup
    private String planCode;          // Business key for plan lookup
    private String groupNumber;       // Group/employer identifier
    private LocalDate effectiveDate;  // Coverage start date
    private LocalDate terminationDate; // Coverage end date (null if active)
    private String relationship;      // SELF, SPOUSE, DEPENDENT
    private boolean isActive;         // Current enrollment status
    
    // Utility methods
    public boolean isCurrentlyActive()
    public boolean isExpired()
    public long getDuration()
}
```

### Database Schema

```sql
CREATE TABLE enrollment (
    enrollment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    member_id UUID NOT NULL REFERENCES member(member_id),
    plan_id UUID NOT NULL REFERENCES plan(plan_id),
    group_number VARCHAR(50),
    effective_date DATE NOT NULL,
    termination_date DATE,
    relationship VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_enrollment_dates 
        CHECK (termination_date IS NULL OR termination_date >= effective_date)
);

-- Indexes for performance
CREATE INDEX idx_enrollment_member ON enrollment(member_id);
CREATE INDEX idx_enrollment_plan ON enrollment(plan_id);
CREATE INDEX idx_enrollment_active ON enrollment(is_active);
CREATE INDEX idx_enrollment_dates ON enrollment(effective_date, termination_date);
```

### CSV Format

```csv
member_number,plan_code,group_number,effective_date,termination_date,relationship,is_active
M000001,COMM-GOLD-001,GRP-12345,2024-01-01,,SELF,true
M000001,COMM-SILVER-002,GRP-12345,2023-01-01,2023-12-31,SELF,false
M000002,MCARE-PARTD-001,GRP-67890,2024-01-01,,SELF,true
```

---

## US Healthcare Enrollment Rules

### Enrollment Scenarios

The system implements realistic US healthcare enrollment patterns:

#### 1. Single Active Coverage (70%)
- Member has one active enrollment
- Most common scenario
- Example: Employee with employer-sponsored plan

```
Member M000001
├─ Plan: COMM-GOLD-001
├─ Effective: 2024-01-01
├─ Termination: null
└─ Status: Active
```

#### 2. Dual Coverage (15%)
- Member has two active enrollments
- Common with Medicare + supplemental
- Example: Retiree with Medicare Part D + Medigap

```
Member M000002
├─ Plan: MCARE-PARTD-001 (Primary)
│  ├─ Effective: 2024-01-01
│  └─ Status: Active
└─ Plan: MCARE-MEDIGAP-001 (Secondary)
   ├─ Effective: 2024-01-01
   └─ Status: Active
```

#### 3. Plan Transition (10%)
- Member changed plans during the year
- Previous enrollment terminated, new enrollment active
- Example: Job change or open enrollment

```
Member M000003
├─ Plan: COMM-SILVER-002 (Old)
│  ├─ Effective: 2023-01-01
│  ├─ Termination: 2024-03-31
│  └─ Status: Inactive
└─ Plan: COMM-GOLD-001 (New)
   ├─ Effective: 2024-04-01
   └─ Status: Active
```

#### 4. Historical Only (5%)
- Member has no active enrollment
- All enrollments terminated
- Example: Former employee, coverage lapsed

```
Member M000004
└─ Plan: COMM-BRONZE-003
   ├─ Effective: 2023-01-01
   ├─ Termination: 2023-12-31
   └─ Status: Inactive
```

### Relationship Types

- **SELF**: Primary member (subscriber)
- **SPOUSE**: Spouse of primary member
- **DEPENDENT**: Child or other dependent

### Date Constraints

- `effective_date` must be present
- `termination_date` can be null (ongoing coverage)
- `termination_date` must be >= `effective_date`
- Active enrollments have `is_active = true` and `termination_date IS NULL`

---

## Implementation Details

### EnrollmentConverter

**Purpose:** Load enrollment data from CSV files

**Key Features:**
- Multi-file support (loads all matching files)
- Pattern matching: `us_pharmacy_enrollments_*.csv`
- Progress logging
- Statistics generation

**Usage:**
```java
EnrollmentConverter converter = new EnrollmentConverter();

// Get available file count
int fileCount = converter.getAvailableFileCount();

// Load all enrollments
List<Enrollment> enrollments = converter.loadAllEnrollments();

// Load specific file
List<Enrollment> batch = converter.loadEnrollments("us_pharmacy_enrollments_01.csv");
```

### EnrollmentDAO

**Purpose:** Database operations for enrollments

**Key Features:**
- Foreign key resolution using JOIN queries
- Batch insert with configurable batch size (1000 records)
- Progress tracking every 10,000 records
- Performance metrics logging
- Transaction management

**Foreign Key Resolution:**
```sql
INSERT INTO enrollment (
    member_id, plan_id, group_number, effective_date, 
    termination_date, relationship, is_active
)
SELECT 
    m.member_id,
    p.plan_id,
    ?, ?, ?, ?, ?
FROM member m
CROSS JOIN plan p
WHERE m.member_number = ? AND p.plan_code = ?
```

**Usage:**
```java
EnrollmentDAO dao = new EnrollmentDAO(connector);

// Insert batch
int inserted = dao.insertBatch(enrollments);

// Count operations
long total = dao.count();
long active = dao.countActive();

// Find by member
List<Enrollment> memberEnrollments = dao.findByMemberNumber("M000001");

// Find all
List<Enrollment> all = dao.findAll();
```

---

## Usage Guide

### Quick Start

1. **Start Services:**
```bash
cd IgniteVSPostgres
make start
```

2. **Load All Data (Recommended):** 🆕
```bash
# Load all data in correct order: Plan → Member → Enrollment
# This single command handles all foreign key relationships
make load-all-data
```

**Alternative: Load Data Step by Step:**
```bash
# Step 1: Load plans first (enrollments reference plans)
make run-create-plan

# Step 2: Load members (enrollments reference members)
make run-create-member

# Step 3: Load enrollments
make run-create-enrollment
```

### Command-Line Operations

**LOAD ALL DATA - Insert all entities in correct order:** 🆕
```bash
make load-all-data
# Loads: Plan (34) → Member (1M) → Enrollment (10M)
# Total time: ~10-15 minutes
```

**CREATE - Insert enrollments:**
```bash
make run-create-enrollment
# Or: mvn exec:java -Dexec.args="CREATE ENROLLMENT"
```

**READ - Display statistics:**
```bash
make run-read-enrollment
# Or: mvn exec:java -Dexec.args="READ ENROLLMENT"
```

**UPDATE - Update enrollment:**
```bash
make run-update-enrollment
# Or: mvn exec:java -Dexec.args="UPDATE ENROLLMENT"
```

**DELETE - Delete enrollment:**
```bash
make run-delete-enrollment
# Or: mvn exec:java -Dexec.args="DELETE ENROLLMENT"
```

**ALL - Run all operations:**
```bash
make run-all-enrollment
# Or: mvn exec:java -Dexec.args="ALL ENROLLMENT"
```

### SQL Queries

**Count enrollments:**
```sql
SELECT COUNT(*) FROM enrollment;
```

**Active enrollments:**
```sql
SELECT COUNT(*) FROM enrollment WHERE is_active = true;
```

**Enrollments by relationship:**
```sql
SELECT relationship, COUNT(*) 
FROM enrollment 
GROUP BY relationship;
```

**Member with multiple plans (dual coverage):**
```sql
SELECT m.member_number, COUNT(*) as plan_count
FROM enrollment e
JOIN member m ON e.member_id = m.member_id
WHERE e.is_active = true
GROUP BY m.member_number
HAVING COUNT(*) > 1;
```

**Plan transitions:**
```sql
SELECT m.member_number, 
       p1.plan_code as old_plan,
       e1.termination_date,
       p2.plan_code as new_plan,
       e2.effective_date
FROM enrollment e1
JOIN enrollment e2 ON e1.member_id = e2.member_id
JOIN member m ON e1.member_id = m.member_id
JOIN plan p1 ON e1.plan_id = p1.plan_id
JOIN plan p2 ON e2.plan_id = p2.plan_id
WHERE e1.is_active = false 
  AND e2.is_active = true
  AND e2.effective_date > e1.termination_date
LIMIT 10;
```

---

## Performance Metrics

### Expected Performance

**Data Volume:**
- 10,000,000 enrollment records
- 20 CSV files (~30MB each)
- Total size: ~589MB

**Load Time:**
- CSV parsing: ~2-3 minutes
- Database insert: ~5-10 minutes
- Total: ~7-13 minutes

**Throughput:**
- ~15,000-30,000 records/second
- Depends on system performance and database configuration

### Performance Logging

All operations are logged to `logs/performance/enrollment_performance.log`:

```
Timestamp|Entity|Operation|Total_Time_Ms|Record_Count|Time_Per_Record_Ms|Records_Per_Sec|Total_Size_Bytes|Time_Per_KB_Ms|MB_Per_Sec|Avg_Record_Size_Bytes
2025-11-07T00:30:00Z|enrollment|INSERT_BATCH|600000|10000000|0.06|16666.67|589000000|1.02|0.98|58.9
```

### Optimization Tips

1. **Batch Size:** Default 1000 records per batch
   - Increase for better throughput
   - Decrease if memory constrained

2. **Indexes:** Ensure indexes exist before bulk insert
   - Or disable during insert, rebuild after

3. **Connection Pooling:** Implement for production
   - Current implementation creates new connection per call

4. **Parallel Processing:** Consider parallel file loading
   - Load multiple CSV files concurrently

---

## Testing

### Unit Tests

Create `EnrollmentDAOTest.java`:

```java
@Test
void testInsertBatch() {
    List<Enrollment> enrollments = createTestEnrollments();
    int inserted = dao.insertBatch(enrollments);
    assertEquals(enrollments.size(), inserted);
}

@Test
void testCountActive() {
    long activeCount = dao.countActive();
    assertTrue(activeCount > 0);
}

@Test
void testFindByMemberNumber() {
    List<Enrollment> enrollments = dao.findByMemberNumber("M000001");
    assertFalse(enrollments.isEmpty());
}
```

### Integration Tests

```bash
# Full integration test
make run-all-enrollment

# Verify data
psql -h localhost -U pbm_user -d pbm_db -c "SELECT COUNT(*) FROM enrollment;"
```

### Data Validation

```sql
-- Check for orphaned enrollments (should be 0)
SELECT COUNT(*) FROM enrollment e
LEFT JOIN member m ON e.member_id = m.member_id
WHERE m.member_id IS NULL;

-- Check for invalid dates (should be 0)
SELECT COUNT(*) FROM enrollment
WHERE termination_date IS NOT NULL 
  AND termination_date < effective_date;

-- Check active status consistency (should be 0)
SELECT COUNT(*) FROM enrollment
WHERE is_active = true 
  AND termination_date IS NOT NULL;
```

---

## Troubleshooting

### Common Issues

**Issue: Foreign key constraint violation**
```
ERROR: insert or update on table "enrollment" violates foreign key constraint
```
**Solution:** Ensure members and plans are loaded first
```bash
make run-create-plan
make run-create-member
make run-create-enrollment
```

**Issue: Out of memory during load**
```
java.lang.OutOfMemoryError: Java heap space
```
**Solution:** Increase JVM heap size
```bash
export MAVEN_OPTS="-Xmx4g"
make run-create-enrollment
```

**Issue: Slow insert performance**
```
Taking longer than expected to insert enrollments
```
**Solution:** Check database configuration
- Increase `shared_buffers` in PostgreSQL
- Disable indexes during bulk insert
- Use connection pooling

---

## Future Enhancements

1. **Coordination of Benefits (COB)**
   - Track primary vs secondary coverage
   - Calculate cost sharing across plans

2. **Enrollment History**
   - Track all enrollment changes
   - Audit trail for compliance

3. **Eligibility Verification**
   - Real-time eligibility checks
   - Integration with claims processing

4. **Enrollment Periods**
   - Open enrollment tracking
   - Special enrollment periods
   - Qualifying life events

5. **Family Coverage**
   - Link family members
   - Track dependent relationships
   - Family deductible tracking

---

## References

- [US Healthcare Enrollment Rules](https://www.healthcare.gov/)
- [Medicare Enrollment](https://www.medicare.gov/basics/get-started-with-medicare/sign-up/when-does-medicare-coverage-start)
- [HIPAA Compliance](https://www.hhs.gov/hipaa/)

---

**End of Documentation**