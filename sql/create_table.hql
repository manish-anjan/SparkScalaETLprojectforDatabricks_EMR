-- Hive Table Creation Script for ETL Pipeline
-- This script creates external Hive tables for CSV, JSON, and Parquet data sources

-- Create database if not exists
CREATE DATABASE IF NOT EXISTS etl_db
COMMENT 'ETL database for cloud data ingestion'
LOCATION 's3a://my-warehouse/etl_db';

-- Customer table for CSV data
CREATE EXTERNAL TABLE IF NOT EXISTS etl_db.customer_csv (
    customer_id BIGINT,
    customer_name STRING,
    email STRING,
    phone STRING,
    address STRING,
    city STRING,
    state STRING,
    zip_code STRING,
    country STRING,
    registration_date DATE,
    is_active BOOLEAN,
    amount DECIMAL(18, 2),
    load_dt DATE
)
COMMENT 'Customer data from CSV sources'
STORED AS PARQUET
LOCATION 's3a://my-warehouse/tables/customer_csv'
TBLPROPERTIES (
    'EXTERNAL' = 'TRUE',
    'transient_lastDdlTime' = '0'
);

-- Customer table for JSON data
CREATE EXTERNAL TABLE IF NOT EXISTS etl_db.customer_json (
    customer_id BIGINT,
    customer_name STRING,
    email STRING,
    phone STRING,
    address STRING,
    city STRING,
    state STRING,
    zip_code STRING,
    country STRING,
    registration_date DATE,
    is_active BOOLEAN,
    amount DECIMAL(18, 2),
    load_dt DATE
)
COMMENT 'Customer data from JSON sources'
STORED AS PARQUET
LOCATION 's3a://my-warehouse/tables/customer_json'
TBLPROPERTIES (
    'EXTERNAL' = 'TRUE',
    'transient_lastDdlTime' = '0'
);

-- Customer table for Parquet data
CREATE EXTERNAL TABLE IF NOT EXISTS etl_db.customer_parquet (
    customer_id BIGINT,
    customer_name STRING,
    email STRING,
    phone STRING,
    address STRING,
    city STRING,
    state STRING,
    zip_code STRING,
    country STRING,
    registration_date DATE,
    is_active BOOLEAN,
    amount DECIMAL(18, 2),
    load_dt DATE
)
COMMENT 'Customer data from Parquet sources'
STORED AS PARQUET
LOCATION 's3a://my-warehouse/tables/customer_parquet'
TBLPROPERTIES (
    'EXTERNAL' = 'TRUE',
    'transient_lastDdlTime' = '0'
);

-- Consolidated table combining all sources
CREATE EXTERNAL TABLE IF NOT EXISTS etl_db.customer_consolidated (
    customer_id BIGINT,
    customer_name STRING,
    email STRING,
    phone STRING,
    address STRING,
    city STRING,
    state STRING,
    zip_code STRING,
    country STRING,
    registration_date DATE,
    is_active BOOLEAN,
    amount DECIMAL(18, 2),
    source_system STRING,
    load_dt DATE
)
COMMENT 'Consolidated customer data from all sources'
PARTITIONED BY (load_dt DATE)
STORED AS PARQUET
LOCATION 's3a://my-warehouse/tables/customer_consolidated'
TBLPROPERTIES (
    'EXTERNAL' = 'TRUE',
    'transient_lastDdlTime' = '0'
);

-- Delta table for customer incremental loads
CREATE TABLE IF NOT EXISTS etl_db.customer_delta (
    customer_id BIGINT,
    customer_name STRING,
    email STRING,
    phone STRING,
    address STRING,
    city STRING,
    state STRING,
    zip_code STRING,
    country STRING,
    registration_date DATE,
    is_active BOOLEAN,
    amount DECIMAL(18, 2),
    load_dt DATE
)
USING DELTA
COMMENT 'Customer data in Delta format for ACID compliance'
LOCATION 's3a://my-warehouse/tables/customer_delta'
TBLPROPERTIES (
    'delta.enableChangeDataFeed' = 'true'
);

-- Show created tables
SHOW TABLES IN etl_db;

-- Describe table schemas
DESCRIBE EXTENDED etl_db.customer_csv;
DESCRIBE EXTENDED etl_db.customer_json;
DESCRIBE EXTENDED etl_db.customer_parquet;
DESCRIBE EXTENDED etl_db.customer_delta;

