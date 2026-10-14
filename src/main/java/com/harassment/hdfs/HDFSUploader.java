package com.harassment.hdfs;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Handles uploading local Windows datasets to the WSL HDFS cluster
 * and verifying their integrity.
 */
public class HDFSUploader {

    private final FileSystem fs;

    public HDFSUploader() throws IOException {
        this.fs = HDFSConnectionManager.getFileSystem();
    }

    public HDFSUploader(FileSystem fs) {
        this.fs = fs;
    }

    /**
     * Uploads a single local file to the specified HDFS path.
     * Automatically creates parent directories in HDFS.
     * Verifies upload by comparing byte lengths.
     *
     * @param localPath Path to the local file on Windows
     * @param hdfsPath  Destination path on HDFS
     * @return true if upload succeeded and sizes match, false otherwise
     */
    public boolean uploadFile(String localPath, String hdfsPath) {
        File localFile = new File(localPath);
        if (!localFile.exists() || !localFile.isFile()) {
            System.err.println("[HDFSUploader] Error: Local file not found: " + localPath);
            return false;
        }

        long localSize = localFile.length();
        Path dstPath = new Path(hdfsPath);

        System.out.println("\n[HDFSUploader] Uploading: " + localFile.getAbsolutePath());
        System.out.println("            -> To HDFS: " + hdfsPath);
        System.out.printf("            -> Local file size: %,d bytes (%.2f MB)\n",
                localSize, (double) localSize / (1024 * 1024));

        try {
            // Ensure parent directory exists
            Path parentDir = dstPath.getParent();
            if (parentDir != null && !fs.exists(parentDir)) {
                System.out.println("[HDFSUploader] Creating parent HDFS directory: " + parentDir);
                fs.mkdirs(parentDir);
            }

            // Attempt standard Hadoop copyFromLocalFile, fallback to stream copy on Windows native issues
            boolean copied = false;
            try {
                fs.copyFromLocalFile(false, true, new Path(localFile.toURI()), dstPath);
                copied = true;
            } catch (Throwable t) {
                // Windows native library / winutils fallback
                System.out.println("[HDFSUploader] Direct copyFromLocalFile encountered: " + t.getMessage()
                        + " -> Using reliable stream transfer...");
            }

            if (!copied) {
                try (InputStream in = new BufferedInputStream(new FileInputStream(localFile));
                     OutputStream out = new BufferedOutputStream(fs.create(dstPath, true))) {
                    IOUtils.copyBytes(in, out, 65536, true);
                }
            }

            // Verify upload size
            FileStatus status = fs.getFileStatus(dstPath);
            long hdfsSize = status.getLen();
            System.out.printf("[HDFSUploader] Upload complete. HDFS file size: %,d bytes\n", hdfsSize);

            if (localSize == hdfsSize) {
                System.out.println("[HDFSUploader] \u2714 Verification SUCCESS: Local and HDFS file sizes match!");
                return true;
            } else {
                System.err.printf("[HDFSUploader] \u2718 Verification FAILED: Size mismatch (Local: %,d, HDFS: %,d)\n",
                        localSize, hdfsSize);
                return false;
            }
        } catch (Exception e) {
            System.err.println("[HDFSUploader] \u2718 Upload failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Uploads the EEOC charges CSV and WORLD laws JSON datasets to HDFS.
     * Prints a formatted summary table of all uploaded datasets.
     *
     * @return true if all datasets uploaded successfully
     */
    public boolean uploadAllDatasets() {
        String baseDir = System.getProperty("user.dir");

        // Resolve local dataset paths
        String eeocLocal = resolvePath(baseDir, "data/raw/eeoc_charges.csv");
        String worldLocal = resolvePath(baseDir, "data/raw/world_laws.json");

        String eeocHdfs = "/user/harassment/eeoc/eeoc_charges.csv";
        String worldHdfs = "/user/harassment/world/world_laws.json";

        System.out.println("================================================================================");
        System.out.println("               HDFS BATCH DATASET UPLOAD TO WSL CLUSTER                         ");
        System.out.println("================================================================================");

        boolean eeocSuccess = uploadFile(eeocLocal, eeocHdfs);
        boolean worldSuccess = uploadFile(worldLocal, worldHdfs);

        // Verification details
        System.out.println("\n--- Detailed HDFS File Attributes ---");
        verifyUpload(eeocHdfs);
        verifyUpload(worldHdfs);

        // Print Summary Table
        System.out.println("\n================================================================================");
        System.out.println("                              UPLOAD SUMMARY TABLE                              ");
        System.out.println("================================================================================");
        System.out.printf("%-14s | %-38s | %-12s | %-10s\n", "Dataset", "HDFS Target Path", "Size", "Status");
        System.out.println("--------------------------------------------------------------------------------");

        printSummaryRow("EEOC Charges", eeocHdfs, eeocSuccess);
        printSummaryRow("World Laws", worldHdfs, worldSuccess);

        System.out.println("================================================================================");

        return eeocSuccess && worldSuccess;
    }

    /**
     * Verifies the upload by querying HDFS metadata for the given path.
     *
     * @param hdfsPath The path in HDFS to verify
     * @return true if file exists and has non-zero length
     */
    public boolean verifyUpload(String hdfsPath) {
        Path path = new Path(hdfsPath);
        try {
            if (!fs.exists(path)) {
                System.out.printf("[HDFSUploader] \u2718 Verification failed: %s does not exist on HDFS.\n", hdfsPath);
                return false;
            }

            FileStatus status = fs.getFileStatus(path);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String modTime = sdf.format(new Date(status.getModificationTime()));

            System.out.println("HDFS Path:          " + status.getPath());
            System.out.printf("  Length:           %,d bytes (%.2f MB)\n", status.getLen(), (double) status.getLen() / (1024 * 1024));
            System.out.println("  Replication:      " + status.getReplication());
            System.out.printf("  Block Size:       %,d bytes (%.1f MB)\n", status.getBlockSize(), (double) status.getBlockSize() / (1024 * 1024));
            System.out.println("  Owner / Group:    " + status.getOwner() + " : " + status.getGroup());
            System.out.println("  Permissions:      " + status.getPermission());
            System.out.println("  Modification:     " + modTime);
            return status.getLen() > 0;
        } catch (IOException e) {
            System.err.println("[HDFSUploader] Error verifying path " + hdfsPath + ": " + e.getMessage());
            return false;
        }
    }

    private void printSummaryRow(String name, String hdfsPath, boolean success) {
        String sizeStr = "N/A";
        try {
            Path path = new Path(hdfsPath);
            if (fs.exists(path)) {
                long len = fs.getFileStatus(path).getLen();
                if (len > 1024 * 1024) {
                    sizeStr = String.format("%.2f MB", (double) len / (1024 * 1024));
                } else {
                    sizeStr = String.format("%.2f KB", (double) len / 1024);
                }
            }
        } catch (Exception ignored) {}

        String statusStr = success ? "\u2714 SUCCESS" : "\u2718 FAILED";
        System.out.printf("%-14s | %-38s | %-12s | %-10s\n", name, hdfsPath, sizeStr, statusStr);
    }

    private String resolvePath(String baseDir, String relativePath) {
        File file = new File(baseDir, relativePath);
        if (file.exists()) {
            return file.getAbsolutePath();
        }
        // Fallback to absolute workspace location
        File fallback = new File("C:/Users/manis/Workplace-Harassment-Analytics", relativePath);
        return fallback.getAbsolutePath();
    }
}