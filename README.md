# Spark Scala ETL Project for Databricks & AWS EMR

A production-ready, enterprise-grade ETL pipeline for cloud data ingestion using Apache Spark, Scala, and Hive-compatible metastores. Supports both Databricks and AWS EMR deployments with seamless cloud storage integration (S3, DBFS).

## Features

- **Multi-Format Support**: CSV, JSON, and Parquet data ingestion
- **Dual Cloud Compatibility**: Databricks and AWS EMR
- **Enterprise Architecture**: Modular, testable Scala codebase
- **Cloud Storage Abstraction**: Automatic path normalization (DBFS, S3, S3A)
- **Hive Metastore Integration**: AWS Glue Catalog, HMS support
- **Data Transformations**: Schema casting, string trimming, load date stamping
- **Configuration Management**: Externalized HOCON configuration with environment overrides
- **Delta Lake Support**: ACID transactions and time-travel capabilities
- **Comprehensive Logging**: SLF4J with production-ready log levels
- **Interactive Notebooks**: Databricks-native notebook for data exploration

## Project Structure

```
SparkScalaETLprojectforDatabricks_EMR/
├── build.sbt                              # SBT build configuration
├── src/
│   ├── main/
│   │   ├── scala/com/etl/spark/
│   │   │   ├── ETLPipeline.scala         # Main entry point
│   │   │   ├── core/
│   │   │   │   └── SparkSessionProvider.scala    # Spark session factory
│   │   │   ├── io/
│   │   │   │   └── CloudFileReader.scala         # Cloud file reader
│   │   │   ├── transform/
│   │   │   │   └── Transformer.scala             # Data transformations
│   │   │   ├── load/
│   │   │   │   └── HiveLoader.scala              # Hive table writer
│   │   │   └── util/
│   │   │       └── ConfigManager.scala           # Configuration handler
│   │   └── resources/
│   │       └── application.conf          # HOCON configuration
│   └── test/
│       └── scala/                        # Test suite (placeholder)
├── notebooks/
│   └── ETLPipeline.scala                 # Databricks notebook
├── sql/
│   └── create_table.hql                  # Hive DDL scripts
├── scripts/
│   ├── run-emr.sh                        # EMR deployment script
│   ├── submit-emr-job.sh                 # EMR job submission
│   └── submit-databricks-job.sh           # Databricks job submission
└── target/                               # Compiled artifacts
```

## Prerequisites

### Local Development
- Java 11+
- Scala 2.12.18
- SBT 1.9+
- AWS CLI v2 (for EMR deployment)

### Databricks
- Databricks workspace with Unity Catalog or Hive Metastore enabled
- DBFS mount point for data access
- Compute cluster with Databricks Runtime 13.3+ (Scala 2.12)

### AWS EMR
- EMR cluster 7.1.0+ with Spark 3.5.0 and Scala 2.12
- S3 bucket for data and jar storage
- IAM role with S3 and Glue Catalog permissions
- (Optional) AWS Glue Metastore or Hive Metastore Service

## Installation & Build

### 1. Clone the Project
```bash
cd E:\POC\ code\SparkScalaETLprojectforDatabricks_EMR
```

### 2. Build the Project
```bash
sbt clean compile
```

### 3. Build Assembly JAR (for EMR/Production)
```bash
sbt assembly
```

The JAR will be created at: `target/SparkScalaETLprojectforDatabricks_EMR-assembly-0.1.0-SNAPSHOT.jar`

## Configuration

### application.conf Overview

The configuration file `src/main/resources/application.conf` uses HOCON format with environment variable overrides:

#### Environment Selection
```hocon
environment = "databricks"        # Options: databricks, emr, local
environment = ${?SPARK_ENV}       # Override via environment variable
```

#### Cloud Storage Configuration
**Databricks:**
```hocon
cloud.dbfs.rootPath = "/dbfs/mnt/etl"
```

**AWS EMR:**
```hocon
cloud.s3.bucket = "my-data-bucket"
cloud.s3.region = "us-east-1"
```

#### Source Data Paths
```hocon
source.csv.path = "data/input/csv"      # Relative path under cloud root
source.json.path = "data/input/json"
source.parquet.path = "data/input/parquet"
```

#### Target Configuration
```hocon
target.database = "etl_db"              # Hive database name
target.warehouse.location = "s3://my-warehouse/tables"
target.writeMode = "overwrite"          # Options: overwrite, append, ignore, error
target.useDataFormat = "delta"          # Options: delta, parquet
```

#### Metastore Configuration
```hocon
# AWS Glue Catalog
hive.metastore.type = "glue"
hive.metastore.glue.catalogId = ""

# OR Hive Metastore Service (HMS)
hive.metastore.type = "hms"
hive.metastore.hms.thriftUri = "thrift://localhost:9083"
hive.metastore.hms.warehouse = "s3a://hive-warehouse"
```

#### Data Transformation Rules
```hocon
transformations {
  trimColumns = ["name", "description", "email"]
  castSchema = true
  addLoadDate = true
  loadDateColumn = "load_dt"
  
  schema {
    customer_id = "BIGINT"
    order_date = "DATE"
    amount = "DECIMAL(18,2)"
  }
}
```

### Setting Environment Variables

**Databricks:**
```bash
export SPARK_ENV=databricks
export DBFS_ROOT=/dbfs/mnt/etl
export TARGET_DB=etl_db
```

**AWS EMR:**
```bash
export SPARK_ENV=emr
export S3_BUCKET=my-data-bucket
export AWS_REGION=us-east-1
export METASTORE_TYPE=glue
```

## Deployment

### Option 1: Databricks Interactive Notebook

1. **Upload the notebook**:
   - In Databricks workspace, click "Workspace" → "Create" → "Notebook"
   - Import from file: `notebooks/ETLPipeline.scala`

2. **Configure parameters**:
   - Set widget parameters in the notebook:
     ```scala
     val sourceFormat = "csv"
     val sourcePath = "/dbfs/mnt/etl/data/input/csv"
     val tableName = "customer_csv"
     val writeMode = "overwrite"
     ```

3. **Run the notebook**:
   - Click "Run All" or execute individual cells
   - Monitor job progress in Databricks UI

### Option 2: Databricks Job Submission

```bash
# Set Databricks credentials
export DATABRICKS_WORKSPACE_URL=https://your-workspace.cloud.databricks.com
export DATABRICKS_TOKEN=your-personal-access-token

# Upload JAR
databricks fs cp target/SparkScalaETLprojectforDatabricks_EMR-assembly-0.1.0-SNAPSHOT.jar \
  dbfs:/Workspace/Shared/jars/

# Submit job
bash scripts/submit-databricks-job.sh
```

### Option 3: AWS EMR Spark-Submit (Interactive/SSH)

1. **Build the JAR**:
```bash
sbt assembly
```

2. **Upload to S3**:
```bash
aws s3 cp target/SparkScalaETLprojectforDatabricks_EMR-assembly-0.1.0-SNAPSHOT.jar \
  s3://my-data-bucket/jars/
```

3. **SSH into EMR master node**:
```bash
aws emr ssh --cluster-id j-1ABC2DEFGHIJK \
  --key-pair-file ~/.ssh/my-key.pem
```

4. **Submit Spark job**:
```bash
spark-submit \
  --class com.etl.spark.ETLPipeline \
  --master yarn \
  --deploy-mode cluster \
  --num-executors 10 \
  --executor-cores 4 \
  --executor-memory 8g \
  --driver-memory 4g \
  --conf spark.sql.shuffle.partitions=200 \
  --conf spark.sql.adaptive.enabled=true \
  --conf spark.hadoop.fs.s3a.impl=org.apache.hadoop.fs.s3a.S3AFileSystem \
  --conf spark.hadoop.fs.s3a.aws.credentials.provider=com.amazonaws.auth.DefaultAWSCredentialsProviderChain \
  --conf SPARK_ENV=emr \
  --conf S3_BUCKET=my-data-bucket \
  --conf AWS_REGION=us-east-1 \
  s3a://my-data-bucket/jars/SparkScalaETLprojectforDatabricks_EMR-assembly-0.1.0-SNAPSHOT.jar
```

### Option 4: AWS EMR Job Step Submission

```bash
# Set cluster ID and environment variables
export EMR_CLUSTER_ID=j-1ABC2DEFGHIJK
export S3_BUCKET=my-data-bucket
export AWS_REGION=us-east-1

# Run deployment script
bash scripts/run-emr.sh

# Or use submit script
bash scripts/submit-emr-job.sh ${EMR_CLUSTER_ID}
```

## Core Module Details

### SparkSessionProvider

Handles Spark session creation with environment-specific configurations:

- **Databricks**:
  - Delta Lake catalog registration
  - S3 file system configuration
  - Adaptive query execution optimization
  
- **AWS EMR**:
  - AWS Glue Catalog configuration (default)
  - Hive Metastore Service support
  - S3A file system with retry logic

**Usage**:
```scala
val spark = SparkSessionProvider.getOrCreateSession
```

### CloudFileReader

Abstracts cloud storage path handling and multi-format reading:

```scala
val reader = CloudFileReader(spark)

// Read CSV
val csvDf = reader.readCSV("data/input/csv")

// Read JSON
val jsonDf = reader.readJSON("data/input/json")

// Read Parquet
val parquetDf = reader.readParquet("data/input/parquet")

// Verify path exists
if (reader.verifyPathExists("data/input/csv")) {
  // Process data
}
```

### Transformer

Applies consistent data quality transformations:

```scala
val transformer = Transformer.create(spark)
val transformedDf = transformer.transform(rawDf)
```

Transformations:
1. **String Trimming**: Trims whitespace from configured string columns
2. **Schema Casting**: Explicitly casts columns to specified types
3. **Load Date**: Adds current_date() column for audit trail

### HiveLoader

Manages Hive metastore table operations:

```scala
val loader = HiveLoader(spark)

// Create database
loader.createDatabase()

// Load data to Hive table
loader.loadToHive(df, "customer_data", "overwrite")

// Load to external table with custom location
loader.loadToExternalTable(df, "customer_data", "s3a://my-bucket/path", "append")

// Get table metadata
val metadata = loader.getTableMetadata("customer_data")
```

## Data Flow

```
┌─────────────────────────────────┐
│ Cloud Storage                   │
│ (S3/DBFS)                       │
│ CSV, JSON, Parquet              │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│ CloudFileReader                 │
│ • Path normalization            │
│ • Multi-format support          │
│ • Path verification             │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│ Transformer                     │
│ • String trimming               │
│ • Schema casting                │
│ • Load date stamping            │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│ HiveLoader                      │
│ • Database creation             │
│ • Table creation/updates        │
│ • Metadata management           │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│ Hive Metastore                  │
│ (AWS Glue / HMS)                │
│ customer_csv                    │
│ customer_json                   │
│ customer_parquet                │
└─────────────────────────────────┘
```

## Running the ETL Pipeline

### Locally (for testing)
```bash
sbt run
```

Requires local `warehouse` directory and mock data in `data/input/` paths.

### Databricks Notebook
1. Open `notebooks/ETLPipeline.scala`
2. Set widget parameters via UI
3. Click "Run All"

### EMR Job Submission
```bash
bash scripts/run-emr.sh
# Or with explicit parameters
EMR_CLUSTER_ID=j-1ABC2DEFGHIJK \
S3_BUCKET=my-data-bucket \
AWS_REGION=us-east-1 \
bash scripts/run-emr.sh
```

## Performance Tuning

### Spark Configurations

```hocon
spark.sql.shuffle.partitions = 200          # Default partition count
spark.sql.adaptive.enabled = true           # Enable adaptive query execution
spark.sql.adaptive.coalescePartitions.enabled = true
spark.sql.adaptive.skewJoin.enabled = true
```

### EMR-Specific Tuning

```hocon
# In spark-submit or spark.conf
--num-executors 10              # Number of executor JVMs
--executor-cores 4              # Cores per executor
--executor-memory 8g            # Memory per executor
--driver-memory 4g              # Driver JVM memory

# Increase for larger datasets:
--num-executors 20
--executor-cores 8
--executor-memory 16g
```

### S3 Connection Tuning

```hocon
spark.hadoop.fs.s3a.connection.timeout=120000
spark.hadoop.fs.s3a.socket.timeout=120000
spark.hadoop.fs.s3a.threads.max=10
spark.hadoop.fs.s3a.threads.core=4
```

## Troubleshooting

### Common Issues

**1. Path Not Found Error**
```
FileNotFoundException: s3://bucket/path does not exist
```
- Verify S3 bucket name and region in `application.conf`
- Check IAM role has S3:GetObject and S3:ListBucket permissions
- Ensure data exists at configured source path

**2. Metastore Connection Error**
```
Cannot connect to Hive Metastore
```
- Verify `hive.metastore.hms.thriftUri` is correct for HMS
- For Glue, ensure `AWSGlueDataCatalogHiveClientFactory` is configured
- Check EMR cluster has IAM permissions for Glue API

**3. Out of Memory**
- Increase executor memory: `--executor-memory 16g`
- Increase number of partitions: `spark.sql.shuffle.partitions=400`
- Check for broadcast joins of large DataFrames

**4. Slow Performance**
- Enable adaptive query execution: `spark.sql.adaptive.enabled=true`
- Increase S3 connection pool: `fs.s3a.threads.max=20`
- Consider Delta Lake for repeated queries (caching)

## Data Quality Checks

The pipeline includes built-in checks:

1. **Row Count Validation**: Compares pre/post transformation counts
2. **Path Verification**: Confirms source paths exist before reading
3. **Schema Validation**: Ensures required columns exist before casting
4. **Table Metadata**: Retrieves and logs table statistics post-load

## Security Considerations

1. **AWS Credentials**: Uses IAM roles; no hardcoded credentials
2. **S3 Encryption**: Objects encrypted at rest using S3 SSE
3. **Databricks Secrets**: Use Databricks Secret scope for API tokens
4. **Data Sensitivity**: Implement PII masking in Transformer if needed

## Monitoring & Logging

### Log Levels
```hocon
logging.level = "INFO"          # Options: DEBUG, INFO, WARN, ERROR
logging.level = ${?LOG_LEVEL}   # Override via environment variable
```

### Metrics
- Row counts at each pipeline stage
- Table creation timestamps
- Data transformation execution time
- Hive metastore operation results

## Dependencies

Key libraries included:
- **Apache Spark** 3.5.0: Core data processing
- **Delta Lake** 3.0.0: ACID transactions and time-travel
- **AWS SDK** 1.12.565: S3 and Glue integration
- **Typesafe Config** 1.4.3: Configuration management
- **SLF4J** 2.0.9: Logging

## Support & Documentation

- [Apache Spark Documentation](https://spark.apache.org/docs/3.5.0/)
- [Databricks Guide](https://docs.databricks.com/)
- [AWS EMR Documentation](https://docs.aws.amazon.com/emr/)
- [Delta Lake Guide](https://docs.delta.io/)

## License

Enterprise grade - internal use only

