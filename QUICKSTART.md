# Quick Start Guide

## 5-Minute Setup

### 1. Build the Project
```bash
cd E:\POC\ code\SparkScalaETLprojectforDatabricks_EMR
sbt clean assembly
```

### 2. Configure Environment Variables

**For Databricks:**
```bash
export SPARK_ENV=databricks
export DBFS_ROOT=/dbfs/mnt/etl
export TARGET_DB=etl_prod
```

**For AWS EMR:**
```bash
export SPARK_ENV=emr
export S3_BUCKET=my-etl-bucket
export AWS_REGION=us-east-1
export METASTORE_TYPE=glue
```

### 3. Prepare Data

Create sample data in your cloud storage:

**CSV (s3://bucket/data/input/csv/ or /dbfs/mnt/etl/data/input/csv/)**
```csv
customer_id,customer_name,email,phone,address,city,state,zip_code,country,registration_date,is_active,amount
1,John Doe,john@example.com,555-1234,123 Main St,Boston,MA,02101,USA,2024-01-15,true,1500.00
2,Jane Smith,jane@example.com,555-5678,456 Oak Ave,New York,NY,10001,USA,2024-01-16,true,2500.00
3,Bob Johnson,bob@example.com,555-9012,789 Pine Rd,Los Angeles,CA,90001,USA,2024-01-17,false,500.00
```

**JSON (s3://bucket/data/input/json/ or /dbfs/mnt/etl/data/input/json/)**
```json
{"customer_id": 4, "customer_name": "Alice Brown", "email": "alice@example.com", "phone": "555-3456", "address": "321 Elm St", "city": "Chicago", "state": "IL", "zip_code": "60601", "country": "USA", "registration_date": "2024-01-18", "is_active": true, "amount": 3000.00}
{"customer_id": 5, "customer_name": "Charlie Davis", "email": "charlie@example.com", "phone": "555-7890", "address": "654 Birch Ln", "city": "Houston", "state": "TX", "zip_code": "77001", "country": "USA", "registration_date": "2024-01-19", "is_active": true, "amount": 1800.00}
```

### 4. Run on Databricks

```bash
# Option A: Upload JAR and run as job
databricks fs cp target/SparkScalaETLprojectforDatabricks_EMR-assembly-0.1.0-SNAPSHOT.jar \
  dbfs:/Workspace/Shared/jars/

bash scripts/submit-databricks-job.sh

# Option B: Use interactive notebook
# Import notebooks/ETLPipeline.scala into Databricks workspace
# Configure parameters and run cells
```

### 5. Run on AWS EMR

```bash
# Upload JAR to S3
aws s3 cp target/SparkScalaETLprojectforDatabricks_EMR-assembly-0.1.0-SNAPSHOT.jar \
  s3://my-etl-bucket/jars/

# Submit to cluster
export EMR_CLUSTER_ID=j-1ABC2DEFGHIJK
bash scripts/run-emr.sh

# Or direct spark-submit
spark-submit \
  --class com.etl.spark.ETLPipeline \
  --master yarn \
  --deploy-mode cluster \
  --num-executors 10 \
  --executor-cores 4 \
  --executor-memory 8g \
  --conf SPARK_ENV=emr \
  --conf S3_BUCKET=my-etl-bucket \
  s3a://my-etl-bucket/jars/SparkScalaETLprojectforDatabricks_EMR-assembly-0.1.0-SNAPSHOT.jar
```

## Verification

### Check Databricks Tables
```sql
SELECT * FROM etl_db.customer_csv LIMIT 10;
SELECT * FROM etl_db.customer_json LIMIT 10;
SELECT * FROM etl_db.customer_parquet LIMIT 10;
```

### Check EMR Tables
```bash
# SSH to cluster
aws emr ssh --cluster-id j-1ABC2DEFGHIJK

# Run Hive queries
hive -e "SELECT * FROM etl_db.customer_csv LIMIT 10;"
hive -e "SELECT COUNT(*) FROM etl_db.customer_json;"
```

### Validate Load
```scala
// In Databricks notebook or EMR spark-shell
spark.sql("SHOW TABLES IN etl_db").show()
spark.sql("SELECT COUNT(*), MIN(load_dt), MAX(load_dt) FROM etl_db.customer_csv").show()
```

## Configuration Override Examples

### Custom Source Paths
```bash
export S3_BUCKET=my-custom-bucket
export DBFS_ROOT=/dbfs/mnt/custom/etl
```

### Change Write Mode
```bash
export WRITE_MODE=append  # Instead of default overwrite
```

### Enable Debug Logging
```bash
export LOG_LEVEL=DEBUG
```

### Change Target Database
```bash
export TARGET_DB=analytics
```

## Common Commands

### SBT
```bash
sbt clean              # Clean build artifacts
sbt compile            # Compile Scala code
sbt test               # Run tests
sbt assembly           # Build fat JAR
sbt run               # Run locally (requires data)
```

### AWS CLI
```bash
# List S3 files
aws s3 ls s3://my-bucket/data/input/ --recursive

# Upload JAR
aws s3 cp target/*.jar s3://my-bucket/jars/

# Monitor EMR step
aws emr describe-step --cluster-id j-xxx --step-id s-xxx
```

### Databricks CLI
```bash
# Configure workspace
databricks configure --token

# Upload files
databricks fs cp local/path dbfs:/path

# Run job
databricks jobs run-now --job-id 123
```

## Next Steps

1. **Customize transformations**: Edit `src/main/scala/com/etl/spark/transform/Transformer.scala`
2. **Add new data sources**: Extend `CloudFileReader` with additional formats
3. **Implement tests**: Add test cases in `src/test/scala/`
4. **Setup CI/CD**: Create GitHub Actions or Jenkins pipeline
5. **Monitor jobs**: Configure CloudWatch (AWS) or Databricks monitoring

