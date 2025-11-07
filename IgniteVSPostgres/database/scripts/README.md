# Data Generation Scripts

This directory contains Python scripts for generating test data for the PBM system.

## Scripts

### 1. generate_members.py
Generates 1,000,000 realistic US member records.

**Output:**
- 10 CSV files: `us_pharmacy_members_01.csv` through `us_pharmacy_members_10.csv`
- 100,000 members per file
- Total size: ~150MB

**Usage:**
```bash
cd database/scripts
python3 generate_members.py
```

### 2. generate_enrollments.py
Generates 10,000,000 realistic US healthcare enrollment records.

**Prerequisites:**
- Member CSV files must exist (run `generate_members.py` first)
- Plan CSV file must exist (`us_pharmacy_plans.csv`)

**Output:**
- 20 CSV files: `us_pharmacy_enrollments_01.csv` through `us_pharmacy_enrollments_20.csv`
- ~500,000 enrollments per file (~30MB each)
- Total size: ~589MB

**Enrollment Scenarios:**
- Single active coverage: 70%
- Dual coverage: 15%
- Plan transitions: 10%
- Historical enrollments: 5%

**Usage:**
```bash
cd database/scripts
python3 generate_enrollments.py
```

**Make executable (optional):**
```bash
chmod +x generate_enrollments.py
./generate_enrollments.py
```

## Data Generation Order

**IMPORTANT:** Generate data in this order to satisfy dependencies:

1. **Plans** (already exists as `us_pharmacy_plans.csv`)
2. **Members** (run `generate_members.py`)
3. **Enrollments** (run `generate_enrollments.py`)

## Configuration

### generate_enrollments.py Configuration

Edit the script to customize:

```python
# Total enrollments to generate
TOTAL_ENROLLMENTS = 10_000_000

# Enrollments per file
ENROLLMENTS_PER_FILE = 500_000

# Enrollment scenario percentages
SINGLE_ACTIVE = 0.70      # 70%
DUAL_COVERAGE = 0.15      # 15%
PLAN_TRANSITION = 0.10    # 10%
HISTORICAL_ONLY = 0.05    # 5%
```

## Output Location

All generated CSV files are written to: `../data/`

Relative to scripts directory:
```
database/
├── data/
│   ├── us_pharmacy_plans.csv
│   ├── us_pharmacy_members_01.csv
│   ├── ...
│   ├── us_pharmacy_members_10.csv
│   ├── us_pharmacy_enrollments_01.csv
│   ├── ...
│   └── us_pharmacy_enrollments_20.csv
└── scripts/
    ├── generate_members.py
    ├── generate_enrollments.py
    └── README.md
```

## Requirements

**Python 3.7+** with standard library (no external dependencies required)

The scripts use only built-in Python modules:
- `csv` - CSV file handling
- `random` - Random data generation
- `datetime` - Date handling
- `pathlib` - File path operations
- `glob` - File pattern matching

## Troubleshooting

### Error: "No member CSV files found"

**Problem:** `generate_enrollments.py` cannot find member files.

**Solution:** Run `generate_members.py` first:
```bash
python3 generate_members.py
```

### Error: "Permission denied"

**Problem:** Script is not executable.

**Solution:** Either:
1. Run with `python3 generate_enrollments.py`
2. Or make executable: `chmod +x generate_enrollments.py`

### Slow generation

**Problem:** Script takes a long time to generate 10M records.

**Solution:** This is normal. Expected time:
- Member generation: ~2-3 minutes
- Enrollment generation: ~5-10 minutes

Progress indicators are shown every 50,000 members processed.

## Data Validation

After generation, verify the data:

```bash
# Count files
ls -l ../data/us_pharmacy_enrollments_*.csv | wc -l
# Should show 20

# Check file sizes
du -h ../data/us_pharmacy_enrollments_*.csv

# Count total lines (excluding headers)
wc -l ../data/us_pharmacy_enrollments_*.csv
# Should show ~10,000,020 (10M + 20 headers)
```

## US Healthcare Enrollment Rules

The enrollment generator implements realistic US healthcare scenarios:

### 1. Single Active Coverage (70%)
Most common scenario - member has one active plan.
```csv
M000001,COMM-GOLD-001,GRP-12345,2024-01-01,,SELF,true
```

### 2. Dual Coverage (15%)
Member has two active plans (e.g., Medicare + Medigap).
```csv
M000002,MCARE-PARTD-001,GRP-67890,2024-01-01,,SELF,true
M000002,MCARE-MEDIGAP-001,GRP-67890,2024-01-01,,SELF,true
```

### 3. Plan Transition (10%)
Member changed plans during the year.
```csv
M000003,COMM-SILVER-002,GRP-11111,2023-01-01,2024-03-31,SELF,false
M000003,COMM-GOLD-001,GRP-11111,2024-04-01,,SELF,true
```

### 4. Historical Only (5%)
Member has no active enrollment.
```csv
M000004,COMM-BRONZE-003,GRP-22222,2023-01-01,2023-12-31,SELF,false
```

## Plan Codes

The script uses 29 plan codes matching `us_pharmacy_plans.csv`:

- **Commercial:** COMM-PLATINUM-001, COMM-GOLD-001, etc.
- **Medicare:** MCARE-PARTD-001, MCARE-MAPD-001, etc.
- **Medicaid:** MCAID-STD-001, MCAID-EXP-001
- **Exchange:** EXCH-SILVER-001, EXCH-GOLD-001
- **Employer:** EMP-TRAD-001, EMP-HDHP-001, EMP-PREM-001
- **Union:** UNION-STD-001, UNION-PREM-001
- **Government:** GOV-VA-001, GOV-TRICARE-001, GOV-FEHB-001
- **CHIP:** CHIP-STD-001
- **Individual:** IND-CATA-001, IND-BRONZE-001, etc.

## Relationship Types

- **SELF:** Primary member (60%)
- **SPOUSE:** Spouse of primary member (25%)
- **DEPENDENT:** Child or other dependent (15%)

## Future Enhancements

Potential improvements to the generator:

1. **Family Grouping:** Link family members together
2. **Seasonal Patterns:** Model open enrollment periods
3. **Geographic Distribution:** Align with member locations
4. **Age-Based Plans:** Match plan types to member age (e.g., Medicare for 65+)
5. **Employer Groups:** Create realistic employer group structures

---

**Last Updated:** 2025-11-07