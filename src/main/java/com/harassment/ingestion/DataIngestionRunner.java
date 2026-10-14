package com.harassment.ingestion;

import com.harassment.utils.ConfigLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;

/**
 * Orchestrator for data ingestion module.
 * Executes dataset downloads/generation, verifies file existence,
 * displays dataset samples, and prints a summary report.
 */
public class DataIngestionRunner {

    public static void run() {
        System.out.println("================================================================================");
        System.out.println("      BIG DATA PIPELINE: DATA INGESTION MODULE EXECUTION");
        System.out.println("================================================================================");
        System.out.println();

        // 1. Download/Generate EEOC dataset
        long eeocRows = EEOCDataDownloader.download();
        System.out.println();

        // 2. Download/Generate WORLD Laws dataset
        int worldCountries = WorldLawsDataDownloader.download();
        System.out.println();

        // 3. Verification & Validation
        verifyDatasets(eeocRows, worldCountries);
    }

    private static void verifyDatasets(long eeocRows, int worldCountries) {
        System.out.println("================================================================================");
        System.out.println("                     DATASET VERIFICATION & INSPECTION");
        System.out.println("================================================================================");

        File rawDir = new File(ConfigLoader.getLocalRawPath());
        File eeocFile = new File(rawDir, ConfigLoader.getEeocFile());
        File worldFile = new File(rawDir, ConfigLoader.getWorldFile());

        DecimalFormat df = new DecimalFormat("#.##");

        // Verify EEOC CSV
        System.out.println("\n[Verification 1] EEOC Charges Dataset:");
        if (eeocFile.exists()) {
            System.out.println("  Status   : EXISTS (OK)");
            System.out.println("  Location : " + eeocFile.getAbsolutePath());
            System.out.printf("  Size     : %s MB (%,d bytes)%n",
                    df.format(eeocFile.length() / (1024.0 * 1024.0)), eeocFile.length());
            System.out.println("  Sample Content (First 3 lines of CSV):");
            printFirstNLines(eeocFile, 3);
        } else {
            System.err.println("  Status   : MISSING! File not found at: " + eeocFile.getAbsolutePath());
        }

        // Verify WORLD Laws JSON
        System.out.println("\n[Verification 2] WORLD Laws Dataset:");
        if (worldFile.exists()) {
            System.out.println("  Status   : EXISTS (OK)");
            System.out.println("  Location : " + worldFile.getAbsolutePath());
            System.out.printf("  Size     : %s KB (%,d bytes)%n",
                    df.format(worldFile.length() / 1024.0), worldFile.length());
            System.out.println("  Sample Content (First country object from JSON):");
            printFirstJsonObject(worldFile);
        } else {
            System.err.println("  Status   : MISSING! File not found at: " + worldFile.getAbsolutePath());
        }

        // Summary Report
        double totalBytes = (eeocFile.exists() ? eeocFile.length() : 0) + (worldFile.exists() ? worldFile.length() : 0);
        double totalMb = totalBytes / (1024.0 * 1024.0);

        System.out.println("\n================================================================================");
        System.out.println("                     DATA INGESTION SUMMARY REPORT");
        System.out.println("================================================================================");
        System.out.printf("  EEOC Charges Records Downloaded/Generated : %,d rows%n", eeocRows);
        System.out.printf("  World Laws Countries Downloaded/Generated : %d countries%n", worldCountries);
        System.out.printf("  Total Raw Data Size                       : %s MB%n", df.format(totalMb));
        System.out.printf("  Destination Raw Directory                 : %s%n", rawDir.getAbsolutePath());
        System.out.println("================================================================================");
    }

    private static void printFirstNLines(File file, int n) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            int count = 0;
            while ((line = reader.readLine()) != null && count < n) {
                System.out.println("    Line " + (count + 1) + ": " + line);
                count++;
            }
        } catch (Exception e) {
            System.err.println("    Error reading sample lines: " + e.getMessage());
        }
    }

    private static void printFirstJsonObject(File file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            boolean inObject = false;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("{")) {
                    inObject = true;
                }
                if (inObject) {
                    System.out.println("    " + line);
                    if (line.trim().startsWith("}") || line.trim().startsWith("},")) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("    Error reading first JSON object: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        run();
    }
}