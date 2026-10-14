package com.harassment.dashboard.util;

import com.harassment.hdfs.WSLIPResolver;
import com.harassment.utils.ConfigLoader;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Shared utility for managing database connections to MySQL and MongoDB
 * running in the WSL environment with cached IP resolution.
 */
public class DatabaseHelper {

    private static String cachedWslIp = null;
    private static long lastIpCheckTime = 0;
    private static final long CACHE_DURATION_MS = 60_000; // 60 seconds

    private static MongoClient mongoClient = null;
    private static String mongoClientIp = null;

    /**
     * Retrieves the WSL IP address, caching it for 60 seconds.
     */
    public static synchronized String getWslIp() {
        long now = System.currentTimeMillis();
        if (cachedWslIp == null || (now - lastIpCheckTime) > CACHE_DURATION_MS) {
            cachedWslIp = WSLIPResolver.getWSLIP();
            lastIpCheckTime = now;
        }
        return cachedWslIp;
    }

    /**
     * Returns a JDBC connection to MySQL on WSL.
     */
    public static Connection getMySQLConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            // Driver registered via SPI
        }
        String ip = getWslIp();
        String user = ConfigLoader.getProperty("mysql.user", "root");
        String pass = ConfigLoader.getProperty("mysql.password", "root");
        String url = "jdbc:mysql://" + ip + ":3306/harassment_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        return DriverManager.getConnection(url, user, pass);
    }

    /**
     * Returns the world_laws MongoDB collection from WSL.
     */
    public static synchronized MongoCollection<Document> getMongoDBCollection() {
        String ip = getWslIp();
        if (mongoClient == null || !ip.equals(mongoClientIp)) {
            if (mongoClient != null) {
                try {
                    mongoClient.close();
                } catch (Exception ignored) {}
            }
            String uri = "mongodb://" + ip + ":27017";
            mongoClient = MongoClients.create(uri);
            mongoClientIp = ip;
        }
        MongoDatabase db = mongoClient.getDatabase("harassment_db");
        return db.getCollection("world_laws");
    }
}
