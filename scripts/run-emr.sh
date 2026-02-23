#!/bin/bash

set -e

# Spark Scala ETL - AWS EMR Deployment Script
# This script compiles, packages, and submits the Spark job to AWS EMR

PROJECT_HOME="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_NAME="SparkScalaETLprojectforDatabricks_EMR"
JAR_NAME="${PROJECT_NAME}-assembly-0.1.0-SNAPSHOT.jar"
BUILD_DIR="${PROJECT_HOME}/target"

# Configuration
EMR_CLUSTER_ID="${EMR_CLUSTER_ID:-}"
S3_BUCKET="${S3_BUCKET:-my-data-bucket}"
AWS_REGION="${AWS_REGION:-us-east-1}"
S3_LOGS="${S3_LOGS:-s3://${S3_BUCKET}/emr-logs}"
SPARK_SUBMIT_ARGS="${SPARK_SUBMIT_ARGS:-}"

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Spark Scala ETL - EMR Deployment${NC}"
echo -e "${GREEN}========================================${NC}"

# Step 1: Build the project
echo -e "${YELLOW}[1/4] Building Scala project...${NC}"
cd "${PROJECT_HOME}"
if ! sbt clean assembly; then
    echo -e "${RED}Build failed${NC}"
    exit 1
fi
echo -e "${GREEN}Build successful${NC}"

# Step 2: Verify JAR file
if [ ! -f "${BUILD_DIR}/${JAR_NAME}" ]; then
    echo -e "${RED}JAR file not found: ${BUILD_DIR}/${JAR_NAME}${NC}"
    exit 1
fi
echo -e "${GREEN}JAR file created: ${BUILD_DIR}/${JAR_NAME}${NC}"

# Step 3: Upload JAR to S3
echo -e "${YELLOW}[2/4] Uploading JAR to S3...${NC}"
JAR_S3_PATH="s3://${S3_BUCKET}/jars/${PROJECT_NAME}/${JAR_NAME}"
aws s3 cp "${BUILD_DIR}/${JAR_NAME}" "${JAR_S3_PATH}" --region "${AWS_REGION}"
echo -e "${GREEN}JAR uploaded to: ${JAR_S3_PATH}${NC}"

# Step 4: Create bootstrap script for EMR
echo -e "${YELLOW}[3/4] Creating EMR bootstrap script...${NC}"
BOOTSTRAP_SCRIPT="${PROJECT_HOME}/scripts/bootstrap-emr.sh"
mkdir -p "$(dirname "${BOOTSTRAP_SCRIPT}")"

cat > "${BOOTSTRAP_SCRIPT}" << 'EOF'
#!/bin/bash
set -e

echo "Installing ETL dependencies on EMR cluster..."

# Update system packages
sudo yum update -y

# Install additional Python packages if needed
sudo pip install boto3 pyspark

# Verify Hadoop and Spark are available
hadoop version
spark-submit --version

echo "Bootstrap script completed successfully"
EOF

chmod +x "${BOOTSTRAP_SCRIPT}"
echo -e "${GREEN}Bootstrap script created${NC}"

# Step 5: Submit Spark job to EMR
echo -e "${YELLOW}[4/4] Submitting Spark job to EMR...${NC}"

if [ -z "${EMR_CLUSTER_ID}" ]; then
    echo -e "${YELLOW}No EMR_CLUSTER_ID provided. Use one of these options:${NC}"
    echo ""
    echo "Option A: Set environment variable and rerun:"
    echo "  export EMR_CLUSTER_ID=<cluster-id>"
    echo "  bash ${PROJECT_HOME}/scripts/run-emr.sh"
    echo ""
    echo "Option B: Run spark-submit directly on cluster:"
    echo "  spark-submit \\"
    echo "    --class com.etl.spark.ETLPipeline \\"
    echo "    --master yarn \\"
    echo "    --deploy-mode cluster \\"
    echo "    --num-executors 10 \\"
    echo "    --executor-cores 4 \\"
    echo "    --executor-memory 8g \\"
    echo "    --driver-memory 4g \\"
    echo "    --conf spark.sql.shuffle.partitions=200 \\"
    echo "    --conf spark.sql.adaptive.enabled=true \\"
    echo "    --conf spark.hadoop.fs.s3a.impl=org.apache.hadoop.fs.s3a.S3AFileSystem \\"
    echo "    --conf spark.hadoop.fs.s3a.aws.credentials.provider=com.amazonaws.auth.DefaultAWSCredentialsProviderChain \\"
    echo "    --conf SPARK_ENV=emr \\"
    echo "    --conf S3_BUCKET=${S3_BUCKET} \\"
    echo "    --conf AWS_REGION=${AWS_REGION} \\"
    echo "    s3a://${S3_BUCKET}/jars/${PROJECT_NAME}/${JAR_NAME}"
    echo ""
    exit 0
fi

# Submit job using EMR step
echo "Submitting job to EMR cluster: ${EMR_CLUSTER_ID}"

JOB_NAME="${PROJECT_NAME}-$(date +%s)"

aws emr add-steps \
    --cluster-id "${EMR_CLUSTER_ID}" \
    --steps Type=spark,Name="${JOB_NAME}",ActionOnFailure=CONTINUE,Args=[--class,com.etl.spark.ETLPipeline,--master,yarn,--deploy-mode,cluster,--num-executors,10,--executor-cores,4,--executor-memory,8g,--driver-memory,4g,--conf,spark.sql.shuffle.partitions=200,--conf,spark.sql.adaptive.enabled=true,--conf,spark.hadoop.fs.s3a.impl=org.apache.hadoop.fs.s3a.S3AFileSystem,--conf,spark.hadoop.fs.s3a.aws.credentials.provider=com.amazonaws.auth.DefaultAWSCredentialsProviderChain,--conf,SPARK_ENV=emr,--conf,S3_BUCKET=${S3_BUCKET},--conf,AWS_REGION=${AWS_REGION},"${JAR_S3_PATH}"] \
    --region "${AWS_REGION}"

echo -e "${GREEN}Job submitted successfully${NC}"
echo -e "${GREEN}Job Name: ${JOB_NAME}${NC}"
echo -e "${GREEN}Cluster ID: ${EMR_CLUSTER_ID}${NC}"
echo ""
echo -e "${YELLOW}Monitor job progress:${NC}"
echo "  aws emr describe-cluster --cluster-id ${EMR_CLUSTER_ID} --region ${AWS_REGION}"
echo ""

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Deployment Complete${NC}"
echo -e "${GREEN}========================================${NC}"

