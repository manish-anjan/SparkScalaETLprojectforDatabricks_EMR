# EMR Job Configuration Examples

## Example 1: Simple CSV to Hive Load

```scala
// Configuration for basic CSV load
environment = "emr"

source.csv.path = "data/raw/customers"
target.database = "etl_prod"
target.writeMode = "overwrite"
target.useDataFormat = "delta"

transformations.addLoadDate = true
transformations.trimColumns = ["name", "email"]
```

## Example 2: Multi-source Consolidation

```scala
// Load from multiple sources into single table
environment = "emr"

source {
  csv.path = "data/raw/customers_csv"
  json.path = "data/raw/customers_json"
  parquet.path = "data/raw/customers_parquet"
}

target {
  database = "analytics"
  writeMode = "append"
}

transformations {
  castSchema = true
  schema {
    customer_id = "BIGINT"
    order_date = "DATE"
    amount = "DECIMAL(18,2)"
  }
}
```

## Example 3: Databricks with Unity Catalog

```scala
environment = "databricks"
cloud.dbfs.rootPath = "/Volumes/myworkspace/default/etl_volume"

source {
  csv.path = "bronze/customers"
  json.path = "bronze/orders"
}

target {
  database = "gold"
  useDataFormat = "delta"
}
```

## Example 4: High-Performance EMR Configuration

```scala
environment = "emr"

cloud.s3 {
  bucket = "enterprise-data-lake"
  region = "us-east-1"
}

source.csv.path = "data/incoming/bulk-load"

target {
  database = "raw"
  writeMode = "append"
  useDataFormat = "delta"
}

spark.sql.shuffle.partitions = 400
spark.sql.adaptive.enabled = true
```

## Example 5: Incremental Load with Partitioning

```scala
environment = "emr"

source.parquet.path = "data/updates/${YEAR}/${MONTH}/${DAY}"

target {
  database = "fact_tables"
  writeMode = "append"
  partitionBy = ["load_dt"]
  externalTable = true
}
```

