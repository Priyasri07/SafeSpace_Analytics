package com.harassment.mongodb;

import com.harassment.hdfs.WSLIPResolver;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.io.File;

/**
 * Orchestrator for the MongoDB pipeline:
 * 1. Resolves dynamic WSL IP and updates mongo.uri
 * 2. Tests connectivity to MongoDB
 * 3. Drops and loads world_laws.json (193 documents)
 * 4. Creates analytical indexes
 * 5. Verifies document count and schema
 * 6. Executes 6 analytical queries and saves results to outputs/mongodb_query_results.json
 * 7. Prints execution summary report
 */
public class MongoDBRunner {

    public static void main(String[] args) {
        run();
    }

    public static void run() {
        System.out.println("================================================================================");
        System.out.println("             MONGODB DATA INGESTION & GLOBAL LEGAL ANALYTICS                    ");
        System.out.println("================================================================================");

        try {
            // Step a: Resolve current WSL IP
            System.out.println("\n[Step 1] Resolving dynamic WSL IP address...");
            String wslIp = WSLIPResolver.updateConfigWithWSLIP();
            System.out.println("[MongoDBRunner] Active WSL IP: " + wslIp);

            // Step b: Test connection
            System.out.println("\n[Step 2] Testing MongoDB connectivity...");
            boolean connected = MongoDBConnectionManager.testConnection();

            if (!connected) {
                printInstructions();
                return;
            }

            MongoCollection<Document> collection = MongoDBConnectionManager.getCollection();
            if (collection == null) {
                System.err.println("[MongoDBRunner] Error: Unable to obtain MongoDB collection.");
                printInstructions();
                return;
            }

            // Step c: Load world_laws.json
            System.out.println("\n[Step 3] Loading world_laws.json into collection '"
                    + collection.getNamespace().getCollectionName() + "'...");
            WorldLawsLoader loader = new WorldLawsLoader();

            String baseDir = System.getProperty("user.dir");
            String jsonPath = resolvePath(baseDir, "data/raw/world_laws.json");
            int inserted = loader.loadJsonFile(jsonPath, collection);

            // Step d: Create Indexes
            System.out.println("\n[Step 4] Creating collection indexes...");
            loader.createIndexes(collection);

            // Step e: Verify load
            System.out.println("\n[Step 5] Verifying collection data...");
            boolean verified = loader.verifyLoad(collection);

            // Step f: Run 6 queries & export results
            System.out.println("\n[Step 6] Running 6 analytical queries...");
            WorldLawsQueries queries = new WorldLawsQueries(collection);
            String outputPath = resolvePath(baseDir, "outputs/mongodb_query_results.json");
            queries.runAllQueries(outputPath);

            // Step g: Print Final Summary
            printFinalSummary(inserted, outputPath, verified);

        } catch (Exception e) {
            System.err.println("[MongoDBRunner] Unexpected error during MongoDB workflow: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Step e: Close connection
            MongoDBConnectionManager.close();
        }
    }

    private static void printInstructions() {
        System.err.println("\n================================================================================");
        System.err.println("                     MONGODB CONNECTION FAILURE INSTRUCTIONS                    ");
        System.err.println("================================================================================");
        System.err.println("Cannot connect to MongoDB. Ensure:");
        System.err.println("  1. Open WSL Ubuntu terminal");
        System.err.println("  2. Run: sudo service mongod start");
        System.err.println("  3. Verify /etc/mongod.conf has bindIp: 0.0.0.0");
        System.err.println("  4. Verify port 27017 is reachable from Windows");
        System.err.println("  5. Re-run this option");
        System.err.println("================================================================================\n");
    }

    private static void printFinalSummary(int docsStored, String outputPath, boolean verified) {
        System.out.println("\n================================================================================");
        System.out.println("                        MONGODB PIPELINE SUMMARY REPORT                         ");
        System.out.println("================================================================================");
        System.out.println("Cluster Target:         " + MongoDBConnectionManager.getActiveUri());
        System.out.println("Database:               " + MongoDBConnectionManager.getActiveDbName());
        System.out.println("Collection:             " + MongoDBConnectionManager.getActiveCollectionName());
        System.out.println("Documents Stored:       " + docsStored + (verified ? " (Verified 193 \u2714)" : ""));
        System.out.println("Indexes Created:        4 (country, country_code, protection_score, region)");
        System.out.println("Queries Executed:       6 (Q1 through Q6 analytical queries)");
        System.out.println("Results Saved To:       " + outputPath);
        System.out.println("Status:                 SUCCESS \u2714");
        System.out.println("\nNext Pipeline Steps Ready:");
        System.out.println("  - Option 4: Run Pig Analysis on HDFS EEOC Charges");
        System.out.println("  - Option 5: Run Hive Analytics on Harassment Data");
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