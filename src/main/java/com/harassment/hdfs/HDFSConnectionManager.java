package com.harassment.hdfs;

import com.harassment.utils.ConfigLoader;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;

import java.io.IOException;
import java.net.URI;

/**
 * Manages cached HDFS FileSystem connections to the WSL Hadoop cluster.
 */
public class HDFSConnectionManager {

    private static FileSystem fileSystem = null;
    private static Configuration cachedConf = null;

    /**
     * Obtains or creates a cached FileSystem instance.
     *
     * @return Open HDFS FileSystem
     * @throws IOException on connection failure
     */
    public static synchronized FileSystem getFileSystem() throws IOException {
        if (fileSystem != null) {
            return fileSystem;
        }

        String uriStr = ConfigLoader.getFsDefaultFS();
        String wslUser = ConfigLoader.getWslUser();

        // Enforce Hadoop user identity to match WSL environment
        if (wslUser != null && !wslUser.isEmpty()) {
            System.setProperty("HADOOP_USER_NAME", wslUser);
        }

        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", uriStr);
        conf.set("dfs.client.use.datanode.hostname", "true");
        conf.set("dfs.datanode.use.datanode.hostname", "true");
        conf.set("ipc.client.connect.max.retries", "3");
        conf.set("ipc.client.connect.retry.interval", "1000");

        // Windows file system compatibility
        String hadoopHome = System.getenv("HADOOP_HOME");
        if (hadoopHome != null && !hadoopHome.isEmpty()) {
            conf.set("hadoop.home.dir", hadoopHome);
        }

        try {
            URI uri = URI.create(uriStr);
            fileSystem = FileSystem.get(uri, conf, wslUser);
            cachedConf = conf;
            return fileSystem;
        } catch (Exception e) {
            String ip = ConfigLoader.getWslIp();
            String errorMsg = String.format(
                    "Cannot reach HDFS at %s. Check that WSL Hadoop is running (jps in WSL should show NameNode). Root cause: %s",
                    uriStr, e.getMessage());
            System.err.println("[HDFSConnectionManager] " + errorMsg);
            throw new IOException(errorMsg, e);
        }
    }

    /**
     * Closes the active HDFS connection and invalidates the cached instance.
     */
    public static synchronized void close() {
        if (fileSystem != null) {
            try {
                fileSystem.close();
                System.out.println("[HDFSConnectionManager] HDFS connection closed successfully.");
            } catch (IOException e) {
                System.err.println("[HDFSConnectionManager] Error closing HDFS connection: " + e.getMessage());
            } finally {
                fileSystem = null;
                cachedConf = null;
            }
        }
    }

    public static Configuration getConfiguration() {
        return cachedConf != null ? cachedConf : new Configuration();
    }
}