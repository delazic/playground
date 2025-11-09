# Apache Ignite Quick Start Guide

## What Has Been Created

This implementation provides a complete Apache Ignite in-memory database solution as an alternative to PostgreSQL.

### Files Created

1. **IgniteConnector.java** - Manages Ignite cluster connection
   - Location: `src/main/java/dejanlazic/playground/inmemory/ignite/IgniteConnector.java`
   - Handles cluster initialization, activation, and shutdown

2. **Member.java** (Ignite Model) - Ignite-compatible Member entity
   - Location: `src/main/java/dejanlazic/playground/inmemory/ignite/model/Member.java`
   - Uses `@QuerySqlField` annotations for SQL indexing

3. **MemberDAO.java** (Ignite DAO) - Data access layer for Members
   - Location: `src/main/java/dejanlazic/playground/inmemory/ignite/dao/MemberDAO.java`
   - Implements CRUD operations using Ignite cache API

4. **IgniteApp.java** - Demo application
   - Location: `src/main/java/dejanlazic/playground/inmemory/ignite/IgniteApp.java`
   - Demonstrates all CRUD operations

5. **IGNITE_IMPLEMENTATION.md** - Comprehensive documentation
   - Complete guide to the Ignite implementation

### Dependencies Added to pom.xml

```xml
<!-- Apache Ignite Core -->
<dependency>
    <groupId>org.apache.ignite</groupId>
    <artifactId>ignite-core</artifactId>
    <version>2.16.0</version>
</dependency>

<!-- Apache Ignite Spring -->
<dependency>
    <groupId>org.apache.ignite</groupId>
    <artifactId>ignite-spring</artifactId>
    <version>2.16.0</version>
</dependency>

<!-- Apache Ignite Indexing -->
<dependency>
    <groupId>org.apache.ignite</groupId>
    <artifactId>ignite-indexing</artifactId>
    <version>2.16.0</version>
</dependency>

<!-- JCache API -->
<dependency>
    <groupId>javax.cache</groupId>
    <artifactId>cache-api</artifactId>
    <version>1.1.1</version>
</dependency>
```

## How to Run

### 1. Compile the Project

```bash
cd IgniteVSPostgres
mvn clean compile
```

### 2. Run the Ignite Demo

```bash
mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.ignite.IgniteApp"
```

### 3. Expected Output

The application will:
- ✅ Connect to Ignite cluster
- ✅ Insert 10 sample members
- ✅ Query and display members
- ✅ Update a member's information
- ✅ Delete a member
- ✅ Show final statistics

## Key Differences: PostgreSQL vs Ignite

| Feature | PostgreSQL | Apache Ignite |
|---------|-----------|---------------|
| Storage | Disk-based | In-memory (with optional persistence) |
| Speed | Moderate | Very fast (10-100x) |
| Primary Key | UUID | Long |
| Annotations | JPA (@Entity, @Table) | Ignite (@QuerySqlField) |
| Connection | JDBC | Cache API |
| Queries | SQL via JDBC | SQL via Ignite SQL engine |
| Relationships | JPA relationships | Manual implementation |

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                    IgniteApp                            │
│                 (Demo Application)                      │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                   MemberDAO                             │
│            (Data Access Object)                         │
│  - insert()  - findById()  - update()                   │
│  - insertBatch()  - findAll()  - delete()               │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                IgniteConnector                          │
│           (Cluster Connection Manager)                  │
│  - initialize()  - getIgnite()  - close()               │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│              Apache Ignite Cluster                      │
│                (In-Memory Database)                     │
│  - SQL_PUBLIC_MEMBER cache                              │
│  - SQL query engine                                     │
│  - Persistence layer (optional)                         │
└─────────────────────────────────────────────────────────┘
```

## Next Steps

### To Complete the Migration

1. **Create Remaining Models**
   - Drug.java
   - Pharmacy.java
   - BenefitPlan.java
   - Enrollment.java
   - Formulary.java
   - FormularyDrug.java
   - Claim.java
   - PharmacyNetwork.java

2. **Create Corresponding DAOs**
   - DrugDAO.java
   - PharmacyDAO.java
   - BenefitPlanDAO.java
   - etc.

3. **Update Configuration**
   - Ensure all caches are defined in `ignite-config.xml`
   - Configure appropriate indexes for each entity

4. **Performance Testing**
   - Benchmark against PostgreSQL
   - Optimize cache configurations
   - Tune memory settings

5. **Production Readiness**
   - Add error handling
   - Implement connection pooling
   - Set up monitoring and metrics
   - Configure backup and recovery

## Code Examples

### Creating a New Model

```java
public class Drug implements Serializable {
    @QuerySqlField(index = true)
    private Long drugId;
    
    @QuerySqlField(index = true)
    private String ndcCode;
    
    @QuerySqlField
    private String drugName;
    
    // ... getters and setters
}
```

### Creating a New DAO

```java
public class DrugDAO {
    private final IgniteConnector connector;
    private final Ignite ignite;
    private static final String CACHE_NAME = "SQL_PUBLIC_DRUG";
    
    public DrugDAO(IgniteConnector connector) {
        this.connector = connector;
        this.ignite = connector.getIgnite();
    }
    
    private IgniteCache<Long, Drug> getCache() {
        return ignite.cache(CACHE_NAME);
    }
    
    public Optional<Drug> findByNdcCode(String ndcCode) {
        IgniteCache<Long, Drug> cache = getCache();
        SqlQuery<Long, Drug> query = new SqlQuery<>(Drug.class, 
            "ndcCode = ?");
        query.setArgs(ndcCode);
        
        List<Cache.Entry<Long, Drug>> results = cache.query(query).getAll();
        return results.isEmpty() ? Optional.empty() : 
            Optional.of(results.get(0).getValue());
    }
}
```

## Troubleshooting

### Issue: OutOfMemoryError
**Solution**: Increase JVM heap size
```bash
export MAVEN_OPTS="-Xmx4g"
mvn exec:java -Dexec.mainClass="..."
```

### Issue: Cache not found
**Solution**: Verify cache is defined in `ignite-config.xml`

### Issue: Query returns no results
**Solution**: Check field names match model class exactly (case-sensitive)

## Resources

- **Full Documentation**: See `IGNITE_IMPLEMENTATION.md`
- **Ignite Docs**: https://ignite.apache.org/docs/latest/
- **SQL Reference**: https://ignite.apache.org/docs/latest/SQL/sql-introduction

## Summary

✅ **Completed:**
- Apache Ignite connector class
- Member model with SQL annotations
- Member DAO with CRUD operations
- Demo application
- Comprehensive documentation

⏳ **Remaining:**
- Create models for other entities (Drug, Pharmacy, etc.)
- Create DAOs for other entities
- Performance testing and optimization
- Production configuration

The foundation is complete and ready for expansion!