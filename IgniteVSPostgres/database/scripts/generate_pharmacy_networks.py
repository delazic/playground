#!/usr/bin/env python3
"""
Generate synthetic pharmacy network data for US healthcare system.

This script creates pharmacy network records that link pharmacies to different
pharmacy benefit networks (PBMs, insurance networks, etc.).

Pharmacy networks in US healthcare:
- Retail networks (CVS Caremark, Express Scripts, OptumRx)
- Specialty pharmacy networks
- Mail-order networks
- Preferred/Standard/Out-of-network tiers
- Regional and national networks
"""

import csv
import random
import os
from datetime import datetime, timedelta
from typing import List, Dict

# Configuration
OUTPUT_DIR = "../../src/main/resources/data"
PHARMACY_FILE = "../../src/main/resources/data/us_pharmacy_pharmacies.csv"
OUTPUT_FILE_PREFIX = "us_pharmacy_pharmacy_networks"
TARGET_FILE_SIZE_MB = 30
ESTIMATED_ROW_SIZE_BYTES = 150  # Approximate size per row

# Major pharmacy networks in the US
PHARMACY_NETWORKS = [
    # Major PBM Networks
    {"name": "CVS Caremark Network", "type": "PBM", "tier": "Preferred"},
    {"name": "CVS Caremark Standard Network", "type": "PBM", "tier": "Standard"},
    {"name": "Express Scripts Network", "type": "PBM", "tier": "Preferred"},
    {"name": "Express Scripts Standard Network", "type": "PBM", "tier": "Standard"},
    {"name": "OptumRx Network", "type": "PBM", "tier": "Preferred"},
    {"name": "OptumRx Standard Network", "type": "PBM", "tier": "Standard"},
    {"name": "Humana Pharmacy Network", "type": "PBM", "tier": "Preferred"},
    {"name": "Prime Therapeutics Network", "type": "PBM", "tier": "Preferred"},
    
    # Retail Networks
    {"name": "Walgreens Retail Network", "type": "Retail", "tier": "Preferred"},
    {"name": "CVS Retail Network", "type": "Retail", "tier": "Preferred"},
    {"name": "Walmart Pharmacy Network", "type": "Retail", "tier": "Standard"},
    {"name": "Kroger Pharmacy Network", "type": "Retail", "tier": "Standard"},
    {"name": "Rite Aid Network", "type": "Retail", "tier": "Standard"},
    
    # Specialty Networks
    {"name": "Accredo Specialty Network", "type": "Specialty", "tier": "Preferred"},
    {"name": "CVS Specialty Network", "type": "Specialty", "tier": "Preferred"},
    {"name": "Walgreens Specialty Network", "type": "Specialty", "tier": "Preferred"},
    {"name": "BriovaRx Specialty Network", "type": "Specialty", "tier": "Standard"},
    
    # Mail Order Networks
    {"name": "Express Scripts Mail Order", "type": "Mail-Order", "tier": "Preferred"},
    {"name": "CVS Caremark Mail Order", "type": "Mail-Order", "tier": "Preferred"},
    {"name": "OptumRx Mail Order", "type": "Mail-Order", "tier": "Preferred"},
    
    # Regional Networks
    {"name": "Northeast Regional Network", "type": "Regional", "tier": "Standard"},
    {"name": "Southeast Regional Network", "type": "Regional", "tier": "Standard"},
    {"name": "Midwest Regional Network", "type": "Regional", "tier": "Standard"},
    {"name": "Southwest Regional Network", "type": "Regional", "tier": "Standard"},
    {"name": "West Coast Regional Network", "type": "Regional", "tier": "Standard"},
    
    # Independent Networks
    {"name": "Independent Pharmacy Network", "type": "Independent", "tier": "Standard"},
    {"name": "Community Pharmacy Network", "type": "Independent", "tier": "Standard"},
    {"name": "Health Mart Network", "type": "Independent", "tier": "Standard"},
]

# Network status options
NETWORK_STATUS = ["Active", "Active", "Active", "Active", "Inactive", "Pending"]

# Contract types
CONTRACT_TYPES = ["Direct", "Indirect", "PSAO", "Aggregator"]


def read_pharmacy_ids_from_header(pharmacy_file: str) -> List[str]:
    """Read only first 2 lines of pharmacy CSV to understand the ID pattern."""
    pharmacy_ids = []
    
    if not os.path.exists(pharmacy_file):
        print(f"Warning: Pharmacy file not found at {pharmacy_file}")
        print("Generating sample pharmacy IDs instead...")
        return [f"PHARM{str(i).zfill(8)}" for i in range(1, 100001)]
    
    try:
        with open(pharmacy_file, 'r', encoding='utf-8') as f:
            reader = csv.DictReader(f)
            # Read only first 2 lines as per requirements
            for i, row in enumerate(reader):
                if i >= 2:
                    break
                if 'pharmacy_id' in row:
                    pharmacy_ids.append(row['pharmacy_id'])
        
        # If we got IDs from the file, extrapolate the pattern
        if pharmacy_ids:
            print(f"Sample pharmacy IDs from file: {pharmacy_ids}")
            # Assume sequential IDs and generate more based on pattern
            if pharmacy_ids[0].startswith('PHARM'):
                start_num = int(pharmacy_ids[0].replace('PHARM', ''))
                # Generate 100,000 pharmacy IDs based on pattern
                pharmacy_ids = [f"PHARM{str(i).zfill(8)}" for i in range(start_num, start_num + 100000)]
                print(f"Generated {len(pharmacy_ids):,} pharmacy IDs based on pattern")
        
    except Exception as e:
        print(f"Error reading pharmacy file: {e}")
        print("Generating sample pharmacy IDs instead...")
        pharmacy_ids = [f"PHARM{str(i).zfill(8)}" for i in range(1, 100001)]
    
    return pharmacy_ids


def generate_network_id(index: int) -> str:
    """Generate a unique network ID."""
    return f"NET{str(index).zfill(10)}"


def generate_effective_date() -> str:
    """Generate a random effective date within the last 5 years."""
    days_ago = random.randint(0, 1825)  # 5 years
    date = datetime.now() - timedelta(days=days_ago)
    return date.strftime('%Y-%m-%d')


def generate_termination_date(effective_date: str, status: str) -> str:
    """Generate termination date based on status."""
    if status == "Active":
        # Active contracts typically don't have termination dates
        if random.random() < 0.9:  # 90% no termination date
            return ""
        # 10% have future termination date
        eff_date = datetime.strptime(effective_date, '%Y-%m-%d')
        days_future = random.randint(30, 730)  # 1 month to 2 years
        term_date = eff_date + timedelta(days=days_future)
        return term_date.strftime('%Y-%m-%d')
    elif status == "Inactive":
        # Inactive contracts have past termination dates
        eff_date = datetime.strptime(effective_date, '%Y-%m-%d')
        days_after = random.randint(180, 1095)  # 6 months to 3 years
        term_date = eff_date + timedelta(days=days_after)
        if term_date > datetime.now():
            term_date = datetime.now() - timedelta(days=random.randint(1, 365))
        return term_date.strftime('%Y-%m-%d')
    else:  # Pending
        return ""


def generate_reimbursement_rate() -> str:
    """Generate reimbursement rate (AWP - discount %)."""
    # Common reimbursement formulas: AWP-15%, AWP-18%, AWP-20%, etc.
    discount = random.choice([12, 13, 14, 15, 16, 17, 18, 19, 20, 22, 24])
    return f"AWP-{discount}%"


def generate_dispensing_fee() -> float:
    """Generate dispensing fee."""
    # Typical range $0.50 to $3.50
    return round(random.uniform(0.50, 3.50), 2)


def generate_pharmacy_network_record(
    network_id: str,
    pharmacy_id: str,
    network_info: Dict
) -> Dict:
    """Generate a single pharmacy network record."""
    status = random.choice(NETWORK_STATUS)
    effective_date = generate_effective_date()
    
    return {
        'network_id': network_id,
        'pharmacy_id': pharmacy_id,
        'network_name': network_info['name'],
        'network_type': network_info['type'],
        'network_tier': network_info['tier'],
        'contract_type': random.choice(CONTRACT_TYPES),
        'effective_date': effective_date,
        'termination_date': generate_termination_date(effective_date, status),
        'status': status,
        'reimbursement_rate': generate_reimbursement_rate(),
        'dispensing_fee': generate_dispensing_fee(),
        'is_preferred': 'true' if network_info['tier'] == 'Preferred' else 'false',
        'is_mail_order': 'true' if network_info['type'] == 'Mail-Order' else 'false',
        'is_specialty': 'true' if network_info['type'] == 'Specialty' else 'false',
        'created_at': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
        'updated_at': datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    }


def calculate_rows_per_file(target_size_mb: int, row_size_bytes: int) -> int:
    """Calculate how many rows fit in target file size."""
    target_size_bytes = target_size_mb * 1024 * 1024
    return target_size_bytes // row_size_bytes


def generate_pharmacy_networks(pharmacy_ids: List[str], output_dir: str):
    """Generate pharmacy network data and save to CSV files."""
    
    # Ensure output directory exists
    os.makedirs(output_dir, exist_ok=True)
    
    # Calculate rows per file
    rows_per_file = calculate_rows_per_file(TARGET_FILE_SIZE_MB, ESTIMATED_ROW_SIZE_BYTES)
    print(f"Target rows per file: ~{rows_per_file:,}")
    
    # Each pharmacy can be in multiple networks (typically 3-8 networks)
    networks_per_pharmacy = lambda: random.randint(3, 8)
    
    # Calculate total records
    total_pharmacies = len(pharmacy_ids)
    avg_networks = 5.5  # Average of 3-8
    estimated_total_records = int(total_pharmacies * avg_networks)
    estimated_files = (estimated_total_records // rows_per_file) + 1
    
    print(f"\nGenerating pharmacy network data...")
    print(f"Total pharmacies: {total_pharmacies:,}")
    print(f"Estimated total records: {estimated_total_records:,}")
    print(f"Estimated files: {estimated_files}")
    
    fieldnames = [
        'network_id', 'pharmacy_id', 'network_name', 'network_type', 'network_tier',
        'contract_type', 'effective_date', 'termination_date', 'status',
        'reimbursement_rate', 'dispensing_fee', 'is_preferred', 'is_mail_order',
        'is_specialty', 'created_at', 'updated_at'
    ]
    
    file_number = 1
    current_file_rows = 0
    current_writer = None
    current_file = None
    network_id_counter = 1
    total_records_written = 0
    
    try:
        for pharmacy_idx, pharmacy_id in enumerate(pharmacy_ids):
            # Determine how many networks this pharmacy belongs to
            num_networks = networks_per_pharmacy()
            
            # Randomly select networks for this pharmacy
            selected_networks = random.sample(PHARMACY_NETWORKS, min(num_networks, len(PHARMACY_NETWORKS)))
            
            for network_info in selected_networks:
                # Check if we need to create a new file
                if current_writer is None or current_file_rows >= rows_per_file:
                    if current_file:
                        current_file.close()
                        print(f"  Completed file {file_number}: {current_file_rows:,} rows")
                    
                    filename = f"{OUTPUT_FILE_PREFIX}_{str(file_number).zfill(2)}.csv"
                    filepath = os.path.join(output_dir, filename)
                    current_file = open(filepath, 'w', newline='', encoding='utf-8')
                    current_writer = csv.DictWriter(current_file, fieldnames=fieldnames)
                    current_writer.writeheader()
                    current_file_rows = 0
                    file_number += 1
                
                # Generate and write record
                record = generate_pharmacy_network_record(
                    generate_network_id(network_id_counter),
                    pharmacy_id,
                    network_info
                )
                current_writer.writerow(record)
                current_file_rows += 1
                network_id_counter += 1
                total_records_written += 1
            
            # Progress indicator
            if (pharmacy_idx + 1) % 10000 == 0:
                print(f"  Processed {pharmacy_idx + 1:,} pharmacies, generated {total_records_written:,} network records")
        
        # Close the last file
        if current_file:
            current_file.close()
            print(f"  Completed file {file_number - 1}: {current_file_rows:,} rows")
        
        print(f"\n✓ Successfully generated {total_records_written:,} pharmacy network records")
        print(f"✓ Created {file_number - 1} CSV file(s)")
        
    except Exception as e:
        print(f"\n✗ Error generating pharmacy networks: {e}")
        if current_file:
            current_file.close()
        raise


def main():
    """Main execution function."""
    print("=" * 70)
    print("Pharmacy Network Data Generator")
    print("=" * 70)
    
    # Read pharmacy IDs (only first 2 lines)
    print(f"\nReading pharmacy IDs from: {PHARMACY_FILE}")
    pharmacy_ids = read_pharmacy_ids_from_header(PHARMACY_FILE)
    
    if not pharmacy_ids:
        print("✗ No pharmacy IDs found. Exiting.")
        return
    
    # Generate pharmacy network data
    generate_pharmacy_networks(pharmacy_ids, OUTPUT_DIR)
    
    print("\n" + "=" * 70)
    print("Generation complete!")
    print("=" * 70)


if __name__ == "__main__":
    main()

# Made with Bob
