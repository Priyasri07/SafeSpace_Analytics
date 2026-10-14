package com.harassment.hdfs;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Common HDFS file and directory operations for demonstrations and pipeline management.
 */
public class HDFSFileOperations {

    private final FileSystem fs;

    public HDFSFileOperations() throws IOException {
        this.fs = HDFSConnectionManager.getFileSystem();
    }

    public HDFSFileOperations(FileSystem fs) {
        this.fs = fs;
    }

    /**
     * Creates an HDFS directory (including any missing parent directories).
     *
     * @param hdfsPath Directory path on HDFS
     * @return true if directory exists or was created, false on error
     */
    public boolean createDirectory(String hdfsPath) {
        try {
            Path path = new Path(hdfsPath);
            if (fs.exists(path)) {
                System.out.println("[HDFSFileOperations] Directory already exists: " + hdfsPath);
                return true;
            }
            boolean success = fs.mkdirs(path);
            if (success) {
                System.out.println("[HDFSFileOperations] \u2714 Created HDFS directory: " + hdfsPath);
            } else {
                System.err.println("[HDFSFileOperations] \u2718 Failed to create directory: " + hdfsPath);
            }
            return success;
        } catch (IOException e) {
            System.err.println("[HDFSFileOperations] Error creating directory " + hdfsPath + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Lists files and sub-directories within the given HDFS path.
     *
     * @param hdfsPath HDFS directory path
     * @return Array of FileStatus objects, or empty array if none/error
     */
    public FileStatus[] listFiles(String hdfsPath) {
        try {
            Path path = new Path(hdfsPath);
            if (!fs.exists(path)) {
                System.out.println("[HDFSFileOperations] Path does not exist: " + hdfsPath);
                return new FileStatus[0];
            }

            FileStatus[] statuses = fs.listStatus(path);
            System.out.println("\nListing contents of: " + hdfsPath + " (" + statuses.length + " items)");
            System.out.println("-----------------------------------------------------------------------------------------");
            System.out.printf("%-45s | %-6s | %-12s | %-4s | %-19s\n",
                    "Path", "Type", "Size", "Repl", "Modified");
            System.out.println("-----------------------------------------------------------------------------------------");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            for (FileStatus status : statuses) {
                String type = status.isDirectory() ? "DIR" : "FILE";
                String sizeStr = status.isDirectory() ? "-" : formatSize(status.getLen());
                String replStr = status.isDirectory() ? "-" : String.valueOf(status.getReplication());
                String modTime = sdf.format(new Date(status.getModificationTime()));
                String name = status.getPath().getName();

                System.out.printf("%-45s | %-6s | %-12s | %-4s | %-19s\n",
                        name, type, sizeStr, replStr, modTime);
            }
            System.out.println("-----------------------------------------------------------------------------------------");
            return statuses;
        } catch (IOException e) {
            System.err.println("[HDFSFileOperations] Error listing path " + hdfsPath + ": " + e.getMessage());
            return new FileStatus[0];
        }
    }

    /**
     * Reads and displays the first N lines of an HDFS file.
     *
     * @param hdfsPath HDFS file path
     * @param n        Number of lines to read
     * @return List containing the first N lines
     */
    public List<String> readFirstNLines(String hdfsPath, int n) {
        List<String> lines = new ArrayList<>();
        Path path = new Path(hdfsPath);

        System.out.printf("\n[HDFSFileOperations] Reading first %d lines of: %s\n", n, hdfsPath);
        System.out.println("-----------------------------------------------------------------------------------------");

        try {
            if (!fs.exists(path)) {
                System.err.println("[HDFSFileOperations] Error: File does not exist: " + hdfsPath);
                return lines;
            }

            try (FSDataInputStream in = fs.open(path);
                 BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                int count = 0;
                while ((line = br.readLine()) != null && count < n) {
                    count++;
                    lines.add(line);
                    System.out.printf("[%2d] %s\n", count, line);
                }
            }
            System.out.println("-----------------------------------------------------------------------------------------");
        } catch (IOException e) {
            System.err.println("[HDFSFileOperations] Error reading file " + hdfsPath + ": " + e.getMessage());
        }

        return lines;
    }

    /**
     * Deletes the specified HDFS path if it exists.
     *
     * @param hdfsPath  HDFS path to delete
     * @param recursive true to delete recursively
     * @return true if deleted, false otherwise
     */
    public boolean deleteIfExists(String hdfsPath, boolean recursive) {
        try {
            Path path = new Path(hdfsPath);
            if (fs.exists(path)) {
                boolean deleted = fs.delete(path, recursive);
                if (deleted) {
                    System.out.println("[HDFSFileOperations] \u2714 Deleted: " + hdfsPath);
                } else {
                    System.err.println("[HDFSFileOperations] \u2718 Could not delete: " + hdfsPath);
                }
                return deleted;
            } else {
                System.out.println("[HDFSFileOperations] Path does not exist (skip delete): " + hdfsPath);
                return true;
            }
        } catch (IOException e) {
            System.err.println("[HDFSFileOperations] Error deleting " + hdfsPath + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves and prints metadata information for the specified HDFS file or directory.
     *
     * @param hdfsPath HDFS path
     * @return FileStatus object or null
     */
    public FileStatus getFileInfo(String hdfsPath) {
        try {
            Path path = new Path(hdfsPath);
            if (!fs.exists(path)) {
                System.err.println("[HDFSFileOperations] Path does not exist: " + hdfsPath);
                return null;
            }

            FileStatus status = fs.getFileStatus(path);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            System.out.println("\n[HDFSFileOperations] File Info for: " + hdfsPath);
            System.out.println("  Type:          " + (status.isDirectory() ? "Directory" : "File"));
            System.out.println("  Size:          " + formatSize(status.getLen()) + " (" + status.getLen() + " bytes)");
            System.out.println("  Replication:   " + status.getReplication());
            System.out.println("  Block Size:    " + formatSize(status.getBlockSize()));
            System.out.println("  Owner / Group: " + status.getOwner() + ":" + status.getGroup());
            System.out.println("  Permissions:   " + status.getPermission());
            System.out.println("  Modified:      " + sdf.format(new Date(status.getModificationTime())));

            return status;
        } catch (IOException e) {
            System.err.println("[HDFSFileOperations] Error getting file info for " + hdfsPath + ": " + e.getMessage());
            return null;
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", (double) bytes / 1024);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", (double) bytes / (1024 * 1024));
        return String.format("%.2f GB", (double) bytes / (1024 * 1024 * 1024));
    }
}