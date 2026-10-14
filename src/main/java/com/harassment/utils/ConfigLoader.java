package com.harassment.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Utility class to load configuration properties from classpath (config.properties)
 * or local config folder. Provides static accessors for all defined settings.
 */
public class ConfigLoader {

    private static final Properties properties = new Properties();
    private static boolean loaded = false;

    static {
        loadProperties();
    }

    public static synchronized void reloadProperties() {
        loaded = false;
        properties.clear();
        loadProperties();
    }

    private static synchronized void loadProperties() {
        if (loaded) {
            return;
        }

        // 1. Try loading from Thread Context ClassLoader
        InputStream inStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties");

        // 2. Fallback to ConfigLoader ClassLoader
        if (inStream == null) {
            inStream = ConfigLoader.class.getClassLoader().getResourceAsStream("config.properties");
        }

        // 3. Fallback to Class getResourceAsStream with leading slash
        if (inStream == null) {
            inStream = ConfigLoader.class.getResourceAsStream("/config.properties");
        }

        if (inStream != null) {
            try {
                properties.load(inStream);
                loaded = true;
            } catch (IOException e) {
                System.err.println("[ConfigLoader] Error reading config.properties from classpath: " + e.getMessage());
            } finally {
                try {
                    inStream.close();
                } catch (IOException ignored) {
                }
            }
        } else {
            // 4. Fallback to filesystem path (e.g. config/config.properties)
            File configFile = new File("config/config.properties");
            if (configFile.exists()) {
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    properties.load(fis);
                    loaded = true;
                } catch (IOException e) {
                    System.err.println("[ConfigLoader] Error reading config/config.properties: " + e.getMessage());
                }
            } else {
                System.err.println("[ConfigLoader] Warning: config.properties could not be found on classpath or filesystem.");
            }
        }
    }

    // Generic property accessors
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public static void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    public static Properties getAllProperties() {
        Properties copy = new Properties();
        copy.putAll(properties);
        return copy;
    }

    // Specific property getters
    public static String getHdfsBasePath() {
        return properties.getProperty("hdfs.base.path", "/user/harassment");
    }

    public static String getFsDefaultFS() {
        return properties.getProperty("fs.defaultFS", "hdfs://localhost:9000");
    }

    public static String getWslIp() {
        return properties.getProperty("wsl.ip", "localhost");
    }

    public static String getWslUser() {
        return properties.getProperty("wsl.user", "priyasri");
    }

    public static boolean isUseDatanodeHostname() {
        return Boolean.parseBoolean(properties.getProperty("dfs.client.use.datanode.hostname", "true"));
    }

    public static String getMongoUri() {
        return properties.getProperty("mongo.uri");
    }

    public static String getMongoDb() {
        return properties.getProperty("mongo.db");
    }

    public static String getMongoCollection() {
        return properties.getProperty("mongo.collection");
    }

    public static String getMysqlHost() {
        return properties.getProperty("mysql.host");
    }

    public static String getMysqlPort() {
        return properties.getProperty("mysql.port");
    }

    public static int getMysqlPortAsInt() {
        String port = properties.getProperty("mysql.port", "3306");
        try {
            return Integer.parseInt(port.trim());
        } catch (NumberFormatException e) {
            return 3306;
        }
    }

    public static String getMysqlDatabase() {
        return properties.getProperty("mysql.database");
    }

    public static String getMysqlUser() {
        return properties.getProperty("mysql.user");
    }

    public static String getMysqlPassword() {
        return properties.getProperty("mysql.password");
    }

    public static String getLocalRawPath() {
        return properties.getProperty("local.raw.path");
    }

    public static String getLocalOutputPath() {
        return properties.getProperty("local.output.path");
    }

    public static String getEeocFile() {
        return properties.getProperty("eeoc.file");
    }

    public static String getWorldFile() {
        return properties.getProperty("world.file");
    }
}