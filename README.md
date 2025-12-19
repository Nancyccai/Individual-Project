# TPC-H Q10 AJU Implementation - Environment Setup and Usage Guide

## Table of Contents
1. [Environment Requirements](#environment-requirements)
2. [Directory Structure](#directory-structure)
3. [Software Installation](#software-installation)
4. [Data Generation Process](#data-generation-process)
5. [Running the Demo](#running-the-demo)
6. [Troubleshooting](#troubleshooting)
7. [Configuration Options](#configuration-options)

## Environment Requirements

### Operating System
- **Recommended**: MacOS Big Sur 11.5 or later
- **Alternative**: Linux (Ubuntu 20.04+ or CentOS 7+)
- **Note**: Windows is currently not supported due to shell script dependencies

### Software Dependencies

| Software | Version | Purpose |
|----------|---------|---------|
| **Java** | 1.8.0_261+ | Flink runtime and Java compilation |
| **Scala** | 2.11.8 | Flink compatibility |
| **Maven** | 3.6.3+ | Project build and dependency management |
| **Python** | 3.8.5+ | Data generation scripts |
| **Apache Flink** | 1.11.2 | Distributed stream processing engine |
| **Google Chrome** | 91.0.4472.106+ | Web interface (optional) |

## Directory Structure

```
latestIP/
├── src/main/java/q10/                    # Main source code
│   ├── model/                           # Data model classes
│   │   ├── Customer.java
│   │   ├── LineItem.java
│   │   ├── Nation.java
│   │   ├── Order.java
│   │   ├── Q10Result.java
│   │   ├── Q10Update.java
│   │   └── UpdateEvent.java
│   ├── process/                         # Processing logic
│   │   ├── Q10ProcessFunctionAJU.java  # Core AJU implementation
│   │   └── Top20ProcessFunction.java   # Top-20 ranking
│   ├── source/                          # Data sources
│   │   └── TpchQ10Source.java          # TPC-H data generator
│   ├── sink/                            # Output handlers
│   │   └── OutputTop20.java
│   ├── state/                           # State management
│   │   ├── CustomersState.java
│   │   ├── LineItemState.java
│   │   ├── OrderState.java
│   │   └── Q10JobAJU.java              # Main job coordinator
│   └── resources/
├── resources/data/                      # TPC-H data files
│   ├── customer.tbl
│   ├── lineitem.tbl
│   ├── nation.tbl
│   └── orders.tbl
├── pom.xml                             # Maven build configuration
└── README.md                           # This documentation
```

## Software Installation

### Step 1: Install Java 8

```bash
# For MacOS (using Homebrew)
brew tap AdoptOpenJDK/openjdk
brew install adoptopenjdk8

# For Ubuntu
sudo apt-get update
sudo apt-get install openjdk-8-jdk

# Verify installation
java -version
# Should output: java version "1.8.0_261"
```

### Step 2: Install Maven

```bash
# For MacOS
brew install maven

# For Ubuntu
sudo apt-get install maven

# Verify installation
mvn -v
# Should output: Apache Maven 3.6.3
```

### Step 3: Install Scala

```bash
# For MacOS
brew install scala@2.11

# For Ubuntu
sudo apt-get install scala

# Verify installation
scala -version
# Should output: Scala code runner version 2.11.8
```

### Step 4: Download and Install Apache Flink

```bash
# Download Flink 1.11.2
wget https://archive.apache.org/dist/flink/flink-1.11.2/flink-1.11.2-bin-scala_2.11.tgz

# Extract the archive
tar -xzf flink-1.11.2-bin-scala_2.11.tgz

# Navigate to Flink directory
cd flink-1.11.2

# Start Flink cluster (local mode)
./bin/start-cluster.sh

# Verify Flink is running
# Open browser and navigate to: http://localhost:8081
```

### Step 5: Install Python 3.8+

```bash
# For MacOS
brew install python@3.8

# For Ubuntu
sudo apt-get install python3.8 python3-pip

# Verify installation
python3 --version
# Should output: Python 3.8.5
```

## Data Generation Process

### Step 1: Obtain TPC-H Tools

1. Visit the official TPC website: http://tpc.org/tpc_documents_current_versions/current_specifications5.asp
2. Download "TPC-H_Tools_v3.0.0.zip"
3. Extract the archive to a local directory

### Step 2: Configure and Build TPC-H Generator

```bash
# Navigate to dbgen directory
cd /path/to/TPC-H_Tools/dbgen

# Copy the makefile template
cp makefile.suite makefile

# Edit makefile with appropriate settings
# For Linux/MacOS, use these settings:
CC      = gcc
DATABASE= ORACLE
MACHINE = LINUX
WORKLOAD = TPCH

# For Windows (if supported):
# MACHINE = WIN32
# DATABASE = SQLSERVER

# Build the dbgen executable
make

# Verify the executable was created
ls -la dbgen
# Should see executable file: dbgen
```

### Step 3: Generate TPC-H Data

```bash
# Generate data with scale factor 5 (approximately 5GB total)
./dbgen -s 5 -vf

# The -s parameter controls the scale factor:
# -s 1: ~1GB data
# -s 5: ~5GB data (recommended for demo)
# -s 10: ~10GB data

# This will generate 8 .tbl files:
# customer.tbl, lineitem.tbl, nation.tbl, orders.tbl,
# part.tbl, partsupp.tbl, region.tbl, supplier.tbl
```

### Step 4: Prepare Data for AJU Demo

```bash
# Create data directory in the project
mkdir -p latestIP/src/main/resources/data

# Copy only the needed tables for Q10
cp /path/to/TPC-H_Tools/dbgen/customer.tbl latestIP/src/main/resources/data/
cp /path/to/TPC-H_Tools/dbgen/lineitem.tbl latestIP/src/main/resources/data/
cp /path/to/TPC-H_Tools/dbgen/nation.tbl latestIP/src/main/resources/data/
cp /path/to/TPC-H_Tools/dbgen/orders.tbl latestIP/src/main/resources/data/

# Verify file sizes (scale factor 5)
ls -lh latestIP/src/main/resources/data/
# Expected:
# -rw-r--r--  1 user  staff   113M Jan 1 12:00 customer.tbl
# -rw-r--r--  1 user  staff   2.9G Jan 1 12:00 lineitem.tbl
# -rw-r--r--  1 user  staff   2.3K Jan 1 12:00 nation.tbl
# -rw-r--r--  1 user  staff   1.1G Jan 1 12:00 orders.tbl
```

### Data File Formats

**nation.tbl (example):**
```
0|ALGERIA|0| haggle. carefully final deposits detect slyly agai
1|ARGENTINA|1|al foxes promise slyly according to the regular accounts. bold requests alon
2|BRAZIL|1|y alongside of the pending deposits. carefully special packages are about the ironic forges. slyly special 
```

**customer.tbl (example):**
```
1|Customer#000000001|IVhzIApeRb ot,c,E|15|25-989-741-2988|711.56|BUILDING|to the even, regular platelets. regular, ironic epitaphs nag e
2|Customer#000000002|XSTf4,NCwDVaWNe6tEgvwfmRchLXak|13|23-768-687-3665|121.65|AUTOMOBILE|l accounts. blithely ironic theodolites integrate boldly: caref
```

**orders.tbl (example):**
```
1|36901|O|173665.47|1996-01-02|5-LOW|Clerk#000000951|0|nstructions sleep furiously among 
2|78002|O|46929.18|1996-12-01|1-URGENT|Clerk#000000880|0| foxes. pending accounts at the pending, silent asymptot
```

**lineitem.tbl (example):**
```
1|1552|106170|1|17|24710.35|0.04|0.02|N|O|1996-03-13|1996-02-12|1996-03-22|DELIVER IN PERSON|TRUCK|egular courts above the
1|67|107140|2|36|13309.60|0.09|0.06|N|O|1996-04-12|1996-02-28|1996-04-20|TAKE BACK RETURN|MAIL|ly final dependencies: slyly bold 
```

## Running the Demo

### Step 1: Build the Project

```bash
# Navigate to project root
cd latestIP

# Build the project using Maven
mvn clean package

# Expected output:
# [INFO] Building jar: /path/to/latestIP/target/q10-aju-1.0-SNAPSHOT.jar
# [INFO] BUILD SUCCESS

# Verify the JAR file was created
ls -lh target/*.jar
```

### Step 2: Start Flink Cluster

```bash
# Navigate to Flink installation directory
cd /path/to/flink-1.11.2

# Start Flink in local mode
./bin/start-cluster.sh

# Check if Flink is running
./bin/flink list
# Should show no running jobs initially

# Monitor Flink Web UI: http://localhost:8081
```

### Step 3: Run the AJU Job

```bash
# Submit the job to Flink
./bin/flink run \
  -c q10.Q10JobAJU \
  /path/to/latestIP/target/q10-aju-1.0-SNAPSHOT.jar \
  --parallelism 1

# Alternative: Run with custom parameters
./bin/flink run \
  -c q10.Q10JobAJU \
  /path/to/latestIP/target/q10-aju-1.0-SNAPSHOT.jar \
  --parallelism 4 \
  --windowSize 50000 \
  --warmupInsert 100000
```

### Step 4: Monitor Job Execution

```bash
# Monitor job status
./bin/flink list
# Output: RUNNING or FINISHED

# View Flink Web UI for detailed metrics:
# 1. Open browser to http://localhost:8081
# 2. Click on "Running Jobs"
# 3. Select your job to see:
#    - Throughput metrics
#    - State size
#    - Task manager details
#    - Watermark progression

# Check job logs
tail -f /path/to/flink-1.11.2/log/flink-*.out
```

### Step 5: View Results

The job produces two output streams:

1. **Top-20 Results** (printed to console):
```
TOP20> Q10Result{custKey=12345, name='Customer#000012345', ... revenue=56789.12}
TOP20> Q10Result{custKey=67890, name='Customer#000067890', ... revenue=45678.90}
...
```

2. **Change Log** (printed to console):
```
Q10-CHANGELOG> Q10Update{custKey=12345, oldRevenue=50000.0, newRevenue=56789.12, delta=6789.12, kind=UPDATE}
```

## Running with Different Configurations

### Configuration via Command Line

```bash
# Basic run with default parameters
./bin/flink run -c q10.Q10JobAJU q10-aju-1.0-SNAPSHOT.jar

# Run with increased parallelism
./bin/flink run -c q10.Q10JobAJU q10-aju-1.0-SNAPSHOT.jar --parallelism 4

# Run with custom data parameters
./bin/flink run -c q10.Q10JobAJU q10-aju-1.0-SNAPSHOT.jar \
  --windowSize 200000 \
  --warmupInsert 400000 \
  --scaleFactor 10

# Run in detached mode
./bin/flink run -d -c q10.Q10JobAJU q10-aju-1.0-SNAPSHOT.jar
```

### Configuration via Environment Variables

```bash
# Set environment variables before running
export FLINK_PARALLELISM=4
export WINDOW_SIZE=100000
export WARMUP_INSERT=200000

./bin/flink run -c q10.Q10JobAJU q10-aju-1.0-SNAPSHOT.jar
```

## Performance Testing

### Run with Different Scale Factors

```bash
# Test with small dataset (debug)
./dbgen -s 0.1 -vf
cp *.tbl latestIP/src/main/resources/data/
./bin/flink run -c q10.Q10JobAJU q10-aju-1.0-SNAPSHOT.jar

# Test with medium dataset
./dbgen -s 1 -vf
cp *.tbl latestIP/src/main/resources/data/
./bin/flink run -c q10.Q10JobAJU q10-aju-1.0-SNAPSHOT.jar --parallelism 2

# Test with large dataset
./dbgen -s 10 -vf
cp *.tbl latestIP/src/main/resources/data/
./bin/flink run -c q10.Q10JobAJU q10-aju-1.0-SNAPSHOT.jar --parallelism 8
```

### Monitor Resource Usage

```bash
# Monitor CPU usage
top -o cpu

# Monitor memory usage
jstat -gc <flink_pid> 1000

# Monitor disk I/O
iostat -x 1

# Monitor network
iftop -i lo0
```

## Troubleshooting

### Common Issues and Solutions

#### Issue 1: Java Version Mismatch
```
Error: Unsupported major.minor version 52.0
```
**Solution:**
```bash
# Ensure Java 8 is being used
java -version
# Should show 1.8.x

# Set JAVA_HOME explicitly
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
```

#### Issue 2: Flink Cluster Not Starting
```
Error: Could not start Flink cluster
```
**Solution:**
```bash
# Check if port 8081 is already in use
lsof -i :8081

# Kill existing process if needed
kill -9 <pid>

# Start with clean state
./bin/stop-cluster.sh
rm -rf /tmp/flink-*
./bin/start-cluster.sh
```

#### Issue 3: Out of Memory Errors
```
java.lang.OutOfMemoryError: Java heap space
```
**Solution:**
```bash
# Increase heap size in Flink configuration
# Edit flink-1.11.2/conf/flink-conf.yaml

# Add or modify:
taskmanager.memory.process.size: 4096m
jobmanager.memory.process.size: 2048m

# Restart Flink
./bin/stop-cluster.sh
./bin/start-cluster.sh
```

#### Issue 4: Data Generation Issues
```
dbgen: command not found
```
**Solution:**
```bash
# Ensure makefile is properly configured
cd /path/to/TPC-H_Tools/dbgen
make clean
make

# Check for compilation errors
gcc --version
# Should be 4.8+ for Linux, clang for MacOS
```

#### Issue 5: Missing Data Files
```
java.io.FileNotFoundException: data/customer.tbl
```
**Solution:**
```bash
# Ensure data files are in correct location
ls -la src/main/resources/data/

# If using IDE, mark resources directory
# For IntelliJ: File -> Project Structure -> Modules -> Mark as Resources
```

### Debug Mode

```bash
# Run Flink job with debug logging
./bin/flink run \
  -c q10.Q10JobAJU \
  q10-aju-1.0-SNAPSHOT.jar \
  --logLevel DEBUG

# Check detailed logs
tail -f /path/to/flink-1.11.2/log/flink-*.log

# Enable remote debugging
./bin/flink run \
  -c q10.Q10JobAJU \
  q10-aju-1.0-SNAPSHOT.jar \
  -Denv.java.opts="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

## Configuration Options

### Runtime Parameters

| Parameter | Default | Description | Command Line Argument |
|-----------|---------|-------------|----------------------|
| `parallelism` | 1 | Number of parallel task slots | `--parallelism 4` |
| `windowSize` | 100000 | FIFO window size for LineItems | `--windowSize 200000` |
| `warmupInsert` | 200000 | Initial LineItems to insert | `--warmupInsert 400000` |
| `scaleFactor` | 5 | TPC-H scale factor (data size) | `--scaleFactor 10` |
| `logLevel` | INFO | Logging level (DEBUG, INFO, WARN) | `--logLevel DEBUG` |
| `checkpointInterval` | 60000 | Fault tolerance checkpoint interval (ms) | `--checkpointInterval 30000` |

### Memory Configuration

```yaml
# flink-conf.yaml modifications for optimal performance
taskmanager.memory.process.size: 4096m
taskmanager.numberOfTaskSlots: 4
jobmanager.memory.process.size: 2048m

# For large datasets (scale factor 10+)
taskmanager.memory.process.size: 8192m
taskmanager.memory.managed.size: 4096m
```

### Performance Tuning

```bash
# Optimize for throughput
./bin/flink run \
  -c q10.Q10JobAJU \
  q10-aju-1.0-SNAPSHOT.jar \
  --bufferTimeout 0 \
  --parallelism $(nproc)

# Optimize for low latency
./bin/flink run \
  -c q10.Q10JobAJU \
  q10-aju-1.0-SNAPSHOT.jar \
  --bufferTimeout 10 \
  --parallelism 1
```

## Quick Start Summary

### For First-Time Users

```bash
# 1. Install dependencies
brew install java8 maven scala

# 2. Download and start Flink
wget https://archive.apache.org/dist/flink/flink-1.11.2/flink-1.11.2-bin-scala_2.11.tgz
tar -xzf flink-1.11.2-bin-scala_2.11.tgz
cd flink-1.11.2
./bin/start-cluster.sh

# 3. Generate TPC-H data
cd /path/to/TPC-H_Tools/dbgen
make
./dbgen -s 1 -vf
cp customer.tbl lineitem.tbl nation.tbl orders.tbl /path/to/latestIP/src/main/resources/data/

# 4. Build and run the demo
cd /path/to/latestIP
mvn clean package
cd /path/to/flink-1.11.2
./bin/flink run -c q10.Q10JobAJU /path/to/latestIP/target/q10-aju-1.0-SNAPSHOT.jar

# 5. Monitor results
# Check console output or visit http://localhost:8081
```

### Estimated Time Requirements

| Step | Time Estimate | Disk Space |
|------|---------------|------------|
| Software Installation | 15-30 minutes | 2GB |
| TPC-H Data Generation (scale 5) | 5-10 minutes | 5GB |
| Project Build | 2-3 minutes | 500MB |
| Job Execution (scale 5) | 10-20 minutes | 1GB heap |

This setup guide provides all necessary steps to run the TPC-H Q10 AJU implementation from scratch. The system demonstrates efficient incremental query maintenance with theoretical guarantees while being practical for real-world streaming scenarios.
