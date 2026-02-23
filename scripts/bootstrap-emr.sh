#!/bin/bash

# EMR Cluster Bootstrap Script
# This script is executed when EMR cluster starts
# Use this for installing dependencies and configuring cluster

#!/bin/bash
set -e

echo "=============================================="
echo "EMR Cluster Bootstrap Script"
echo "=============================================="

# Update system packages
echo "Updating system packages..."
sudo yum update -y

# Install development tools
echo "Installing development tools..."
sudo yum install -y git maven git-core

# Configure S3 access
echo "Configuring S3 access..."
cat > /tmp/s3_config.sh << 'EOF'
#!/bin/bash

# Add S3A configuration to Spark defaults
cat >> /etc/spark/conf/spark-defaults.conf << 'SPARKEOF'

# S3A Configuration
spark.hadoop.fs.s3a.impl=org.apache.hadoop.fs.s3a.S3AFileSystem
spark.hadoop.fs.s3a.aws.credentials.provider=com.amazonaws.auth.DefaultAWSCredentialsProviderChain
spark.hadoop.fs.s3a.connection.timeout=120000
spark.hadoop.fs.s3a.socket.timeout=120000
spark.hadoop.fs.s3a.threads.max=10
spark.hadoop.fs.s3a.threads.core=4

# Delta Lake Configuration
spark.sql.extensions=io.delta.sql.DeltaSparkSessionExtension
spark.sql.catalog.spark_catalog=org.apache.spark.sql.delta.catalog.DeltaCatalog

# AWS Glue Catalog Configuration (optional)
spark.hadoop.hive.metastore.client.factory.class=com.amazonaws.glue.catalog.metastore.AWSGlueDataCatalogHiveClientFactory

SPARKEOF

echo "S3A configuration added to Spark defaults"
EOF

sudo bash /tmp/s3_config.sh

# Configure Hadoop settings
echo "Configuring Hadoop settings..."
cat >> /etc/hadoop/conf/hdfs-site.xml << 'HADOOPEOF'
    <property>
        <name>fs.s3a.aws.credentials.provider</name>
        <value>com.amazonaws.auth.DefaultAWSCredentialsProviderChain</value>
    </property>
    <property>
        <name>fs.s3a.connection.timeout</name>
        <value>120000</value>
    </property>
    <property>
        <name>fs.s3a.socket.timeout</name>
        <value>120000</value>
    </property>
HADOOPEOF

# Download and install AWS Glue libraries if needed
echo "Installing AWS Glue dependencies..."
aws s3 cp s3://aws-glue-scripts-${AWS_REGION}/latest/install-glue-libs.sh . || true
bash install-glue-libs.sh || true

# Verify Spark is available
echo "Verifying Spark installation..."
spark-submit --version

# Create ETL working directory
echo "Creating ETL working directory..."
mkdir -p /mnt/emr-etl/logs
mkdir -p /mnt/emr-etl/data

# Download JAR from S3 (optional)
if [ ! -z "${SPARK_JAR_S3_PATH}" ]; then
    echo "Downloading Spark JAR from S3..."
    aws s3 cp "${SPARK_JAR_S3_PATH}" /mnt/emr-etl/
fi

echo "=============================================="
echo "Bootstrap script completed successfully"
echo "=============================================="

# Log completion
date >> /mnt/emr-etl/logs/bootstrap.log
echo "Bootstrap completed at $(date)" >> /mnt/emr-etl/logs/bootstrap.log

