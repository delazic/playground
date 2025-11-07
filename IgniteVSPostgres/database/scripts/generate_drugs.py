#!/usr/bin/env python3
"""
Generate realistic US healthcare drug data for PBM system testing.

This script generates 20,000 drug products following FDA and US healthcare standards:
- NDC (National Drug Code) format
- Generic and brand name drugs
- Multiple therapeutic categories
- Realistic pricing (AWP, WAC, MAC)
- Specialty drugs
- Controlled substances

Output: Single CSV file with 20,000 drug records
"""

import csv
import random
from datetime import datetime, timedelta
from pathlib import Path

# Configuration
TOTAL_DRUGS = 20000
OUTPUT_DIR = Path("../data")
OUTPUT_FILE = "us_pharmacy_drugs.csv"

# Drug type distribution
GENERIC_PCT = 0.75        # 75% generic drugs
BRAND_PCT = 0.23          # 23% brand drugs
SPECIALTY_PCT = 0.02      # 2% specialty drugs (high-cost)

# Controlled substance distribution (DEA schedules)
CONTROLLED_PCT = 0.15     # 15% are controlled substances
DEA_SCHEDULES = {
    'II': 0.30,   # 30% - High potential for abuse (opioids, stimulants)
    'III': 0.25,  # 25% - Moderate potential (codeine combinations)
    'IV': 0.35,   # 35% - Low potential (benzodiazepines)
    'V': 0.10     # 10% - Lowest potential (cough preparations)
}

# Dosage forms
DOSAGE_FORMS = [
    ('TABLET', 0.40),
    ('CAPSULE', 0.25),
    ('SOLUTION', 0.10),
    ('INJECTION', 0.08),
    ('CREAM', 0.05),
    ('OINTMENT', 0.04),
    ('SUSPENSION', 0.03),
    ('PATCH', 0.02),
    ('INHALER', 0.02),
    ('SUPPOSITORY', 0.01)
]

# Routes of administration
ROUTES = [
    ('ORAL', 0.70),
    ('TOPICAL', 0.10),
    ('INJECTION', 0.08),
    ('INHALATION', 0.05),
    ('OPHTHALMIC', 0.03),
    ('OTIC', 0.02),
    ('RECTAL', 0.01),
    ('TRANSDERMAL', 0.01)
]

# Therapeutic categories (major drug classes)
THERAPEUTIC_CATEGORIES = {
    'CARDIOVASCULAR': {
        'weight': 0.15,
        'classes': ['ACE_INHIBITOR', 'BETA_BLOCKER', 'CALCIUM_CHANNEL_BLOCKER', 'DIURETIC', 'STATIN', 'ANTICOAGULANT']
    },
    'ANTIBIOTICS': {
        'weight': 0.12,
        'classes': ['PENICILLIN', 'CEPHALOSPORIN', 'MACROLIDE', 'FLUOROQUINOLONE', 'TETRACYCLINE']
    },
    'CNS': {
        'weight': 0.12,
        'classes': ['ANTIDEPRESSANT', 'ANTIPSYCHOTIC', 'ANXIOLYTIC', 'ANTICONVULSANT', 'STIMULANT', 'OPIOID']
    },
    'DIABETES': {
        'weight': 0.08,
        'classes': ['INSULIN', 'METFORMIN', 'SULFONYLUREA', 'DPP4_INHIBITOR', 'GLP1_AGONIST', 'SGLT2_INHIBITOR']
    },
    'RESPIRATORY': {
        'weight': 0.08,
        'classes': ['BRONCHODILATOR', 'CORTICOSTEROID', 'ANTIHISTAMINE', 'DECONGESTANT']
    },
    'GASTROINTESTINAL': {
        'weight': 0.07,
        'classes': ['PPI', 'H2_BLOCKER', 'ANTACID', 'ANTIEMETIC', 'LAXATIVE']
    },
    'PAIN_MANAGEMENT': {
        'weight': 0.10,
        'classes': ['NSAID', 'OPIOID', 'MUSCLE_RELAXANT', 'TOPICAL_ANALGESIC']
    },
    'ONCOLOGY': {
        'weight': 0.05,
        'classes': ['CHEMOTHERAPY', 'TARGETED_THERAPY', 'IMMUNOTHERAPY', 'HORMONE_THERAPY']
    },
    'IMMUNOLOGY': {
        'weight': 0.05,
        'classes': ['IMMUNOSUPPRESSANT', 'BIOLOGIC', 'CORTICOSTEROID']
    },
    'ENDOCRINE': {
        'weight': 0.04,
        'classes': ['THYROID', 'HORMONE_REPLACEMENT', 'OSTEOPOROSIS']
    },
    'DERMATOLOGY': {
        'weight': 0.04,
        'classes': ['TOPICAL_STEROID', 'ANTIFUNGAL', 'ACNE_TREATMENT', 'PSORIASIS_TREATMENT']
    },
    'OPHTHALMOLOGY': {
        'weight': 0.03,
        'classes': ['GLAUCOMA', 'ANTIBIOTIC_EYE', 'ANTI_INFLAMMATORY_EYE']
    },
    'UROLOGY': {
        'weight': 0.02,
        'classes': ['BPH_TREATMENT', 'OVERACTIVE_BLADDER', 'ED_TREATMENT']
    },
    'OTHER': {
        'weight': 0.05,
        'classes': ['VITAMIN', 'SUPPLEMENT', 'ANTIPARASITIC', 'ANTIVIRAL']
    }
}

# Major pharmaceutical manufacturers
MANUFACTURERS = [
    'PFIZER', 'JOHNSON_JOHNSON', 'ROCHE', 'NOVARTIS', 'MERCK',
    'SANOFI', 'GLAXOSMITHKLINE', 'ABBVIE', 'GILEAD', 'AMGEN',
    'ASTRAZENECA', 'BRISTOL_MYERS_SQUIBB', 'ELI_LILLY', 'BOEHRINGER_INGELHEIM',
    'BAYER', 'TEVA', 'MYLAN', 'SANDOZ', 'ACTAVIS', 'DR_REDDY',
    'SUN_PHARMA', 'LUPIN', 'CIPLA', 'AUROBINDO', 'ZYDUS'
]

# Common drug name components
GENERIC_PREFIXES = [
    'am', 'az', 'ben', 'cef', 'cip', 'clo', 'di', 'dox', 'en', 'flu',
    'gab', 'hyd', 'ibu', 'ket', 'lev', 'met', 'nap', 'om', 'par', 'pred',
    'pro', 'ris', 'ser', 'sim', 'tam', 'val', 'var', 'ven', 'war', 'zol'
]

GENERIC_SUFFIXES = [
    'azole', 'cillin', 'cycline', 'dipine', 'floxacin', 'olol', 'prazole',
    'pril', 'sartan', 'statin', 'tidine', 'triptan', 'vir', 'zepam', 'zolam'
]

BRAND_PREFIXES = [
    'Acti', 'Bio', 'Cardi', 'Derm', 'Endo', 'Flex', 'Gluco', 'Hepa',
    'Immuno', 'Keto', 'Lipo', 'Medi', 'Neuro', 'Onco', 'Phar', 'Rena',
    'Sero', 'Thera', 'Ultra', 'Vita', 'Xeno', 'Zymo'
]

BRAND_SUFFIXES = [
    'max', 'plus', 'pro', 'forte', 'xr', 'cr', 'sr', 'la', 'er', 'od',
    'hct', 'duo', 'tri', 'quad', 'plex', 'care', 'guard', 'shield'
]

# Strength ranges by dosage form
STRENGTH_RANGES = {
    'TABLET': ['5mg', '10mg', '20mg', '25mg', '40mg', '50mg', '100mg', '250mg', '500mg'],
    'CAPSULE': ['10mg', '20mg', '25mg', '40mg', '50mg', '75mg', '100mg', '150mg', '200mg'],
    'SOLUTION': ['5mg/5ml', '10mg/5ml', '25mg/5ml', '50mg/5ml', '100mg/5ml'],
    'INJECTION': ['10mg/ml', '25mg/ml', '50mg/ml', '100mg/ml', '250mg/ml'],
    'CREAM': ['0.1%', '0.5%', '1%', '2%', '5%'],
    'OINTMENT': ['0.1%', '0.5%', '1%', '2%', '5%'],
    'SUSPENSION': ['125mg/5ml', '250mg/5ml', '500mg/5ml'],
    'PATCH': ['12mcg/hr', '25mcg/hr', '50mcg/hr', '75mcg/hr', '100mcg/hr'],
    'INHALER': ['90mcg', '100mcg', '200mcg', '250mcg'],
    'SUPPOSITORY': ['25mg', '50mg', '100mg', '200mg']
}

# Package sizes
PACKAGE_SIZES = {
    'TABLET': [30, 60, 90, 100, 500],
    'CAPSULE': [30, 60, 90, 100, 500],
    'SOLUTION': [100, 120, 240, 480],
    'INJECTION': [1, 5, 10, 25],
    'CREAM': [15, 30, 45, 60],
    'OINTMENT': [15, 30, 45, 60],
    'SUSPENSION': [100, 150, 200],
    'PATCH': [5, 10, 30],
    'INHALER': [1, 2, 3],
    'SUPPOSITORY': [12, 24, 50]
}


def generate_ndc_code(manufacturer_code, product_code, package_code):
    """Generate an 11-digit NDC code in format XXXXX-XXXX-XX."""
    return f"{manufacturer_code:05d}-{product_code:04d}-{package_code:02d}"


def generate_generic_name():
    """Generate a realistic generic drug name."""
    prefix = random.choice(GENERIC_PREFIXES)
    suffix = random.choice(GENERIC_SUFFIXES)
    return f"{prefix}{suffix}"


def generate_brand_name():
    """Generate a realistic brand drug name."""
    prefix = random.choice(BRAND_PREFIXES)
    suffix = random.choice(BRAND_SUFFIXES)
    return f"{prefix}{suffix}"


def select_weighted_random(choices):
    """Select a random item based on weighted probabilities."""
    if isinstance(choices, dict):
        items = list(choices.keys())
        weights = list(choices.values())
    else:
        items = [item[0] for item in choices]
        weights = [item[1] for item in choices]
    return random.choices(items, weights=weights)[0]


def select_therapeutic_category():
    """Select a therapeutic category and drug class."""
    # Extract categories and their weights
    categories = list(THERAPEUTIC_CATEGORIES.keys())
    weights = [THERAPEUTIC_CATEGORIES[cat]['weight'] for cat in categories]
    
    # Select category based on weights
    category = random.choices(categories, weights=weights)[0]
    
    # Select random drug class from the category
    drug_class = random.choice(THERAPEUTIC_CATEGORIES[category]['classes'])
    
    return category, drug_class


def generate_pricing(is_generic, is_specialty, dosage_form):
    """Generate realistic drug pricing (AWP, WAC, MAC)."""
    if is_specialty:
        # Specialty drugs: $1,000 - $10,000 per unit
        awp = round(random.uniform(1000, 10000), 2)
        wac = round(awp * 0.80, 2)  # WAC typically 80% of AWP
        mac = None  # No MAC for specialty drugs
    elif is_generic:
        # Generic drugs: $0.10 - $50 per unit
        awp = round(random.uniform(0.10, 50.00), 2)
        wac = round(awp * 0.85, 2)
        mac = round(awp * 0.70, 2)  # MAC typically 70% of AWP for generics
    else:
        # Brand drugs: $10 - $500 per unit
        awp = round(random.uniform(10.00, 500.00), 2)
        wac = round(awp * 0.82, 2)
        mac = None  # No MAC for brand drugs
    
    return awp, wac, mac


def generate_fda_approval_date(is_generic, is_specialty):
    """Generate a realistic FDA approval date."""
    if is_specialty:
        # Specialty drugs: mostly recent (2010-2024)
        start_year = 2010
    elif is_generic:
        # Generic drugs: older approvals (1980-2024)
        start_year = 1980
    else:
        # Brand drugs: mix of old and new (1990-2024)
        start_year = 1990
    
    end_year = 2024
    year = random.randint(start_year, end_year)
    month = random.randint(1, 12)
    day = random.randint(1, 28)
    
    return f"{year}-{month:02d}-{day:02d}"


def is_controlled_substance(drug_class):
    """Determine if drug is a controlled substance based on class."""
    controlled_classes = ['OPIOID', 'STIMULANT', 'ANXIOLYTIC', 'MUSCLE_RELAXANT']
    return drug_class in controlled_classes


def generate_drug(sequence):
    """Generate a single drug record."""
    # Determine drug type
    rand = random.random()
    if rand < SPECIALTY_PCT:
        is_specialty = True
        is_generic = False
        is_brand = True
    elif rand < SPECIALTY_PCT + GENERIC_PCT:
        is_specialty = False
        is_generic = True
        is_brand = False
    else:
        is_specialty = False
        is_generic = False
        is_brand = True
    
    # Select therapeutic category and class
    category, drug_class = select_therapeutic_category()
    
    # Generate names
    generic_name = generate_generic_name()
    if is_brand:
        drug_name = generate_brand_name()
    else:
        drug_name = generic_name.capitalize()
    
    # Select dosage form and route
    dosage_form = select_weighted_random(DOSAGE_FORMS)
    route = select_weighted_random(ROUTES)
    
    # Select strength
    strength = random.choice(STRENGTH_RANGES.get(dosage_form, ['10mg']))
    
    # Select manufacturer
    if is_generic:
        manufacturer = random.choice(MANUFACTURERS[-10:])  # Generic manufacturers
    else:
        manufacturer = random.choice(MANUFACTURERS[:15])   # Brand manufacturers
    
    # Generate NDC code
    manufacturer_code = random.randint(1, 99999)
    product_code = sequence
    package_code = random.randint(1, 99)
    ndc_code = generate_ndc_code(manufacturer_code, product_code, package_code)
    
    # Package size
    package_size = random.choice(PACKAGE_SIZES.get(dosage_form, [30]))
    package_unit = 'EA' if dosage_form in ['TABLET', 'CAPSULE'] else 'ML'
    
    # Pricing
    awp, wac, mac = generate_pricing(is_generic, is_specialty, dosage_form)
    
    # Controlled substance
    is_controlled = is_controlled_substance(drug_class) and random.random() < CONTROLLED_PCT
    dea_schedule = select_weighted_random(DEA_SCHEDULES) if is_controlled else None
    
    # FDA approval date
    fda_approval_date = generate_fda_approval_date(is_generic, is_specialty)
    
    # Active status (98% active, 2% discontinued)
    is_active = random.random() < 0.98
    
    drug = {
        'ndc_code': ndc_code,
        'drug_name': drug_name,
        'generic_name': generic_name,
        'strength': strength,
        'dosage_form': dosage_form,
        'route': route,
        'manufacturer': manufacturer,
        'drug_class': drug_class,
        'therapeutic_category': category,
        'is_generic': str(is_generic).lower(),
        'is_brand': str(is_brand).lower(),
        'is_specialty': str(is_specialty).lower(),
        'is_controlled': str(is_controlled).lower(),
        'dea_schedule': dea_schedule if dea_schedule else '',
        'awp_price': f"{awp:.2f}",
        'wac_price': f"{wac:.2f}",
        'mac_price': f"{mac:.2f}" if mac else '',
        'package_size': package_size,
        'package_unit': package_unit,
        'fda_approval_date': fda_approval_date,
        'is_active': str(is_active).lower()
    }
    
    return drug


def write_drugs_to_csv(drugs, output_file):
    """Write drugs to a CSV file."""
    fieldnames = [
        'ndc_code', 'drug_name', 'generic_name', 'strength', 'dosage_form',
        'route', 'manufacturer', 'drug_class', 'therapeutic_category',
        'is_generic', 'is_brand', 'is_specialty', 'is_controlled', 'dea_schedule',
        'awp_price', 'wac_price', 'mac_price', 'package_size', 'package_unit',
        'fda_approval_date', 'is_active'
    ]
    
    with open(output_file, 'w', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(drugs)
    
    file_size_mb = output_file.stat().st_size / (1024 * 1024)
    print(f"  ✓ Created {output_file.name} ({len(drugs):,} drugs, {file_size_mb:.1f} MB)")


def generate_statistics(drugs):
    """Generate and display statistics about the drugs."""
    print("\n" + "=" * 80)
    print("Drug Statistics")
    print("=" * 80)
    
    # Count by type
    generic_count = sum(1 for d in drugs if d['is_generic'] == 'true')
    brand_count = sum(1 for d in drugs if d['is_brand'] == 'true')
    specialty_count = sum(1 for d in drugs if d['is_specialty'] == 'true')
    
    print("\nBy Drug Type:")
    print(f"  Generic:   {generic_count:6,d} ({(generic_count/len(drugs))*100:5.1f}%)")
    print(f"  Brand:     {brand_count:6,d} ({(brand_count/len(drugs))*100:5.1f}%)")
    print(f"  Specialty: {specialty_count:6,d} ({(specialty_count/len(drugs))*100:5.1f}%)")
    
    # Count by therapeutic category
    category_counts = {}
    for d in drugs:
        cat = d['therapeutic_category']
        category_counts[cat] = category_counts.get(cat, 0) + 1
    
    print("\nTop 10 Therapeutic Categories:")
    for cat, count in sorted(category_counts.items(), key=lambda x: x[1], reverse=True)[:10]:
        pct = (count / len(drugs)) * 100
        print(f"  {cat:25s}: {count:5,d} ({pct:5.1f}%)")
    
    # Count by dosage form
    form_counts = {}
    for d in drugs:
        form = d['dosage_form']
        form_counts[form] = form_counts.get(form, 0) + 1
    
    print("\nTop 5 Dosage Forms:")
    for form, count in sorted(form_counts.items(), key=lambda x: x[1], reverse=True)[:5]:
        pct = (count / len(drugs)) * 100
        print(f"  {form:15s}: {count:5,d} ({pct:5.1f}%)")
    
    # Controlled substances
    controlled_count = sum(1 for d in drugs if d['is_controlled'] == 'true')
    print(f"\nControlled Substances: {controlled_count:,d} ({(controlled_count/len(drugs))*100:5.1f}%)")
    
    # Active vs inactive
    active_count = sum(1 for d in drugs if d['is_active'] == 'true')
    inactive_count = len(drugs) - active_count
    print(f"\nActive Status:")
    print(f"  Active:   {active_count:6,d} ({(active_count/len(drugs))*100:5.1f}%)")
    print(f"  Inactive: {inactive_count:6,d} ({(inactive_count/len(drugs))*100:5.1f}%)")
    
    # Price statistics
    awp_prices = [float(d['awp_price']) for d in drugs]
    avg_awp = sum(awp_prices) / len(awp_prices)
    min_awp = min(awp_prices)
    max_awp = max(awp_prices)
    
    print(f"\nAWP Price Statistics:")
    print(f"  Average: ${avg_awp:,.2f}")
    print(f"  Minimum: ${min_awp:,.2f}")
    print(f"  Maximum: ${max_awp:,.2f}")


def generate_all_drugs():
    """Generate all drug records."""
    print("=" * 80)
    print("US Healthcare Drug Data Generator")
    print("=" * 80)
    print()
    print(f"Configuration:")
    print(f"  Total drugs: {TOTAL_DRUGS:,}")
    print(f"  Output directory: {OUTPUT_DIR}")
    print(f"  Output file: {OUTPUT_FILE}")
    print()
    print(f"Drug type distribution:")
    print(f"  Generic:   {GENERIC_PCT*100:5.1f}%")
    print(f"  Brand:     {BRAND_PCT*100:5.1f}%")
    print(f"  Specialty: {SPECIALTY_PCT*100:5.1f}%")
    print()
    
    # Create output directory if it doesn't exist
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    
    print("Generating drugs...")
    print("-" * 80)
    
    all_drugs = []
    for i in range(1, TOTAL_DRUGS + 1):
        drug = generate_drug(i)
        all_drugs.append(drug)
        
        # Progress indicator
        if i % 2000 == 0:
            print(f"  Generated {i:,} drugs...")
    
    print("-" * 80)
    print()
    
    # Write to CSV
    output_file = OUTPUT_DIR / OUTPUT_FILE
    write_drugs_to_csv(all_drugs, output_file)
    
    # Generate statistics
    generate_statistics(all_drugs)
    
    print()
    print("=" * 80)
    print("✓ Drug generation complete!")
    print("=" * 80)
    print()
    print(f"Output file: {output_file.absolute()}")
    print()


if __name__ == "__main__":
    try:
        generate_all_drugs()
    except Exception as e:
        print(f"\nERROR: {e}")
        import traceback
        traceback.print_exc()
