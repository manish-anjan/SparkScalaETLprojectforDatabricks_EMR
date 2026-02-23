package com.etl.spark.io

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.slf4j.LoggerFactory
import com.etl.spark.util.ConfigManager

class CloudFileReader(spark: SparkSession) {
  private val log = LoggerFactory.getLogger(this.getClass)
  
  def readCSV(path: String): DataFrame = {
    val fullPath = normalizePath(path)
    log.info(s"Reading CSV from: $fullPath")
    
    spark.read
      .format("csv")
      .option("header", ConfigManager.getBoolean("source.csv.header", true))
      .option("inferSchema", ConfigManager.getBoolean("source.csv.inferSchema", true))
      .option("delimiter", ConfigManager.getString("source.csv.delimiter", ","))
      .option("nullValue", ConfigManager.getString("source.csv.nullValue", ""))
      .option("nanValue", ConfigManager.getString("source.csv.nanValue", "NaN"))
      .load(fullPath)
  }
  
  def readJSON(path: String): DataFrame = {
    val fullPath = normalizePath(path)
    log.info(s"Reading JSON from: $fullPath")
    
    spark.read
      .format("json")
      .option("multiLine", ConfigManager.getBoolean("source.json.multiLine", false))
      .load(fullPath)
  }
  
  def readParquet(path: String): DataFrame = {
    val fullPath = normalizePath(path)
    log.info(s"Reading Parquet from: $fullPath")
    
    spark.read
      .format("parquet")
      .load(fullPath)
  }
  
  def readDelta(path: String): DataFrame = {
    val fullPath = normalizePath(path)
    log.info(s"Reading Delta from: $fullPath")
    
    spark.read
      .format("delta")
      .load(fullPath)
  }
  
  def readGeneric(path: String, format: String): DataFrame = {
    val fullPath = normalizePath(path)
    log.info(s"Reading $format from: $fullPath")
    
    format.toLowerCase match {
      case "csv" => readCSV(path)
      case "json" => readJSON(path)
      case "parquet" => readParquet(path)
      case "delta" => readDelta(path)
      case _ =>
        log.warn(s"Unknown format: $format, attempting generic read")
        spark.read.load(fullPath)
    }
  }
  
  private def normalizePath(path: String): String = {
    val environment = ConfigManager.getEnvironment
    
    environment match {
      case "databricks" =>
        if (path.startsWith("/")) {
          if (path.startsWith("/dbfs/")) path else s"/dbfs$path"
        } else {
          val dbfsRoot = ConfigManager.getString("cloud.dbfs.rootPath", "/dbfs/mnt/etl")
          s"$dbfsRoot/$path"
        }
      
      case "emr" =>
        if (path.startsWith("s3://")) {
          // Convert s3:// to s3a://
          path.replace("s3://", "s3a://")
        } else if (path.startsWith("s3a://")) {
          path
        } else {
          val s3Bucket = ConfigManager.getString("cloud.s3.bucket")
          s"s3a://$s3Bucket/$path"
        }
      
      case _ =>
        // Default to relative path for local
        path
    }
  }
  
  def verifyPathExists(path: String): Boolean = {
    val fullPath = normalizePath(path)
    try {
      val hadoopFS = org.apache.hadoop.fs.FileSystem.get(
        new java.net.URI(fullPath),
        spark.sparkContext.hadoopConfiguration
      )
      hadoopFS.exists(new org.apache.hadoop.fs.Path(fullPath))
    } catch {
      case e: Exception =>
        log.warn(s"Error verifying path $fullPath: ${e.getMessage}")
        false
    }
  }
}

object CloudFileReader {
  def apply(spark: SparkSession): CloudFileReader = new CloudFileReader(spark)
}

