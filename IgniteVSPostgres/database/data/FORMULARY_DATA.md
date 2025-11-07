# US Healthcare Formularies - Synthetic Data Documentation

**Last Updated:** 2025-11-07  
**Data File:** `us_pharmacy_formularies.csv`  
**Total Records:** 5,000 formularies  
**Generator Script:** `../scripts/generate_formularies.py`

---

## Table of Contents

1. [Overview](#overview)
2. [US Healthcare Formulary System](#us-healthcare-formulary-system)
3. [Data Generation Rules](#data-generation-rules)
4. [Market Segment Distribution](#market-segment-distribution)
5. [Formulary Characteristics](#formulary-characteristics)
6. [Data Schema](#data-schema)
7. [Generation Process](#generation-process)
8. [Usage Examples](#usage-examples)
9. [Validation Rules](#validation-rules)

---

## Overview

This document describes the synthetic formulary data generated for the PBM (Pharmacy Benefit Management) system. The data represents 5,000 realistic US healthcare formularies covering all major market segments and following industry standards.

### Key Statistics

- **Total Formularies:** 5,000
- **Market Segments:** 6 (Medicare Part D, Medicare Advantage, Commercial, Medicaid, Federal, Regional)
- **Insurance Carriers:** 30+ major US health insurers
- **PBMs (Pharmacy Benefit Managers):** 10 major PBMs
- **Coverage Period:** 2024 (annual formularies)
- **File Size:** ~1.2 MB

---

## US Healthcare Formulary System

### What is a Formulary?

A **formulary** is a list of prescription drugs covered by a health insurance plan. It defines:

1. **Which drugs are covered** (included vs excluded)
2. **Drug tiers** (Generic, Preferred Brand, Non-Preferred, Specialty)
3. **Cost-sharing** (copays or coinsurance per tier)
4. **Restrictions** (prior authorization, step therapy, quantity limits)

### Formulary-Plan Relationship

```
Plan (1) ←→ (1..N) Formulary
```

- Each plan has **one or more formularies** (typically one per year)
- Each formulary belongs to **exactly one plan**
- Formularies are updated annually (especially Medicare)

### Real-World Context

In the US healthcare system:
- **~5,000-7,000 unique formularies** exist across all market segments
- **Top 3 PBMs** (CVS Caremark, Express Scripts, OptumRx) manage ~80% of prescriptions
- **Medicare formularies** must be approved by CMS (Centers for Medicare & Medicaid Services)
- **Commercial formularies** vary widely by employer and carrier
- **Medicaid formularies** are state-specific with federal guidelines

---

## Data Generation Rules

### 1. Market Segment Distribution

The 5,000 formularies are distributed across market segments based on real-world proportions:

| Market Segment | Count | Percentage | Rationale |
|----------------|-------|------------|-----------|
| Medicare Part D | 800 | 16% | ~750+ standalone Part D plans nationwide |
| Medicare Advantage | 3,000 | 60% | ~3,500+ MAPD plans (largest segment) |
| Commercial | 500 | 10% | Major carriers with standard formularies |
| Medicaid | 100 | 2% | 50 states × 2 formularies average |
| Federal Programs | 20 | 0.4% | VA, TRICARE, FEHB programs |
| Regional/Specialty | 580 | 11.6% | Regional carriers and specialty plans |
| **TOTAL** | **5,000** | **100%** | |

### 2. Formulary Type Distribution

Each formulary is assigned a type based on coverage breadth:

| Type | Percentage | Description | Drug Count Range |
|------|------------|-------------|------------------|
| STANDARD | 40% | Standard coverage, most common | 2,000-4,000 drugs |
| ENHANCED | 25% | Premium coverage, broader drug list | 2,500-4,000 drugs |
| BASIC | 20% | Value/limited coverage | 1,000-2,500 drugs |
| SPECIALTY | 10% | Focus on high-cost specialty drugs | 500-1,500 drugs |
| MAIL_ORDER | 5% | Mail order pharmacy specific | 2,000-3,500 drugs |

### 3. Tier Structure Distribution

Formularies use different tier structures for cost-sharing:

| Tier Count | Percentage | Description | Example |
|------------|------------|-------------|---------|
| 3-Tier | 15% | Simple structure | Generic, Preferred, Non-Preferred |
| 4-Tier | 35% | Standard structure | + Specialty tier |
| 5-Tier | 40% | Enhanced structure | + High-cost Specialty |
| 6-Tier | 10% | Complex structure | + Biologics tier |

**Tier Definitions:**
- **Tier 1:** Generic drugs (lowest cost)
- **Tier 2:** Preferred brand drugs
- **Tier 3:** Non-preferred brand drugs
- **Tier 4:** Specialty drugs (high-cost)
- **Tier 5:** High-cost specialty (biologics, rare disease)
- **Tier 6:** Ultra-specialty (gene therapy, CAR-T)

### 4. Coverage Level Distribution

| Coverage Level | Description | Typical Use |
|----------------|-------------|-------------|
| COMPREHENSIVE | Broad drug coverage, low restrictions | Premium commercial plans |
| STANDARD | Typical coverage, moderate restrictions | Most Medicare and commercial plans |
| BASIC | Limited coverage, more restrictions | Value plans, HDHPs |
| LIMITED | Minimal coverage, high restrictions | Catastrophic plans |

### 5. Restriction Percentages

Each formulary includes realistic restriction percentages:

| Restriction Type | Range | Average | Purpose |
|------------------|-------|---------|---------|
| Prior Authorization | 5-25% | 15% | Requires approval before dispensing |
| Step Therapy | 3-15% | 9% | Must try cheaper alternatives first |
| Quantity Limits | 10-30% | 20% | Maximum quantity per fill |

---

## Market Segment Distribution

### Medicare Part D (800 formularies)

**Characteristics:**
- Standalone prescription drug plans
- Must meet CMS minimum coverage requirements
- Annual formulary updates required
- Typically 4-5 tier structures

**Carriers:**
- Humana, WellCare, Aetna, Cigna, UnitedHealthcare
- CVS Health, Anthem, Kaiser, BCBS, Centene

**Example:**
```
Formulary: FORM-MEDI-HUMA-2024-0001
Name: HUMANA MEDICARE_PART_D Standard 4-Tier Formulary 2024
Plan: MCARE-PARTD-001
Tiers: 4
Drug Count: 2,500-3,500
```

### Medicare Advantage (3,000 formularies)

**Characteristics:**
- Medicare Advantage plans with drug coverage (MAPD)
- Largest market segment
- Integrated medical and drug benefits
- Regional variations

**Carriers:**
- Humana, UnitedHealthcare, Anthem, CVS Health, Aetna
- Kaiser, Centene, Cigna, BCBS, WellCare, Molina

**Example:**
```
Formulary: FORM-MEDI-UNIT-2024-1500
Name: UNITED_HEALTHCARE MEDICARE_ADVANTAGE Enhanced 5-Tier Formulary 2024
Plan: MCARE-MAPD-015
Tiers: 5
Drug Count: 3,000-4,000
```

### Commercial Insurance (500 formularies)

**Characteristics:**
- Employer-sponsored and individual market plans
- Wide variation in coverage and cost-sharing
- Often managed by major PBMs
- 3-6 tier structures

**Carriers:**
- UnitedHealthcare, Anthem, Aetna, Cigna, Humana
- BCBS, Kaiser, Centene, Molina, Health Net

**Example:**
```
Formulary: FORM-COMM-ANTH-2024-0250
Name: ANTHEM COMMERCIAL Standard 4-Tier Formulary 2024
Plan: COMM-GOLD-001
Tiers: 4
Drug Count: 2,500-3,500
```

### Medicaid (100 formularies)

**Characteristics:**
- State-specific formularies
- Must cover all FDA-approved drugs (with some exceptions)
- Managed Care Organizations (MCOs) may have variations
- Lower cost-sharing than commercial plans

**Carriers:**
- Centene, Molina, Anthem, UnitedHealthcare, Aetna
- WellCare, Health Net, Amerigroup, BCBS

**Example:**
```
Formulary: FORM-MEDI-CENT-2024-0050
Name: CENTENE MEDICAID Standard 3-Tier Formulary 2024
Plan: MCAID-CA-001
Region: CA
Tiers: 3
Drug Count: 3,000-4,000
```

### Federal Programs (20 formularies)

**Characteristics:**
- VA (Veterans Affairs): National formulary
- TRICARE: Military health system
- FEHB (Federal Employees): Multiple carriers

**Programs:**
- VA, TRICARE, FEHB_BCBS, FEHB_AETNA, FEHB_KAISER
- FEHB_UNITED, FEHB_GEHA

**Example:**
```
Formulary: FORM-FEDE-VA-2024-0001
Name: VA FEDERAL Standard 4-Tier Formulary 2024
Plan: GOV-VA-001
Region: NATIONAL
Tiers: 4
Drug Count: 2,000-3,000
```

### Regional/Specialty (580 formularies)

**Characteristics:**
- Regional carriers (state or multi-state)
- Specialty plans (HIV, oncology, rare disease)
- Smaller market share but important coverage

**Example:**
```
Formulary: FORM-REGI-BCBS-2024-0300
Name: BCBS REGIONAL Specialty 5-Tier Formulary 2024
Plan: COMM-SILVER-050
Region: TX
Tiers: 5
Drug Count: 1,500-2,500
```

---

## Formulary Characteristics

### PBM (Pharmacy Benefit Manager) Distribution

The 10 major PBMs are distributed across formularies:

| PBM | Market Share | Description |
|-----|--------------|-------------|
| CVS_CAREMARK | ~25% | Largest PBM, integrated with CVS Health |
| EXPRESS_SCRIPTS | ~20% | Cigna subsidiary, second largest |
| OPTUM_RX | ~20% | UnitedHealth subsidiary |
| HUMANA_PHARMACY | ~10% | Integrated with Humana plans |
| PRIME_THERAPEUTICS | ~8% | Non-profit, BCBS affiliated |
| MAGELLAN_RX | ~5% | Specialty and behavioral health focus |
| ENVOLVE_PHARMACY | ~4% | Centene subsidiary |
| MEDIMPACT | ~3% | Independent PBM |
| NAVITUS | ~3% | Non-profit PBM |
| ELIXIR | ~2% | Regional PBM |

### Drug Count Distribution

Number of drugs in each formulary varies by type:

| Formulary Type | Min Drugs | Max Drugs | Average |
|----------------|-----------|-----------|---------|
| SPECIALTY | 500 | 1,500 | 1,000 |
| BASIC | 1,000 | 2,500 | 1,750 |
| STANDARD | 2,000 | 4,000 | 3,000 |
| ENHANCED | 2,500 | 4,000 | 3,250 |
| MAIL_ORDER | 2,000 | 3,500 | 2,750 |

### Active Status

- **95% Active:** Current formularies in use (2024)
- **5% Inactive:** Historical formularies (2023 or earlier)

---

## Data Schema

### CSV File Structure

```csv
formulary_code,formulary_name,plan_code,market_segment,carrier,pbm,formulary_type,tier_count,coverage_level,effective_date,termination_date,region,drug_count,prior_auth_pct,step_therapy_pct,quantity_limit_pct,is_active
```

### Field Definitions

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `formulary_code` | VARCHAR(50) | Unique formulary identifier | FORM-MEDI-HUMA-2024-0001 |
| `formulary_name` | VARCHAR(200) | Descriptive formulary name | HUMANA MEDICARE_PART_D Standard 4-Tier Formulary 2024 |
| `plan_code` | VARCHAR(50) | Associated plan code (FK) | MCARE-PARTD-001 |
| `market_segment` | VARCHAR(50) | Market segment category | MEDICARE_PART_D |
| `carrier` | VARCHAR(50) | Insurance carrier | HUMANA |
| `pbm` | VARCHAR(50) | Pharmacy Benefit Manager | CVS_CAREMARK |
| `formulary_type` | VARCHAR(20) | Type of formulary | STANDARD |
| `tier_count` | INTEGER | Number of drug tiers | 4 |
| `coverage_level` | VARCHAR(20) | Coverage breadth | COMPREHENSIVE |
| `effective_date` | DATE | Start date | 2024-01-01 |
| `termination_date` | DATE | End date | 2024-12-31 |
| `region` | VARCHAR(10) | Geographic region | NATIONAL or state code |
| `drug_count` | INTEGER | Approximate drugs covered | 2,500 |
| `prior_auth_pct` | INTEGER | % requiring prior auth | 15 |
| `step_therapy_pct` | INTEGER | % requiring step therapy | 9 |
| `quantity_limit_pct` | INTEGER | % with quantity limits | 20 |
| `is_active` | BOOLEAN | Current active status | true |

---

## Generation Process

### Step-by-Step Process

1. **Load Plan Codes**
   - Reads from `us_pharmacy_plans.csv`
   - Falls back to generated plan codes if file not found
   - Ensures formularies link to valid plans

2. **Generate by Market Segment**
   - Iterates through 6 market segments
   - Generates specified count for each segment
   - Applies segment-specific rules

3. **Assign Characteristics**
   - Selects carrier from segment-appropriate list
   - Assigns PBM (weighted random selection)
   - Determines formulary type (weighted distribution)
   - Sets tier count (weighted distribution)
   - Assigns coverage level

4. **Generate Identifiers**
   - Creates unique formulary code
   - Generates descriptive name
   - Links to plan code

5. **Set Dates and Restrictions**
   - Sets effective date (2024-01-01)
   - Sets termination date (2024-12-31)
   - Assigns region (state or NATIONAL)
   - Calculates drug count based on type
   - Sets restriction percentages

6. **Determine Active Status**
   - 95% marked as active
   - 5% marked as inactive (historical)

7. **Write to CSV**
   - Writes all 5,000 formularies to single file
   - Includes header row
   - Validates data integrity

### Script Usage

```bash
cd database/scripts

# Run the generator
python3 generate_formularies.py

# Output
# - Creates: ../data/us_pharmacy_formularies.csv
# - Size: ~1.2 MB
# - Records: 5,000 formularies
```

---

## Usage Examples

### Example 1: Medicare Part D Formulary

```csv
FORM-MEDI-HUMA-2024-0001,HUMANA MEDICARE_PART_D Standard 4-Tier Formulary 2024,MCARE-PARTD-001,MEDICARE_PART_D,HUMANA,CVS_CAREMARK,STANDARD,4,STANDARD,2024-01-01,2024-12-31,NATIONAL,2847,12,7,18,true
```

**Interpretation:**
- Humana Medicare Part D plan
- Standard 4-tier formulary
- Managed by CVS Caremark
- Covers ~2,847 drugs
- 12% require prior authorization
- 7% require step therapy
- 18% have quantity limits
- Currently active

### Example 2: Commercial Enhanced Formulary

```csv
FORM-COMM-ANTH-2024-0250,ANTHEM COMMERCIAL Enhanced 5-Tier Formulary 2024,COMM-GOLD-001,COMMERCIAL,ANTHEM,EXPRESS_SCRIPTS,ENHANCED,5,COMPREHENSIVE,2024-01-01,2024-12-31,NATIONAL,3456,8,5,15,true
```

**Interpretation:**
- Anthem commercial plan
- Enhanced 5-tier formulary (broader coverage)
- Managed by Express Scripts
- Covers ~3,456 drugs
- Lower restrictions (8% prior auth, 5% step therapy)
- Comprehensive coverage level

### Example 3: Medicaid State Formulary

```csv
FORM-MEDI-CENT-2024-0050,CENTENE MEDICAID Standard 3-Tier Formulary 2024,MCAID-CA-001,MEDICAID,CENTENE,ENVOLVE_PHARMACY,STANDARD,3,COMPREHENSIVE,2024-01-01,2024-12-31,CA,3789,10,6,12,true
```

**Interpretation:**
- California Medicaid (Medi-Cal)
- Managed by Centene
- Simple 3-tier structure
- Covers ~3,789 drugs (broad Medicaid coverage)
- Regional to California

---

## Validation Rules

### Data Integrity Checks

1. **Unique Formulary Codes**
   - Each formulary_code must be unique
   - Format: `FORM-{SEGMENT}-{CARRIER}-{YEAR}-{SEQUENCE}`

2. **Valid Plan References**
   - Each plan_code should exist in plans table
   - Foreign key relationship enforced

3. **Date Consistency**
   - effective_date must be before termination_date
   - Most formularies are annual (Jan 1 - Dec 31)

4. **Tier Count Range**
   - Must be between 3 and 6
   - Most common: 4 or 5 tiers

5. **Drug Count Reasonableness**
   - Minimum: 500 drugs (specialty formularies)
   - Maximum: 4,000 drugs (comprehensive formularies)
   - Average: ~2,500-3,000 drugs

6. **Restriction Percentages**
   - prior_auth_pct: 5-25%
   - step_therapy_pct: 3-15%
   - quantity_limit_pct: 10-30%

7. **Market Segment Distribution**
   - Total must equal 5,000
   - Proportions should match configuration

### SQL Validation Queries

```sql
-- Check total count
SELECT COUNT(*) FROM formulary;
-- Expected: 5,000

-- Check for duplicates
SELECT formulary_code, COUNT(*) 
FROM formulary 
GROUP BY formulary_code 
HAVING COUNT(*) > 1;
-- Expected: 0 rows

-- Check market segment distribution
SELECT market_segment, COUNT(*) as count,
       ROUND(COUNT(*) * 100.0 / 5000, 1) as percentage
FROM formulary
GROUP BY market_segment
ORDER BY count DESC;

-- Check active vs inactive
SELECT is_active, COUNT(*) as count,
       ROUND(COUNT(*) * 100.0 / 5000, 1) as percentage
FROM formulary
GROUP BY is_active;
-- Expected: ~95% active, ~5% inactive

-- Check tier distribution
SELECT tier_count, COUNT(*) as count
FROM formulary
GROUP BY tier_count
ORDER BY tier_count;

-- Check for orphaned formularies (no matching plan)
SELECT COUNT(*) 
FROM formulary f
LEFT JOIN plan p ON f.plan_code = p.plan_code
WHERE p.plan_code IS NULL;
-- Expected: 0 (all formularies should have valid plans)
```

---

## Future Enhancements

### Potential Improvements

1. **Formulary-Drug Relationships**
   - Generate `formulary_drug` table linking formularies to specific drugs
   - Include tier assignments for each drug
   - Add restriction flags (prior auth, step therapy, quantity limits)

2. **Historical Versioning**
   - Generate multiple years of formularies (2022, 2023, 2024)
   - Track formulary changes over time
   - Model drug additions/removals

3. **Regional Variations**
   - More detailed regional formularies
   - State-specific Medicaid variations
   - Multi-state regional carriers

4. **Specialty Formularies**
   - Dedicated HIV/AIDS formularies
   - Oncology-specific formularies
   - Rare disease formularies

5. **Cost-Sharing Details**
   - Add copay amounts per tier
   - Include coinsurance percentages
   - Model deductible structures

---

## References

### Industry Standards

- **CMS Medicare Part D Formulary Requirements**: https://www.cms.gov/Medicare/Prescription-Drug-Coverage/PrescriptionDrugCovContra
- **NCPDP Formulary Standards**: https://www.ncpdp.org/
- **Academy of Managed Care Pharmacy (AMCP)**: https://www.amcp.org/

### Data Sources

- Medicare Plan Finder: https://www.medicare.gov/plan-compare/
- State Medicaid Formularies: Various state health department websites
- PBM Market Share Reports: Industry publications

---

**Document Version:** 1.0  
**Last Updated:** 2025-11-07  
**Maintained By:** PBM System Development Team