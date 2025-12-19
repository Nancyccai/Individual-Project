# TPC-H Query 10 AJU Implementation - Complete Setup and Running Guide

## Table of Contents
1. [Project Overview](#project-overview)
2. [System Architecture](#system-architecture)
3. [Query 10 Overview](#query-10-overview)
4. [AJU Algorithm Core Concepts](#aju-algorithm-core-concepts)
5. [Quick Start](#quick-start)
6. [Detailed Setup Guide](#detailed-setup-guide)
7. [Data Preparation](#data-preparation)
8. [Running in IntelliJ IDEA](#running-in-intellij-idea)
9. [Configuration Options](#configuration-options)
10. [Code Structure Explanation](#code-structure-explanation)


## Project Overview

This project implements **TPC-H Query 10** using the **AJU (Acyclic Join under Updates)** algorithm based on the SIGMOD 2020 paper "Maintaining Acyclic Foreign-Key Joins under Updates". The system demonstrates efficient incremental query processing with theoretical guarantees.

### Key Features
- ✅ **Incremental Processing**: Updates only affected data, not entire dataset
- ✅ **Constant Delay Enumeration**: Immediate access to query results
- ✅ **Theoretical Guarantees**: O(λ) update complexity where λ measures update sequence difficulty
- ✅ **Built on Apache Flink**: Distributed, fault-tolerant stream processing
- ✅ **Supports All TPC-H Queries**: Extensible framework for analytical workloads
## System Architecture

### Architecture Diagram
```
┌─────────────────────────────────────────────────────────────┐
│                     TPC-H Query 10 AJU                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────┐    ┌──────────────┐    ┌──────────────┐    │
│  │  Data       │    │  Stream      │    │  Result      │    │
│  │  Source     │───▶│  Processing  │──▶│  Output      │    │
│  │ (TPC-H .tbl)│    │  (Flink)     │    │  (Console)   │    │
│  └─────────────┘    └──────────────┘    └──────────────┘    │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│              Components Breakdown:                          │
│                                                             │
│  1. TpchQ10Source: Streams TPC-H data with updates          │
│  2. Q10ProcessFunctionAJU: Core AJU algorithm               │
│  3. Top20ProcessFunction: Maintains top-20 customers        │
│  4. Q10JobAJU: Main job coordination                        │
└─────────────────────────────────────────────────────────────┘
```

### Data Flow
1. **Source Phase**: Reads TPC-H data files and simulates updates
2. **Processing Phase**: AJU algorithm incrementally maintains join results
3. **Output Phase**: Emits both full results and change logs

## Query 10 Overview

### SQL Query (Original TPC-H Q10)
```sql
SELECT 
    c_custkey,
    c_name,
    SUM(l_extendedprice * (1 - l_discount)) AS revenue,
    c_acctbal,
    n_name,
    c_address,
    c_phone,
    c_comment
FROM 
    customer,
    orders,
    lineitem,
    nation
WHERE 
    c_custkey = o_custkey
    AND l_orderkey = o_orderkey
    AND o_orderdate >= '1993-10-01'
    AND o_orderdate < '1994-01-01'
    AND l_returnflag = 'R'
    AND c_nationkey = n_nationkey
GROUP BY 
    c_custkey,
    c_name,
    c_acctbal,
    c_phone,
    n_name,
    c_address,
    c_comment
ORDER BY 
    revenue DESC
LIMIT 20;
```

### Simplified AJU Implementation
The implementation focuses on the core foreign-key join:
- `customer` ↔ `orders` (c_custkey = o_custkey)
- `orders` ↔ `lineitem` (o_orderkey = l_orderkey)
- `customer` ↔ `nation` (c_nationkey = n_nationkey)

## AJU Algorithm Core Concepts

### Key Principles
1. **Acyclic Foreign-Key Joins**: Join graph must be a DAG with foreign-key constraints
2. **Live Tuples**: Track which tuples can produce join results
3. **Assertion Keys**: Ensure consistency across multiple join paths
4. **Enclosure Measure (λ)**: Quantifies update sequence difficulty

### Update Complexity
- **Best case (λ=1)**: O(1) update time (FIFO sequences)
- **Worst case**: O(√|db|) update time
- **Average case**: O(λ) where λ is typically small

### Algorithm Steps
1. **Initialization**: Build foreign-key DAG and indexes
2. **Update Processing**: 
   - Insert/delete triggers status changes
   - Propagate changes through the DAG
   - Maintain live tuple sets
3. **Result Enumeration**: Constant-delay access to results

## Quick Start

### Prerequisites Check
Before starting, ensure you have:
- **IntelliJ IDEA** (Community or Ultimate edition)
- **Java 11** (JDK, not just JRE)
- **Maven 3.6+** (bundled with IntelliJ)
- **Flink 1.17.1**

### Setup (Using Pre-configured Project)

1. **Download the Project**
   ```bash
   # Clone the repository or download ZIP
   git clone <repository-url>
   cd latestIP
   ```

2. **Open in IntelliJ IDEA**
   ```
   File → Open → Select 'latestIP' folder
   Wait for Maven to import dependencies (auto-detected)
   ```

3. **Run Immediately**
   ```
   1. Navigate to: src/main/java/q10/Q10JobAJU.java
   2. Right-click → Run 'Q10JobAJU.main()'
   3. View results in Run console
   ```

### What You'll See
```
TOP20> Q10Result{custKey=67890, name='Customer#000067890', ... revenue=45678.90}
Q10-CHANGELOG> Q10Update{custKey=12345, delta=6789.12, kind=UPDATE}
```

## Detailed Setup Guide

### Software Requirements

| Software | Minimum Version | Recommended Version | Installation Guide |
|----------|-----------------|---------------------|-------------------|
| **IntelliJ IDEA** | 2021.3 | 2023.2 | [Download](https://www.jetbrains.com/idea/download/) |
| **Java JDK** | 1.8.0_261 | 11.0.20 | [Oracle JDK](https://www.oracle.com/java/technologies/javase-downloads.html) or [OpenJDK](https://adoptium.net/) |
| **Apache Maven** | 3.6.3 | 3.9.4 | [Download](https://maven.apache.org/download.cgi) |
| **Apache Flink** | 1.11.2 | 1.17.1 | [Download](https://flink.apache.org/downloads.html) |


### Dependencies in pom.xml

The project uses these key dependencies:
```xml
<dependencies>
    <!-- Flink Streaming API -->
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-streaming-java</artifactId>
        <version>1.17.1</version>
    </dependency>
    
    <!-- Flink Client -->
    <dependency>
        <groupId>org.apache.flink</groupId>
        <artifactId>flink-clients</artifactId>
        <version>1.17.1</version>
    </dependency>
    
    <!-- Logging -->
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-simple</artifactId>
        <version>1.7.36</version>
    </dependency>
</dependencies>
```

## Data Preparation

### Option 1: Use Provided Sample Data

The project includes small sample data files(scale factor = 0.01) in:
```
src/main/resources/data/
├── customer.tbl    
├── lineitem.tbl  
├── nation.tbl   
└── orders.tbl  
```

### Option 2: Generate Larger TPC-H Dataset

If you need larger data for performance testing:

1. **Clone TPC-H Generator**
   ```bash
   # Clone from GitHub (easier than official TPC website)
   git clone https://github.com/electrum/tpch-dbgen.git
   cd tpch-dbgen
   ```

2. **Compile the Generator**
   ```bash
   # Simple compilation
   make
   
   # If make fails, try:
   gcc -O3 -o dbgen dbgen.c -lm
   ```

3. **Generate Data**
   ```bash
   # Generate 1GB dataset (scale factor 1)
   ./dbgen -s 1
   
   # Scale factors:
   # -s 0.1  # 100MB (good for testing)
   # -s 1    # 1GB (default)
   # -s 5    # 5GB (for performance testing)
   # -s 10   # 10GB (for large-scale testing)
   ```

4. **Copy to Project**
   ```bash
   # Copy generated files to project
   cp customer.tbl lineitem.tbl nation.tbl orders.tbl \
      /path/to/latestIP/src/main/resources/data/
   
   # Or replace if files exist
   mv customer.tbl /path/to/latestIP/src/main/resources/data/
   mv lineitem.tbl /path/to/latestIP/src/main/resources/data/
   mv nation.tbl /path/to/latestIP/src/main/resources/data/
   mv orders.tbl /path/to/latestIP/src/main/resources/data/
   ```

## Running in IntelliJ IDEA

### Method 1: Direct Run (Simplest)

1. **Open Main Class**
   ```
   Navigate to: src/main/java/q10/Q10JobAJU.java
   ```

2. **Run the Program**
   ```
   Right-click → Run 'Q10JobAJU.main()'
   ```

3. **Monitor Output**
   ```
   Console shows:
   - Processing progress
   - Top-20 customer results
   - Change log updates
   - Execution time
   ```

### Method 2: Maven Execution

1. **Run via Maven Plugin**
   ```
   Maven tool window → Plugins → exec → exec:java
   Double-click to run
   ```

2. **Build and Package**
   ```
   Maven → Lifecycle → package
   Creates: target/q10-1.0.jar
   ```

### Run with Standalone Flink Cluster

1. **Download and Start Flink**
   ```bash
   # Download Flink (if not installed)
   wget https://archive.apache.org/dist/flink/flink-1.17.1/flink-1.17.1-bin-scala_2.12.tgz
   tar -xzf flink-1.17.1-bin-scala_2.12.tgz
   cd flink-1.17.1
   
   # Start cluster
   ./bin/start-cluster.sh
   
   # Verify cluster is running
   # Open browser: http://localhost:8081
   ```

2. **Submit Job to Flink**
   ```bash
   # Submit the job
   ./bin/flink run \
     -c q10.Q10JobAJU \
     /path/to/latestIP/target/q10-1.0.jar
   
   # With custom parallelism
   ./bin/flink run \
     -c q10.Q10JobAJU \
     /path/to/latestIP/target/q10-1.0.jar \
     --parallelism 4
   ```

3. **Monitor Job**
   ```bash
   # List running jobs
   ./bin/flink list
   
   # View job details
   # Visit: http://localhost:8081
   
   # Cancel job
   ./bin/flink cancel <job-id>
   ```

## Configuration Options

### Runtime Parameters

| Parameter | Default | Description | Usage Example |
|-----------|---------|-------------|---------------|
| `windowSize` | 100000 | FIFO window size for LineItems | `--windowSize 50000` |
| `warmupInsert` | 200000 | Initial LineItems to insert | `--warmupInsert 100000` |
| `parallelism` | 1 | Flink parallelism level | `--parallelism 4` |
| `logLevel` | INFO | Logging level (DEBUG, INFO, WARN, ERROR) | `--logLevel DEBUG` |
| `checkpointInterval` | 60000 | Checkpoint interval in milliseconds | `--checkpointInterval 30000` |

### Memory Configuration

For large datasets, adjust memory settings:

**In IntelliJ Run Configuration:**
```
VM options: -Xmx4g -Xms2g -XX:MaxMetaspaceSize=512m
```

**In pom.xml (for Maven):**
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>-Xmx2g -XX:MaxPermSize=256m</argLine>
    </configuration>
</plugin>
```

**For Flink cluster (flink-conf.yaml):**
```yaml
taskmanager.memory.process.size: 4096m
taskmanager.memory.managed.size: 2048m
jobmanager.memory.process.size: 2048m
```

### Performance Tuning Tips

1. **For Small Datasets (<1GB)**
   ```bash
   java -Xmx2g -Xms1g -jar q10-aju-1.0-SNAPSHOT.jar --parallelism 1
   ```

2. **For Medium Datasets (1-10GB)**
   ```bash
   java -Xmx8g -Xms4g -jar q10-aju-1.0-SNAPSHOT.jar --parallelism 4
   ```

3. **For Large Datasets (>10GB)**
   ```bash
   # Use Flink cluster with multiple task managers
   ./bin/flink run -p 8 -c q10.Q10JobAJU q10-aju-1.0-SNAPSHOT.jar
   ```
## Code Structure Explanation

### Core Classes

#### 1. `Q10JobAJU.java` - Main Job Coordinator
```java
// Main entry point
public class Q10JobAJU {
    public static void main(String[] args) throws Exception {
        // 1. Setup Flink environment
        // 2. Create data source stream
        // 3. Apply AJU processing function
        // 4. Output results to console
    }
}
```

**Key Responsibilities:**
- Flink execution environment setup
- Stream topology definition
- Job configuration and execution

#### 2. `Q10ProcessFunctionAJU.java` - Core AJU Algorithm
```java
public class Q10ProcessFunctionAJU 
        extends KeyedProcessFunction<Long, UpdateEvent<?>, Q10Result> {
    
    // State management for AJU
    private MapState<Long, Customer> customers;
    private MapState<Long, Nation> nations;
    private MapState<Long, Long> orderCustomer;
    private MapState<Long, Double> orderRevenue;
    private MapState<Long, Boolean> orderAlive;
    
    @Override
    public void processElement(UpdateEvent<?> evt, ...) {
        // AJU algorithm implementation
        // 1. Handle dimension updates (customer, nation)
        // 2. Handle fact updates (orders, lineitem)
        // 3. Maintain live tuple status
        // 4. Emit result updates
    }
}
```

**AJU State Management:**
- **Live Tuples**: Track which tuples can produce join results
- **Counters**: Maintain counts of live child tuples
- **Revenue Aggregation**: Incrementally update revenue totals

**Algorithm Logic:**
1. **LineItem Insertion**:
   - Update order revenue
   - Check if order becomes alive (first live lineitem)
   - Propagate to customer if order status changes

2. **LineItem Deletion**:
   - Update order revenue
   - Check if order becomes dead (no live lineitems)
   - Propagate to customer if order status changes

3. **Order Alive Logic**:
   - Order is "alive" if it has at least one lineitem with returnFlag='R'
   - Customer revenue aggregates only from alive orders

#### 3. `TpchQ10Source.java` - Data Source
```java
public class TpchQ10Source implements SourceFunction<UpdateEvent<?>> {
    
    @Override
    public void run(SourceContext<UpdateEvent<?>> ctx) {
        // Two-phase simulation:
        // Phase 1: Insert warmup data
        // Phase 2: FIFO sliding window
    }
}
```

**Update Simulation Strategy:**
- **Warmup Phase**: Insert initial set of lineitems
- **Steady State**: FIFO sliding window (insert new, delete oldest)
- **Realistic Workload**: Simulates continuous data streams

#### 4. `Top20ProcessFunction.java` - Result Aggregation
```java
public class Top20ProcessFunction 
        extends KeyedProcessFunction<Integer, Q10Result, String> {
    
    @Override
    public void processElement(Q10Result r, ...) {
        // Maintain top-20 customers by revenue
        // Periodic emission of current top-20
    }
}
```

**Features:**
- Periodic emission (every 20,000 updates)
- Maintains live customer set with revenue
- Sorts and outputs top-20 customers

### Package Structure
```
src/main/java/q10/
├── Q10JobAJU.java                    # Main job entry point
├── model/                           # Data models
│   ├── Customer.java
│   ├── LineItem.java
│   ├── Nation.java
│   ├── Order.java
│   ├── Q10Result.java              # Query result format
│   ├── Q10Update.java              # Update notification format
│   └── UpdateEvent.java            # Generic update wrapper
├── process/                         # Processing functions
│   ├── Q10ProcessFunctionAJU.java  # Core AJU algorithm
│   ├── Top20ProcessFunction.java   # Top-20 maintenance
│   └── Top20ProcessFunction3attritubes.java  # Alternative
├── sink/                           # Output sinks
│   └── OutputTop20.java
├── source/                         # Data sources
│   └── TpchQ10Source.java
└── state/                          # State management
    ├── Q10Job.java
    └── Q10JobAIU.java
```

### Key AJU Concepts in This Implementation

#### 1. **Foreign-Key Acyclic Graph**
```
customer (c_custkey) ← orders (o_custkey)
  ↓
nation (n_nationkey)   orders (o_orderkey) ← lineitem (l_orderkey)
```

### Resources
- **AJU Paper**: ["Maintaining Acyclic Foreign-Key Joins under Updates" (SIGMOD 2020)](https://dl.acm.org/doi/10.1145/3318464.3380586)
- **Flink Documentation**: https://flink.apache.org/
- **TPC-H Specification**: http://www.tpc.org/tpch/

## Conclusion

This implementation demonstrates the AJU algorithm's ability to efficiently maintain TPC-H Query 10 results under updates. The system provides:

- ✅ **Easy Setup**: Runs in IntelliJ with one click
- ✅ **Flexible Deployment**: Embedded or standalone Flink
- ✅ **Scalable Performance**: Handles GB-scale datasets
- ✅ **Research Value**: Implements cutting-edge incremental processing

To get started immediately:
1. Open `Q10JobAJU.java` in IntelliJ
2. Click the green run button
3. Observe real-time query results

---

*Last Updated: December 2024*  
*Version: 1.0*  
*Compatibility: Java 8/11, Flink 1.11-1.17, IntelliJ 2025.3+*
