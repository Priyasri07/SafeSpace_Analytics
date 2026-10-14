package com.harassment.hive;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * Generates HiveQL scripts for EEOC charges and world laws analytics.
 */
public class HiveScriptGenerator {

    private final String scriptsDir;

    public HiveScriptGenerator() {
        this("scripts/hive");
    }

    public HiveScriptGenerator(String scriptsDir) {
        this.scriptsDir = scriptsDir;
    }

    /**
     * Generates scripts/hive/eeoc_analysis.hql
     *
     * @return File object of generated script
     */
    public File generateEEOCAnalysisScript() throws IOException {
        ensureDirectory();
        File file = new File(scriptsDir, "eeoc_analysis.hql");

        String scriptContent =
                "-- EEOC Charges Analysis in Hive\n" +
                "-- Create database\n" +
                "CREATE DATABASE IF NOT EXISTS harassment_db;\n" +
                "USE harassment_db;\n\n" +
                "-- Drop existing table if any\n" +
                "DROP TABLE IF EXISTS eeoc_charges;\n\n" +
                "-- Create EXTERNAL table over HDFS CSV (skips header)\n" +
                "CREATE EXTERNAL TABLE eeoc_charges (\n" +
                "    charge_filing_date STRING,\n" +
                "    cp_sex STRING,\n" +
                "    cp_race STRING,\n" +
                "    naics_code STRING,\n" +
                "    naics_description STRING,\n" +
                "    num_employees INT,\n" +
                "    respondent_name STRING\n" +
                ")\n" +
                "ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.OpenCSVSerde'\n" +
                "WITH SERDEPROPERTIES (\n" +
                "    \"separatorChar\" = \",\",\n" +
                "    \"quoteChar\"     = \"\\\"\",\n" +
                "    \"escapeChar\"    = \"\\\\\"\n" +
                ")\n" +
                "STORED AS TEXTFILE\n" +
                "LOCATION '/user/harassment/eeoc/'\n" +
                "TBLPROPERTIES (\"skip.header.line.count\"=\"1\");\n\n" +
                "-- Q1: Total charge count\n" +
                "INSERT OVERWRITE DIRECTORY '/user/harassment/outputs/hive_q1_total'\n" +
                "ROW FORMAT DELIMITED FIELDS TERMINATED BY ','\n" +
                "SELECT COUNT(*) AS total_charges FROM eeoc_charges;\n\n" +
                "-- Q2: Top 10 industries by charge count\n" +
                "INSERT OVERWRITE DIRECTORY '/user/harassment/outputs/hive_q2_industries'\n" +
                "ROW FORMAT DELIMITED FIELDS TERMINATED BY ','\n" +
                "SELECT naics_description, COUNT(*) AS charge_count\n" +
                "FROM eeoc_charges\n" +
                "GROUP BY naics_description\n" +
                "ORDER BY charge_count DESC\n" +
                "LIMIT 10;\n\n" +
                "-- Q3: Charges by sex\n" +
                "INSERT OVERWRITE DIRECTORY '/user/harassment/outputs/hive_q3_sex'\n" +
                "ROW FORMAT DELIMITED FIELDS TERMINATED BY ','\n" +
                "SELECT cp_sex, COUNT(*) AS count\n" +
                "FROM eeoc_charges\n" +
                "GROUP BY cp_sex;\n\n" +
                "-- Q4: Charges by race\n" +
                "INSERT OVERWRITE DIRECTORY '/user/harassment/outputs/hive_q4_race'\n" +
                "ROW FORMAT DELIMITED FIELDS TERMINATED BY ','\n" +
                "SELECT cp_race, COUNT(*) AS count\n" +
                "FROM eeoc_charges\n" +
                "GROUP BY cp_race\n" +
                "ORDER BY count DESC;\n\n" +
                "-- Q5: Yearly trend\n" +
                "INSERT OVERWRITE DIRECTORY '/user/harassment/outputs/hive_q5_yearly'\n" +
                "ROW FORMAT DELIMITED FIELDS TERMINATED BY ','\n" +
                "SELECT YEAR(charge_filing_date) AS year, COUNT(*) AS charge_count\n" +
                "FROM eeoc_charges\n" +
                "GROUP BY YEAR(charge_filing_date)\n" +
                "ORDER BY year;\n";

        writeFile(file, scriptContent);
        System.out.println("[HiveScriptGenerator] Generated: " + file.getAbsolutePath());
        return file;
    }

    /**
     * Generates scripts/hive/world_laws_analysis.hql
     *
     * @return File object of generated script
     */
    public File generateWorldLawsAnalysisScript() throws IOException {
        ensureDirectory();
        File file = new File(scriptsDir, "world_laws_analysis.hql");

        String scriptContent =
                "-- World Laws Analysis in Hive\n" +
                "USE harassment_db;\n\n" +
                "DROP TABLE IF EXISTS world_laws;\n\n" +
                "CREATE EXTERNAL TABLE world_laws (\n" +
                "    country STRING,\n" +
                "    country_code STRING,\n" +
                "    region STRING,\n" +
                "    sexual_harassment_prohibited STRING,\n" +
                "    covers_all_workers STRING,\n" +
                "    covers_all_perpetrators STRING,\n" +
                "    employer_prevention_responsibility STRING,\n" +
                "    employer_liability STRING,\n" +
                "    retaliation_protection STRING,\n" +
                "    protection_score FLOAT\n" +
                ")\n" +
                "ROW FORMAT DELIMITED FIELDS TERMINATED BY ','\n" +
                "STORED AS TEXTFILE\n" +
                "LOCATION '/user/harassment/world/'\n" +
                "TBLPROPERTIES (\"skip.header.line.count\"=\"1\");\n\n" +
                "-- Note: HDFS folder has world_laws.json too -- Hive will skip non-matching files\n\n" +
                "-- Q6: Region-wise protection score\n" +
                "INSERT OVERWRITE DIRECTORY '/user/harassment/outputs/hive_q6_region_avg'\n" +
                "ROW FORMAT DELIMITED FIELDS TERMINATED BY ','\n" +
                "SELECT region, AVG(protection_score) AS avg_score, COUNT(*) AS countries\n" +
                "FROM world_laws\n" +
                "GROUP BY region\n" +
                "ORDER BY avg_score DESC;\n";

        writeFile(file, scriptContent);
        System.out.println("[HiveScriptGenerator] Generated: " + file.getAbsolutePath());
        return file;
    }

    /**
     * Generates all HiveQL analysis scripts.
     */
    public void generateAllScripts() throws IOException {
        generateEEOCAnalysisScript();
        generateWorldLawsAnalysisScript();
    }

    private void ensureDirectory() {
        File dir = new File(scriptsDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private void writeFile(File file, String content) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }
}
