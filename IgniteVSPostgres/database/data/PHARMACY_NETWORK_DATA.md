# Pharmacy Network Data

## Overview
This document describes the synthetic pharmacy network data generated for the US healthcare system simulation.

## Data Generation
- **Script**: `database/scripts/generate_pharmacy_networks.py`
- **Output Location**: `src/main/resources/data/`
- **File Pattern**: `us_pharmacy_pharmacy_networks_01.csv`, `us_pharmacy_pharmacy_networks_02.csv`, etc.
- **File Size**: ~30 MB per file

## Schema
The pharmacy network data includes the following fields:

| Field | Type | Description |
|-------|------|-------------|
| network_id | VARCHAR | Unique network assignment ID (NET0000000001) |
| pharmacy_id | VARCHAR | Foreign key to pharmacies table |
| network_name | VARCHAR | Name of the pharmacy network |
| network_type | VARCHAR | Type: PBM, Retail, Specialty, Mail-Order, Regional, Independent |
| network_tier | VARCHAR | Tier: Preferred, Standard |
| contract_type | VARCHAR | Contract type: Direct, Indirect, PSAO, Aggregator |
| effective_date | DATE | When the network participation started |
| termination_date | DATE | When the network participation ended (nullable) |
| status | VARCHAR | Active, Inactive, or Pending |
| reimbursement_rate | VARCHAR | Reimbursement formula (e.g., AWP-15%) |
| dispensing_fee | DECIMAL | Dispensing fee amount |
| is_preferred | BOOLEAN | Whether this is a preferred network |
| is_mail_order | BOOLEAN | Whether this is a mail-order network |
| is_specialty | BOOLEAN | Whether this is a specialty pharmacy network |
| created_at | TIMESTAMP | Record creation timestamp |
| updated_at | TIMESTAMP | Record last update timestamp |

## US Healthcare Pharmacy Networks

### Network Types

#### 1. PBM Networks (Pharmacy Benefit Managers)
Major PBMs that manage prescription drug benefits:
- **CVS Caremark**: One of the largest PBMs in the US
- **Express Scripts**: Major PBM owned by Cigna
- **OptumRx**: PBM division of UnitedHealth Group
- **Humana Pharmacy**: Integrated with Humana health plans
- **Prime Therapeutics**: Owned by Blue Cross Blue Shield plans

#### 2. Retail Networks
Chain pharmacy networks:
- **Walgreens**: National retail pharmacy chain
- **CVS**: Largest pharmacy chain in the US
- **Walmart**: Discount retail pharmacy
- **Kroger**: Grocery store pharmacy network
- **Rite Aid**: Regional retail pharmacy chain

#### 3. Specialty Networks
Networks for high-cost, complex medications:
- **Accredo**: Express Scripts specialty pharmacy
- **CVS Specialty**: CVS Health specialty division
- **Walgreens Specialty**: Specialty pharmacy services
- **BriovaRx**: Specialty pharmacy network

#### 4. Mail-Order Networks
Home delivery pharmacy services:
- Typically operated by major PBMs
- Lower cost alternative to retail
- 90-day supply options

#### 5. Regional Networks
Geographic-specific networks:
- Northeast, Southeast, Midwest, Southwest, West Coast
- Serve specific geographic areas
- Often include independent pharmacies

#### 6. Independent Networks
Networks of independent pharmacies:
- **Health Mart**: McKesson-affiliated independents
- **Community Pharmacy Network**: Local independent pharmacies
- **Independent Pharmacy Network**: Various independent pharmacies

### Network Tiers

#### Preferred Tier
- Lower copays for members
- Better reimbursement terms for pharmacies
- Stricter quality and performance requirements
- Typically 60-70% of network pharmacies

#### Standard Tier
- Higher copays for members
- Standard reimbursement rates
- Basic quality requirements
- Broader pharmacy access

### Contract Types

#### Direct Contracts
- Direct agreement between pharmacy and PBM/payer
- Negotiated rates and terms
- Most common for large chains

#### Indirect Contracts
- Through intermediary organizations
- Common for independent pharmacies
- May have less favorable terms

#### PSAO (Pharmacy Services Administrative Organization)
- Group purchasing organization for pharmacies
- Negotiates on behalf of independent pharmacies
- Provides better rates through collective bargaining

#### Aggregator
- Third-party that aggregates multiple pharmacies
- Provides network access
- Administrative services

## Data Characteristics

### Volume
- Each pharmacy participates in 3-8 networks on average
- Total records: ~550,000 (for 100,000 pharmacies)
- Multiple CSV files of ~30 MB each

### Status Distribution
- **Active**: ~67% (most current network participations)
- **Inactive**: ~17% (terminated contracts)
- **Pending**: ~16% (contracts being negotiated)

### Reimbursement Rates
- Based on AWP (Average Wholesale Price)
- Typical discounts: 12-24% off AWP
- Format: "AWP-15%", "AWP-18%", etc.

### Dispensing Fees
- Range: $0.50 to $3.50 per prescription
- Varies by network and contract type
- Preferred networks typically have higher fees

### Effective Dates
- Randomly distributed over last 5 years
- Reflects realistic contract cycles
- Annual renewals common

### Termination Dates
- Active contracts: mostly no termination date (90%)
- Inactive contracts: past termination dates
- Pending contracts: no termination date

## Data Loading Implementation

### Foreign Key Resolution
The pharmacy network data uses a **foreign key resolution pattern** to map pharmacy references to actual database IDs:

1. **CSV Format**: Contains synthetic pharmacy IDs (e.g., `PHARM00000001`)
2. **Mapping Process**:
   - `PharmacyNetworkConverter` loads the pharmacy CSV to create a mapping
   - Synthetic IDs are mapped to actual NCPDP IDs (e.g., `1849423`)
3. **Database Insert**:
   - `PharmacyNetworkDAO` uses a JOIN query to resolve NCPDP ID to pharmacy_id UUID
   - SQL: `SELECT p.pharmacy_id FROM pharmacy p WHERE p.ncpdp_id = ?`

This pattern ensures referential integrity and follows the same approach used by:
- `EnrollmentDAO` (member_number → member_id, plan_code → plan_id)
- `FormularyDrugDAO` (formulary_code → formulary_id, ndc_code → drug_id)

### Data Quality Handling

#### Invalid Pharmacy References
- **Issue**: CSV may contain references to pharmacies beyond available data
- **Solution**: Records with non-existent pharmacy IDs are skipped with warnings
- **Example**: `PHARM00100000` when only 50,000 pharmacies exist

#### Date Constraint Violations
- **Issue**: Some records have termination_date before effective_date
- **Solution**: Invalid date ranges are automatically corrected:
  - If `termination_date < effective_date`, termination_date is set to NULL
  - Warning is logged for each correction
- **Database Constraint**: `CHECK (termination_date IS NULL OR termination_date >= effective_date)`

### Java Implementation

#### Model Changes
- Added `ncpdpId` field to `PharmacyNetwork` model
- Used for pharmacy lookup during database insert

#### Converter Enhancements
- Loads pharmacy ID mapping from `us_pharmacy_pharmacies.csv`
- Validates and corrects date ranges
- Skips records with invalid pharmacy references

#### DAO Pattern
- Uses SELECT with JOIN for foreign key resolution
- Ensures data integrity at database level
- Batch insert with transaction support

## Usage

### Generate Data
```bash
cd IgniteVSPostgres/database/scripts
python3 generate_pharmacy_networks.py
```

### Load Data into PostgreSQL
```sql
COPY pharmacy_networks(
    network_id, pharmacy_id, network_name, network_type, network_tier,
    contract_type, effective_date, termination_date, status,
    reimbursement_rate, dispensing_fee, is_preferred, is_mail_order,
    is_specialty, created_at, updated_at
)
FROM '/path/to/us_pharmacy_pharmacy_networks_01.csv'
DELIMITER ','
CSV HEADER;
```

## Business Rules

### Network Participation
1. Pharmacies can participate in multiple networks simultaneously
2. Specialty pharmacies more likely in specialty networks
3. Chain pharmacies typically in their own retail networks
4. Independent pharmacies rely on PSAO contracts

### Contract Management
1. Contracts typically annual with renewal options
2. Preferred status requires meeting quality metrics
3. Termination can occur for non-compliance
4. Pending status during negotiation period

### Reimbursement
1. Preferred networks offer better reimbursement
2. Specialty networks have higher dispensing fees
3. Mail-order typically has lower fees but higher volume
4. Direct contracts often have better terms

## Related Tables
- **pharmacies**: Parent table with pharmacy details
- **benefit_plans**: Plans that use these networks
- **claims**: Prescription claims processed through networks

## Notes
- Data is synthetic and for testing purposes only
- Reflects realistic US pharmacy network structures
- Network names based on actual US healthcare entities
- Reimbursement rates reflect industry standards