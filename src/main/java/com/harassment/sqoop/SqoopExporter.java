package com.harassment.sqoop;

import com.harassment.hdfs.WSLIPResolver;
import com.harassment.utils.ConfigLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles exporting analytical results from HDFS into MySQL tables via Apache Sqoop.
 */
public class SqoopExporter {

    private final String wslIp;
    private final String dbUser;
    private final String dbPassword;

    public SqoopExporter() {
        this.wslIp = WSLIPResolver.getWSLIP();
        this.dbUser = ConfigLoader.getProperty("mysql.user", "root");
        this.dbPassword = ConfigLoader.getProperty("mysql.password", "root");
    }

    public SqoopExporter(String wslIp, String dbUser, String dbPassword) {
        this.wslIp = wslIp;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    private List<String> buildBaseExportArgs(String table, String exportDir, String columns) {
        return new ArrayList<>(Arrays.asList(
                "export",
                "--connect", "jdbc:mysql://" + wslIp + ":3306/harassment_db",
                "--username", dbUser,
                "--password", dbPassword,
                "--table", table,
                "--export-dir", exportDir,
                "--input-fields-terminated-by", ",",
                "--columns", columns,
                "-m", "1"
        ));
    }

    public boolean exportIndustryRisk() {
        System.out.println("\n[SqoopExporter] Exporting Industry Risk (hive_q2_industries -> industry_risk)...");
        List<String> args = buildBaseExportArgs(
                "industry_risk",
                "/user/harassment/outputs/hive_q2_industries/",
                "naics_description,charge_count"
        );
        int exitCode = SqoopCommandRunner.execute(args);
        boolean success = (exitCode == 0);
        System.out.println("  Status: " + (success ? "✔ SUCCESS" : "✖ FAILED (Exit Code: " + exitCode + ")"));
        return success;
    }

    public boolean exportSexDistribution() {
        System.out.println("\n[SqoopExporter] Exporting Sex Distribution (hive_q3_sex -> sex_distribution)...");
        List<String> args = buildBaseExportArgs(
                "sex_distribution",
                "/user/harassment/outputs/hive_q3_sex/",
                "cp_sex,charge_count"
        );
        int exitCode = SqoopCommandRunner.execute(args);
        boolean success = (exitCode == 0);
        System.out.println("  Status: " + (success ? "✔ SUCCESS" : "✖ FAILED (Exit Code: " + exitCode + ")"));
        return success;
    }

    public boolean exportRaceDistribution() {
        System.out.println("\n[SqoopExporter] Exporting Race Distribution (hive_q4_race -> race_distribution)...");
        List<String> args = buildBaseExportArgs(
                "race_distribution",
                "/user/harassment/outputs/hive_q4_race/",
                "cp_race,charge_count"
        );
        int exitCode = SqoopCommandRunner.execute(args);
        boolean success = (exitCode == 0);
        System.out.println("  Status: " + (success ? "✔ SUCCESS" : "✖ FAILED (Exit Code: " + exitCode + ")"));
        return success;
    }

    public boolean exportYearlyTrend() {
        System.out.println("\n[SqoopExporter] Exporting Yearly Trend (hive_q5_yearly -> yearly_trend)...");
        List<String> args = buildBaseExportArgs(
                "yearly_trend",
                "/user/harassment/outputs/hive_q5_yearly/",
                "year,charge_count"
        );
        int exitCode = SqoopCommandRunner.execute(args);
        boolean success = (exitCode == 0);
        System.out.println("  Status: " + (success ? "✔ SUCCESS" : "✖ FAILED (Exit Code: " + exitCode + ")"));
        return success;
    }

    public boolean exportRegionProtection() {
        System.out.println("\n[SqoopExporter] Exporting Region Protection (hive_q6_region_avg -> region_protection)...");
        List<String> args = buildBaseExportArgs(
                "region_protection",
                "/user/harassment/outputs/hive_q6_region_avg/",
                "region,avg_protection_score,country_count"
        );
        int exitCode = SqoopCommandRunner.execute(args);
        boolean success = (exitCode == 0);
        System.out.println("  Status: " + (success ? "✔ SUCCESS" : "✖ FAILED (Exit Code: " + exitCode + ")"));
        return success;
    }

    public int exportAll() {
        System.out.println("\n=======================================================");
        System.out.println("      Starting Sqoop Exports (HDFS -> MySQL)");
        System.out.println("=======================================================");

        int successCount = 0;

        if (exportIndustryRisk()) successCount++;
        if (exportSexDistribution()) successCount++;
        if (exportRaceDistribution()) successCount++;
        if (exportYearlyTrend()) successCount++;
        if (exportRegionProtection()) successCount++;

        System.out.println("\n[SqoopExporter] Completed " + successCount + " of 5 exports successfully.");
        return successCount;
    }
}
