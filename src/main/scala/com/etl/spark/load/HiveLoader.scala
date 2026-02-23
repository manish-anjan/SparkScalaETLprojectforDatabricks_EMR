package com.etl.spark.load

import org.apache.spark.sql.{DataFrame, SparkSession, SaveMode}
import org.slf4j.LoggerFactory
import com.etl.spark.util.ConfigManager

class HiveLoader(spark: SparkSession) {
  private val log = LoggerFactory.getLogger(this.getClass)
  
  def loadToHive(df: DataFrame, tableName: String): Unit = {
    loadToHive(df, tableName, ConfigManager.getString("target.writeMode", "overwrite"))
  }
  
  def loadToHive(df: DataFrame, tableName: String, writeMode: String): Unit = {
    val database = ConfigManager.getString("target.database")
    val fullyQualifiedTableName = s"$database.$tableName"
    
    log.info(s"Loading data to Hive table: $fullyQualifiedTableName with mode: $writeMode")
    
    val mode = writeMode.toLowerCase match {
      case "overwrite" => SaveMode.Overwrite
      case "append" => SaveMode.Append
      case "ignore" => SaveMode.Ignore
      case "error" => SaveMode.ErrorIfExists
      case _ =>
        log.warn(s"Unknown write mode: $writeMode, defaulting to Overwrite")
        SaveMode.Overwrite
    }
    
    val useDataFormat = ConfigManager.getString("target.useDataFormat", "delta")
    
    try {
      df.write
        .format(useDataFormat)
        .mode(mode)
        .option("path", getTableLocation(tableName, useDataFormat))
        .saveAsTable(fullyQualifiedTableName)
      
      log.info(s"Successfully loaded data to table: $fullyQualifiedTableName")
    } catch {
      case e: Exception =>
        log.error(s"Failed to load data to table: $fullyQualifiedTableName", e)
        throw e
    }
  }
  
  def loadToExternalTable(df: DataFrame, tableName: String, location: String): Unit = {
    loadToExternalTable(df, tableName, location, ConfigManager.getString("target.writeMode", "overwrite"))
  }
  
  def loadToExternalTable(df: DataFrame, tableName: String, location: String, writeMode: String): Unit = {
    val database = ConfigManager.getString("target.database")
    val fullyQualifiedTableName = s"$database.$tableName"
    val normalizedLocation = normalizePath(location)
    
    log.info(s"Loading data to external table: $fullyQualifiedTableName at location: $normalizedLocation")
    
    val mode = writeMode.toLowerCase match {
      case "overwrite" => SaveMode.Overwrite
      case "append" => SaveMode.Append
      case "ignore" => SaveMode.Ignore
      case "error" => SaveMode.ErrorIfExists
      case _ =>
        log.warn(s"Unknown write mode: $writeMode, defaulting to Overwrite")
        SaveMode.Overwrite
    }
    
    val useDataFormat = ConfigManager.getString("target.useDataFormat", "delta")
    
    try {
      df.write
        .format(useDataFormat)
        .mode(mode)
        .option("path", normalizedLocation)
        .saveAsTable(fullyQualifiedTableName)
      
      log.info(s"Successfully loaded data to external table: $fullyQualifiedTableName")
    } catch {
      case e: Exception =>
        log.error(s"Failed to load data to external table: $fullyQualifiedTableName", e)
        throw e
    }
  }
  
  def createDatabase(): Unit = {
    val database = ConfigManager.getString("target.database")
    
    try {
      spark.sql(s"CREATE DATABASE IF NOT EXISTS $database")
      log.info(s"Database ensured: $database")
    } catch {
      case e: Exception =>
        log.error(s"Failed to create database: $database", e)
        throw e
    }
  }
  
  def truncateTable(tableName: String): Unit = {
    val database = ConfigManager.getString("target.database")
    val fullyQualifiedTableName = s"$database.$tableName"
    
    try {
      spark.sql(s"TRUNCATE TABLE $fullyQualifiedTableName")
      log.info(s"Truncated table: $fullyQualifiedTableName")
    } catch {
      case e: Exception =>
        log.warn(s"Failed to truncate table: $fullyQualifiedTableName", e)
    }
  }
  
  def dropTable(tableName: String, ifExists: Boolean = true): Unit = {
    val database = ConfigManager.getString("target.database")
    val fullyQualifiedTableName = s"$database.$tableName"
    
    try {
      val dropStmt = if (ifExists) s"DROP TABLE IF EXISTS $fullyQualifiedTableName" 
                     else s"DROP TABLE $fullyQualifiedTableName"
      spark.sql(dropStmt)
      log.info(s"Dropped table: $fullyQualifiedTableName")
    } catch {
      case e: Exception =>
        log.error(s"Failed to drop table: $fullyQualifiedTableName", e)
        throw e
    }
  }
  
  def getTableMetadata(tableName: String): Map[String, String] = {
    val database = ConfigManager.getString("target.database")
    val fullyQualifiedTableName = s"$database.$tableName"
    
    try {
      val metadata = spark.sql(s"DESCRIBE EXTENDED $fullyQualifiedTableName")
        .collect()
        .map(row => (row.getString(0), row.getString(1)))
        .toMap
      
      log.info(s"Retrieved metadata for table: $fullyQualifiedTableName")
      metadata
    } catch {
      case e: Exception =>
        log.error(s"Failed to get metadata for table: $fullyQualifiedTableName", e)
        Map.empty[String, String]
    }
  }
  
  private def getTableLocation(tableName: String, format: String): String = {
    val warehouseLocation = ConfigManager.getString("target.warehouse.location")
    val environment = ConfigManager.getEnvironment
    
    environment match {
      case "databricks" =>
        s"$warehouseLocation/$tableName"
      case "emr" =>
        s"$warehouseLocation/$tableName"
      case _ =>
        s"$warehouseLocation/$tableName"
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
          path.replace("s3://", "s3a://")
        } else if (path.startsWith("s3a://")) {
          path
        } else {
          val s3Bucket = ConfigManager.getString("cloud.s3.bucket")
          s"s3a://$s3Bucket/$path"
        }
      
      case _ => path
    }
  }
}

object HiveLoader {
  def apply(spark: SparkSession): HiveLoader = new HiveLoader(spark)
}

