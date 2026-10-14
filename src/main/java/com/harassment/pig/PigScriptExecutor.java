package com.harassment.pig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Executes Apache Pig Latin scripts inside WSL Ubuntu from the Windows Java environment.
 */
public class PigScriptExecutor {

    private final String workspacePathInWsl;

    public PigScriptExecutor() {
        this("/mnt/c/Users/manis/Workplace-Harassment-Analytics");
    }

    public PigScriptExecutor(String workspacePathInWsl) {
        this.workspacePathInWsl = workspacePathInWsl;
    }

    /**
     * Executes a single Pig script inside WSL Ubuntu via ProcessBuilder.
     *
     * @param scriptName Name of the script (e.g., world_laws_analysis.pig)
     * @return Process exit code (0 for success)
     */
    public int executePigScript(String scriptName) {
        System.out.println("\n================================================================================");
        System.out.println("            EXECUTING PIG SCRIPT: " + scriptName + " (WSL / MapReduce)");
        System.out.println("================================================================================");

        // Environment variables and command string for WSL execution
        String wslCommand = String.format(
                "export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64; " +
                "export HADOOP_HOME=/home/priyasri/hadoop-3.3.6; " +
                "cd %s && /home/priyasri/pig-0.17.0/bin/pig -x mapreduce scripts/pig/%s",
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
                    System.out.println("  [Pig] " + line);
                }
            }

            exitCode = process.waitFor();
            long duration = (System.currentTimeMillis() - startTime) / 1000;

            if (exitCode == 0) {
                System.out.printf("[PigScriptExecutor] \u2714 SUCCESS: Script '%s' completed successfully in %d seconds.\n",
                        scriptName, duration);
            } else {
                System.err.printf("[PigScriptExecutor] \u2718 FAILED: Script '%s' exited with code %d after %d seconds.\n",
                        scriptName, exitCode, duration);
            }
        } catch (Exception e) {
            System.err.println("[PigScriptExecutor] Error executing script " + scriptName + ": " + e.getMessage());
            e.printStackTrace();
        }

        return exitCode;
    }

    /**
     * Executes all generated Pig Latin scripts and prints an overall execution summary.
     *
     * @return true if all scripts completed with exit code 0
     */
    public boolean executeAllScripts() {
        System.out.println("\n[PigScriptExecutor] Starting batch execution of all Pig Latin scripts...");

        int exit1 = executePigScript("world_laws_analysis.pig");
        int exit2 = executePigScript("eeoc_industry_analysis.pig");

        boolean allPassed = (exit1 == 0 && exit2 == 0);

        System.out.println("\n================================================================================");
        System.out.println("                       PIG EXECUTION BATCH SUMMARY                              ");
        System.out.println("================================================================================");
        System.out.printf("  1. world_laws_analysis.pig    : %s\n", (exit1 == 0 ? "\u2714 SUCCESS" : "\u2718 FAILED (" + exit1 + ")"));
        System.out.printf("  2. eeoc_industry_analysis.pig : %s\n", (exit2 == 0 ? "\u2714 SUCCESS" : "\u2718 FAILED (" + exit2 + ")"));
        System.out.println("================================================================================");

        return allPassed;
    }

    private boolean isFilteredNoise(String line) {
        if (line == null || line.trim().isEmpty()) {
            return true;
        }
        String lower = line.toLowerCase();
        return lower.startsWith("slf4j: class path contains multiple")
                || lower.startsWith("slf4j: see http://www.slf4j.org")
                || lower.startsWith("slf4j: defaulting to no-operation")
                || lower.contains("slf4j-log4j12");
    }
}