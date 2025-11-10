#!/usr/bin/env python3
"""
Generate drug interaction data for PBM system.
Creates 20,000 drug interaction records with realistic data.
Outputs CSV files of approximately 30 MB each.
"""

import csv
import random
import uuid
from datetime import datetime, timedelta
from pathlib import Path

# Common drug names (expanded list)
DRUGS = [
    # Cardiovascular
    "Warfarin", "Aspirin", "Clopidogrel", "Atorvastatin", "Simvastatin", "Rosuvastatin",
    "Lisinopril", "Losartan", "Metoprolol", "Carvedilol", "Amlodipine", "Diltiazem",
    "Furosemide", "Hydrochlorothiazide", "Spironolactone", "Digoxin", "Amiodarone",
    
    # Antibiotics
    "Amoxicillin", "Azithromycin", "Ciprofloxacin", "Levofloxacin", "Doxycycline",
    "Cephalexin", "Metronidazole", "Clarithromycin", "Trimethoprim-Sulfamethoxazole",
    
    # Antidepressants/Psychiatric
    "Sertraline", "Fluoxetine", "Escitalopram", "Venlafaxine", "Duloxetine",
    "Bupropion", "Mirtazapine", "Trazodone", "Alprazolam", "Lorazepam", "Clonazepam",
    "Quetiapine", "Aripiprazole", "Risperidone", "Lithium", "Valproic Acid",
    
    # Pain Management
    "Ibuprofen", "Naproxen", "Celecoxib", "Tramadol", "Oxycodone", "Hydrocodone",
    "Morphine", "Fentanyl", "Gabapentin", "Pregabalin", "Acetaminophen",
    
    # Diabetes
    "Metformin", "Glipizide", "Glyburide", "Insulin Glargine", "Insulin Lispro",
    "Sitagliptin", "Empagliflozin", "Dulaglutide", "Semaglutide",
    
    # Anticoagulants/Antiplatelets
    "Apixaban", "Rivaroxaban", "Dabigatran", "Enoxaparin", "Heparin",
    
    # Proton Pump Inhibitors
    "Omeprazole", "Pantoprazole", "Esomeprazole", "Lansoprazole",
    
    # Thyroid
    "Levothyroxine", "Methimazole", "Propylthiouracil",
    
    # Respiratory
    "Albuterol", "Montelukast", "Fluticasone", "Budesonide", "Prednisone",
    
    # Immunosuppressants
    "Tacrolimus", "Cyclosporine", "Mycophenolate", "Azathioprine",
    
    # Antivirals
    "Acyclovir", "Valacyclovir", "Oseltamivir", "Remdesivir",
    
    # Antifungals
    "Fluconazole", "Itraconazole", "Voriconazole", "Ketoconazole",
    
    # MAO Inhibitors
    "Phenelzine", "Tranylcypromine", "Selegiline", "Rasagiline",
    
    # Others
    "Methotrexate", "Allopurinol", "Colchicine", "Sildenafil", "Tadalafil",
    "Finasteride", "Tamsulosin", "Oxybutynin", "Donepezil", "Memantine"
]

# Interaction severity levels
SEVERITY_LEVELS = [
    ("Contraindicated", 0.05),  # 5%
    ("Major", 0.20),             # 20%
    ("Moderate", 0.45),          # 45%
    ("Minor", 0.30)              # 30%
]

# Interaction mechanisms
MECHANISMS = [
    "CYP450 Enzyme Inhibition",
    "CYP450 Enzyme Induction",
    "P-glycoprotein Interaction",
    "Protein Binding Displacement",
    "Renal Excretion Competition",
    "Additive CNS Depression",
    "Additive QT Prolongation",
    "Additive Bleeding Risk",
    "Serotonin Syndrome Risk",
    "Hypertensive Crisis Risk",
    "Additive Nephrotoxicity",
    "Additive Hepatotoxicity",
    "Electrolyte Imbalance",
    "Pharmacodynamic Antagonism",
    "Pharmacodynamic Synergy",
    "Absorption Interference",
    "Metabolic Competition",
    "Receptor Competition",
    "Additive Anticholinergic Effects",
    "Additive Hypotension"
]

# Clinical effects
CLINICAL_EFFECTS = [
    "Increased bleeding risk",
    "Decreased therapeutic effect",
    "Increased drug levels/toxicity",
    "Decreased drug levels/efficacy",
    "QT interval prolongation",
    "Serotonin syndrome",
    "CNS depression/sedation",
    "Hypertensive crisis",
    "Hypotension",
    "Hyperkalemia",
    "Hypokalemia",
    "Renal impairment",
    "Hepatotoxicity",
    "Rhabdomyolysis",
    "Seizure risk",
    "Respiratory depression",
    "Cardiac arrhythmia",
    "Hypoglycemia",
    "Hyperglycemia",
    "Gastrointestinal bleeding",
    "Acute kidney injury",
    "Liver enzyme elevation",
    "Confusion/delirium",
    "Orthostatic hypotension",
    "Bradycardia",
    "Tachycardia"
]

# Management recommendations
MANAGEMENT_RECOMMENDATIONS = [
    "Avoid combination - use alternative therapy",
    "Monitor closely - adjust dose as needed",
    "Separate administration by 2-4 hours",
    "Monitor drug levels and adjust dose",
    "Monitor for signs of toxicity",
    "Monitor blood pressure regularly",
    "Monitor renal function",
    "Monitor liver function tests",
    "Monitor electrolytes",
    "Monitor INR/PT if on anticoagulants",
    "Monitor for bleeding signs",
    "Monitor for CNS effects",
    "Consider dose reduction of 25-50%",
    "Use lowest effective dose",
    "Monitor ECG for QT prolongation",
    "Educate patient on warning signs",
    "Consider therapeutic drug monitoring",
    "Consult specialist before combining",
    "Use with extreme caution",
    "Monitor glucose levels closely"
]

# Evidence levels
EVIDENCE_LEVELS = [
    ("Established", 0.30),
    ("Probable", 0.35),
    ("Theoretical", 0.25),
    ("Case Reports", 0.10)
]

# Onset timing
ONSET_TIMING = ["Rapid (hours)", "Delayed (days)", "Prolonged (weeks)", "Variable"]

# Documentation sources
DOCUMENTATION_SOURCES = [
    "FDA Drug Label",
    "Clinical Trial Data",
    "Case Reports",
    "Pharmacokinetic Studies",
    "Post-Marketing Surveillance",
    "Expert Consensus",
    "Systematic Review",
    "Meta-Analysis"
]


def weighted_choice(choices):
    """Select item based on weighted probabilities."""
    items, weights = zip(*choices)
    return random.choices(items, weights=weights, k=1)[0]


def generate_interaction_id():
    """Generate unique interaction ID."""
    return f"DI-{uuid.uuid4().hex[:12].upper()}"


def generate_drug_pair():
    """Generate a pair of interacting drugs."""
    drug1, drug2 = random.sample(DRUGS, 2)
    # Ensure consistent ordering for the same pair
    return tuple(sorted([drug1, drug2]))


def generate_interaction_record(record_id):
    """Generate a single drug interaction record."""
    drug1, drug2 = generate_drug_pair()
    severity = weighted_choice(SEVERITY_LEVELS)
    evidence = weighted_choice(EVIDENCE_LEVELS)
    
    # Generate multiple mechanisms, effects, and recommendations for realism
    num_mechanisms = random.randint(1, 3)
    mechanisms = ", ".join(random.sample(MECHANISMS, num_mechanisms))
    
    num_effects = random.randint(1, 4)
    effects = "; ".join(random.sample(CLINICAL_EFFECTS, num_effects))
    
    num_recommendations = random.randint(1, 3)
    recommendations = "; ".join(random.sample(MANAGEMENT_RECOMMENDATIONS, num_recommendations))
    
    num_sources = random.randint(1, 3)
    sources = ", ".join(random.sample(DOCUMENTATION_SOURCES, num_sources))
    
    # Generate dates
    last_updated = datetime.now() - timedelta(days=random.randint(0, 730))
    last_reviewed = last_updated - timedelta(days=random.randint(0, 180))
    
    return {
        "interaction_id": generate_interaction_id(),
        "drug_1_name": drug1,
        "drug_1_ndc": f"{random.randint(10000, 99999)}-{random.randint(100, 999)}-{random.randint(10, 99)}",
        "drug_2_name": drug2,
        "drug_2_ndc": f"{random.randint(10000, 99999)}-{random.randint(100, 999)}-{random.randint(10, 99)}",
        "severity_level": severity,
        "interaction_mechanism": mechanisms,
        "clinical_effects": effects,
        "management_recommendation": recommendations,
        "evidence_level": evidence,
        "onset_timing": random.choice(ONSET_TIMING),
        "documentation_source": sources,
        "requires_alert": "Yes" if severity in ["Contraindicated", "Major"] else "No",
        "requires_intervention": "Yes" if severity == "Contraindicated" else random.choice(["Yes", "No"]),
        "patient_counseling_required": random.choice(["Yes", "No"]),
        "prescriber_notification_required": "Yes" if severity in ["Contraindicated", "Major"] else random.choice(["Yes", "No"]),
        "last_reviewed_date": last_reviewed.strftime("%Y-%m-%d"),
        "last_updated_date": last_updated.strftime("%Y-%m-%d"),
        "active_status": random.choice(["Active", "Active", "Active", "Under Review"]),
        "reference_id": f"REF-{random.randint(100000, 999999)}",
        "notes": f"Interaction between {drug1} and {drug2}. {severity} severity. Monitor patient closely."
    }


def estimate_file_size(records, fieldnames):
    """Estimate CSV file size in bytes."""
    # Estimate average row size
    sample_row = records[0] if records else {}
    row_size = sum(len(str(sample_row.get(field, ""))) for field in fieldnames) + len(fieldnames) * 2  # +2 for commas and quotes
    header_size = sum(len(field) for field in fieldnames) + len(fieldnames) * 2
    return header_size + (row_size * len(records))


def generate_drug_interactions(total_records=20000, target_file_size_mb=30):
    """Generate drug interaction records and save to CSV files."""
    
    output_dir = Path(__file__).parent.parent.parent / "src" / "main" / "resources" / "data"
    output_dir.mkdir(parents=True, exist_ok=True)
    
    print(f"Generating {total_records:,} drug interaction records...")
    print(f"Target file size: ~{target_file_size_mb} MB per file")
    
    # Generate all records
    all_records = []
    seen_pairs = set()
    
    while len(all_records) < total_records:
        record = generate_interaction_record(len(all_records) + 1)
        drug_pair = (record["drug_1_name"], record["drug_2_name"])
        
        # Allow some duplicates but not too many (realistic scenario)
        if drug_pair not in seen_pairs or random.random() < 0.1:
            all_records.append(record)
            seen_pairs.add(drug_pair)
    
    fieldnames = list(all_records[0].keys())
    
    # Calculate records per file to achieve target size
    target_size_bytes = target_file_size_mb * 1024 * 1024
    sample_size = estimate_file_size(all_records[:100], fieldnames)
    records_per_file = int((target_size_bytes / sample_size) * 100)
    
    print(f"Estimated records per file: ~{records_per_file:,}")
    
    # Split into files
    file_count = 0
    for i in range(0, len(all_records), records_per_file):
        file_count += 1
        chunk = all_records[i:i + records_per_file]
        
        filename = output_dir / f"us_pharmacy_drug_interactions_{file_count:02d}.csv"
        
        with open(filename, 'w', newline='', encoding='utf-8') as csvfile:
            writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
            writer.writeheader()
            writer.writerows(chunk)
        
        file_size_mb = filename.stat().st_size / (1024 * 1024)
        print(f"Created {filename.name}: {len(chunk):,} records, {file_size_mb:.2f} MB")
    
    print(f"\n✓ Successfully generated {len(all_records):,} drug interaction records")
    print(f"✓ Created {file_count} CSV files in {output_dir}")
    print(f"✓ Total size: {sum(f.stat().st_size for f in output_dir.glob('us_pharmacy_drug_interactions_*.csv')) / (1024 * 1024):.2f} MB")


if __name__ == "__main__":
    generate_drug_interactions(total_records=20000, target_file_size_mb=30)


