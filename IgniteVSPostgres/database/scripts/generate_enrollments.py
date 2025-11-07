#!/usr/bin/env python3
"""
Generate realistic US healthcare enrollment data for PBM system testing.

This script generates 10 million enrollment records following US healthcare enrollment rules:
- Single active coverage (70%)
- Dual coverage (15%)
- Plan transitions (10%)
- Historical enrollments (5%)

Output: 20 CSV files with ~500,000 enrollments each (~30MB per file)
"""

import csv
import random
from datetime import datetime, timedelta
from pathlib import Path
import glob

# Configuration
TOTAL_ENROLLMENTS = 10_000_000
ENROLLMENTS_PER_FILE = 500_000
NUM_FILES = TOTAL_ENROLLMENTS // ENROLLMENTS_PER_FILE
OUTPUT_DIR = Path("../data")
FILE_PREFIX = "us_pharmacy_enrollments"

# Enrollment scenarios (percentages)
SINGLE_ACTIVE = 0.70      # 70% - One active enrollment
DUAL_COVERAGE = 0.15      # 15% - Two active enrollments
PLAN_TRANSITION = 0.10    # 10% - Changed plans during year
HISTORICAL_ONLY = 0.05    # 5% - No active enrollment

# Relationship types
RELATIONSHIPS = ["SELF", "SPOUSE", "DEPENDENT"]
RELATIONSHIP_WEIGHTS = [0.60, 0.25, 0.15]  # 60% self, 25% spouse, 15% dependent

# Plan codes (must match us_pharmacy_plans.csv)
PLAN_CODES = [
    # Commercial Plans
    "COMM-PLATINUM-001", "COMM-GOLD-001", "COMM-SILVER-001", "COMM-BRONZE-001",
    "COMM-HDHP-001", "COMM-SILVER-002", "COMM-GOLD-002", "COMM-BRONZE-002",
    
    # Medicare Plans
    "MCARE-PARTD-001", "MCARE-PARTD-002", "MCARE-MAPD-001", "MCARE-MEDIGAP-001",
    
    # Medicaid Plans
    "MCAID-STD-001", "MCAID-EXP-001",
    
    # Exchange Plans
    "EXCH-SILVER-001", "EXCH-GOLD-001",
    
    # Employer Plans
    "EMP-TRAD-001", "EMP-HDHP-001", "EMP-PREM-001",
    
    # Union Plans
    "UNION-STD-001", "UNION-PREM-001",
    
    # Government Plans
    "GOV-VA-001", "GOV-TRICARE-001", "GOV-FEHB-001",
    
    # CHIP
    "CHIP-STD-001",
    
    # Individual Market
    "IND-CATA-001", "IND-BRONZE-001", "IND-SILVER-001", "IND-GOLD-001"
]

# Group numbers (employer/sponsor identifiers)
GROUP_PREFIXES = ["GRP", "EMP", "UNI", "GOV", "IND"]


def find_member_files():
    """Find all member CSV files to get member numbers."""
    patterns = [
        "../data/us_pharmacy_members_*.csv",
        "../data/us_members_*.csv"
    ]
    
    member_files = []
    for pattern in patterns:
        files = glob.glob(pattern)
        if files:
            member_files.extend(files)
    
    if not member_files:
        raise FileNotFoundError(
            "No member CSV files found. Please generate members first using generate_members.py"
        )
    
    return sorted(member_files)


def load_member_numbers():
    """Load all member numbers from CSV files."""
    print("Loading member numbers from CSV files...")
    member_files = find_member_files()
    print(f"Found {len(member_files)} member files")
    
    member_numbers = []
    for file_path in member_files:
        with open(file_path, 'r') as f:
            reader = csv.DictReader(f)
            for row in reader:
                member_numbers.append(row['member_number'])
    
    print(f"Loaded {len(member_numbers):,} member numbers")
    return member_numbers


def generate_group_number():
    """Generate a random group number."""
    prefix = random.choice(GROUP_PREFIXES)
    number = random.randint(10000, 99999)
    return f"{prefix}-{number}"


def generate_date(start_year=2023, end_year=2024):
    """Generate a random date within the specified year range."""
    start_date = datetime(start_year, 1, 1)
    end_date = datetime(end_year, 12, 31)
    days_between = (end_date - start_date).days
    random_days = random.randint(0, days_between)
    return start_date + timedelta(days=random_days)


def generate_single_active_enrollment(member_number):
    """Generate a single active enrollment (70% of cases)."""
    return [{
        'member_number': member_number,
        'plan_code': random.choice(PLAN_CODES),
        'group_number': generate_group_number(),
        'effective_date': '2024-01-01',
        'termination_date': '',
        'relationship': random.choices(RELATIONSHIPS, weights=RELATIONSHIP_WEIGHTS)[0],
        'is_active': 'true'
    }]


def generate_dual_coverage_enrollment(member_number):
    """Generate dual coverage enrollments (15% of cases)."""
    # Primary and secondary coverage (e.g., Medicare + Medigap)
    primary_plan = random.choice([p for p in PLAN_CODES if 'MCARE' in p or 'COMM' in p])
    secondary_plan = random.choice([p for p in PLAN_CODES if p != primary_plan])
    
    group_num = generate_group_number()
    relationship = random.choices(RELATIONSHIPS, weights=RELATIONSHIP_WEIGHTS)[0]
    
    return [
        {
            'member_number': member_number,
            'plan_code': primary_plan,
            'group_number': group_num,
            'effective_date': '2024-01-01',
            'termination_date': '',
            'relationship': relationship,
            'is_active': 'true'
        },
        {
            'member_number': member_number,
            'plan_code': secondary_plan,
            'group_number': group_num,
            'effective_date': '2024-01-01',
            'termination_date': '',
            'relationship': relationship,
            'is_active': 'true'
        }
    ]


def generate_plan_transition_enrollment(member_number):
    """Generate plan transition enrollments (10% of cases)."""
    # Old plan (terminated) and new plan (active)
    old_plan = random.choice(PLAN_CODES)
    new_plan = random.choice([p for p in PLAN_CODES if p != old_plan])
    
    # Transition date (sometime during 2024)
    transition_month = random.randint(2, 11)
    transition_date = f"2024-{transition_month:02d}-01"
    termination_date = f"2024-{transition_month-1:02d}-{random.choice([28, 30, 31]):02d}"
    
    group_num = generate_group_number()
    relationship = random.choices(RELATIONSHIPS, weights=RELATIONSHIP_WEIGHTS)[0]
    
    return [
        {
            'member_number': member_number,
            'plan_code': old_plan,
            'group_number': group_num,
            'effective_date': '2023-01-01',
            'termination_date': termination_date,
            'relationship': relationship,
            'is_active': 'false'
        },
        {
            'member_number': member_number,
            'plan_code': new_plan,
            'group_number': group_num,
            'effective_date': transition_date,
            'termination_date': '',
            'relationship': relationship,
            'is_active': 'true'
        }
    ]


def generate_historical_enrollment(member_number):
    """Generate historical (inactive) enrollment (5% of cases)."""
    return [{
        'member_number': member_number,
        'plan_code': random.choice(PLAN_CODES),
        'group_number': generate_group_number(),
        'effective_date': '2023-01-01',
        'termination_date': '2023-12-31',
        'relationship': random.choices(RELATIONSHIPS, weights=RELATIONSHIP_WEIGHTS)[0],
        'is_active': 'false'
    }]


def generate_enrollments_for_member(member_number):
    """Generate enrollment(s) for a member based on scenario probabilities."""
    scenario = random.random()
    
    if scenario < SINGLE_ACTIVE:
        return generate_single_active_enrollment(member_number)
    elif scenario < SINGLE_ACTIVE + DUAL_COVERAGE:
        return generate_dual_coverage_enrollment(member_number)
    elif scenario < SINGLE_ACTIVE + DUAL_COVERAGE + PLAN_TRANSITION:
        return generate_plan_transition_enrollment(member_number)
    else:
        return generate_historical_enrollment(member_number)


def write_enrollments_to_csv(enrollments, file_number):
    """Write enrollments to a CSV file."""
    output_file = OUTPUT_DIR / f"{FILE_PREFIX}_{file_number:02d}.csv"
    
    fieldnames = [
        'member_number', 'plan_code', 'group_number',
        'effective_date', 'termination_date', 'relationship', 'is_active'
    ]
    
    with open(output_file, 'w', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(enrollments)
    
    file_size_mb = output_file.stat().st_size / (1024 * 1024)
    print(f"  ✓ Created {output_file.name} ({len(enrollments):,} enrollments, {file_size_mb:.1f} MB)")


def generate_all_enrollments():
    """Generate all enrollment records."""
    print("=" * 80)
    print("US Healthcare Enrollment Data Generator")
    print("=" * 80)
    print()
    print(f"Configuration:")
    print(f"  Total enrollments: {TOTAL_ENROLLMENTS:,}")
    print(f"  Enrollments per file: {ENROLLMENTS_PER_FILE:,}")
    print(f"  Number of files: {NUM_FILES}")
    print(f"  Output directory: {OUTPUT_DIR}")
    print()
    print(f"Enrollment scenarios:")
    print(f"  Single active coverage: {SINGLE_ACTIVE*100:.0f}%")
    print(f"  Dual coverage: {DUAL_COVERAGE*100:.0f}%")
    print(f"  Plan transitions: {PLAN_TRANSITION*100:.0f}%")
    print(f"  Historical only: {HISTORICAL_ONLY*100:.0f}%")
    print()
    
    # Create output directory if it doesn't exist
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    
    # Load member numbers
    member_numbers = load_member_numbers()
    
    if len(member_numbers) == 0:
        print("ERROR: No member numbers found!")
        return
    
    print()
    print("Generating enrollments...")
    print("-" * 80)
    
    all_enrollments = []
    members_processed = 0
    file_number = 1
    
    # Ensure every member has at least one enrollment
    for member_number in member_numbers:
        enrollments = generate_enrollments_for_member(member_number)
        all_enrollments.extend(enrollments)
        members_processed += 1
        
        # Write to file when we reach the target size
        if len(all_enrollments) >= ENROLLMENTS_PER_FILE:
            write_enrollments_to_csv(all_enrollments[:ENROLLMENTS_PER_FILE], file_number)
            all_enrollments = all_enrollments[ENROLLMENTS_PER_FILE:]
            file_number += 1
        
        # Progress indicator
        if members_processed % 50000 == 0:
            print(f"  Processed {members_processed:,} members, generated {(file_number-1) * ENROLLMENTS_PER_FILE + len(all_enrollments):,} enrollments...")
    
    # Write remaining enrollments
    if all_enrollments:
        write_enrollments_to_csv(all_enrollments, file_number)
    
    print("-" * 80)
    print()
    print("=" * 80)
    print("✓ Enrollment generation complete!")
    print("=" * 80)
    print()
    
    # Calculate statistics
    total_files = file_number
    total_size_mb = sum((OUTPUT_DIR / f"{FILE_PREFIX}_{i:02d}.csv").stat().st_size 
                        for i in range(1, total_files + 1)) / (1024 * 1024)
    
    print(f"Summary:")
    print(f"  Files created: {total_files}")
    print(f"  Total size: {total_size_mb:.1f} MB")
    print(f"  Members with enrollments: {members_processed:,}")
    print(f"  Total enrollment records: ~{TOTAL_ENROLLMENTS:,}")
    print()
    print(f"Files location: {OUTPUT_DIR.absolute()}")
    print()


if __name__ == "__main__":
    try:
        generate_all_enrollments()
    except Exception as e:
        print(f"\nERROR: {e}")
        import traceback
        traceback.print_exc()

# Made with Bob
