# Pharmacy Data Documentation

## Overview
This document describes the synthetic pharmacy data generated for the PBM (Pharmacy Benefit Management) system. The data represents a realistic distribution of pharmacies across the United States.

## Data File
- **File**: `src/main/resources/data/us_pharmacy_pharmacies.csv`
- **Total Records**: 50,000 unique pharmacies
- **Format**: CSV (Comma-Separated Values)
- **Encoding**: UTF-8

## Data Generation
The pharmacy data was generated using the `database/scripts/generate_pharmacies.py` script, which creates synthetic but realistic pharmacy records based on actual US healthcare industry distributions.

### Generation Date
Generated: November 7, 2025

### Generation Command
```bash
cd IgniteVSPostgres
python3 database/scripts/generate_pharmacies.py
```

## CSV Schema

| Column Name | Data Type | Description | Example |
|------------|-----------|-------------|---------|
| `ncpdp_id` | VARCHAR(7) | National Council for Prescription Drug Programs ID (unique) | `1234567` |
| `pharmacy_name` | VARCHAR(255) | Name of the pharmacy | `CVS Pharmacy #1234` |
| `npi` | VARCHAR(10) | National Provider Identifier (unique) | `1234567890` |
| `address` | VARCHAR(255) | Street address | `123 Main St, Suite 100` |
| `city` | VARCHAR(100) | City name | `Los Angeles` |
| `state` | VARCHAR(2) | Two-letter state code | `CA` |
| `zip_code` | VARCHAR(10) | ZIP code | `90001` |
| `phone` | VARCHAR(20) | Phone number | `(555) 123-4567` |
| `pharmacy_type` | VARCHAR(20) | Type of pharmacy | `RETAIL` |
| `is_active` | BOOLEAN | Whether pharmacy is currently active | `true` |

## Data Distribution

### By Pharmacy Type
The distribution reflects typical US pharmacy market composition:

| Type | Count | Percentage | Description |
|------|-------|------------|-------------|
| RETAIL | 35,016 | 70.0% | Traditional retail pharmacies |
| LONG_TERM_CARE | 7,486 | 15.0% | Nursing home and assisted living pharmacies |
| SPECIALTY | 4,929 | 9.9% | Specialty medication pharmacies |
| MAIL_ORDER | 2,569 | 5.1% | Mail-order prescription services |

### By Chain Type
Distribution based on actual US pharmacy market share:

| Chain Type | Count | Percentage | Description |
|------------|-------|------------|-------------|
| INDEPENDENT/OTHER | 26,806 | 53.6% | Independent pharmacies and small chains |
| CVS | 8,000 | 16.0% | CVS Pharmacy locations |
| WALGREENS | 7,416 | 14.8% | Walgreens locations |
| WALMART | 3,916 | 7.8% | Walmart Pharmacy locations |
| RITE AID | 2,000 | 4.0% | Rite Aid locations |
| REGIONAL CHAIN | 1,862 | 3.7% | Regional chains (Hy-Vee, Meijer, H-E-B, etc.) |

### By Status
| Status | Count | Percentage |
|--------|-------|------------|
| Active | 47,501 | 95.0% |
| Inactive | 2,499 | 5.0% |

### Geographic Distribution (Top 10 States)
Distribution based on state population:

| State | Count | Percentage | State Name |
|-------|-------|------------|------------|
| CA | 6,031 | 12.1% | California |
| TX | 4,499 | 9.0% | Texas |
| FL | 3,309 | 6.6% | Florida |
| NY | 2,985 | 6.0% | New York |
| PA | 2,030 | 4.1% | Pennsylvania |
| IL | 1,919 | 3.8% | Illinois |
| OH | 1,833 | 3.7% | Ohio |
| GA | 1,684 | 3.4% | Georgia |
| NC | 1,570 | 3.1% | North Carolina |
| MI | 1,569 | 3.1% | Michigan |

All 50 US states are represented in the dataset with distribution proportional to population.

## Data Characteristics

### Unique Identifiers
- **NCPDP IDs**: All 50,000 are unique 7-digit numbers
- **NPIs**: All 50,000 are unique 10-digit National Provider Identifiers
- No duplicate identifiers exist in the dataset

### Pharmacy Names
- **Chain Pharmacies**: Include store numbers (e.g., "CVS Pharmacy #1234")
- **Independent Pharmacies**: Use realistic names (e.g., "Family Pharmacy", "Main Street Drug Store")
- **Regional Chains**: Include actual regional chain names with store numbers

### Addresses
- Realistic US street addresses with numbers, street names, and types
- Approximately 20% include suite/unit numbers
- ZIP codes are appropriate for their respective states

### Phone Numbers
- Standard US format: (XXX) XXX-XXXX
- Area codes are realistic (200-999 range)

## Database Integration

### Table Mapping
This CSV data maps to the `pharmacy` table in the database schema:

```sql
CREATE TABLE pharmacy (
    pharmacy_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ncpdp_id VARCHAR(7) UNIQUE NOT NULL,
    pharmacy_name VARCHAR(255) NOT NULL,
    npi VARCHAR(10),
    address VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(2),
    zip_code VARCHAR(10),
    phone VARCHAR(20),
    pharmacy_type pharmacy_type DEFAULT 'RETAIL',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

### Loading Data
The data can be loaded using the Java application's DAO layer or directly via SQL COPY command.

## Data Quality

### Validation Rules
- ✅ All NCPDP IDs are unique and 7 digits
- ✅ All NPIs are unique and 10 digits
- ✅ All state codes are valid 2-letter US state abbreviations
- ✅ All pharmacy types match the enum: RETAIL, MAIL_ORDER, SPECIALTY, LONG_TERM_CARE
- ✅ All is_active values are boolean (true/false)
- ✅ No null values in required fields

### Data Integrity
- No duplicate NCPDP IDs
- No duplicate NPIs
- All records have complete required fields
- Geographic distribution matches US population patterns
- Chain distribution reflects actual market share

## Use Cases

### Testing Scenarios
1. **Network Management**: Test pharmacy network assignments and contracts
2. **Claims Processing**: Validate pharmacy information during claim adjudication
3. **Geographic Analysis**: Test location-based pharmacy searches
4. **Chain Analysis**: Analyze performance by pharmacy chain
5. **Type-Based Routing**: Test specialty vs retail pharmacy routing logic

### Performance Testing
With 50,000 records, this dataset is suitable for:
- Index performance testing
- Query optimization
- Bulk insert operations
- Geographic search performance
- Join operation testing with claims and other tables

## Regeneration

To regenerate the pharmacy data with different parameters:

```bash
cd IgniteVSPostgres
python3 database/scripts/generate_pharmacies.py
```

The script can be modified to:
- Change the total number of pharmacies
- Adjust chain distribution percentages
- Modify geographic distribution
- Change pharmacy type ratios
- Adjust active/inactive ratios

## Related Documentation
- [Database Schema](../init/01-create-schema.sql)
- [Generation Script](../scripts/generate_pharmacies.py)
- [README](../scripts/README.md)

## Notes

### Real-World Context
- The US has approximately 67,000-70,000 pharmacies in reality
- This dataset represents ~71% of the actual pharmacy count
- Distribution percentages are based on industry reports and market data
- Chain counts reflect approximate market share as of 2024

### Synthetic Data Disclaimer
This is synthetic data generated for testing and development purposes. While it reflects realistic distributions and patterns, it does not represent actual pharmacies or real-world data.

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-11-07 | Initial generation of 50,000 pharmacy records |