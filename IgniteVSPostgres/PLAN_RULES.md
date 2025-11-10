# Plan Rules Documentation

## Overview

This document defines the rules, requirements, and data model for plan rules in the US healthcare pharmacy benefit management (PBM) system. Plan rules are configurable business logic that defines how a pharmacy benefit plan operates, including coverage decisions, prior authorization requirements, quantity limits, and clinical edits.

## Table of Contents

1. [Core Concepts](#core-concepts)
2. [Rule Types](#rule-types)
3. [Data Model](#data-model)
4. [Business Rules](#business-rules)
5. [Rule Execution](#rule-execution)
6. [Data Generation](#data-generation)

---

## Core Concepts

### What are Plan Rules?

**Plan rules** are configurable business logic entries that define:
- Which drugs are covered and at what cost
- Clinical criteria for drug approval
- Quantity and refill restrictions
- Prior authorization requirements
- Cost-sharing arrangements
- Network requirements

### Rule Components

Each rule consists of:
1. **Rule Type**: Category of rule (e.g., PRIOR_AUTH, QUANTITY_LIMIT)
2. **Rule Criteria**: Conditions that trigger the rule (stored as JSONB)
3. **Rule Action**: What happens when criteria are met (stored as JSONB)
4. **Priority**: Execution order when multiple rules apply
5. **Status**: Active/inactive flag

---

## Rule Types

### 1. Coverage Rules (COVERAGE)

Define which drugs are covered and under what conditions.

**Criteria Examples:**
- Drug class or therapeutic category
- Formulary tier
- Brand vs generic status
- Specialty designation

**Actions:**
- Coverage status (covered/not covered)
- Tier assignment
- Preferred status
- Restrictions or limitations

**Example:**
```json
{
  "rule_type": "COVERAGE",
  "rule_criteria": {
    "drug_class": "ANTIHYPERTENSIVE",
    "is_generic": true
  },
  "rule_action": {
    "covered": true,
    "tier": 1,
    "preferred": true
  }
}
```

### 2. Prior Authorization Rules (PRIOR_AUTH)

Determine when prior authorization is required.

**Criteria Examples:**
- Drug class or specific NDC
- Diagnosis requirements
- Age restrictions
- Quantity thresholds
- Step therapy requirements

**Actions:**
- PA required (yes/no)
- PA type (clinical review, automated)
- Required documentation
- Approval duration

**Example:**
```json
{
  "rule_type": "PRIOR_AUTH",
  "rule_criteria": {
    "drug_class": "SPECIALTY_ONCOLOGY",
    "min_age": 18,
    "diagnosis_required": true,
    "diagnosis_codes": ["C50.9", "C61"]
  },
  "rule_action": {
    "requires_pa": true,
    "pa_type": "CLINICAL_REVIEW",
    "approval_duration_days": 90,
    "required_documents": ["diagnosis", "treatment_plan"]
  }
}
```

### 3. Quantity Limit Rules (QUANTITY_LIMIT)

Set maximum quantities and days supply.

**Criteria Examples:**
- Drug or drug class
- Diagnosis
- Age
- Gender

**Actions:**
- Maximum quantity per fill
- Maximum days supply
- Refill frequency
- Lifetime maximum

**Example:**
```json
{
  "rule_type": "QUANTITY_LIMIT",
  "rule_criteria": {
    "drug_class": "OPIOID_ANALGESIC",
    "acute_pain": true
  },
  "rule_action": {
    "max_quantity": 30,
    "max_days_supply": 7,
    "max_refills": 0,
    "override_allowed": false
  }
}
```

### 4. Cost Share Rules (COST_SHARE)

Define patient cost-sharing amounts.

**Criteria Examples:**
- Tier level
- Pharmacy type (retail/mail/specialty)
- Brand vs generic
- Accumulator status

**Actions:**
- Copay amount
- Coinsurance percentage
- Deductible application
- Out-of-pocket maximum

**Example:**
```json
{
  "rule_type": "COST_SHARE",
  "rule_criteria": {
    "tier": 1,
    "pharmacy_type": "RETAIL",
    "days_supply": 30
  },
  "rule_action": {
    "copay": 10.00,
    "coinsurance": 0.00,
    "apply_deductible": false
  }
}
```

### 5. Clinical Edit Rules (CLINICAL_EDIT)

Enforce clinical safety and appropriateness.

**Criteria Examples:**
- Age restrictions
- Gender restrictions
- Pregnancy category
- Drug-drug interactions
- Duplicate therapy
- Therapeutic duplication

**Actions:**
- Reject claim
- Require override
- Generate warning
- Require prescriber contact

**Example:**
```json
{
  "rule_type": "CLINICAL_EDIT",
  "rule_criteria": {
    "drug_class": "TERATOGENIC",
    "gender": "F",
    "age_range": [15, 45]
  },
  "rule_action": {
    "action": "REQUIRE_OVERRIDE",
    "warning_message": "Pregnancy risk - verify contraception",
    "prescriber_contact_required": true
  }
}
```

### 6. Refill Rules (REFILL_RESTRICTION)

Control refill timing and frequency.

**Criteria Examples:**
- Drug class
- Controlled substance schedule
- Days supply
- Previous fill date

**Actions:**
- Refill too soon threshold (percentage)
- Early refill allowance
- Vacation override allowed
- Lost/stolen policy

**Example:**
```json
{
  "rule_type": "REFILL_RESTRICTION",
  "rule_criteria": {
    "drug_class": "CONTROLLED_SUBSTANCE",
    "dea_schedule": "II"
  },
  "rule_action": {
    "refill_too_soon_threshold": 0.75,
    "early_refill_days": 0,
    "vacation_override": false,
    "lost_stolen_allowed": false
  }
}
```

### 7. Network Rules (NETWORK_RESTRICTION)

Define pharmacy network requirements.

**Criteria Examples:**
- Drug type (specialty/maintenance)
- Days supply
- Cost threshold
- Geographic location

**Actions:**
- Required pharmacy type
- Preferred pharmacy incentive
- Out-of-network penalty
- Mail order requirement

**Example:**
```json
{
  "rule_type": "NETWORK_RESTRICTION",
  "rule_criteria": {
    "drug_type": "SPECIALTY",
    "cost_threshold": 1000.00
  },
  "rule_action": {
    "required_pharmacy_type": "SPECIALTY",
    "out_of_network_allowed": false,
    "preferred_pharmacies": ["SPEC001", "SPEC002"]
  }
}
```

### 8. Step Therapy Rules (STEP_THERAPY)

Require trial of preferred drugs first.

**Criteria Examples:**
- Drug class
- Diagnosis
- Previous medication history

**Actions:**
- Required first-line drugs
- Trial duration
- Failure criteria
- Override conditions

**Example:**
```json
{
  "rule_type": "STEP_THERAPY",
  "rule_criteria": {
    "drug_class": "PROTON_PUMP_INHIBITOR",
    "diagnosis": "GERD"
  },
  "rule_action": {
    "required_first_line": ["omeprazole", "pantoprazole"],
    "trial_duration_days": 30,
    "failure_criteria": "inadequate_response",
    "override_with_pa": true
  }
}
```

### 9. Age/Gender Restriction Rules (AGE_GENDER_RESTRICTION)

Enforce age and gender appropriateness.

**Criteria Examples:**
- Specific drug or class
- Age range
- Gender

**Actions:**
- Allow/deny
- Warning message
- Override requirements

**Example:**
```json
{
  "rule_type": "AGE_GENDER_RESTRICTION",
  "rule_criteria": {
    "drug_name": "finasteride",
    "indication": "BPH"
  },
  "rule_action": {
    "allowed_gender": "M",
    "min_age": 18,
    "deny_if_not_met": true,
    "message": "Finasteride for BPH is indicated for males only"
  }
}
```

### 10. Duplicate Therapy Rules (DUPLICATE_THERAPY)

Prevent concurrent use of similar drugs.

**Criteria Examples:**
- Drug classes
- Therapeutic categories
- Lookback period

**Actions:**
- Reject claim
- Require override
- Allow with warning

**Example:**
```json
{
  "rule_type": "DUPLICATE_THERAPY",
  "rule_criteria": {
    "drug_classes": ["STATIN", "STATIN"],
    "lookback_days": 30
  },
  "rule_action": {
    "action": "REJECT",
    "message": "Duplicate statin therapy detected",
    "override_allowed": true,
    "override_reason_required": true
  }
}
```

---

## Data Model

### Database Schema

```sql
CREATE TABLE plan_rule (
    rule_id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL REFERENCES plan(plan_id) ON DELETE CASCADE,
    rule_type VARCHAR(50) NOT NULL,
    rule_name VARCHAR(255) NOT NULL,
    rule_criteria JSONB NOT NULL,
    rule_action JSONB NOT NULL,
    priority INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

### Field Descriptions

- **rule_id**: Unique identifier
- **plan_id**: Foreign key to plan table
- **rule_type**: Type of rule (see Rule Types above)
- **rule_name**: Human-readable rule description
- **rule_criteria**: JSONB containing conditions (flexible schema)
- **rule_action**: JSONB containing actions to take (flexible schema)
- **priority**: Execution order (higher = earlier, 0 = default)
- **is_active**: Whether rule is currently enforced
- **created_at**: Rule creation timestamp
- **updated_at**: Last modification timestamp

---

## Business Rules

### 1. Rule Priority

- Rules are evaluated in priority order (highest first)
- Rules with same priority evaluated in creation order
- First matching rule typically wins (unless configured otherwise)
- Priority range: -100 to 100 (0 = default)

### 2. Rule Activation

- Only active rules are evaluated
- Rules can be temporarily deactivated without deletion
- Effective date ranges can be stored in criteria
- Historical rules retained for audit purposes

### 3. Rule Conflicts

- Multiple rules may apply to same scenario
- Conflict resolution strategies:
  - **Most Restrictive**: Apply strictest rule
  - **First Match**: Apply first matching rule
  - **All Apply**: Apply all matching rules
  - **Priority Based**: Use priority to determine winner

### 4. Rule Overrides

- Some rules allow manual overrides
- Override requires reason code and documentation
- Override authority levels (pharmacist, prescriber, plan admin)
- Override audit trail maintained

### 5. Rule Testing

- Rules should be tested before activation
- Test mode allows evaluation without enforcement
- A/B testing capabilities for rule optimization
- Impact analysis before rule changes

---

## Rule Execution

### Execution Flow

1. **Claim Received**: New claim enters adjudication
2. **Rule Retrieval**: Load active rules for member's plan
3. **Rule Filtering**: Filter rules applicable to claim
4. **Rule Sorting**: Sort by priority
5. **Rule Evaluation**: Evaluate criteria against claim data
6. **Action Execution**: Execute actions for matching rules
7. **Result Aggregation**: Combine results from all rules
8. **Decision**: Approve, deny, or pend claim

### Performance Considerations

- Index on plan_id and rule_type
- Cache frequently used rules
- Evaluate simple rules first
- Parallel evaluation where possible
- Rule compilation for complex logic

---

## Data Generation

### Generation Strategy

For realistic simulation with 100 plans:

#### Rule Distribution per Plan

- **Basic Plan**: 150-200 rules
- **Standard Plan**: 200-250 rules
- **Comprehensive Plan**: 250-300 rules
- **Medicare/Complex Plan**: 300-400 rules

**Target**: 20,000-30,000 total rules (200-300 per plan average)

#### Rule Type Distribution

- **Coverage Rules**: 30% (6,000-9,000 rules)
- **Prior Auth Rules**: 20% (4,000-6,000 rules)
- **Quantity Limit Rules**: 15% (3,000-4,500 rules)
- **Cost Share Rules**: 15% (3,000-4,500 rules)
- **Clinical Edit Rules**: 10% (2,000-3,000 rules)
- **Refill Rules**: 5% (1,000-1,500 rules)
- **Network Rules**: 3% (600-900 rules)
- **Step Therapy Rules**: 2% (400-600 rules)

#### Priority Distribution

- **High Priority (50-100)**: 10% of rules
- **Normal Priority (0-49)**: 80% of rules
- **Low Priority (-1 to -100)**: 10% of rules

#### Active Status

- **Active**: 95% of rules
- **Inactive**: 5% of rules (for testing/historical)

### CSV File Format

```csv
rule_id,plan_id,rule_type,rule_name,rule_criteria,rule_action,priority,is_active,created_at
```

### Sample Data Patterns

#### Coverage Rule
```csv
1,1,COVERAGE,"Generic Tier 1 Coverage","{""drug_class"":""ANTIHYPERTENSIVE"",""is_generic"":true}","{""covered"":true,""tier"":1,""preferred"":true}",0,true,2024-01-01 00:00:00
```

#### Prior Auth Rule
```csv
2,1,PRIOR_AUTH,"Specialty Oncology PA","{""drug_class"":""SPECIALTY_ONCOLOGY"",""min_age"":18}","{""requires_pa"":true,""pa_type"":""CLINICAL_REVIEW"",""approval_duration_days"":90}",50,true,2024-01-01 00:00:00
```

### Foreign Key Considerations

- **plan_id** must reference existing plans
- Validate plan_id exists before generating rules
- Distribute rules across all plans
- Some rules may be shared across multiple plans (duplicate with different plan_id)

---

## Implementation Notes

### JSONB Advantages

- Flexible schema for diverse rule types
- Easy to add new rule types without schema changes
- Efficient indexing with GIN indexes
- Native JSON operators for querying

### Recommended Indexes

```sql
CREATE INDEX idx_plan_rule_plan_id ON plan_rule(plan_id);
CREATE INDEX idx_plan_rule_type ON plan_rule(rule_type);
CREATE INDEX idx_plan_rule_active ON plan_rule(is_active) WHERE is_active = true;
CREATE INDEX idx_plan_rule_priority ON plan_rule(priority DESC);
CREATE INDEX idx_plan_rule_criteria ON plan_rule USING GIN (rule_criteria);
CREATE INDEX idx_plan_rule_action ON plan_rule USING GIN (rule_action);
```

### Query Examples

```sql
-- Find all active prior auth rules for a plan
SELECT * FROM plan_rule 
WHERE plan_id = 1 
  AND rule_type = 'PRIOR_AUTH' 
  AND is_active = true 
ORDER BY priority DESC;

-- Find rules for specific drug class
SELECT * FROM plan_rule 
WHERE rule_criteria @> '{"drug_class": "SPECIALTY_ONCOLOGY"}'::jsonb
  AND is_active = true;

-- Find rules requiring PA
SELECT * FROM plan_rule 
WHERE rule_action @> '{"requires_pa": true}'::jsonb
  AND is_active = true;
```

---

## References

- **CMS Medicare Part D**: Coverage determination rules
- **NCPDP Standards**: Pharmacy claim adjudication
- **FDA Guidelines**: Drug safety and appropriateness
- **State Regulations**: Controlled substance rules
- **Payer Policies**: Commercial plan requirements

---

## Version History

- **v1.0** (2024-11-10): Initial documentation
  - Defined 10 rule types
  - Established data model
  - Documented business rules
  - Specified data generation patterns