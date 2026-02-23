package com.etl.spark.transform

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.slf4j.LoggerFactory
import com.etl.spark.util.ConfigManager
import scala.collection.mutable
import scala.collection.JavaConverters._

trait DataTransformer {
  def transform(df: DataFrame): DataFrame
}

class StandardTransformer(spark: SparkSession) extends DataTransformer {
  private val log = LoggerFactory.getLogger(this.getClass)
  
  override def transform(df: DataFrame): DataFrame = {
    log.info("Starting data transformations")
    var transformedDf = df
    
    // 1. Trim string columns
    transformedDf = trimStringColumns(transformedDf)
    
    // 2. Cast schema explicitly
    if (ConfigManager.getBoolean("transformations.castSchema", true)) {
      transformedDf = castSchema(transformedDf)
    }
    
    // 3. Add load date column
    if (ConfigManager.getBoolean("transformations.addLoadDate", true)) {
      transformedDf = addLoadDateColumn(transformedDf)
    }
    
    log.info("Data transformations completed")
    transformedDf
  }
  
  private def trimStringColumns(df: DataFrame): DataFrame = {
    log.info("Trimming string columns")
    val trimColumns = ConfigManager.getStringList("transformations.trimColumns")
    
    if (trimColumns.isEmpty) {
      log.info("No trim columns specified, using all string columns")
      val stringCols = df.schema.filter(_.dataType == StringType).map(_.name)
      trimAllStringColumns(df, stringCols)
    } else {
      trimAllStringColumns(df, trimColumns)
    }
  }
  
  private def trimAllStringColumns(df: DataFrame, columns: Seq[String]): DataFrame = {
    var result = df
    columns.foreach { colName =>
      if (df.columns.contains(colName)) {
        result = result.withColumn(colName, trim(col(colName)))
      }
    }
    result
  }
  
  private def castSchema(df: DataFrame): DataFrame = {
    log.info("Casting schema columns")
    
    try {
      val schemaConfig = ConfigManager.getConfig.getConfig("transformations.schema")
      val schemaCasting = mutable.Map[String, String]()
      
      schemaConfig.entrySet().asScala.foreach { entry =>
        schemaCasting(entry.getKey) = entry.getValue.unwrapped().toString
      }
      
      var result = df
      schemaCasting.foreach { case (colName, dataType) =>
        if (df.columns.contains(colName)) {
          try {
            result = result.withColumn(colName, col(colName).cast(dataType))
            log.debug(s"Cast column $colName to $dataType")
          } catch {
            case e: Exception =>
              log.warn(s"Failed to cast column $colName to $dataType: ${e.getMessage}")
          }
        }
      }
      result
    } catch {
      case e: Exception =>
        log.warn(s"Failed to load schema casting config: ${e.getMessage}")
        df
    }
  }
  
  private def addLoadDateColumn(df: DataFrame): DataFrame = {
    val loadDateColumn = ConfigManager.getString("transformations.loadDateColumn", "load_dt")
    log.info(s"Adding load date column: $loadDateColumn")
    
    df.withColumn(loadDateColumn, current_date())
  }
}

object Transformer {
  def create(spark: SparkSession): DataTransformer = {
    new StandardTransformer(spark)
  }
}

