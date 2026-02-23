# Troubleshooting Guide

Common issues and solutions when deploying and running the Spark Scala ETL pipeline.

## Build Issues

### Issue: SBT not found
```
sbt: command not found
```

**Solution:**
1. Download from https://www.scala-sbt.org/download.html
2. Extract and add to PATH
3. Verify: `sbt sbtVersion`

### Issue: Scala version mismatch
```
error: value foreach is not a member of scala.collection.Iterable
```

**Solution:**
- Verify Scala 2.12.18 in build.sbt
- Run `sbt clean`
- Delete ~/.sbt and ~/.ivy2 caches
- Recompile: `sbt compile`

### Issue: Dependency resolution fails
```
Error downloading org.apache.spark:spark-core_2.12:3.5.0
```

**Solution:**
1. Check internet connectivity
2. Verify Maven repositories are accessible
3. Use proxy if behind corporate firewall:
   ```bash
   sbt -Dhttp.proxyHost=proxy.example.com \
       -Dhttp.proxyPort=8080 \
       clean compile
   ```

### Issue: Out of memory during build
```
java.lang.OutOfMemoryError: Java heap space
```

**Solution:**
```bash
export SBT_OPTS="-Xmx4g -XX:+UseG1GC"
sbt assembly
```

## Databricks Deployment Issues

### Issue: DBFS path not found
```
FileNotFoundException: dbfs:/mnt/etl/data/input/csv not found
```

**Solution:**
1. Verify mount point exists: `%fs ls /mnt/etl/`
2. Check data uploaded: `%fs ls /mnt/etl/data/input/csv/`
3. Update `application.conf`:
   ```hocon
   cloud.dbfs.rootPath = "/dbfs/mnt/etl"  # or correct path
   ```

### Issue: JAR not found when submitting job
```
FileNotFoundException: dbfs:/Workspace/Shared/jars/XXX.jar
```

**Solution:**
1. Upload JAR: `databricks fs cp target/*.jar dbfs:/Workspace/Shared/jars/`
2. Verify upload: `databricks fs ls dbfs:/Workspace/Shared/jars/`
3. Use exact JAR name in job configuration

### Issue: Table creation fails
```
AnalysisException: Cannot create table etl_db.customer_csv
```

**Solution:**
1. Ensure database exists:
   ```sql
   CREATE DATABASE IF NOT EXISTS etl_db;
   ```
2. Check table doesn't exist or use `CREATE TABLE IF NOT EXISTS`
3. Verify write permissions in warehouse location

### Issue: Metastore connection error
```
IOException: Cannot create a login context with the provided parameters
```

**Solution:**
- This is usually expected for HMS; switch to Glue Catalog or ignore
- Set in job config:
  ```hocon
  hive.metastore.type = "glue"
  ```

### Issue: Notebook timeout
```
ExecutionTimeoutException
```

**Solution:**
1. Increase notebook timeout (default 3600 seconds)
2. Increase cluster size: add more workers
3. Reduce data size for testing
4. Optimize queries with `EXPLAIN`

## AWS EMR Issues

### Issue: Cluster creation fails
```
Error creating cluster: insufficient capacity
```

**Solution:**
1. Try different instance type: `m6i.xlarge`, `m5.2xlarge`
2. Try different availability zone
3. Reduce instance count temporarily
4. Request capacity increase from AWS

### Issue: S3 access denied
```
AccessDenied: Access Denied
```

**Solution:**
1. Verify IAM role has S3 permissions:
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Allow",
         "Action": ["s3:*"],
         "Resource": ["arn:aws:s3:::my-bucket/*"]
       }
     ]
   }
   ```
2. Check bucket policy allows EMR access
3. Verify bucket is in same region

### Issue: Glue Catalog not accessible
```
java.lang.RuntimeException: Unable to instantiate org.apache.hadoop.hive.metastore.HiveMetaStoreClient
```

**Solution:**
1. Enable Glue Catalog in metastore:
   ```bash
   aws glue put-data-catalog-encryption-settings ...
   ```
2. Update Spark config:
   ```
   spark.hadoop.hive.metastore.client.factory.class=\
     com.amazonaws.glue.catalog.metastore.AWSGlueDataCatalogHiveClientFactory
   ```
3. Verify IAM role has Glue permissions

### Issue: Spark job fails in YARN
```
ApplicationMaster failed with status: FAILED
```

**Check logs:**
```bash
yarn logs -applicationId application_xxx -log_files stdout
```

**Common causes:**
- Memory: Increase `--executor-memory`
- Timeout: Increase `spark.network.timeout`
- Jar not found: Verify S3 path

### Issue: S3A read timeouts
```
java.io.EOFException: Unexpected end of stream
```

**Solution:**
Update spark-submit with S3A timeouts:
```bash
--conf spark.hadoop.fs.s3a.connection.timeout=120000 \
--conf spark.hadoop.fs.s3a.socket.timeout=120000 \
--conf spark.hadoop.fs.s3a.threads.max=10
```

Or in `application.conf`:
```hocon
spark {
  hadoop {
    "fs.s3a.connection.timeout" = 120000
    "fs.s3a.socket.timeout" = 120000
    "fs.s3a.threads.max" = 10
  }
}
```

## Data Issues

### Issue: CSV parsing errors
```
Malformed CSV record, expected 12 fields but got 15
```

**Solution:**
1. Check CSV delimiter and quoting:
   ```hocon
   source.csv.delimiter = ","
   source.csv.quote = "\""
   source.csv.escape = "\\"
   ```
2. Use `inferSchema = false` and specify schema explicitly
3. Check for embedded newlines in data

### Issue: JSON parsing fails
```
SparkException: Could not parse the root element
```

**Solution:**
1. Verify JSON is valid: `jq < data.json`
2. Enable multiLine if records span lines:
   ```hocon
   source.json.multiLine = true
   ```
3. Check encoding (UTF-8 expected)

### Issue: Schema mismatch
```
AnalysisException: Mismatch between struct field
```

**Solution:**
1. Disable schema casting temporarily:
   ```hocon
   transformations.castSchema = false
   ```
2. Review actual schema:
   ```scala
   df.printSchema()
   ```
3. Update casting rules in config

### Issue: Out of memory during transformation
```
OutOfMemoryError: GC overhead limit exceeded
```

**Solution:**
1. Increase executor memory: `--executor-memory 16g`
2. Reduce partition size (increase partition count)
3. Avoid broadcasting large DataFrames
4. Use persist() strategically

## Performance Issues

### Issue: Job runs slow
```
Completed all tasks in 5 hours
```

**Solution:**
1. Check task distribution:
   ```scala
   df.rdd.getNumPartitions  // Should be ~200
   ```
2. Increase partitions:
   ```hocon
   spark.sql.shuffle.partitions = 400
   ```
3. Enable adaptive execution:
   ```hocon
   spark.sql.adaptive.enabled = true
   ```
4. Use Delta Lake for repeated queries

### Issue: Skewed data (some tasks slow)
```
Task 1 finished in 2 min but Task 5 still running after 30 min
```

**Solution:**
1. Enable skew join handling:
   ```hocon
   spark.sql.adaptive.skewJoin.enabled = true
   ```
2. Check for data skew:
   ```scala
   df.groupBy("customer_id").count().describe().show()
   ```
3. Repartition before join:
   ```scala
   df.repartition($"customer_id")
   ```

### Issue: Slow S3 reads (EMR)
```
S3 bandwidth appears bottlenecked
```

**Solution:**
1. Increase S3A threads:
   ```bash
   --conf spark.hadoop.fs.s3a.threads.max=20
   ```
2. Use S3 Select if supported
3. Copy data to EBS for processing
4. Use regional endpoint

## Logging & Debugging

### Enable Debug Logging

**Databricks:**
```scala
// In notebook
spark.sparkContext.setLogLevel("DEBUG")
```

**EMR:**
```bash
spark-submit \
  --conf spark.driver.extraJavaOptions="-Dorg.apache.spark.log.level=DEBUG" \
  ...
```

**Configuration:**
```hocon
logging.level = "DEBUG"
```

### Capture Detailed Logs

**Databricks:**
- Job runs show logs directly
- Export logs: Runs → Details → View logs

**EMR:**
```bash
# SSH to master
ssh -i key.pem hadoop@master-dns

# Check Spark logs
ls /var/log/spark/

# Check Yarn logs
yarn logs -applicationId application_xxx

# Check S3 for archived logs
aws s3 ls s3://my-bucket/emr-logs/ --recursive
```

### Monitor Resources

**Databricks:**
```scala
// In notebook
val df = spark.read.parquet(...)
println(s"Partition count: ${df.rdd.getNumPartitions}")
println(s"Executor count: ${spark.sparkContext.statusTracker.getExecutorInfos.length}")
```

**EMR:**
- Web UI: `http://master-dns:8088` (YARN)
- Web UI: `http://master-dns:8080` (Spark if available)

## Getting Help

### Check Documentation
1. README.md - Overview and quick start
2. QUICKSTART.md - 5-minute setup
3. ARCHITECTURE.md - Design decisions
4. DEPLOYMENT.md - Step-by-step deployment

### Enable Verbose Output

```bash
# Local
sbt -v clean compile run

# EMR
spark-submit --verbose ...

# Databricks notebook
spark.conf.set("spark.sql.debug.maxToStringFields", "100")
```

### Collect Diagnostics

```bash
# Gather system info
uname -a
java -version
sbt sbtVersion

# Gather config
cat src/main/resources/application.conf

# Gather logs
tar -czf etl-logs.tar.gz logs/ target/

# Share for support
# (redact credentials and sensitive paths)
```

## Common Fix Checklist

- [ ] Check Java/Scala/SBT versions match build.sbt
- [ ] Verify cloud credentials and permissions
- [ ] Confirm data paths exist in cloud storage
- [ ] Check network connectivity and firewalls
- [ ] Review Spark and Hadoop logs
- [ ] Verify configuration file syntax (HOCON)
- [ ] Test with smaller data subset
- [ ] Increase memory and timeouts
- [ ] Clear caches and rebuild
- [ ] Check for known issues in Spark/Hadoop release notes

