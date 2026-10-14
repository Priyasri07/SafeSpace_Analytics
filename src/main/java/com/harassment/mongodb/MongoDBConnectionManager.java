package com.harassment.mongodb;

import com.harassment.hdfs.WSLIPResolver;
import com.harassment.utils.ConfigLoader;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.concurrent.TimeUnit;

/**
 * Manages the MongoDB connection from Windows to the MongoDB instance running in WSL Ubuntu.
 */
public class MongoDBConnectionManager {

    private static MongoClient mongoClient = null;
    private static MongoDatabase mongoDatabase = null;
    private static String activeUri = null;
    private static String activeDbName = null;
    private static String activeCollectionName = null;

    /**
     * Initializes or retrieves the singleton MongoDatabase instance.
     * Auto-detects the current WSL IP and refreshes the connection string if needed.
     *
     * @return Active MongoDatabase
     */
    public static synchronized MongoDatabase getDatabase() {
        if (mongoDatabase != null && mongoClient != null) {
            return mongoDatabase;
        }

        // Auto-detect dynamic WSL IP and update config
        String wslIp = WSLIPResolver.updateConfigWithWSLIP();

        activeUri = ConfigLoader.getMongoUri();
        if (activeUri == null || activeUri.isEmpty()) {
            activeUri = "mongodb://" + wslIp + ":27017";
        }

        activeDbName = ConfigLoader.getMongoDb();
        if (activeDbName == null || activeDbName.isEmpty()) {
            activeDbName = "harassment_db";
        }

        activeCollectionName = ConfigLoader.getMongoCollection();
        if (activeCollectionName == null || activeCollectionName.isEmpty()) {
            activeCollectionName = "world_laws";
        }

        try {
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(activeUri))
                    .applyToClusterSettings(builder ->
                            builder.serverSelectionTimeout(5000, TimeUnit.MILLISECONDS))
                    .applyToSocketSettings(builder ->
                            builder.connectTimeout(5000, TimeUnit.MILLISECONDS)
                                   .readTimeout(10000, TimeUnit.MILLISECONDS))
                    .build();

            mongoClient = MongoClients.create(settings);
            mongoDatabase = mongoClient.getDatabase(activeDbName);
            return mongoDatabase;
        } catch (Exception e) {
            printConnectionFailure(activeUri, e.getMessage());
            return null;
        }
    }

    /**
     * Retrieves the target collection (default: world_laws).
     *
     * @return MongoCollection of Document
     */
    public static synchronized MongoCollection<Document> getCollection() {
        MongoDatabase db = getDatabase();
        if (db == null) {
            return null;
        }
        String collName = activeCollectionName != null ? activeCollectionName : "world_laws";
        return db.getCollection(collName);
    }

    /**
     * Tests connectivity to MongoDB by executing a ping command.
     *
     * @return true if ping succeeds, false otherwise
     */
    public static synchronized boolean testConnection() {
        try {
            MongoDatabase db = getDatabase();
            if (db == null) {
                return false;
            }
            Document pingResult = db.runCommand(new Document("ping", 1));
            boolean success = pingResult != null && pingResult.containsKey("ok");
            if (success) {
                System.out.println("[MongoDBConnectionManager] SUCCESS: Connected to MongoDB at " + activeUri);
                System.out.println("[MongoDBConnectionManager] Target Database: " + activeDbName);
                return true;
            } else {
                printConnectionFailure(activeUri, "Ping returned non-ok response");
                return false;
            }
        } catch (Exception e) {
            printConnectionFailure(activeUri, e.getMessage());
            return false;
        }
    }

    /**
     * Closes the MongoClient connection.
     */
    public static synchronized void close() {
        if (mongoClient != null) {
            try {
                mongoClient.close();
                System.out.println("[MongoDBConnectionManager] MongoDB connection closed successfully.");
            } catch (Exception e) {
                System.err.println("[MongoDBConnectionManager] Error closing MongoDB connection: " + e.getMessage());
            } finally {
                mongoClient = null;
                mongoDatabase = null;
            }
        }
    }

    private static void printConnectionFailure(String uri, String reason) {
        System.err.println("\n[MongoDBConnectionManager] Cannot connect to MongoDB at " + uri + " (" + reason + "). Ensure:");
        System.err.println("  1. WSL MongoDB is running (sudo service mongod start)");
        System.err.println("  2. mongod.conf has bindIp: 0.0.0.0");
        System.err.println("  3. Port 27017 is reachable from Windows");
    }

    public static String getActiveUri() {
        return activeUri;
    }

    public static String getActiveDbName() {
        return activeDbName;
    }

    public static String getActiveCollectionName() {
        return activeCollectionName;
    }
}