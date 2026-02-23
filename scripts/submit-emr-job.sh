#!/bin/bash

# AWS EMR Spark Submit Command Reference
# Use these commands to submit jobs directly to running EMR clusters

set -e

# Configuration
EMR_CLUSTER_ID="${1:-}"
JAR_PATH="${2:-s3://my-data-bucket/jars/SparkScalaETLprojectforDatabricks_EMR/SparkScalaETLprojectforDatabricks_EMR-assembly-0.1.0-SNAPSHOT.jar}"
S3_BUCKET="${3:-my-data-bucket}"
AWS_REGION="${4:-us-east-1}"

if [ -z "${EMR_CLUSTER_ID}" ]; then
    echo "Usage: $0 <cluster-id> [jar-path] [s3-bucket] [aws-region]"
    echo ""
    echo "Example:"
    echo "  $0 j-1ABC2DEFGHIJK"
    exit 1
fi

echo "=========================================="
echo "EMR Spark Submit - Standard ETL Job"
echo "=========================================="
echo "Cluster ID: ${EMR_CLUSTER_ID}"
echo "JAR: ${JAR_PATH}"
echo "S3 Bucket: ${S3_BUCKET}"
echo "Region: ${AWS_REGION}"
echo ""

# SSH into cluster and run spark-submit
echo "Connecting to EMR cluster and submitting job..."

aws emr add-steps \
    --cluster-id "${EMR_CLUSTER_ID}" \
    --steps Type=spark,Name="SparkScalaETL-$(date +%s)",ActionOnFailure=CONTINUE,Args=[\
        --class,com.etl.spark.ETLPipeline,\
        --master,yarn,\
        --deploy-mode,cluster,\
        --num-executors,10,\
        --executor-cores,4,\
        --executor-memory,8g,\
        --driver-memory,4g,\
        --driver-cores,2,\
        --conf,"spark.driver.maxResultSize=4g",\
        --conf,"spark.sql.shuffle.partitions=200",\
        --conf,"spark.sql.adaptive.enabled=true",\
        --conf,"spark.sql.adaptive.coalescePartitions.enabled=true",\
        --conf,"spark.sql.adaptive.skewJoin.enabled=true",\
        --conf,"spark.hadoop.fs.s3a.impl=org.apache.hadoop.fs.s3a.S3AFileSystem",\
        --conf,"spark.hadoop.fs.s3a.aws.credentials.provider=com.amazonaws.auth.DefaultAWSCredentialsProviderChain",\
        --conf,"spark.hadoop.fs.s3a.connection.timeout=120000",\
        --conf,"spark.hadoop.fs.s3a.socket.timeout=120000",\
        --conf,"spark.sql.extensions=io.delta.sql.DeltaSparkSessionExtension",\
        --conf,"spark.sql.catalog.spark_catalog=org.apache.spark.sql.delta.catalog.DeltaCatalog",\
        --conf,"SPARK_ENV=emr",\
        --conf,"S3_BUCKET=${S3_BUCKET}",\
        --conf,"AWS_REGION=${AWS_REGION}",\
        "${JAR_PATH}"\
    ] \
    --region "${AWS_REGION}"

echo ""
echo "=========================================="
echo "Job submitted successfully"
echo "=========================================="
echo ""
echo "Monitor progress:"
echo "  aws emr describe-step --cluster-id ${EMR_CLUSTER_ID} --step-id <step-id> --region ${AWS_REGION}"
echo ""

