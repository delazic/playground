# US Pharmacy Benefit Plans - Data Documentation

## Overview

This document describes the comprehensive set of US pharmacy benefit plans included in the `us_pharmacy_plans.csv` file. These plans represent the most common types of prescription drug coverage in the United States healthcare system.

## File Location

```
IgniteVSPostgres/src/main/resources/data/us_pharmacy_plans.csv
```

**Note:** CSV files are stored in the Java application's resources directory and loaded via classpath. This ensures the data is packaged with the application and accessible regardless of the working directory.

## Plan Categories

### 1. Commercial Plans (5 plans)

Commercial health insurance plans offered by private insurers, typically purchased by individuals or provided by employers.

| Plan Code | Plan Name | Deductible | OOP Max | Tier 1 Copay | Description |
|-----------|-----------|------------|---------|--------------|-------------|
| COMM_PLAT_001 | Platinum Commercial Plan | $0 | $2,000 | $5 | Lowest cost-sharing, highest premiums |
| COMM_GOLD_001 | Gold Commercial Plan | $500 | $3,000 | $10 | Low cost-sharing, moderate premiums |
| COMM_SILV_001 | Silver Commercial Plan | $1,500 | $5,000 | $15 | Moderate cost-sharing and premiums |
| COMM_BRON_001 | Bronze Commercial Plan | $3,000 | $8,000 | $20 | Higher cost-sharing, lower premiums |
| COMM_HDHP_001 | High Deductible Health Plan | $5,000 | $7,000 | 20% coinsurance | HSA-eligible with high deductible |

**Key Features:**
- Metal tier system (Platinum, Gold, Silver, Bronze)
- HDHP compatible with Health Savings Accounts (HSA)
- Mail order available for all tiers
- Specialty pharmacy required for high-tier drugs

### 2. Medicare Plans (4 plans)

Federal health insurance program for people 65+ and certain younger people with disabilities.

| Plan Code | Plan Name | Deductible | OOP Max | Tier 1 Copay | Description |
|-----------|-----------|------------|---------|--------------|-------------|
| MCARE_PDPB_001 | Medicare Part D Basic | $545 | $8,000 | $0 | Standard Medicare Part D coverage |
| MCARE_PDPE_001 | Medicare Part D Enhanced | $0 | $8,000 | $0 | Enhanced with lower copays |
| MCARE_MAPD_001 | Medicare Advantage Prescription Drug | $0 | $5,000 | $0 | Integrated MA with drug coverage |
| MCARE_SUPP_001 | Medicare Supplement | $0 | $0 | N/A | Medigap (no drug coverage) |

**Key Features:**
- Part D Basic follows CMS standard benefit design
- Coverage gap ("donut hole") applies to basic plans
- Enhanced plans offer additional benefits
- MAPD integrates medical and pharmacy benefits
- Medigap supplements Original Medicare but doesn't cover drugs

**2024 Medicare Part D Standard Benefit:**
- Deductible: $545
- Initial Coverage: 25% coinsurance
- Coverage Gap: 25% coinsurance (closed in 2020)
- Catastrophic Coverage: $0 or 5% after $8,000 OOP

### 3. Medicaid Plans (2 plans)

State and federal program providing health coverage to low-income individuals and families.

| Plan Code | Plan Name | Deductible | OOP Max | Tier 1 Copay | Description |
|-----------|-----------|------------|---------|--------------|-------------|
| MCAID_STD_001 | Medicaid Standard | $0 | $0 | $1 | State Medicaid pharmacy benefit |
| MCAID_EXP_001 | Medicaid Expansion | $0 | $0 | $0 | ACA Medicaid expansion coverage |

**Key Features:**
- Minimal or no cost-sharing
- State-specific formularies
- Prior authorization common
- No mail order (varies by state)
- Specialty pharmacy required

### 4. Exchange/Marketplace Plans (2 plans)

Plans sold through the Affordable Care Act (ACA) Health Insurance Marketplace.

| Plan Code | Plan Name | Deductible | OOP Max | Tier 1 Copay | Description |
|-----------|-----------|------------|---------|--------------|-------------|
| EXCH_SILV_001 | Exchange Silver Plan | $2,000 | $6,000 | $15 | ACA Marketplace Silver tier |
| EXCH_GOLD_001 | Exchange Gold Plan | $1,000 | $4,000 | $10 | ACA Marketplace Gold tier |

**Key Features:**
- Must cover essential health benefits
- Actuarial value: Silver (70%), Gold (80%)
- Cost-sharing reductions available for eligible individuals
- Standardized benefit designs in some states

### 5. Employer-Sponsored Plans (3 plans)

Health insurance provided by employers to their employees.

| Plan Code | Plan Name | Deductible | OOP Max | Tier 1 Copay | Description |
|-----------|-----------|------------|---------|--------------|-------------|
| EMP_TRAD_001 | Employer Traditional PPO | $750 | $4,000 | $10 | Traditional employer-sponsored PPO |
| EMP_HDHP_001 | Employer HDHP with HSA | $4,000 | $6,500 | 20% coinsurance | HDHP with HSA contribution |
| EMP_PREM_001 | Employer Premium Plan | $250 | $2,500 | $5 | Premium plan with low cost-sharing |

**Key Features:**
- Most common source of health insurance in US
- Employer typically pays portion of premium
- HDHP plans allow HSA contributions
- Premium plans offer lowest cost-sharing

### 6. Union Plans (2 plans)

Health benefits negotiated through collective bargaining agreements.

| Plan Code | Plan Name | Deductible | OOP Max | Tier 1 Copay | Description |
|-----------|-----------|------------|---------|--------------|-------------|
| UNION_STD_001 | Union Standard Plan | $500 | $3,500 | $8 | Union-negotiated pharmacy benefit |
| UNION_PREM_001 | Union Premium Plan | $100 | $2,000 | $3 | Enhanced union pharmacy benefit |

**Key Features:**
- Negotiated through collective bargaining
- Often more generous than standard employer plans
- May include additional benefits
- Typically lower cost-sharing

### 7. Government Employee Plans (5 plans)

Health insurance for government employees and military personnel.

#### Veterans Affairs (VA)
| Plan Code | Plan Name | Deductible | OOP Max | Tier 1 Copay | Description |
|-----------|-----------|------------|---------|--------------|-------------|
| VA_STANDARD_001 | Veterans Affairs Standard | $0 | $0 | $5 | VA pharmacy benefit for veterans |

#### TRICARE (Military)
| Plan Code | Plan Name | Deductible | OOP Max | Tier 1 Copay | Description |
|-----------|-----------|------------|---------|--------------|-------------|
| TRICARE_STD_001 | TRICARE Standard | $0 | $3,500 | $13 | Military health system pharmacy |
| TRICARE_PRM_001 | TRICARE Prime | $0 | $3,000 | $0 | No copays at military facilities |

#### Federal Employee Health Benefits (FEHB)
| Plan Code | Plan Name | Deductible | OOP Max | Tier 1 Copay | Description |
|-----------|-----------|------------|---------|--------------|-------------|
| FEHB_BASIC_001 | Federal Employee Basic | $1,000 | $5,000 | $10 | FEHB basic tier |
| FEHB_STAND_001 | Federal Employee Standard | $500 | $4,000 | $8 | FEHB standard tier |
| FEHB_PREM_001 | Federal Employee Premium | $250 | $3,000 | $5 | FEHB premium tier |

**Key Features:**
- VA: Priority groups determine copay amounts
- TRICARE: Active duty, retirees, and families
- FEHB: Wide choice of plans for federal employees

### 8. Children's Health Insurance Program (1 plan)

State-federal partnership providing low-cost health coverage to children.

| Plan Code | Plan Name | Deductible | OOP Max | Tier 1 Copay | Description |
|-----------|-----------|------------|---------|--------------|-------------|
| CHIP_STD_001 | Children's Health Insurance | $0 | $0 | $0 | State CHIP pharmacy benefit |

**Key Features:**
- Covers children in families with income too high for Medicaid
- Minimal or no cost-sharing
- Comprehensive benefits including dental and vision

### 9. Individual Market Plans (4 plans)

Plans purchased directly by individuals, not through an employer.

| Plan Code | Plan Name | Deductible | OOP Max | Tier 1 Copay | Description |
|-----------|-----------|------------|---------|--------------|-------------|
| IND_CATAS_001 | Individual Catastrophic | $9,450 | $9,450 | 50% coinsurance | For under 30 or hardship exemption |
| IND_BRON_001 | Individual Bronze | $7,000 | $9,450 | $25 | Individual market bronze plan |
| IND_SILV_001 | Individual Silver | $4,500 | $9,450 | $20 | Individual market silver plan |
| IND_GOLD_001 | Individual Gold | $2,000 | $9,450 | $15 | Individual market gold plan |

**Key Features:**
- Catastrophic plans only for those under 30 or with hardship exemption
- Metal tiers follow ACA standards
- Higher premiums than employer-sponsored plans
- May qualify for premium tax credits

### 10. PBM Carve-Out Plans (1 plan)

Standalone pharmacy benefit management separate from medical coverage.

| Plan Code | Plan Name | Deductible | OOP Max | Tier 1 Copay | Description |
|-----------|-----------|------------|---------|--------------|-------------|
| PBMCO_STD_001 | PBM Carve-Out Standard | $1,000 | $5,000 | $10 | Standalone PBM carve-out plan |

**Key Features:**
- Pharmacy benefits managed separately from medical
- Common in self-insured employer plans
- Allows specialized pharmacy benefit management

## Drug Tier Structure

All plans follow a standard 5-tier formulary structure:

### Tier 1: Generic Drugs
- **Cost:** Lowest copay ($0-$25) or 20% coinsurance
- **Examples:** Lisinopril, Metformin, Atorvastatin
- **Coverage:** Preferred generics, widely available

### Tier 2: Preferred Brand Drugs
- **Cost:** Low-moderate copay ($5-$50) or 25% coinsurance
- **Examples:** Januvia, Eliquis
- **Coverage:** Brand drugs on preferred formulary

### Tier 3: Non-Preferred Brand Drugs
- **Cost:** Moderate-high copay ($25-$100) or 25-30% coinsurance
- **Examples:** Symbicort, non-preferred brands
- **Coverage:** Brand drugs not on preferred formulary
- **Requirements:** May require prior authorization

### Tier 4: Specialty Drugs
- **Cost:** High copay ($50-$400) or 33-40% coinsurance
- **Examples:** Humira, Enbrel, biologics
- **Coverage:** High-cost specialty medications
- **Requirements:** 
  - Prior authorization required
  - Step therapy often required
  - Specialty pharmacy dispensing
  - Limited quantity/days supply

### Tier 5: Specialty Biologics (Some Plans)
- **Cost:** Highest copay ($100-$500) or 50% coinsurance
- **Examples:** Gene therapies, rare disease treatments
- **Coverage:** Ultra-high-cost specialty biologics
- **Requirements:** Extensive prior authorization and management

## Cost-Sharing Components

### Deductible
- Amount patient pays before plan coverage begins
- Ranges from $0 (Medicaid, some Medicare) to $9,450 (Catastrophic)
- May not apply to preventive drugs or Tier 1 generics

### Out-of-Pocket Maximum (OOP Max)
- Maximum amount patient pays in a year
- After reaching OOP max, plan pays 100%
- Ranges from $0 (Medicaid) to $9,450 (Individual market)

### Copay
- Fixed dollar amount per prescription
- Varies by tier (Tier 1: $0-$25, Tier 4: $50-$400)
- Simpler for patients to understand

### Coinsurance
- Percentage of drug cost
- Common in HDHPs and catastrophic plans
- Ranges from 20% (Tier 1) to 50% (Tier 5)

## Additional Features

### Mail Order Pharmacy
- **Available:** Most commercial, Medicare, employer plans
- **Not Available:** Medicaid (varies by state), catastrophic plans
- **Benefits:** 90-day supply, lower copays, home delivery
- **Typical Savings:** 2x copay for 3x supply

### Specialty Pharmacy
- **Required:** Medicare, Medicaid, most Tier 4/5 drugs
- **Optional:** Some commercial plans
- **Services:** 
  - Clinical support and monitoring
  - Prior authorization assistance
  - Patient education
  - Adherence programs

## Regulatory Compliance

### Medicare Part D Requirements
- Must cover at least 2 drugs per therapeutic category
- Protected classes must cover "all or substantially all" drugs
- Annual out-of-pocket threshold: $8,000 (2024)
- Coverage gap closed as of 2020

### ACA Requirements
- Essential health benefits must be covered
- No annual or lifetime limits
- Preventive drugs covered at $0 copay
- Metal tier actuarial values:
  - Bronze: 60%
  - Silver: 70%
  - Gold: 80%
  - Platinum: 90%

### Medicaid Requirements
- Nominal copays only (typically $0-$8)
- No cost-sharing for certain populations
- State-specific formularies
- Rebate program for manufacturers

## CSV File Structure

### Columns

1. **plan_code** - Unique identifier (e.g., COMM_GOLD_001)
2. **plan_name** - Descriptive name
3. **plan_type** - Category (COMMERCIAL, MEDICARE, MEDICAID, etc.)
4. **plan_category** - Sub-category (GOLD, SILVER, PART_D, etc.)
5. **effective_date** - Plan start date (2024-01-01)
6. **annual_deductible** - Annual deductible amount
7. **out_of_pocket_max** - Annual OOP maximum
8. **tier1_copay** through **tier5_copay** - Fixed copay amounts
9. **tier1_coinsurance** through **tier5_coinsurance** - Percentage coinsurance
10. **mail_order_available** - TRUE/FALSE
11. **specialty_pharmacy_required** - TRUE/FALSE
12. **description** - Plan description

### Data Format
- Currency values in dollars (no $ symbol)
- Percentages as decimals (0.20 = 20%)
- Dates in YYYY-MM-DD format
- Boolean as TRUE/FALSE

## Usage

### Import to Database

```sql
COPY plan (plan_code, plan_name, plan_type, effective_date, ...)
FROM '/path/to/us_pharmacy_plans.csv'
DELIMITER ','
CSV HEADER;
```

### Analysis Queries

```sql
-- Average deductible by plan type
SELECT plan_type, AVG(annual_deductible) as avg_deductible
FROM plan
GROUP BY plan_type
ORDER BY avg_deductible;

-- Plans with mail order
SELECT plan_name, tier1_copay
FROM plan
WHERE mail_order_available = TRUE;

-- Specialty pharmacy requirements
SELECT plan_type, COUNT(*) as plan_count
FROM plan
WHERE specialty_pharmacy_required = TRUE
GROUP BY plan_type;
```

## Market Statistics

### Plan Distribution in US (Approximate)
- **Employer-Sponsored:** 49% of population
- **Medicare:** 18% of population
- **Medicaid:** 20% of population
- **Individual Market:** 6% of population
- **Other Government:** 7% of population

### Average Costs (2024)
- **Generic Drug:** $10-$15 copay
- **Brand Drug:** $40-$60 copay
- **Specialty Drug:** $100-$200 copay or 25-33% coinsurance
- **Annual Deductible:** $1,500-$3,000 (commercial plans)

## References

- Centers for Medicare & Medicaid Services (CMS)
- Healthcare.gov (ACA Marketplace)
- Kaiser Family Foundation (KFF) Employer Health Benefits Survey
- National Association of Insurance Commissioners (NAIC)

## Version History

- **v1.0** (2024-11-06) - Initial release with 30 representative US pharmacy plans