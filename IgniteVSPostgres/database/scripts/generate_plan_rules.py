#!/usr/bin/env python3
"""
Generate synthetic plan rules data for pharmacy benefit plans.
Creates realistic plan rule records following US healthcare PBM standards.
Target: 20,000-30,000 rules for 30 plans (667-1000 rules per plan).
"""

import csv
import random
import os
import sys
import json
from datetime import datetime, timedelta

# Configuration
OUTPUT_DIR = "../../src/main/resources/data"
OUTPUT_FILE_PREFIX = "us_pharmacy_plan_rules"
TARGET_FILE_SIZE_MB = 30
ESTIMATED_BYTES_PER_ROW = 800  # JSONB fields make rows larger
NUM_PLANS = 30
TARGET_TOTAL_RULES = 25000  # Middle of 20k-30k range
RULES_PER_PLAN_MIN = 667
RULES_PER_PLAN_MAX = 1000

# Rule type distribution (based on PLAN_RULES.md)
RULE_TYPE_DISTRIBUTION = {
    'COVERAGE': 30,
    'PRIOR_AUTH': 20,
    'QUANTITY_LIMIT': 15,
    'COST_SHARE': 15,
    'CLINICAL_EDIT': 10,
    'REFILL_RESTRICTION': 5,
    'NETWORK_RESTRICTION': 3,
    'STEP_THERAPY': 2
}

# Priority distribution
PRIORITY_DISTRIBUTION = {
    'HIGH': (50, 100, 10),      # (min, max, weight)
    'NORMAL': (0, 49, 80),
    'LOW': (-100, -1, 10)
}

# Active status distribution
ACTIVE_STATUS_DISTRIBUTION = {
    True: 95,
    False: 5
}

# Drug classes for rules
DRUG_CLASSES = [
    'ANTIHYPERTENSIVE', 'ANTIDIABETIC', 'STATIN', 'ANTIBIOTIC',
    'ANTIDEPRESSANT', 'ANTIPSYCHOTIC', 'ANTICOAGULANT', 'OPIOID_ANALGESIC',
    'NSAID', 'PROTON_PUMP_INHIBITOR', 'BRONCHODILATOR', 'CORTICOSTEROID',
    'ANTICONVULSANT', 'IMMUNOSUPPRESSANT', 'SPECIALTY_ONCOLOGY',
    'SPECIALTY_RHEUMATOLOGY', 'SPECIALTY_MS', 'SPECIALTY_HIV',
    'CONTRACEPTIVE', 'ERECTILE_DYSFUNCTION', 'SLEEP_AID', 'ANTIHISTAMINE'
]

# Diagnosis codes (ICD-10)
DIAGNOSIS_CODES = [
    'E11.9', 'I10', 'J45.909', 'F32.9', 'M79.3', 'K21.9',
    'C50.9', 'C61', 'M05.9', 'G35', 'B20'
]

# Pharmacy types
PHARMACY_TYPES = ['RETAIL', 'MAIL_ORDER', 'SPECIALTY', 'LONG_TERM_CARE']

# Denial/rejection messages
DENIAL_MESSAGES = [
    'Product not covered',
    'Prior authorization required',
    'Quantity limit exceeded',
    'Refill too soon',
    'Step therapy required',
    'Age restriction not met',
    'Gender restriction not met',
    'Duplicate therapy detected'
]

class PlanRuleGenerator:
    def __init__(self):
        self.rule_id_counter = 1
        self.plans = []
        self.load_plan_data()
    
    def load_plan_data(self):
        """Load plan IDs from existing data."""
        print("Loading plan data...")
        
        # For 30 plans, generate plan_ids 1-30
        # In real scenario, would load from CSV
        self.plans = list(range(1, NUM_PLANS + 1))
        
        print(f"Loaded {len(self.plans)} plans")
    
    def weighted_choice(self, choices_dict):
        """Select random choice based on weighted distribution."""
        choices = list(choices_dict.keys())
        weights = list(choices_dict.values())
        return random.choices(choices, weights=weights, k=1)[0]
    
    def generate_priority(self):
        """Generate priority based on distribution."""
        priority_level = self.weighted_choice({
            'HIGH': 10,
            'NORMAL': 80,
            'LOW': 10
        })
        
        if priority_level == 'HIGH':
            return random.randint(50, 100)
        elif priority_level == 'NORMAL':
            return random.randint(0, 49)
        else:
            return random.randint(-100, -1)
    
    def generate_coverage_rule(self, plan_id):
        """Generate a coverage rule."""
        drug_class = random.choice(DRUG_CLASSES)
        is_generic = random.choice([True, False])
        tier = random.randint(1, 5)
        
        criteria = {
            "drug_class": drug_class,
            "is_generic": is_generic
        }
        
        # Add optional criteria
        if random.random() < 0.3:
            criteria["specialty"] = random.choice([True, False])
        
        action = {
            "covered": random.choice([True, False]) if random.random() < 0.1 else True,
            "tier": tier,
            "preferred": random.choice([True, False])
        }
        
        if not action["covered"]:
            action["denial_message"] = "Product not covered under plan"
        
        rule_name = f"Coverage for {drug_class} - Tier {tier}"
        
        return criteria, action, rule_name
    
    def generate_prior_auth_rule(self, plan_id):
        """Generate a prior authorization rule."""
        drug_class = random.choice(DRUG_CLASSES)
        
        criteria = {
            "drug_class": drug_class
        }
        
        # Add age restriction (30% of PA rules)
        if random.random() < 0.3:
            criteria["min_age"] = random.choice([18, 21, 65])
        
        # Add diagnosis requirement (50% of PA rules)
        if random.random() < 0.5:
            criteria["diagnosis_required"] = True
            criteria["diagnosis_codes"] = random.sample(DIAGNOSIS_CODES, random.randint(1, 3))
        
        # Add quantity threshold (20% of PA rules)
        if random.random() < 0.2:
            criteria["quantity_threshold"] = random.choice([30, 60, 90, 100])
        
        action = {
            "requires_pa": True,
            "pa_type": random.choice(["CLINICAL_REVIEW", "AUTOMATED", "PRESCRIBER_ATTESTATION"]),
            "approval_duration_days": random.choice([30, 60, 90, 180, 365])
        }
        
        if criteria.get("diagnosis_required"):
            action["required_documents"] = ["diagnosis", "treatment_plan"]
        
        rule_name = f"Prior Auth for {drug_class}"
        
        return criteria, action, rule_name
    
    def generate_quantity_limit_rule(self, plan_id):
        """Generate a quantity limit rule."""
        drug_class = random.choice(DRUG_CLASSES)
        
        criteria = {
            "drug_class": drug_class
        }
        
        # Add condition-specific criteria (40% of rules)
        if random.random() < 0.4:
            criteria["acute_pain"] = random.choice([True, False])
        
        max_quantity = random.choice([30, 60, 90, 100, 120])
        max_days_supply = random.choice([7, 14, 30, 60, 90])
        
        action = {
            "max_quantity": max_quantity,
            "max_days_supply": max_days_supply,
            "max_refills": random.randint(0, 11),
            "override_allowed": random.choice([True, False])
        }
        
        rule_name = f"Quantity Limit for {drug_class} - Max {max_quantity} units"
        
        return criteria, action, rule_name
    
    def generate_cost_share_rule(self, plan_id):
        """Generate a cost share rule."""
        tier = random.randint(1, 5)
        pharmacy_type = random.choice(PHARMACY_TYPES)
        days_supply = random.choice([30, 60, 90])
        
        criteria = {
            "tier": tier,
            "pharmacy_type": pharmacy_type,
            "days_supply": days_supply
        }
        
        # Calculate copay/coinsurance based on tier
        if tier == 1:
            copay = round(random.uniform(5.00, 15.00), 2)
            coinsurance = 0.00
        elif tier == 2:
            copay = round(random.uniform(15.00, 35.00), 2)
            coinsurance = 0.00
        elif tier == 3:
            copay = round(random.uniform(35.00, 70.00), 2)
            coinsurance = 0.00
        elif tier == 4:
            copay = 0.00
            coinsurance = 0.30
        else:  # tier 5
            copay = 0.00
            coinsurance = 0.30
        
        action = {
            "copay": copay,
            "coinsurance": coinsurance,
            "apply_deductible": random.choice([True, False])
        }
        
        # Mail order discount (20% less copay)
        if pharmacy_type == 'MAIL_ORDER' and copay > 0:
            action["copay"] = round(copay * 0.80, 2)
        
        rule_name = f"Cost Share Tier {tier} - {pharmacy_type} - {days_supply} days"
        
        return criteria, action, rule_name
    
    def generate_clinical_edit_rule(self, plan_id):
        """Generate a clinical edit rule."""
        drug_class = random.choice(DRUG_CLASSES)
        
        criteria = {
            "drug_class": drug_class
        }
        
        # Age restriction (60% of clinical edits)
        if random.random() < 0.6:
            if random.random() < 0.5:
                criteria["min_age"] = random.choice([18, 21, 65])
            else:
                criteria["max_age"] = random.choice([12, 17, 21])
        
        # Gender restriction (30% of clinical edits)
        if random.random() < 0.3:
            criteria["gender"] = random.choice(['M', 'F'])
        
        # Pregnancy category (20% of clinical edits)
        if random.random() < 0.2:
            criteria["pregnancy_category"] = random.choice(['X', 'D'])
            if "gender" not in criteria:
                criteria["gender"] = 'F'
            criteria["age_range"] = [15, 45]
        
        action = {
            "action": random.choice(["REJECT", "REQUIRE_OVERRIDE", "WARNING"]),
            "warning_message": random.choice(DENIAL_MESSAGES)
        }
        
        if action["action"] == "REQUIRE_OVERRIDE":
            action["prescriber_contact_required"] = random.choice([True, False])
        
        rule_name = f"Clinical Edit for {drug_class}"
        
        return criteria, action, rule_name
    
    def generate_refill_restriction_rule(self, plan_id):
        """Generate a refill restriction rule."""
        drug_class = random.choice(DRUG_CLASSES)
        
        criteria = {
            "drug_class": drug_class
        }
        
        # Controlled substance schedule (50% of refill rules)
        if random.random() < 0.5:
            criteria["dea_schedule"] = random.choice(["II", "III", "IV", "V"])
        
        action = {
            "refill_too_soon_threshold": random.choice([0.75, 0.80, 0.85, 0.90]),
            "early_refill_days": random.choice([0, 2, 3, 5]),
            "vacation_override": random.choice([True, False]),
            "lost_stolen_allowed": random.choice([True, False])
        }
        
        rule_name = f"Refill Restriction for {drug_class}"
        
        return criteria, action, rule_name
    
    def generate_network_restriction_rule(self, plan_id):
        """Generate a network restriction rule."""
        drug_type = random.choice(['SPECIALTY', 'MAINTENANCE', 'ACUTE'])
        
        criteria = {
            "drug_type": drug_type
        }
        
        # Cost threshold (40% of network rules)
        if random.random() < 0.4:
            criteria["cost_threshold"] = round(random.uniform(500.00, 2000.00), 2)
        
        # Days supply requirement (30% of network rules)
        if random.random() < 0.3:
            criteria["min_days_supply"] = random.choice([60, 90])
        
        required_pharmacy = random.choice(PHARMACY_TYPES)
        
        action = {
            "required_pharmacy_type": required_pharmacy,
            "out_of_network_allowed": random.choice([True, False])
        }
        
        if required_pharmacy == 'SPECIALTY':
            action["preferred_pharmacies"] = [f"SPEC{str(i).zfill(3)}" for i in random.sample(range(1, 100), 3)]
        
        rule_name = f"Network Restriction - {drug_type} requires {required_pharmacy}"
        
        return criteria, action, rule_name
    
    def generate_step_therapy_rule(self, plan_id):
        """Generate a step therapy rule."""
        drug_class = random.choice(DRUG_CLASSES)
        
        criteria = {
            "drug_class": drug_class
        }
        
        # Diagnosis requirement (60% of step therapy rules)
        if random.random() < 0.6:
            criteria["diagnosis"] = random.choice(DIAGNOSIS_CODES)
        
        # First-line drugs (generic names)
        first_line_drugs = [
            f"{drug_class.lower()}_generic_{i}" 
            for i in range(1, random.randint(2, 4))
        ]
        
        action = {
            "required_first_line": first_line_drugs,
            "trial_duration_days": random.choice([14, 30, 60, 90]),
            "failure_criteria": random.choice([
                "inadequate_response",
                "adverse_reaction",
                "contraindication"
            ]),
            "override_with_pa": True
        }
        
        rule_name = f"Step Therapy for {drug_class}"
        
        return criteria, action, rule_name
    
    def generate_rule(self, plan_id, rule_type):
        """Generate a rule based on type."""
        if rule_type == 'COVERAGE':
            criteria, action, rule_name = self.generate_coverage_rule(plan_id)
        elif rule_type == 'PRIOR_AUTH':
            criteria, action, rule_name = self.generate_prior_auth_rule(plan_id)
        elif rule_type == 'QUANTITY_LIMIT':
            criteria, action, rule_name = self.generate_quantity_limit_rule(plan_id)
        elif rule_type == 'COST_SHARE':
            criteria, action, rule_name = self.generate_cost_share_rule(plan_id)
        elif rule_type == 'CLINICAL_EDIT':
            criteria, action, rule_name = self.generate_clinical_edit_rule(plan_id)
        elif rule_type == 'REFILL_RESTRICTION':
            criteria, action, rule_name = self.generate_refill_restriction_rule(plan_id)
        elif rule_type == 'NETWORK_RESTRICTION':
            criteria, action, rule_name = self.generate_network_restriction_rule(plan_id)
        elif rule_type == 'STEP_THERAPY':
            criteria, action, rule_name = self.generate_step_therapy_rule(plan_id)
        else:
            raise ValueError(f"Unknown rule type: {rule_type}")
        
        rule_id = self.rule_id_counter
        self.rule_id_counter += 1
        
        priority = self.generate_priority()
        is_active = self.weighted_choice(ACTIVE_STATUS_DISTRIBUTION)
        
        # Timestamps
        created_at = datetime.now() - timedelta(days=random.randint(0, 365))
        
        return {
            'rule_id': rule_id,
            'plan_id': plan_id,
            'rule_type': rule_type,
            'rule_name': rule_name,
            'rule_criteria': json.dumps(criteria),
            'rule_action': json.dumps(action),
            'priority': priority,
            'is_active': str(is_active).lower(),
            'created_at': created_at.strftime('%Y-%m-%d %H:%M:%S')
        }
    
    def generate_all_rules(self):
        """Generate all plan rules."""
        print(f"\nGenerating plan rules for {len(self.plans)} plans...")
        print(f"Target: {TARGET_TOTAL_RULES:,} total rules")
        print(f"Rules per plan: {RULES_PER_PLAN_MIN}-{RULES_PER_PLAN_MAX}")
        print(f"Target file size: {TARGET_FILE_SIZE_MB} MB per file")
        
        headers = [
            'rule_id', 'plan_id', 'rule_type', 'rule_name',
            'rule_criteria', 'rule_action', 'priority', 'is_active', 'created_at'
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
        total_rules_generated = 0
        generated_files = []
        current_filename = ""
        
        # Statistics tracking
        rule_type_counts = {rt: 0 for rt in RULE_TYPE_DISTRIBUTION.keys()}
        
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
        
        # Generate rules for each plan
        for plan_id in self.plans:
            # Determine number of rules for this plan
            num_rules = random.randint(RULES_PER_PLAN_MIN, RULES_PER_PLAN_MAX)
            
            # Generate rules
            for _ in range(num_rules):
                # Check if we need a new file
                if rows_in_current_file >= rows_per_file:
                    writer = open_new_file()
                
                # Select rule type based on distribution
                rule_type = self.weighted_choice(RULE_TYPE_DISTRIBUTION)
                
                # Generate rule
                rule = self.generate_rule(plan_id, rule_type)
                writer.writerow(rule)
                
                rows_in_current_file += 1
                total_rules_generated += 1
                rule_type_counts[rule_type] += 1
            
            # Progress reporting
            if plan_id % 5 == 0:
                progress = (plan_id / len(self.plans)) * 100
                print(f"  Progress: {plan_id} / {len(self.plans)} plans ({progress:.1f}%) - {total_rules_generated:,} rules")
        
        # Close last file
        if current_file:
            current_file.close()
            file_size_mb = os.path.getsize(current_filename) / (1024 * 1024)
            print(f"  ✓ Completed: {os.path.basename(current_filename)} ({file_size_mb:.2f} MB)")
        
        print(f"\n✓ Successfully generated {total_rules_generated:,} plan rules in {file_number - 1} files")
        
        # Print file summary
        print("\n" + "="*60)
        print("GENERATED FILES:")
        print("="*60)
        for filepath in generated_files:
            file_size_mb = os.path.getsize(filepath) / (1024 * 1024)
            with open(filepath, 'r') as f:
                line_count = sum(1 for _ in f) - 1  # Subtract header
            print(f"  {os.path.basename(filepath)}: {file_size_mb:.2f} MB ({line_count:,} rules)")
        print("="*60)
        
        # Print statistics
        self.print_statistics(total_rules_generated, file_number - 1, rule_type_counts)
    
    def print_statistics(self, total_rules, num_files, rule_type_counts):
        """Print generation statistics."""
        print("\n" + "="*60)
        print("GENERATION STATISTICS")
        print("="*60)
        print(f"Total plans: {len(self.plans)}")
        print(f"Total rules generated: {total_rules:,}")
        print(f"Average rules per plan: {total_rules / len(self.plans):.0f}")
        print(f"Number of files: {num_files}")
        print(f"Target file size: {TARGET_FILE_SIZE_MB} MB")
        
        print(f"\nRule type distribution:")
        for rule_type, count in sorted(rule_type_counts.items(), key=lambda x: x[1], reverse=True):
            percentage = (count / total_rules) * 100
            print(f"  {rule_type}: {count:,} ({percentage:.1f}%)")
        
        print(f"\nPriority distribution (estimated):")
        print(f"  High (50-100): ~{int(total_rules * 0.10):,} (10%)")
        print(f"  Normal (0-49): ~{int(total_rules * 0.80):,} (80%)")
        print(f"  Low (-100 to -1): ~{int(total_rules * 0.10):,} (10%)")
        
        print(f"\nActive status distribution:")
        print(f"  Active: ~{int(total_rules * 0.95):,} (95%)")
        print(f"  Inactive: ~{int(total_rules * 0.05):,} (5%)")
        
        print("\nFile location: " + OUTPUT_DIR)
        print(f"File pattern: {OUTPUT_FILE_PREFIX}_XX.csv")
        print("="*60)

def main():
    """Main execution function."""
    print("="*60)
    print("PLAN RULES DATA GENERATOR")
    print("="*60)
    print("Generates plan rules for pharmacy benefit plans")
    print("Following US healthcare PBM standards")
    print("="*60)
    
    # Set random seed for reproducibility
    random.seed(42)
    
    # Create generator
    generator = PlanRuleGenerator()
    
    # Generate plan rules
    generator.generate_all_rules()
    
    print("\n✓ Plan rules generation completed successfully!")
    print("\nNext steps:")
    print("1. Load plan rules into database")
    print("2. Verify foreign key relationships with plans")
    print("3. Test plan rule CRUD operations")
    print("4. Test JSONB queries on rule_criteria and rule_action")

if __name__ == "__main__":
    main()

# Made with Bob
