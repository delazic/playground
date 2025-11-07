# Formulary-Drug Relationship Data Documentation

## Overview

This document describes the formulary-drug relationship data generation process, business rules, and data characteristics for the US Healthcare system simulation.

## Data Generation Summary

- **Total Relationships**: 10,000,000 (10 million)
- **Formularies**: 5,000 unique formularies
- **Average Drugs per Formulary**: ~2,000 drugs
- **Output Files**: 64 CSV files
- **File Naming**: `us_pharmacy_formularies_drugs_01.csv` through `us_pharmacy_formularies_drugs_64.csv`
- **File Size**: ~15.6 MB per file (targeting 30 MB)
- **Location**: `IgniteVSPostgres/src/main/resources/data/`

## File Structure

### CSV Schema

Each CSV file contains the following columns:

| Column | Type | Description |
|--------|------|-------------|
| `formulary_drug_id` | UUID | Unique identifier for the formulary-drug relationship |
| `formulary_code` | String | Foreign key to formularies table |
| `ndc_code` | String | National Drug Code - foreign key to drugs table |
| `tier` | Integer (1-5) | Drug tier assignment (1=lowest cost, 5=highest cost) |
| `status` | String | Drug status: PREFERRED, NON_PREFERRED, or SPECIALTY |
| `requires_prior_auth` | Boolean | Whether prior authorization is required |
| `requires_step_therapy` | Boolean | Whether step therapy is required |
| `quantity_limit` | Integer | Maximum quantity allowed (if applicable) |
| `days_supply_limit` | Integer | Maximum days supply allowed (if applicable) |

## Business Rules

### 1. Drug Selection Rules

#### Target Drugs per Formulary Type

Different formulary types include different numbers of drugs:

- **SPECIALTY**: 500-1,500 drugs
  - Focus on specialty medications
  - Higher inclusion rate for specialty drugs (80%)
  
- **BASIC**: 1,500-2,000 drugs
  - Essential medications only
  - Lower specialty drug inclusion (20%)
  
- **STANDARD**: 1,800-2,200 drugs
  - Balanced coverage
  - Moderate specialty drug inclusion (40%)
  
- **ENHANCED**: 2,000-2,500 drugs
  - Comprehensive coverage
  - Higher specialty drug inclusion (60%)
  
- **MAIL_ORDER**: 1,800-2,200 drugs
  - Maintenance medications
  - Moderate specialty drug inclusion

#### Protected Drug Classes (Medicare Part D Requirement)

For Medicare Part D and Medicare Advantage formularies, the following drug classes must have "all or substantially all" drugs covered (92% coverage):

- ANTICONVULSANT
- ANTIDEPRESSANT
- CHEMOTHERAPY
- TARGETED_THERAPY
- IMMUNOTHERAPY
- ANTIPSYCHOTIC
- IMMUNOSUPPRESSANT
- BIOLOGIC

#### Generic Drug Inclusion

- **Enhanced/Standard Formularies**: 85% of all generic drugs
- **Basic/Specialty Formularies**: 70% of all generic drugs
- Rationale: Generics are cost-effective and widely covered

#### Specialty Drug Inclusion

Varies by formulary type (see above). Specialty drugs are expensive and require special handling.

#### Brand Drug Inclusion

Brand drugs fill remaining slots after protected classes, generics, and specialty drugs are included.

### 2. Tier Assignment Rules

#### Tier 1 - Generic Drugs (Lowest Cost)
- **Assignment**: 95% of generic drugs → Tier 1
- **Characteristics**: Most affordable, widely available
- **Coverage**: ~94.1% of all relationships

#### Tier 2 - Preferred Brand Drugs
- **Assignment**: 60% of brand drugs → Tier 2
- **Characteristics**: Brand drugs with favorable pricing agreements
- **Coverage**: ~2.6% of all relationships

#### Tier 3 - Non-Preferred Brand Drugs
- **Assignment**: 40% of brand drugs → Tier 3
- **Characteristics**: Brand drugs without favorable pricing
- **Coverage**: ~1.9% of all relationships

#### Tier 4 - Specialty Drugs (Standard)
- **Assignment**: 70% of specialty drugs → Tier 4
- **Characteristics**: High-cost specialty medications
- **Coverage**: ~1.1% of all relationships

#### Tier 5 - High-Cost Specialty Drugs
- **Assignment**: 30% of specialty drugs → Tier 5
- **Characteristics**: Extremely expensive specialty medications
- **Coverage**: ~0.2% of all relationships

### 3. Utilization Management Rules

Utilization management controls are applied based on tier to manage costs and ensure appropriate use.

#### Tier 1 (Generic) - Minimal Controls
- **Prior Authorization**: 2% of drugs
- **Step Therapy**: 1% of drugs
- **Quantity Limits**: 5% of drugs
- Rationale: Low cost, minimal restrictions needed

#### Tier 2 (Preferred Brand) - Light Controls
- **Prior Authorization**: 10% of drugs
- **Step Therapy**: 8% of drugs
- **Quantity Limits**: 15% of drugs
- Rationale: Moderate cost, some oversight needed

#### Tier 3 (Non-Preferred Brand) - Moderate Controls
- **Prior Authorization**: 25% of drugs
- **Step Therapy**: 20% of drugs
- **Quantity Limits**: 30% of drugs
- Rationale: Higher cost, encourage generic alternatives

#### Tier 4 (Specialty) - Significant Controls
- **Prior Authorization**: 60% of drugs
- **Step Therapy**: 15% of drugs
- **Quantity Limits**: 50% of drugs
- Rationale: Very high cost, careful management required

#### Tier 5 (High-Cost Specialty) - Extensive Controls
- **Prior Authorization**: 90% of drugs
- **Step Therapy**: 25% of drugs
- **Quantity Limits**: 70% of drugs
- Rationale: Extremely high cost, maximum oversight

### 4. Quantity Limits

When quantity limits are applied, they vary by dosage form:

| Dosage Form | Allowed Days Supply |
|-------------|---------------------|
| TABLET | 30, 60, or 90 days |
| CAPSULE | 30, 60, or 90 days |
| SOLUTION | 30 or 60 days |
| INJECTION | 30 or 90 days |
| CREAM | 30 or 60 days |
| OINTMENT | 30 or 60 days |
| SUSPENSION | 30 days |
| PATCH | 30 days |
| INHALER | 30 or 90 days |
| SUPPOSITORY | 30 days |

### 5. Drug Status Assignment

- **PREFERRED**: Tiers 1-2, and some Tier 3 drugs (40%)
- **NON_PREFERRED**: Most Tier 3 drugs (60%)
- **SPECIALTY**: Tiers 4-5

## Actual Data Distribution

Based on the generated 10,000,000 relationships:

### Tier Distribution
- Tier 1: 9,413,474 (94.1%)
- Tier 2: 257,150 (2.6%)
- Tier 3: 194,017 (1.9%)
- Tier 4: 111,502 (1.1%)
- Tier 5: 23,857 (0.2%)

### Status Distribution
- PREFERRED: 9,748,397 (97.5%)
- NON_PREFERRED: 116,244 (1.2%)
- SPECIALTY: 135,359 (1.4%)

### Utilization Management
- Prior Authorization: 350,915 (3.5%)
- Step Therapy: 176,186 (1.8%)
- Quantity Limits: 639,003 (6.4%)

## Healthcare Regulations Compliance

### Medicare Part D Requirements

1. **Protected Classes Coverage**: 92% of drugs in protected classes are included for Medicare formularies
2. **Tier Structure**: Supports 5-tier structure common in Medicare Part D
3. **Utilization Management**: Implements appropriate controls while maintaining access

### Commercial Insurance Standards

1. **Flexible Tier Counts**: Supports 3-5 tier structures
2. **Generic Preference**: High generic inclusion rates
3. **Cost Management**: Appropriate utilization management controls

## Data Quality Characteristics

### Realistic Distribution
- Heavily weighted toward generic drugs (Tier 1) reflecting real-world formulary composition
- Specialty drugs represent small percentage but with high management requirements
- Utilization management increases with drug cost/tier

### Relationship Integrity
- Each relationship is unique (formulary_code + ndc_code combination)
- All foreign keys reference valid formularies and drugs
- Business rules consistently applied across all relationships

### Scalability
- 10 million relationships provide realistic scale for testing
- Data split across 64 files for manageable file sizes
- Supports parallel processing and batch loading

## Generation Script

**Script**: `IgniteVSPostgres/database/scripts/generate_formularies_drugs.py`

**Key Configuration**:
```python
MAX_TOTAL_RELATIONSHIPS = 10_000_000
MAX_FILE_SIZE_MB = 30
OUTPUT_FILE_PREFIX = "us_pharmacy_formularies_drugs"
OUTPUT_DIR = "../../src/main/resources/data"
```

**Execution**:
```bash
cd IgniteVSPostgres/database/scripts
python3 generate_formularies_drugs.py
```

## Use Cases

### 1. Benefit Verification
Determine if a drug is covered by a specific formulary and at what tier/cost.

### 2. Prior Authorization Workflow
Identify which drugs require prior authorization for a given formulary.

### 3. Step Therapy Management
Implement step therapy protocols based on formulary requirements.

### 4. Cost Estimation
Calculate member cost-sharing based on tier and formulary design.

### 5. Formulary Comparison
Compare drug coverage across different formularies.

### 6. Utilization Analysis
Analyze patterns of utilization management requirements.

## Related Documentation

- [FORMULARY_DATA.md](./FORMULARY_DATA.md) - Formulary data structure and generation
- [DRUG_DATA.md](./DRUG_DATA.md) - Drug data structure and generation
- [PLAN_DATA.md](./PLAN_DATA.md) - Benefit plan data structure
- [MEMBER_DATA.md](./MEMBER_DATA.md) - Member data structure
- [ENROLLMENT_DATA.md](./ENROLLMENT_DATA.md) - Enrollment data structure

## Version History

- **v1.0** (2025-11-07): Initial documentation
  - 10 million relationships
  - 5,000 formularies
  - 64 output files
  - Comprehensive business rules implementation