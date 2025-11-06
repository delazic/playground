# Benefit Plan Model Documentation

## Overview

The BenefitPlan model represents US pharmacy benefit plans with complete cost-sharing structures. It includes a POJO class and a CSV converter for loading plan data from resources.

## Package Structure

```
dejanlazic.playground.inmemory.rdbms/
├── model/
│   └── BenefitPlan.java           - POJO representing a pharmacy benefit plan
├── converter/
│   └── BenefitPlanConverter.java  - CSV parser and plan loader
├── DatabaseConnector.java          - Pure JDBC database connector
└── App.java                        - Database connectivity application
```

## Files

### 1. BenefitPlan.java (POJO)

**Package:** `dejanlazic.playground.inmemory.rdbms.model`

**Purpose:** Plain Old Java Object representing a pharmacy benefit plan with all cost-sharing details.

**Key Features:**
- 20 fields mapping to CSV columns
- Utility methods for tier-based lookups
- Support for both copay and coinsurance structures
- Proper equals/hashCode based on planCode
- Comprehensive toString() for debugging

**Fields:**

| Field | Type | Description |
|-------|------|-------------|
| planCode | String | Unique plan identifier (e.g., COMM_GOLD_001) |
| planName | String | Human-readable plan name |
| planType | String | Plan type (COMMERCIAL, MEDICARE, MEDICAID, etc.) |
| planCategory | String | Plan category (GOLD, SILVER, BRONZE, etc.) |
| effectiveDate | LocalDate | Plan effective date |
| annualDeductible | BigDecimal | Annual deductible amount |
| outOfPocketMax | BigDecimal | Annual out-of-pocket maximum |
| tier1Copay - tier5Copay | BigDecimal | Fixed copay amounts per tier |
| tier1Coinsurance - tier5Coinsurance | BigDecimal | Coinsurance percentages (0.20 = 20%) |
| mailOrderAvailable | boolean | Whether mail order is available |
| specialtyPharmacyRequired | boolean | Whether specialty pharmacy is required |
| description | String | Plan description |

**Utility Methods:**

```java
// Get copay for specific tier (1-5)
BigDecimal getCopayForTier(int tier)

// Get coinsurance for specific tier (1-5)
BigDecimal getCoinsuranceForTier(int tier)

// Check if plan uses copay for tier
boolean usesCopayForTier(int tier)

// Check if plan uses coinsurance for tier
boolean usesCoinsuranceForTier(int tier)
```

### 2. BenefitPlanConverter.java

**Package:** `dejanlazic.playground.inmemory.rdbms.converter`

**Purpose:** Reads and parses benefit plans from CSV file in classpath resources.

**Key Features:**
- Reads from `us_pharmacy_plans.csv` in resources folder
- Parses all 20 CSV columns into BenefitPlan objects
- Provides search methods (by code, type, category)
- Handles date parsing (yyyy-MM-dd format)
- Handles boolean parsing (TRUE/FALSE, 1/0, yes/no)
- Pretty-prints plan summaries

**Methods:**

```java
// Load all plans from CSV
List<BenefitPlan> loadAllPlans() throws IOException

// Parse single CSV line
BenefitPlan parseLine(String line)

// Find plan by code
BenefitPlan findByPlanCode(String planCode) throws IOException

// Find plans by type (COMMERCIAL, MEDICARE, etc.)
List<BenefitPlan> findByPlanType(String planType) throws IOException

// Find plans by category (GOLD, SILVER, etc.)
List<BenefitPlan> findByPlanCategory(String planCategory) throws IOException

// Get total plan count
int getPlanCount() throws IOException

// Print formatted plan summary
void printPlanSummary(BenefitPlan plan)
```

### 3. us_pharmacy_plans.csv

**Location:** `src/main/resources/us_pharmacy_plans.csv`

**Purpose:** Contains 30 representative US pharmacy benefit plans.

**Why in resources folder:**
- ✅ Packaged with application JAR
- ✅ Easy to read using ClassLoader
- ✅ Works in both development and Docker environments
- ✅ No file path dependencies
- ✅ Follows Java best practices

## Usage Examples

### Load All Plans

```java
import dejanlazic.playground.inmemory.rdbms.converter.BenefitPlanConverter;
import dejanlazic.playground.inmemory.rdbms.model.BenefitPlan;

BenefitPlanConverter converter = new BenefitPlanConverter();

try {
    List<BenefitPlan> plans = converter.loadAllPlans();
    System.out.println("Loaded " + plans.size() + " plans");
} catch (IOException e) {
    e.printStackTrace();
}
```

### Find Specific Plan

```java
BenefitPlanConverter converter = new BenefitPlanConverter();

try {
    BenefitPlan goldPlan = converter.findByPlanCode("COMM_GOLD_001");
    if (goldPlan != null) {
        System.out.println("Found: " + goldPlan.getPlanName());
        System.out.println("Tier 1 Copay: $" + goldPlan.getTier1Copay());
    }
} catch (IOException e) {
    e.printStackTrace();
}
```

### Find Plans by Type

```java
BenefitPlanConverter converter = new BenefitPlanConverter();

try {
    List<BenefitPlan> medicareP plans = converter.findByPlanType("MEDICARE");
    System.out.println("Found " + medicarePlans.size() + " Medicare plans");
    
    for (BenefitPlan plan : medicarePlans) {
        System.out.println("  - " + plan.getPlanCode() + ": " + plan.getPlanName());
    }
} catch (IOException e) {
    e.printStackTrace();
}
```

### Calculate Patient Cost

```java
BenefitPlan plan = converter.findByPlanCode("COMM_GOLD_001");

// For a Tier 2 drug
int tier = 2;
BigDecimal drugCost = new BigDecimal("100.00");

BigDecimal patientPay;
if (plan.usesCopayForTier(tier)) {
    // Fixed copay
    patientPay = plan.getCopayForTier(tier);
} else if (plan.usesCoinsuranceForTier(tier)) {
    // Percentage coinsurance
    BigDecimal coinsurance = plan.getCoinsuranceForTier(tier);
    patientPay = drugCost.multiply(coinsurance);
} else {
    // No cost-sharing
    patientPay = BigDecimal.ZERO;
}

System.out.println("Patient pays: $" + patientPay);
```

### Print Plan Summary

```java
BenefitPlanConverter converter = new BenefitPlanConverter();
BenefitPlan plan = converter.findByPlanCode("COMM_GOLD_001");

converter.printPlanSummary(plan);
```

**Output:**
```
======================================================================
Plan Code: COMM_GOLD_001
Plan Name: Gold Commercial Plan
Type: COMMERCIAL / Category: GOLD
Effective Date: 2024-01-01
----------------------------------------------------------------------
Annual Deductible: $500
Out-of-Pocket Max: $3000
----------------------------------------------------------------------
Tier Copays:
  Tier 1 (Generic):           $10
  Tier 2 (Preferred Brand):   $25
  Tier 3 (Non-Preferred):     $50
  Tier 4 (Specialty):         $100
  Tier 5 (Specialty Biologic): $200
----------------------------------------------------------------------
Tier Coinsurance:
  Tier 1: 0%
  Tier 2: 0%
  Tier 3: 0%
  Tier 4: 0%
  Tier 5: 0%
----------------------------------------------------------------------
Mail Order Available: Yes
Specialty Pharmacy Required: Yes
----------------------------------------------------------------------
Description: Low cost-sharing with moderate premiums
======================================================================
```

## Usage in Your Application

You can use the BenefitPlanConverter in your own code to load and work with pharmacy plans:

```java
import dejanlazic.playground.inmemory.rdbms.converter.BenefitPlanConverter;
import dejanlazic.playground.inmemory.rdbms.model.BenefitPlan;

public class YourApplication {
    public static void main(String[] args) {
        BenefitPlanConverter converter = new BenefitPlanConverter();
        
        try {
            // Load all plans
            List<BenefitPlan> plans = converter.loadAllPlans();
            System.out.println("Loaded " + plans.size() + " plans");
            
            // Find specific plan
            BenefitPlan goldPlan = converter.findByPlanCode("COMM_GOLD_001");
            if (goldPlan != null) {
                converter.printPlanSummary(goldPlan);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

## Docker Considerations

### Why CSV in Resources Folder

When running in Docker, the CSV file in resources:
- ✅ Is packaged in the JAR file
- ✅ Accessible via ClassLoader (no file system paths needed)
- ✅ Works identically in development and production
- ✅ No volume mounting required
- ✅ Portable across environments

### Reading from Resources

```java
// This works in both development and Docker
InputStream inputStream = getClass()
    .getClassLoader()
    .getResourceAsStream("us_pharmacy_plans.csv");
```

### Alternative: External File

If you need to update plans without rebuilding:

```yaml
# docker-compose.yml
volumes:
  - ./database/data:/app/data
```

```java
// Read from external file
File file = new File("/app/data/us_pharmacy_plans.csv");
```

## Plan Types Included

The CSV contains 30 plans across 10 categories:

1. **Commercial** (5) - Platinum, Gold, Silver, Bronze, HDHP
2. **Medicare** (4) - Part D Basic, Enhanced, MAPD, Medigap
3. **Medicaid** (2) - Standard, Expansion
4. **Exchange** (2) - Silver, Gold
5. **Employer** (3) - Traditional, HDHP, Premium
6. **Union** (2) - Standard, Premium
7. **Government** (5) - VA, TRICARE, FEHB
8. **CHIP** (1) - Children's Health Insurance
9. **Individual** (4) - Catastrophic, Bronze, Silver, Gold
10. **PBM Carve-Out** (1) - Standalone PBM

## Cost-Sharing Models

### Copay-Based Plans (Most Common)

Plans with fixed dollar amounts per prescription:
- Commercial plans (Platinum, Gold, Silver, Bronze)
- Medicare Part D
- Medicaid
- Employer plans
- Union plans

**Example:** Gold plan charges $10 for Tier 1, $25 for Tier 2, etc.

### Coinsurance-Based Plans (HDHP)

Plans with percentage-based cost-sharing:
- High Deductible Health Plans (HDHP)
- Catastrophic plans

**Example:** HDHP charges 20% for Tier 1, 30% for Tier 3, etc.

### Hybrid Plans

Some plans use copays for lower tiers and coinsurance for higher tiers:
- Medicare Part D (copays for Tiers 1-3, coinsurance for Tier 4)

## Integration with Database

The BenefitPlan POJO can be used to:

1. **Load plans into database:**
```java
BenefitPlanConverter converter = new BenefitPlanConverter();
List<BenefitPlan> plans = converter.loadAllPlans();

for (BenefitPlan plan : plans) {
    // Insert into database using JDBC
    String sql = "INSERT INTO plan (plan_code, plan_name, ...) VALUES (?, ?, ...)";
    // Execute with PreparedStatement
}
```

2. **Compare CSV data with database:**
```java
// Load from CSV
List<BenefitPlan> csvPlans = converter.loadAllPlans();

// Load from database
List<BenefitPlan> dbPlans = loadFromDatabase();

// Compare
```

3. **Validate plan configurations:**
```java
BenefitPlan plan = converter.findByPlanCode("COMM_GOLD_001");

// Validate tier structure
for (int tier = 1; tier <= 5; tier++) {
    if (plan.usesCopayForTier(tier)) {
        System.out.println("Tier " + tier + " uses copay: $" + plan.getCopayForTier(tier));
    } else if (plan.usesCoinsuranceForTier(tier)) {
        System.out.println("Tier " + tier + " uses coinsurance: " + 
            plan.getCoinsuranceForTier(tier).multiply(BigDecimal.valueOf(100)) + "%");
    }
}
```

## Testing

### Unit Tests

Create JUnit tests for the model:

```java
@Test
void testPlanLoading() throws IOException {
    BenefitPlanConverter converter = new BenefitPlanConverter();
    List<BenefitPlan> plans = converter.loadAllPlans();
    
    assertEquals(30, plans.size(), "Should load 30 plans");
}

@Test
void testFindByPlanCode() throws IOException {
    BenefitPlanConverter converter = new BenefitPlanConverter();
    BenefitPlan plan = converter.findByPlanCode("COMM_GOLD_001");
    
    assertNotNull(plan);
    assertEquals("Gold Commercial Plan", plan.getPlanName());
    assertEquals(new BigDecimal("500"), plan.getAnnualDeductible());
}

@Test
void testTierUtilityMethods() throws IOException {
    BenefitPlanConverter converter = new BenefitPlanConverter();
    BenefitPlan plan = converter.findByPlanCode("COMM_GOLD_001");
    
    assertTrue(plan.usesCopayForTier(1));
    assertFalse(plan.usesCoinsuranceForTier(1));
    assertEquals(new BigDecimal("10"), plan.getCopayForTier(1));
}
```

## Next Steps

1. ✅ Create BenefitPlan POJO
2. ✅ Create BenefitPlanConverter
3. ✅ Move CSV to resources folder
4. ⏳ Create JUnit tests for model
5. ⏳ Integrate with database (load CSV data into PostgreSQL)
6. ⏳ Create DAO layer for database operations
7. ⏳ Implement plan lookup service

## Related Documentation

- [DATABASE_CONNECTOR.md](DATABASE_CONNECTOR.md) - Pure JDBC database connectivity
- [DATA.md](database/data/DATA.md) - US pharmacy plans documentation
- [DATABASE_INITIALIZATION.md](DATABASE_INITIALIZATION.md) - Database setup guide