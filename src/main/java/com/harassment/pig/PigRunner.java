package com.harassment.pig;

import com.harassment.hdfs.HDFSConnectionManager;
import com.harassment.hdfs.HDFSFileOperations;
import com.harassment.hdfs.WSLIPResolver;
import org.apache.hadoop.fs.FileSystem;

import java.io.File;

/**
 * Orchestrator for the Apache Pig workflow:
 * 1. Flattens world_laws.json into world_laws_pig.csv
 * 2. Uploads world_laws_pig.csv to HDFS
 * 3. Generates world_laws_analysis.pig and eeoc_industry_analysis.pig
 * 4. Cleans up stale HDFS output directories
 * 5. Executes both Pig Latin scripts inside WSL in MapReduce mode
 * 6. Verifies outputs in HDFS and displays sample records
 * 7. Downloads all result partitions locally to outputs/pig_results/
 * 8. Generates outputs/pig_results_summary.json and final summary report
 */
public class PigRunner {

    public static void main(String[] args) {
        run();
    }

    public static void run() {
        System.out.println("================================================================================");
        System.out.println("            APACHE PIG DATA PROCESSING & ANALYTICS PIPELINE                     ");
        System.out.println("================================================================================");

        try {
            // Step 0: Ensure WSL IP and HDFS connection are synchronized
            System.out.println("\n[Step 1] Synchronizing WSL IP configuration...");
            String wslIp = WSLIPResolver.updateConfigWithWSLIP();
            System.out.println("[PigRunner] Active WSL IP: " + wslIp);

            // Step a & b: Convert world_laws.json -> CSV & Upload to HDFS
            System.out.println("\n[Step 2] Converting world_laws.json to flattened CSV for Pig...");
            WorldLawsJsonToCsv converter = new WorldLawsJsonToCsv();
            String baseDir = System.getProperty("user.dir");
            String jsonPath = resolvePath(baseDir, "data/raw/world_laws.json");
            String csvPath = resolvePath(baseDir, "data/processed/world_laws_pig.csv");

            boolean convertedAndUploaded = converter.convertAndUpload(jsonPath, csvPath);
            if (!convertedAndUploaded) {
                System.err.println("[PigRunner] \u2718 Failed to convert or upload world_laws_pig.csv. Aborting Pig pipeline.");
                return;
            }

            // Step c & d: Generate Pig Latin scripts
            System.out.println("\n[Step 3] Generating Pig Latin analysis scripts...");
            PigScriptGenerator generator = new PigScriptGenerator();
            generator.generateAllScripts();
            System.out.println("Generated scripts: world_laws_analysis.pig, eeoc_industry_analysis.pig");

            // Step 4: Clean up old output folders in HDFS for idempotency
            System.out.println("\n[Step 4] Preparing HDFS output directories...");
            FileSystem fs = HDFSConnectionManager.getFileSystem();
            HDFSFileOperations fileOps = new HDFSFileOperations(fs);

            for (String folder : PigResultVerifier.OUTPUT_FOLDERS) {
                fileOps.deleteIfExists("/user/harassment/outputs/" + folder, true);
            }

            // Step e: Execute scripts inside WSL in MapReduce mode
            System.out.println("\n[Step 5] Executing Pig scripts in WSL MapReduce mode...");
            PigScriptExecutor executor = new PigScriptExecutor();
            boolean executed = executor.executeAllScripts();

            if (!executed) {
                System.err.println("[PigRunner] Warning: One or more Pig scripts failed during execution.");
            }

            // Step f: Verify outputs in HDFS
            System.out.println("\n[Step 6] Verifying output datasets in HDFS...");
            PigResultVerifier verifier = new PigResultVerifier();
            boolean verified = verifier.verifyOutputs();

            // Step g: Download results to local outputs/pig_results/
            System.out.println("\n[Step 7] Downloading Pig outputs to local workspace...");
            String summaryJson = verifier.downloadResultsToLocal();

            // Step h: Print Final Summary
            printFinalSummary(verified, summaryJson);

        } catch (Exception e) {
            System.err.println("[PigRunner] Unexpected error during Pig workflow: " + e.getMessage());
            e.printStackTrace();
        } finally {
            HDFSConnectionManager.close();
        }
    }

    private static void printFinalSummary(boolean verified, String summaryJson) {
        System.out.println("\n================================================================================");
        System.out.println("                         PIG PIPELINE SUMMARY REPORT                            ");
        System.out.println("================================================================================");
        System.out.println("Scripts Executed:       2 (world_laws_analysis.pig, eeoc_industry_analysis.pig)");
        System.out.println("Output Folders Created: 6 HDFS folders in /user/harassment/outputs/");
        System.out.println("  1. pig_not_prohibited   - Countries where harassment is not prohibited");
        System.out.println("  2. pig_region_count     - Country count grouped and ordered by region");
        System.out.println("  3. pig_avg_protection   - Average protection score by region");
        System.out.println("  4. pig_full_protection  - Countries meeting all 6 legal indicators");
        System.out.println("  5. pig_industry_count   - EEOC charges grouped by industry description");
        System.out.println("  6. pig_sex_count        - EEOC charges grouped by charging party sex");
        System.out.println("Results Downloaded To:  outputs/pig_results/");
        System.out.println("Summary JSON File:      " + summaryJson);
        System.out.println("Overall Status:         " + (verified ? "SUCCESS \u2714" : "PARTIAL / VERIFICATION WARNINGS \u26A0"));
        System.out.println("\nNext Pipeline Steps Ready:");
        System.out.println("  - Option 5: Run Hive Analysis on Harassment Datasets");
        System.out.println("  - Option 6: Sqoop Import/Export");
        System.out.println("================================================================================\n");
    }

    private static String resolvePath(String baseDir, String relativePath) {
        File file = new File(baseDir, relativePath);
        if (file.exists()) {
            return file.getAbsolutePath();
        }
        File fallback = new File("C:/Users/manis/Workplace-Harassment-Analytics", relativePath);
        return fallback.getAbsolutePath();
    }
}