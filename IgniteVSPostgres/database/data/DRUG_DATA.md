# US Healthcare Drug Data - Synthetic Data Documentation

**Last Updated:** 2025-11-07  
**Data File:** `us_pharmacy_drugs.csv`  
**Total Records:** 20,000 drugs  
**Generator Script:** `../scripts/generate_drugs.py`

---

## Table of Contents

1. [Overview](#overview)
2. [US Healthcare Drug System](#us-healthcare-drug-system)
3. [NDC (National Drug Code)](#ndc-national-drug-code)
4. [Data Generation Rules](#data-generation-rules)
5. [Drug Classification](#drug-classification)
6. [Therapeutic Categories](#therapeutic-categories)
7. [Pricing Structure](#pricing-structure)
8. [Data Schema](#data-schema)
9. [Generation Process](#generation-process)
10. [Usage Examples](#usage-examples)
11. [Validation Rules](#validation-rules)

---

## Overview

This document describes the synthetic drug data generated for the PBM (Pharmacy Benefit Management) system. The data represents 20,000 realistic US healthcare drug products following FDA standards and industry practices.

### Key Statistics

- **Total Drugs:** 20,000
- **Generic Drugs:** ~15,000 (75%)
- **Brand Drugs:** ~4,600 (23%)
- **Specialty Drugs:** ~400 (2%)
- **Controlled Substances:** ~3,000 (15%)
- **Therapeutic Categories:** 14 major categories
- **File Size:** ~4-5 MB

---

## US Healthcare Drug System

### What is a Drug Product?

In the US healthcare system, a **drug product** is a specific formulation of a medication identified by:

1. **NDC (National Drug Code)** - Unique identifier (10-11 digits, stored as 14 characters with hyphens)
2. **Active Ingredient** - Chemical entity that provides therapeutic effect
3. **Strength** - Amount of active ingredient (e.g., 10mg, 500mg)
4. **Dosage Form** - Physical form (tablet, capsule, injection, etc.)
5. **Manufacturer** - Company that produces the drug

### Total Drug Count in US

- **~150,000 NDC codes** in FDA database (includes discontinued)
- **~20,000-30,000 actively marketed drugs**
- **~1,500-2,000 unique active ingredients**

### Drug Lifecycle

```
1. FDA Approval → 2. Patent Protection → 3. Generic Entry → 4. Market Competition
   (Brand Only)      (Brand Exclusive)      (Generic Available)  (Multiple Generics)
```

---

## NDC (National Drug Code)

### NDC Format

The FDA assigns a unique **NDC** to every drug product. The NDC consists of 10-11 digits, typically formatted with hyphens:

**Format:** `XXXXX-XXXX-XX` (5-4-2 configuration, most common)

```
NDC: 00002-1234-01
     │     │    │
     │     │    └─ Package Code (2 digits)
     │     └────── Product Code (4 digits)
     └──────────── Labeler Code (5 digits)
```

**Components:**
1. **Labeler Code (5 digits):** Identifies the manufacturer/distributor
2. **Product Code (4 digits):** Identifies the specific drug formulation
3. **Package Code (2 digits):** Identifies the package size/type

**Note:** NDC codes can also use 4-4-2 or 5-3-2 configurations. In our database, we store NDCs as VARCHAR(14) to accommodate all formats with hyphens (e.g., "00002-1234-01" = 14 characters).

### NDC Examples

```
00002-1234-01  →  Eli Lilly, Prozac 20mg capsules, bottle of 30
00378-5678-93  →  Mylan, Lisinopril 10mg tablets, bottle of 90
00069-9876-30  →  Pfizer, Lipitor 40mg tablets, bottle of 30
```

### NDC Variations

The same drug may have multiple NDCs for:
- Different package sizes (30 vs 90 tablets)
- Different manufacturers (brand vs generic)
- Different strengths (10mg vs 20mg)

---

## Data Generation Rules

### 1. Drug Type Distribution

The 20,000 drugs are distributed by type based on real-world market:

| Drug Type | Count | Percentage | Description |
|-----------|-------|------------|-------------|
| Generic | 15,000 | 75% | Off-patent, multiple manufacturers |
| Brand | 4,600 | 23% | Patented, single manufacturer |
| Specialty | 400 | 2% | High-cost, complex administration |
| **TOTAL** | **20,000** | **100%** | |

**Rationale:**
- Generic drugs dominate by **volume** (80% of prescriptions)
- Brand drugs dominate by **cost** (higher prices)
- Specialty drugs are small by volume but **50% of total drug spend**

### 2. Therapeutic Category Distribution

Drugs are distributed across 14 major therapeutic categories:

| Category | Percentage | Drug Count | Examples |
|----------|------------|------------|----------|
| CARDIOVASCULAR | 15% | 3,000 | Blood pressure, cholesterol, heart |
| ANTIBIOTICS | 12% | 2,400 | Bacterial infections |
| CNS | 12% | 2,400 | Depression, anxiety, pain, seizures |
| DIABETES | 8% | 1,600 | Insulin, oral diabetes medications |
| RESPIRATORY | 8% | 1,600 | Asthma, COPD, allergies |
| GASTROINTESTINAL | 7% | 1,400 | Acid reflux, nausea, constipation |
| PAIN_MANAGEMENT | 10% | 2,000 | NSAIDs, opioids, muscle relaxants |
| ONCOLOGY | 5% | 1,000 | Cancer treatments |
| IMMUNOLOGY | 5% | 1,000 | Autoimmune, biologics |
| ENDOCRINE | 4% | 800 | Thyroid, hormones |
| DERMATOLOGY | 4% | 800 | Skin conditions |
| OPHTHALMOLOGY | 3% | 600 | Eye conditions |
| UROLOGY | 2% | 400 | Prostate, bladder |
| OTHER | 5% | 1,000 | Vitamins, antivirals, etc. |

### 3. Dosage Form Distribution

| Dosage Form | Percentage | Description |
|-------------|------------|-------------|
| TABLET | 40% | Solid oral dosage, most common |
| CAPSULE | 25% | Gelatin shell with powder/liquid |
| SOLUTION | 10% | Liquid for oral or injection |
| INJECTION | 8% | IV, IM, or subcutaneous |
| CREAM | 5% | Topical semi-solid |
| OINTMENT | 4% | Topical greasy base |
| SUSPENSION | 3% | Liquid with particles |
| PATCH | 2% | Transdermal delivery |
| INHALER | 2% | Respiratory delivery |
| SUPPOSITORY | 1% | Rectal or vaginal |

### 4. Route of Administration

| Route | Percentage | Description |
|-------|------------|-------------|
| ORAL | 70% | By mouth (tablets, capsules, liquids) |
| TOPICAL | 10% | Applied to skin |
| INJECTION | 8% | IV, IM, subcutaneous |
| INHALATION | 5% | Breathed into lungs |
| OPHTHALMIC | 3% | Eye drops/ointments |
| OTIC | 2% | Ear drops |
| RECTAL | 1% | Suppositories |
| TRANSDERMAL | 1% | Patches through skin |

### 5. Controlled Substances

**15% of drugs are controlled substances** (DEA scheduled):

| DEA Schedule | Percentage | Description | Examples |
|--------------|------------|-------------|----------|
| Schedule II | 30% | High abuse potential | Oxycodone, Adderall, Fentanyl |
| Schedule III | 25% | Moderate abuse potential | Codeine combinations, Testosterone |
| Schedule IV | 35% | Low abuse potential | Benzodiazepines, Tramadol |
| Schedule V | 10% | Lowest abuse potential | Cough preparations with codeine |

**Controlled Drug Classes:**
- Opioids (pain management)
- Stimulants (ADHD)
- Anxiolytics (anxiety - benzodiazepines)
- Muscle relaxants

---

## Drug Classification

### Generic vs Brand

#### Generic Drugs (75%)
- **Off-patent medications**
- Multiple manufacturers can produce
- Same active ingredient as brand
- Bioequivalent to brand (FDA approved)
- **Lower cost** (typically 80-85% less than brand)
- **Examples:** Lisinopril, Metformin, Atorvastatin

#### Brand Drugs (23%)
- **Patented medications**
- Single manufacturer (patent holder)
- Original innovator drug
- **Higher cost** (premium pricing)
- **Examples:** Humira, Eliquis, Januvia

#### Specialty Drugs (2%)
- **High-cost medications** (>$1,000/month)
- Complex administration (often injectable)
- Require special handling/storage
- Treat complex/rare conditions
- **50% of total drug spend** despite low volume
- **Examples:** Biologics, cancer drugs, gene therapies

### Drug Naming

#### Generic Name (INN - International Nonproprietary Name)
- Scientific name of active ingredient
- Lowercase (e.g., lisinopril, metformin)
- Same worldwide
- Required on all prescriptions

#### Brand Name (Trade Name)
- Marketing name chosen by manufacturer
- Capitalized (e.g., Prinivil, Glucophage)
- Can vary by country
- Protected by trademark

**Example:**
```
Generic Name: atorvastatin
Brand Names: Lipitor (Pfizer), Torvast (Pfizer - Europe)
```

---

## Therapeutic Categories

### 1. CARDIOVASCULAR (15% - 3,000 drugs)

**Drug Classes:**
- **ACE Inhibitors:** Lower blood pressure (lisinopril, enalapril)
- **Beta Blockers:** Slow heart rate (metoprolol, atenolol)
- **Calcium Channel Blockers:** Relax blood vessels (amlodipine, diltiazem)
- **Diuretics:** Remove excess fluid (hydrochlorothiazide, furosemide)
- **Statins:** Lower cholesterol (atorvastatin, simvastatin)
- **Anticoagulants:** Prevent blood clots (warfarin, apixaban)

**Common Conditions:** Hypertension, heart failure, atrial fibrillation, high cholesterol

### 2. ANTIBIOTICS (12% - 2,400 drugs)

**Drug Classes:**
- **Penicillins:** Broad-spectrum (amoxicillin, ampicillin)
- **Cephalosporins:** Similar to penicillins (cephalexin, ceftriaxone)
- **Macrolides:** Alternative to penicillins (azithromycin, clarithromycin)
- **Fluoroquinolones:** Broad-spectrum (ciprofloxacin, levofloxacin)
- **Tetracyclines:** Broad-spectrum (doxycycline, minocycline)

**Common Conditions:** Bacterial infections, pneumonia, UTIs, skin infections

### 3. CNS - Central Nervous System (12% - 2,400 drugs)

**Drug Classes:**
- **Antidepressants:** SSRIs, SNRIs (sertraline, escitalopram, duloxetine)
- **Antipsychotics:** Schizophrenia, bipolar (risperidone, quetiapine)
- **Anxiolytics:** Benzodiazepines (alprazolam, lorazepam)
- **Anticonvulsants:** Seizures (levetiracetam, gabapentin)
- **Stimulants:** ADHD (amphetamine, methylphenidate)
- **Opioids:** Pain (oxycodone, hydrocodone, morphine)

**Common Conditions:** Depression, anxiety, ADHD, epilepsy, chronic pain

### 4. DIABETES (8% - 1,600 drugs)

**Drug Classes:**
- **Insulin:** Injectable (insulin glargine, insulin lispro)
- **Metformin:** First-line oral (metformin)
- **Sulfonylureas:** Stimulate insulin (glipizide, glyburide)
- **DPP-4 Inhibitors:** Incretin-based (sitagliptin, linagliptin)
- **GLP-1 Agonists:** Injectable (semaglutide, liraglutide)
- **SGLT2 Inhibitors:** Kidney-based (empagliflozin, dapagliflozin)

**Common Conditions:** Type 1 diabetes, Type 2 diabetes

### 5. RESPIRATORY (8% - 1,600 drugs)

**Drug Classes:**
- **Bronchodilators:** Open airways (albuterol, salmeterol)
- **Corticosteroids:** Reduce inflammation (fluticasone, budesonide)
- **Antihistamines:** Allergies (cetirizine, loratadine)
- **Decongestants:** Nasal congestion (pseudoephedrine, phenylephrine)

**Common Conditions:** Asthma, COPD, allergies, hay fever

### 6. GASTROINTESTINAL (7% - 1,400 drugs)

**Drug Classes:**
- **PPIs:** Proton pump inhibitors (omeprazole, pantoprazole)
- **H2 Blockers:** Acid reducers (ranitidine, famotidine)
- **Antacids:** Neutralize acid (calcium carbonate, magnesium hydroxide)
- **Antiemetics:** Nausea (ondansetron, metoclopramide)
- **Laxatives:** Constipation (docusate, polyethylene glycol)

**Common Conditions:** GERD, ulcers, nausea, constipation, diarrhea

### 7. PAIN_MANAGEMENT (10% - 2,000 drugs)

**Drug Classes:**
- **NSAIDs:** Non-steroidal anti-inflammatory (ibuprofen, naproxen)
- **Opioids:** Narcotic pain relievers (oxycodone, hydrocodone)
- **Muscle Relaxants:** Spasm relief (cyclobenzaprine, baclofen)
- **Topical Analgesics:** Local pain relief (lidocaine, capsaicin)

**Common Conditions:** Acute pain, chronic pain, arthritis, back pain

### 8. ONCOLOGY (5% - 1,000 drugs)

**Drug Classes:**
- **Chemotherapy:** Cytotoxic agents (doxorubicin, cisplatin)
- **Targeted Therapy:** Specific cancer pathways (imatinib, erlotinib)
- **Immunotherapy:** Immune system activation (pembrolizumab, nivolumab)
- **Hormone Therapy:** Hormone-sensitive cancers (tamoxifen, letrozole)

**Common Conditions:** Various cancers (breast, lung, colon, leukemia)

### 9. IMMUNOLOGY (5% - 1,000 drugs)

**Drug Classes:**
- **Immunosuppressants:** Prevent rejection (tacrolimus, cyclosporine)
- **Biologics:** Monoclonal antibodies (adalimumab, infliximab)
- **Corticosteroids:** Systemic inflammation (prednisone, methylprednisolone)

**Common Conditions:** Rheumatoid arthritis, Crohn's disease, transplant rejection

### 10. ENDOCRINE (4% - 800 drugs)

**Drug Classes:**
- **Thyroid:** Hypothyroidism (levothyroxine)
- **Hormone Replacement:** Menopause (estrogen, progesterone)
- **Osteoporosis:** Bone density (alendronate, denosumab)

**Common Conditions:** Hypothyroidism, menopause, osteoporosis

### 11-14. Other Categories

- **DERMATOLOGY (4%):** Topical steroids, antifungals, acne treatments
- **OPHTHALMOLOGY (3%):** Glaucoma, eye infections, inflammation
- **UROLOGY (2%):** BPH, overactive bladder, erectile dysfunction
- **OTHER (5%):** Vitamins, antivirals, antiparasitics

---

## Pricing Structure

### AWP (Average Wholesale Price)

**Most common pricing benchmark** in PBM systems:
- Published by pricing compendia (Medi-Span, First DataBank)
- Typically **20-25% above actual acquisition cost**
- Used for reimbursement calculations
- Updated regularly (monthly or quarterly)

**Formula:**
```
Pharmacy Reimbursement = AWP - Discount% + Dispensing Fee
Example: $100 AWP - 15% + $2.50 = $87.50
```

### WAC (Wholesale Acquisition Cost)

**Manufacturer's list price:**
- Price before discounts/rebates
- Typically **80-85% of AWP**
- Used for specialty drugs
- More transparent than AWP

### MAC (Maximum Allowable Cost)

**Used for generic drugs:**
- Sets maximum reimbursement amount
- Prevents overpayment for generics
- Typically **70-80% of AWP**
- Updated frequently based on market prices

### Pricing by Drug Type

| Drug Type | AWP Range | WAC Range | MAC Range |
|-----------|-----------|-----------|-----------|
| Generic | $0.10 - $50 | $0.08 - $42 | $0.07 - $35 |
| Brand | $10 - $500 | $8 - $410 | N/A |
| Specialty | $1,000 - $10,000 | $800 - $8,000 | N/A |

**Note:** Specialty drugs can exceed $10,000 per dose (e.g., gene therapies: $1-2 million per treatment)

---

## Data Schema

### CSV File Structure

```csv
ndc_code,drug_name,generic_name,strength,dosage_form,route,manufacturer,drug_class,therapeutic_category,is_generic,is_brand,is_specialty,is_controlled,dea_schedule,awp_price,wac_price,mac_price,package_size,package_unit,fda_approval_date,is_active
```

### Field Definitions

| Field | Type | Description | Example |
|-------|------|-------------|---------|
| `ndc_code` | VARCHAR(14) | Unique NDC identifier with hyphens | 00002-1234-01 |
| `drug_name` | VARCHAR(200) | Brand or generic name | Lipitor |
| `generic_name` | VARCHAR(200) | Active ingredient name | atorvastatin |
| `strength` | VARCHAR(50) | Drug strength | 40mg |
| `dosage_form` | VARCHAR(50) | Physical form | TABLET |
| `route` | VARCHAR(50) | Administration route | ORAL |
| `manufacturer` | VARCHAR(100) | Manufacturer name | PFIZER |
| `drug_class` | VARCHAR(100) | Pharmacological class | STATIN |
| `therapeutic_category` | VARCHAR(100) | Therapeutic category | CARDIOVASCULAR |
| `is_generic` | BOOLEAN | Generic drug flag | true |
| `is_brand` | BOOLEAN | Brand drug flag | false |
| `is_specialty` | BOOLEAN | Specialty drug flag | false |
| `is_controlled` | BOOLEAN | Controlled substance flag | false |
| `dea_schedule` | VARCHAR(5) | DEA schedule (II-V) | II |
| `awp_price` | DECIMAL(10,2) | Average Wholesale Price | 125.50 |
| `wac_price` | DECIMAL(10,2) | Wholesale Acquisition Cost | 103.25 |
| `mac_price` | DECIMAL(10,2) | Maximum Allowable Cost | 87.85 |
| `package_size` | INTEGER | Units per package | 30 |
| `package_unit` | VARCHAR(20) | Unit type | EA (each) |
| `fda_approval_date` | DATE | FDA approval date | 1996-12-17 |
| `is_active` | BOOLEAN | Currently marketed | true |

---

## Generation Process

### Step-by-Step Process

1. **Determine Drug Type**
   - Random selection based on distribution (75% generic, 23% brand, 2% specialty)
   - Sets pricing range and characteristics

2. **Select Therapeutic Category**
   - Weighted random selection from 14 categories
   - Determines drug class within category

3. **Generate Names**
   - Generic name: Combine prefix + suffix (e.g., "am" + "pril" = "ampril")
   - Brand name: Combine brand prefix + suffix (e.g., "Cardio" + "max" = "Cardiomax")

4. **Select Physical Characteristics**
   - Dosage form (weighted random)
   - Route of administration (based on form)
   - Strength (from predefined ranges per form)
   - Package size (based on dosage form)

5. **Assign Manufacturer**
   - Generic drugs: Generic manufacturers (Teva, Mylan, etc.)
   - Brand drugs: Innovator companies (Pfizer, Merck, etc.)

6. **Generate NDC Code**
   - Manufacturer code: Random 5 digits
   - Product code: Sequence number
   - Package code: Random 2 digits
   - Format: XXXXX-XXXX-XX

7. **Calculate Pricing**
   - AWP: Based on drug type and form
   - WAC: 80-85% of AWP
   - MAC: 70-80% of AWP (generics only)

8. **Determine Controlled Status**
   - Based on drug class (opioids, stimulants, etc.)
   - Assign DEA schedule if controlled

9. **Set FDA Approval Date**
   - Specialty: Recent (2010-2024)
   - Generic: Older (1980-2024)
   - Brand: Mixed (1990-2024)

10. **Set Active Status**
    - 98% active (currently marketed)
    - 2% inactive (discontinued)

### Script Usage

```bash
cd database/scripts

# Run the generator
python3 generate_drugs.py

# Output
# - Creates: ../data/us_pharmacy_drugs.csv
# - Size: ~4-5 MB
# - Records: 20,000 drugs
```

---

## Usage Examples

### Example 1: Generic Cardiovascular Drug

```csv
12345-0001-01,Ampril,ampril,10mg,TABLET,ORAL,TEVA,ACE_INHIBITOR,CARDIOVASCULAR,true,false,false,false,,12.50,10.63,8.75,90,EA,1995-03-15,true
```

**Interpretation:**
- Generic ACE inhibitor (blood pressure medication)
- 10mg tablet, oral administration
- Manufactured by Teva (generic manufacturer)
- AWP: $12.50, WAC: $10.63, MAC: $8.75
- Package of 90 tablets
- FDA approved in 1995
- Currently active

### Example 2: Brand Specialty Drug

```csv
67890-5000-01,Biomax,biomax,25mg/ml,INJECTION,INJECTION,AMGEN,BIOLOGIC,IMMUNOLOGY,false,true,true,false,,5250.00,4200.00,,1,EA,2018-06-20,true
```

**Interpretation:**
- Brand specialty biologic (immunology)
- 25mg/ml injection
- Manufactured by Amgen (innovator)
- AWP: $5,250, WAC: $4,200, No MAC (specialty)
- Single dose vial
- FDA approved in 2018
- Currently active

### Example 3: Controlled Substance (Opioid)

```csv
23456-2500-30,Oxypro,oxycodone,10mg,TABLET,ORAL,PURDUE,OPIOID,PAIN_MANAGEMENT,false,true,false,true,II,45.00,37.80,,30,EA,1996-01-10,true
```

**Interpretation:**
- Brand opioid pain medication
- 10mg tablet, oral administration
- DEA Schedule II (high abuse potential)
- AWP: $45.00, WAC: $37.80
- Package of 30 tablets
- FDA approved in 1996
- Currently active

---

## Validation Rules

### Data Integrity Checks

1. **Unique NDC Codes**
   - Each ndc_code must be unique
   - Format: XXXXX-XXXX-XX (10-11 digits with hyphens, stored as VARCHAR(14))
   - Common formats: 5-4-2, 4-4-2, or 5-3-2

2. **Drug Type Consistency**
   - Exactly one of is_generic, is_brand must be true
   - is_specialty can be true with is_brand

3. **Pricing Consistency**
   - AWP > WAC (WAC typically 80-85% of AWP)
   - MAC < AWP (MAC typically 70-80% of AWP)
   - MAC only for generic drugs
   - Specialty drugs: AWP > $1,000

4. **Controlled Substance Rules**
   - If is_controlled = true, dea_schedule must be II, III, IV, or V
   - If is_controlled = false, dea_schedule must be empty
   - Only certain drug classes can be controlled

5. **Date Validation**
   - fda_approval_date must be between 1980-01-01 and 2024-12-31
   - Specialty drugs typically approved after 2010

6. **Package Size Reasonableness**
   - Tablets/Capsules: 30, 60, 90, 100, 500
   - Liquids: 100-480 ml
   - Injections: 1-25 units

### SQL Validation Queries

```sql
-- Check total count
SELECT COUNT(*) FROM drug;
-- Expected: 20,000

-- Check for duplicate NDCs
SELECT ndc_code, COUNT(*) 
FROM drug 
GROUP BY ndc_code 
HAVING COUNT(*) > 1;
-- Expected: 0 rows

-- Check drug type distribution
SELECT 
    SUM(CASE WHEN is_generic THEN 1 ELSE 0 END) as generic_count,
    SUM(CASE WHEN is_brand THEN 1 ELSE 0 END) as brand_count,
    SUM(CASE WHEN is_specialty THEN 1 ELSE 0 END) as specialty_count
FROM drug;
-- Expected: ~15,000 generic, ~4,600 brand, ~400 specialty

-- Check therapeutic category distribution
SELECT therapeutic_category, COUNT(*) as count,
       ROUND(COUNT(*) * 100.0 / 20000, 1) as percentage
FROM drug
GROUP BY therapeutic_category
ORDER BY count DESC;

-- Check controlled substances
SELECT COUNT(*) as controlled_count,
       ROUND(COUNT(*) * 100.0 / 20000, 1) as percentage
FROM drug
WHERE is_controlled = true;
-- Expected: ~3,000 (15%)

-- Check pricing consistency
SELECT COUNT(*) as invalid_pricing
FROM drug
WHERE wac_price > awp_price
   OR (mac_price IS NOT NULL AND mac_price > awp_price);
-- Expected: 0 (all pricing should be consistent)

-- Check specialty drug pricing
SELECT COUNT(*) as specialty_drugs,
       MIN(awp_price) as min_price,
       MAX(awp_price) as max_price,
       AVG(awp_price) as avg_price
FROM drug
WHERE is_specialty = true;
-- Expected: min > $1,000

-- Check active vs inactive
SELECT is_active, COUNT(*) as count,
       ROUND(COUNT(*) * 100.0 / 20000, 1) as percentage
FROM drug
GROUP BY is_active;
-- Expected: ~98% active, ~2% inactive
```

---

## Future Enhancements

### Potential Improvements

1. **Drug Interactions**
   - Create `drug_interaction` table
   - Link drugs with interaction severity
   - Include clinical significance

2. **Therapeutic Equivalence**
   - Link generic drugs to brand equivalents
   - AB-rated generics (bioequivalent)
   - Therapeutic alternatives

3. **Indications and Contraindications**
   - Add approved indications (what drug treats)
   - Add contraindications (when not to use)
   - Link to ICD-10 diagnosis codes

4. **Dosing Information**
   - Typical adult dose
   - Pediatric dosing
   - Renal/hepatic adjustments

5. **Multi-Source Drugs**
   - Link multiple NDCs for same drug
   - Different manufacturers
   - Different package sizes

6. **Historical Pricing**
   - Track price changes over time
   - Identify price trends
   - Support cost analysis

---

## References

### Industry Standards

- **FDA National Drug Code Directory**: https://www.fda.gov/drugs/drug-approvals-and-databases/national-drug-code-directory
- **DEA Controlled Substances**: https://www.dea.gov/drug-information/drug-scheduling
- **Medi-Span Drug Database**: https://www.wolterskluwer.com/en/solutions/medi-span
- **First DataBank**: https://www.fdbhealth.com/

### Pricing References

- **AWP Pricing**: Industry standard benchmark
- **MAC Lists**: State Medicaid programs publish MAC lists
- **340B Drug Pricing**: Federal ceiling prices for safety-net providers

---

**Document Version:** 1.0  
**Last Updated:** 2025-11-07  
**Maintained By:** PBM System Development Team