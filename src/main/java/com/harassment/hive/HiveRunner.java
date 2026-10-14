package com.harassment.hive;

import com.harassment.hdfs.HDFSConnectionManager;
import com.harassment.hdfs.HDFSFileOperations;
import com.harassment.hdfs.WSLIPResolver;
import org.apache.hadoop.fs.FileSystem;

/**
 * Orchestrator for the Apache Hive workflow:
 * a) Resolves dynamic WSL IP via WSLIPResolver
 * b) Generates all HiveQL scripts (eeoc_analysis.hql, world_laws_analysis.hql)
 * c) Cleans stale HDFS output directories for idempotency
 * d) Executes both HiveQL scripts inside WSL in MapReduce mode
 * e) Verifies outputs in HDFS and displays sample records
 * f) Downloads all result partitions locally to outputs/hive_results/
 * g) Generates outputs/hive_results_summary.json and final summary report
 */
public class HiveRunner {

    public static void main(String[] args) {
        run();
    }

    public static void run() {
        System.out.println("================================================================================");
        System.out.println("            APACHE HIVE DATA WAREHOUSING & ANALYTICS PIPELINE                   ");
        System.out.println("================================================================================");

        try {
            // Step a: Ensure WSL IP and HDFS connection are synchronized
            System.out.println("\n[Step 1] Synchronizing WSL IP configuration...");
            String wslIp = WSLIPResolver.updateConfigWithWSLIP();
            System.out.println("[HiveRunner] Active WSL IP: " + wslIp);

            // Step b & c: Generate HiveQL analysis scripts
            System.out.println("\n[Step 2] Generating HiveQL analysis scripts...");
            HiveScriptGenerator generator = new HiveScriptGenerator();
            generator.generateAllScripts();
            System.out.println("Generated scripts: eeoc_analysis.hql, world_laws_analysis.hql");

            // Step 3: Clean up old output folders in HDFS for idempotency
            System.out.println("\n[Step 3] Preparing HDFS output directories...");
            FileSystem fs = HDFSConnectionManager.getFileSystem();
            HDFSFileOperations fileOps = new HDFSFileOperations(fs);

            for (String folder : HiveResultVerifier.OUTPUT_FOLDERS) {
                fileOps.deleteIfExists("/user/harassment/outputs/" + folder, true);
            }

            // Step d: Execute scripts inside WSL
            System.out.println("\n[Step 4] Executing HiveQL scripts in WSL...");
            HiveScriptExecutor executor = new HiveScriptExecutor();
            boolean executed = executor.executeAllScripts();

            if (!executed) {
                System.err.println("[HiveRunner] Warning: One or more Hive scripts encountered errors.");
            }

            // Step e: Verify outputs in HDFS
            System.out.println("\n[Step 5] Verifying output datasets in HDFS...");
            HiveResultVerifier verifier = new HiveResultVerifier();
            boolean verified = verifier.verifyOutputs();

            // Step f: Download results to local outputs/hive_results/
            System.out.println("\n[Step 6] Downloading Hive outputs to local workspace...");
            String summaryJson = verifier.downloadResultsToLocal();

            // Step g: Print Final Summary
            printFinalSummary(verified, summaryJson);

        } catch (Exception e) {
            System.err.println("[HiveRunner] Unexpected error during Hive workflow: " + e.getMessage());
            e.printStackTrace();
        } finally {
            HDFSConnectionManager.close();
        }
    }

    private static void printFinalSummary(boolean verified, String summaryJson) {
        System.out.println("\n================================================================================");
        System.out.println("                         HIVE PIPELINE SUMMARY REPORT                           ");
        System.out.println("================================================================================");
        System.out.println("Scripts Executed:       2 (eeoc_analysis.hql, world_laws_analysis.hql)");
        System.out.println("Tables Created:         2 (eeoc_charges, world_laws in harassment_db)");
        System.out.println("Output Folders Created: 6 HDFS folders in /user/harassment/outputs/");
        System.out.println("  1. hive_q1_total        - Total EEOC charges count");
        System.out.println("  2. hive_q2_industries   - Top 10 industries by charge count");
        System.out.println("  3. hive_q3_sex          - EEOC charges grouped by charging party sex");
        System.out.println("  4. hive_q4_race         - EEOC charges grouped by charging party race");
        System.out.println("  5. hive_q5_yearly       - EEOC charges grouped by filing year");
        System.out.println("  6. hive_q6_region_avg   - Region-wise average protection score and count");
        System.out.println("Results Downloaded To:  outputs/hive_results/");
        System.out.println("Summary JSON File:      " + summaryJson);
        System.out.println("Overall Status:         " + (verified ? "SUCCESS \u2714" : "PARTIAL / VERIFICATION WARNINGS \u26A0"));
        System.out.println("\nNext Pipeline Steps Ready:");
        System.out.println("  - Option 6: Sqoop Import/Export");
        System.out.println("  - Option 7: Company Linking (Text Analytics)");
        System.out.println("================================================================================\n");
    }
}
