## Pure JDBC Database Connector

This implementation provides a simple, pure JDBC connection to PostgreSQL without any connection pooling or ORM frameworks.

### Architecture

- **Pure JDBC**: Uses only `java.sql` package and PostgreSQL JDBC driver
- **No Connection Pooling**: Each `getConnection()` call creates a new database connection
- **No ORM**: Direct SQL queries using JDBC Statement and PreparedStatement
- **Simple Configuration**: Properties file for database credentials

### Package Structure

```
dejanlazic.playground.inmemory.rdbms
├── DatabaseConnector.java    - Core connector class
└── App.java                   - Demo application
```

### Files

1. **DatabaseConnector.java** - Core connector class
   - Package: `dejanlazic.playground.inmemory.rdbms`
   - Loads database configuration from properties file
   - Provides `getConnection()` method to get new connections
   - Includes connection testing utilities

2. **database.properties** - Configuration file
   ```properties
   db.url=jdbc:postgresql://localhost:5432/pbm_db
   db.username=pbm_user
   db.password=pbm_password
   db.driver=org.postgresql.Driver
   ```

3. **App.java** - Demo application
   - Package: `dejanlazic.playground.inmemory.rdbms`
   - Tests database connectivity
   - Queries database metadata
   - Demonstrates basic SQL queries

4. **DatabaseConnectionTest.java** - JUnit 5 test class
   - Package: `dejanlazic.playground.inmemory`
   - Automated tests for database connectivity
   - Uses JUnit 5 annotations

### Usage

#### Basic Connection

```java
import dejanlazic.playground.inmemory.rdbms.DatabaseConnector;

DatabaseConnector connector = new DatabaseConnector();

try (Connection conn = connector.getConnection()) {
    // Use the connection
    // Connection is automatically closed by try-with-resources
}
```

#### Execute Query

```java
import dejanlazic.playground.inmemory.rdbms.DatabaseConnector;

DatabaseConnector connector = new DatabaseConnector();

String sql = "SELECT * FROM member WHERE member_number = ?";

try (Connection conn = connector.getConnection();
     PreparedStatement pstmt = conn.prepareStatement(sql)) {
    
    pstmt.setString(1, "M000001");
    
    try (ResultSet rs = pstmt.executeQuery()) {
        while (rs.next()) {
            String firstName = rs.getString("first_name");
            String lastName = rs.getString("last_name");
            System.out.println(firstName + " " + lastName);
        }
    }
}
```

#### Execute Update

```java
import dejanlazic.playground.inmemory.rdbms.DatabaseConnector;

DatabaseConnector connector = new DatabaseConnector();

String sql = "UPDATE member SET email = ? WHERE member_id = ?";

try (Connection conn = connector.getConnection();
     PreparedStatement pstmt = conn.prepareStatement(sql)) {
    
    pstmt.setString(1, "newemail@example.com");
    pstmt.setObject(2, UUID.fromString("..."));
    
    int rowsAffected = pstmt.executeUpdate();
    System.out.println("Updated " + rowsAffected + " rows");
}
```

#### Transaction Management

```java
import dejanlazic.playground.inmemory.rdbms.DatabaseConnector;

DatabaseConnector connector = new DatabaseConnector();

try (Connection conn = connector.getConnection()) {
    conn.setAutoCommit(false);
    
    try {
        // Execute multiple statements
        try (PreparedStatement pstmt1 = conn.prepareStatement("INSERT INTO ...")) {
            pstmt1.executeUpdate();
        }
        
        try (PreparedStatement pstmt2 = conn.prepareStatement("UPDATE ...")) {
            pstmt2.executeUpdate();
        }
        
        conn.commit();
    } catch (SQLException e) {
        conn.rollback();
        throw e;
    }
}
```

### Running the Application

#### Prerequisites

1. PostgreSQL database running (via Docker):
   ```bash
   cd IgniteVSPostgres
   docker-compose up -d postgres
   ```

2. Build the project:
   ```bash
   mvn clean compile
   ```

#### Run Application

**Option 1: Using the script (Recommended)**
```bash
cd IgniteVSPostgres
./run-app.sh
```

**Option 2: Using Maven directly**
```bash
mvn exec:java -Dexec.mainClass="dejanlazic.playground.inmemory.rdbms.App"
```

**Option 3: Run from IntelliJ IDEA**
- Right-click on `App.java`
- Select "Run 'App.main()'"

#### Run JUnit Tests

**Option 1: Using the script**
```bash
cd IgniteVSPostgres
./run-junit-tests.sh
```

**Option 2: Using Maven**
```bash
mvn test
```

**Option 3: Run from IntelliJ IDEA**
- Right-click on `DatabaseConnectionTest.java`
- Select "Run 'DatabaseConnectionTest'"

### Expected Output

```
============================================================
PBM Database Connection Test - Pure JDBC
============================================================

Test 1: Testing database connectivity...
✓ Connection successful!

Test 2: Retrieving database information...
✓ Database: PostgreSQL 16.1, Driver: PostgreSQL JDBC Driver 42.7.1

Test 3: Querying database tables...
Tables in database:
  1. claim
  2. claim_2024
  3. claim_2025
  4. claim_line
  5. drug
  ...
✓ Found 15 tables

Test 4: Querying sample member data...
Sample members:
Member #        First Name           Last Name            Gender  
-----------------------------------------------------------------
M000001         John                 Doe                  M       
M000002         Jane                 Smith                F       
...
✓ Query successful

Test 5: Counting records in key tables...
  member         : 5 records
  plan           : 4 records
  drug           : 13 records
  pharmacy       : 5 records
  claim          : 15 records
✓ Count queries successful

============================================================
All tests completed!
============================================================
```

### Important Notes

#### Connection Management

- **Always close connections**: Use try-with-resources or explicit `close()` calls
- **No connection pooling**: Each `getConnection()` creates a new connection
- **Resource cleanup**: Close ResultSet, Statement, and Connection in reverse order

#### Performance Considerations

- Creating new connections is expensive (network overhead, authentication)
- For production use, consider adding connection pooling (HikariCP, Apache DBCP)
- Use PreparedStatement for repeated queries (better performance and security)

#### Security

- Never hardcode credentials in source code
- Use environment variables or secure configuration management
- Always use PreparedStatement to prevent SQL injection
- Close connections to prevent resource leaks

### Troubleshooting

#### Connection Refused

```
Error: Connection refused
```

**Solution**: Ensure PostgreSQL is running
```bash
docker-compose ps postgres
docker-compose up -d postgres
```

#### Authentication Failed

```
Error: password authentication failed
```

**Solution**: Check credentials in `database.properties`

#### Driver Not Found

```
Error: org.postgresql.Driver not found
```

**Solution**: Ensure PostgreSQL JDBC driver is in classpath
```bash
mvn clean compile
```

#### ClassNotFoundException when running from IntelliJ

**Solution**: The Maven dependencies need to be downloaded
```bash
mvn clean install
```

Then refresh the project in IntelliJ (File → Reload All from Disk)

### Project Structure

```
IgniteVSPostgres/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── dejanlazic/playground/inmemory/rdbms/
│   │   │       ├── DatabaseConnector.java
│   │   │       └── App.java
│   │   └── resources/
│   │       └── database.properties
│   └── test/
│       └── java/
│           └── dejanlazic/playground/inmemory/
│               └── DatabaseConnectionTest.java
├── run-app.sh               # Script to run App
├── run-junit-tests.sh       # Script to run JUnit tests
└── pom.xml                  # Maven configuration
```

### Next Steps

1. Implement Data Access Objects (DAOs) for each entity
2. Create repository pattern for database operations
3. Add proper error handling and logging
4. Implement connection pooling for production use
5. Add integration tests for database operations