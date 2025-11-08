#!/usr/bin/env python3
"""
Generate realistic US healthcare formulary data for PBM system testing.

This script generates 5,000 formularies covering all major US healthcare market segments:
- Medicare Part D (800 formularies)
- Medicare Advantage MAPD (3,000 formularies)
- Commercial Insurance (500 formularies)
- Medicaid (100 formularies)
- Federal Programs (20 formularies)
- Regional/Specialty (580 formularies)

Each formulary is linked to a plan and includes metadata about coverage type,
effective dates, and formulary characteristics.

Output: Single CSV file with 5,000 formulary records
"""

import csv
import random
from datetime import datetime, timedelta
from pathlib import Path
import glob

# Configuration
TOTAL_FORMULARIES = 5000
OUTPUT_DIR = Path("../../src/main/resources/data")
OUTPUT_FILE = "us_pharmacy_formularies.csv"

# Market segment distribution (must sum to TOTAL_FORMULARIES)
MEDICARE_PART_D = 800        # 16% - Standalone prescription drug plans
MEDICARE_ADVANTAGE = 3000    # 60% - Medicare Advantage with drug coverage
COMMERCIAL = 500             # 10% - Employer and individual market
MEDICAID = 100               # 2%  - State Medicaid programs
FEDERAL = 20                 # 0.4% - VA, TRICARE, FEHB
REGIONAL_SPECIALTY = 580     # 11.6% - Regional carriers and specialty plans

# Formulary types
FORMULARY_TYPES = {
    'STANDARD': 0.40,      # 40% - Standard coverage
    'ENHANCED': 0.25,      # 25% - Enhanced/premium coverage
    'BASIC': 0.20,         # 20% - Basic/value coverage
    'SPECIALTY': 0.10,     # 10% - Specialty drug focus
    'MAIL_ORDER': 0.05     # 5% - Mail order specific
}

# Tier structures (number of tiers in formulary)
TIER_STRUCTURES = {
    3: 0.15,  # 15% - Simple 3-tier (Generic, Preferred, Non-Preferred)
    4: 0.35,  # 35% - Standard 4-tier (+ Specialty)
    5: 0.40,  # 40% - Enhanced 5-tier (+ High-cost Specialty)
    6: 0.10   # 10% - Complex 6-tier (+ Biologics)
}

# Coverage levels
COVERAGE_LEVELS = ['COMPREHENSIVE', 'STANDARD', 'BASIC', 'LIMITED']

# Major PBMs (Pharmacy Benefit Managers)
PBMS = [
    'CVS_CAREMARK', 'EXPRESS_SCRIPTS', 'OPTUM_RX', 
    'HUMANA_PHARMACY', 'PRIME_THERAPEUTICS', 'MAGELLAN_RX',
    'ENVOLVE_PHARMACY', 'MEDIMPACT', 'NAVITUS', 'ELIXIR'
]

# Insurance carriers
CARRIERS = {
    'MEDICARE_PART_D': [
        'HUMANA', 'WELLCARE', 'AETNA', 'CIGNA', 'UNITED_HEALTHCARE',
        'CVS_HEALTH', 'ANTHEM', 'KAISER', 'BCBS', 'CENTENE'
    ],
    'MEDICARE_ADVANTAGE': [
        'HUMANA', 'UNITED_HEALTHCARE', 'ANTHEM', 'CVS_HEALTH', 'AETNA',
        'KAISER', 'CENTENE', 'CIGNA', 'BCBS', 'WELLCARE', 'MOLINA'
    ],
    'COMMERCIAL': [
        'UNITED_HEALTHCARE', 'ANTHEM', 'AETNA', 'CIGNA', 'HUMANA',
        'BCBS', 'KAISER', 'CENTENE', 'MOLINA', 'HEALTH_NET'
    ],
    'MEDICAID': [
        'CENTENE', 'MOLINA', 'ANTHEM', 'UNITED_HEALTHCARE', 'AETNA',
        'WELLCARE', 'HEALTH_NET', 'AMERIGROUP', 'BCBS'
    ],
    'FEDERAL': [
        'VA', 'TRICARE', 'FEHB_BCBS', 'FEHB_AETNA', 'FEHB_KAISER',
        'FEHB_UNITED', 'FEHB_GEHA'
    ]
}

# US States (for regional formularies)
US_STATES = [
    'AL', 'AK', 'AZ', 'AR', 'CA', 'CO', 'CT', 'DE', 'FL', 'GA',
    'HI', 'ID', 'IL', 'IN', 'IA', 'KS', 'KY', 'LA', 'ME', 'MD',
    'MA', 'MI', 'MN', 'MS', 'MO', 'MT', 'NE', 'NV', 'NH', 'NJ',
    'NM', 'NY', 'NC', 'ND', 'OH', 'OK', 'OR', 'PA', 'RI', 'SC',
    'SD', 'TN', 'TX', 'UT', 'VT', 'VA', 'WA', 'WV', 'WI', 'WY'
]


def find_plan_files():
    """Find plan CSV files to link formularies to plans."""
    patterns = [
        "../data/us_pharmacy_plans.csv",
        "../data/us_plans.csv"
    ]
    
    for pattern in patterns:
        files = glob.glob(pattern)
        if files:
            return files
    
    return []


def load_plan_codes():
    """Load all plan codes from CSV files."""
    print("Loading plan codes from CSV files...")
    plan_files = find_plan_files()
    
    if not plan_files:
        print("⚠️  Warning: No plan CSV files found. Using default plan codes.")
        return generate_default_plan_codes()
    
    print(f"Found {len(plan_files)} plan file(s)")
    
    plan_codes = []
    for file_path in plan_files:
        with open(file_path, 'r') as f:
            reader = csv.DictReader(f)
            for row in reader:
                plan_codes.append(row['plan_code'])
    
    print(f"Loaded {len(plan_codes)} plan codes")
    return plan_codes


def generate_default_plan_codes():
    """Generate default plan codes if no plan file exists."""
    plan_codes = []
    
    # Commercial plans
    for i in range(1, 11):
        plan_codes.extend([
            f"COMM-PLATINUM-{i:03d}",
            f"COMM-GOLD-{i:03d}",
            f"COMM-SILVER-{i:03d}",
            f"COMM-BRONZE-{i:03d}",
            f"COMM-HDHP-{i:03d}"
        ])
    
    # Medicare plans
    for i in range(1, 21):
        plan_codes.extend([
            f"MCARE-PARTD-{i:03d}",
            f"MCARE-MAPD-{i:03d}",
            f"MCARE-MEDIGAP-{i:03d}"
        ])
    
    # Medicaid plans
    for state in US_STATES[:10]:
        plan_codes.append(f"MCAID-{state}-001")
    
    return plan_codes


def generate_formulary_code(segment, carrier, sequence):
    """Generate a unique formulary code."""
    year = 2024
    return f"FORM-{segment[:4].upper()}-{carrier[:4].upper()}-{year}-{sequence:04d}"


def generate_formulary_name(segment, carrier, formulary_type, tier_count):
    """Generate a descriptive formulary name."""
    type_names = {
        'STANDARD': 'Standard',
        'ENHANCED': 'Enhanced',
        'BASIC': 'Basic',
        'SPECIALTY': 'Specialty',
        'MAIL_ORDER': 'Mail Order'
    }
    
    type_name = type_names.get(formulary_type, 'Standard')
    return f"{carrier} {segment} {type_name} {tier_count}-Tier Formulary 2024"


def select_weighted_random(choices_dict):
    """Select a random item based on weighted probabilities."""
    items = list(choices_dict.keys())
    weights = list(choices_dict.values())
    return random.choices(items, weights=weights)[0]


def generate_date(year=2024, month=1, day=1):
    """Generate a date."""
    return f"{year}-{month:02d}-{day:02d}"


def generate_formularies_for_segment(segment, count, plan_codes):
    """Generate formularies for a specific market segment."""
    formularies = []
    carriers = CARRIERS.get(segment, CARRIERS['COMMERCIAL'])
    
    for i in range(count):
        # Select carrier and PBM
        carrier = random.choice(carriers)
        pbm = random.choice(PBMS)
        
        # Select formulary characteristics
        formulary_type = select_weighted_random(FORMULARY_TYPES)
        tier_count = select_weighted_random(TIER_STRUCTURES)
        coverage_level = random.choice(COVERAGE_LEVELS)
        
        # Generate codes and names
        formulary_code = generate_formulary_code(segment, carrier, i + 1)
        formulary_name = generate_formulary_name(segment, carrier, formulary_type, tier_count)
        
        # Link to a plan (if available)
        plan_code = random.choice(plan_codes) if plan_codes else f"PLAN-{segment}-{i+1:04d}"
        
        # Effective dates (most formularies are annual)
        effective_date = generate_date(2024, 1, 1)
        termination_date = generate_date(2024, 12, 31)
        
        # Regional information (for some segments)
        region = random.choice(US_STATES) if segment in ['MEDICAID', 'REGIONAL'] else 'NATIONAL'
        
        # Drug count (approximate number of drugs in formulary)
        if formulary_type == 'SPECIALTY':
            drug_count = random.randint(500, 1500)
        elif formulary_type == 'BASIC':
            drug_count = random.randint(1000, 2500)
        else:
            drug_count = random.randint(2000, 4000)
        
        # Prior authorization percentage
        prior_auth_pct = random.randint(5, 25)
        
        # Step therapy percentage
        step_therapy_pct = random.randint(3, 15)
        
        # Quantity limit percentage
        quantity_limit_pct = random.randint(10, 30)
        
        # Active status (95% active, 5% historical)
        is_active = random.random() < 0.95
        
        formulary = {
            'formulary_code': formulary_code,
            'formulary_name': formulary_name,
            'plan_code': plan_code,
            'market_segment': segment,
            'carrier': carrier,
            'pbm': pbm,
            'formulary_type': formulary_type,
            'tier_count': tier_count,
            'coverage_level': coverage_level,
            'effective_date': effective_date,
            'termination_date': termination_date,
            'region': region,
            'drug_count': drug_count,
            'prior_auth_pct': prior_auth_pct,
            'step_therapy_pct': step_therapy_pct,
            'quantity_limit_pct': quantity_limit_pct,
            'is_active': str(is_active).lower()
        }
        
        formularies.append(formulary)
    
    return formularies


def write_formularies_to_csv(formularies, output_file):
    """Write formularies to a CSV file."""
    fieldnames = [
        'formulary_code', 'formulary_name', 'plan_code', 'market_segment',
        'carrier', 'pbm', 'formulary_type', 'tier_count', 'coverage_level',
        'effective_date', 'termination_date', 'region', 'drug_count',
        'prior_auth_pct', 'step_therapy_pct', 'quantity_limit_pct', 'is_active'
    ]
    
    with open(output_file, 'w', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(formularies)
    
    file_size_mb = output_file.stat().st_size / (1024 * 1024)
    print(f"  ✓ Created {output_file.name} ({len(formularies):,} formularies, {file_size_mb:.1f} MB)")


def generate_statistics(formularies):
    """Generate and display statistics about the formularies."""
    print("\n" + "=" * 80)
    print("Formulary Statistics")
    print("=" * 80)
    
    # Count by market segment
    segment_counts = {}
    for f in formularies:
        segment = f['market_segment']
        segment_counts[segment] = segment_counts.get(segment, 0) + 1
    
    print("\nBy Market Segment:")
    for segment, count in sorted(segment_counts.items()):
        pct = (count / len(formularies)) * 100
        print(f"  {segment:25s}: {count:5,d} ({pct:5.1f}%)")
    
    # Count by formulary type
    type_counts = {}
    for f in formularies:
        ftype = f['formulary_type']
        type_counts[ftype] = type_counts.get(ftype, 0) + 1
    
    print("\nBy Formulary Type:")
    for ftype, count in sorted(type_counts.items()):
        pct = (count / len(formularies)) * 100
        print(f"  {ftype:15s}: {count:5,d} ({pct:5.1f}%)")
    
    # Count by tier structure
    tier_counts = {}
    for f in formularies:
        tiers = f['tier_count']
        tier_counts[tiers] = tier_counts.get(tiers, 0) + 1
    
    print("\nBy Tier Structure:")
    for tiers, count in sorted(tier_counts.items()):
        pct = (count / len(formularies)) * 100
        print(f"  {tiers}-Tier: {count:5,d} ({pct:5.1f}%)")
    
    # Count by PBM
    pbm_counts = {}
    for f in formularies:
        pbm = f['pbm']
        pbm_counts[pbm] = pbm_counts.get(pbm, 0) + 1
    
    print("\nTop 5 PBMs:")
    for pbm, count in sorted(pbm_counts.items(), key=lambda x: x[1], reverse=True)[:5]:
        pct = (count / len(formularies)) * 100
        print(f"  {pbm:20s}: {count:5,d} ({pct:5.1f}%)")
    
    # Active vs inactive
    active_count = sum(1 for f in formularies if f['is_active'] == 'true')
    inactive_count = len(formularies) - active_count
    print(f"\nActive Status:")
    print(f"  Active:   {active_count:5,d} ({(active_count/len(formularies))*100:5.1f}%)")
    print(f"  Inactive: {inactive_count:5,d} ({(inactive_count/len(formularies))*100:5.1f}%)")


def generate_all_formularies():
    """Generate all formulary records."""
    print("=" * 80)
    print("US Healthcare Formulary Data Generator")
    print("=" * 80)
    print()
    print(f"Configuration:")
    print(f"  Total formularies: {TOTAL_FORMULARIES:,}")
    print(f"  Output directory: {OUTPUT_DIR}")
    print(f"  Output file: {OUTPUT_FILE}")
    print()
    print(f"Market segment distribution:")
    print(f"  Medicare Part D:        {MEDICARE_PART_D:5,d} ({(MEDICARE_PART_D/TOTAL_FORMULARIES)*100:5.1f}%)")
    print(f"  Medicare Advantage:     {MEDICARE_ADVANTAGE:5,d} ({(MEDICARE_ADVANTAGE/TOTAL_FORMULARIES)*100:5.1f}%)")
    print(f"  Commercial:             {COMMERCIAL:5,d} ({(COMMERCIAL/TOTAL_FORMULARIES)*100:5.1f}%)")
    print(f"  Medicaid:               {MEDICAID:5,d} ({(MEDICAID/TOTAL_FORMULARIES)*100:5.1f}%)")
    print(f"  Federal Programs:       {FEDERAL:5,d} ({(FEDERAL/TOTAL_FORMULARIES)*100:5.1f}%)")
    print(f"  Regional/Specialty:     {REGIONAL_SPECIALTY:5,d} ({(REGIONAL_SPECIALTY/TOTAL_FORMULARIES)*100:5.1f}%)")
    print()
    
    # Create output directory if it doesn't exist
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    
    # Load plan codes
    plan_codes = load_plan_codes()
    
    print()
    print("Generating formularies...")
    print("-" * 80)
    
    all_formularies = []
    
    # Generate formularies for each market segment
    segments = [
        ('MEDICARE_PART_D', MEDICARE_PART_D),
        ('MEDICARE_ADVANTAGE', MEDICARE_ADVANTAGE),
        ('COMMERCIAL', COMMERCIAL),
        ('MEDICAID', MEDICAID),
        ('FEDERAL', FEDERAL),
        ('REGIONAL', REGIONAL_SPECIALTY)
    ]
    
    for segment, count in segments:
        print(f"  Generating {count:,} {segment} formularies...")
        formularies = generate_formularies_for_segment(segment, count, plan_codes)
        all_formularies.extend(formularies)
    
    print("-" * 80)
    print()
    
    # Write to CSV
    output_file = OUTPUT_DIR / OUTPUT_FILE
    write_formularies_to_csv(all_formularies, output_file)
    
    # Generate statistics
    generate_statistics(all_formularies)
    
    print()
    print("=" * 80)
    print("✓ Formulary generation complete!")
    print("=" * 80)
    print()
    print(f"Output file: {output_file.absolute()}")
    print()


if __name__ == "__main__":
    try:
        generate_all_formularies()
    except Exception as e:
        print(f"\nERROR: {e}")
        import traceback
        traceback.print_exc()
