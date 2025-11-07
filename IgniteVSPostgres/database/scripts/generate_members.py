#!/usr/bin/env python3
"""
Generate 1,000,000 representative US healthcare members
Output: us_pharmacy_members.csv
"""

import csv
import random
from datetime import datetime, timedelta
from faker import Faker
import sys

# Initialize Faker with US locale
fake = Faker('en_US')
Faker.seed(42)  # For reproducibility
random.seed(42)

# US State distribution (approximate population percentages)
US_STATES = {
    'CA': 12.0, 'TX': 9.0, 'FL': 6.5, 'NY': 6.0, 'PA': 4.0,
    'IL': 3.9, 'OH': 3.6, 'GA': 3.3, 'NC': 3.2, 'MI': 3.1,
    'NJ': 2.8, 'VA': 2.6, 'WA': 2.3, 'AZ': 2.2, 'MA': 2.1,
    'TN': 2.1, 'IN': 2.0, 'MO': 1.9, 'MD': 1.9, 'WI': 1.8,
    'CO': 1.7, 'MN': 1.7, 'SC': 1.6, 'AL': 1.5, 'LA': 1.4,
    'KY': 1.4, 'OR': 1.3, 'OK': 1.2, 'CT': 1.1, 'UT': 1.0,
    'IA': 1.0, 'NV': 0.9, 'AR': 0.9, 'MS': 0.9, 'KS': 0.9,
    'NM': 0.6, 'NE': 0.6, 'WV': 0.6, 'ID': 0.5, 'HI': 0.4,
    'NH': 0.4, 'ME': 0.4, 'MT': 0.3, 'RI': 0.3, 'DE': 0.3,
    'SD': 0.3, 'ND': 0.2, 'AK': 0.2, 'VT': 0.2, 'WY': 0.2
}

# Gender distribution (approximate US population)
GENDER_DIST = {'F': 50.5, 'M': 49.3, 'U': 0.2}

# Age distribution (representative of US population)
def generate_age_distribution():
    """Generate age based on US population distribution"""
    rand = random.random() * 100
    if rand < 6.0:  # 0-4 years
        return random.randint(0, 4)
    elif rand < 12.5:  # 5-14 years
        return random.randint(5, 14)
    elif rand < 25.5:  # 15-24 years
        return random.randint(15, 24)
    elif rand < 38.5:  # 25-34 years
        return random.randint(25, 34)
    elif rand < 51.5:  # 35-44 years
        return random.randint(35, 44)
    elif rand < 64.5:  # 45-54 years
        return random.randint(45, 54)
    elif rand < 77.5:  # 55-64 years
        return random.randint(55, 64)
    elif rand < 90.5:  # 65-74 years
        return random.randint(65, 74)
    else:  # 75+ years
        return random.randint(75, 95)

def weighted_choice(choices_dict):
    """Select item based on weighted probabilities"""
    total = sum(choices_dict.values())
    rand = random.uniform(0, total)
    cumulative = 0
    for choice, weight in choices_dict.items():
        cumulative += weight
        if rand <= cumulative:
            return choice
    return list(choices_dict.keys())[-1]

def generate_member_number(index):
    """Generate unique member number"""
    return f"MBR{index:09d}"

def generate_phone():
    """Generate US phone number"""
    area_code = random.randint(200, 999)
    exchange = random.randint(200, 999)
    number = random.randint(1000, 9999)
    return f"{area_code}-{exchange}-{number}"

def generate_member(index):
    """Generate a single member record"""
    # Generate age and date of birth
    age = generate_age_distribution()
    today = datetime.now()
    dob = today - timedelta(days=age*365 + random.randint(0, 364))
    
    # Generate gender
    gender = weighted_choice(GENDER_DIST)
    
    # Generate name based on gender
    if gender == 'M':
        first_name = fake.first_name_male()
    elif gender == 'F':
        first_name = fake.first_name_female()
    else:
        first_name = fake.first_name()
    
    last_name = fake.last_name()
    
    # Generate location
    state = weighted_choice(US_STATES)
    
    city = fake.city()
    address = fake.street_address()
    zip_code = fake.zipcode()
    
    # Generate contact info
    phone = generate_phone()
    email = f"{first_name.lower()}.{last_name.lower()}{random.randint(1, 999)}@{fake.free_email_domain()}"
    
    # Member number
    member_number = generate_member_number(index)
    
    return {
        'member_number': member_number,
        'first_name': first_name,
        'last_name': last_name,
        'date_of_birth': dob.strftime('%Y-%m-%d'),
        'gender': gender,
        'address': address.replace(',', ' '),  # Remove commas for CSV
        'city': city.replace(',', ' '),
        'state': state,
        'zip_code': zip_code,
        'phone': phone,
        'email': email
    }

def main():
    total_members = 1_000_000
    num_files = 10
    members_per_file = total_members // num_files
    output_dir = '../data'
    
    print(f"Generating {total_members:,} US healthcare members...")
    print(f"Splitting into {num_files} files ({members_per_file:,} members each)")
    print(f"Output directory: {output_dir}")
    
    # CSV headers matching the member table schema
    fieldnames = [
        'member_number', 'first_name', 'last_name', 'date_of_birth',
        'gender', 'address', 'city', 'state', 'zip_code', 'phone', 'email'
    ]
    
    try:
        member_index = 1
        
        for file_num in range(1, num_files + 1):
            output_file = f'{output_dir}/us_pharmacy_members_{file_num:02d}.csv'
            print(f"\n📝 Creating file {file_num}/{num_files}: {output_file}")
            
            with open(output_file, 'w', newline='', encoding='utf-8') as csvfile:
                writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
                writer.writeheader()
                
                # Generate members for this file
                batch_size = 10_000
                for i in range(members_per_file):
                    member = generate_member(member_index)
                    writer.writerow(member)
                    member_index += 1
                    
                    # Progress reporting
                    if member_index % batch_size == 0:
                        progress = (member_index / total_members) * 100
                        print(f"   Progress: {member_index:,} / {total_members:,} ({progress:.1f}%)")
                        sys.stdout.flush()
            
            print(f"   ✅ File {file_num} complete: {members_per_file:,} members")
        
        print(f"\n✅ Successfully generated {total_members:,} members in {num_files} files!")
        
        # Print statistics
        print("\n📊 Generation Statistics:")
        print(f"   - Total records: {total_members:,}")
        print(f"   - Files created: {num_files}")
        print(f"   - Records per file: {members_per_file:,}")
        print(f"   - Age range: 0-95 years")
        print(f"   - Gender distribution: ~50% F, ~49% M, ~1% U")
        print(f"   - States covered: All 50 US states")
        
        # List created files
        print("\n📁 Created Files:")
        for file_num in range(1, num_files + 1):
            filename = f'us_pharmacy_members_{file_num:02d}.csv'
            print(f"   - {filename}")
        
    except Exception as e:
        print(f"❌ Error generating members: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()


