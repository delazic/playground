#!/usr/bin/env python3
"""
Generate 1 million synthetic pharmacy claims for PBM adjudication simulation.
Creates realistic claim data matching NCPDP format for a typical day's processing.
"""

import csv
import random
import uuid
from datetime import datetime, timedelta
from decimal import Decimal
import os
import sys

# Configuration
TOTAL_CLAIMS = 1_000_000
OUTPUT_DIR = "../../src/main/resources/data"
OUTPUT_FILE_PREFIX = "us_pharmacy_claims_simulation_1m"
TARGET_FILE_SIZE_MB = 30  # Target size for each file in MB
ESTIMATED_BYTES_PER_ROW = 250  # Approximate size of one CSV row

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
    90: 20,   # 20% are 90-day supplies
    60: 15,   # 15% are 60-day supplies
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

# Transaction types
TRANSACTION_TYPES = ['B1', 'B2', 'B3']  # Billing, Reversal, Rebill

class ClaimsDataGenerator:
    def __init__(self):
        self.member_ids = []
        self.pharmacy_ids = []
        self.drug_ndcs = []
        self.claim_counter = 1
        
        # Load existing data IDs
        self.load_reference_data()
        
    def load_reference_data(self):
        """Load existing member, pharmacy, and drug data from CSV files."""
        print("Loading reference data...")
        
        # Load member IDs
        member_files = [f for f in os.listdir(OUTPUT_DIR) if f.startswith('us_pharmacy_members_')]
        for file in sorted(member_files)[:5]:  # Load first 5 files for variety
            filepath = os.path.join(OUTPUT_DIR, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                reader = csv.DictReader(f)
                for row in reader:
                    self.member_ids.append(row['member_number'])
        
        print(f"Loaded {len(self.member_ids)} member IDs")
        
        # Load pharmacy IDs
        pharmacy_file = os.path.join(OUTPUT_DIR, 'us_pharmacy_pharmacies.csv')
        if os.path.exists(pharmacy_file):
            with open(pharmacy_file, 'r', encoding='utf-8') as f:
                reader = csv.DictReader(f)
                for row in reader:
                    self.pharmacy_ids.append(row['ncpdp_id'])
        
        print(f"Loaded {len(self.pharmacy_ids)} pharmacy IDs")
        
        # Load drug NDC codes
        drug_file = os.path.join(OUTPUT_DIR, 'us_pharmacy_drugs.csv')
        if os.path.exists(drug_file):
            with open(drug_file, 'r', encoding='utf-8') as f:
                reader = csv.DictReader(f)
                for row in reader:
                    self.drug_ndcs.append(row['ndc_code'])
        
        print(f"Loaded {len(self.drug_ndcs)} drug NDC codes")
        
        # Validate we have enough reference data
        if not self.member_ids or not self.pharmacy_ids or not self.drug_ndcs:
            print("ERROR: Missing reference data. Please generate members, pharmacies, and drugs first.")
            sys.exit(1)
    
    def weighted_choice(self, choices_dict):
        """Select a random choice based on weighted distribution."""
        choices = list(choices_dict.keys())
        weights = list(choices_dict.values())
        return random.choices(choices, weights=weights, k=1)[0]
    
    def generate_service_date_time(self, day_offset_hours):
        """Generate service date/time for a specific hour of the day."""
        # Simulate one day: 2024-11-09
        base_date = datetime(2024, 11, 9, 0, 0, 0)
        service_datetime = base_date + timedelta(hours=day_offset_hours)
        return service_datetime
    
    def generate_claim_number(self):
        """Generate unique claim number."""
        claim_num = f"CLM{self.claim_counter:015d}"
        self.claim_counter += 1
        return claim_num
    
    def calculate_pricing(self, days_supply, quantity):
        """Calculate realistic pricing for a claim."""
        # Base ingredient cost per unit (varies by drug type)
        unit_cost = random.uniform(0.50, 150.00)
        ingredient_cost = round(Decimal(unit_cost * quantity), 2)
        
        # Dispensing fee (typically $1-$5)
        dispensing_fee = round(Decimal(random.uniform(1.50, 3.50)), 2)
        
        # Total cost
        total_cost = ingredient_cost + dispensing_fee
        
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
        
        return ingredient_cost, dispensing_fee, patient_pay, plan_pay
    
    def generate_claim(self, hour_of_day):
        """Generate a single claim record for a specific hour."""
        # Select random foreign keys
        member_id = random.choice(self.member_ids)
        pharmacy_id = random.choice(self.pharmacy_ids)
        ndc = random.choice(self.drug_ndcs)
        
        # Generate service date/time
        service_datetime = self.generate_service_date_time(hour_of_day)
        date_of_service = service_datetime.date()
        
        # Generate claim number
        claim_number = self.generate_claim_number()
        
        # Transaction type (mostly B1 - Billing)
        transaction_type = random.choices(['B1', 'B2', 'B3'], weights=[95, 3, 2], k=1)[0]
        
        # Generate days supply and quantity
        days_supply = self.weighted_choice(DAYS_SUPPLY_DISTRIBUTION)
        quantity_min, quantity_max = QUANTITY_RANGES[days_supply]
        quantity_dispensed = random.randint(quantity_min, quantity_max)
        
        # Refill number (0-11, with 0 being most common)
        refill_number = random.choices(range(12), weights=[40] + [5]*11, k=1)[0]
        
        # DAW code (0-9, with 0 being most common)
        daw_code = random.choices(range(10), weights=[70, 10, 5, 5, 3, 2, 2, 1, 1, 1], k=1)[0]
        
        # Calculate pricing
        ingredient_cost, dispensing_fee, patient_pay, plan_pay = \
            self.calculate_pricing(days_supply, quantity_dispensed)
        
        # Person code (01=cardholder, 02-09=dependents)
        person_code = random.choices(['01', '02', '03', '04'], weights=[60, 25, 10, 5], k=1)[0]
        
        # Prescriber NPI (10-digit)
        prescriber_npi = f"{random.randint(1000000000, 9999999999)}"
        
        # Prescription number
        prescription_number = f"RX{random.randint(1000000, 9999999)}"
        
        # Date prescription written (1-30 days before service date)
        date_written = date_of_service - timedelta(days=random.randint(1, 30))
        
        return {
            'claim_number': claim_number,
            'transaction_type': transaction_type,
            'date_of_service': date_of_service.strftime('%Y-%m-%d'),
            'received_timestamp': service_datetime.strftime('%Y-%m-%d %H:%M:%S'),
            'member_id': member_id,
            'person_code': person_code,
            'pharmacy_id': pharmacy_id,
            'pharmacy_npi': f"{random.randint(1000000000, 9999999999)}",
            'prescription_number': prescription_number,
            'ndc': ndc,
            'quantity_dispensed': f"{quantity_dispensed}",
            'days_supply': days_supply,
            'refill_number': refill_number,
            'daw_code': daw_code,
            'date_written': date_written.strftime('%Y-%m-%d'),
            'prescriber_npi': prescriber_npi,
            'prescriber_id': f"PRES{random.randint(100000, 999999)}",
            'ingredient_cost_submitted': f"{ingredient_cost:.2f}",
            'dispensing_fee_submitted': f"{dispensing_fee:.2f}",
            'patient_pay_amount': f"{patient_pay:.2f}",
            'plan_pay_amount': f"{plan_pay:.2f}",
            'tax_amount': '0.00'
        }
    
    def generate_claims(self):
        """Generate all claims distributed across 24 hours, split into multiple files."""
        print(f"\nGenerating {TOTAL_CLAIMS:,} claims for one day simulation...")
        print(f"Target file size: {TARGET_FILE_SIZE_MB} MB per file")
        
        # CSV headers matching NCPDP format
        headers = [
            'claim_number', 'transaction_type', 'date_of_service', 'received_timestamp',
            'member_id', 'person_code', 'pharmacy_id', 'pharmacy_npi',
            'prescription_number', 'ndc', 'quantity_dispensed', 'days_supply',
            'refill_number', 'daw_code', 'date_written',
            'prescriber_npi', 'prescriber_id',
            'ingredient_cost_submitted', 'dispensing_fee_submitted',
            'patient_pay_amount', 'plan_pay_amount', 'tax_amount'
        ]
        
        # Create output directory if it doesn't exist
        os.makedirs(OUTPUT_DIR, exist_ok=True)
        
        # Calculate rows per file
        target_bytes = TARGET_FILE_SIZE_MB * 1024 * 1024
        rows_per_file = target_bytes // ESTIMATED_BYTES_PER_ROW
        print(f"Estimated rows per file: {rows_per_file:,}")
        
        # Hourly distribution (realistic PBM pattern)
        hourly_distribution = [
            0.02, 0.01, 0.01, 0.01, 0.02, 0.03,  # 12AM-6AM: 10%
            0.05, 0.07, 0.09, 0.10, 0.09, 0.08,  # 6AM-12PM: 48%
            0.07, 0.06, 0.06, 0.07, 0.08, 0.07,  # 12PM-6PM: 41%
            0.03, 0.02, 0.02, 0.01, 0.01, 0.01   # 6PM-12AM: 10%
        ]
        
        # Initialize file tracking
        file_number = 1
        rows_in_current_file = 0
        current_file = None
        writer = None
        total_generated = 0
        generated_files = []
        current_filename = ""
        
        def open_new_file():
            nonlocal current_file, writer, file_number, rows_in_current_file, current_filename
            if current_file:
                current_file.close()
                file_size_mb = os.path.getsize(current_filename) / (1024 * 1024)
                print(f"  ✓ Completed: {os.path.basename(current_filename)} ({file_size_mb:.2f} MB)")
            
            filename = f"{OUTPUT_FILE_PREFIX}_{file_number:02d}.csv"
            current_filename = os.path.join(OUTPUT_DIR, filename)
            generated_files.append(current_filename)
            current_file = open(current_filename, 'w', newline='', encoding='utf-8')
            writer = csv.DictWriter(current_file, fieldnames=headers)
            writer.writeheader()
            print(f"  Creating file {file_number}: {filename}")
            file_number += 1
            rows_in_current_file = 0
            return writer
        
        # Open first file
        writer = open_new_file()
        
        # Generate claims hour by hour
        for hour in range(24):
            claims_this_hour = int(TOTAL_CLAIMS * hourly_distribution[hour])
            
            print(f"Hour {hour:02d}:00 - Generating {claims_this_hour:,} claims...")
            
            for _ in range(claims_this_hour):
                # Check if we need a new file
                if rows_in_current_file >= rows_per_file:
                    writer = open_new_file()
                
                claim = self.generate_claim(hour)
                writer.writerow(claim)
                rows_in_current_file += 1
                total_generated += 1
                
                if total_generated % 50000 == 0:
                    progress = (total_generated / TOTAL_CLAIMS) * 100
                    print(f"  Progress: {total_generated:,} / {TOTAL_CLAIMS:,} ({progress:.1f}%)")
        
        # Generate remaining claims to reach exactly 1M
        remaining = TOTAL_CLAIMS - total_generated
        if remaining > 0:
            print(f"Generating remaining {remaining:,} claims...")
            for _ in range(remaining):
                # Check if we need a new file
                if rows_in_current_file >= rows_per_file:
                    writer = open_new_file()
                
                hour = random.randint(8, 17)  # Peak hours
                claim = self.generate_claim(hour)
                writer.writerow(claim)
                rows_in_current_file += 1
                total_generated += 1
        
        # Close last file
        if current_file:
            current_file.close()
            file_size_mb = os.path.getsize(current_filename) / (1024 * 1024)
            print(f"  ✓ Completed: {os.path.basename(current_filename)} ({file_size_mb:.2f} MB)")
        
        print(f"\n✓ Successfully generated {total_generated:,} claims in {file_number - 1} files")
        
        # Print file summary
        print("\n" + "="*60)
        print("GENERATED FILES:")
        print("="*60)
        for filepath in generated_files:
            file_size_mb = os.path.getsize(filepath) / (1024 * 1024)
            print(f"  {os.path.basename(filepath)}: {file_size_mb:.2f} MB")
        print("="*60)
        
        # Print statistics
        self.print_statistics(file_number - 1)
    
    def print_statistics(self, num_files):
        """Print generation statistics."""
        print("\n" + "="*60)
        print("GENERATION STATISTICS")
        print("="*60)
        print(f"Total claims generated: {TOTAL_CLAIMS:,}")
        print(f"Number of files: {num_files}")
        print(f"Target file size: {TARGET_FILE_SIZE_MB} MB")
        print(f"Simulation date: 2024-11-09 (one full day)")
        print(f"\nHourly distribution:")
        print(f"  Peak hours (9AM-12PM): ~{int(TOTAL_CLAIMS * 0.28):,} claims")
        print(f"  Business hours (8AM-6PM): ~{int(TOTAL_CLAIMS * 0.70):,} claims")
        print(f"  Off hours (6PM-8AM): ~{int(TOTAL_CLAIMS * 0.30):,} claims")
        print(f"\nTransaction mix:")
        print(f"  B1 (Billing): ~{int(TOTAL_CLAIMS * 0.95):,} (95%)")
        print(f"  B2 (Reversal): ~{int(TOTAL_CLAIMS * 0.03):,} (3%)")
        print(f"  B3 (Rebill): ~{int(TOTAL_CLAIMS * 0.02):,} (2%)")
        print("\nFile location: " + OUTPUT_DIR)
        print(f"File pattern: {OUTPUT_FILE_PREFIX}_XX.csv")
        print("="*60)

def main():
    """Main execution function."""
    print("="*60)
    print("PBM CLAIMS SIMULATION DATA GENERATOR")
    print("="*60)
    print(f"Target: {TOTAL_CLAIMS:,} claims (1 day simulation)")
    print("Simulates mid-size PBM processing volume")
    print("="*60)
    
    # Create generator
    generator = ClaimsDataGenerator()
    
    # Generate claims
    generator.generate_claims()
    
    print("\n✓ Claims generation completed successfully!")
    print("\nNext steps:")
    print("1. Run ClaimSimulationApp to process these claims")
    print("2. Monitor adjudication statistics and performance")

if __name__ == "__main__":
    main()

