package com.etl.spark.util

import com.typesafe.config.{Config, ConfigFactory}
import scala.collection.JavaConverters._

object ConfigManager {
  private lazy val config: Config = ConfigFactory.load().resolve()
  
  def getConfig: Config = config
  
  def getString(path: String): String = {
    try {
      config.getString(path)
    } catch {
      case e: Exception =>
        throw new RuntimeException(s"Configuration key '$path' not found", e)
    }
  }
  
  def getString(path: String, default: String): String = {
    try {
      config.getString(path)
    } catch {
      case _: Exception => default
    }
  }
  
  def getInt(path: String): Int = {
    try {
      config.getInt(path)
    } catch {
      case e: Exception =>
        throw new RuntimeException(s"Configuration key '$path' not found", e)
    }
  }
  
  def getInt(path: String, default: Int): Int = {
    try {
      config.getInt(path)
    } catch {
      case _: Exception => default
    }
  }
  
  def getBoolean(path: String): Boolean = {
    try {
      config.getBoolean(path)
    } catch {
      case e: Exception =>
        throw new RuntimeException(s"Configuration key '$path' not found", e)
    }
  }
  
  def getBoolean(path: String, default: Boolean): Boolean = {
    try {
      config.getBoolean(path)
    } catch {
      case _: Exception => default
    }
  }
  
  def getStringList(path: String): List[String] = {
    try {
      config.getStringList(path).asScala.toList
    } catch {
      case _: Exception => List.empty[String]
    }
  }
  
  def getEnvironment: String = {
    getString("environment", "databricks").toLowerCase
  }
  
  def isDataBricks: Boolean = getEnvironment == "databricks"
  
  def isEMR: Boolean = getEnvironment == "emr"
  
  def getSourcePath(format: String): String = {
    val environment = getEnvironment
    val sourcePath = getString(s"source.$format.path")
    
    environment match {
      case "databricks" =>
        val dbfsRoot = getString("cloud.dbfs.rootPath")
        s"$dbfsRoot/$sourcePath"
      case "emr" =>
        val s3Bucket = getString("cloud.s3.bucket")
        val s3Region = getString("cloud.s3.region")
        s"s3a://$s3Bucket/$sourcePath"
      case _ => sourcePath
    }
  }
  
  def getTargetLocation: String = {
    val environment = getEnvironment
    val warehouseLocation = getString("target.warehouse.location")
    
    environment match {
      case "databricks" =>
        val dbfsRoot = getString("cloud.dbfs.rootPath")
        s"$dbfsRoot/warehouse"
      case "emr" =>
        warehouseLocation
      case _ => warehouseLocation
    }
  }
}

