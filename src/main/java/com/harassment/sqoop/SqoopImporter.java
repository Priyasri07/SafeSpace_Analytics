package com.harassment.sqoop;

import com.harassment.hdfs.HDFSFileOperations;
import com.harassment.hdfs.WSLIPResolver;
import com.harassment.utils.ConfigLoader;
import org.apache.hadoop.fs.FileStatus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles importing relational data from MySQL into HDFS via Apache Sqoop.
 */
public class SqoopImporter {

    private final String wslIp;
    private final String dbUser;
    private final String dbPassword;

    public SqoopImporter() {
        this.wslIp = WSLIPResolver.getWSLIP();
        this.dbUser = ConfigLoader.getProperty("mysql.user", "root");
        this.dbPassword = ConfigLoader.getProperty("mysql.password", "root");
    }

    public SqoopImporter(String wslIp, String dbUser, String dbPassword) {
        this.wslIp = wslIp;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    public boolean importCompanyMetadata() {
        System.out.println("\n[SqoopImporter] Importing company_metadata (MySQL -> HDFS)...");

        List<String> args = new ArrayList<>(Arrays.asList(
                "import",
                "--connect", "jdbc:mysql://" + wslIp + ":3306/harassment_db",
                "--username", dbUser,
                "--password", dbPassword,
                "--table", "company_metadata",
                "--target-dir", "/user/harassment/mysql_import/company_metadata",
                "--as-textfile",
                "--fields-terminated-by", ",",
                "-m", "1",
                "--delete-target-dir"
        ));

        int exitCode = SqoopCommandRunner.execute(args);
        boolean success = (exitCode == 0);
        System.out.println("  Status: " + (success ? "✔ SUCCESS" : "✖ FAILED (Exit Code: " + exitCode + ")"));
        return success;
    }

    public void verifyImport() {
        String hdfsDir = "/user/harassment/mysql_import/company_metadata";
        System.out.println("\n[SqoopImporter] Verifying imported data in HDFS at: " + hdfsDir);

        try {
            HDFSFileOperations hdfsOps = new HDFSFileOperations();
            FileStatus[] files = hdfsOps.listFiles(hdfsDir);

            boolean foundData = false;
            if (files != null) {
                for (FileStatus file : files) {
                    if (file.isFile() && file.getPath().getName().startsWith("part-")) {
                        System.out.println("Reading first 10 lines from part file: " + file.getPath().getName());
                        List<String> lines = hdfsOps.readFirstNLines(file.getPath().toString(), 10);
                        foundData = true;
                        break;
                    }
                }
            }

            if (!foundData) {
                System.err.println("[SqoopImporter] Warning: No part-* data files found in " + hdfsDir);
            }
        } catch (Exception e) {
            System.err.println("[SqoopImporter] ✖ Error verifying HDFS import: " + e.getMessage());
        }
    }
}
