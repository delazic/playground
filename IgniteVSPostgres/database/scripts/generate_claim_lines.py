#!/usr/bin/env python3
"""
Generate synthetic claim line data for pharmacy claims.
Creates realistic claim line records following US healthcare PBM standards.
Each claim can have 1-5 lines (medications), with most having 1 line.
"""

import csv
import random
import os
import sys
from datetime import datetime, timedelta
from decimal import Decimal

# Configuration
OUTPUT_DIR = "../../src/main/resources/data"
OUTPUT_FILE_PREFIX = "us_pharmacy_claim_lines"
TARGET_FILE_SIZE_MB = 30
ESTIMATED_BYTES_PER_ROW = 400

# Line distribution per claim (realistic pattern)
LINES_PER_CLAIM_DISTRIBUTION = {
    1: 85,  # 85% single-line claims
    2: 12,  # 12% two-line claims
    3: 2,   # 2% three-line claims
    4: 0.5, # 0.5% four-line claims
    5: 0.5  # 0.5% five-line claims
}

# Tier distribution
TIER_DISTRIBUTION = {
    1: 60,  # Generic
    2: 25,  # Preferred brand
    3: 10,  # Non-preferred brand
    4: 4,   # Specialty
    5: 1    # High-cost specialty
}

# Status distribution
STATUS_DISTRIBUTION = {
    'APPROVED': 87,
    'DENIED': 10,
    'PENDING': 2,
    'ADJUSTED': 1
}

# Denial codes for denied lines
DENIAL_CODES = {
    '70': 'Product not covered',
    '75': 'Prior authorization required',
    '76': 'Plan limitations exceeded',
    '79': 'Refill too soon',
    '85': 'Patient not covered',
    '88': 'DUR reject',
    'M4': 'Missing/invalid NDC',
    'M6': 'Missing/invalid quantity'
}

# DAW codes
DAW_CODES = ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9']

# Unit of measure
UNIT_OF_MEASURE = ['EA', 'ML', 'GM', 'MG', 'EACH']

class ClaimLineGenerator:
    def __init__(self):
        self.claims = []
        self.drugs = []
        self.line_id_counter = 1
        self.load_reference_data()
    
    def load_reference_data(self):
        """Load existing claims and drugs data."""
        print("Loading reference data...")
        
        # Load claims
        claim_files = [f for f in os.listdir(OUTPUT_DIR) 
                      if f.startswith('us_pharmacy_claims_simulation_1m_')]
        
        for file in sorted(claim_files):
            filepath = os.path.join(OUTPUT_DIR, file)
            print(f"  Loading claims from {file}...")
            with open(filepath, 'r', encoding='utf-8') as f:
                reader = csv.DictReader(f)
                for row in reader:
                    self.claims.append({
                        'claim_number': row['claim_number'],
                        'date_of_service': row['date_of_service'],
                        'ndc': row['ndc'],
                        'quantity_dispensed': row['quantity_dispensed'],
                        'days_supply': row['days_supply'],
                        'prescription_number': row['prescription_number'],
                        'refill_number': row['refill_number'],
                        'prescriber_npi': row['prescriber_npi'],
                        'ingredient_cost_submitted': row['ingredient_cost_submitted'],
                        'dispensing_fee_submitted': row['dispensing_fee_submitted'],
                        'patient_pay_amount': row['patient_pay_amount'],
                        'plan_pay_amount': row['plan_pay_amount']
                    })
        
        print(f"Loaded {len(self.claims)} claims")
        
        # Load drugs
        drug_file = os.path.join(OUTPUT_DIR, 'us_pharmacy_drugs.csv')
        if os.path.exists(drug_file):
            with open(drug_file, 'r', encoding='utf-8') as f:
                reader = csv.DictReader(f)
                for row in reader:
                    self.drugs.append({
                        'ndc_code': row['ndc_code'],
                        'drug_name': row['drug_name'],
                        'generic_name': row['generic_name'],
                        'is_generic': row['is_generic']
                    })
        
        print(f"Loaded {len(self.drugs)} drugs")
        
        if not self.claims or not self.drugs:
            print("ERROR: Missing reference data. Generate claims and drugs first.")
            sys.exit(1)
    
    def weighted_choice(self, choices_dict):
        """Select random choice based on weighted distribution."""
        choices = list(choices_dict.keys())
        weights = list(choices_dict.values())
        return random.choices(choices, weights=weights, k=1)[0]
    
    def get_drug_info(self, ndc):
        """Get drug information by NDC."""
        for drug in self.drugs:
            if drug['ndc_code'] == ndc:
                return drug
        # Return default if not found
        return {
            'ndc_code': ndc,
            'drug_name': 'Unknown Drug',
            'generic_name': 'Unknown',
            'is_generic': 'true'
        }
    
    def calculate_line_pricing(self, tier, ingredient_cost, dispensing_fee):
        """Calculate pricing for a claim line based on tier."""
        ingredient_cost = Decimal(ingredient_cost)
        dispensing_fee = Decimal(dispensing_fee)
        
        billed_amount = ingredient_cost + dispensing_fee
        
        # Calculate allowed amount (typically 80-95% of billed for approved)
        allowed_amount = billed_amount * Decimal(random.uniform(0.80, 0.95))
        allowed_amount = round(allowed_amount, 2)
        
        # Calculate patient responsibility based on tier
        if tier == 1:
            copay = Decimal(random.uniform(5.00, 15.00))
            coinsurance = Decimal('0.00')
        elif tier == 2:
            copay = Decimal(random.uniform(15.00, 35.00))
            coinsurance = Decimal('0.00')
        elif tier == 3:
            copay = Decimal(random.uniform(35.00, 70.00))
            coinsurance = Decimal('0.00')
        elif tier == 4:
            copay = Decimal('0.00')
            coinsurance = allowed_amount * Decimal('0.30')
        else:  # tier 5
            copay = Decimal('0.00')
            coinsurance = allowed_amount * Decimal('0.30')
        
        copay = round(copay, 2)
        coinsurance = round(coinsurance, 2)
        
        # Deductible (10% of claims have deductible applied)
        if random.random() < 0.10:
            deductible = round(Decimal(random.uniform(10.00, 50.00)), 2)
        else:
            deductible = Decimal('0.00')
        
        patient_responsibility = copay + coinsurance + deductible
        
        # Ensure patient responsibility doesn't exceed allowed amount
        if patient_responsibility > allowed_amount:
            patient_responsibility = allowed_amount
            copay = allowed_amount
            coinsurance = Decimal('0.00')
            deductible = Decimal('0.00')
        
        paid_amount = allowed_amount - patient_responsibility
        
        # Sales tax (5% of claims have sales tax)
        if random.random() < 0.05:
            sales_tax = round(billed_amount * Decimal('0.06'), 2)
        else:
            sales_tax = Decimal('0.00')
        
        return {
            'billed_amount': billed_amount,
            'allowed_amount': allowed_amount,
            'paid_amount': paid_amount,
            'patient_responsibility': patient_responsibility,
            'copay_amount': copay,
            'coinsurance_amount': coinsurance,
            'deductible_amount': deductible,
            'sales_tax': sales_tax
        }
    
    def generate_claim_line(self, claim, line_number, use_claim_drug=True):
        """Generate a single claim line."""
        line_id = self.line_id_counter
        self.line_id_counter += 1
        
        # Determine if this line uses the claim's drug or a different one
        if use_claim_drug or line_number == 1:
            ndc = claim['ndc']
            quantity = claim['quantity_dispensed']
            days_supply = claim['days_supply']
            ingredient_cost = claim['ingredient_cost_submitted']
            dispensing_fee = claim['dispensing_fee_submitted']
            prescription_number = claim['prescription_number']
            refill_number = claim['refill_number']
        else:
            # Additional medication - select random drug
            random_drug = random.choice(self.drugs)
            ndc = random_drug['ndc_code']
            days_supply = random.choice([7, 14, 30, 60, 90])
            quantity = random.randint(30, 90) if days_supply == 30 else random.randint(90, 270)
            ingredient_cost = round(Decimal(random.uniform(10.00, 200.00)), 2)
            dispensing_fee = round(Decimal(random.uniform(1.50, 3.50)), 2)
            prescription_number = f"RX{random.randint(1000000, 9999999)}"
            refill_number = random.randint(0, 11)
        
        # Get drug info
        drug_info = self.get_drug_info(ndc)
        
        # Determine tier
        tier = self.weighted_choice(TIER_DISTRIBUTION)
        
        # Determine status
        line_status = self.weighted_choice(STATUS_DISTRIBUTION)
        
        # Calculate pricing
        pricing = self.calculate_line_pricing(tier, ingredient_cost, dispensing_fee)
        
        # Denial information
        if line_status == 'DENIED':
            denial_code = random.choice(list(DENIAL_CODES.keys()))
            denial_reason = DENIAL_CODES[denial_code]
            pricing['paid_amount'] = Decimal('0.00')
        else:
            denial_code = ''
            denial_reason = ''
        
        # Adjustment information
        if line_status == 'ADJUSTED':
            adjustment_reason = random.choice([
                'Pricing correction',
                'Quantity adjustment',
                'Coordination of benefits',
                'Duplicate claim adjustment'
            ])
        else:
            adjustment_reason = ''
        
        # DAW code
        daw_code = random.choices(DAW_CODES, weights=[70, 10, 5, 5, 3, 2, 2, 1, 1, 1], k=1)[0]
        
        # Generic indicator
        generic_indicator = drug_info['is_generic'].lower() == 'true'
        brand_indicator = not generic_indicator
        
        # Unit of measure
        unit_of_measure = random.choice(UNIT_OF_MEASURE)
        
        # Rendering provider NPI (same as prescriber or different)
        if random.random() < 0.90:
            rendering_provider_npi = claim['prescriber_npi']
        else:
            rendering_provider_npi = f"{random.randint(1000000000, 9999999999)}"
        
        # Service facility NPI (pharmacy NPI - would need to load from pharmacy data)
        service_facility_npi = f"{random.randint(1000000000, 9999999999)}"
        
        # Place of service (pharmacy codes)
        place_of_service = random.choice(['01', '11', '12', '20', '21'])
        
        # Prior auth number (5% of claims)
        if random.random() < 0.05:
            prior_auth_number = f"PA{random.randint(100000, 999999)}"
        else:
            prior_auth_number = ''
        
        # Formulary status
        formulary_status = random.choices(
            ['COVERED', 'NON_COVERED', 'RESTRICTED'],
            weights=[85, 10, 5],
            k=1
        )[0]
        
        # Date written (1-30 days before service date)
        service_date = datetime.strptime(claim['date_of_service'], '%Y-%m-%d')
        date_written = service_date - timedelta(days=random.randint(1, 30))
        
        # Processing time (50-500ms)
        processing_time_ms = random.randint(50, 500)
        
        # Timestamps
        created_at = datetime.now()
        
        return {
            'claim_line_id': line_id,
            'claim_number': claim['claim_number'],
            'line_number': line_number,
            'service_date': claim['date_of_service'],
            'ndc': ndc,
            'drug_name': drug_info['drug_name'],
            'quantity_dispensed': quantity,
            'days_supply': days_supply,
            'unit_of_measure': unit_of_measure,
            'rendering_provider_npi': rendering_provider_npi,
            'service_facility_npi': service_facility_npi,
            'place_of_service': place_of_service,
            'billed_amount': f"{pricing['billed_amount']:.2f}",
            'allowed_amount': f"{pricing['allowed_amount']:.2f}",
            'paid_amount': f"{pricing['paid_amount']:.2f}",
            'patient_responsibility': f"{pricing['patient_responsibility']:.2f}",
            'copay_amount': f"{pricing['copay_amount']:.2f}",
            'coinsurance_amount': f"{pricing['coinsurance_amount']:.2f}",
            'deductible_amount': f"{pricing['deductible_amount']:.2f}",
            'ingredient_cost': f"{ingredient_cost}",
            'dispensing_fee': f"{dispensing_fee}",
            'sales_tax': f"{pricing['sales_tax']:.2f}",
            'line_status': line_status,
            'denial_code': denial_code,
            'denial_reason': denial_reason,
            'adjustment_reason': adjustment_reason,
            'prior_auth_number': prior_auth_number,
            'formulary_status': formulary_status,
            'tier_level': tier,
            'daw_code': daw_code,
            'generic_indicator': str(generic_indicator).lower(),
            'brand_indicator': str(brand_indicator).lower(),
            'prescription_number': prescription_number,
            'refill_number': refill_number,
            'date_written': date_written.strftime('%Y-%m-%d'),
            'prescriber_npi': claim['prescriber_npi'],
            'processing_time_ms': processing_time_ms,
            'created_at': created_at.strftime('%Y-%m-%d %H:%M:%S')
        }
    
    def generate_all_claim_lines(self):
        """Generate claim lines for all claims."""
        print(f"\nGenerating claim lines for {len(self.claims)} claims...")
        print(f"Target file size: {TARGET_FILE_SIZE_MB} MB per file")
        
        headers = [
            'claim_line_id', 'claim_number', 'line_number', 'service_date',
            'ndc', 'drug_name', 'quantity_dispensed', 'days_supply', 'unit_of_measure',
            'rendering_provider_npi', 'service_facility_npi', 'place_of_service',
            'billed_amount', 'allowed_amount', 'paid_amount', 'patient_responsibility',
            'copay_amount', 'coinsurance_amount', 'deductible_amount',
            'ingredient_cost', 'dispensing_fee', 'sales_tax',
            'line_status', 'denial_code', 'denial_reason', 'adjustment_reason',
            'prior_auth_number', 'formulary_status', 'tier_level', 'daw_code',
            'generic_indicator', 'brand_indicator',
            'prescription_number', 'refill_number', 'date_written', 'prescriber_npi',
            'processing_time_ms', 'created_at'
        ]
        
        # Create output directory
        os.makedirs(OUTPUT_DIR, exist_ok=True)
        
        # Calculate rows per file
        target_bytes = TARGET_FILE_SIZE_MB * 1024 * 1024
        rows_per_file = target_bytes // ESTIMATED_BYTES_PER_ROW
        print(f"Estimated rows per file: {rows_per_file:,}")
        
        # Initialize file tracking
        file_number = 1
        rows_in_current_file = 0
        current_file = None
        writer = None
        total_lines_generated = 0
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
        
        # Generate claim lines for each claim
        for idx, claim in enumerate(self.claims):
            # Determine number of lines for this claim
            num_lines = self.weighted_choice(LINES_PER_CLAIM_DISTRIBUTION)
            
            # Generate lines
            for line_num in range(1, num_lines + 1):
                # Check if we need a new file
                if rows_in_current_file >= rows_per_file:
                    writer = open_new_file()
                
                # First line uses claim's drug, additional lines use random drugs
                use_claim_drug = (line_num == 1)
                line = self.generate_claim_line(claim, line_num, use_claim_drug)
                writer.writerow(line)
                rows_in_current_file += 1
                total_lines_generated += 1
            
            # Progress reporting
            if (idx + 1) % 50000 == 0:
                progress = ((idx + 1) / len(self.claims)) * 100
                print(f"  Progress: {idx + 1:,} / {len(self.claims):,} claims ({progress:.1f}%) - {total_lines_generated:,} lines")
        
        # Close last file
        if current_file:
            current_file.close()
            file_size_mb = os.path.getsize(current_filename) / (1024 * 1024)
            print(f"  ✓ Completed: {os.path.basename(current_filename)} ({file_size_mb:.2f} MB)")
        
        print(f"\n✓ Successfully generated {total_lines_generated:,} claim lines in {file_number - 1} files")
        
        # Print file summary
        print("\n" + "="*60)
        print("GENERATED FILES:")
        print("="*60)
        for filepath in generated_files:
            file_size_mb = os.path.getsize(filepath) / (1024 * 1024)
            with open(filepath, 'r') as f:
                line_count = sum(1 for _ in f) - 1  # Subtract header
            print(f"  {os.path.basename(filepath)}: {file_size_mb:.2f} MB ({line_count:,} lines)")
        print("="*60)
        
        # Print statistics
        self.print_statistics(total_lines_generated, file_number - 1)
    
    def print_statistics(self, total_lines, num_files):
        """Print generation statistics."""
        print("\n" + "="*60)
        print("GENERATION STATISTICS")
        print("="*60)
        print(f"Total claims processed: {len(self.claims):,}")
        print(f"Total claim lines generated: {total_lines:,}")
        print(f"Average lines per claim: {total_lines / len(self.claims):.2f}")
        print(f"Number of files: {num_files}")
        print(f"Target file size: {TARGET_FILE_SIZE_MB} MB")
        print(f"\nLine distribution:")
        print(f"  Single-line claims: ~{int(len(self.claims) * 0.85):,} (85%)")
        print(f"  Multi-line claims: ~{int(len(self.claims) * 0.15):,} (15%)")
        print(f"\nStatus distribution (estimated):")
        print(f"  Approved: ~{int(total_lines * 0.87):,} (87%)")
        print(f"  Denied: ~{int(total_lines * 0.10):,} (10%)")
        print(f"  Pending: ~{int(total_lines * 0.02):,} (2%)")
        print(f"  Adjusted: ~{int(total_lines * 0.01):,} (1%)")
        print(f"\nTier distribution (estimated):")
        print(f"  Tier 1 (Generic): ~{int(total_lines * 0.60):,} (60%)")
        print(f"  Tier 2 (Preferred Brand): ~{int(total_lines * 0.25):,} (25%)")
        print(f"  Tier 3 (Non-Preferred): ~{int(total_lines * 0.10):,} (10%)")
        print(f"  Tier 4-5 (Specialty): ~{int(total_lines * 0.05):,} (5%)")
        print("\nFile location: " + OUTPUT_DIR)
        print(f"File pattern: {OUTPUT_FILE_PREFIX}_XX.csv")
        print("="*60)

def main():
    """Main execution function."""
    print("="*60)
    print("CLAIM LINE DATA GENERATOR")
    print("="*60)
    print("Generates claim line details for existing pharmacy claims")
    print("Following US healthcare PBM standards")
    print("="*60)
    
    # Create generator
    generator = ClaimLineGenerator()
    
    # Generate claim lines
    generator.generate_all_claim_lines()
    
    print("\n✓ Claim line generation completed successfully!")
    print("\nNext steps:")
    print("1. Load claim lines into database")
    print("2. Verify foreign key relationships")
    print("3. Test claim line CRUD operations")

if __name__ == "__main__":
    main()

# Made with Bob
