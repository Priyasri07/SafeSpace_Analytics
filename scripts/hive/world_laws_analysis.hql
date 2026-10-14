-- World Laws Analysis in Hive
USE harassment_db;

DROP TABLE IF EXISTS world_laws;

CREATE EXTERNAL TABLE world_laws (
    country STRING,
    country_code STRING,
    region STRING,
    sexual_harassment_prohibited STRING,
    covers_all_workers STRING,
    covers_all_perpetrators STRING,
    employer_prevention_responsibility STRING,
    employer_liability STRING,
    retaliation_protection STRING,
    protection_score FLOAT
)
ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
STORED AS TEXTFILE
LOCATION '/user/harassment/world/'
TBLPROPERTIES ("skip.header.line.count"="1");

-- Note: HDFS folder has world_laws.json too -- Hive will skip non-matching files

-- Q6: Region-wise protection score
INSERT OVERWRITE DIRECTORY '/user/harassment/outputs/hive_q6_region_avg'
ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
SELECT region, AVG(protection_score) AS avg_score, COUNT(*) AS countries
FROM world_laws
GROUP BY region
ORDER BY avg_score DESC;
