package com.harassment.hive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.harassment.hdfs.HDFSConnectionManager;
import com.harassment.hdfs.HDFSFileOperations;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Verifies Hive outputs in HDFS, prints sample output records, downloads result files
 * locally to outputs/hive_results/, and compiles outputs/hive_results_summary.json.
 */
public class HiveResultVerifier {

    private final FileSystem fs;
    private final HDFSFileOperations fileOps;
    private final ObjectMapper objectMapper;

    public static final String[] OUTPUT_FOLDERS = {
            "hive_q1_total",
            "hive_q2_industries",
            "hive_q3_sex",
            "hive_q4_race",
            "hive_q5_yearly",
            "hive_q6_region_avg"
    };

    public HiveResultVerifier() throws IOException {
        this.fs = HDFSConnectionManager.getFileSystem();
        this.fileOps = new HDFSFileOperations(this.fs);
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Verifies and prints sample lines for all 6 Hive output folders in HDFS.
     *
     * @return true if all folders exist and have output files
     */
    public boolean verifyOutputs() {
        System.out.println("\n================================================================================");
        System.out.println("                 VERIFYING HIVE OUTPUT DATASETS IN HDFS                          ");
        System.out.println("================================================================================");

        boolean allExist = true;

        for (String folderName : OUTPUT_FOLDERS) {
            String hdfsPath = "/user/harassment/outputs/" + folderName;
            Path path = new Path(hdfsPath);

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println("Output Folder: " + hdfsPath);
            System.out.println("--------------------------------------------------------------------------------");

            try {
                if (!fs.exists(path)) {
                    System.err.println("  \u2718 Folder NOT found in HDFS: " + hdfsPath);
                    allExist = false;
                    continue;
                }

                FileStatus[] parts = fs.listStatus(path);
                long totalBytes = 0;
                List<Path> dataFiles = new ArrayList<>();

                for (FileStatus part : parts) {
                    totalBytes += part.getLen();
                    String name = part.getPath().getName();
                    if (!name.startsWith("_") && !part.isDirectory()) {
                        dataFiles.add(part.getPath());
                    }
                }

                System.out.printf("  Status: Available (%d files, %,d total bytes)\n", parts.length, totalBytes);
                System.out.println("  Sample Lines (First 10 records):");

                int linesPrinted = 0;
                for (Path dataFile : dataFiles) {
                    try (FSDataInputStream in = fs.open(dataFile);
                         BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null && linesPrinted < 10) {
                            linesPrinted++;
                            System.out.printf("    [%d] %s\n", linesPrinted, line);
                        }
                    }
                    if (linesPrinted >= 10) break;
                }

                if (linesPrinted == 0) {
                    System.out.println("    (No data records found in output files)");
                }

            } catch (IOException e) {
                System.err.println("  Error verifying folder " + hdfsPath + ": " + e.getMessage());
                allExist = false;
            }
        }

        System.out.println("\n================================================================================");
        return allExist;
    }

    /**
     * Downloads all output folders from HDFS to outputs/hive_results/FOLDER_NAME/
     * and compiles outputs/hive_results_summary.json.
     *
     * @return Path to summary JSON file
     */
    public String downloadResultsToLocal() throws IOException {
        String baseDir = System.getProperty("user.dir");
        File resultsBaseDir = new File(baseDir, "outputs/hive_results");
        if (!resultsBaseDir.exists()) {
            resultsBaseDir.mkdirs();
        }

        Map<String, Object> summaryMap = new LinkedHashMap<>();

        System.out.println("\n[HiveResultVerifier] Downloading Hive output files from HDFS to local directory...");

        for (String folderName : OUTPUT_FOLDERS) {
            String hdfsPath = "/user/harassment/outputs/" + folderName;
            Path path = new Path(hdfsPath);

            File localFolder = new File(resultsBaseDir, folderName);
            if (!localFolder.exists()) {
                localFolder.mkdirs();
            }

            List<String> records = new ArrayList<>();

            if (fs.exists(path)) {
                FileStatus[] statuses = fs.listStatus(path);
                for (FileStatus status : statuses) {
                    Path srcFile = status.getPath();
                    File dstFile = new File(localFolder, srcFile.getName());

                    // Download file
                    try (FSDataInputStream in = fs.open(srcFile);
                         FileOutputStream out = new FileOutputStream(dstFile)) {
                        IOUtils.copyBytes(in, out, 4096, true);
                    }

                    // Collect records if data file
                    if (!srcFile.getName().startsWith("_") && !status.isDirectory()) {
                        try (FSDataInputStream in = fs.open(srcFile);
                             BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = br.readLine()) != null) {
                                if (!line.trim().isEmpty()) {
                                    records.add(line);
                                }
                            }
                        }
                    }
                }

                System.out.printf("  \u2714 Downloaded %-25s -> %s (%d records)\n",
                        folderName, localFolder.getPath(), records.size());
            } else {
                System.err.println("  \u2718 Skip missing HDFS folder: " + hdfsPath);
            }

            summaryMap.put(folderName, records);
        }

        File summaryJsonFile = new File(baseDir, "outputs/hive_results_summary.json");
        objectMapper.writeValue(summaryJsonFile, summaryMap);
        System.out.println("[HiveResultVerifier] \u2714 Summary JSON saved to: " + summaryJsonFile.getAbsolutePath());

        return summaryJsonFile.getAbsolutePath();
    }
}
