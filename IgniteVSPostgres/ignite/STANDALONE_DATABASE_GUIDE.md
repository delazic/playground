# Using Apache Ignite as a Standalone Database

This guide explains how to use Apache Ignite as a **complete database replacement** for PostgreSQL, not just as a cache layer.

## 🎯 Overview

Apache Ignite is configured as a **distributed SQL database** with:
- ✅ **Full ACID transactions**
- ✅ **SQL support** (ANSI-99 subset)
- ✅ **Persistent storage** (survives restarts)
- ✅ **JDBC/ODBC connectivity**
- ✅ **Distributed queries**
- ✅ **Indexes and constraints**

## 🚀 Quick Start

### 1. Start Ignite

```bash
cd IgniteVSPostgres
docker-compose up -d ignite
```

### 2. Connect via JDBC

**Connection String:**
```
jdbc:ignite:thin://localhost:10800
```

**Java Example:**
```java
import java.sql.*;

public class IgniteConnection {
    public static void main(String[] args) throws SQLException {
        String url = "jdbc:ignite:thin://localhost:10800";
        
        try (Connection conn = DriverManager.getConnection(url)) {
            System.out.println("Connected to Ignite!");
            
            // Create table
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(
                    "CREATE TABLE IF NOT EXISTS MEMBER (" +
                    "  MEMBER_ID BIGINT PRIMARY KEY," +
                    "  FIRST_NAME VARCHAR(100)," +
                    "  LAST_NAME VARCHAR(100)," +
                    "  EMAIL VARCHAR(100)" +
                    ") WITH \"template=partitioned, backups=0\""
                );
            }
            
            // Insert data
            try (PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO MEMBER (MEMBER_ID, FIRST_NAME, LAST_NAME, EMAIL) VALUES (?, ?, ?, ?)")) {
                pstmt.setLong(1, 1L);
                pstmt.setString(2, "John");
                pstmt.setString(3, "Doe");
                pstmt.setString(4, "john.doe@example.com");
                pstmt.executeUpdate();
            }
            
            // Query data
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM MEMBER")) {
                while (rs.next()) {
                    System.out.printf("%d: %s %s (%s)%n",
                        rs.getLong("MEMBER_ID"),
                        rs.getString("FIRST_NAME"),
                        rs.getString("LAST_NAME"),
                        rs.getString("EMAIL")
                    );
                }
            }
        }
    }
}
```

### 3. Connect via sqlline (CLI)

```bash
# Enter Ignite container
docker exec -it pbm-ignite bash

# Start sqlline
cd /opt/ignite/apache-ignite/bin
./sqlline.sh -u "jdbc:ignite:thin://127.0.0.1:10800"

# Run SQL commands
!tables
SELECT * FROM MEMBER;
!quit
```

## 📊 Database Schema

### Initialize Tables

**Option 1: Run SQL script**
```bash
# Copy SQL script to container
docker cp ignite/init/01-create-tables.sql pbm-ignite:/tmp/

# Execute via sqlline
docker exec -it pbm-ignite bash -c "cd /opt/ignite/apache-ignite/bin && ./sqlline.sh -u 'jdbc:ignite:thin://127.0.0.1:10800' -f /tmp/01-create-tables.sql"
```

**Option 2: Use JDBC from Java**
```java
try (Connection conn = DriverManager.getConnection("jdbc:ignite:thin://localhost:10800");
     Statement stmt = conn.createStatement()) {
    
    // Read and execute SQL file
    String sql = Files.readString(Path.of("ignite/init/01-create-tables.sql"));
    for (String statement : sql.split(";")) {
        if (!statement.trim().isEmpty()) {
            stmt.execute(statement);
        }
    }
}
```

### Available Tables

All PBM tables are created with proper schemas:

| Table | Type | Description |
|-------|------|-------------|
| MEMBER | Partitioned | Member demographics |
| PLAN | Replicated | Benefit plans |
| DRUG | Replicated | Drug catalog |
| PHARMACY | Replicated | Pharmacy directory |
| ENROLLMENT | Partitioned | Member enrollments |
| FORMULARY | Replicated | Formulary definitions |
| FORMULARY_DRUG | Partitioned | Drug coverage rules |
| PHARMACY_NETWORK | Partitioned | Network assignments |
| CLAIM | Partitioned | Claims transactions |

## 💻 Java DAO Implementation

### Add Dependencies

```xml
<dependencies>
    <!-- Ignite Core -->
    <dependency>
        <groupId>org.apache.ignite</groupId>
        <artifactId>ignite-core</artifactId>
        <version>2.16.0</version>
    </dependency>
    
    <!-- Ignite JDBC Driver -->
    <dependency>
        <groupId>org.apache.ignite</groupId>
        <artifactId>ignite-core</artifactId>
        <version>2.16.0</version>
    </dependency>
    
    <!-- Optional: Ignite Spring -->
    <dependency>
        <groupId>org.apache.ignite</groupId>
        <artifactId>ignite-spring</artifactId>
        <version>2.16.0</version>
    </dependency>
</dependencies>
```

### Connection Manager

```java
package dejanlazic.playground.inmemory.ignite;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class IgniteConnectionManager {
    private static final String JDBC_URL = "jdbc:ignite:thin://localhost:10800";
    
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL);
    }
    
    public static void testConnection() {
        try (Connection conn = getConnection()) {
            System.out.println("✅ Connected to Ignite successfully!");
            System.out.println("Database: " + conn.getMetaData().getDatabaseProductName());
            System.out.println("Version: " + conn.getMetaData().getDatabaseProductVersion());
        } catch (SQLException e) {
            System.err.println("❌ Failed to connect to Ignite: " + e.getMessage());
        }
    }
}
```

### Example DAO

```java
package dejanlazic.playground.inmemory.ignite.dao;

import dejanlazic.playground.inmemory.rdbms.model.Member;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class IgniteMemberDAO {
    private final String jdbcUrl;
    
    public IgniteMemberDAO(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }
    
    public void insert(Member member) throws SQLException {
        String sql = "INSERT INTO MEMBER (MEMBER_ID, MEMBER_NUMBER, FIRST_NAME, LAST_NAME, " +
                    "DATE_OF_BIRTH, GENDER, ADDRESS, CITY, STATE, ZIP_CODE, PHONE, EMAIL) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, member.getMemberId());
            pstmt.setString(2, member.getMemberNumber());
            pstmt.setString(3, member.getFirstName());
            pstmt.setString(4, member.getLastName());
            pstmt.setDate(5, member.getDateOfBirth());
            pstmt.setString(6, member.getGender());
            pstmt.setString(7, member.getAddress());
            pstmt.setString(8, member.getCity());
            pstmt.setString(9, member.getState());
            pstmt.setString(10, member.getZipCode());
            pstmt.setString(11, member.getPhone());
            pstmt.setString(12, member.getEmail());
            
            pstmt.executeUpdate();
        }
    }
    
    public Member findById(Long memberId) throws SQLException {
        String sql = "SELECT * FROM MEMBER WHERE MEMBER_ID = ?";
        
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, memberId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMember(rs);
                }
            }
        }
        return null;
    }
    
    public List<Member> findAll() throws SQLException {
        String sql = "SELECT * FROM MEMBER";
        List<Member> members = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                members.add(mapResultSetToMember(rs));
            }
        }
        return members;
    }
    
    public void update(Member member) throws SQLException {
        String sql = "UPDATE MEMBER SET FIRST_NAME = ?, LAST_NAME = ?, EMAIL = ?, " +
                    "PHONE = ?, ADDRESS = ?, CITY = ?, STATE = ?, ZIP_CODE = ? " +
                    "WHERE MEMBER_ID = ?";
        
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, member.getFirstName());
            pstmt.setString(2, member.getLastName());
            pstmt.setString(3, member.getEmail());
            pstmt.setString(4, member.getPhone());
            pstmt.setString(5, member.getAddress());
            pstmt.setString(6, member.getCity());
            pstmt.setString(7, member.getState());
            pstmt.setString(8, member.getZipCode());
            pstmt.setLong(9, member.getMemberId());
            
            pstmt.executeUpdate();
        }
    }
    
    public void delete(Long memberId) throws SQLException {
        String sql = "DELETE FROM MEMBER WHERE MEMBER_ID = ?";
        
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setLong(1, memberId);
            pstmt.executeUpdate();
        }
    }
    
    private Member mapResultSetToMember(ResultSet rs) throws SQLException {
        Member member = new Member();
        member.setMemberId(rs.getLong("MEMBER_ID"));
        member.setMemberNumber(rs.getString("MEMBER_NUMBER"));
        member.setFirstName(rs.getString("FIRST_NAME"));
        member.setLastName(rs.getString("LAST_NAME"));
        member.setDateOfBirth(rs.getDate("DATE_OF_BIRTH"));
        member.setGender(rs.getString("GENDER"));
        member.setAddress(rs.getString("ADDRESS"));
        member.setCity(rs.getString("CITY"));
        member.setState(rs.getString("STATE"));
        member.setZipCode(rs.getString("ZIP_CODE"));
        member.setPhone(rs.getString("PHONE"));
        member.setEmail(rs.getString("EMAIL"));
        return member;
    }
}
```

## 🔄 Data Migration from PostgreSQL

### Export from PostgreSQL

```bash
# Export members to CSV
psql -h localhost -U pbm_user -d pbm_db -c "\COPY member TO '/tmp/members.csv' CSV HEADER"
```

### Import to Ignite

```java
public class DataMigration {
    public static void migrateMembers() throws Exception {
        String pgUrl = "jdbc:postgresql://localhost:5432/pbm_db";
        String igniteUrl = "jdbc:ignite:thin://localhost:10800";
        
        try (Connection pgConn = DriverManager.getConnection(pgUrl, "pbm_user", "pbm_password");
             Connection igniteConn = DriverManager.getConnection(igniteUrl)) {
            
            // Read from PostgreSQL
            String selectSql = "SELECT * FROM member";
            try (Statement stmt = pgConn.createStatement();
                 ResultSet rs = stmt.executeQuery(selectSql)) {
                
                // Prepare insert for Ignite
                String insertSql = "INSERT INTO MEMBER (MEMBER_ID, MEMBER_NUMBER, FIRST_NAME, " +
                                  "LAST_NAME, DATE_OF_BIRTH, GENDER, ADDRESS, CITY, STATE, " +
                                  "ZIP_CODE, PHONE, EMAIL) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                
                try (PreparedStatement pstmt = igniteConn.prepareStatement(insertSql)) {
                    int count = 0;
                    while (rs.next()) {
                        pstmt.setLong(1, rs.getLong("member_id"));
                        pstmt.setString(2, rs.getString("member_number"));
                        pstmt.setString(3, rs.getString("first_name"));
                        pstmt.setString(4, rs.getString("last_name"));
                        pstmt.setDate(5, rs.getDate("date_of_birth"));
                        pstmt.setString(6, rs.getString("gender"));
                        pstmt.setString(7, rs.getString("address"));
                        pstmt.setString(8, rs.getString("city"));
                        pstmt.setString(9, rs.getString("state"));
                        pstmt.setString(10, rs.getString("zip_code"));
                        pstmt.setString(11, rs.getString("phone"));
                        pstmt.setString(12, rs.getString("email"));
                        pstmt.addBatch();
                        
                        if (++count % 1000 == 0) {
                            pstmt.executeBatch();
                            System.out.println("Migrated " + count + " members");
                        }
                    }
                    pstmt.executeBatch(); // Final batch
                    System.out.println("Migration complete: " + count + " members");
                }
            }
        }
    }
}
```

## 📈 Performance Comparison

### Benchmark Setup

```java
public class PerformanceBenchmark {
    public static void main(String[] args) throws Exception {
        String pgUrl = "jdbc:postgresql://localhost:5432/pbm_db";
        String igniteUrl = "jdbc:ignite:thin://localhost:10800";
        
        // Test 1: Single row lookup
        benchmarkSingleLookup(pgUrl, "PostgreSQL");
        benchmarkSingleLookup(igniteUrl, "Ignite");
        
        // Test 2: Range query
        benchmarkRangeQuery(pgUrl, "PostgreSQL");
        benchmarkRangeQuery(igniteUrl, "Ignite");
        
        // Test 3: Aggregation
        benchmarkAggregation(pgUrl, "PostgreSQL");
        benchmarkAggregation(igniteUrl, "Ignite");
    }
    
    private static void benchmarkSingleLookup(String url, String dbName) throws Exception {
        try (Connection conn = DriverManager.getConnection(url)) {
            String sql = "SELECT * FROM MEMBER WHERE MEMBER_ID = ?";
            
            long start = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setLong(1, i + 1);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        rs.next();
                    }
                }
            }
            long duration = System.nanoTime() - start;
            
            System.out.printf("%s - Single Lookup (1000 queries): %.2f ms (%.2f μs/query)%n",
                dbName, duration / 1_000_000.0, duration / 1_000.0);
        }
    }
    
    private static void benchmarkRangeQuery(String url, String dbName) throws Exception {
        try (Connection conn = DriverManager.getConnection(url)) {
            String sql = "SELECT * FROM MEMBER WHERE MEMBER_ID BETWEEN ? AND ?";
            
            long start = System.nanoTime();
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1, 1);
                pstmt.setLong(2, 10000);
                try (ResultSet rs = pstmt.executeQuery()) {
                    int count = 0;
                    while (rs.next()) count++;
                }
            }
            long duration = System.nanoTime() - start;
            
            System.out.printf("%s - Range Query (10K rows): %.2f ms%n",
                dbName, duration / 1_000_000.0);
        }
    }
    
    private static void benchmarkAggregation(String url, String dbName) throws Exception {
        try (Connection conn = DriverManager.getConnection(url)) {
            String sql = "SELECT STATE, COUNT(*) as cnt FROM MEMBER GROUP BY STATE";
            
            long start = System.nanoTime();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    // Process results
                }
            }
            long duration = System.nanoTime() - start;
            
            System.out.printf("%s - Aggregation Query: %.2f ms%n",
                dbName, duration / 1_000_000.0);
        }
    }
}
```

### Expected Results

| Operation | PostgreSQL | Ignite | Speedup |
|-----------|------------|--------|---------|
| Single Lookup | 50-100 μs | 10-20 μs | 3-5x faster |
| Range Query (10K) | 50-100 ms | 10-30 ms | 2-5x faster |
| Aggregation | 100-200 ms | 20-50 ms | 3-5x faster |
| Insert (batch 1K) | 200-300 ms | 50-100 ms | 2-4x faster |

## 🎯 Best Practices

### 1. Use Appropriate Cache Modes

- **REPLICATED**: Small reference tables (drugs, pharmacies, plans)
- **PARTITIONED**: Large transactional tables (members, claims, enrollments)

### 2. Enable Persistence

Always enable persistence for production:
```xml
<property name="persistenceEnabled" value="true"/>
```

### 3. Use Batch Operations

```java
try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
    for (Member member : members) {
        // Set parameters
        pstmt.addBatch();
        
        if (count++ % 1000 == 0) {
            pstmt.executeBatch();
        }
    }
    pstmt.executeBatch(); // Final batch
}
```

### 4. Create Proper Indexes

```sql
CREATE INDEX IDX_MEMBER_NAME ON MEMBER(LAST_NAME, FIRST_NAME);
CREATE INDEX IDX_CLAIM_MEMBER_DATE ON CLAIM(MEMBER_ID, SERVICE_DATE DESC);
```

### 5. Use Transactions

```java
conn.setAutoCommit(false);
try {
    // Multiple operations
    conn.commit();
} catch (Exception e) {
    conn.rollback();
    throw e;
}
```

## 🔍 Monitoring & Management

### View System Tables

```sql
-- List all tables
SELECT * FROM SYS.TABLES WHERE SCHEMA_NAME = 'PUBLIC';

-- View table statistics
SELECT TABLE_NAME, CACHE_NAME, KEY_TYPE, VALUE_TYPE 
FROM SYS.TABLES 
WHERE SCHEMA_NAME = 'PUBLIC';

-- Check indexes
SELECT * FROM SYS.INDEXES WHERE SCHEMA_NAME = 'PUBLIC';
```

### REST API Monitoring

```bash
# Get cluster topology
curl http://localhost:8080/ignite?cmd=top

# Get cache metrics
curl "http://localhost:8080/ignite?cmd=cache&cacheName=SQL_PUBLIC_MEMBER"

# Execute SQL via REST
curl "http://localhost:8080/ignite?cmd=qryfldexe&pageSize=10&cacheName=SQL_PUBLIC_MEMBER&qry=SELECT+COUNT(*)+FROM+MEMBER"
```

## 📚 Resources

- [Ignite SQL Reference](https://ignite.apache.org/docs/latest/SQL/sql-introduction)
- [JDBC Driver](https://ignite.apache.org/docs/latest/SQL/JDBC/jdbc-driver)
- [Data Modeling](https://ignite.apache.org/docs/latest/data-modeling/data-modeling)
- [Performance Tuning](https://ignite.apache.org/docs/latest/perf-and-troubleshooting/general-perf-tips)

## ✅ Next Steps

1. ✅ Start Ignite container
2. ✅ Initialize database schema
3. ✅ Migrate data from PostgreSQL
4. ✅ Implement DAO layer
5. ✅ Run performance benchmarks
6. ✅ Compare results with PostgreSQL

You now have Apache Ignite running as a **full-featured distributed SQL database**! 🎉