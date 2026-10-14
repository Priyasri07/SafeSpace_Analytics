package com.harassment.hdfs;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;

/**
 * Orchestrator for HDFS operations:
 * 1. Resolves dynamic WSL IP address
 * 2. Updates configuration
 * 3. Tests HDFS connectivity
 * 4. Creates directory hierarchy
 * 5. Uploads datasets (EEOC CSV + World Laws JSON)
 * 6. Verifies files and displays samples
 * 7. Generates complete pipeline summary report
 */
public class HDFSRunner {

    public static void main(String[] args) {
        run();
    }

    public static void run() {
        System.out.println("================================================================================");
        System.out.println("                HDFS DATASET INGESTION & PIPELINE STAGING                       ");
        System.out.println("================================================================================");

        try {
            // Step a: Resolve current WSL IP
            System.out.println("\n[Step 1] Resolving current WSL IP address...");
            String wslIP = WSLIPResolver.getWSLIP();
            if (wslIP == null || wslIP.isEmpty()) {
                System.err.println("[HDFSRunner] Failed to detect WSL IP address. Ensure WSL Ubuntu is accessible.");
                printTroubleshootingInstructions();
                return;
            }
            System.out.println("[HDFSRunner] Detected WSL IP: " + wslIP);

            // Step b: Update config/config.properties
            System.out.println("\n[Step 2] Updating configuration properties...");
            WSLIPResolver.updateConfigWithWSLIP();

            // Step c: Test connection
            System.out.println("\n[Step 3] Testing HDFS connection to hdfs://" + wslIP + ":9000 ...");
            boolean connected = WSLIPResolver.testConnection(wslIP);

            if (!connected) {
                printTroubleshootingInstructions();
                return;
            }

            System.out.println("\n\u2714 Successfully connected to HDFS cluster!");

            // Step d: Perform HDFS operations
            FileSystem fs = HDFSConnectionManager.getFileSystem();
            HDFSFileOperations fileOps = new HDFSFileOperations(fs);
            HDFSUploader uploader = new HDFSUploader(fs);

            System.out.println("\n[Step 4] Creating required HDFS directory structure...");
            String[] targetDirs = new String[]{
                    "/user/harassment/eeoc",
                    "/user/harassment/world",
                    "/user/harassment/outputs",
                    "/user/harassment/mysql_import"
            };
            for (String dir : targetDirs) {
                fileOps.createDirectory(dir);
            }

            System.out.println("\n[Step 5] Uploading datasets to HDFS...");
            boolean uploadSuccess = uploader.uploadAllDatasets();

            System.out.println("\n[Step 6] Listing directory contents...");
            fileOps.listFiles("/user/harassment/eeoc");
            fileOps.listFiles("/user/harassment/world");

            System.out.println("\n[Step 7] Sampling data directly from HDFS...");
            fileOps.readFirstNLines("/user/harassment/eeoc/eeoc_charges.csv", 3);

            // Final HDFS Summary Report
            printFinalReport(fileOps, wslIP, uploadSuccess);

        } catch (Exception e) {
            System.err.println("[HDFSRunner] Unexpected error during HDFS workflow: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Step f: Close connection
            HDFSConnectionManager.close();
        }
    }

    private static void printTroubleshootingInstructions() {
        System.err.println("\n================================================================================");
        System.err.println("                       HDFS CONNECTION FAILURE INSTRUCTIONS                    ");
        System.err.println("================================================================================");
        System.err.println("HDFS is not reachable. To start WSL Hadoop:");
        System.err.println("  1. Open WSL Ubuntu terminal");
        System.err.println("  2. Run: start-dfs.sh");
        System.err.println("  3. Run: jps (verify NameNode is running)");
        System.err.println("  4. Re-run this option");
        System.err.println("================================================================================\n");
    }

    private static void printFinalReport(HDFSFileOperations fileOps, String wslIP, boolean uploadSuccess) {
        System.out.println("\n================================================================================");
        System.out.println("                         HDFS PIPELINE EXECUTION REPORT                         ");
        System.out.println("================================================================================");
        System.out.println("Cluster URI:            hdfs://" + wslIP + ":9000");
        System.out.println("Upload Status:          " + (uploadSuccess ? "SUCCESS \u2714" : "PARTIAL / FAILED \u2718"));

        System.out.println("\nStaged HDFS Assets:");
        FileStatus eeocStatus = fileOps.getFileInfo("/user/harassment/eeoc/eeoc_charges.csv");
        FileStatus worldStatus = fileOps.getFileInfo("/user/harassment/world/world_laws.json");

        if (eeocStatus != null && worldStatus != null) {
            System.out.printf("  1. EEOC Charges:      %,d bytes (%s)\n", eeocStatus.getLen(), eeocStatus.getPath());
            System.out.printf("  2. World Laws:        %,d bytes (%s)\n", worldStatus.getLen(), worldStatus.getPath());
        }

        System.out.println("\nNext Pipeline Steps Ready:");
        System.out.println("  - Option 3: Load WORLD data into MongoDB");
        System.out.println("  - Option 4: Run Pig Analysis on HDFS EEOC Charges");
        System.out.println("  - Option 5: Run Hive Analytics on HDFS Datasets");
        System.out.println("================================================================================\n");
    }
}