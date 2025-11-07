# US Pharmacy Members Dataset

## Overview

This dataset contains **1,000,000** synthetic US healthcare members generated for testing and development purposes.

## File Information

- **File Pattern**: `us_pharmacy_members_01.csv` through `us_pharmacy_members_10.csv`
- **Total Files**: 10 files
- **Records per File**: 100,000 members
- **Size per File**: ~12 MB
- **Total Size**: ~120 MB
- **Format**: CSV (Comma-Separated Values)
- **Encoding**: UTF-8
- **Total Records**: 1,000,000 members
- **Generated**: November 6, 2024

## Schema

The CSV file matches the `member` table schema from `01-create-schema.sql`:

| Column | Type | Description |
|--------|------|-------------|
| `member_number` | VARCHAR(50) | Unique member identifier (format: MBR000000001 - MBR001000000) |
| `first_name` | VARCHAR(100) | Member's first name |
| `last_name` | VARCHAR(100) | Member's last name |
| `date_of_birth` | DATE | Date of birth (YYYY-MM-DD format) |
| `gender` | CHAR(1) | Gender (M=Male, F=Female, U=Unspecified) |
| `address` | VARCHAR(255) | Street address |
| `city` | VARCHAR(100) | City name |
| `state` | VARCHAR(2) | US state code (2-letter abbreviation) |
| `zip_code` | VARCHAR(10) | ZIP code |
| `phone` | VARCHAR(20) | Phone number (format: XXX-XXX-XXXX) |
| `email` | VARCHAR(255) | Email address |

## Data Characteristics

### Demographics

**Age Distribution** (Representative of US Population):
- 0-4 years: ~6%
- 5-14 years: ~6.5%
- 15-24 years: ~13%
- 25-34 years: ~13%
- 35-44 years: ~13%
- 45-54 years: ~13%
- 55-64 years: ~13%
- 65-74 years: ~13%
- 75+ years: ~9.5%

**Gender Distribution**:
- Female (F): ~50.5%
- Male (M): ~49.3%
- Unspecified (U): ~0.2%

**Geographic Distribution**:
All 50 US states represented with population-weighted distribution:
- Top states: CA (~12%), TX (~9%), FL (~6.5%), NY (~6%), PA (~4%)
- All states included based on approximate US population percentages

### Data Quality

- **Uniqueness**: All member numbers are unique (MBR000000001 through MBR001000000)
- **Realistic Names**: Generated using US name distributions
- **Valid Addresses**: Realistic US addresses with proper formatting
- **Valid Dates**: All dates of birth are valid and age-appropriate
- **Valid Emails**: Properly formatted email addresses
- **Valid Phone Numbers**: US phone number format (XXX-XXX-XXXX)

## Files Structure

The dataset is split into 10 files for easier handling:

| File | Member Range | Records |
|------|-------------|---------|
| `us_pharmacy_members_01.csv` | MBR000000001 - MBR000100000 | 100,000 |
| `us_pharmacy_members_02.csv` | MBR000100001 - MBR000200000 | 100,000 |
| `us_pharmacy_members_03.csv` | MBR000200001 - MBR000300000 | 100,000 |
| `us_pharmacy_members_04.csv` | MBR000300001 - MBR000400000 | 100,000 |
| `us_pharmacy_members_05.csv` | MBR000400001 - MBR000500000 | 100,000 |
| `us_pharmacy_members_06.csv` | MBR000500001 - MBR000600000 | 100,000 |
| `us_pharmacy_members_07.csv` | MBR000600001 - MBR000700000 | 100,000 |
| `us_pharmacy_members_08.csv` | MBR000700001 - MBR000800000 | 100,000 |
| `us_pharmacy_members_09.csv` | MBR000800001 - MBR000900000 | 100,000 |
| `us_pharmacy_members_10.csv` | MBR000900001 - MBR001000000 | 100,000 |

## Sample Data

```csv
member_number,first_name,last_name,date_of_birth,gender,address,city,state,zip_code,phone,email
MBR000000001,Margaret,Johnson,1980-06-30,F,819 Johnson Course,Johnberg,TX,29158,304-892-9935,margaret.johnson90@gmail.com
MBR000000002,Kathy,Johnson,1980-11-02,F,79402 Peterson Drives Apt. 511,Lake Debra,FL,50298,816-227-4257,kathy.johnson734@gmail.com
MBR000000003,Heather,Cooper,1962-04-22,F,1849 Ray Squares,Lindsaymouth,VA,09617,206-977-3615,heather.cooper715@gmail.com
```

## Generation Method

Data was generated using Python with the Faker library:
- **Script**: `../scripts/generate_members.py`
- **Library**: Faker 37.12.0
- **Seed**: 42 (for reproducibility)
- **Locale**: en_US

## Usage

### Loading into PostgreSQL

**Option 1: Load all files sequentially**
```bash
for i in {01..10}; do
  psql -U pbm_user -d pbm_db -c "COPY member (member_number, first_name, last_name, date_of_birth, gender, address, city, state, zip_code, phone, email) FROM '/path/to/us_pharmacy_members_${i}.csv' DELIMITER ',' CSV HEADER;"
done
```

**Option 2: Load individual files**
```sql
COPY member (member_number, first_name, last_name, date_of_birth, gender,
             address, city, state, zip_code, phone, email)
FROM '/path/to/us_pharmacy_members_01.csv'
DELIMITER ','
CSV HEADER;
```

Repeat for files 02 through 10.

### Using with Java Application

The data can be loaded using the `MemberDAO` (to be implemented) or directly via JDBC batch operations.

## Performance Considerations

- **File Size**: 10 files × 12 MB each = ~120 MB total
- **Split Files**: Easier to handle, can be loaded in parallel
- **Load Time**: Approximately 3-6 seconds per file, 30-60 seconds total (depends on hardware)
- **Parallel Loading**: Files can be loaded concurrently for faster import
- **Indexing**: Ensure indexes are created after bulk load for optimal performance
- **Memory**: Batch operations recommended (1000-10000 records per batch)

## Data Privacy

⚠️ **Important**: This is **synthetic data** generated for testing purposes only. No real personal information is included. All names, addresses, phone numbers, and email addresses are fictitious.

## Regeneration

To regenerate the dataset with different data:

```bash
cd database/scripts
python3 generate_members.py
```

This will create a new `us_pharmacy_members.csv` file with 1,000,000 new synthetic members.

## Related Files

- **Schema**: `../init/01-create-schema.sql` - Database table definitions
- **Generator**: `../scripts/generate_members.py` - Python script to generate data
- **Plans Data**: `us_pharmacy_plans.csv` - Related benefit plans dataset (see PLAN_DATA.md)

## Statistics

```
Total Records:     1,000,000
Total Files:       10
Records per File:  100,000
File Size (each):  ~12 MB
Total Size:        ~120 MB
Unique Members:    1,000,000
States Covered:    50
Age Range:         0-95 years
Gender Types:      3 (M, F, U)
```

## Version History

- **v1.0** (2024-11-06): Initial generation of 1M members with US population-representative demographics