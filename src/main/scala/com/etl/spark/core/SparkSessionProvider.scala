package com.etl.spark.core

import org.apache.spark.sql.SparkSession
import org.slf4j.LoggerFactory
import com.etl.spark.util.ConfigManager

object SparkSessionProvider {
  private val log = LoggerFactory.getLogger(this.getClass)
  
  def getOrCreateSession: SparkSession = {
    val environment = ConfigManager.getEnvironment
    log.info(s"Creating SparkSession for environment: $environment")
    
    val builder = SparkSession.builder()
      .appName(ConfigManager.getString("spark.appName"))
      .enableHiveSupport()
    
    if (environment == "databricks") {
      configureDatabricks(builder)
    } else if (environment == "emr") {
      configureEMR(builder)
    } else {
      configureLocal(builder)
    }
    
    val spark = builder.getOrCreate()
    
    spark.sparkContext.setLogLevel("INFO")
    log.info(s"SparkSession created successfully: ${spark.version}")
    
    spark
  }
  
  private def configureDatabricks(builder: SparkSession.Builder): SparkSession.Builder = {
    log.info("Configuring SparkSession for Databricks")
    
    builder
      .config("spark.sql.shuffle.partitions", ConfigManager.getInt("spark.sql.shuffle.partitions"))
      .config("spark.sql.adaptive.enabled", ConfigManager.getBoolean("spark.sql.adaptive.enabled"))
      .config("spark.sql.adaptive.coalescePartitions.enabled", 
              ConfigManager.getBoolean("spark.sql.adaptive.coalescePartitions.enabled"))
      .config("spark.sql.adaptive.skewJoin.enabled", 
              ConfigManager.getBoolean("spark.sql.adaptive.skewJoin.enabled"))
      .config("spark.databricks.delta.preview.enabled", true)
      .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
      .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
      .config("spark.delta.logStore.class", "org.apache.spark.sql.delta.storage.S3SingleDriverLogStore")
      .config("fs.s3.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
      .config("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
      .config("fs.s3a.aws.credentials.provider", 
              "com.amazonaws.auth.DefaultAWSCredentialsProviderChain")
      .getOrCreate()
  }
  
  private def configureEMR(builder: SparkSession.Builder): SparkSession.Builder = {
    log.info("Configuring SparkSession for AWS EMR")
    
    val metastoreType = ConfigManager.getString("hive.metastore.type", "glue")
    
    val baseConfig = builder
      .config("spark.sql.shuffle.partitions", ConfigManager.getInt("spark.sql.shuffle.partitions"))
      .config("spark.sql.adaptive.enabled", ConfigManager.getBoolean("spark.sql.adaptive.enabled"))
      .config("spark.sql.adaptive.coalescePartitions.enabled",
              ConfigManager.getBoolean("spark.sql.adaptive.coalescePartitions.enabled"))
      .config("spark.sql.adaptive.skewJoin.enabled",
              ConfigManager.getBoolean("spark.sql.adaptive.skewJoin.enabled"))
      .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
      .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
      .config("fs.s3.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
      .config("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
      .config("fs.s3a.aws.credentials.provider",
              "com.amazonaws.auth.DefaultAWSCredentialsProviderChain")
      .config("fs.s3a.connection.timeout", "120000")
      .config("fs.s3a.socket.timeout", "120000")
    
    if (metastoreType == "glue") {
      configureGlueCatalog(baseConfig)
    } else {
      configureHiveMetastore(baseConfig)
    }
  }
  
  private def configureGlueCatalog(builder: SparkSession.Builder): SparkSession.Builder = {
    log.info("Configuring AWS Glue Catalog as Metastore")
    
    builder
      .config("spark.sql.catalog.glue_catalog", "org.apache.spark.sql.glue.catalog.GlueCatalog")
      .config("spark.sql.defaultCatalog", "glue_catalog")
      .config("spark.hadoop.hive.metastore.client.factory.class",
              "com.amazonaws.glue.catalog.metastore.AWSGlueDataCatalogHiveClientFactory")
      .config("spark.hadoop.hive.metastore.warehouse.dir",
              ConfigManager.getString("hive.metastore.hms.warehouse", "s3a://my-warehouse/hive"))
      .getOrCreate()
  }
  
  private def configureHiveMetastore(builder: SparkSession.Builder): SparkSession.Builder = {
    log.info("Configuring Hive Metastore Service (HMS)")
    
    builder
      .config("spark.hadoop.hive.metastore.uris",
              ConfigManager.getString("hive.metastore.hms.thriftUri", "thrift://localhost:9083"))
      .config("spark.hadoop.hive.metastore.warehouse.dir",
              ConfigManager.getString("hive.metastore.hms.warehouse", "s3a://my-warehouse/hive"))
      .getOrCreate()
  }
  
  private def configureLocal(builder: SparkSession.Builder): SparkSession.Builder = {
    log.info("Configuring SparkSession for Local development")
    
    builder
      .master("local[*]")
      .config("spark.sql.shuffle.partitions", 4)
      .config("spark.sql.warehouse.dir", "./warehouse")
      .getOrCreate()
  }
}

