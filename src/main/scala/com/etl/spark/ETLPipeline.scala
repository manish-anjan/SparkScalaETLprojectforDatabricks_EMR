package com.etl.spark

import org.slf4j.LoggerFactory
import com.etl.spark.core.SparkSessionProvider
import com.etl.spark.io.CloudFileReader
import com.etl.spark.transform.Transformer
import com.etl.spark.load.HiveLoader
import com.etl.spark.util.ConfigManager

object ETLPipeline {
  private val log = LoggerFactory.getLogger(this.getClass)
  
  def main(args: Array[String]): Unit = {
    try {
      log.info("========================================")
      log.info("Starting Spark Scala ETL Pipeline")
      log.info("========================================")
      
      // Initialize Spark Session
      val spark = SparkSessionProvider.getOrCreateSession
      log.info(s"Spark Session initialized: ${spark.sparkContext.appName}")
      
      // Create readers and loaders
      val reader = CloudFileReader(spark)
      val transformer = Transformer.create(spark)
      val loader = HiveLoader(spark)
      
      // Ensure database exists
      loader.createDatabase()
      
      // Run ETL pipeline for each input format
      runCSVPipeline(spark, reader, transformer, loader)
      runJSONPipeline(spark, reader, transformer, loader)
      runParquetPipeline(spark, reader, transformer, loader)
      
      log.info("========================================")
      log.info("ETL Pipeline completed successfully")
      log.info("========================================")
      
      spark.stop()
      
    } catch {
      case e: Exception =>
        log.error("ETL Pipeline failed", e)
        throw e
    }
  }
  
  private def runCSVPipeline(spark: org.apache.spark.sql.SparkSession,
                             reader: CloudFileReader,
                             transformer: com.etl.spark.transform.DataTransformer,
                             loader: HiveLoader): Unit = {
    try {
      log.info("Starting CSV Pipeline")
      
      val sourcePath = ConfigManager.getSourcePath("csv")
      if (!reader.verifyPathExists(sourcePath)) {
        log.warn(s"CSV source path does not exist: $sourcePath, skipping CSV pipeline")
        return
      }
      
      val csvDf = reader.readCSV(sourcePath)
      log.info(s"CSV data loaded, row count: ${csvDf.count()}")
      csvDf.printSchema()
      
      val transformedDf = transformer.transform(csvDf)
      log.info(s"CSV data transformed")
      
      loader.loadToHive(transformedDf, "customer_csv")
      log.info("CSV data loaded to Hive table: etl_db.customer_csv")
      
    } catch {
      case e: Exception =>
        log.error("CSV Pipeline failed", e)
    }
  }
  
  private def runJSONPipeline(spark: org.apache.spark.sql.SparkSession,
                              reader: CloudFileReader,
                              transformer: com.etl.spark.transform.DataTransformer,
                              loader: HiveLoader): Unit = {
    try {
      log.info("Starting JSON Pipeline")
      
      val sourcePath = ConfigManager.getSourcePath("json")
      if (!reader.verifyPathExists(sourcePath)) {
        log.warn(s"JSON source path does not exist: $sourcePath, skipping JSON pipeline")
        return
      }
      
      val jsonDf = reader.readJSON(sourcePath)
      log.info(s"JSON data loaded, row count: ${jsonDf.count()}")
      jsonDf.printSchema()
      
      val transformedDf = transformer.transform(jsonDf)
      log.info(s"JSON data transformed")
      
      loader.loadToHive(transformedDf, "customer_json")
      log.info("JSON data loaded to Hive table: etl_db.customer_json")
      
    } catch {
      case e: Exception =>
        log.error("JSON Pipeline failed", e)
    }
  }
  
  private def runParquetPipeline(spark: org.apache.spark.sql.SparkSession,
                                 reader: CloudFileReader,
                                 transformer: com.etl.spark.transform.DataTransformer,
                                 loader: HiveLoader): Unit = {
    try {
      log.info("Starting Parquet Pipeline")
      
      val sourcePath = ConfigManager.getSourcePath("parquet")
      if (!reader.verifyPathExists(sourcePath)) {
        log.warn(s"Parquet source path does not exist: $sourcePath, skipping Parquet pipeline")
        return
      }
      
      val parquetDf = reader.readParquet(sourcePath)
      log.info(s"Parquet data loaded, row count: ${parquetDf.count()}")
      parquetDf.printSchema()
      
      val transformedDf = transformer.transform(parquetDf)
      log.info(s"Parquet data transformed")
      
      loader.loadToHive(transformedDf, "customer_parquet")
      log.info("Parquet data loaded to Hive table: etl_db.customer_parquet")
      
    } catch {
      case e: Exception =>
        log.error("Parquet Pipeline failed", e)
    }
  }
}

