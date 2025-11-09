#!/usr/bin/env python3
"""
Fix pharmacy network CSV data issues:
1. Fix invalid date ranges (termination_date < effective_date)
2. Fix invalid pharmacy IDs (PHARM00050001+) by wrapping to valid range
"""

import csv
import os
import glob
from datetime import datetime

def fix_pharmacy_id(pharmacy_id):
    """
    Fix pharmacy IDs that are out of range (> PHARM00050000)
    Wrap them back to valid range using modulo
    """
    if not pharmacy_id.startswith('PHARM'):
        return pharmacy_id
    
    try:
        id_num = int(pharmacy_id[5:])  # Extract number after 'PHARM'
        if id_num > 50000:
            # Wrap to valid range: 1-50000
            new_id_num = ((id_num - 1) % 50000) + 1
            return f"PHARM{new_id_num:08d}"
        return pharmacy_id
    except (ValueError, IndexError):
        return pharmacy_id

def fix_date_range(effective_date, termination_date):
    """
    Fix invalid date ranges where termination < effective
    Set termination to empty string if invalid
    """
    if not effective_date or not termination_date:
        return effective_date, termination_date
    
    try:
        eff_dt = datetime.strptime(effective_date, '%Y-%m-%d')
        term_dt = datetime.strptime(termination_date, '%Y-%m-%d')
        
        if term_dt < eff_dt:
            # Invalid range - clear termination date
            return effective_date, ''
        return effective_date, termination_date
    except ValueError:
        # Invalid date format - keep as is
        return effective_date, termination_date

def process_file(input_file):
    """Process a single CSV file and fix data issues"""
    print(f"Processing {os.path.basename(input_file)}...")
    
    rows = []
    fixed_pharmacy_ids = 0
    fixed_date_ranges = 0
    
    # Read the file
    with open(input_file, 'r', encoding='utf-8') as f:
        reader = csv.reader(f)
        header = next(reader)
        rows.append(header)
        
        for row in reader:
            if len(row) < 16:
                rows.append(row)
                continue
            
            # Fix pharmacy ID (field index 1)
            original_pharmacy_id = row[1]
            fixed_pharmacy_id = fix_pharmacy_id(row[1])
            if original_pharmacy_id != fixed_pharmacy_id:
                row[1] = fixed_pharmacy_id
                fixed_pharmacy_ids += 1
            
            # Fix date range (field indices 6 and 7)
            original_effective = row[6]
            original_termination = row[7]
            fixed_effective, fixed_termination = fix_date_range(row[6], row[7])
            
            if original_termination != fixed_termination:
                row[6] = fixed_effective
                row[7] = fixed_termination
                fixed_date_ranges += 1
            
            rows.append(row)
    
    # Write back to file
    with open(input_file, 'w', encoding='utf-8', newline='') as f:
        writer = csv.writer(f)
        writer.writerows(rows)
    
    print(f"  Fixed {fixed_pharmacy_ids} pharmacy IDs")
    print(f"  Fixed {fixed_date_ranges} date ranges")
    return fixed_pharmacy_ids, fixed_date_ranges

def main():
    """Main function to process all pharmacy network CSV files"""
    # Get the data directory
    script_dir = os.path.dirname(os.path.abspath(__file__))
    data_dir = os.path.join(script_dir, '..', '..', 'src', 'main', 'resources', 'data')
    
    # Find all pharmacy network CSV files
    pattern = os.path.join(data_dir, 'us_pharmacy_pharmacy_networks_*.csv')
    files = sorted(glob.glob(pattern))
    
    if not files:
        print(f"No files found matching pattern: {pattern}")
        return
    
    print(f"Found {len(files)} pharmacy network files to process\n")
    
    total_pharmacy_fixes = 0
    total_date_fixes = 0
    
    for file in files:
        pharmacy_fixes, date_fixes = process_file(file)
        total_pharmacy_fixes += pharmacy_fixes
        total_date_fixes += date_fixes
    
    print(f"\n{'='*60}")
    print(f"SUMMARY:")
    print(f"  Total files processed: {len(files)}")
    print(f"  Total pharmacy IDs fixed: {total_pharmacy_fixes}")
    print(f"  Total date ranges fixed: {total_date_fixes}")
    print(f"{'='*60}")

if __name__ == '__main__':
    main()

# Made with Bob
