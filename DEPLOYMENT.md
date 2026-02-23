# Deployment Guide

Complete step-by-step instructions for deploying the Spark Scala ETL pipeline to Databricks and AWS EMR.

## Prerequisites Checklist

### Databricks
- [ ] Databricks workspace access
- [ ] Databricks CLI installed and configured
- [ ] Personal access token created
- [ ] DBFS mount point configured (e.g., `/dbfs/mnt/etl`)
- [ ] Compute cluster with runtime 13.3+ (Scala 2.12)
- [ ] Data uploaded to DBFS path

### AWS EMR
- [ ] AWS CLI v2 installed and configured
- [ ] IAM permissions for EC2, EMR, S3, and Glue
- [ ] S3 bucket created for data and jars
- [ ] Key pair created for cluster access
- [ ] (Optional) Glue Catalog enabled on account

## Deployment: Databricks

### Step 1: Build the Project

```bash
cd E:\POC\ code\SparkScalaETLprojectforDatabricks_EMR
sbt clean assembly
```

JAR will be at: `target/SparkScalaETLprojectforDatabricks_EMR-assembly-0.1.0-SNAPSHOT.jar`

### Step 2: Upload Data to DBFS

**Using Databricks CLI:**
```bash
# Configure authentication
databricks configure --token

# Create directory structure
databricks fs mkdir -p dbfs:/mnt/etl/data/input/{csv,json,parquet}

# Upload sample data
databricks fs cp data/input/csv/sample_customers.csv \
  dbfs:/mnt/etl/data/input/csv/ -r

databricks fs cp data/input/json/sample_customers.json \
  dbfs:/mnt/etl/data/input/json/ -r
```

**Using Databricks Web UI:**
1. Click "Data" in left sidebar
2. Navigate to DBFS or mount point
3. Click "Upload" and select files
4. Create folder structure: `mnt/etl/data/input/{csv,json,parquet}`

### Step 3: Upload JAR to Databricks

```bash
databricks fs mkdir -p dbfs:/Workspace/Shared/jars/
databricks fs cp target/SparkScalaETLprojectforDatabricks_EMR-assembly-0.1.0-SNAPSHOT.jar \
  dbfs:/Workspace/Shared/jars/
```

### Step 4: Create or Update Configuration

**Option A: Using Notebook Widgets**
```scala
// In notebook, set parameters:
val sourceFormat = "csv"
val sourcePath = "/dbfs/mnt/etl/data/input/csv"
val tableName = "customer_csv"
val writeMode = "overwrite"
val trimColumns = "name,email"
```

**Option B: Use application.conf**

Update `src/main/resources/application.conf`:
```hocon
environment = "databricks"
cloud.dbfs.rootPath = "/dbfs/mnt/etl"
target.database = "etl_prod"
source.csv.path = "data/input/csv"
```

Recompile and upload new JAR.

### Step 5: Create Cluster

In Databricks workspace:
1. Click "Compute" in left sidebar
2. Click "Create Cluster"
3. Configure:
   - **Cluster name**: `spark-etl-cluster`
   - **Databricks Runtime**: 13.3 or later (Scala 2.12)
   - **Worker type**: 8GB Memory, 2 Cores minimum
   - **Min workers**: 1
   - **Max workers**: 4
4. Click "Create Cluster"
5. Wait for cluster to start

### Step 6: Create Job

**Via Databricks Web UI:**

1. Click "Jobs" in left sidebar
2. Click "Create Job"
3. Configure:
   - **Task name**: `ETL-Spark-Submit`
   - **Type**: Spark submit
   - **Main class**: `com.etl.spark.ETLPipeline`
   - **Parameters**: (leave empty if using application.conf)
   - **Cluster**: Select `spark-etl-cluster`
   - **JAR path**: `dbfs:/Workspace/Shared/jars/SparkScalaETLprojectforDatabricks_EMR-assembly-0.1.0-SNAPSHOT.jar`
4. Click "Create"

**Via Databricks CLI:**

```bash
cat > job_config.json << EOF
{
  "name": "SparkScalaETL-Pipeline",
  "new_cluster": {
    "spark_version": "13.3.x-scala2.12",
    "node_type_id": "i3.xlarge",
    "num_workers": 2,
    "aws_attributes": {
      "zone_id": "us-west-2a"
    }
  },
  "spark_jar_task": {
    "jar_uri": "dbfs:/Workspace/Shared/jars/SparkScalaETLprojectforDatabricks_EMR-assembly-0.1.0-SNAPSHOT.jar",
    "main_class_name": "com.etl.spark.ETLPipeline"
  },
  "timeout_seconds": 3600,
  "max_concurrent_runs": 1
}
EOF

databricks jobs create --json-file job_config.json
```

### Step 7: Run Job

**Via Web UI:**
1. Go to "Jobs"
2. Click job name
3. Click "Run Now"
4. Monitor progress in "Runs" tab

**Via CLI:**
```bash
JOB_ID=$(databricks jobs list | grep "SparkScalaETL-Pipeline" | awk '{print $1}')
databricks jobs run-now --job-id $JOB_ID
```

### Step 8: Verify Results

```sql
-- In Databricks SQL
SELECT COUNT(*) FROM etl_prod.customer_csv;
SELECT * FROM etl_prod.customer_json LIMIT 5;
DESCRIBE EXTENDED etl_prod.customer_parquet;
```

## Deployment: AWS EMR

### Step 1: Build the Project

```bash
cd E:\POC\ code\SparkScalaETLprojectforDatabricks_EMR
sbt clean assembly
```

### Step 2: Create S3 Bucket and Upload Files

```bash
# Set variables
AWS_REGION=us-east-1
S3_BUCKET=my-etl-bucket-$(date +%s)
JAR_NAME=SparkScalaETLprojectforDatabricks_EMR-assembly-0.1.0-SNAPSHOT.jar

# Create bucket
aws s3 mb s3://${S3_BUCKET} --region ${AWS_REGION}

# Create directory structure
aws s3api put-object --bucket ${S3_BUCKET} --key data/input/csv/
aws s3api put-object --bucket ${S3_BUCKET} --key data/input/json/
aws s3api put-object --bucket ${S3_BUCKET} --key jars/

# Upload files
aws s3 cp data/input/csv/sample_customers.csv \
  s3://${S3_BUCKET}/data/input/csv/ --region ${AWS_REGION}

aws s3 cp data/input/json/sample_customers.json \
  s3://${S3_BUCKET}/data/input/json/ --region ${AWS_REGION}

aws s3 cp target/${JAR_NAME} \
  s3://${S3_BUCKET}/jars/ --region ${AWS_REGION}

echo "S3_BUCKET=${S3_BUCKET}" >> ~/.bashrc
```

### Step 3: Create EMR Cluster

**Via AWS Console:**

1. Go to EMR > Clusters > Create cluster
2. Configure:
   - **Cluster name**: `spark-etl-cluster`
   - **Release label**: `emr-7.1.0`
   - **Applications**: Spark, Hadoop, Hive
   - **Instance type**: `m5.xlarge`
   - **Number of instances**: 3 (1 master, 2 core)
   - **EC2 key pair**: Select your key pair
   - **S3 folder**: `s3://${S3_BUCKET}/emr-logs/`
3. Click "Create cluster"
4. Wait for cluster state to be "Waiting"

**Via AWS CLI:**

```bash
aws emr create-cluster \
  --name spark-etl-cluster \
  --release-label emr-7.1.0 \
  --applications Name=Spark Name=Hadoop Name=Hive \
  --instance-type m5.xlarge \
  --instance-count 3 \
  --ec2-attributes KeyName=your-key-pair \
  --log-uri s3://${S3_BUCKET}/emr-logs/ \
  --region ${AWS_REGION}
```

### Step 4: Submit Job

**Option A: Add Step to Running Cluster**

```bash
CLUSTER_ID=j-1ABC2DEFGHIJK  # From cluster details
JAR_S3=s3://${S3_BUCKET}/jars/${JAR_NAME}

aws emr add-steps \
  --cluster-id ${CLUSTER_ID} \
  --steps Type=spark,Name="SparkScalaETL",ActionOnFailure=CONTINUE,\
    Args=[--class,com.etl.spark.ETLPipeline,--master,yarn,\
    --deploy-mode,cluster,--num-executors,10,--executor-cores,4,\
    --executor-memory,8g,--driver-memory,4g,\
    --conf,spark.sql.shuffle.partitions=200,\
    --conf,spark.sql.adaptive.enabled=true,\
    --conf,SPARK_ENV=emr,--conf,S3_BUCKET=${S3_BUCKET},\
    --conf,AWS_REGION=${AWS_REGION},\
    ${JAR_S3}] \
  --region ${AWS_REGION}
```

**Option B: SSH and Run spark-submit**

```bash
# Get master node DNS
MASTER_DNS=$(aws emr describe-cluster --cluster-id ${CLUSTER_ID} \
  --query 'Cluster.MasterPublicDNSName' --output text \
  --region ${AWS_REGION})

# SSH to cluster
ssh -i your-key-pair.pem hadoop@${MASTER_DNS}

# Run spark-submit
spark-submit \
  --class com.etl.spark.ETLPipeline \
  --master yarn \
  --deploy-mode cluster \
  --num-executors 10 \
  --executor-cores 4 \
  --executor-memory 8g \
  --driver-memory 4g \
  --conf spark.sql.shuffle.partitions=200 \
  --conf SPARK_ENV=emr \
  --conf S3_BUCKET=${S3_BUCKET} \
  --conf AWS_REGION=${AWS_REGION} \
  s3://${S3_BUCKET}/jars/${JAR_NAME}
```

### Step 5: Monitor Job

```bash
# Check step status
aws emr describe-step \
  --cluster-id ${CLUSTER_ID} \
  --step-id s-1ABC2DEFGHIJK \
  --region ${AWS_REGION}

# Check cluster logs in S3
aws s3 ls s3://${S3_BUCKET}/emr-logs/ --recursive --human-readable

# SSH and check yarn logs
yarn logs -applicationId <app_id>
```

### Step 6: Verify Results

```bash
# SSH to cluster
ssh -i your-key-pair.pem hadoop@${MASTER_DNS}

# Launch Hive
hive

# Query tables
SELECT COUNT(*) FROM etl_db.customer_csv;
SELECT * FROM etl_db.customer_json LIMIT 5;
```

## Post-Deployment Verification

### Databricks

```sql
-- List databases and tables
SHOW DATABASES;
SHOW TABLES IN etl_prod;

-- Verify data loaded
SELECT COUNT(*) as row_count, MIN(load_dt), MAX(load_dt) 
FROM etl_prod.customer_csv;

-- Check table properties
DESCRIBE EXTENDED etl_prod.customer_csv;

-- Verify Delta properties
SELECT * FROM etl_prod.customer_delta_history 
LIMIT 1;
```

### EMR

```bash
# SSH to cluster
ssh -i key.pem hadoop@${MASTER_DNS}

# Check HDFS
hdfs dfs -ls /user/hive/warehouse/etl_db.db/

# Check S3 location
aws s3 ls s3://${S3_BUCKET}/warehouse/tables/

# Hive queries
hive -e "SHOW DATABASES;"
hive -e "SELECT COUNT(*) FROM etl_db.customer_csv;"
```

## Cleanup

### Databricks
```bash
# Delete job
databricks jobs delete --job-id <job-id>

# Delete cluster (done via UI or CLI)
databricks clusters delete --cluster-id <cluster-id>

# Delete DBFS files (optional)
databricks fs rm -r dbfs:/mnt/etl/
```

### AWS EMR
```bash
# Terminate cluster
aws emr terminate-clusters --cluster-ids ${CLUSTER_ID} --region ${AWS_REGION}

# Delete S3 bucket (WARNING: irreversible)
aws s3 rm s3://${S3_BUCKET}/ --recursive
aws s3 rb s3://${S3_BUCKET}
```

## Troubleshooting Deployment Issues

See TROUBLESHOOTING.md for common issues and solutions.

