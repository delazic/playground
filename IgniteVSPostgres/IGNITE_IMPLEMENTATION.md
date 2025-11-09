# Apache Ignite Implementation Guide

## Overview

This document describes the Apache Ignite in-memory database implementation for the PBM (Pharmacy Benefit Management) system. Ignite provides a distributed, in-memory data grid with SQL capabilities, offering an alternative to PostgreSQL for high-performance data processing.

## Architecture

### Components

1. **IgniteConnector** - Manages Ignite cluster connection and lifecycle
2. **Model Classes** - Ignite-compatible POJOs with `@QuerySqlField` annotations
3. **DAO Classes** - Data Access Objects using Ignite SQL queries
4. **IgniteApp** - Demo application showcasing CRUD operations

### Key Features

- **In-Memory Storage**: All data stored in RAM for ultra-fast access
- **SQL Support**: Full SQL query capabilities via Ignite SQL engine
- **Persistence**: Optional disk-based persistence for durability
- **Distributed**: Can scale across multiple nodes
- **ACID Transactions**: Full transactional support

## Project Structure

```
IgniteVSPostgres/
├── src/main/java/dejanlazic/playground/inmemory/
│   ├── ignite/
│   │   ├── IgniteConnector.java          # Cluster connection manager
│   │   ├── IgniteApp.java                # Demo application
│   │   ├── model/
│   │   │   └── Member.java               # Ignite-compatible Member entity
│   │   └── dao/
│   │       └── MemberDAO.java            # Member data access object
│   └── rdbms/                            # Original PostgreSQL implementation
├── ignite/
│   └── config/
│       └── ignite-config.xml             # Ignite XML configuration
└── pom.xml                               # Maven dependencies
```

## Dependencies

The following dependencies have been added to `pom.xml`:

```xml
<!-- Apache Ignite Core -->
<dependency>
    <groupId>org.apache.ignite</groupId>
    <artifactId>ignite-core</artifactId>
    <version>2.16.0</version>
</dependency>

<!-- Apache Ignite Spring (for XML configuration) -->
<dependency>
    <groupId>org.apache.ignite</groupId>
    <artifactId>ignite-spring</artifactId>
    <version>2.16.0</version>
</dependency>

<!-- Apache Ignite Indexing (for SQL queries) -->
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

## Model Classes

Ignite model classes use `@QuerySqlField` annotations to enable SQL indexing:

```java
public class Member implements Serializable {
    @QuerySqlField(index = true)
    private Long memberId;
    
    @QuerySqlField(index = true)
    private String memberNumber;
    
    @QuerySqlField
    private String firstName;
    
    // ... other fields
}
```

### Key Differences from PostgreSQL Models

1. **Serializable**: All models must implement `Serializable`
2. **Annotations**: Use `@QuerySqlField` instead of JPA annotations
3. **Types**: Use `java.sql.Date` and `java.sql.Timestamp` for date/time fields
4. **Simplicity**: No complex relationships or lazy loading

## DAO Implementation

Ignite DAOs use the cache API and SQL queries:

```java
public class MemberDAO {
    private IgniteCache<Long, Member> getCache() {
        return ignite.cache("SQL_PUBLIC_MEMBER");
    }
    
    public Optional<Member> findByMemberNumber(String memberNumber) {
        SqlQuery<Long, Member> query = new SqlQuery<>(Member.class, 
            "memberNumber = ?");
        query.setArgs(memberNumber);
        
        List<Cache.Entry<Long, Member>> results = cache.query(query).getAll();
        // ...
    }
}
```

### Supported Operations

- **insert()** - Insert single record
- **insertBatch()** - Bulk insert with progress logging
- **findById()** - Find by primary key
- **findByMemberNumber()** - Find using SQL query
- **findAll()** - Retrieve all records
- **update()** - Update existing record
- **delete()** - Delete by ID
- **count()** - Count total records using SQL
- **exists()** - Check record existence

## Configuration

### XML Configuration

The Ignite cluster is configured via `ignite/config/ignite-config.xml`:

- **Cluster Name**: pbm-ignite-cluster
- **Discovery**: TCP/IP with localhost
- **Persistence**: Enabled for durability
- **SQL Schema**: PUBLIC
- **Cache Mode**: PARTITIONED for scalability

### Programmatic Configuration

Alternatively, use programmatic configuration:

```java
IgniteConnector connector = new IgniteConnector(true);
```

## Running the Application

### Compile the Project

```bash
cd IgniteVSPostgres
mvn clean compile
```

### Run the Ignite Demo

```bash
mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.ignite.IgniteApp"
```

### Expected Output

```
============================================================
Apache Ignite In-Memory Database Demo
============================================================

Testing Ignite connectivity...
✓ Connection successful!
✓ Ignite Cluster: pbm-ignite-cluster, Nodes: 1, Active: true

============================================================
Running CRUD Operations
============================================================

============================================================
CREATE - Inserting Sample Members
============================================================

✅ Inserted 10 members
📊 Total members in cache: 10

============================================================
READ - Querying Members
============================================================

📖 Found 10 members
...
```

## Performance Considerations

### Advantages

1. **Speed**: In-memory operations are 10-100x faster than disk-based databases
2. **Scalability**: Can distribute data across multiple nodes
3. **SQL Support**: Familiar SQL syntax for queries
4. **No Network Overhead**: Direct memory access

### Limitations

1. **Memory Usage**: All data must fit in RAM
2. **Persistence Overhead**: Writing to disk adds latency
3. **Complexity**: Distributed systems are harder to debug
4. **Cost**: Requires more RAM than traditional databases

## Migration from PostgreSQL

### Steps to Migrate

1. **Create Ignite Models**: Add `@QuerySqlField` annotations
2. **Update DAOs**: Replace JDBC with Ignite cache API
3. **Configure Cluster**: Set up `ignite-config.xml`
4. **Test Thoroughly**: Verify all operations work correctly
5. **Monitor Performance**: Track memory usage and query times

### Compatibility Notes

- **UUID vs Long**: Ignite uses Long for primary keys (PostgreSQL uses UUID)
- **Enums**: Store as Strings in Ignite
- **Timestamps**: Use `java.sql.Timestamp` instead of `LocalDateTime`
- **Relationships**: Implement manually (no JPA-style relationships)

## Troubleshooting

### Common Issues

1. **OutOfMemoryError**: Increase JVM heap size with `-Xmx4g`
2. **Cache Not Found**: Ensure cache is defined in XML config
3. **Query Errors**: Check field names match model class
4. **Connection Timeout**: Verify network settings and firewall

### Debug Mode

Enable Ignite debug logging:

```java
System.setProperty("IGNITE_QUIET", "false");
```

## Next Steps

1. **Implement All Models**: Create Ignite versions of all entities
2. **Migrate DAOs**: Convert all PostgreSQL DAOs to Ignite
3. **Performance Testing**: Benchmark against PostgreSQL
4. **Production Config**: Tune for production workloads
5. **Monitoring**: Set up metrics and alerting

## Resources

- [Apache Ignite Documentation](https://ignite.apache.org/docs/latest/)
- [Ignite SQL Reference](https://ignite.apache.org/docs/latest/SQL/sql-introduction)
- [Performance Tuning Guide](https://ignite.apache.org/docs/latest/perf-and-troubleshooting/general-perf-tips)

## Conclusion

Apache Ignite provides a powerful in-memory database solution for high-performance applications. This implementation demonstrates the core concepts and provides a foundation for migrating the entire PBM system from PostgreSQL to Ignite.