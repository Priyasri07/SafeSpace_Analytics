package com.harassment.sqoop;

import com.harassment.hdfs.WSLIPResolver;

/**
 * Orchestrates the end-to-end Apache Sqoop import/export pipeline.
 */
public class SqoopRunner {

    public static void run() {
        System.out.println("========================================================================");
        System.out.println("             Apache Sqoop Module - Bidirectional SQL <-> HDFS           ");
        System.out.println("========================================================================");

        try {
            // Step a: Resolve WSL IP
            System.out.println("\n[Step 1] Resolving dynamic WSL IP...");
            String wslIp = WSLIPResolver.getWSLIP();
            System.out.println("  Detected WSL IP: " + wslIp);

            // Step b & c: Create MySQL database + 6 tables and insert sample rows
            System.out.println("\n[Step 2] Setting up MySQL Schema & Seed Data...");
            MySQLSchemaManager schemaManager = new MySQLSchemaManager();
            boolean schemaReady = schemaManager.createDatabaseAndTables();
            if (!schemaReady) {
                System.err.println("✖ Aborting Sqoop workflow: MySQL schema creation failed.");
                return;
            }

            schemaManager.insertSampleCompanyMetadata();
            System.out.println("MySQL schema ready: 6 tables created");
            schemaManager.verifyTables();

            // Step e: EXPORT: HDFS -> MySQL (5 tables)
            System.out.println("\n[Step 3] Exporting 5 Analytical Output Datasets from HDFS to MySQL...");
            SqoopExporter exporter = new SqoopExporter();
            int exportsSucceeded = exporter.exportAll();

            // Step f: Verify exports in MySQL
            System.out.println("\n[Step 4] Verifying Exported Results in MySQL...");
            MySQLResultVerifier verifier = new MySQLResultVerifier();
            verifier.verifyAll();

            // Step g: IMPORT: MySQL -> HDFS (1 table: company_metadata)
            System.out.println("\n[Step 5] Importing Relational Metadata from MySQL to HDFS...");
            SqoopImporter importer = new SqoopImporter();
            boolean importSucceeded = importer.importCompanyMetadata();
            int importsSucceeded = importSucceeded ? 1 : 0;

            // Step h: Verify import in HDFS
            System.out.println("\n[Step 6] Verifying Imported Data in HDFS...");
            importer.verifyImport();

            // Step i: Final Summary
            System.out.println("\n========================================================================");
            System.out.println("                         Sqoop Execution Summary                        ");
            System.out.println("========================================================================");
            System.out.println("  - Tables created: 6");
            System.out.println("  - Exports succeeded: " + exportsSucceeded + " / 5");
            System.out.println("  - Imports succeeded: " + importsSucceeded + " / 1");
            if (exportsSucceeded == 5 && importsSucceeded == 1) {
                System.out.println("  - Bidirectional SQL ↔ HDFS demonstrated: SUCCESS");
            } else {
                System.out.println("  - Bidirectional SQL ↔ HDFS demonstrated: PARTIAL/FAILED");
            }
            System.out.println("========================================================================");

        } catch (Exception e) {
            System.err.println("[SqoopRunner] ✖ Unexpected error during Sqoop workflow: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
