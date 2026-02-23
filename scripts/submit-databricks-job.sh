#!/bin/bash

# Databricks Job Submission Script
# Run this script to submit the ETL job to Databricks

set -e

# Configuration
DATABRICKS_WORKSPACE_URL="${DATABRICKS_WORKSPACE_URL:-https://your-workspace.cloud.databricks.com}"
DATABRICKS_TOKEN="${DATABRICKS_TOKEN:-your-token}"
JAR_PATH="${JAR_PATH:-/Workspace/Shared/jars/SparkScalaETLprojectforDatabricks_EMR-assembly-0.1.0-SNAPSHOT.jar}"
DBFS_ROOT="${DBFS_ROOT:-/dbfs/mnt/etl}"
TARGET_DB="${TARGET_DB:-etl_db}"

echo "=========================================="
echo "Databricks ETL Job Submission"
echo "=========================================="
echo "Workspace URL: ${DATABRICKS_WORKSPACE_URL}"
echo "JAR Path: ${JAR_PATH}"
echo "DBFS Root: ${DBFS_ROOT}"
echo "Target Database: ${TARGET_DB}"
echo ""

# Create job configuration
JOB_CONFIG=$(cat <<EOF
{
  "name": "SparkScalaETL-Pipeline",
  "new_cluster": {
    "spark_version": "13.3.x-scala2.12",
    "node_type_id": "i3.xlarge",
    "num_workers": 2,
    "aws_attributes": {
      "zone_id": "us-west-2a",
      "instance_profile_arn": "arn:aws:iam::YOUR_ACCOUNT_ID:instance-profile/YOUR_PROFILE"
    }
  },
  "spark_jar_task": {
    "jar_uri": "${JAR_PATH}",
    "main_class_name": "com.etl.spark.ETLPipeline"
  },
  "spark_conf": {
    "spark.sql.shuffle.partitions": "200",
    "spark.sql.adaptive.enabled": "true",
    "spark.databricks.delta.preview.enabled": "true",
    "SPARK_ENV": "databricks",
    "DBFS_ROOT": "${DBFS_ROOT}",
    "TARGET_DB": "${TARGET_DB}"
  },
  "timeout_seconds": 3600,
  "max_retries": 1
}
EOF
)

echo "Job Configuration:"
echo "${JOB_CONFIG}" | jq .

# Submit job using Databricks API
echo ""
echo "Submitting job to Databricks workspace..."

RESPONSE=$(curl -s -X POST \
  "${DATABRICKS_WORKSPACE_URL}/api/2.1/jobs/create" \
  -H "Authorization: Bearer ${DATABRICKS_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "${JOB_CONFIG}")

JOB_ID=$(echo "${RESPONSE}" | jq -r '.job_id // empty')

if [ -z "${JOB_ID}" ]; then
    echo "Failed to create job"
    echo "Response: ${RESPONSE}"
    exit 1
fi

echo ""
echo "=========================================="
echo "Job created successfully"
echo "=========================================="
echo "Job ID: ${JOB_ID}"
echo ""
echo "To run the job:"
echo "  curl -X POST ${DATABRICKS_WORKSPACE_URL}/api/2.1/jobs/run-now \\"
echo "    -H \"Authorization: Bearer ${DATABRICKS_TOKEN}\" \\"
echo "    -d '{\"job_id\": ${JOB_ID}}'"
echo ""

