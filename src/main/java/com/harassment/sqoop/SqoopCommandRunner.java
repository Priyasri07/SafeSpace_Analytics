package com.harassment.sqoop;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Executes Apache Sqoop commands inside WSL from Windows.
 */
public class SqoopCommandRunner {

    /**
     * Executes Sqoop with the given arguments, filtering noisy logs.
     *
     * @param args Sqoop arguments
     * @return Exit code of the command
     */
    public static int execute(List<String> args) {
        return runCommand(args, false);
    }

    /**
     * Executes Sqoop with the given arguments in quiet mode (only prints errors).
     *
     * @param args Sqoop arguments
     * @return Exit code of the command
     */
    public static int executeQuiet(List<String> args) {
        return runCommand(args, true);
    }

    private static int runCommand(List<String> args, boolean quiet) {
        StringBuilder argStr = new StringBuilder();
        for (String arg : args) {
            if (arg == null || arg.isEmpty()) continue;
            if (arg.contains(" ") && !arg.startsWith("\"") && !arg.startsWith("'")) {
                argStr.append(" \"").append(arg).append("\"");
            } else {
                argStr.append(" ").append(arg);
            }
        }

        String fullBashCmd = "export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64; " +
                "export HADOOP_HOME=/home/priyasri/hadoop-3.3.6; " +
                "export SQOOP_HOME=/home/priyasri/sqoop-1.4.7; " +
                "cd /mnt/c/Users/manis/Workplace-Harassment-Analytics && " +
                "/home/priyasri/sqoop-1.4.7/bin/sqoop" + argStr.toString();

        List<String> processCmd = new ArrayList<>();
        processCmd.add("wsl");
        processCmd.add("-d");
        processCmd.add("Ubuntu");
        processCmd.add("bash");
        processCmd.add("-c");
        processCmd.add(fullBashCmd);

        try {
            ProcessBuilder pb = new ProcessBuilder(processCmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (isFiltered(line)) {
                        continue;
                    }

                    if (quiet) {
                        if (isErrorLine(line)) {
                            System.err.println(line);
                        }
                    } else {
                        System.out.println(line);
                    }
                }
            }

            int exitCode = process.waitFor();
            return exitCode;
        } catch (Exception e) {
            System.err.println("[SqoopCommandRunner] Execution error: " + e.getMessage());
            return 1;
        }
    }

    private static boolean isFiltered(String line) {
        return line.contains("Warning:") ||
                line.contains("INFO") ||
                line.contains("WARN") ||
                line.contains("SLF4J") ||
                line.contains("Loading class");
    }

    private static boolean isErrorLine(String line) {
        String lower = line.toLowerCase();
        return lower.contains("error") || lower.contains("exception") || lower.contains("failed") || lower.contains("fatal");
    }
}
