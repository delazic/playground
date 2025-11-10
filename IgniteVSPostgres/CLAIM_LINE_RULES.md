# Claim Line Rules and Requirements

## Overview

This document defines the rules, requirements, and data model for claim lines (also called claim details or line items) in the US healthcare pharmacy benefit management (PBM) system. Each claim line represents an individual service, procedure, or product within a healthcare claim.

## Table of Contents

1. [Core Concepts](#core-concepts)
2. [Data Model](#data-model)
3. [Business Rules](#business-rules)
4. [Validation Rules](#validation-rules)
5. [Adjudication Rules](#adjudication-rules)
6. [Data Generation](#data-generation)

---

## Core Concepts

### What is a Claim Line?

A **claim line** is a detailed record of a specific service or product provided to a patient. In pharmacy claims:
- Each prescription fill typically generates one claim line
- Multiple medications dispensed together may create multiple lines
- Adjustments and corrections create additional lines

### Relationship to Claim Header

- **One-to-Many**: One claim (header) can have multiple claim lines
- **Sequential Numbering**: Lines are numbered sequentially (1, 2, 3, ...)
- **Independent Adjudication**: Each line is adjudicated separately
- **Aggregate Totals**: Header totals are sum of all line amounts

---

## Data Model

### Essential Fields

#### Identification
- **claim_line_id** (BIGINT, PK): Unique identifier for the claim line
- **claim_id** (BIGINT, FK): Reference to parent claim
- **line_number** (INTEGER): Sequential line number within claim (1-based)

#### Service Information
- **service_date** (DATE): Date service was provided
- **procedure_code** (VARCHAR): CPT/HCPCS code for service
- **ndc** (VARCHAR): National Drug Code (11-digit)
- **drug_name** (VARCHAR): Name of medication
- **quantity_dispensed** (DECIMAL): Number of units dispensed
- **days_supply** (INTEGER): Duration of medication supply
- **unit_of_measure** (VARCHAR): Unit type (EA, ML, GM, etc.)

#### Provider Information
- **rendering_provider_npi** (VARCHAR): NPI of provider who performed service
- **service_facility_npi** (VARCHAR): NPI of facility where service occurred
- **place_of_service** (VARCHAR): Code indicating service location

#### Financial Information
- **billed_amount** (DECIMAL): Provider's submitted charge
- **allowed_amount** (DECIMAL): Payer's approved amount
- **paid_amount** (DECIMAL): Actual payment to provider
- **patient_responsibility** (DECIMAL): Patient's portion (copay + coinsurance + deductible)
- **copay_amount** (DECIMAL): Fixed copayment amount
- **coinsurance_amount** (DECIMAL): Percentage-based patient cost
- **deductible_amount** (DECIMAL): Amount applied to deductible
- **ingredient_cost** (DECIMAL): Cost of medication
- **dispensing_fee** (DECIMAL): Pharmacy dispensing fee
- **sales_tax** (DECIMAL): Applicable sales tax

#### Adjudication Information
- **line_status** (VARCHAR): APPROVED, DENIED, PENDING, ADJUSTED
- **denial_code** (VARCHAR): Reason code if denied
- **denial_reason** (VARCHAR): Description of denial
- **adjustment_reason** (VARCHAR): Reason for adjustment
- **prior_auth_number** (VARCHAR): Prior authorization reference

#### Formulary & Coverage
- **formulary_status** (VARCHAR): Covered, non-covered, restricted
- **tier_level** (INTEGER): Drug tier (1-5)
- **daw_code** (VARCHAR): Dispense As Written code (0-9)
- **generic_indicator** (BOOLEAN): Is generic drug
- **brand_indicator** (BOOLEAN): Is brand drug

#### Clinical Information
- **diagnosis_pointer** (VARCHAR): Links to diagnosis codes on header
- **prescriber_npi** (VARCHAR): Prescribing provider NPI
- **prescription_number** (VARCHAR): Rx number
- **refill_number** (INTEGER): Refill count (0-11)
- **date_written** (DATE): Date prescription was written

#### Audit Trail
- **created_at** (TIMESTAMP): Record creation timestamp
- **updated_at** (TIMESTAMP): Last update timestamp
- **processed_by** (VARCHAR): System/user who processed
- **processing_time_ms** (INTEGER): Processing duration

---

## Business Rules

### 1. Line Number Rules

- **Sequential**: Line numbers must be sequential starting from 1
- **Unique**: Line numbers must be unique within a claim
- **No Gaps**: No gaps allowed in sequence (1, 2, 3, not 1, 3, 5)
- **Immutable**: Line numbers don't change after creation

### 2. Date Rules

- **Service Date Range**: Line service date must be within claim date range
- **Date Written**: Prescription date must be before or equal to service date
- **Timely Filing**: Lines must be submitted within payer deadlines (typically 90-365 days)

### 3. Financial Rules

- **Amount Hierarchy**: `billed_amount >= allowed_amount >= paid_amount`
- **Patient Responsibility**: `patient_responsibility = copay + coinsurance + deductible`
- **Total Reconciliation**: `allowed_amount = paid_amount + patient_responsibility`
- **Non-Negative**: All financial amounts must be >= 0
- **Precision**: All amounts stored with 2 decimal places

### 4. Quantity Rules

- **Positive Values**: Quantity must be > 0
- **Reasonable Limits**: Quantity should align with days supply
  - 30-day supply: typically 30-90 units
  - 90-day supply: typically 90-270 units
- **Metric Quantity**: Use standard units (tablets, capsules, ml, grams)

### 5. Days Supply Rules

- **Standard Values**: Common values are 7, 14, 30, 60, 90 days
- **Maximum**: Typically capped at 90 days for retail, 90-100 for mail order
- **Minimum**: At least 1 day
- **Alignment**: Should align with quantity dispensed

### 6. Procedure Code Rules

- **Valid Codes**: Must be valid CPT/HCPCS code for service date
- **NDC Required**: Pharmacy claims require valid NDC code
- **Format**: NDC must be 11-digit format (5-4-2)

### 7. Provider Rules

- **Valid NPI**: All NPIs must be valid 10-digit numbers
- **Active Providers**: Providers must be active on service date
- **Credentials**: Rendering provider must be credentialed for service type

---

## Validation Rules

### Pre-Adjudication Validation

1. **Required Fields Check**
   - claim_id, line_number, service_date, ndc, quantity, days_supply
   - billed_amount, rendering_provider_npi

2. **Format Validation**
   - NDC: 11 digits (5-4-2 format)
   - NPI: 10 digits
   - Dates: Valid date format
   - Amounts: Numeric with 2 decimals

3. **Range Validation**
   - Quantity: 1 to 9999
   - Days Supply: 1 to 100
   - Amounts: 0.00 to 999999.99

4. **Reference Data Validation**
   - claim_id exists in claim table
   - NDC exists in drug table
   - Provider NPI exists in provider table

### Post-Adjudication Validation

1. **Financial Reconciliation**
   - Sum of line amounts = claim header amount
   - Patient responsibility + paid amount = allowed amount

2. **Status Consistency**
   - If claim denied, all lines must be denied
   - If claim approved, at least one line must be approved

---

## Adjudication Rules

### Line-Level Adjudication Process

1. **Eligibility Check**
   - Member active on service date
   - Coverage effective for service

2. **Formulary Check**
   - Drug on formulary
   - Tier determination
   - Prior authorization required?

3. **Quantity Limits**
   - Check quantity limits per formulary
   - Check days supply limits
   - Refill too soon check

4. **Pricing Calculation**
   - Determine allowed amount (AWP, MAC, etc.)
   - Calculate patient responsibility
   - Apply copay/coinsurance
   - Apply deductible if applicable

5. **Clinical Edits**
   - Drug-drug interactions
   - Duplicate therapy
   - Age/gender appropriateness
   - Pregnancy contraindications

6. **Payment Determination**
   - Calculate plan payment
   - Apply coordination of benefits
   - Apply maximum benefit limits

### Denial Reasons

Common line-level denial codes:
- **70**: Product not covered
- **75**: Prior authorization required
- **76**: Plan limitations exceeded
- **79**: Refill too soon
- **85**: Patient not covered
- **88**: DUR reject
- **M4**: Missing/invalid NDC
- **M6**: Missing/invalid quantity

---

## Data Generation

### Generation Strategy

For realistic simulation, claim lines should be generated with:

1. **Distribution Patterns**
   - 85% single-line claims (one medication)
   - 12% two-line claims (two medications)
   - 3% three or more lines (multiple medications)

2. **Typical Scenarios**
   - **Maintenance Medications**: 30-90 day supplies, tier 1-2
   - **Acute Medications**: 7-14 day supplies, tier 1-3
   - **Specialty Medications**: 30 day supplies, tier 4-5, high cost
   - **Compound Medications**: Multiple ingredients, custom pricing

3. **Financial Patterns**
   - **Tier 1 (Generic)**: $5-$15 copay, 60% of claims
   - **Tier 2 (Preferred Brand)**: $15-$35 copay, 25% of claims
   - **Tier 3 (Non-Preferred)**: $35-$70 copay, 10% of claims
   - **Tier 4 (Specialty)**: 30% coinsurance, 4% of claims
   - **Tier 5 (High-Cost Specialty)**: 30% coinsurance, 1% of claims

4. **Status Distribution**
   - 87% approved
   - 10% denied
   - 2% pending
   - 1% adjusted

### Data Volume

For 1 million claims:
- Estimated 1.2 million claim lines (1.2 lines per claim average)
- File size: ~40-50 MB per 100,000 lines
- Recommended: Split into files of 30 MB each (~75,000 lines per file)

### CSV File Format

```csv
claim_line_id,claim_id,line_number,service_date,ndc,drug_name,quantity_dispensed,days_supply,unit_of_measure,rendering_provider_npi,billed_amount,allowed_amount,paid_amount,patient_responsibility,copay_amount,coinsurance_amount,deductible_amount,ingredient_cost,dispensing_fee,sales_tax,line_status,tier_level,daw_code,generic_indicator,prescription_number,refill_number,date_written,created_at
```

### Foreign Key Considerations

When generating claim lines:
1. **claim_id** must reference existing claims
2. **ndc** should reference existing drugs
3. **service_date** must match or be within claim date range
4. **Line numbers** must be sequential per claim
5. **Financial amounts** must reconcile with claim header

---

## Implementation Notes

### Database Indexes

Recommended indexes for performance:
```sql
CREATE INDEX idx_claim_line_claim_id ON claim_line(claim_id);
CREATE INDEX idx_claim_line_service_date ON claim_line(service_date);
CREATE INDEX idx_claim_line_ndc ON claim_line(ndc);
CREATE INDEX idx_claim_line_status ON claim_line(line_status);
CREATE INDEX idx_claim_line_claim_line ON claim_line(claim_id, line_number);
```

### Constraints

```sql
ALTER TABLE claim_line 
  ADD CONSTRAINT fk_claim_line_claim 
  FOREIGN KEY (claim_id) REFERENCES claim(claim_id) ON DELETE CASCADE;

ALTER TABLE claim_line
  ADD CONSTRAINT chk_line_number_positive
  CHECK (line_number > 0);

ALTER TABLE claim_line
  ADD CONSTRAINT chk_amounts_non_negative
  CHECK (billed_amount >= 0 AND allowed_amount >= 0 AND paid_amount >= 0);
```

### Performance Considerations

- Partition large claim_line tables by service_date
- Archive claim lines older than 7 years
- Use bulk insert for data loading
- Index foreign keys for join performance

---

## References

- **NCPDP Telecommunication Standard**: Pharmacy claim format
- **HIPAA 837**: Electronic claim submission standard
- **CMS-1500**: Professional claim form
- **NDC Directory**: National Drug Code database
- **HCPCS/CPT Codes**: Procedure code standards

---

## Version History

- **v1.0** (2024-11-10): Initial documentation
  - Defined core data model
  - Established business rules
  - Documented validation requirements
  - Specified data generation patterns