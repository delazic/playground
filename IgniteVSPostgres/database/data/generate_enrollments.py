#!/usr/bin/env python3
"""
Generate 10 million enrollment records for PBM system
Splits output into ~15MB CSV files
Follows US healthcare enrollment rules
"""

import csv
import random
import os
from datetime import datetime, timedelta
from pathlib import Path

# Configuration
TOTAL_RECORDS = 10_000_000
MAX_FILE_SIZE_MB = 30
OUTPUT_DIR = Path(".")
FILE_PREFIX = "us_pharmacy_enrollments"

# Enrollment scenarios and their probabilities
SCENARIOS = {
    'single_active': 0.70,      # 70% - One active plan
    'dual_coverage': 0.15,      # 15% - Two active plans (primary + secondary)
    'plan_transition': 0.10,    # 10% - Transitioning between plans
    'historical_only': 0.05     # 5% - Only historical enrollments
}

# Relationship types and probabilities
RELATIONSHIPS = {
    'SELF': 0.60,
    'SPOUSE': 0.20,
    'CHILD': 0.15,
    'DEPENDENT': 0.05
}

# Plan types for realistic distribution
PLAN_TYPES = ['COMMERCIAL', 'MEDICARE', 'MEDICAID', 'EXCHANGE']


def load_member_ids():
    """Load member IDs from CSV files"""
    member_ids = []
    # Try both naming patterns
    csv_files = sorted(OUTPUT_DIR.glob("us_pharmacy_members_*.csv"))
    if not csv_files:
        csv_files = sorted(OUTPUT_DIR.glob("us_members_*.csv"))
    
    if not csv_files:
        print("ERROR: No member CSV files found. Please generate members first.")
        print("Looking for: us_pharmacy_members_*.csv or us_members_*.csv")
        return []
    
    print(f"Loading member IDs from {len(csv_files)} files...")
    for csv_file in csv_files:
        with open(csv_file, 'r') as f:
            reader = csv.DictReader(f)
            for row in reader:
                member_ids.append(row['member_number'])
    
    print(f"Loaded {len(member_ids):,} member IDs")
    return member_ids


def load_plan_codes():
    """Load plan codes from CSV file"""
    plan_file = OUTPUT_DIR / "us_pharmacy_plans.csv"
    if not plan_file.exists():
        print("ERROR: us_pharmacy_plans.csv not found")
        return []
    
    plan_codes = []
    with open(plan_file, 'r') as f:
        reader = csv.DictReader(f)
        for row in reader:
            plan_codes.append(row['plan_code'])
    
    print(f"Loaded {len(plan_codes):,} plan codes")
    return plan_codes


def random_date(start_year=2020, end_year=2025):
    """Generate random date between start and end year"""
    start = datetime(start_year, 1, 1)
    end = datetime(end_year, 12, 31)
    delta = end - start
    random_days = random.randint(0, delta.days)
    return start + timedelta(days=random_days)


def generate_group_number():
    """Generate realistic group number"""
    return f"GRP{random.randint(100000, 999999)}"


def choose_scenario():
    """Choose enrollment scenario based on probabilities"""
    rand = random.random()
    cumulative = 0
    for scenario, prob in SCENARIOS.items():
        cumulative += prob
        if rand <= cumulative:
            return scenario
    return 'single_active'


def choose_relationship():
    """Choose relationship based on probabilities"""
    rand = random.random()
    cumulative = 0
    for rel, prob in RELATIONSHIPS.items():
        cumulative += prob
        if rand <= cumulative:
            return rel
    return 'SELF'


def generate_enrollment(member_id, plan_codes, scenario):
    """Generate enrollment record(s) based on scenario"""
    enrollments = []
    group_number = generate_group_number()
    relationship = choose_relationship()
    
    if scenario == 'single_active':
        # One active enrollment
        effective_date = random_date(2023, 2024)
        enrollments.append({
            'member_number': member_id,
            'plan_code': random.choice(plan_codes),
            'group_number': group_number,
            'effective_date': effective_date.strftime('%Y-%m-%d'),
            'termination_date': '',
            'relationship': relationship,
            'is_active': 'true'
        })
    
    elif scenario == 'dual_coverage':
        # Two active enrollments (primary + secondary)
        effective_date = random_date(2023, 2024)
        
        # Primary coverage
        enrollments.append({
            'member_number': member_id,
            'plan_code': random.choice(plan_codes),
            'group_number': group_number,
            'effective_date': effective_date.strftime('%Y-%m-%d'),
            'termination_date': '',
            'relationship': 'SELF',
            'is_active': 'true'
        })
        
        # Secondary coverage (spouse's plan or Medicare supplement)
        secondary_date = effective_date + timedelta(days=random.randint(0, 30))
        enrollments.append({
            'member_number': member_id,
            'plan_code': random.choice(plan_codes),
            'group_number': generate_group_number(),
            'effective_date': secondary_date.strftime('%Y-%m-%d'),
            'termination_date': '',
            'relationship': 'SPOUSE' if relationship == 'SELF' else relationship,
            'is_active': 'true'
        })
    
    elif scenario == 'plan_transition':
        # Transitioning from old plan to new plan
        old_effective = random_date(2022, 2023)
        old_termination = old_effective + timedelta(days=random.randint(180, 365))
        new_effective = old_termination + timedelta(days=1)
        
        # Old plan (terminated)
        enrollments.append({
            'member_number': member_id,
            'plan_code': random.choice(plan_codes),
            'group_number': group_number,
            'effective_date': old_effective.strftime('%Y-%m-%d'),
            'termination_date': old_termination.strftime('%Y-%m-%d'),
            'relationship': relationship,
            'is_active': 'false'
        })
        
        # New plan (active)
        enrollments.append({
            'member_number': member_id,
            'plan_code': random.choice(plan_codes),
            'group_number': generate_group_number(),
            'effective_date': new_effective.strftime('%Y-%m-%d'),
            'termination_date': '',
            'relationship': relationship,
            'is_active': 'true'
        })
    
    elif scenario == 'historical_only':
        # Only historical enrollments (no active)
        num_historical = random.randint(1, 3)
        current_date = random_date(2020, 2022)
        
        for _ in range(num_historical):
            effective_date = current_date
            termination_date = effective_date + timedelta(days=random.randint(180, 730))
            
            enrollments.append({
                'member_number': member_id,
                'plan_code': random.choice(plan_codes),
                'group_number': generate_group_number(),
                'effective_date': effective_date.strftime('%Y-%m-%d'),
                'termination_date': termination_date.strftime('%Y-%m-%d'),
                'relationship': relationship,
                'is_active': 'false'
            })
            
            current_date = termination_date + timedelta(days=random.randint(1, 30))
    
    return enrollments


def estimate_row_size(row):
    """Estimate size of CSV row in bytes"""
    return len(','.join(str(v) for v in row.values())) + 1


def main():
    print("=" * 80)
    print("PBM Enrollment Data Generator")
    print("=" * 80)
    print()
    
    # Load reference data
    member_ids = load_member_ids()
    if not member_ids:
        return
    
    plan_codes = load_plan_codes()
    if not plan_codes:
        return
    
    print()
    print(f"Generating {TOTAL_RECORDS:,} enrollment records...")
    print(f"Target file size: ~{MAX_FILE_SIZE_MB}MB per file")
    print()
    
    # CSV headers
    headers = ['member_number', 'plan_code', 'group_number', 'effective_date', 
               'termination_date', 'relationship', 'is_active']
    
    # Initialize counters
    file_number = 1
    current_file_size = 0
    records_written = 0
    total_enrollments = 0
    current_file = None
    current_writer = None
    
    # Ensure each member is used at least once
    member_index = 0
    members_used = set()
    
    try:
        # Open first file
        filename = OUTPUT_DIR / f"{FILE_PREFIX}_{file_number:02d}.csv"
        current_file = open(filename, 'w', newline='')
        current_writer = csv.DictWriter(current_file, fieldnames=headers)
        current_writer.writeheader()
        current_file_size = len(','.join(headers)) + 1
        
        print(f"Writing to: {filename}")
        
        while total_enrollments < TOTAL_RECORDS:
            # Ensure all members are used at least once
            if member_index < len(member_ids):
                member_id = member_ids[member_index]
                member_index += 1
            else:
                # After all members used once, select randomly
                member_id = random.choice(member_ids)
            
            members_used.add(member_id)
            
            # Choose scenario and generate enrollments
            scenario = choose_scenario()
            enrollments = generate_enrollment(member_id, plan_codes, scenario)
            
            for enrollment in enrollments:
                if total_enrollments >= TOTAL_RECORDS:
                    break
                
                # Estimate row size
                row_size = estimate_row_size(enrollment)
                
                # Check if we need to start a new file
                if current_file_size + row_size > MAX_FILE_SIZE_MB * 1024 * 1024:
                    current_file.close()
                    records_written = 0
                    file_number += 1
                    filename = OUTPUT_DIR / f"{FILE_PREFIX}_{file_number:02d}.csv"
                    current_file = open(filename, 'w', newline='')
                    current_writer = csv.DictWriter(current_file, fieldnames=headers)
                    current_writer.writeheader()
                    current_file_size = len(','.join(headers)) + 1
                    print(f"Writing to: {filename}")
                
                # Write row
                current_writer.writerow(enrollment)
                current_file_size += row_size
                records_written += 1
                total_enrollments += 1
                
                # Progress update
                if total_enrollments % 100000 == 0:
                    print(f"  Progress: {total_enrollments:,} / {TOTAL_RECORDS:,} "
                          f"({total_enrollments/TOTAL_RECORDS*100:.1f}%) - "
                          f"File {file_number}, Size: {current_file_size/1024/1024:.1f}MB")
        
        # Close last file
        if current_file:
            current_file.close()
        
        print()
        print("=" * 80)
        print("Generation Complete!")
        print("=" * 80)
        print(f"Total enrollment records: {total_enrollments:,}")
        print(f"Total files created: {file_number}")
        print(f"Unique members used: {len(members_used):,} / {len(member_ids):,}")
        print(f"Coverage: {len(members_used)/len(member_ids)*100:.1f}%")
        print()
        
        # Scenario statistics
        print("Enrollment Scenarios Distribution:")
        for scenario, prob in SCENARIOS.items():
            expected = int(TOTAL_RECORDS * prob)
            print(f"  {scenario:20s}: ~{expected:,} ({prob*100:.0f}%)")
        
    except Exception as e:
        print(f"ERROR: {e}")
        if current_file:
            current_file.close()
        raise


if __name__ == "__main__":
    main()

# Made with Bob
