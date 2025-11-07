#!/usr/bin/env python3
"""
US Healthcare Formulary-Drug Relationship Generator

This script generates realistic formulary-drug relationships following US healthcare rules:
- Links 5,000 formularies to ~2,000 drugs each (10 million total relationships)
- Applies tier assignments based on drug characteristics
- Implements utilization management rules (prior auth, step therapy, quantity limits)
- Follows Medicare Part D and commercial insurance regulations
- Respects protected drug class requirements

Output: Multiple CSV files (30MB each) with formulary-drug records
"""

import csv
import random
import uuid
from pathlib import Path
from collections import defaultdict

# Configuration
OUTPUT_DIR = Path("../../src/main/resources/data")
FORMULARY_FILE = "us_pharmacy_formularies.csv"
DRUG_FILE = "us_pharmacy_drugs.csv"
OUTPUT_FILE_PREFIX = "us_pharmacy_formularies_drugs"
MAX_FILE_SIZE_MB = 30  # Target file size in MB
ESTIMATED_BYTES_PER_RECORD = 200  # Approximate bytes per CSV record
RECORDS_PER_FILE = int((MAX_FILE_SIZE_MB * 1024 * 1024) / ESTIMATED_BYTES_PER_RECORD)
MAX_TOTAL_RELATIONSHIPS = 10_000_000  # Limit total relationships to 10 million

# Target drugs per formulary (adjusted to reach ~10M total with 5000 formularies)
TARGET_DRUGS_PER_FORMULARY = {
    'SPECIALTY': (500, 1500),      # Specialty formularies: 500-1,500 drugs
    'BASIC': (1500, 2000),          # Basic formularies: 1,500-2,000 drugs
    'STANDARD': (1800, 2200),       # Standard formularies: 1,800-2,200 drugs
    'ENHANCED': (2000, 2500),       # Enhanced formularies: 2,000-2,500 drugs
    'MAIL_ORDER': (1800, 2200)      # Mail order formularies: 1,800-2,200 drugs
}

# Protected drug classes (Medicare Part D requirement: must cover "all or substantially all")
PROTECTED_CLASSES = [
    'ANTICONVULSANT',
    'ANTIDEPRESSANT',
    'CHEMOTHERAPY',
    'TARGETED_THERAPY',
    'IMMUNOTHERAPY',
    'ANTIPSYCHOTIC',
    'IMMUNOSUPPRESSANT',
    'BIOLOGIC'
]

# Tier assignment rules based on drug characteristics
TIER_ASSIGNMENT_RULES = {
    # Generic drugs -> Tier 1 (lowest cost)
    'generic': {
        'tier': 1,
        'probability': 0.95  # 95% of generics go to Tier 1
    },
    # Brand drugs -> Tier 2 or 3
    'brand_preferred': {
        'tier': 2,
        'probability': 0.60  # 60% of brands are preferred (Tier 2)
    },
    'brand_non_preferred': {
        'tier': 3,
        'probability': 0.40  # 40% of brands are non-preferred (Tier 3)
    },
    # Specialty drugs -> Tier 4 or 5
    'specialty_standard': {
        'tier': 4,
        'probability': 0.70  # 70% of specialty drugs go to Tier 4
    },
    'specialty_high_cost': {
        'tier': 5,
        'probability': 0.30  # 30% of specialty drugs go to Tier 5
    }
}

# Utilization management percentages by tier
UTILIZATION_MANAGEMENT = {
    1: {  # Tier 1 (Generic)
        'prior_auth': 0.02,      # 2% require prior auth
        'step_therapy': 0.01,    # 1% require step therapy
        'quantity_limit': 0.05   # 5% have quantity limits
    },
    2: {  # Tier 2 (Preferred Brand)
        'prior_auth': 0.10,      # 10% require prior auth
        'step_therapy': 0.08,    # 8% require step therapy
        'quantity_limit': 0.15   # 15% have quantity limits
    },
    3: {  # Tier 3 (Non-Preferred Brand)
        'prior_auth': 0.25,      # 25% require prior auth
        'step_therapy': 0.20,    # 20% require step therapy
        'quantity_limit': 0.30   # 30% have quantity limits
    },
    4: {  # Tier 4 (Specialty)
        'prior_auth': 0.60,      # 60% require prior auth
        'step_therapy': 0.15,    # 15% require step therapy
        'quantity_limit': 0.50   # 50% have quantity limits
    },
    5: {  # Tier 5 (High-Cost Specialty)
        'prior_auth': 0.90,      # 90% require prior auth
        'step_therapy': 0.25,    # 25% require step therapy
        'quantity_limit': 0.70   # 70% have quantity limits
    }
}

# Quantity limits by dosage form (days supply)
QUANTITY_LIMITS = {
    'TABLET': [30, 60, 90],
    'CAPSULE': [30, 60, 90],
    'SOLUTION': [30, 60],
    'INJECTION': [30, 90],
    'CREAM': [30, 60],
    'OINTMENT': [30, 60],
    'SUSPENSION': [30],
    'PATCH': [30],
    'INHALER': [30, 90],
    'SUPPOSITORY': [30]
}

# Days supply limits
DAYS_SUPPLY_LIMITS = [30, 60, 90]


def load_formularies():
    """Load all formularies from CSV file."""
    print("Loading formularies...")
    formularies = []
    
    formulary_path = OUTPUT_DIR / FORMULARY_FILE
    
    with open(formulary_path, 'r') as f:
        reader = csv.DictReader(f)
        for row in reader:
            formularies.append(row)
    
    print(f"  ✓ Loaded {len(formularies):,} formularies")
    return formularies


def load_drugs():
    """Load all drugs from CSV file."""
    print("Loading drugs...")
    drugs = []
    
    drug_path = OUTPUT_DIR / DRUG_FILE
    
    with open(drug_path, 'r') as f:
        reader = csv.DictReader(f)
        for row in reader:
            drugs.append(row)
    
    print(f"  ✓ Loaded {len(drugs):,} drugs")
    
    # Categorize drugs
    drug_categories = {
        'generic': [],
        'brand': [],
        'specialty': [],
        'protected': []
    }
    
    for drug in drugs:
        if drug['is_generic'] == 'true':
            drug_categories['generic'].append(drug)
        elif drug['is_specialty'] == 'true':
            drug_categories['specialty'].append(drug)
        elif drug['is_brand'] == 'true':
            drug_categories['brand'].append(drug)
        
        # Check if protected class
        if drug['drug_class'] in PROTECTED_CLASSES:
            drug_categories['protected'].append(drug)
    
    print(f"    - Generic: {len(drug_categories['generic']):,}")
    print(f"    - Brand: {len(drug_categories['brand']):,}")
    print(f"    - Specialty: {len(drug_categories['specialty']):,}")
    print(f"    - Protected classes: {len(drug_categories['protected']):,}")
    
    return drugs, drug_categories


def assign_tier(drug, formulary_tier_count):
    """Assign a tier to a drug based on its characteristics."""
    # Generic drugs -> Tier 1
    if drug['is_generic'] == 'true':
        return 1
    
    # Specialty drugs -> Tier 4 or 5
    if drug['is_specialty'] == 'true':
        if formulary_tier_count >= 5:
            # 70% Tier 4, 30% Tier 5
            return 4 if random.random() < 0.70 else 5
        else:
            return min(4, formulary_tier_count)
    
    # Brand drugs -> Tier 2 or 3
    if drug['is_brand'] == 'true':
        # 60% preferred (Tier 2), 40% non-preferred (Tier 3)
        tier = 2 if random.random() < 0.60 else 3
        return min(tier, formulary_tier_count)
    
    # Default to Tier 2
    return min(2, formulary_tier_count)


def apply_utilization_management(tier, drug):
    """Determine utilization management requirements based on tier and drug."""
    um_rules = UTILIZATION_MANAGEMENT.get(tier, UTILIZATION_MANAGEMENT[3])
    
    # Prior authorization
    requires_prior_auth = random.random() < um_rules['prior_auth']
    
    # Step therapy (more likely for expensive drugs)
    requires_step_therapy = random.random() < um_rules['step_therapy']
    
    # Quantity limits
    has_quantity_limit = random.random() < um_rules['quantity_limit']
    
    quantity_limit = None
    days_supply_limit = None
    
    if has_quantity_limit:
        dosage_form = drug['dosage_form']
        quantity_limit = random.choice(QUANTITY_LIMITS.get(dosage_form, [30]))
        days_supply_limit = random.choice(DAYS_SUPPLY_LIMITS)
    
    return requires_prior_auth, requires_step_therapy, quantity_limit, days_supply_limit


def select_drugs_for_formulary(formulary, all_drugs, drug_categories):
    """Select drugs for a specific formulary following US healthcare rules."""
    formulary_type = formulary['formulary_type']
    tier_count = int(formulary['tier_count'])
    market_segment = formulary['market_segment']
    
    # Determine target drug count
    min_drugs, max_drugs = TARGET_DRUGS_PER_FORMULARY.get(formulary_type, (2000, 3000))
    target_count = random.randint(min_drugs, max_drugs)
    
    selected_drugs = set()
    
    # RULE 1: Protected classes - must include "all or substantially all" (90%+)
    if market_segment in ['MEDICARE_PART_D', 'MEDICARE_ADVANTAGE']:
        protected_drugs = drug_categories['protected']
        protected_sample_size = int(len(protected_drugs) * 0.92)  # 92% coverage
        protected_sample = random.sample(protected_drugs, min(protected_sample_size, len(protected_drugs)))
        selected_drugs.update(drug['ndc_code'] for drug in protected_sample)
    
    # RULE 2: Generic drugs - include most generics (80-95%)
    generic_drugs = drug_categories['generic']
    generic_inclusion_rate = 0.85 if formulary_type in ['ENHANCED', 'STANDARD'] else 0.70
    generic_sample_size = int(len(generic_drugs) * generic_inclusion_rate)
    generic_sample = random.sample(generic_drugs, min(generic_sample_size, len(generic_drugs)))
    selected_drugs.update(drug['ndc_code'] for drug in generic_sample)
    
    # RULE 3: Specialty drugs - selective inclusion
    specialty_drugs = drug_categories['specialty']
    if formulary_type == 'SPECIALTY':
        # Specialty formularies include most specialty drugs
        specialty_sample_size = int(len(specialty_drugs) * 0.80)
    elif formulary_type == 'ENHANCED':
        specialty_sample_size = int(len(specialty_drugs) * 0.60)
    elif formulary_type == 'STANDARD':
        specialty_sample_size = int(len(specialty_drugs) * 0.40)
    else:  # BASIC
        specialty_sample_size = int(len(specialty_drugs) * 0.20)
    
    specialty_sample = random.sample(specialty_drugs, min(specialty_sample_size, len(specialty_drugs)))
    selected_drugs.update(drug['ndc_code'] for drug in specialty_sample)
    
    # RULE 4: Brand drugs - fill remaining slots
    brand_drugs = drug_categories['brand']
    remaining_slots = target_count - len(selected_drugs)
    
    if remaining_slots > 0:
        available_brands = [d for d in brand_drugs if d['ndc_code'] not in selected_drugs]
        brand_sample_size = min(remaining_slots, len(available_brands))
        brand_sample = random.sample(available_brands, brand_sample_size)
        selected_drugs.update(drug['ndc_code'] for drug in brand_sample)
    
    # Create drug lookup
    drug_lookup = {drug['ndc_code']: drug for drug in all_drugs}
    
    return [drug_lookup[ndc] for ndc in selected_drugs if ndc in drug_lookup]


def generate_formulary_drug_record(formulary, drug, sequence):
    """Generate a single formulary-drug relationship record."""
    tier_count = int(formulary['tier_count'])
    
    # Assign tier
    tier = assign_tier(drug, tier_count)
    
    # Apply utilization management
    requires_prior_auth, requires_step_therapy, quantity_limit, days_supply_limit = \
        apply_utilization_management(tier, drug)
    
    # Determine status (preferred vs non-preferred)
    if tier <= 2:
        status = 'PREFERRED'
    elif tier == 3:
        status = 'NON_PREFERRED' if random.random() < 0.60 else 'PREFERRED'
    else:
        status = 'SPECIALTY'
    
    record = {
        'formulary_drug_id': str(uuid.uuid4()),
        'formulary_code': formulary['formulary_code'],
        'ndc_code': drug['ndc_code'],
        'tier': tier,
        'status': status,
        'requires_prior_auth': str(requires_prior_auth).lower(),
        'requires_step_therapy': str(requires_step_therapy).lower(),
        'quantity_limit': quantity_limit if quantity_limit else '',
        'days_supply_limit': days_supply_limit if days_supply_limit else ''
    }
    
    return record


def write_records_to_multiple_files(all_records, output_dir):
    """Write records to multiple CSV files of approximately 30MB each."""
    fieldnames = [
        'formulary_drug_id', 'formulary_code', 'ndc_code', 'tier', 'status',
        'requires_prior_auth', 'requires_step_therapy', 'quantity_limit', 'days_supply_limit'
    ]
    
    total_records = len(all_records)
    num_files = (total_records + RECORDS_PER_FILE - 1) // RECORDS_PER_FILE
    
    print(f"\n  Writing {total_records:,} records to {num_files} file(s)...")
    print(f"  Target: ~{RECORDS_PER_FILE:,} records per file (~{MAX_FILE_SIZE_MB}MB)")
    print()
    
    output_files = []
    
    for file_num in range(1, num_files + 1):
        start_idx = (file_num - 1) * RECORDS_PER_FILE
        end_idx = min(start_idx + RECORDS_PER_FILE, total_records)
        batch = all_records[start_idx:end_idx]
        
        # Create filename with zero-padded number
        filename = f"{OUTPUT_FILE_PREFIX}_{file_num:02d}.csv"
        output_file = output_dir / filename
        
        with open(output_file, 'w', newline='') as f:
            writer = csv.DictWriter(f, fieldnames=fieldnames)
            writer.writeheader()
            writer.writerows(batch)
        
        file_size_mb = output_file.stat().st_size / (1024 * 1024)
        print(f"  ✓ Created {filename} ({len(batch):,} records, {file_size_mb:.1f} MB)")
        output_files.append(output_file)
    
    return output_files


def generate_statistics(records, formularies):
    """Generate and display statistics about the formulary-drug relationships."""
    print("\n" + "=" * 80)
    print("Formulary-Drug Relationship Statistics")
    print("=" * 80)
    
    # Total records
    print(f"\nTotal Records: {len(records):,}")
    
    # Average drugs per formulary
    avg_drugs = len(records) / len(formularies)
    print(f"Average Drugs per Formulary: {avg_drugs:,.0f}")
    
    # Tier distribution
    tier_counts = defaultdict(int)
    for record in records:
        tier_counts[record['tier']] += 1
    
    print("\nTier Distribution:")
    for tier in sorted(tier_counts.keys()):
        count = tier_counts[tier]
        pct = (count / len(records)) * 100
        print(f"  Tier {tier}: {count:8,d} ({pct:5.1f}%)")
    
    # Status distribution
    status_counts = defaultdict(int)
    for record in records:
        status_counts[record['status']] += 1
    
    print("\nStatus Distribution:")
    for status, count in sorted(status_counts.items()):
        pct = (count / len(records)) * 100
        print(f"  {status:20s}: {count:8,d} ({pct:5.1f}%)")
    
    # Utilization management
    prior_auth_count = sum(1 for r in records if r['requires_prior_auth'] == 'true')
    step_therapy_count = sum(1 for r in records if r['requires_step_therapy'] == 'true')
    quantity_limit_count = sum(1 for r in records if r['quantity_limit'])
    
    print("\nUtilization Management:")
    print(f"  Prior Authorization: {prior_auth_count:8,d} ({(prior_auth_count/len(records))*100:5.1f}%)")
    print(f"  Step Therapy:        {step_therapy_count:8,d} ({(step_therapy_count/len(records))*100:5.1f}%)")
    print(f"  Quantity Limits:     {quantity_limit_count:8,d} ({(quantity_limit_count/len(records))*100:5.1f}%)")


def generate_all_formulary_drugs():
    """Generate all formulary-drug relationships."""
    print("=" * 80)
    print("US Healthcare Formulary-Drug Relationship Generator")
    print("=" * 80)
    print()
    
    # Load data
    formularies = load_formularies()
    all_drugs, drug_categories = load_drugs()
    
    print()
    print(f"Generating formulary-drug relationships (max {MAX_TOTAL_RELATIONSHIPS:,})...")
    print("-" * 80)
    
    all_records = []
    sequence = 1
    
    for i, formulary in enumerate(formularies, 1):
        # Check if we've reached the limit
        if len(all_records) >= MAX_TOTAL_RELATIONSHIPS:
            print(f"  Reached maximum relationship limit of {MAX_TOTAL_RELATIONSHIPS:,}")
            break
        
        # Select drugs for this formulary
        selected_drugs = select_drugs_for_formulary(formulary, all_drugs, drug_categories)
        
        # Generate records (but don't exceed the limit)
        for drug in selected_drugs:
            if len(all_records) >= MAX_TOTAL_RELATIONSHIPS:
                break
            record = generate_formulary_drug_record(formulary, drug, sequence)
            all_records.append(record)
            sequence += 1
        
        # Progress indicator
        if i % 500 == 0:
            print(f"  Processed {i:,} formularies ({len(all_records):,} relationships)...")
    
    print("-" * 80)
    print(f"  ✓ Generated {len(all_records):,} total relationships")
    
    # Ensure output directory exists
    output_dir = OUTPUT_DIR
    output_dir.mkdir(parents=True, exist_ok=True)
    
    # Write to multiple CSV files
    output_files = write_records_to_multiple_files(all_records, output_dir)
    
    # Generate statistics
    generate_statistics(all_records, formularies)
    
    print()
    print("=" * 80)
    print("✓ Formulary-drug relationship generation complete!")
    print("=" * 80)
    print()
    print(f"Output directory: {output_dir.absolute()}")
    print(f"Files created: {len(output_files)}")
    for f in output_files:
        print(f"  - {f.name}")
    print()


if __name__ == "__main__":
    try:
        generate_all_formulary_drugs()
    except Exception as e:
        print(f"\nERROR: {e}")
        import traceback
        traceback.print_exc()

# Made with Bob
