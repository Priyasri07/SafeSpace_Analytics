package com.harassment.hive;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Executes Apache HiveQL scripts inside WSL Ubuntu from the Windows Java environment.
 */
public class HiveScriptExecutor {

    private final String workspacePathInWsl;

    public HiveScriptExecutor() {
        this("/mnt/c/Users/manis/Workplace-Harassment-Analytics");
    }

    public HiveScriptExecutor(String workspacePathInWsl) {
        this.workspacePathInWsl = workspacePathInWsl;
    }

    /**
     * Executes a single HiveQL script inside WSL Ubuntu via ProcessBuilder.
     *
     * @param scriptName Name of the script (e.g., eeoc_analysis.hql)
     * @return Process exit code (0 for success)
     */
    public int executeHiveScript(String scriptName) {
        System.out.println("\n================================================================================");
        System.out.println("            EXECUTING HIVE SCRIPT: " + scriptName + " (WSL / MapReduce)");
        System.out.println("================================================================================");

        // Command string for WSL execution as required by prompt
        String wslCommand = String.format(
                "export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64; " +
                "cd %s && /home/priyasri/hive-3.1.3/bin/hive -f scripts/hive/%s",
                workspacePathInWsl, scriptName);

        List<String> command = new ArrayList<>();
        command.add("wsl");
        command.add("-d");
        command.add("Ubuntu");
        command.add("bash");
        command.add("-c");
        command.add(wslCommand);

        int exitCode = -1;
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            long startTime = System.currentTimeMillis();
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (isFilteredNoise(line)) {
                        continue;
                    }
                    System.out.println("  [Hive] " + line);
                }
            }

            exitCode = process.waitFor();
            long duration = (System.currentTimeMillis() - startTime) / 1000;

            if (exitCode == 0) {
                System.out.printf("[HiveScriptExecutor] \u2714 SUCCESS: Script '%s' completed successfully in %d seconds.\n",
                        scriptName, duration);
            } else {
                System.err.printf("[HiveScriptExecutor] \u2718 FAILED: Script '%s' exited with code %d after %d seconds.\n",
                        scriptName, exitCode, duration);
            }
        } catch (Exception e) {
            System.err.println("[HiveScriptExecutor] Error executing script " + scriptName + ": " + e.getMessage());
            e.printStackTrace();
        }

        return exitCode;
    }

    /**
     * Executes all generated HiveQL scripts and prints an overall execution summary.
     *
     * @return true if all scripts completed with exit code 0
     */
    public boolean executeAllScripts() {
        System.out.println("\n[HiveScriptExecutor] Starting batch execution of all HiveQL scripts...");

        int exit1 = executeHiveScript("eeoc_analysis.hql");
        int exit2 = executeHiveScript("world_laws_analysis.hql");

        boolean allPassed = (exit1 == 0 && exit2 == 0);

        System.out.println("\n================================================================================");
        System.out.println("                       HIVE EXECUTION BATCH SUMMARY                              ");
        System.out.println("================================================================================");
        System.out.printf("  1. eeoc_analysis.hql        : %s\n", (exit1 == 0 ? "\u2714 SUCCESS" : "\u2718 FAILED (" + exit1 + ")"));
        System.out.printf("  2. world_laws_analysis.hql  : %s\n", (exit2 == 0 ? "\u2714 SUCCESS" : "\u2718 FAILED (" + exit2 + ")"));
        System.out.println("================================================================================");

        return allPassed;
    }

    private boolean isFilteredNoise(String line) {
        if (line == null || line.trim().isEmpty()) {
            return true;
        }
        String lower = line.toLowerCase();
        return lower.startsWith("slf4j: class path contains multiple")
                || lower.startsWith("slf4j: found binding")
                || lower.startsWith("slf4j: see http://www.slf4j.org")
                || lower.startsWith("slf4j: actual binding is of type")
                || lower.contains("screen size is bogus");
    }
}
