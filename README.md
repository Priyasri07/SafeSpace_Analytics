# 🛡️ SafeSpace Analytics
### A Big Data Pipeline for Workplace Harassment Pattern Detection

## Abstract
Workplace harassment remains a pervasive challenge that impacts employee well-being, organizational culture, and legal compliance across diverse industries. This project delivers an end-to-end Big Data analytics pipeline designed to ingest, process, and correlate large-scale structured EEOC workplace charge records alongside semi-structured global legal frameworks. By combining distributed storage in HDFS with NoSQL document storage in MongoDB, the system enables scalable pattern discovery, trend identification, and comparative legal compliance analysis. The analytical workflow leverages Apache Pig for extract-transform-load (ETL) routines, Apache Hive for multi-dimensional data warehousing queries, Apache Sqoop for relational data interchange, and MySQL for reporting marts. Ultimately, the pipeline equips researchers and stakeholders with data-driven insights into harassment patterns and protective legislative coverage.

## Tech Stack
- **Programming Language:** Java 8 (OpenJDK 1.8 compatible with Hadoop ecosystem)
- **Build Tool:** Apache Maven 3.9+
- **Distributed Storage:** Apache Hadoop HDFS 3.3.6
- **NoSQL Database:** MongoDB 4.x / 7.x
- **Data Warehousing:** Apache Hive 3.1.3
- **Data Flow / ETL:** Apache Pig 0.17.0
- **Data Transfer:** Apache Sqoop
- **Relational Database:** MySQL 8.0

## Project Structure
```
Workplace-Harassment-Analytics/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── harassment/
│       │           ├── ingestion/
│       │           ├── mongodb/
│       │           ├── hdfs/
│       │           ├── hive/
│       │           ├── pig/
│       │           ├── sqoop/
│       │           ├── analytics/
│       │           └── utils/
│       └── resources/
├── data/
│   ├── raw/
│   └── processed/
├── scripts/
│   ├── pig/
│   ├── hive/
│   └── sqoop/
├── outputs/
├── report/
│   └── screenshots/
├── config/
│   └── config.properties
├── pom.xml
├── .gitignore
└── README.md
```

## Build Instructions
Ensure Java 8 and Apache Maven are configured in your environment. Run the following command from the project root:

```bash
mvn clean package
```

To compile only without packaging:
```bash
mvn clean compile
```

## Run Instructions
Execute the packaged fat JAR using Java 8:

```bash
java -jar target/workplace-harassment-analytics-1.0-SNAPSHOT.jar
```
