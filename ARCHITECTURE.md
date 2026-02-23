# Architecture & Design Decisions

## Overview

The Spark Scala ETL project follows a modular, cloud-agnostic architecture that abstracts platform differences between Databricks and AWS EMR while maintaining feature parity.

## Architecture Layers

```
┌──────────────────────────────────────────────────────────┐
│                    Applications                          │
│  (ETLPipeline.scala, Databricks Notebook)               │
└────────────┬─────────────────────────────────┬───────────┘
             │                                 │
┌────────────▼───────────────┐     ┌──────────▼────────────┐
│  Configuration Management   │     │  Cloud Abstraction   │
│  (ConfigManager.scala)      │     │  (Path Normalization)│
└────────────┬───────────────┘     └──────────┬───────────┘
             │                                │
    ┌────────▼────────────────────────────────▼────────┐
    │          Core ETL Pipeline Modules               │
    │  ┌──────────┐ ┌────────┐ ┌─────┐ ┌──────────┐  │
    │  │ Reader   │ │ Config │ │ Xfm │ │ Loader   │  │
    │  └──────────┘ └────────┘ └─────┘ └──────────┘  │
    └────────┬──────────────────────────────────┬─────┘
             │                                  │
┌────────────▼────────────────┐    ┌───────────▼──────────────┐
│   Spark Data Processing     │    │  Hive Metastore Client   │
│  (DataFrame Operations)     │    │  (Table Management)      │
└────────────┬────────────────┘    └───────────┬──────────────┘
             │                                  │
             └──────────────┬───────────────────┘
                            │
                ┌───────────▼───────────┐
                │   Cloud Storage       │
                │  (S3A, DBFS, etc)    │
                │   & Metastores       │
                │  (Glue, HMS, etc)    │
                └───────────────────────┘
```

## Module Design

### 1. **ConfigManager** (Configuration Layer)
- **Responsibility**: Centralized configuration with environment overrides
- **Design Pattern**: Singleton object with lazy initialization
- **Features**:
  - HOCON format with environment variable substitution
  - Type-safe configuration accessors
  - Cloud-aware path resolution
  - Fallback defaults

### 2. **SparkSessionProvider** (Platform Abstraction)
- **Responsibility**: Create environment-specific Spark sessions
- **Design Pattern**: Factory object
- **Platform-Specific Configs**:
  - **Databricks**: Delta catalog, S3 filesystem, adaptive execution
  - **EMR**: Glue Catalog, S3A filesystem, HMS support
  - **Local**: In-memory warehouse, minimal configuration

### 3. **CloudFileReader** (Data Ingestion)
- **Responsibility**: Abstract cloud storage path handling and multi-format reading
- **Design Pattern**: Wrapper class with implicit conversions
- **Features**:
  - Format-specific readers (CSV, JSON, Parquet, Delta)
  - Path normalization (DBFS → S3A → local)
  - Path existence verification
  - Error handling and logging

### 4. **Transformer** (Data Quality)
- **Responsibility**: Apply consistent transformations across all datasets
- **Design Pattern**: Strategy pattern with trait-based composition
- **Transformations**:
  - String column trimming
  - Explicit schema casting
  - Load date column injection
  - Extensible for custom transformations

### 5. **HiveLoader** (Data Output)
- **Responsibility**: Manage Hive metastore operations
- **Design Pattern**: Wrapper around Spark SQL Hive client
- **Features**:
  - Database creation
  - Table creation/replacement
  - External and managed table support
  - Write mode handling (overwrite, append, ignore)
  - Metadata retrieval

## Cloud Storage Abstraction

### Path Resolution Strategy

```
Input: "data/input/csv"
       ↓
┌──────────────────────────────┐
│ Environment Detection        │
│ (SPARK_ENV variable)         │
└──────┬───────────────────────┘
       │
       ├─ "databricks" → Prepend DBFS root
       │  Result: "/dbfs/mnt/etl/data/input/csv"
       │
       ├─ "emr" → Use S3A with bucket
       │  Result: "s3a://my-bucket/data/input/csv"
       │
       └─ "local" → Use relative path
          Result: "data/input/csv"
```

### Supported Protocols

| Platform | Primary | Secondary | Notes |
|----------|---------|-----------|-------|
| **Databricks** | /dbfs/ | /Volumes/ | DBFS for workspace data |
| **EMR Glue** | s3a:// | s3:// | S3A for Hadoop compatibility |
| **Local** | file:// | ./ | Relative paths preferred |

## Metastore Configuration

### Databricks (Default)
```
┌────────────────────────────┐
│  Databricks Workspace      │
│  ┌──────────────────────┐  │
│  │  Unity Catalog       │  │
│  │  OR                  │  │
│  │  Hive Metastore      │  │
│  └──────────────────────┘  │
└────────────────────────────┘
```

### AWS EMR with Glue Catalog
```
┌──────────────────────────────┐
│    AWS Account              │
│  ┌────────────────────────┐ │
│  │  AWS Glue Catalog      │ │
│  │  (Central Metadata)    │ │
│  └────────────────────────┘ │
│           ▲                 │
│           │                 │
│  ┌────────▼────────────┐    │
│  │  EMR Cluster        │    │
│  │  (Spark + Hadoop)   │    │
│  └─────────────────────┘    │
└──────────────────────────────┘
```

### AWS EMR with HMS
```
┌──────────────────────────────┐
│    EMR Cluster              │
│  ┌────────────────────────┐ │
│  │  Hive Metastore Svc    │ │
│  │  (thrift://host:9083)  │ │
│  └────────────────────────┘ │
│           ▲                 │
│           │                 │
│  ┌────────▼────────────┐    │
│  │  Spark Executors    │    │
│  │  (Read from HMS)    │    │
│  └─────────────────────┘    │
└──────────────────────────────┘
```

## Data Flow Patterns

### Pattern 1: Batch Full Load
```
Raw Data → Reader → Transformer → HiveLoader (OVERWRITE)
           (Count)   (Transform)  (Create/Replace)
```

### Pattern 2: Incremental Load
```
New Data → Reader → Transformer → HiveLoader (APPEND)
           (Verify) (Add load_dt) (Insert into)
```

### Pattern 3: Multi-Source Consolidation
```
        ┌─→ CSV Reader ─┐
Data ──┤─→ JSON Reader ─┼→ Transformer → HiveLoader
        └─→ Parquet ────┘
```

## Error Handling Strategy

```
┌────────────────────────────┐
│  Exception Caught          │
└────────────┬───────────────┘
             │
    ┌────────▼────────────┐
    │  Error Type?        │
    └────┬──────┬─────────┘
         │      │
    ┌────▼──┐  ┌▼────────────────────┐
    │ Path  │  │  Schema/Data Issues  │
    │ Issue │  └┬────────────────────┘
    └────┬──┘   │
         │      │ Log & Continue
    Skip ├──────┤ (non-blocking)
    Data │      │
         │    ┌─▼──────────────┐
         │    │ Fatal Error    │
         │    └┬────────────────┘
         │     │
         │  Throw &
         │  Stop Job
```

## Performance Optimizations

### 1. Adaptive Query Execution
```
spark.sql.adaptive.enabled = true
- Dynamically adjusts partition count
- Coalesces small partitions
- Handles skewed joins
```

### 2. S3A Connection Pooling (EMR)
```
fs.s3a.threads.max = 10
fs.s3a.threads.core = 4
- Parallel S3 reads
- Connection reuse
- Timeout handling
```

### 3. Delta Lake Optimizations (Both)
```
spark.sql.shuffle.partitions = 200
- Pre-optimized partition count
- Reduced shuffle I/O
- Better small file handling
```

## Scalability Considerations

### Vertical Scaling
- Increase executor memory: `--executor-memory 32g`
- Increase cores per executor: `--executor-cores 8`
- Increase driver memory: `--driver-memory 8g`

### Horizontal Scaling
- Increase executor count: `--num-executors 50`
- Increase partitions: `spark.sql.shuffle.partitions = 500`
- Use Delta Lake for better caching

### Data Size Handling
```
Size < 1 GB    → Single executor, few partitions
Size 1-10 GB   → Standard configuration (as default)
Size 10-100 GB → Increase executors, shuffle partitions
Size > 100 GB  → Consider external Hive table, partitioning
```

## Security Architecture

```
┌─────────────────────────────────────┐
│      IAM Roles / Service Account    │
│      (No Hardcoded Credentials)     │
└─────────────┬───────────────────────┘
              │
    ┌─────────▼──────────────┐
    │  AWS SDK Chain         │
    │  (DefaultProvider)     │
    └─────────┬──────────────┘
              │
    ┌─────────▼──────────────┐
    │  S3 Access             │
    │  Glue Access           │
    │  (via temporary STS)   │
    └───────────────────────┘
```

## Testing Strategy

### Unit Tests
- Config parsing and resolution
- Path normalization logic
- Schema casting rules

### Integration Tests
- Spark session creation
- File read/write operations
- Metastore interactions

### End-to-End Tests
- Full pipeline with sample data
- Both Databricks and EMR execution

## Deployment Patterns

### Pattern 1: Development
```
Local SBT → Test → Compile → Databricks Notebook
```

### Pattern 2: Staging
```
GitHub Push → CI Build → Assembly JAR
                            ↓
                      Upload to S3
                            ↓
                      Submit to Dev EMR
```

### Pattern 3: Production
```
GitHub Tag → Build → Unit/Integration Tests
                            ↓
                      Assembly JAR to Artifactory
                            ↓
                      Deploy to Production Clusters
                      (Databricks + EMR)
```

## Future Enhancement Opportunities

1. **Partitioned Delta Tables**: Implement partitioning strategy
2. **Change Data Capture**: Use Delta CDF for incremental syncs
3. **Schema Evolution**: Auto-detect and migrate schemas
4. **Quality Metrics**: Implement data quality framework
5. **Data Lineage**: Add OpenLineage integration
6. **Cost Optimization**: Implement right-sizing recommendations
7. **Multi-cloud**: Extend to Azure (ADLS) and GCP (GCS)

