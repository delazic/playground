# Pharmacy Benefit Management (PBM)

## Table of Contents
1. [System Architecture](#system-architecture)
   - [System Overview](#system-overview)
   - [Core Business Processes](#core-business-processes)
   - [High-Level Architecture](#high-level-architecture)
   - [Database Design](#database-design)
   - [Service Layer](#service-layer)
   - [API Specifications](#api-specifications)
   - [Performance & Caching Strategy](#performance--caching-strategy)
   - [Security & Compliance](#security--compliance)
2. [Implementation](#implementation)

---

## System Architecture

### System Overview

The Pharmacy Benefit Management (PBM) system is designed to process prescription drug claims in real-time for the US healthcare market. The system manages the complete lifecycle of pharmacy benefits including member eligibility, formulary management, claims adjudication, pricing calculations, and pharmacy network management.

### Key Features
- **Real-time Claims Adjudication**: Process pharmacy claims in milliseconds
- **Member Eligibility Verification**: Validate member coverage and benefits
- **Formulary Management**: Manage drug coverage, tiers, and restrictions
- **Pricing & Reimbursement**: Calculate patient copays and pharmacy reimbursements
- **Drug Utilization Review (DUR)**: Clinical safety checks and drug interactions
- **Prior Authorization**: Workflow management for restricted medications
- **Pharmacy Network Management**: Maintain pharmacy contracts and networks

### Technology Stack
- **Backend**: Java-based microservices
- **Database**: PostgreSQL for persistent storage
- **Caching Layer**: Pluggable in-memory solution (Apache Ignite, Redis, Hazelcast)
- **API**: RESTful services with JSON payloads
- **Future**: NCPDP standards compliance layer

---

### Core Business Processes

#### 1. Claims Adjudication Workflow

The claims adjudication process is the heart of the PBM system:

```mermaid
flowchart TD
    A[Pharmacy submits claim] --> B[Validate claim format]
    B --> C{Valid?}
    C -->|No| D[Return rejection]
    C -->|Yes| E[Verify member eligibility]
    E --> F{Eligible?}
    F -->|No| D
    F -->|Yes| G[Check formulary coverage]
    G --> H{Covered?}
    H -->|No| I[Check prior auth]
    H -->|Yes| J[Perform DUR checks]
    I --> K{Approved?}
    K -->|No| D
    K -->|Yes| J
    J --> L{DUR Pass?}
    L -->|No| M[Return DUR rejection]
    L -->|Yes| N[Calculate pricing]
    N --> O[Apply plan rules]
    O --> P[Calculate copay/reimbursement]
    P --> Q[Create claim record]
    Q --> R[Return approval with pricing]
```

#### 2. Key Business Entities

- **Member**: Individual covered under a health plan
- **Plan**: Benefit design with coverage rules and cost-sharing
- **Pharmacy**: Retail or mail-order pharmacy in the network
- **Drug**: Medication with NDC code, pricing, and clinical data
- **Formulary**: List of covered drugs with tier assignments
- **Claim**: Transaction record of a prescription fill
- **Prior Authorization**: Approval workflow for restricted drugs

---

### High-Level Architecture

```mermaid
graph TB
    subgraph External Systems
        A[Pharmacy POS System]
        B[Provider EHR System]
        C[Plan Sponsor Portal]
    end
    
    subgraph API Gateway Layer
        D[API Gateway]
        E[Authentication Service]
        F[Rate Limiter]
    end
    
    subgraph Service Layer
        G[Claims Service]
        H[Eligibility Service]
        I[Formulary Service]
        J[Pricing Service]
        K[DUR Service]
        L[Prior Auth Service]
        M[Pharmacy Service]
        N[Member Service]
    end
    
    subgraph Data Layer
        O[Cache Layer]
        P[PostgreSQL Database]
        Q[Audit Log Store]
    end
    
    A --> D
    B --> D
    C --> D
    D --> E
    E --> F
    F --> G
    F --> H
    F --> I
    F --> J
    F --> K
    F --> L
    F --> M
    F --> N
    
    G --> O
    H --> O
    I --> O
    J --> O
    K --> O
    L --> O
    M --> O
    N --> O
    
    O --> P
    G --> Q
    H --> Q
    L --> Q
```

### Microservices Architecture

The system follows a microservices pattern with the following services:

1. **Claims Adjudication Service**: Core claim processing engine
2. **Eligibility Service**: Member coverage verification
3. **Formulary Service**: Drug coverage and tier management
4. **Pricing Service**: Cost calculation engine
5. **DUR Service**: Clinical safety checks
6. **Prior Authorization Service**: Approval workflow management
7. **Pharmacy Service**: Network and pharmacy data management
8. **Member Service**: Member demographics and enrollment
9. **Plan Service**: Benefit plan configuration
10. **Audit Service**: Transaction logging and compliance

---

### Database Design

#### Entity Relationship Diagram

```mermaid
erDiagram
    MEMBER ||--o{ ENROLLMENT : has
    MEMBER ||--o{ CLAIM : submits
    PLAN ||--o{ ENROLLMENT : covers
    PLAN ||--o{ FORMULARY : defines
    PLAN ||--o{ PLAN_RULE : contains
    FORMULARY ||--o{ FORMULARY_DRUG : includes
    DRUG ||--o{ FORMULARY_DRUG : listed_in
    DRUG ||--o{ CLAIM : prescribed
    DRUG ||--o{ DRUG_INTERACTION : has
    PHARMACY ||--o{ CLAIM : processes
    PHARMACY ||--o{ PHARMACY_NETWORK : belongs_to
    CLAIM ||--o{ CLAIM_LINE : contains
    CLAIM ||--o{ DUR_RESULT : generates
    PRIOR_AUTH ||--o{ CLAIM : authorizes
    
    MEMBER {
        uuid member_id PK
        string member_number UK
        string first_name
        string last_name
        date date_of_birth
        string gender
        string address
        string city
        string state
        string zip_code
        string phone
        string email
        timestamp created_at
        timestamp updated_at
    }
    
    PLAN {
        uuid plan_id PK
        string plan_code UK
        string plan_name
        string plan_type
        date effective_date
        date termination_date
        jsonb benefit_design
        boolean is_active
        timestamp created_at
        timestamp updated_at
    }
    
    ENROLLMENT {
        uuid enrollment_id PK
        uuid member_id FK
        uuid plan_id FK
        string group_number
        date effective_date
        date termination_date
        string relationship
        boolean is_active
        timestamp created_at
        timestamp updated_at
    }
    
    DRUG {
        uuid drug_id PK
        string ndc_code UK
        string drug_name
        string generic_name
        string strength
        string dosage_form
        string manufacturer
        string drug_class
        boolean is_generic
        boolean is_brand
        decimal awp_price
        decimal mac_price
        timestamp created_at
        timestamp updated_at
    }
    
    FORMULARY {
        uuid formulary_id PK
        uuid plan_id FK
        string formulary_name
        date effective_date
        date termination_date
        boolean is_active
        timestamp created_at
        timestamp updated_at
    }
    
    FORMULARY_DRUG {
        uuid formulary_drug_id PK
        uuid formulary_id FK
        uuid drug_id FK
        integer tier
        string status
        boolean requires_prior_auth
        boolean requires_step_therapy
        integer quantity_limit
        integer days_supply_limit
        timestamp created_at
        timestamp updated_at
    }
    
    PHARMACY {
        uuid pharmacy_id PK
        string ncpdp_id UK
        string pharmacy_name
        string npi
        string address
        string city
        string state
        string zip_code
        string phone
        string pharmacy_type
        boolean is_active
        timestamp created_at
        timestamp updated_at
    }
    
    PHARMACY_NETWORK {
        uuid network_id PK
        uuid pharmacy_id FK
        string network_name
        date effective_date
        date termination_date
        decimal reimbursement_rate
        boolean is_preferred
        timestamp created_at
        timestamp updated_at
    }
    
    CLAIM {
        uuid claim_id PK
        string claim_number UK
        uuid member_id FK
        uuid pharmacy_id FK
        uuid drug_id FK
        uuid plan_id FK
        date service_date
        date fill_date
        integer quantity_dispensed
        integer days_supply
        decimal ingredient_cost
        decimal dispensing_fee
        decimal total_cost
        decimal patient_pay
        decimal plan_pay
        string claim_status
        string rejection_code
        timestamp submitted_at
        timestamp processed_at
        timestamp created_at
        timestamp updated_at
    }
    
    CLAIM_LINE {
        uuid claim_line_id PK
        uuid claim_id FK
        integer line_number
        string adjustment_type
        decimal adjustment_amount
        string adjustment_reason
        timestamp created_at
    }
    
    DUR_RESULT {
        uuid dur_id PK
        uuid claim_id FK
        string dur_type
        string severity
        string message
        string conflict_drug
        boolean override_allowed
        boolean was_overridden
        timestamp created_at
    }
    
    PRIOR_AUTH {
        uuid prior_auth_id PK
        uuid member_id FK
        uuid drug_id FK
        string auth_number UK
        date effective_date
        date expiration_date
        integer quantity_approved
        string status
        string diagnosis_code
        string prescriber_npi
        timestamp created_at
        timestamp updated_at
    }
    
    PLAN_RULE {
        uuid rule_id PK
        uuid plan_id FK
        string rule_type
        string rule_name
        jsonb rule_criteria
        jsonb rule_action
        integer priority
        boolean is_active
        timestamp created_at
        timestamp updated_at
    }
    
    DRUG_INTERACTION {
        uuid interaction_id PK
        uuid drug_id_1 FK
        uuid drug_id_2 FK
        string severity
        string interaction_type
        string description
        timestamp created_at
    }
```

### Key Database Tables

#### Core Tables

**MEMBER**: Stores member demographic information
- Primary identifier: `member_id` (UUID)
- Unique business key: `member_number`
- Includes personal information and contact details

**PLAN**: Defines benefit plan configurations
- Primary identifier: `plan_id` (UUID)
- Stores benefit design in JSONB for flexibility
- Supports date-based plan versions

**ENROLLMENT**: Links members to plans
- Manages member-plan relationships
- Supports multiple enrollments per member
- Tracks effective and termination dates

**DRUG**: Master drug database
- NDC (National Drug Code) as unique identifier
- Includes pricing information (AWP, MAC)
- Supports both brand and generic drugs

**CLAIM**: Transaction records
- Captures all claim details
- Stores pricing breakdown
- Tracks claim status and processing timestamps

#### Supporting Tables

**FORMULARY**: Drug coverage lists per plan
**FORMULARY_DRUG**: Drug-specific coverage rules
**PHARMACY**: Pharmacy network directory
**PRIOR_AUTH**: Authorization records
**DUR_RESULT**: Clinical safety check results
**PLAN_RULE**: Configurable business rules

### Indexing Strategy

```sql
-- High-performance indexes for common queries
CREATE INDEX idx_member_number ON member(member_number);
CREATE INDEX idx_enrollment_member_active ON enrollment(member_id, is_active);
CREATE INDEX idx_claim_member_date ON claim(member_id, service_date DESC);
CREATE INDEX idx_claim_status ON claim(claim_status, submitted_at);
CREATE INDEX idx_drug_ndc ON drug(ndc_code);
CREATE INDEX idx_formulary_drug_lookup ON formulary_drug(formulary_id, drug_id);
CREATE INDEX idx_pharmacy_ncpdp ON pharmacy(ncpdp_id);
CREATE INDEX idx_prior_auth_member ON prior_auth(member_id, status, expiration_date);

-- Composite indexes for complex queries
CREATE INDEX idx_enrollment_active_dates ON enrollment(member_id, is_active, effective_date, termination_date);
CREATE INDEX idx_claim_processing ON claim(claim_status, submitted_at) WHERE claim_status = 'PENDING';
```

---

### Service Layer

#### 1. Claims Adjudication Service

**Responsibility**: Process pharmacy claims in real-time

**Core Operations**:
- Validate claim data
- Orchestrate eligibility, formulary, and DUR checks
- Calculate pricing and cost-sharing
- Generate claim response

**Key Methods**:
```java
ClaimResponse adjudicateClaim(ClaimRequest request)
ClaimResponse reverseClaim(String claimNumber)
ClaimResponse rebillClaim(String claimNumber, ClaimRequest updates)
List<Claim> getClaimHistory(String memberId, LocalDate startDate, LocalDate endDate)
```

**Business Logic**:
- Real-time processing with <500ms response time
- Atomic transaction handling
- Comprehensive validation rules
- Rejection code mapping

#### 2. Eligibility Service

**Responsibility**: Verify member coverage and benefits

**Core Operations**:
- Check member enrollment status
- Validate coverage dates
- Retrieve benefit details
- Check accumulator balances

**Key Methods**:
```java
EligibilityResponse checkEligibility(String memberId, LocalDate serviceDate)
BenefitDetails getBenefitDetails(String memberId, String planId)
AccumulatorStatus getAccumulators(String memberId, String planId)
```

**Business Logic**:
- Date-based eligibility validation
- Multi-plan support
- Real-time accumulator tracking
- Coordination of benefits (COB)

#### 3. Formulary Service

**Responsibility**: Manage drug coverage and restrictions

**Core Operations**:
- Check drug coverage status
- Retrieve tier information
- Validate quantity limits
- Check prior authorization requirements

**Key Methods**:
```java
FormularyStatus checkCoverage(String planId, String ndcCode)
List<FormularyDrug> searchFormulary(String planId, String searchTerm)
List<Alternative> getAlternatives(String ndcCode, String planId)
```

**Business Logic**:
- Tier-based coverage
- Step therapy protocols
- Quantity and day supply limits
- Therapeutic alternatives

#### 4. Pricing Service

**Responsibility**: Calculate costs and reimbursements

**Core Operations**:
- Calculate ingredient cost
- Apply pricing formulas (AWP, MAC, etc.)
- Calculate patient copay/coinsurance
- Determine pharmacy reimbursement

**Key Methods**:
```java
PricingResult calculatePricing(PricingRequest request)
CopayAmount calculateCopay(String planId, String ndcCode, int tier)
ReimbursementAmount calculateReimbursement(String pharmacyId, String ndcCode)
```

**Business Logic**:
- Multiple pricing methodologies
- Tier-based cost sharing
- Deductible and out-of-pocket max tracking
- Network-based reimbursement rates

#### 5. Drug Utilization Review (DUR) Service

**Responsibility**: Perform clinical safety checks

**Core Operations**:
- Check drug-drug interactions
- Validate therapeutic duplication
- Check age/gender appropriateness
- Verify dosage limits

**Key Methods**:
```java
List<DURResult> performDUR(DURRequest request)
List<Interaction> checkInteractions(String ndcCode, List<String> currentMedications)
boolean validateDosage(String ndcCode, int quantity, int daysSupply)
```

**Business Logic**:
- Real-time interaction checking
- Severity-based alerts
- Override capabilities
- Clinical rule engine

#### 6. Prior Authorization Service

**Responsibility**: Manage approval workflows

**Core Operations**:
- Submit authorization requests
- Check authorization status
- Approve/deny requests
- Track authorization usage

**Key Methods**:
```java
PriorAuth submitRequest(PriorAuthRequest request)
PriorAuthStatus checkStatus(String authNumber)
PriorAuth approveRequest(String authNumber, ApprovalDetails details)
boolean validateAuthorization(String memberId, String ndcCode, LocalDate serviceDate)
```

**Business Logic**:
- Workflow state management
- Expiration tracking
- Quantity tracking
- Clinical criteria evaluation

#### 7. Pharmacy Service

**Responsibility**: Manage pharmacy network data

**Core Operations**:
- Maintain pharmacy directory
- Manage network assignments
- Track pharmacy contracts
- Validate pharmacy credentials

**Key Methods**:
```java
Pharmacy getPharmacy(String ncpdpId)
List<Pharmacy> searchPharmacies(PharmacySearchCriteria criteria)
NetworkStatus checkNetworkStatus(String pharmacyId, String networkId)
```

#### 8. Member Service

**Responsibility**: Manage member information

**Core Operations**:
- Maintain member demographics
- Manage enrollments
- Track member history
- Handle member updates

**Key Methods**:
```java
Member getMember(String memberId)
List<Enrollment> getEnrollments(String memberId)
Member updateMember(String memberId, MemberUpdate update)
```

---

### API Specifications

#### REST API Design Principles

- RESTful resource-based URLs
- JSON request/response format
- HTTP status codes for responses
- Versioned APIs (v1, v2, etc.)
- Pagination for list endpoints
- Filtering and sorting support

#### Authentication & Authorization

```
Authorization: Bearer <JWT_TOKEN>
X-API-Key: <API_KEY>
```

#### Common Response Structure

```json
{
  "status": "success|error",
  "timestamp": "2024-01-15T10:30:00Z",
  "data": {},
  "errors": [],
  "metadata": {
    "requestId": "uuid",
    "processingTime": 125
  }
}
```

#### 1. Claims API

##### Submit Claim
```
POST /api/v1/claims
Content-Type: application/json

Request:
{
  "claimNumber": "CLM20240115001",
  "memberId": "M123456",
  "pharmacyId": "PHM001",
  "serviceDate": "2024-01-15",
  "prescription": {
    "ndcCode": "00002-1234-01",
    "quantity": 30,
    "daysSupply": 30,
    "refillNumber": 0,
    "prescriberId": "NPI1234567890"
  },
  "pricing": {
    "ingredientCost": 125.50,
    "dispensingFee": 2.50
  }
}

Response (200 OK):
{
  "status": "success",
  "timestamp": "2024-01-15T10:30:00Z",
  "data": {
    "claimId": "uuid",
    "claimNumber": "CLM20240115001",
    "claimStatus": "APPROVED",
    "authorizationNumber": "AUTH123456",
    "pricing": {
      "totalCost": 128.00,
      "patientPay": 10.00,
      "planPay": 118.00,
      "copayType": "FLAT"
    },
    "durResults": [],
    "messages": [
      {
        "code": "INFO001",
        "message": "Claim processed successfully"
      }
    ]
  },
  "metadata": {
    "requestId": "uuid",
    "processingTime": 245
  }
}

Response (400 Bad Request - Rejection):
{
  "status": "error",
  "timestamp": "2024-01-15T10:30:00Z",
  "errors": [
    {
      "code": "REJECT_75",
      "message": "Prior authorization required",
      "field": "prescription.ndcCode"
    }
  ],
  "metadata": {
    "requestId": "uuid",
    "processingTime": 180
  }
}
```

##### Reverse Claim
```
POST /api/v1/claims/{claimNumber}/reverse
Content-Type: application/json

Request:
{
  "reversalReason": "DISPENSING_ERROR"
}

Response (200 OK):
{
  "status": "success",
  "data": {
    "claimId": "uuid",
    "claimNumber": "CLM20240115001",
    "claimStatus": "REVERSED",
    "reversalDate": "2024-01-15T11:00:00Z"
  }
}
```

##### Get Claim History
```
GET /api/v1/claims?memberId=M123456&startDate=2024-01-01&endDate=2024-01-31&page=1&size=20

Response (200 OK):
{
  "status": "success",
  "data": {
    "claims": [
      {
        "claimId": "uuid",
        "claimNumber": "CLM20240115001",
        "serviceDate": "2024-01-15",
        "drugName": "Lisinopril 10mg",
        "quantity": 30,
        "totalCost": 128.00,
        "patientPay": 10.00,
        "claimStatus": "APPROVED"
      }
    ],
    "pagination": {
      "page": 1,
      "size": 20,
      "totalElements": 45,
      "totalPages": 3
    }
  }
}
```

#### 2. Eligibility API

##### Check Eligibility
```
POST /api/v1/eligibility/check
Content-Type: application/json

Request:
{
  "memberId": "M123456",
  "serviceDate": "2024-01-15"
}

Response (200 OK):
{
  "status": "success",
  "data": {
    "memberId": "M123456",
    "memberName": "John Doe",
    "isEligible": true,
    "plan": {
      "planId": "PLN001",
      "planName": "Gold Plan",
      "effectiveDate": "2024-01-01",
      "terminationDate": "2024-12-31"
    },
    "benefits": {
      "deductible": {
        "amount": 500.00,
        "met": 150.00,
        "remaining": 350.00
      },
      "outOfPocketMax": {
        "amount": 2000.00,
        "met": 450.00,
        "remaining": 1550.00
      }
    }
  }
}
```

#### 3. Formulary API

##### Check Drug Coverage
```
GET /api/v1/formulary/coverage?planId=PLN001&ndcCode=00002-1234-01

Response (200 OK):
{
  "status": "success",
  "data": {
    "isCovered": true,
    "tier": 2,
    "status": "PREFERRED",
    "restrictions": {
      "requiresPriorAuth": false,
      "requiresStepTherapy": false,
      "quantityLimit": 30,
      "daysSupplyLimit": 30
    },
    "costSharing": {
      "copay": 10.00,
      "coinsurance": null
    }
  }
}
```

##### Search Formulary
```
GET /api/v1/formulary/search?planId=PLN001&query=lisinopril&page=1&size=20

Response (200 OK):
{
  "status": "success",
  "data": {
    "drugs": [
      {
        "ndcCode": "00002-1234-01",
        "drugName": "Lisinopril 10mg Tablet",
        "genericName": "Lisinopril",
        "tier": 1,
        "isGeneric": true,
        "copay": 5.00
      }
    ],
    "pagination": {
      "page": 1,
      "size": 20,
      "totalElements": 8,
      "totalPages": 1
    }
  }
}
```

#### 4. Prior Authorization API

##### Submit Prior Auth Request
```
POST /api/v1/prior-auth
Content-Type: application/json

Request:
{
  "memberId": "M123456",
  "ndcCode": "00002-5678-01",
  "quantity": 30,
  "daysSupply": 30,
  "diagnosisCode": "I10",
  "prescriberId": "NPI1234567890",
  "clinicalJustification": "Patient has tried and failed two other medications"
}

Response (201 Created):
{
  "status": "success",
  "data": {
    "priorAuthId": "uuid",
    "authNumber": "PA20240115001",
    "status": "PENDING",
    "submittedDate": "2024-01-15T10:30:00Z",
    "expectedDecisionDate": "2024-01-17T23:59:59Z"
  }
}
```

##### Check Prior Auth Status
```
GET /api/v1/prior-auth/{authNumber}

Response (200 OK):
{
  "status": "success",
  "data": {
    "priorAuthId": "uuid",
    "authNumber": "PA20240115001",
    "status": "APPROVED",
    "effectiveDate": "2024-01-16",
    "expirationDate": "2024-07-16",
    "quantityApproved": 30,
    "approvedBy": "Clinical Pharmacist",
    "approvalDate": "2024-01-16T14:30:00Z"
  }
}
```

#### 5. Pharmacy API

##### Get Pharmacy Details
```
GET /api/v1/pharmacies/{ncpdpId}

Response (200 OK):
{
  "status": "success",
  "data": {
    "pharmacyId": "uuid",
    "ncpdpId": "1234567",
    "pharmacyName": "Main Street Pharmacy",
    "npi": "1234567890",
    "address": {
      "street": "123 Main St",
      "city": "Springfield",
      "state": "IL",
      "zipCode": "62701"
    },
    "phone": "217-555-0100",
    "pharmacyType": "RETAIL",
    "networks": [
      {
        "networkId": "NET001",
        "networkName": "Preferred Network",
        "isPreferred": true
      }
    ]
  }
}
```

##### Search Pharmacies
```
GET /api/v1/pharmacies/search?zipCode=62701&radius=10&networkId=NET001

Response (200 OK):
{
  "status": "success",
  "data": {
    "pharmacies": [
      {
        "pharmacyId": "uuid",
        "ncpdpId": "1234567",
        "pharmacyName": "Main Street Pharmacy",
        "address": "123 Main St, Springfield, IL 62701",
        "distance": 2.5,
        "isPreferred": true,
        "phone": "217-555-0100"
      }
    ]
  }
}
```

#### 6. Member API

##### Get Member Details
```
GET /api/v1/members/{memberId}

Response (200 OK):
{
  "status": "success",
  "data": {
    "memberId": "uuid",
    "memberNumber": "M123456",
    "firstName": "John",
    "lastName": "Doe",
    "dateOfBirth": "1980-05-15",
    "gender": "M",
    "contact": {
      "address": "456 Oak Ave",
      "city": "Springfield",
      "state": "IL",
      "zipCode": "62701",
      "phone": "217-555-0200",
      "email": "john.doe@email.com"
    },
    "enrollments": [
      {
        "enrollmentId": "uuid",
        "planId": "PLN001",
        "planName": "Gold Plan",
        "effectiveDate": "2024-01-01",
        "terminationDate": "2024-12-31",
        "isActive": true
      }
    ]
  }
}
```

#### API Error Codes

| Code | Description |
|------|-------------|
| REJECT_01 | Member not found |
| REJECT_02 | Member not eligible |
| REJECT_03 | Plan not active |
| REJECT_04 | Drug not covered |
| REJECT_05 | Quantity limit exceeded |
| REJECT_06 | Days supply limit exceeded |
| REJECT_75 | Prior authorization required |
| REJECT_76 | Step therapy required |
| REJECT_88 | DUR rejection |
| REJECT_99 | System error |

---

### Performance & Caching Strategy

#### Performance Requirements

- **Claims Processing**: <500ms response time (95th percentile)
- **Eligibility Check**: <100ms response time
- **Formulary Lookup**: <50ms response time
- **Throughput**: 10,000+ claims per minute
- **Availability**: 99.9% uptime

#### Caching Architecture

```mermaid
graph LR
    A[API Request] --> B{Cache Hit?}
    B -->|Yes| C[Return from Cache]
    B -->|No| D[Query Database]
    D --> E[Update Cache]
    E --> F[Return Response]
    
    subgraph Cache Layer
        G[Member Cache]
        H[Plan Cache]
        I[Formulary Cache]
        J[Drug Cache]
        K[Pharmacy Cache]
    end
```

#### Cache Strategy by Entity

##### 1. Member Data
- **Cache Duration**: 15 minutes
- **Invalidation**: On member update
- **Key Pattern**: `member:{memberId}`
- **Rationale**: Moderate change frequency, high read volume

##### 2. Plan & Formulary Data
- **Cache Duration**: 1 hour
- **Invalidation**: On plan/formulary update
- **Key Pattern**: `plan:{planId}`, `formulary:{planId}:{ndcCode}`
- **Rationale**: Low change frequency, very high read volume

##### 3. Drug Master Data
- **Cache Duration**: 24 hours
- **Invalidation**: On drug update
- **Key Pattern**: `drug:{ndcCode}`
- **Rationale**: Very low change frequency, high read volume

##### 4. Pharmacy Data
- **Cache Duration**: 1 hour
- **Invalidation**: On pharmacy update
- **Key Pattern**: `pharmacy:{ncpdpId}`
- **Rationale**: Low change frequency, moderate read volume

##### 5. Prior Authorization Status
- **Cache Duration**: 5 minutes
- **Invalidation**: On status change
- **Key Pattern**: `prior-auth:{authNumber}`
- **Rationale**: Moderate change frequency, high read volume during claim processing

#### Cache Implementation Options

##### Option 1: Apache Ignite
```java
// Distributed cache with SQL capabilities
IgniteCache<String, Member> memberCache = ignite.cache("memberCache");
memberCache.put(memberId, member);
Member cachedMember = memberCache.get(memberId);
```

**Pros**:
- SQL queries on cached data
- Distributed computing capabilities
- ACID transactions
- Persistence options

**Cons**:
- Higher memory footprint
- More complex setup

##### Option 2: Redis
```java
// Simple key-value cache
redisTemplate.opsForValue().set("member:" + memberId, member, 15, TimeUnit.MINUTES);
Member cachedMember = redisTemplate.opsForValue().get("member:" + memberId);
```

**Pros**:
- Simple and fast
- Rich data structures
- Pub/sub capabilities
- Wide adoption

**Cons**:
- Limited query capabilities
- Single-threaded per instance

##### Option 3: Hazelcast
```java
// In-memory data grid
HazelcastInstance hz = Hazelcast.newHazelcastInstance();
IMap<String, Member> memberMap = hz.getMap("members");
memberMap.put(memberId, member);
```

**Pros**:
- Easy clustering
- Good Java integration
- Distributed collections
- WAN replication

**Cons**:
- Less feature-rich than Ignite
- Smaller community

#### Database Optimization

##### Connection Pooling
```java
// HikariCP configuration
HikariConfig config = new HikariConfig();
config.setMaximumPoolSize(50);
config.setMinimumIdle(10);
config.setConnectionTimeout(30000);
config.setIdleTimeout(600000);
config.setMaxLifetime(1800000);
```

##### Read Replicas
- Master for writes
- Read replicas for queries
- Load balancing across replicas

##### Partitioning Strategy
- Partition claims by date (monthly partitions)
- Partition audit logs by date (daily partitions)
- Improves query performance and maintenance

```sql
-- Example: Partitioned claims table
CREATE TABLE claim (
    claim_id UUID,
    service_date DATE,
    ...
) PARTITION BY RANGE (service_date);

CREATE TABLE claim_2024_01 PARTITION OF claim
    FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');
```

#### Monitoring & Metrics

Key metrics to track:
- Cache hit ratio (target: >90%)
- Average response time per endpoint
- Database query execution time
- Connection pool utilization
- Error rates by type
- Throughput (requests per second)

---

### Security & Compliance

#### Security Architecture

```mermaid
graph TB
    A[Client] -->|HTTPS/TLS 1.3| B[API Gateway]
    B -->|JWT Validation| C[Auth Service]
    C -->|RBAC Check| D[Authorization]
    D -->|Encrypted| E[Service Layer]
    E -->|Encrypted at Rest| F[Database]
    E -->|Audit Log| G[Audit Service]
    
    subgraph Security Controls
        H[WAF]
        I[Rate Limiting]
        J[DDoS Protection]
        K[Encryption]
    end
```

#### Authentication & Authorization

##### JWT Token Structure
```json
{
  "sub": "user@example.com",
  "iss": "pbm-auth-service",
  "aud": "pbm-api",
  "exp": 1705334400,
  "iat": 1705330800,
  "roles": ["PHARMACY", "CLAIMS_PROCESSOR"],
  "permissions": [
    "claims:submit",
    "claims:reverse",
    "eligibility:check"
  ],
  "pharmacyId": "PHM001"
}
```

##### Role-Based Access Control (RBAC)

| Role | Permissions |
|------|-------------|
| PHARMACY | Submit claims, check eligibility, search formulary |
| CLAIMS_PROCESSOR | View claims, process reversals, run reports |
| CLINICAL_PHARMACIST | Review prior auths, override DUR, view clinical data |
| PLAN_ADMIN | Manage plans, formularies, configure rules |
| SYSTEM_ADMIN | Full access, user management, system configuration |

#### Data Protection

##### Encryption at Rest
- Database: AES-256 encryption
- Backups: Encrypted with separate keys
- File storage: Server-side encryption

##### Encryption in Transit
- TLS 1.3 for all API communications
- Certificate pinning for mobile apps
- Mutual TLS for service-to-service communication

##### PHI/PII Protection
- Tokenization of sensitive data
- Field-level encryption for SSN, DOB
- Masking in logs and error messages
- Data retention policies

#### HIPAA Compliance

##### Required Controls

1. **Access Controls**
   - Unique user identification
   - Emergency access procedures
   - Automatic logoff
   - Encryption and decryption

2. **Audit Controls**
   - Log all PHI access
   - Track user activities
   - Tamper-proof audit logs
   - Regular audit reviews

3. **Integrity Controls**
   - Data validation
   - Error correction
   - Checksums for data transmission

4. **Transmission Security**
   - Encryption in transit
   - Integrity controls
   - Network security

##### Audit Logging

All PHI access must be logged:
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "userId": "user@example.com",
  "action": "READ",
  "resource": "member",
  "resourceId": "M123456",
  "ipAddress": "192.168.1.100",
  "userAgent": "PBM-API-Client/1.0",
  "result": "SUCCESS",
  "phi_accessed": ["name", "dob", "address"]
}
```

#### API Security

##### Rate Limiting
```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1705334400
```

- Per API key: 1000 requests/minute
- Per IP: 100 requests/minute
- Burst allowance: 150% of limit for 10 seconds

##### Input Validation
- Schema validation for all requests
- SQL injection prevention
- XSS protection
- Command injection prevention

##### API Key Management
- Rotation every 90 days
- Separate keys for dev/test/prod
- Key revocation capability
- Usage tracking per key

#### Disaster Recovery

##### Backup Strategy
- Database: Daily full backup, hourly incremental
- Retention: 30 days online, 7 years archived
- Geographic redundancy: Multi-region backups
- Recovery Time Objective (RTO): 4 hours
- Recovery Point Objective (RPO): 1 hour

##### High Availability
- Multi-AZ deployment
- Active-active database replication
- Automatic failover
- Health checks and monitoring

---

## Implementation

### Development Environment Setup

This section provides step-by-step instructions for setting up the PBM system development environment on macOS.

#### Prerequisites

- **macOS** (10.15 or later)
- **Homebrew** (package manager)
- **Docker Desktop** (for containerized services)
- **Java 17+** (OpenJDK recommended)
- **Maven 3.8+** (build tool)
- **Git** (version control)

#### Quick Start

The fastest way to get started is using the automated setup script:

```bash
# Navigate to project directory
cd IgniteVSPostgres

# Make setup script executable
chmod +x setup.sh

# Run setup script
./setup.sh
```

The setup script will:
1. Check and install required dependencies (Homebrew, Docker, Java, Maven, PostgreSQL client)
2. Create environment configuration file (.env)
3. Start Docker containers (PostgreSQL, Redis, pgAdmin)
4. Initialize database schema with sample data
5. Verify the installation

#### Manual Setup

If you prefer to set up manually or need more control:

##### 1. Install Dependencies

```bash
# Install Homebrew (if not already installed)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install Docker Desktop
# Download from: https://www.docker.com/products/docker-desktop

# Install Java 17
brew install openjdk@17

# Install Maven
brew install maven

# Install PostgreSQL client (optional, for psql command)
brew install postgresql@16

# Install Docker Compose (if not included with Docker Desktop)
brew install docker-compose
```

##### 2. Configure Environment

```bash
# Copy environment template
cp .env.example .env

# Edit .env file with your preferred settings (optional)
nano .env
```

##### 3. Start Services

```bash
# Start all services using Docker Compose
docker-compose up -d

# Or use Make commands
make start
```

##### 4. Verify Installation

```bash
# Check PostgreSQL is running
docker-compose exec postgres pg_isready -U pbm_user -d pbm_db

# Connect to database
psql -h localhost -p 5432 -U pbm_user -d pbm_db
# Password: pbm_password

# Check tables were created
\dt

# Exit psql
\q
```

#### Service Access

Once setup is complete, you can access the following services:

**PostgreSQL Database:**
- Host: `localhost`
- Port: `5432`
- Database: `pbm_db`
- Username: `pbm_user`
- Password: `pbm_password`

**pgAdmin (Database Management UI):**
- URL: http://localhost:5050
- Email: `admin@example.com`
- Password: `admin`

**Redis Cache:**
- Host: `localhost`
- Port: `6379`

#### Useful Commands

The project includes a Makefile with convenient commands:

```bash
# Show all available commands
make help

# Docker Services
make start              # Start all services
make stop               # Stop all services
make restart            # Restart all services
make logs               # View logs from all services
make logs-db            # View PostgreSQL logs only
make logs-redis         # View Redis logs only

# Database
make db-shell           # Connect to PostgreSQL shell
make db-reset           # Reset database (WARNING: deletes all data)
make clean              # Clean up everything (containers and volumes)
make pgadmin            # Open pgAdmin in browser

# Build & Test
make build              # Build Java application
make test               # Run tests

# Run Application - All Entities
make run                # Run all CRUD operations for all entities
make run-all            # Same as 'make run'

# Plan Operations
make run-create-plan    # CREATE: Insert benefit plans from CSV
make run-read-plan      # READ: Display benefit plans
make run-update-plan    # UPDATE: Update a sample plan
make run-delete-plan    # DELETE: Delete a sample plan
make run-all-plan       # Run all CRUD operations for plans

# Member Operations
make run-create-member  # CREATE: Insert members from CSV
make run-read-member    # READ: Display member count
make run-update-member  # UPDATE: Update a sample member
make run-delete-member  # DELETE: Delete a sample member
make run-all-member     # Run all CRUD operations for members

# Enrollment Operations
make run-create-enrollment  # CREATE: Insert 10M enrollments from CSV
make run-read-enrollment    # READ: Display enrollment statistics
make run-update-enrollment  # UPDATE: Update a sample enrollment
make run-delete-enrollment  # DELETE: Delete a sample enrollment
make run-all-enrollment     # Run all CRUD operations for enrollments

# Drug Operations
make run-create-drug        # CREATE: Insert 20K drugs from CSV
make run-read-drug          # READ: Display drug statistics
make run-update-drug        # UPDATE: Update a sample drug
make run-delete-drug        # DELETE: Delete a sample drug
make run-all-drug           # Run all CRUD operations for drugs

# Pharmacy Operations 🆕
make run-create-pharmacy    # CREATE: Insert 50K pharmacies from CSV
make run-read-pharmacy      # READ: Display pharmacy statistics
make run-update-pharmacy    # UPDATE: Update a sample pharmacy
make run-delete-pharmacy    # DELETE: Delete a sample pharmacy
make run-all-pharmacy       # Run all CRUD operations for pharmacies

# Formulary Operations
make run-create-formulary   # CREATE: Insert formularies from CSV
make run-read-formulary     # READ: Display formulary statistics
make run-update-formulary   # UPDATE: Update a sample formulary
make run-delete-formulary   # DELETE: Delete a sample formulary
make run-all-formulary      # Run all CRUD operations for formularies

# Formulary-Drug Operations
make run-create-formulary-drug  # CREATE: Insert formulary-drug relationships from CSV (10M records)
make run-read-formulary-drug    # READ: Display formulary-drug statistics
make run-update-formulary-drug  # UPDATE: Update a sample formulary-drug relationship
make run-delete-formulary-drug  # DELETE: Delete a sample formulary-drug relationship
make run-all-formulary-drug     # Run all CRUD operations for formulary-drug relationships

# Pharmacy Network Operations 🆕
make run-create-pharmacy-network  # CREATE: Insert pharmacy networks from CSV (550K records)
make run-read-pharmacy-network    # READ: Display pharmacy network statistics
make run-update-pharmacy-network  # UPDATE: Update a sample pharmacy network
make run-delete-pharmacy-network  # DELETE: Delete a sample pharmacy network
make run-all-pharmacy-network     # Run all CRUD operations for pharmacy networks
```

#### Database Schema

The database schema is automatically created when Docker containers start. The initialization scripts are located in:

- `database/init/01-create-schema.sql` - Creates all tables, indexes, and constraints
- `database/init/02-seed-data.sql` - Populates sample data for testing

**Database Structure:**
- 15+ tables covering all PBM entities
- UUID primary keys for all tables
- Partitioned claims table by service_date
- Comprehensive indexes for performance
- Automatic timestamp triggers
- Sample data including:
  - 5 members (seed data)
  - 4 plans (Gold, Silver, Bronze, Medicare - seed data)
  - 13 drugs (generic and brand - seed data)
  - 5 pharmacies (seed data)
  - 15 sample claims (seed data)
  - Drug interactions
  - Prior authorizations

**Large-Scale Test Data:**
- 34 US pharmacy benefit plans (CSV)
- 20,000 US pharmacy drugs (1 CSV file)
- 50,000 US pharmacies (1 CSV file)
- 550,000 pharmacy network assignments (3 CSV files) 🆕
- 1,000,000 members (10 CSV files)
- 10,000,000 enrollments (20 CSV files)
- 4,909 formularies (1 CSV file)
- 10,000,000 formulary-drug relationships (64 CSV files)

#### Connecting to PostgreSQL

**Using psql (command line):**
```bash
psql -h localhost -p 5432 -U pbm_user -d pbm_db
```

**Using pgAdmin (web interface):**
1. Open http://localhost:5050
2. Login with admin@example.com / admin
3. Add new server:
   - Name: PBM Local
   - Host: host.docker.internal (on Mac) or postgres (if in Docker network)
   - Port: 5432
   - Database: pbm_db
   - Username: pbm_user
   - Password: pbm_password

**Using Java application:**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/pbm_db
spring.datasource.username=pbm_user
spring.datasource.password=pbm_password
```

#### Troubleshooting

**Docker not running:**
```bash
# Start Docker Desktop application
open -a Docker

# Wait for Docker to start, then retry
docker-compose up -d
```

**Port conflicts:**
```bash
# Check what's using port 5432
lsof -i :5432

# Stop conflicting service or change port in docker-compose.yml
```

**Database connection refused:**
```bash
# Check PostgreSQL container status
docker-compose ps

# View PostgreSQL logs
docker-compose logs postgres

# Restart PostgreSQL
docker-compose restart postgres
```

**Permission denied on setup.sh:**
```bash
# Make script executable
chmod +x setup.sh
```

**Database schema not created:**
```bash
# Manually run initialization scripts
docker-compose exec -T postgres psql -U pbm_user -d pbm_db < database/init/01-create-schema.sql
docker-compose exec -T postgres psql -U pbm_user -d pbm_db < database/init/02-seed-data.sql
```

#### Next Steps

After setting up the development environment:

1. **Generate Test Data:**
   ```bash
   cd database/scripts
   
   # Generate all data files
   python3 generate_pharmacies.py          # 50K pharmacies
   python3 generate_pharmacy_networks.py   # 550K network assignments 🆕
   python3 generate_drugs.py               # 20K drugs
   python3 generate_formularies.py         # 5K formularies
   python3 generate_members.py             # 1M members
   python3 generate_enrollments.py         # 10M enrollments
   python3 generate_formularies_drugs.py   # 10M formulary-drug relationships
   ```

2. **Load Test Data:**
   
   **Option A: Load All Data at Once (Recommended)**
   ```bash
   # Load all data in correct order: Plan → Drug → Pharmacy → Pharmacy Networks → Member → Enrollment → Formulary → Formulary-Drug
   # This respects foreign key relationships automatically
   # ⚠️ Total time: 15-20 minutes for all 20+ million records
   make load-all-data
   ```
   
   **Option B: Load Data Step by Step**
   ```bash
   # Step 1: Load benefit plans (34 plans)
   make run-create-plan
   
   # Step 2: Load drugs (20,000 drugs)
   make run-create-drug
   
   # Step 3: Load pharmacies (50,000 pharmacies)
   make run-create-pharmacy
   
   # Step 4: Load pharmacy networks (550,000 network assignments) 🆕
   make run-create-pharmacy-network
   
   # Step 5: Load members (1 million members)
   make run-create-member
   
   # Step 6: Load enrollments (10 million enrollments)
   # ⚠️ This will take 5-10 minutes
   make run-create-enrollment
   
   # Step 7: Load formularies (4,909 formularies)
   make run-create-formulary
   
   # Step 8: Load formulary-drug relationships (10 million relationships)
   # ⚠️ This will take 5-10 minutes
   make run-create-formulary-drug
   ```

3. **Explore the Database:**
   ```sql
   -- View all tables
   \dt
   
   -- Check loaded data
   SELECT COUNT(*) FROM plan;              -- Should show 34
   SELECT COUNT(*) FROM drug;              -- Should show 20,000
   SELECT COUNT(*) FROM pharmacy;          -- Should show 50,000
   SELECT COUNT(*) FROM pharmacy_network;  -- Should show 550,000 🆕
   SELECT COUNT(*) FROM member;            -- Should show 1,000,000
   SELECT COUNT(*) FROM enrollment;        -- Should show 10,000,000
   SELECT COUNT(*) FROM formulary;         -- Should show 4,909
   SELECT COUNT(*) FROM formulary_drug;    -- Should show 10,000,000
   
   -- Check enrollment statistics
   SELECT COUNT(*) FROM enrollment WHERE is_active = true;
   SELECT relationship, COUNT(*) FROM enrollment GROUP BY relationship;
   
   -- Check drug statistics
   SELECT COUNT(*) FROM drug WHERE is_generic = true;
   SELECT COUNT(*) FROM drug WHERE is_brand = true;
   SELECT drug_class, COUNT(*) FROM drug GROUP BY drug_class ORDER BY COUNT(*) DESC LIMIT 10;
   
   -- Check pharmacy statistics
   SELECT COUNT(*) FROM pharmacy WHERE is_active = true;
   SELECT pharmacy_type, COUNT(*) FROM pharmacy GROUP BY pharmacy_type ORDER BY COUNT(*) DESC;
   SELECT state, COUNT(*) FROM pharmacy GROUP BY state ORDER BY COUNT(*) DESC LIMIT 10;
   
   -- Check pharmacy network statistics 🆕
   SELECT COUNT(*) FROM pharmacy_network;
   SELECT network_type, COUNT(*) FROM pharmacy_network GROUP BY network_type ORDER BY COUNT(*) DESC;
   SELECT network_tier, COUNT(*) FROM pharmacy_network GROUP BY network_tier;
   SELECT status, COUNT(*) FROM pharmacy_network GROUP BY status;
   SELECT COUNT(*) FROM pharmacy_network WHERE is_preferred = true;
   
   -- Check average networks per pharmacy 🆕
   SELECT AVG(network_count) as avg_networks_per_pharmacy
   FROM (SELECT pharmacy_id, COUNT(*) as network_count
         FROM pharmacy_network
         GROUP BY pharmacy_id) subq;
   
   -- Check formulary statistics
   SELECT COUNT(*) FROM formulary WHERE is_active = true;
   
   -- Check formulary-drug statistics
   SELECT COUNT(*) FROM formulary_drug;
   SELECT tier, COUNT(*) FROM formulary_drug GROUP BY tier ORDER BY tier;
   SELECT status, COUNT(*) FROM formulary_drug GROUP BY status;
   SELECT COUNT(*) FROM formulary_drug WHERE requires_prior_auth = true;
   ```

4. **Review the Architecture:**
   - Read the [System Architecture](#system-architecture) section
   - Understand the [Database Design](#database-design)
   - Review [API Specifications](#api-specifications)

5. **Start Development:**
   - Proceed to Phase 1 tasks below
   - Implement core data models
   - Set up CI/CD pipeline

---

### Roadmap

#### Phase 1: Foundation (Months 1-2)
- [x] Set up development environment
- [x] Create database schema
- [x] Implement core data models (Plan, Drug, Pharmacy, Member, Enrollment, Formulary, FormularyDrug) 🆕
- [x] Generate large-scale test data (20M+ records) 🆕
- [x] Refactor PerformanceMetrics to rdbms package
- [x] Complete Pharmacy implementation with CRUD operations 🆕
- [ ] Set up CI/CD pipeline
- [ ] Implement authentication service

#### Phase 2: Core Services (Months 3-4)
- [ ] Implement Member Service
- [ ] Implement Plan Service
- [ ] Implement Formulary Service
- [ ] Implement Pharmacy Service
- [ ] Set up caching layer

#### Phase 3: Claims Processing (Months 5-6)
- [ ] Implement Eligibility Service
- [ ] Implement Pricing Service
- [ ] Implement DUR Service
- [ ] Implement Claims Adjudication Service
- [ ] Integration testing

#### Phase 4: Advanced Features (Months 7-8)
- [ ] Implement Prior Authorization Service
- [ ] Implement reporting capabilities
- [ ] Implement audit logging
- [ ] Performance optimization
- [ ] Security hardening

#### Phase 5: Standards & Integration (Months 9-10)
- [ ] NCPDP D.0 compliance layer
- [ ] External system integrations
- [ ] Load testing
- [ ] User acceptance testing
- [ ] Documentation completion







## Apache Ignite vs PostgreSQL Performance Comparison

This project also includes performance benchmarking between Apache Ignite (in-memory data grid) and PostgreSQL for PBM workloads. See the [IgniteVSPostgres](./IgniteVSPostgres/) directory for implementation details.

#### Use Cases for Each Technology

**Apache Ignite**:
- Real-time claims adjudication (sub-100ms response)
- High-frequency eligibility checks
- Formulary lookups during claim processing
- Session state management
- Distributed computing for analytics

**PostgreSQL**:
- Persistent storage for all entities
- Complex reporting queries
- Historical data analysis
- Audit trail storage
- Batch processing jobs

**Hybrid Approach** (Recommended):
- PostgreSQL as system of record
- Ignite for hot data caching and real-time processing
- Write-through cache pattern for consistency
- Periodic cache refresh from PostgreSQL


## Appendix

### Glossary

- **AWP**: Average Wholesale Price - benchmark drug pricing
- **MAC**: Maximum Allowable Cost - generic drug pricing limit
- **NDC**: National Drug Code - unique drug identifier
- **NCPDP**: National Council for Prescription Drug Programs - standards organization
- **DUR**: Drug Utilization Review - clinical safety checks
- **PBM**: Pharmacy Benefit Manager - manages prescription drug benefits
- **PHI**: Protected Health Information - HIPAA-protected data
- **Prior Authorization**: Pre-approval required for certain medications
- **Formulary**: List of covered drugs with tier assignments
- **Adjudication**: Process of evaluating and pricing a claim

### References

- NCPDP Standards: https://www.ncpdp.org/
- HIPAA Regulations: https://www.hhs.gov/hipaa/
- FDA NDC Directory: https://www.fda.gov/drugs/drug-approvals-and-databases/national-drug-code-directory

