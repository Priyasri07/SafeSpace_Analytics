-- EEOC Charges Analysis in Hive
-- Create database
CREATE DATABASE IF NOT EXISTS harassment_db;
USE harassment_db;

-- Drop existing table if any
DROP TABLE IF EXISTS eeoc_charges;

-- Create EXTERNAL table over HDFS CSV (skips header)
CREATE EXTERNAL TABLE eeoc_charges (
    charge_filing_date STRING,
    cp_sex STRING,
    cp_race STRING,
    naics_code STRING,
    naics_description STRING,
    num_employees INT,
    respondent_name STRING
)
ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.OpenCSVSerde'
WITH SERDEPROPERTIES (
    "separatorChar" = ",",
    "quoteChar"     = "\"",
    "escapeChar"    = "\\"
)
STORED AS TEXTFILE
LOCATION '/user/harassment/eeoc/'
TBLPROPERTIES ("skip.header.line.count"="1");

-- Q1: Total charge count
INSERT OVERWRITE DIRECTORY '/user/harassment/outputs/hive_q1_total'
ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
SELECT COUNT(*) AS total_charges FROM eeoc_charges;

-- Q2: Top 10 industries by charge count
INSERT OVERWRITE DIRECTORY '/user/harassment/outputs/hive_q2_industries'
ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
SELECT naics_description, COUNT(*) AS charge_count
FROM eeoc_charges
GROUP BY naics_description
ORDER BY charge_count DESC
LIMIT 10;

-- Q3: Charges by sex
INSERT OVERWRITE DIRECTORY '/user/harassment/outputs/hive_q3_sex'
ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
SELECT cp_sex, COUNT(*) AS count
FROM eeoc_charges
GROUP BY cp_sex;

-- Q4: Charges by race
INSERT OVERWRITE DIRECTORY '/user/harassment/outputs/hive_q4_race'
ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
SELECT cp_race, COUNT(*) AS count
FROM eeoc_charges
GROUP BY cp_race
ORDER BY count DESC;

-- Q5: Yearly trend
INSERT OVERWRITE DIRECTORY '/user/harassment/outputs/hive_q5_yearly'
ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
SELECT YEAR(charge_filing_date) AS year, COUNT(*) AS charge_count
FROM eeoc_charges
GROUP BY YEAR(charge_filing_date)
ORDER BY year;
