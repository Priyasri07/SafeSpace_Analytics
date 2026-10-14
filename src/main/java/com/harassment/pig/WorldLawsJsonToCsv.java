package com.harassment.pig;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harassment.hdfs.HDFSUploader;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Pre-processes semi-structured world_laws.json into a flattened CSV format (world_laws_pig.csv)
 * suitable for Apache Pig using PigStorage(','), without requiring piggybank.jar / JsonLoader.
 * Also uploads the flattened CSV into HDFS.
 */
public class WorldLawsJsonToCsv {

    private final ObjectMapper objectMapper;

    public WorldLawsJsonToCsv() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Converts world_laws.json into world_laws_pig.csv.
     *
     * @param inputJsonPath  Path to data/raw/world_laws.json
     * @param outputCsvPath Path to data/processed/world_laws_pig.csv
     * @return Total rows written (including header)
     */
    public int convert(String inputJsonPath, String outputCsvPath) throws IOException {
        File jsonFile = new File(inputJsonPath);
        if (!jsonFile.exists()) {
            jsonFile = new File("C:/Users/manis/Workplace-Harassment-Analytics", inputJsonPath);
        }

        if (!jsonFile.exists()) {
            throw new IOException("Input JSON not found: " + inputJsonPath);
        }

        System.out.println("\n[WorldLawsJsonToCsv] Reading JSON dataset: " + jsonFile.getAbsolutePath());
        List<Map<String, Object>> countries = objectMapper.readValue(
                jsonFile, new TypeReference<List<Map<String, Object>>>() {});

        File csvFile = new File(outputCsvPath);
        File parentDir = csvFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        int rowsWritten = 0;
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(csvFile), StandardCharsets.UTF_8))) {

            // Header row
            writer.write("country,country_code,region,sexual_harassment_prohibited,covers_all_workers,covers_all_perpetrators,employer_prevention_responsibility,employer_liability,retaliation_protection,protection_score");
            writer.newLine();
            rowsWritten++;

            for (Map<String, Object> map : countries) {
                String country = cleanString(map.get("country"));
                String code = cleanString(map.get("country_code"));
                String region = cleanString(map.get("region"));
                String prohibited = String.valueOf(map.get("sexual_harassment_prohibited"));
                String workers = String.valueOf(map.get("covers_all_workers"));
                String perps = String.valueOf(map.get("covers_all_perpetrators"));
                String prevention = String.valueOf(map.get("employer_prevention_responsibility"));
                String liability = String.valueOf(map.get("employer_liability"));
                String retaliation = String.valueOf(map.get("retaliation_protection"));
                String score = String.valueOf(map.get("protection_score"));

                String line = String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        country, code, region, prohibited, workers, perps, prevention, liability, retaliation, score);

                writer.write(line);
                writer.newLine();
                rowsWritten++;
            }
        }

        System.out.printf("[WorldLawsJsonToCsv] SUCCESS: Flattened %d countries to CSV: %s (Total lines with header: %d)\n",
                countries.size(), csvFile.getAbsolutePath(), rowsWritten);

        return rowsWritten;
    }

    /**
     * Converts world_laws.json to CSV and immediately uploads it to HDFS at
     * /user/harassment/world/world_laws_pig.csv.
     *
     * @param inputJsonPath  Local input JSON path
     * @param outputCsvPath Local output CSV path
     * @return true if both conversion and upload succeed
     */
    public boolean convertAndUpload(String inputJsonPath, String outputCsvPath) {
        try {
            int rows = convert(inputJsonPath, outputCsvPath);
            if (rows <= 0) {
                return false;
            }

            File csvFile = new File(outputCsvPath);
            if (!csvFile.exists()) {
                csvFile = new File("C:/Users/manis/Workplace-Harassment-Analytics", outputCsvPath);
            }

            String hdfsTarget = "/user/harassment/world/world_laws_pig.csv";
            System.out.println("[WorldLawsJsonToCsv] Uploading flattened CSV to HDFS: " + hdfsTarget);

            HDFSUploader uploader = new HDFSUploader();
            boolean uploaded = uploader.uploadFile(csvFile.getAbsolutePath(), hdfsTarget);
            if (uploaded) {
                System.out.println("[WorldLawsJsonToCsv] \u2714 Upload verified for " + hdfsTarget);
            } else {
                System.err.println("[WorldLawsJsonToCsv] \u2718 Upload failed for " + hdfsTarget);
            }
            return uploaded;
        } catch (Exception e) {
            System.err.println("[WorldLawsJsonToCsv] Error during conversion or HDFS upload: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private String cleanString(Object obj) {
        if (obj == null) return "";
        // Strip commas and quotes if any to maintain clean CSV format
        return String.valueOf(obj).replace(",", " ").replace("\"", "").trim();
    }
}