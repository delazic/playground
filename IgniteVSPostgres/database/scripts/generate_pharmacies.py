#!/usr/bin/env python3
"""
Generate synthetic pharmacy data for US pharmacies.
Creates 50,000 unique pharmacy records with realistic data.
"""

import csv
import random
from datetime import datetime

# Pharmacy chains and their typical store counts
PHARMACY_CHAINS = {
    'CVS': 9600,
    'WALGREENS': 8900,
    'WALMART': 4700,
    'RITE AID': 2400,
    'KROGER': 2200,
    'ALBERTSONS': 1700,
    'PUBLIX': 1200,
    'COSTCO': 600,
    'SAM\'S CLUB': 600,
    'TARGET': 1800,
    'INDEPENDENT': 20000,  # Independent pharmacies
    'REGIONAL CHAIN': 6300  # Various regional chains
}

# Pharmacy types distribution
PHARMACY_TYPES = {
    'RETAIL': 0.70,
    'MAIL_ORDER': 0.05,
    'SPECIALTY': 0.10,
    'LONG_TERM_CARE': 0.15
}

# US States with population-based distribution
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

# Common street names
STREET_NAMES = [
    'Main', 'Oak', 'Maple', 'Cedar', 'Elm', 'Washington', 'Lake', 'Hill',
    'Park', 'Pine', 'First', 'Second', 'Third', 'Broadway', 'Market',
    'Church', 'Spring', 'Center', 'River', 'Sunset', 'Madison', 'Lincoln',
    'Jefferson', 'Franklin', 'Jackson', 'Wilson', 'Highland', 'Forest',
    'Valley', 'Ridge', 'Summit', 'College', 'University', 'Commerce'
]

STREET_TYPES = ['St', 'Ave', 'Blvd', 'Dr', 'Rd', 'Ln', 'Way', 'Pkwy', 'Ct']

# Common city name components
CITY_PREFIXES = [
    'Spring', 'Green', 'Fair', 'Clear', 'River', 'Lake', 'Mount', 'New',
    'West', 'East', 'North', 'South', 'Port', 'Fort', 'Saint'
]

CITY_SUFFIXES = [
    'field', 'ville', 'town', 'dale', 'wood', 'view', 'port', 'land',
    'brook', 'side', 'ford', 'burg', 'ton', 'city', 'haven'
]

# Major US cities for realistic distribution
MAJOR_CITIES = {
    'CA': ['Los Angeles', 'San Diego', 'San Jose', 'San Francisco', 'Fresno', 'Sacramento'],
    'TX': ['Houston', 'San Antonio', 'Dallas', 'Austin', 'Fort Worth', 'El Paso'],
    'FL': ['Jacksonville', 'Miami', 'Tampa', 'Orlando', 'St Petersburg'],
    'NY': ['New York', 'Buffalo', 'Rochester', 'Yonkers', 'Syracuse'],
    'PA': ['Philadelphia', 'Pittsburgh', 'Allentown', 'Erie', 'Reading'],
    'IL': ['Chicago', 'Aurora', 'Naperville', 'Joliet', 'Rockford'],
    'OH': ['Columbus', 'Cleveland', 'Cincinnati', 'Toledo', 'Akron'],
    'GA': ['Atlanta', 'Augusta', 'Columbus', 'Savannah', 'Athens'],
    'NC': ['Charlotte', 'Raleigh', 'Greensboro', 'Durham', 'Winston-Salem'],
    'MI': ['Detroit', 'Grand Rapids', 'Warren', 'Sterling Heights', 'Ann Arbor']
}


def generate_ncpdp_id():
    """Generate a unique 7-digit NCPDP ID."""
    return f"{random.randint(1000000, 9999999)}"


def generate_npi():
    """Generate a unique 10-digit NPI."""
    return f"{random.randint(1000000000, 9999999999)}"


def generate_address():
    """Generate a realistic US address."""
    number = random.randint(1, 9999)
    street = random.choice(STREET_NAMES)
    street_type = random.choice(STREET_TYPES)
    
    # Sometimes add suite/unit number
    if random.random() < 0.2:
        suite = f", Suite {random.randint(1, 500)}"
    else:
        suite = ""
    
    return f"{number} {street} {street_type}{suite}"


def generate_city(state):
    """Generate a city name, preferring major cities for the state."""
    if state in MAJOR_CITIES and random.random() < 0.6:
        return random.choice(MAJOR_CITIES[state])
    else:
        # Generate synthetic city name
        if random.random() < 0.5:
            return f"{random.choice(CITY_PREFIXES)}{random.choice(CITY_SUFFIXES)}"
        else:
            return random.choice(CITY_PREFIXES) + " " + random.choice(CITY_SUFFIXES).capitalize()


def generate_zip_code(state):
    """Generate a realistic ZIP code based on state."""
    # Simplified ZIP code ranges by state (first digit)
    state_zip_prefixes = {
        'CA': ['9'], 'TX': ['7', '8'], 'FL': ['3'], 'NY': ['1'],
        'PA': ['1'], 'IL': ['6'], 'OH': ['4'], 'GA': ['3'],
        'NC': ['2'], 'MI': ['4']
    }
    
    prefix = random.choice(state_zip_prefixes.get(state, ['0', '1', '2', '3', '4', '5', '6', '7', '8', '9']))
    return f"{prefix}{random.randint(0, 9)}{random.randint(0, 9)}{random.randint(0, 9)}{random.randint(0, 9)}"


def generate_phone():
    """Generate a US phone number."""
    area_code = random.randint(200, 999)
    exchange = random.randint(200, 999)
    number = random.randint(1000, 9999)
    return f"({area_code}) {exchange}-{number}"


def select_state():
    """Select a state based on population distribution."""
    rand = random.random() * 100
    cumulative = 0
    for state, percentage in US_STATES.items():
        cumulative += percentage
        if rand <= cumulative:
            return state
    return 'CA'  # Default fallback


def select_pharmacy_type():
    """Select pharmacy type based on distribution."""
    rand = random.random()
    cumulative = 0
    for ptype, percentage in PHARMACY_TYPES.items():
        cumulative += percentage
        if rand <= cumulative:
            return ptype
    return 'RETAIL'  # Default fallback


def generate_pharmacy_name(chain_name, city, state, store_number):
    """Generate pharmacy name based on chain and location."""
    if chain_name == 'INDEPENDENT':
        # Generate independent pharmacy names
        prefixes = ['Family', 'Community', 'Main Street', 'Corner', 'Neighborhood', 
                   'Local', 'Town', 'Village', 'City', 'Professional']
        suffixes = ['Pharmacy', 'Drug Store', 'Apothecary', 'Prescriptions', 'Drugs']
        
        if random.random() < 0.3:
            # Use city name
            return f"{city} {random.choice(suffixes)}"
        else:
            return f"{random.choice(prefixes)} {random.choice(suffixes)}"
    
    elif chain_name == 'REGIONAL CHAIN':
        # Generate regional chain names
        regional_chains = [
            'Hy-Vee', 'Meijer', 'H-E-B', 'Giant Eagle', 'Wegmans',
            'ShopRite', 'Stop & Shop', 'Food Lion', 'Harris Teeter',
            'Safeway', 'Vons', 'Jewel-Osco', 'Acme', 'Randalls'
        ]
        return f"{random.choice(regional_chains)} Pharmacy #{store_number}"
    
    else:
        # Chain pharmacy
        return f"{chain_name} Pharmacy #{store_number}"


def generate_pharmacies(count=50000):
    """Generate pharmacy records."""
    print(f"Generating {count:,} pharmacy records...")
    
    pharmacies = []
    used_ncpdp_ids = set()
    used_npis = set()
    
    # Calculate how many pharmacies per chain
    total_weight = sum(PHARMACY_CHAINS.values())
    chain_counts = {
        chain: int((weight / total_weight) * count)
        for chain, weight in PHARMACY_CHAINS.items()
    }
    
    # Adjust to ensure we hit exactly the target count
    current_total = sum(chain_counts.values())
    if current_total < count:
        chain_counts['INDEPENDENT'] += (count - current_total)
    
    pharmacy_id = 1
    
    for chain_name, chain_count in chain_counts.items():
        print(f"  Generating {chain_count:,} {chain_name} pharmacies...")
        
        for i in range(chain_count):
            # Generate unique IDs
            while True:
                ncpdp_id = generate_ncpdp_id()
                if ncpdp_id not in used_ncpdp_ids:
                    used_ncpdp_ids.add(ncpdp_id)
                    break
            
            while True:
                npi = generate_npi()
                if npi not in used_npis:
                    used_npis.add(npi)
                    break
            
            # Select location
            state = select_state()
            city = generate_city(state)
            address = generate_address()
            zip_code = generate_zip_code(state)
            phone = generate_phone()
            
            # Generate pharmacy name
            store_number = i + 1
            pharmacy_name = generate_pharmacy_name(chain_name, city, state, store_number)
            
            # Select pharmacy type
            pharmacy_type = select_pharmacy_type()
            
            # Most pharmacies are active
            is_active = random.random() < 0.95
            
            pharmacy = {
                'ncpdp_id': ncpdp_id,
                'pharmacy_name': pharmacy_name,
                'npi': npi,
                'address': address,
                'city': city,
                'state': state,
                'zip_code': zip_code,
                'phone': phone,
                'pharmacy_type': pharmacy_type,
                'is_active': str(is_active).lower()
            }
            
            pharmacies.append(pharmacy)
            pharmacy_id += 1
            
            # Progress indicator
            if pharmacy_id % 5000 == 0:
                print(f"    Generated {pharmacy_id:,} pharmacies...")
    
    return pharmacies


def write_csv(pharmacies, filename):
    """Write pharmacies to CSV file."""
    print(f"\nWriting {len(pharmacies):,} pharmacies to {filename}...")
    
    fieldnames = [
        'ncpdp_id', 'pharmacy_name', 'npi', 'address', 'city',
        'state', 'zip_code', 'phone', 'pharmacy_type', 'is_active'
    ]
    
    with open(filename, 'w', newline='', encoding='utf-8') as csvfile:
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(pharmacies)
    
    print(f"✓ Successfully wrote {len(pharmacies):,} pharmacies to {filename}")


def generate_statistics(pharmacies):
    """Generate statistics about the pharmacy data."""
    total = len(pharmacies)
    
    # Count by type
    type_counts = {}
    for p in pharmacies:
        ptype = p['pharmacy_type']
        type_counts[ptype] = type_counts.get(ptype, 0) + 1
    
    # Count by state (top 10)
    state_counts = {}
    for p in pharmacies:
        state = p['state']
        state_counts[state] = state_counts.get(state, 0) + 1
    
    top_states = sorted(state_counts.items(), key=lambda x: x[1], reverse=True)[:10]
    
    # Count active vs inactive
    active_count = sum(1 for p in pharmacies if p['is_active'] == 'true')
    inactive_count = total - active_count
    
    # Count by chain (from pharmacy name)
    chain_counts = {}
    for p in pharmacies:
        name = p['pharmacy_name']
        if 'CVS' in name:
            chain = 'CVS'
        elif 'WALGREENS' in name:
            chain = 'WALGREENS'
        elif 'WALMART' in name:
            chain = 'WALMART'
        elif 'RITE AID' in name:
            chain = 'RITE AID'
        elif any(x in name for x in ['Hy-Vee', 'Meijer', 'H-E-B', 'Giant Eagle', 'Wegmans']):
            chain = 'REGIONAL CHAIN'
        else:
            chain = 'INDEPENDENT/OTHER'
        
        chain_counts[chain] = chain_counts.get(chain, 0) + 1
    
    return {
        'total': total,
        'type_counts': type_counts,
        'top_states': top_states,
        'active_count': active_count,
        'inactive_count': inactive_count,
        'chain_counts': chain_counts
    }


def main():
    """Main function to generate pharmacy data."""
    print("=" * 70)
    print("US Pharmacy Data Generator")
    print("=" * 70)
    print()
    
    # Generate pharmacies
    pharmacies = generate_pharmacies(50000)
    
    # Write to CSV
    output_file = 'src/main/resources/data/us_pharmacy_pharmacies.csv'
    write_csv(pharmacies, output_file)
    
    # Generate statistics
    stats = generate_statistics(pharmacies)
    
    print("\n" + "=" * 70)
    print("Generation Statistics")
    print("=" * 70)
    print(f"Total Pharmacies: {stats['total']:,}")
    print(f"Active: {stats['active_count']:,} ({stats['active_count']/stats['total']*100:.1f}%)")
    print(f"Inactive: {stats['inactive_count']:,} ({stats['inactive_count']/stats['total']*100:.1f}%)")
    print()
    
    print("By Pharmacy Type:")
    for ptype, count in sorted(stats['type_counts'].items()):
        print(f"  {ptype:20s}: {count:6,} ({count/stats['total']*100:5.1f}%)")
    print()
    
    print("Top 10 States:")
    for state, count in stats['top_states']:
        print(f"  {state}: {count:6,} ({count/stats['total']*100:5.1f}%)")
    print()
    
    print("By Chain Type:")
    for chain, count in sorted(stats['chain_counts'].items(), key=lambda x: x[1], reverse=True):
        print(f"  {chain:20s}: {count:6,} ({count/stats['total']*100:5.1f}%)")
    
    print("\n" + "=" * 70)
    print("✓ Pharmacy data generation complete!")
    print("=" * 70)


if __name__ == '__main__':
    main()
