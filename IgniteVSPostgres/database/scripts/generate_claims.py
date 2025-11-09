#!/usr/bin/env python3
"""
Generate synthetic pharmacy claims data for PBM system.
Creates 10,000,000 claims records with proper foreign key relationships.
Outputs to multiple CSV files, each approximately 30MB in size.
"""

import csv
import random
import uuid
from datetime import datetime, timedelta
from decimal import Decimal
import os
import sys

# Configuration
TOTAL_CLAIMS = 10_000_000
TARGET_FILE_SIZE_MB = 30
TARGET_FILE_SIZE_BYTES = TARGET_FILE_SIZE_MB * 1024 * 1024
OUTPUT_DIR = "../src/main/resources/data"
FILE_PREFIX = "us_pharmacy_claims"

# Claim status distribution (based on industry benchmarks)
CLAIM_STATUS_DISTRIBUTION = {
    'APPROVED': 87,      # 87% approved
    'REJECTED': 10,      # 10% rejected
    'PENDING': 2,        # 2% pending
    'REVERSED': 0.5,     # 0.5% reversed
    'REBILLED': 0.5      # 0.5% rebilled
}

# Rejection code distribution (for rejected claims)
REJECTION_CODES = {
    '70': 25,   # Product Not Covered
    '75': 30,   # Prior Authorization Required
    '76': 15,   # Plan Limitations Exceeded
    '79': 15,   # Refill Too Soon
    '85': 10,   # Patient Not Covered
    '88': 5     # DUR Reject
}

# Days supply distribution
DAYS_SUPPLY_DISTRIBUTION = {
    30: 60,   # 60% are 30-day supplies
    60: 15,   # 15% are 60-day supplies
    90: 20,   # 20% are 90-day supplies
    7: 3,     # 3% are 7-day supplies
    14: 2     # 2% are 14-day supplies
}

# Quantity dispensed ranges by days supply
QUANTITY_RANGES = {
    7: (7, 14),
    14: (14, 28),
    30: (30, 90),
    60: (60, 180),
    90: (90, 270)
}

class ClaimsDataGenerator:
    def __init__(self):
        self.member_ids = []
        self.pharmacy_ids = []
        self.drug_ids = []
        self.plan_ids = []
        self.ndc_codes = []
        self.claim_counter = 1
        
        # Load existing data IDs
        self.load_reference_data()
        
    def load_reference_data(self):
        """Load existing member, pharmacy, drug, and plan IDs from CSV files."""
        print("Loading reference data...")
        
        # Load member IDs
        member_files = [f for f in os.listdir(OUTPUT_DIR) if f.startswith('us_pharmacy_members_')]
        for file in member_files:
            filepath = os.path.join(OUTPUT_DIR, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                reader = csv.DictReader(f)
                for row in reader:
                    self.member_ids.append(row['member_id'])
        
        print(f"Loaded {len(self.member_ids)} member IDs")
        
        # Load pharmacy IDs
        pharmacy_file = os.path.join(OUTPUT_DIR, 'us_pharmacy_pharmacies.csv')
        if os.path.exists(pharmacy_file):
            with open(pharmacy_file, 'r', encoding='utf-8') as f:
                reader = csv.DictReader(f)
                for row in reader:
                    self.pharmacy_ids.append(row['pharmacy_id'])
        
        print(f"Loaded {len(self.pharmacy_ids)} pharmacy IDs")
        
        # Load drug IDs and NDC codes
        drug_file = os.path.join(OUTPUT_DIR, 'us_pharmacy_drugs.csv')
        if os.path.exists(drug_file):
            with open(drug_file, 'r', encoding='utf-8') as f:
                reader = csv.DictReader(f)
                for row in reader:
                    self.drug_ids.append(row['drug_id'])
                    self.ndc_codes.append(row['ndc_code'])
        
        print(f"Loaded {len(self.drug_ids)} drug IDs")
        
        # Load plan IDs
        plan_file = os.path.join(OUTPUT_DIR, 'us_pharmacy_plans.csv')
        if os.path.exists(plan_file):
            with open(plan_file, 'r', encoding='utf-8') as f:
                reader = csv.DictReader(f)
                for row in reader:
                    self.plan_ids.append(row['plan_id'])
        
        print(f"Loaded {len(self.plan_ids)} plan IDs")
        
        # Validate we have enough reference data
        if not self.member_ids or not self.pharmacy_ids or not self.drug_ids or not self.plan_ids:
            print("ERROR: Missing reference data. Please generate members, pharmacies, drugs, and plans first.")
            sys.exit(1)
    
    def weighted_choice(self, choices_dict):
        """Select a random choice based on weighted distribution."""
        choices = list(choices_dict.keys())
        weights = list(choices_dict.values())
        return random.choices(choices, weights=weights, k=1)[0]
    
    def generate_service_date(self):
        """Generate a random service date in 2024-2025."""
        start_date = datetime(2024, 1, 1)
        end_date = datetime(2025, 12, 31)
        days_between = (end_date - start_date).days
        random_days = random.randint(0, days_between)
        return start_date + timedelta(days=random_days)
    
    def generate_claim_number(self):
        """Generate unique claim number."""
        claim_num = f"CLM{self.claim_counter:015d}"
        self.claim_counter += 1
        return claim_num
    
    def calculate_pricing(self, days_supply, quantity, status):
        """Calculate realistic pricing for a claim."""
        # Base ingredient cost per unit (varies by drug type)
        unit_cost = random.uniform(0.50, 150.00)
        ingredient_cost = round(Decimal(unit_cost * quantity), 2)
        
        # Dispensing fee (typically $1-$5)
        dispensing_fee = round(Decimal(random.uniform(1.00, 5.00)), 2)
        
        # Total cost
        total_cost = ingredient_cost + dispensing_fee
        
        if status == 'APPROVED':
            # Calculate patient pay based on tier (simulated)
            tier = random.randint(1, 5)
            if tier == 1:
                patient_pay = round(Decimal(random.uniform(5.00, 15.00)), 2)
            elif tier == 2:
                patient_pay = round(Decimal(random.uniform(15.00, 35.00)), 2)
            elif tier == 3:
                patient_pay = round(Decimal(random.uniform(35.00, 70.00)), 2)
            elif tier == 4:
                # Specialty - coinsurance (30%)
                patient_pay = round(total_cost * Decimal('0.30'), 2)
            else:
                # High-cost specialty - coinsurance (30%)
                patient_pay = round(total_cost * Decimal('0.30'), 2)
            
            # Ensure patient pay doesn't exceed total cost
            if patient_pay > total_cost:
                patient_pay = total_cost
            
            plan_pay = total_cost - patient_pay
        else:
            # Rejected/Pending claims have no payment
            patient_pay = Decimal('0.00')
            plan_pay = Decimal('0.00')
        
        return ingredient_cost, dispensing_fee, total_cost, patient_pay, plan_pay
    
    def generate_claim(self):
        """Generate a single claim record."""
        # Select random foreign keys
        member_id = random.choice(self.member_ids)
        pharmacy_id = random.choice(self.pharmacy_ids)
        drug_id = random.choice(self.drug_ids)
        plan_id = random.choice(self.plan_ids)
        
        # Generate dates
        service_date = self.generate_service_date()
        fill_date = service_date + timedelta(days=random.randint(0, 2))
        
        # Generate claim number
        claim_number = self.generate_claim_number()
        
        # Determine claim status
        status = self.weighted_choice(CLAIM_STATUS_DISTRIBUTION)
        
        # Determine rejection code if rejected
        rejection_code = ''
        if status == 'REJECTED':
            rejection_code = self.weighted_choice(REJECTION_CODES)
        
        # Generate days supply and quantity
        days_supply = self.weighted_choice(DAYS_SUPPLY_DISTRIBUTION)
        quantity_min, quantity_max = QUANTITY_RANGES[days_supply]
        quantity_dispensed = random.randint(quantity_min, quantity_max)
        
        # Calculate pricing
        ingredient_cost, dispensing_fee, total_cost, patient_pay, plan_pay = \
            self.calculate_pricing(days_supply, quantity_dispensed, status)
        
        # Generate timestamps
        submitted_at = service_date + timedelta(hours=random.randint(0, 48))
        
        if status in ['APPROVED', 'REJECTED']:
            # Processed within minutes to hours of submission
            processed_at = submitted_at + timedelta(seconds=random.randint(1, 3600))
        elif status == 'REVERSED':
            # Reversed after being approved
            processed_at = submitted_at + timedelta(seconds=random.randint(1, 3600))
        elif status == 'REBILLED':
            # Rebilled after initial rejection
            processed_at = submitted_at + timedelta(days=random.randint(1, 7))
        else:
            # Pending - not yet processed
            processed_at = ''
        
        created_at = submitted_at
        updated_at = processed_at if processed_at else submitted_at
        
        return {
            'claim_id': str(uuid.uuid4()),
            'claim_number': claim_number,
            'member_id': member_id,
            'pharmacy_id': pharmacy_id,
            'drug_id': drug_id,
            'plan_id': plan_id,
            'service_date': service_date.strftime('%Y-%m-%d'),
            'fill_date': fill_date.strftime('%Y-%m-%d'),
            'quantity_dispensed': quantity_dispensed,
            'days_supply': days_supply,
            'ingredient_cost': f"{ingredient_cost:.2f}",
            'dispensing_fee': f"{dispensing_fee:.2f}",
            'total_cost': f"{total_cost:.2f}",
            'patient_pay': f"{patient_pay:.2f}",
            'plan_pay': f"{plan_pay:.2f}",
            'claim_status': status,
            'rejection_code': rejection_code,
            'submitted_at': submitted_at.strftime('%Y-%m-%d %H:%M:%S'),
            'processed_at': processed_at.strftime('%Y-%m-%d %H:%M:%S') if processed_at else '',
            'created_at': created_at.strftime('%Y-%m-%d %H:%M:%S'),
            'updated_at': updated_at.strftime('%Y-%m-%d %H:%M:%S') if updated_at else created_at.strftime('%Y-%m-%d %H:%M:%S')
        }
    
    def estimate_row_size(self, row):
        """Estimate the size of a CSV row in bytes."""
        return len(','.join(str(v) for v in row.values()).encode('utf-8')) + 1  # +1 for newline
    
    def generate_claims(self):
        """Generate all claims and write to multiple CSV files."""
        print(f"\nGenerating {TOTAL_CLAIMS:,} claims...")
        print(f"Target file size: {TARGET_FILE_SIZE_MB}MB per file")
        
        # CSV headers
        headers = [
            'claim_id', 'claim_number', 'member_id', 'pharmacy_id', 'drug_id', 'plan_id',
            'service_date', 'fill_date', 'quantity_dispensed', 'days_supply',
            'ingredient_cost', 'dispensing_fee', 'total_cost', 'patient_pay', 'plan_pay',
            'claim_status', 'rejection_code', 'submitted_at', 'processed_at',
            'created_at', 'updated_at'
        ]
        
        file_number = 1
        current_file_size = 0
        claims_in_current_file = 0
        total_claims_generated = 0
        
        # Create output directory if it doesn't exist
        os.makedirs(OUTPUT_DIR, exist_ok=True)
        
        # Open first file
        filename = os.path.join(OUTPUT_DIR, f"{FILE_PREFIX}_{file_number:02d}.csv")
        csvfile = open(filename, 'w', newline='', encoding='utf-8')
        writer = csv.DictWriter(csvfile, fieldnames=headers)
        writer.writeheader()
        
        print(f"\nWriting to {filename}...")
        
        try:
            while total_claims_generated < TOTAL_CLAIMS:
                # Generate claim
                claim = self.generate_claim()
                
                # Estimate row size
                row_size = self.estimate_row_size(claim)
                
                # Check if we need to start a new file
                if current_file_size + row_size > TARGET_FILE_SIZE_BYTES and claims_in_current_file > 0:
                    csvfile.close()
                    print(f"  Completed: {claims_in_current_file:,} claims, {current_file_size / 1024 / 1024:.2f}MB")
                    
                    # Start new file
                    file_number += 1
                    filename = os.path.join(OUTPUT_DIR, f"{FILE_PREFIX}_{file_number:02d}.csv")
                    csvfile = open(filename, 'w', newline='', encoding='utf-8')
                    writer = csv.DictWriter(csvfile, fieldnames=headers)
                    writer.writeheader()
                    
                    current_file_size = 0
                    claims_in_current_file = 0
                    print(f"\nWriting to {filename}...")
                
                # Write claim
                writer.writerow(claim)
                current_file_size += row_size
                claims_in_current_file += 1
                total_claims_generated += 1
                
                # Progress update
                if total_claims_generated % 100000 == 0:
                    progress = (total_claims_generated / TOTAL_CLAIMS) * 100
                    print(f"  Progress: {total_claims_generated:,} / {TOTAL_CLAIMS:,} ({progress:.1f}%)")
        
        finally:
            csvfile.close()
            print(f"  Completed: {claims_in_current_file:,} claims, {current_file_size / 1024 / 1024:.2f}MB")
        
        print(f"\n✓ Successfully generated {total_claims_generated:,} claims across {file_number} files")
        
        # Print statistics
        self.print_statistics(file_number)
    
    def print_statistics(self, num_files):
        """Print generation statistics."""
        print("\n" + "="*60)
        print("GENERATION STATISTICS")
        print("="*60)
        print(f"Total claims generated: {TOTAL_CLAIMS:,}")
        print(f"Number of files: {num_files}")
        print(f"Average claims per file: {TOTAL_CLAIMS // num_files:,}")
        print(f"\nExpected distribution:")
        print(f"  Approved: ~{int(TOTAL_CLAIMS * 0.87):,} ({CLAIM_STATUS_DISTRIBUTION['APPROVED']}%)")
        print(f"  Rejected: ~{int(TOTAL_CLAIMS * 0.10):,} ({CLAIM_STATUS_DISTRIBUTION['REJECTED']}%)")
        print(f"  Pending: ~{int(TOTAL_CLAIMS * 0.02):,} ({CLAIM_STATUS_DISTRIBUTION['PENDING']}%)")
        print(f"  Reversed: ~{int(TOTAL_CLAIMS * 0.005):,} ({CLAIM_STATUS_DISTRIBUTION['REVERSED']}%)")
        print(f"  Rebilled: ~{int(TOTAL_CLAIMS * 0.005):,} ({CLAIM_STATUS_DISTRIBUTION['REBILLED']}%)")
        print("\nFiles location: " + OUTPUT_DIR)
        print("="*60)

def main():
    """Main execution function."""
    print("="*60)
    print("PHARMACY CLAIMS DATA GENERATOR")
    print("="*60)
    print(f"Target: {TOTAL_CLAIMS:,} claims")
    print(f"File size: ~{TARGET_FILE_SIZE_MB}MB per file")
    print("="*60)
    
    # Create generator
    generator = ClaimsDataGenerator()
    
    # Generate claims
    generator.generate_claims()
    
    print("\n✓ Claims generation completed successfully!")

if __name__ == "__main__":
    main()

# Made with Bob
