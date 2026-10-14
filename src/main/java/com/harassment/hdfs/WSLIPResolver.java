package com.harassment.hdfs;

import com.harassment.utils.ConfigLoader;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Resolves the dynamic WSL IP address, updates configuration properties
 * (for HDFS and MongoDB), and tests connectivity to the HDFS NameNode.
 */
public class WSLIPResolver {

    /**
     * Executes `wsl -d Ubuntu bash -c "hostname -I"` to extract the current WSL IP.
     *
     * @return Detected WSL IP address
     */
    public static String getWSLIP() {
        try {
            ProcessBuilder pb = new ProcessBuilder("wsl", "-d", "Ubuntu", "bash", "-c", "hostname -I");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append(" ");
                }
            }

            process.waitFor(5, TimeUnit.SECONDS);
            String output = sb.toString().trim();
            if (!output.isEmpty()) {
                String[] tokens = output.split("\\s+");
                if (tokens.length > 0 && !tokens[0].isEmpty()) {
                    return tokens[0].trim();
                }
            }
        } catch (Exception e) {
            System.err.println("[WSLIPResolver] Error querying WSL IP: " + e.getMessage());
        }
        return "127.0.0.1";
    }

    /**
     * Reads config.properties, updates wsl.ip, fs.defaultFS, and mongo.uri, and writes back.
     *
     * @return The updated WSL IP
     */
    public static String updateConfigWithWSLIP() {
        String currentIp = getWSLIP();
        System.out.println("[WSLIPResolver] Detected current WSL IP address: " + currentIp);

        String fsDefaultFS = "hdfs://" + currentIp + ":9000";
        String mongoUri = "mongodb://" + currentIp + ":27017";

        File configFile = new File("config/config.properties");
        if (!configFile.exists()) {
            configFile = new File(ConfigLoader.getProperty("config.file", "config/config.properties"));
        }

        updateConfigFile(configFile, currentIp, fsDefaultFS, mongoUri);

        File resConfigFile = new File("src/main/resources/config.properties");
        if (resConfigFile.exists()) {
            updateConfigFile(resConfigFile, currentIp, fsDefaultFS, mongoUri);
        }

        // Update in-memory properties
        ConfigLoader.setProperty("wsl.ip", currentIp);
        ConfigLoader.setProperty("fs.defaultFS", fsDefaultFS);
        ConfigLoader.setProperty("mongo.uri", mongoUri);

        return currentIp;
    }

    private static void updateConfigFile(File file, String ip, String fsDefaultFS, String mongoUri) {
        if (!file.exists()) {
            return;
        }

        List<String> lines = new ArrayList<>();
        boolean foundWslIp = false;
        boolean foundFsDefault = false;
        boolean foundMongoUri = false;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("wsl.ip=")) {
                    lines.add("wsl.ip=" + ip);
                    foundWslIp = true;
                } else if (line.trim().startsWith("fs.defaultFS=")) {
                    lines.add("fs.defaultFS=" + fsDefaultFS);
                    foundFsDefault = true;
                } else if (line.trim().startsWith("mongo.uri=")) {
                    lines.add("mongo.uri=" + mongoUri);
                    foundMongoUri = true;
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("[WSLIPResolver] Error reading " + file.getPath() + ": " + e.getMessage());
            return;
        }

        if (!foundWslIp) {
            lines.add("wsl.ip=" + ip);
        }
        if (!foundFsDefault) {
            lines.add("fs.defaultFS=" + fsDefaultFS);
        }
        if (!foundMongoUri) {
            lines.add("mongo.uri=" + mongoUri);
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8)) {
            for (String l : lines) {
                writer.write(l + "\n");
            }
            writer.flush();
        } catch (IOException e) {
            System.err.println("[WSLIPResolver] Error writing " + file.getPath() + ": " + e.getMessage());
        }
    }

    /**
     * Tries to connect to hdfs://<wslIP>:9000 using Hadoop FileSystem API.
     *
     * @param wslIP WSL IP address
     * @return true if reachable and responds
     */
    public static boolean testConnection(String wslIP) {
        String uri = "hdfs://" + wslIP + ":9000";
        System.out.println("[WSLIPResolver] Testing HDFS connection to " + uri + " ...");

        try {
            Configuration conf = new Configuration();
            conf.set("fs.defaultFS", uri);
            conf.set("dfs.client.use.datanode.hostname", "true");
            conf.set("ipc.client.connect.max.retries", "2");
            conf.set("ipc.client.connect.retry.interval", "1000");
            System.setProperty("HADOOP_USER_NAME", ConfigLoader.getWslUser());

            FileSystem fs = FileSystem.get(URI.create(uri), conf);
            Path root = new Path("/");
            boolean exists = fs.exists(root);
            if (exists) {
                System.out.println("[WSLIPResolver] SUCCESS: Connected to HDFS NameNode at " + uri);
                return true;
            }
        } catch (Exception e) {
            System.err.println("[WSLIPResolver] Connection failed for " + uri + ": " + e.getMessage());
        }

        return false;
    }
}