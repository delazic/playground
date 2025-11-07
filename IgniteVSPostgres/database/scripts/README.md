# Data Generation Scripts

This directory contains Python scripts for generating test data for the PBM system.

## Scripts

### 1. generate_formularies.py
Generates 5,000 realistic US healthcare formularies.

**Output:**
- Single CSV file: `us_pharmacy_formularies.csv`
- 5,000 formulary records
- Total size: ~1.2MB

**Market Segments:**
- Medicare Part D: 800 formularies (16%)
- Medicare Advantage: 3,000 formularies (60%)
- Commercial: 500 formularies (10%)
- Medicaid: 100 formularies (2%)
- Federal Programs: 20 formularies (0.4%)
- Regional/Specialty: 580 formularies (11.6%)

**Usage:**
```bash
cd database/scripts
python3 generate_formularies.py
```

### 2. generate_drugs.py
Generates 20,000 realistic US healthcare drug products.

**Output:**
- Single CSV file: `us_pharmacy_drugs.csv`
- 20,000 drug records
- Total size: ~2.9MB

**Drug Distribution:**
- Generic drugs: 75% (~15,000)
- Brand drugs: 23% (~4,600)
- Specialty drugs: 2% (~400)
- Controlled substances: 15% (~3,000)

**Features:**
- NDC (National Drug Code) format
- Realistic pricing (AWP, WAC, MAC)
- 14 therapeutic categories
- 10 dosage forms
- DEA schedules for controlled substances

**Usage:**
```bash
cd database/scripts
python3 generate_drugs.py
```

### 3. generate_members.py
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

### 4. generate_enrollments.py
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
2. **Formularies** (run `generate_formularies.py`) - Links to plans
3. **Drugs** (run `generate_drugs.py`) - Independent, can run anytime
4. **Members** (run `generate_members.py`)
5. **Enrollments** (run `generate_enrollments.py`) - Links to members and plans

**Quick Start:**
```bash
cd database/scripts

# Generate all data in correct order
python3 generate_formularies.py
python3 generate_drugs.py
python3 generate_members.py
python3 generate_enrollments.py
```

## Configuration

### generate_formularies.py Configuration

Edit the script to customize:

```python
# Total formularies to generate
TOTAL_FORMULARIES = 5000

# Market segment distribution
MEDICARE_PART_D = 800        # 16%
MEDICARE_ADVANTAGE = 3000    # 60%
COMMERCIAL = 500             # 10%
MEDICAID = 100               # 2%
FEDERAL = 20                 # 0.4%
REGIONAL_SPECIALTY = 580     # 11.6%

# Formulary type distribution
FORMULARY_TYPES = {
    'STANDARD': 0.40,      # 40%
    'ENHANCED': 0.25,      # 25%
    'BASIC': 0.20,         # 20%
    'SPECIALTY': 0.10,     # 10%
    'MAIL_ORDER': 0.05     # 5%
}
```

### generate_drugs.py Configuration

Edit the script to customize:

```python
# Total drugs to generate
TOTAL_DRUGS = 20000

# Drug type distribution
GENERIC_PCT = 0.75        # 75% generic drugs
BRAND_PCT = 0.23          # 23% brand drugs
SPECIALTY_PCT = 0.02      # 2% specialty drugs

# Controlled substance distribution
CONTROLLED_PCT = 0.15     # 15% are controlled substances
DEA_SCHEDULES = {
    'II': 0.30,   # 30% - High potential for abuse
    'III': 0.25,  # 25% - Moderate potential
    'IV': 0.35,   # 35% - Low potential
    'V': 0.10     # 10% - Lowest potential
}
```

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
│   ├── us_pharmacy_formularies.csv
│   ├── us_pharmacy_drugs.csv
│   ├── us_pharmacy_members_01.csv
│   ├── ...
│   ├── us_pharmacy_members_10.csv
│   ├── us_pharmacy_enrollments_01.csv
│   ├── ...
│   ├── us_pharmacy_enrollments_20.csv
│   ├── PLAN_DATA.md                          # Plans documentation
│   ├── FORMULARY_DATA.md                     # Formularies documentation
│   ├── DRUG_DATA.md                          # Drugs documentation
│   ├── MEMBER_DATA.md                        # Members documentation
│   └── ENROLLMENT_DATA.md                    # Enrollments documentation
└── scripts/
    ├── generate_formularies.py
    ├── generate_drugs.py
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

### Error: "No plan CSV files found"

**Problem:** `generate_formularies.py` cannot find plan files.

**Solution:** Ensure `us_pharmacy_plans.csv` exists in `../data/` directory. The script will use default plan codes if not found.

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

**Problem:** Script takes a long time to generate data.

**Solution:** This is normal. Expected time:
- Formulary generation: ~10-30 seconds
- Drug generation: ~10-20 seconds
- Member generation: ~2-3 minutes
- Enrollment generation: ~5-10 minutes

Progress indicators are shown during generation.

## Data Validation

After generation, verify the data:

```bash
# Check formularies
ls -l ../data/us_pharmacy_formularies.csv
wc -l ../data/us_pharmacy_formularies.csv
# Should show 5,001 (5,000 + 1 header)

# Check drugs
ls -l ../data/us_pharmacy_drugs.csv
wc -l ../data/us_pharmacy_drugs.csv
# Should show 20,001 (20,000 + 1 header)

# Check members
ls -l ../data/us_pharmacy_members_*.csv | wc -l
# Should show 10

# Check enrollments
ls -l ../data/us_pharmacy_enrollments_*.csv | wc -l
# Should show 20

# Check file sizes
du -h ../data/us_pharmacy_*.csv

# Count total records
wc -l ../data/us_pharmacy_formularies.csv      # 5,001
wc -l ../data/us_pharmacy_drugs.csv            # 20,001
wc -l ../data/us_pharmacy_members_*.csv        # ~1,000,010
wc -l ../data/us_pharmacy_enrollments_*.csv    # ~10,000,020
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