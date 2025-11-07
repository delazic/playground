# Enrollment Data Documentation

## Overview

This dataset contains **10,000,000** synthetic enrollment records for the PBM system following US healthcare enrollment rules.

## File Information

- **Location**: `IgniteVSPostgres/src/main/resources/data/`
- **File Pattern**: `us_pharmacy_enrollments_01.csv` through `us_pharmacy_enrollments_20.csv`
- **Total Files**: 20 files
- **Records per File**: ~500,000 enrollments
- **Total Records**: 10,000,000 enrollments
- **Format**: CSV (Comma-Separated Values)
- **Encoding**: UTF-8

**Note:** CSV files are stored in the Java application's resources directory (`src/main/resources/data/`) and loaded via classpath using `getResourceAsStream()`. This ensures portability and proper packaging in JAR files.

## Prerequisites (for regeneration)

1. **Member data must exist first**
   - Member CSV files in `src/main/resources/data/`
   - The script will load member IDs from these files

2. **Plan data must exist**
   - Ensure `us_pharmacy_plans.csv` exists in `src/main/resources/data/`
   - The script will load plan codes from this file

3. **Python 3.7+**
   - No external dependencies required (uses only standard library)

## US Healthcare Enrollment Rules Implemented

### Enrollment Scenarios (Realistic Distribution)

1. **Single Active Plan (70%)**
   - Most common scenario
   - One active enrollment per member
   - Typical for employer-sponsored or individual plans

2. **Dual Coverage (15%)**
   - Two active plans simultaneously
   - Primary + Secondary coverage
   - Common scenarios:
     - Covered under both parents' plans (children)
     - Spouse's plan as secondary
     - Medicare + Medicare Supplement
     - Medicare Part D + Employer coverage

3. **Plan Transition (10%)**
   - Member switching between plans
   - Old plan terminated, new plan active
   - Common during:
     - Job changes
     - Open enrollment periods
     - Life events (marriage, divorce)

4. **Historical Only (5%)**
   - No active enrollments
   - 1-3 historical enrollments
   - Represents:
     - Inactive members
     - Deceased members
     - Members who left the system

### Relationship Types

- **SELF (60%)** - Primary member
- **SPOUSE (20%)** - Covered spouse
- **CHILD (15%)** - Dependent child
- **DEPENDENT (5%)** - Other dependent

### Data Quality Rules

✅ **Every member used at least once** - Ensures all members have enrollment history  
✅ **Realistic date ranges** - 2020-2025 for historical, 2023-2024 for active  
✅ **Valid group numbers** - Format: GRP######  
✅ **Proper termination dates** - Always after effective dates  
✅ **Active flag consistency** - Matches presence/absence of termination date  

## Usage

### Generate Enrollment Data

```bash
cd IgniteVSPostgres/database/data
python3 generate_enrollments.py
```

### Expected Output

```
================================================================================
PBM Enrollment Data Generator
================================================================================

Loading member IDs from 10 files...
Loaded 1,000,000 member IDs
Loaded 50 plan codes

Generating 10,000,000 enrollment records...
Target file size: ~15MB per file

Writing to: enrollments_001.csv
  Progress: 100,000 / 10,000,000 (1.0%) - File 1, Size: 2.3MB
  Progress: 200,000 / 10,000,000 (2.0%) - File 1, Size: 4.6MB
  ...
Writing to: enrollments_002.csv
  ...

================================================================================
Generation Complete!
================================================================================
Total enrollment records: 10,000,000
Total files created: 45
Unique members used: 1,000,000 / 1,000,000
Coverage: 100.0%

Enrollment Scenarios Distribution:
  single_active       : ~7,000,000 (70%)
  dual_coverage       : ~1,500,000 (15%)
  plan_transition     : ~1,000,000 (10%)
  historical_only     : ~500,000 (5%)
```

### Output Files

Files will be created in the current directory:
- `enrollments_001.csv` (~15MB)
- `enrollments_002.csv` (~15MB)
- `enrollments_003.csv` (~15MB)
- ... (approximately 45 files total)

## CSV File Format

```csv
member_number,plan_code,group_number,effective_date,termination_date,relationship,is_active
M0000001,PLAN001,GRP123456,2024-01-15,,SELF,true
M0000002,PLAN023,GRP234567,2023-06-01,2024-05-31,SPOUSE,false
M0000002,PLAN045,GRP345678,2024-06-01,,SPOUSE,true
```

### Field Descriptions

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| member_number | VARCHAR(50) | Member identifier | M0000001 |
| plan_code | VARCHAR(50) | Plan identifier | PLAN001 |
| group_number | VARCHAR(50) | Group/employer identifier | GRP123456 |
| effective_date | DATE | Enrollment start date | 2024-01-15 |
| termination_date | DATE | Enrollment end date (empty if active) | 2024-12-31 or empty |
| relationship | VARCHAR(20) | Member relationship | SELF, SPOUSE, CHILD, DEPENDENT |
| is_active | BOOLEAN | Current enrollment status | true, false |

## Performance

- **Generation Speed**: ~50,000-100,000 records/second
- **Total Time**: ~2-3 minutes for 10 million records
- **Memory Usage**: Low (streaming write, no buffering)
- **Disk Space**: ~450MB total (45 files × ~10MB each)

## Validation

After generation, verify:

```bash
# Count total records
wc -l enrollments_*.csv

# Check file sizes
ls -lh enrollments_*.csv

# Verify CSV format
head -n 5 enrollments_001.csv

# Check for unique members
cut -d',' -f1 enrollments_*.csv | sort -u | wc -l
```

## Loading into Database

**Using Java Application (Recommended):**
```bash
# Load all enrollments using the Java application
make run-create-enrollment
```

The Java application loads CSV files from the classpath (`src/main/resources/data/`) using the `EnrollmentConverter` class, which reads files via `getResourceAsStream()`.

**Implementation Details:**
- **Converter**: `EnrollmentConverter.java` - Loads 20 CSV files from classpath
- **DAO**: `EnrollmentDAO.java` - Handles database batch operations
- **Method**: `getResourceAsStream()` - Reads files from `src/main/resources/data/`
- **Files**: Automatically loads `us_pharmacy_enrollments_01.csv` through `us_pharmacy_enrollments_20.csv`

## Troubleshooting

### Error: "No member CSV files found"
**Solution**: Generate member data first using the member data generator

### Error: "us_pharmacy_plans.csv not found"
**Solution**: Ensure the plan CSV file exists in the same directory

### Files too large/small
**Solution**: Adjust `MAX_FILE_SIZE_MB` constant in the script (default: 15MB)

### Need more/fewer records
**Solution**: Adjust `TOTAL_RECORDS` constant in the script (default: 10,000,000)

## Customization

Edit the script to adjust:

```python
# Total records to generate
TOTAL_RECORDS = 10_000_000

# Maximum file size in MB
MAX_FILE_SIZE_MB = 15

# Scenario probabilities (must sum to 1.0)
SCENARIOS = {
    'single_active': 0.70,
    'dual_coverage': 0.15,
    'plan_transition': 0.10,
    'historical_only': 0.05
}

# Relationship probabilities (must sum to 1.0)
RELATIONSHIPS = {
    'SELF': 0.60,
    'SPOUSE': 0.20,
    'CHILD': 0.15,
    'DEPENDENT': 0.05
}
```

## Next Steps

1. ✅ Generate enrollment data
2. Create `Enrollment.java` model class
3. Create `EnrollmentConverter.java` to load CSV files
4. Create `EnrollmentDAO.java` for database operations
5. Add enrollment CRUD operations to `App.java`
6. Update Makefile with enrollment targets

---

**Note**: This generator creates realistic test data following US healthcare enrollment patterns. For production use, always use actual enrollment data from your systems.