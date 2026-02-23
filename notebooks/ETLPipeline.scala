// Databricks notebook source
// MAGIC %md
// MAGIC # Spark Scala ETL Pipeline for Databricks
// MAGIC
// MAGIC This notebook provides an interactive ETL pipeline for Databricks runtime.
// MAGIC It supports CSV, JSON, and Parquet data formats with automatic schema casting and data transformations.

// COMMAND ----------

// Import required packages
import org.apache.spark.sql.{DataFrame, SparkSession, SaveMode}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import java.time.LocalDate

// COMMAND ----------

// MAGIC %md
// MAGIC ## Configuration Parameters

// COMMAND ----------

// Define parameters for the notebook
val sourceDatabase = "etl_db"
val targetDatabase = "etl_db"
val sourceFormat = dbutils.widgets.get("sourceFormat") // Options: csv, json, parquet
val sourcePath = dbutils.widgets.get("sourcePath") // DBFS path
val tableName = dbutils.widgets.get("tableName") // Target table name
val writeMode = dbutils.widgets.get("writeMode") // Options: overwrite, append
val trimColumns = dbutils.widgets.getAll("trimColumns").split(",").filter(_.nonEmpty) // Columns to trim

println(s"Configuration:")
println(s"  Source Format: $sourceFormat")
println(s"  Source Path: $sourcePath")
println(s"  Target Table: $tableName")
println(s"  Write Mode: $writeMode")
println(s"  Trim Columns: ${trimColumns.mkString(", ")}")

// COMMAND ----------

// MAGIC %md
// MAGIC ## Step 1: Read Source Data

// COMMAND ----------

// Read data based on format
val rawDf = sourceFormat.toLowerCase match {
  case "csv" =>
    spark.read
      .format("csv")
      .option("header", "true")
      .option("inferSchema", "true")
      .option("delimiter", ",")
      .option("nullValue", "")
      .load(sourcePath)

  case "json" =>
    spark.read
      .format("json")
      .option("multiLine", "false")
      .load(sourcePath)

  case "parquet" =>
    spark.read
      .format("parquet")
      .load(sourcePath)

  case _ =>
    throw new Exception(s"Unsupported format: $sourceFormat")
}

println(s"Data loaded. Row count: ${rawDf.count()}")
rawDf.printSchema()

// COMMAND ----------

// MAGIC %md
// MAGIC ## Step 2: Data Transformations

// COMMAND ----------

// Apply transformations
var transformedDf = rawDf

// 1. Trim string columns
if (trimColumns.nonEmpty) {
  println(s"Trimming columns: ${trimColumns.mkString(", ")}")
  trimColumns.foreach { colName =>
    if (rawDf.columns.contains(colName)) {
      transformedDf = transformedDf.withColumn(colName, trim(col(colName)))
    }
  }
}

// 2. Add load date column
val loadDtColumn = "load_dt"
println(s"Adding load date column: $loadDtColumn")
transformedDf = transformedDf.withColumn(loadDtColumn, current_date())

// 3. Show transformed data sample
println("Transformed data sample:")
transformedDf.show(5)

// COMMAND ----------

// MAGIC %md
// MAGIC ## Step 3: Create Database

// COMMAND ----------

spark.sql(s"CREATE DATABASE IF NOT EXISTS $targetDatabase")
println(s"Database ensured: $targetDatabase")

// COMMAND ----------

// MAGIC %md
// MAGIC ## Step 4: Load to Hive Table

// COMMAND ----------

val fullyQualifiedTableName = s"$targetDatabase.$tableName"

// Convert write mode string to SaveMode
val saveMode = writeMode.toLowerCase match {
  case "overwrite" => SaveMode.Overwrite
  case "append" => SaveMode.Append
  case "ignore" => SaveMode.Ignore
  case _ => SaveMode.Overwrite
}

// Write to Delta table (Databricks default)
transformedDf.write
  .format("delta")
  .mode(saveMode)
  .option("path", s"/user/hive/warehouse/$fullyQualifiedTableName")
  .saveAsTable(fullyQualifiedTableName)

println(s"Data loaded to table: $fullyQualifiedTableName")

// COMMAND ----------

// MAGIC %md
// MAGIC ## Step 5: Data Quality Checks

// COMMAND ----------

// Verify data loaded successfully
val tableExists = spark.catalog.tableExists(fullyQualifiedTableName)
println(s"Table exists: $tableExists")

if (tableExists) {
  val tableCount = spark.sql(s"SELECT COUNT(*) as count FROM $fullyQualifiedTableName").collect()(0)(0)
  println(s"Table row count: $tableCount")

  spark.sql(s"DESCRIBE EXTENDED $fullyQualifiedTableName").show(30)
}

// COMMAND ----------

// MAGIC %md
// MAGIC ## Step 6: Sample Query

// COMMAND ----------

// Display sample data from loaded table
spark.sql(s"SELECT * FROM $fullyQualifiedTableName LIMIT 20").show()

// COMMAND ----------

// MAGIC %md
// MAGIC ## ETL Pipeline Complete
// MAGIC
// MAGIC Summary:
// MAGIC - Source Format: CSV
// MAGIC - Source Path: DBFS path
// MAGIC - Transformations: String trim, Load date added
// MAGIC - Target Table: Fully qualified table name
// MAGIC - Write Mode: Overwrite/Append

