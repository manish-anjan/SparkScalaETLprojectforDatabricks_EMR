# Technical Reference Guide

Complete technical reference for the Spark Scala ETL Pipeline.

---

## API Reference

### ConfigManager

```scala
object ConfigManager {
  def getConfig: Config                          // Get full config object
  def getString(path: String): String            // Get string value
  def getString(path: String, default: String)   // Get with default
  def getInt(path: String): Int                  // Get int value
  def getInt(path: String, default: Int)         // Get int with default
  def getBoolean(path: String): Boolean          // Get boolean value
  def getBoolean(path: String, default: Boolean) // Get boolean with default
  def getStringList(path: String): List[String]  // Get string list
  
  def getEnvironment: String                     // "databricks", "emr", "local"
  def isDataBricks: Boolean                      // Is Databricks environment
  def isEMR: Boolean                             // Is EMR environment
  
  def getSourcePath(format: String): String      // Cloud-aware source path
  def getTargetLocation: String                  // Cloud-aware target location
}
```

### SparkSessionProvider

```scala
object SparkSessionProvider {
  def getOrCreateSession: SparkSession           // Get or create Spark session
}
```

**Configuration Applied:**
- Delta Lake catalog extensions
- Adaptive query execution
- S3/S3A file system settings
- Metastore configuration (Glue/HMS)
- Partition optimization

### CloudFileReader

```scala
class CloudFileReader(spark: SparkSession) {
  def readCSV(path: String): DataFrame           // Read CSV file
  def readJSON(path: String): DataFrame          // Read JSON file
  def readParquet(path: String): DataFrame       // Read Parquet file
  def readDelta(path: String): DataFrame         // Read Delta table
  def readGeneric(path: String, format: String): DataFrame  // Generic reader
  def verifyPathExists(path: String): Boolean    // Check path exists
}
```

**Supported Paths:**
- Databricks: `/dbfs/path` or relative
- EMR: `s3a://bucket/path` or `s3://bucket/path`
- Local: `relative/path` or `file://absolute/path`

### Transformer

```scala
trait DataTransformer {
  def transform(df: DataFrame): DataFrame       // Apply transformations
}

object Transformer {
  def create(spark: SparkSession): DataTransformer
}
```

**Transformations Applied:**
1. String column trimming (if configured)
2. Explicit schema casting (if enabled)
3. Load date column injection (if enabled)

### HiveLoader

```scala
class HiveLoader(spark: SparkSession) {
  def loadToHive(df: DataFrame, tableName: String): Unit
  def loadToHive(df: DataFrame, tableName: String, writeMode: String): Unit
  def loadToExternalTable(df: DataFrame, tableName: String, location: String): Unit
  def loadToExternalTable(df: DataFrame, tableName: String, location: String, writeMode: String): Unit
  
  def createDatabase(): Unit                     // Create target database
  def truncateTable(tableName: String): Unit     // Truncate table
  def dropTable(tableName: String, ifExists: Boolean): Unit
  def getTableMetadata(tableName: String): Map[String, String]
}
```

**Write Modes:**
- `"overwrite"` → SaveMode.Overwrite (DROP + CREATE)
- `"append"` → SaveMode.Append (INSERT INTO)
- `"ignore"` → SaveMode.Ignore (skip if exists)
- `"error"` → SaveMode.ErrorIfExists (throw error)

---

## Configuration Reference

### Top-Level Settings

```hocon
environment = "databricks"         # "databricks", "emr", or "local"
environment = ${?SPARK_ENV}        # Override via environment variable

spark.appName = "SparkScalaETL"
spark.master = "local[*]"          # Overridden for Databricks/EMR

logging.level = "INFO"             # "DEBUG", "INFO", "WARN", "ERROR"
logging.level = ${?LOG_LEVEL}
```

### Spark Configuration

```hocon
spark.sql.shuffle.partitions = 200                    # Default partition count
spark.sql.adaptive.enabled = true                     # Adaptive query execution
spark.sql.adaptive.coalescePartitions.enabled = true  # Coalesce small partitions
spark.sql.adaptive.skewJoin.enabled = true            # Skew join handling
spark.databricks.delta.preview.enabled = true         # Databricks Delta features
```

### Cloud Configuration

#### Databricks
```hocon
cloud.dbfs.rootPath = "/dbfs/mnt/etl"
cloud.dbfs.rootPath = ${?DBFS_ROOT}
```

#### AWS EMR
```hocon
cloud.s3.bucket = "my-data-bucket"
cloud.s3.bucket = ${?S3_BUCKET}
cloud.s3.region = "us-east-1"
cloud.s3.region = ${?AWS_REGION}
cloud.s3.usePathStyle = false                   # Use virtual-hosted style
cloud.s3.usePathStyle = ${?S3_PATH_STYLE}
```

### Source Configuration

```hocon
source.csv {
  path = "data/input/csv"                       # Relative path
  format = "csv"
  inferSchema = true                            # Auto-detect schema
  header = true                                 # CSV has header row
  delimiter = ","                               # Field delimiter
  nullValue = ""                                # Null representation
  nanValue = "NaN"                              # NaN representation
}

source.json {
  path = "data/input/json"
  format = "json"
  multiLine = false                             # Each line is one record
}

source.parquet {
  path = "data/input/parquet"
  format = "parquet"                            # Auto schema detection
}
```

### Target Configuration

```hocon
target {
  database = "etl_db"
  database = ${?TARGET_DB}
  
  warehouse.location = "s3://my-warehouse/tables"
  warehouse.location = ${?WAREHOUSE_LOCATION}
  
  writeMode = "overwrite"                       # "overwrite", "append", "ignore", "error"
  writeMode = ${?WRITE_MODE}
  
  useDataFormat = "delta"                       # "delta" or "parquet"
  useDataFormat = ${?DATA_FORMAT}
  
  externalTable = true                          # Use external location
  partitionBy = []                              # Partition columns (future)
}
```

### Hive Configuration

```hocon
hive {
  metastore.type = "glue"                       # "glue" or "hms"
  metastore.type = ${?METASTORE_TYPE}
  
  glue {
    catalogId = ""                              # AWS account ID (optional)
    catalogId = ${?GLUE_CATALOG_ID}
  }
  
  hms {
    thriftUri = "thrift://localhost:9083"       # HMS server URI
    thriftUri = ${?HMS_THRIFT_URI}
    warehouse = "s3a://hive-warehouse"
    warehouse = ${?HMS_WAREHOUSE}
  }
  
  enabled = true
  partitionDiscovery = true
  validatePartitionColumns = true
}
```

### Transformation Configuration

```hocon
transformations {
  trimColumns = ["name", "description", "email"] # Columns to trim (or all if empty)
  castSchema = true                              # Enable schema casting
  addLoadDate = true                             # Add load_dt column
  loadDateColumn = "load_dt"                     # Load date column name
  
  schema {
    customer_id = "BIGINT"
    order_date = "DATE"
    amount = "DECIMAL(18,2)"
    # Add more column → type mappings as needed
  }
}
```

### AWS Configuration

```hocon
aws.emr {
  clusterName = "spark-etl-cluster"
  clusterName = ${?EMR_CLUSTER_NAME}
  releaseLabel = "emr-7.1.0"
  releaseLabel = ${?EMR_RELEASE_VERSION}
  instanceType = "m5.xlarge"
  instanceType = ${?EMR_INSTANCE_TYPE}
  instanceCount = 3
  instanceCount = ${?EMR_INSTANCE_COUNT}
  logUri = "s3://my-emr-logs/spark-etl/"
  logUri = ${?EMR_LOG_URI}
}

aws.credentials {
  assumeRoleArn = ""
  assumeRoleArn = ${?AWS_ASSUME_ROLE_ARN}
  sessionName = "spark-etl-session"
  sessionName = ${?AWS_ROLE_SESSION_NAME}
}
```

### Job Configuration

```hocon
job {
  retries = 1
  retries = ${?JOB_RETRIES}
  timeout = 3600                                 # Seconds
  timeout = ${?JOB_TIMEOUT}
  enableMetrics = true
  enableMetrics = ${?ENABLE_METRICS}
  enableCheckpoint = false
  enableCheckpoint = ${?ENABLE_CHECKPOINT}
}
```

---

## Environment Variables Reference

### Environment Selection
```bash
export SPARK_ENV=databricks    # or "emr" or "local"
```

### Cloud Paths
```bash
export DBFS_ROOT=/dbfs/mnt/etl
export S3_BUCKET=my-data-bucket
export AWS_REGION=us-east-1
```

### Target Settings
```bash
export TARGET_DB=etl_prod
export WAREHOUSE_LOCATION=s3://my-warehouse/tables
export WRITE_MODE=overwrite
export DATA_FORMAT=delta
```

### Metastore
```bash
export METASTORE_TYPE=glue         # or "hms"
export GLUE_CATALOG_ID=123456789
export HMS_THRIFT_URI=thrift://host:9083
export HMS_WAREHOUSE=s3a://warehouse
```

### Logging
```bash
export LOG_LEVEL=INFO              # "DEBUG", "INFO", "WARN", "ERROR"
```

### AWS
```bash
export AWS_ASSUME_ROLE_ARN=arn:aws:iam::123456789:role/spark-role
export AWS_ROLE_SESSION_NAME=spark-etl-session
export EMR_CLUSTER_NAME=spark-etl-cluster
export EMR_RELEASE_VERSION=emr-7.1.0
export EMR_INSTANCE_TYPE=m5.xlarge
export EMR_INSTANCE_COUNT=3
export EMR_LOG_URI=s3://my-logs/emr/
```

---

## SQL Reference

### Create Database
```sql
CREATE DATABASE IF NOT EXISTS etl_db
COMMENT 'ETL database for cloud data ingestion'
LOCATION 's3a://my-warehouse/etl_db';
```

### Create External Table
```sql
CREATE EXTERNAL TABLE IF NOT EXISTS etl_db.customer (
    customer_id BIGINT,
    customer_name STRING,
    load_dt DATE
)
STORED AS PARQUET
LOCATION 's3a://my-warehouse/tables/customer'
TBLPROPERTIES ('EXTERNAL' = 'TRUE');
```

### Create Delta Table
```sql
CREATE TABLE IF NOT EXISTS etl_db.customer_delta (
    customer_id BIGINT,
    customer_name STRING,
    load_dt DATE
)
USING DELTA
LOCATION 's3a://my-warehouse/tables/customer_delta'
TBLPROPERTIES ('delta.enableChangeDataFeed' = 'true');
```

### Query Examples
```sql
-- List databases
SHOW DATABASES;

-- List tables
SHOW TABLES IN etl_db;

-- Describe table
DESCRIBE EXTENDED etl_db.customer;

-- Query data
SELECT COUNT(*) FROM etl_db.customer;
SELECT * FROM etl_db.customer WHERE load_dt = CURRENT_DATE() LIMIT 10;

-- Check Delta history
SELECT * FROM etl_db.customer_delta@v0;  -- Specific version
SELECT * FROM etl_db.customer_delta TIMESTAMP AS OF '2026-02-24';
```

---

## Spark Submit Command Reference

### Databricks (via Job)
```bash
databricks jobs create --json-file job_config.json
databricks jobs run-now --job-id <job-id>
```

### EMR - Basic
```bash
spark-submit \
  --class com.etl.spark.ETLPipeline \
  --master yarn \
  --deploy-mode cluster \
  --num-executors 10 \
  --executor-cores 4 \
  --executor-memory 8g \
  --driver-memory 4g \
  s3a://bucket/jars/app.jar
```

### EMR - Full Configuration
```bash
spark-submit \
  --class com.etl.spark.ETLPipeline \
  --master yarn \
  --deploy-mode cluster \
  --num-executors 10 \
  --executor-cores 4 \
  --executor-memory 8g \
  --driver-memory 4g \
  --driver-cores 2 \
  --conf spark.driver.maxResultSize=4g \
  --conf spark.sql.shuffle.partitions=200 \
  --conf spark.sql.adaptive.enabled=true \
  --conf spark.hadoop.fs.s3a.impl=org.apache.hadoop.fs.s3a.S3AFileSystem \
  --conf spark.hadoop.fs.s3a.aws.credentials.provider=com.amazonaws.auth.DefaultAWSCredentialsProviderChain \
  --conf spark.hadoop.fs.s3a.connection.timeout=120000 \
  --conf spark.hadoop.fs.s3a.socket.timeout=120000 \
  --conf SPARK_ENV=emr \
  --conf S3_BUCKET=my-bucket \
  --conf AWS_REGION=us-east-1 \
  s3a://bucket/jars/app.jar
```

---

## Logging Reference

### Log Levels
```scala
spark.sparkContext.setLogLevel("DEBUG")    // Verbose debugging
spark.sparkContext.setLogLevel("INFO")     // Standard (default)
spark.sparkContext.setLogLevel("WARN")     // Warnings only
spark.sparkContext.setLogLevel("ERROR")    // Errors only
```

### Accessing Logs

**Databricks:**
- Job runs show logs directly in UI
- Export logs: Runs → Details → View logs

**EMR:**
```bash
# Check driver logs
ssh hadoop@master
yarn logs -applicationId application_xxx

# Check S3
aws s3 ls s3://bucket/emr-logs/ --recursive
```

**Local:**
```bash
# SBT output goes to console
sbt run 2>&1 | tee etl.log
```

---

## File Paths Reference

### Databricks
```
/dbfs/mnt/etl/data/input/csv/        Source CSV files
/dbfs/mnt/etl/data/input/json/       Source JSON files
/dbfs/mnt/etl/data/input/parquet/    Source Parquet files
/user/hive/warehouse/etl_db/         Hive warehouse
```

### EMR
```
s3://bucket/data/input/csv/          Source CSV files
s3://bucket/data/input/json/         Source JSON files
s3://bucket/data/input/parquet/      Source Parquet files
s3://bucket/warehouse/tables/        Table location
s3://bucket/emr-logs/                EMR cluster logs
```

### Local
```
./data/input/csv/                    Source CSV files
./data/input/json/                   Source JSON files
./data/input/parquet/                Source Parquet files
./warehouse/                         Local Hive warehouse
```

---

## Error Codes & Solutions

### FileNotFoundException
```
Cause: Source path doesn't exist
Fix: 1. Verify bucket/path exists
     2. Check permissions
     3. Verify region settings
```

### AccessDeniedException
```
Cause: IAM permissions insufficient
Fix: 1. Verify IAM role attached to cluster
     2. Check S3 bucket policy
     3. Check Glue permissions
```

### OutOfMemoryError
```
Cause: Insufficient memory for execution
Fix: 1. Increase --executor-memory
     2. Increase spark.sql.shuffle.partitions
     3. Reduce batch size
```

### UnresolvedException
```
Cause: Column not found in DataFrame
Fix: 1. Check source data schema
     2. Review transformation logic
     3. Disable inferSchema if needed
```

---

## Monitoring Metrics

### Row Counts
```scala
println(s"Source rows: ${sourceDf.count()}")
println(s"Transformed rows: ${transformedDf.count()}")
```

### Execution Time
```scala
val startTime = System.currentTimeMillis()
// ... execution ...
val duration = System.currentTimeMillis() - startTime
println(s"Execution time: ${duration}ms")
```

### Partition Info
```scala
println(s"Partitions: ${df.rdd.getNumPartitions}")
```

### Resource Usage
```bash
# Databricks
spark.sparkContext.statusTracker.getExecutorInfos.length

# EMR
yarn top
```

---

## Performance Tuning

### For Small Data (< 1 GB)
```bash
spark.sql.shuffle.partitions = 50
--executor-memory 2g
--num-executors 2
```

### For Medium Data (1-10 GB) [DEFAULT]
```bash
spark.sql.shuffle.partitions = 200
--executor-memory 8g
--num-executors 10
```

### For Large Data (10-100 GB)
```bash
spark.sql.shuffle.partitions = 500
--executor-memory 16g
--num-executors 20
```

### For Very Large Data (> 100 GB)
```bash
spark.sql.shuffle.partitions = 1000
--executor-memory 32g
--num-executors 50
spark.sql.adaptive.enabled = true
```

---

## Useful Commands

### SBT
```bash
sbt clean              # Clean build artifacts
sbt compile            # Compile Scala code
sbt run               # Run locally
sbt test              # Run tests
sbt assembly          # Build fat JAR
sbt clean assembly    # Clean and build JAR
```

### AWS CLI
```bash
aws s3 ls s3://bucket/path/             # List S3 files
aws s3 cp local.txt s3://bucket/        # Upload file
aws s3 sync ./local s3://bucket/local   # Sync directory
aws emr describe-cluster --cluster-id j-xxx  # Cluster info
aws emr describe-step --cluster-id j-xxx --step-id s-xxx  # Step status
```

### Databricks CLI
```bash
databricks configure --token             # Set up authentication
databricks fs ls dbfs:/path/            # List DBFS files
databricks fs cp file dbfs:/path/       # Upload file
databricks jobs create --json-file job.json  # Create job
databricks jobs run-now --job-id <id>   # Run job
```

---

**End of Technical Reference Guide**

