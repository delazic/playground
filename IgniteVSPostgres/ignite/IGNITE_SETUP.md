# Apache Ignite Setup Guide

## Overview

This project now includes Apache Ignite as an in-memory data grid for performance comparison with PostgreSQL. Ignite runs in Docker alongside PostgreSQL, allowing you to test and compare both systems.

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   PostgreSQL    │     │  Apache Ignite  │     │    pgAdmin      │
│   Port: 5432    │     │  Port: 10800    │     │   Port: 5050    │
│                 │     │  REST: 8080     │     │                 │
└─────────────────┘     └─────────────────┘     └─────────────────┘
         │                       │                       │
         └───────────────────────┴───────────────────────┘
                            pbm-network
```

## Quick Start

### 1. Start All Services

```bash
cd IgniteVSPostgres
docker-compose up -d
```

This starts:
- PostgreSQL (port 5432)
- Apache Ignite (ports 10800, 8080, 47100, 47500)
- pgAdmin (port 5050)

### 2. Verify Ignite is Running

Check container status:
```bash
docker ps | grep pbm-ignite
```

Check Ignite logs:
```bash
docker logs pbm-ignite
```

Test REST API:
```bash
curl http://localhost:8080/ignite?cmd=version
```

### 3. Stop Services

```bash
docker-compose down
```

To remove volumes (data):
```bash
docker-compose down -v
```

## Ignite Ports

| Port  | Purpose                    | Description                          |
|-------|----------------------------|--------------------------------------|
| 10800 | Thin Client                | Java/JDBC thin client connections    |
| 8080  | REST API                   | HTTP REST interface                  |
| 47100 | Communication SPI          | Node-to-node communication           |
| 47500 | Discovery SPI              | Cluster discovery                    |
| 11211 | Memcached (optional)       | Memcached protocol support           |

## Configuration

### Main Configuration File
Location: `ignite/config/ignite-config.xml`

Key settings:
- **Memory**: 1GB initial, 2GB max
- **Persistence**: Enabled (data survives restarts)
- **Cache Mode**: Partitioned for large datasets, Replicated for reference data
- **Backups**: 0 (single node setup)

### Pre-configured Caches

| Cache Name           | Mode        | Use Case                    |
|---------------------|-------------|------------------------------|
| MembersCache        | PARTITIONED | Member records               |
| EnrollmentsCache    | PARTITIONED | Enrollment data              |
| DrugsCache          | REPLICATED  | Drug catalog (reference)     |
| FormulariesCache    | REPLICATED  | Formulary definitions        |
| FormularyDrugsCache | PARTITIONED | Formulary-drug relationships |
| PharmaciesCache     | REPLICATED  | Pharmacy directory           |
| ClaimsCache         | PARTITIONED | Claims transactions          |

## Connecting to Ignite

### From Java Application

Add dependency to `pom.xml`:
```xml
<dependency>
    <groupId>org.apache.ignite</groupId>
    <artifactId>ignite-core</artifactId>
    <version>2.16.0</version>
</dependency>
<dependency>
    <groupId>org.apache.ignite</groupId>
    <artifactId>ignite-spring</artifactId>
    <version>2.16.0</version>
</dependency>
```

Connect using thin client:
```java
ClientConfiguration cfg = new ClientConfiguration()
    .setAddresses("localhost:10800");

try (IgniteClient client = Ignition.startClient(cfg)) {
    ClientCache<Long, Member> cache = client.cache("MembersCache");
    // Use cache...
}
```

### Using REST API

Get cache size:
```bash
curl "http://localhost:8080/ignite?cmd=size&cacheName=MembersCache"
```

Put data:
```bash
curl "http://localhost:8080/ignite?cmd=put&key=1&val=test&cacheName=MembersCache"
```

Get data:
```bash
curl "http://localhost:8080/ignite?cmd=get&key=1&cacheName=MembersCache"
```

### Using SQL

Ignite supports JDBC and ODBC:
```java
String url = "jdbc:ignite:thin://localhost:10800";
Connection conn = DriverManager.getConnection(url);
```

## Performance Testing

### Load Data into Ignite

You can load data from PostgreSQL into Ignite for comparison:

```java
// Read from PostgreSQL
List<Member> members = postgresDAO.getAllMembers();

// Write to Ignite
ClientCache<Long, Member> cache = igniteClient.cache("MembersCache");
members.forEach(m -> cache.put(m.getMemberId(), m));
```

### Run Benchmarks

Compare query performance:
```java
// PostgreSQL query
long pgStart = System.currentTimeMillis();
List<Claim> pgClaims = postgresDAO.getClaimsByMember(memberId);
long pgTime = System.currentTimeMillis() - pgStart;

// Ignite query
long igniteStart = System.currentTimeMillis();
List<Claim> igniteClaims = igniteCache.query(
    new SqlQuery<>(Claim.class, "memberId = ?")
        .setArgs(memberId)
).getAll();
long igniteTime = System.currentTimeMillis() - igniteStart;

System.out.printf("PostgreSQL: %dms, Ignite: %dms%n", pgTime, igniteTime);
```

## Monitoring

### Check Cluster State
```bash
curl http://localhost:8080/ignite?cmd=top
```

### View Cache Statistics
```bash
curl "http://localhost:8080/ignite?cmd=cache&cacheName=MembersCache"
```

### Access Logs
```bash
docker logs -f pbm-ignite
```

## Troubleshooting

### Container Won't Start

Check logs:
```bash
docker logs pbm-ignite
```

Common issues:
- Port conflicts (10800, 8080 already in use)
- Insufficient memory (increase Docker memory limit)
- Configuration file errors

### Out of Memory

Increase JVM heap in `docker-compose.yml`:
```yaml
environment:
  JVM_OPTS: "-Xms2g -Xmx4g -server -XX:+UseG1GC"
```

### Persistence Issues

Clear persistence data:
```bash
docker-compose down -v
docker volume rm ignitevspostgres_ignite_data
docker-compose up -d
```

## Advanced Configuration

### Multi-Node Cluster

To add more Ignite nodes, update `docker-compose.yml`:

```yaml
ignite-node-2:
  image: apacheignite/ignite:2.16.0
  container_name: pbm-ignite-2
  environment:
    IGNITE_QUIET: "false"
  volumes:
    - ./ignite/config:/config
  networks:
    - pbm-network
  command: /opt/ignite/apache-ignite/bin/ignite.sh /config/ignite-config.xml
```

Update discovery in `ignite-config.xml`:
```xml
<property name="addresses">
    <list>
        <value>pbm-ignite:47500..47509</value>
        <value>pbm-ignite-2:47500..47509</value>
    </list>
</property>
```

### Enable Backups

For data redundancy, set backups in cache configuration:
```xml
<property name="backups" value="1"/>
```

### Tune Performance

Adjust memory regions:
```xml
<property name="initialSize" value="#{2L * 1024 * 1024 * 1024}"/>
<property name="maxSize" value="#{4L * 1024 * 1024 * 1024}"/>
```

## Resources

- [Apache Ignite Documentation](https://ignite.apache.org/docs/latest/)
- [Ignite Docker Hub](https://hub.docker.com/r/apacheignite/ignite)
- [Performance Tuning Guide](https://ignite.apache.org/docs/latest/perf-and-troubleshooting/general-perf-tips)
- [SQL Reference](https://ignite.apache.org/docs/latest/SQL/sql-introduction)

## Next Steps

1. **Load test data** into Ignite caches
2. **Run performance benchmarks** comparing PostgreSQL vs Ignite
3. **Implement caching strategies** (write-through, read-through)
4. **Test distributed queries** across partitioned data
5. **Monitor memory usage** and optimize cache configurations

## Comparison: PostgreSQL vs Ignite

| Feature              | PostgreSQL          | Apache Ignite        |
|---------------------|---------------------|----------------------|
| Storage             | Disk-based          | Memory-first         |
| Query Speed         | Good (with indexes) | Excellent (in-memory)|
| Scalability         | Vertical            | Horizontal           |
| ACID Transactions   | Full support        | Full support         |
| SQL Support         | Complete            | Subset               |
| Persistence         | Native              | Optional             |
| Best For            | Complex queries     | High-speed reads     |

Use this setup to determine which system best fits your pharmacy benefits management workload!