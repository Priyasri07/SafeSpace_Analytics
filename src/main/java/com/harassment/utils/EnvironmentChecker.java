package com.harassment.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Verifies both the host Windows and the guest WSL Ubuntu Big Data environments.
 */
public class EnvironmentChecker {

    public static class CommandResult {
        public final int exitCode;
        public final String output;

        public CommandResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output == null ? "" : output.trim();
        }

        public boolean isSuccess() {
            return exitCode == 0;
        }
    }

    public static void checkEnvironment() {
        System.out.println("=====================================================================");
        System.out.println("        BIG DATA ENVIRONMENT VERIFICATION (Windows + WSL)");
        System.out.println("=====================================================================");
        System.out.println();

        // PART A: Check Windows Side
        System.out.println("--- WINDOWS SIDE ---");
        String winJava = getWindowsJava();
        String winMaven = getWindowsMaven();
        System.out.printf("  %-10s : %s%n", "Java", winJava);
        System.out.printf("  %-10s : %s%n", "Maven", winMaven);
        System.out.println();

        // PART B: Check WSL Ubuntu Side (Batch Execution for fast response)
        Map<String, String> wslResults = runWslProbe();

        System.out.println("--- WSL UBUNTU SIDE ---");
        String wslUbuntu = parseWslUbuntu(wslResults.get("UBUNTU"));
        String wslJava = parseWslJava(wslResults.get("JAVA"));
        String wslHadoop = parseWslHadoop(wslResults.get("HADOOP"));
        String wslHdfs = parseWslHdfs(wslResults.get("JPS"));
        String wslHive = parseWslHive(wslResults.get("HIVE"));
        String wslPig = parseWslPig(wslResults.get("PIG"));
        String wslSqoop = parseWslSqoop(wslResults.get("SQOOP"));
        String wslMongo = parseWslMongo(wslResults.get("MONGOD"));
        String wslMysql = parseWslMysql(wslResults.get("MYSQL"));

        System.out.printf("  %-10s : %s%n", "Ubuntu", wslUbuntu);
        System.out.printf("  %-10s : %s%n", "Java", wslJava);
        System.out.printf("  %-10s : %s%n", "Hadoop", wslHadoop);
        System.out.printf("  %-10s : %s%n", "HDFS", wslHdfs);
        System.out.printf("  %-10s : %s%n", "Hive", wslHive);
        System.out.printf("  %-10s : %s%n", "Pig", wslPig);
        System.out.printf("  %-10s : %s%n", "Sqoop", wslSqoop);
        System.out.printf("  %-10s : %s%n", "MongoDB", wslMongo);
        System.out.printf("  %-10s : %s%n", "MySQL", wslMysql);
        System.out.println();

        // Connectivity
        System.out.println("--- CONNECTIVITY ---");
        String wslIp = parseWslIp(wslResults.get("HOSTNAME"));
        System.out.printf("  WSL IP Address : %s%n", wslIp);
        System.out.println("  HDFS URL (WSL)      : hdfs://localhost:9000");
        System.out.printf("  HDFS URL (Windows)  : hdfs://%s:9000%n", wslIp);
        System.out.println();

        // Summary Calculation (10 tracked components: Win Java, Win Maven, Ubuntu, WSL Java, Hadoop, Hive, Pig, Sqoop, MongoDB, MySQL)
        List<String> missing = new ArrayList<>();
        int installedCount = 0;

        if (winJava.startsWith("FOUND")) installedCount++;
        if (winMaven.startsWith("FOUND")) installedCount++;
        if (wslUbuntu.startsWith("FOUND")) installedCount++;
        if (wslJava.startsWith("FOUND")) installedCount++;
        if (wslHadoop.startsWith("FOUND")) installedCount++;
        if (wslHive.startsWith("FOUND")) installedCount++;

        if (wslPig.startsWith("FOUND")) {
            installedCount++;
        } else {
            missing.add("pig");
        }

        if (wslSqoop.startsWith("FOUND")) {
            installedCount++;
        } else {
            missing.add("sqoop");
        }

        if (wslMongo.startsWith("FOUND")) {
            installedCount++;
        } else {
            missing.add("mongodb");
        }

        if (wslMysql.startsWith("FOUND")) {
            installedCount++;
        } else {
            missing.add("mysql");
        }

        System.out.println("--- SUMMARY ---");
        System.out.printf("  Tools installed: %d / 10%n", installedCount);
        if (missing.isEmpty()) {
            System.out.println("  All required Big Data tools are installed!");
        } else {
            System.out.printf("  Missing: %s%n", String.join(", ", missing));
        }
        System.out.println();
        System.out.println("=====================================================================");

        // Generate / update report/setup_guide.md
        generateSetupGuide(missing);
    }

    private static String getWindowsJava() {
        CommandResult res = runWindowsCommand("java -version");
        for (String line : res.output.split("\n")) {
            line = line.trim();
            if (line.contains("version \"")) {
                return "FOUND     | " + line;
            }
        }
        return res.isSuccess() ? "FOUND     | " + res.output.split("\n")[0].trim() : "NOT FOUND | (command not found)";
    }

    private static String getWindowsMaven() {
        CommandResult res = runWindowsCommand("mvn -version");
        if (!res.output.contains("Apache Maven")) {
            // Fallback check standard installation folder
            File mvnCmd = new File("C:\\Users\\manis\\apache-maven-3.9.16\\bin\\mvn.cmd");
            if (mvnCmd.exists()) {
                res = runWindowsCommand("\"" + mvnCmd.getAbsolutePath() + "\" -version");
            }
        }
        for (String line : res.output.split("\n")) {
            line = line.trim();
            if (line.startsWith("Apache Maven")) {
                return "FOUND     | " + line;
            }
        }
        return "NOT FOUND | (command not found)";
    }

    private static Map<String, String> runWslProbe() {
        Map<String, String> map = new HashMap<>();
        String batchCmd =
                "echo '===UBUNTU==='; lsb_release -a 2>&1; " +
                "echo '===JAVA==='; java -version 2>&1; " +
                "echo '===HADOOP==='; hadoop version 2>&1; " +
                "echo '===JPS==='; jps 2>&1; " +
                "echo '===HDFS==='; hdfs dfs -ls / 2>&1; " +
                "echo '===HIVE==='; hive --version 2>&1; " +
                "echo '===PIG==='; pig --version 2>&1; " +
                "echo '===SQOOP==='; sqoop version 2>&1; " +
                "echo '===MONGOD==='; mongod --version 2>&1; " +
                "echo '===MYSQL==='; mysql --version 2>&1; " +
                "echo '===HOSTNAME==='; hostname -I 2>&1; " +
                "echo '===WHICH==='; which hdfs hive pig sqoop 2>&1";

        CommandResult res = runWslCommand(batchCmd);
        String currentKey = null;
        StringBuilder currentBuffer = new StringBuilder();

        for (String line : res.output.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("===") && trimmed.endsWith("===")) {
                if (currentKey != null) {
                    map.put(currentKey, currentBuffer.toString().trim());
                }
                currentKey = trimmed.replace("===", "");
                currentBuffer = new StringBuilder();
            } else {
                currentBuffer.append(line).append("\n");
            }
        }
        if (currentKey != null) {
            map.put(currentKey, currentBuffer.toString().trim());
        }

        return map;
    }

    private static String parseWslUbuntu(String out) {
        if (out == null) return "NOT FOUND | WSL not available";
        for (String line : out.split("\n")) {
            if (line.startsWith("Description:")) {
                return "FOUND     | " + line.substring("Description:".length()).trim();
            }
        }
        return "FOUND     | Ubuntu Linux";
    }

    private static String parseWslJava(String out) {
        if (out == null) return "NOT FOUND | (command not found)";
        for (String line : out.split("\n")) {
            line = line.trim();
            if (line.contains("version \"")) {
                return "FOUND     | " + line;
            }
        }
        return "NOT FOUND | (command not found)";
    }

    private static String parseWslHadoop(String out) {
        if (out == null) return "NOT FOUND | (command not found)";
        for (String line : out.split("\n")) {
            line = line.trim();
            if (line.startsWith("Hadoop ")) {
                return "FOUND     | " + line;
            }
        }
        return "NOT FOUND | (command not found)";
    }

    private static String parseWslHdfs(String jpsOut) {
        if (jpsOut == null) return "STOPPED   | HDFS daemons not running";
        boolean hasNameNode = jpsOut.contains("NameNode");
        boolean hasDataNode = jpsOut.contains("DataNode");
        boolean hasSecondary = jpsOut.contains("SecondaryNameNode");

        if (hasNameNode && hasDataNode && hasSecondary) {
            return "RUNNING   | NameNode + DataNode + SecondaryNameNode active";
        } else if (hasNameNode || hasDataNode) {
            return "PARTIAL   | NameNode or DataNode active";
        }
        return "STOPPED   | HDFS daemons not running";
    }

    private static String parseWslHive(String out) {
        if (out == null) return "NOT FOUND | (command not found)";
        for (String line : out.split("\n")) {
            line = line.trim();
            if (line.startsWith("Hive ")) {
                return "FOUND     | " + line;
            }
        }
        return "NOT FOUND | (command not found)";
    }

    private static String parseWslPig(String out) {
        if (out == null || out.toLowerCase().contains("not found") || out.contains("Command 'pig'")) {
            return "NOT FOUND | (command not found)";
        }
        for (String line : out.split("\n")) {
            line = line.trim();
            if (line.toLowerCase().contains("pig version")) {
                return "FOUND     | " + line;
            }
        }
        return "NOT FOUND | (command not found)";
    }

    private static String parseWslSqoop(String out) {
        if (out == null || out.toLowerCase().contains("not found") || out.contains("Command 'sqoop'")) {
            return "NOT FOUND | (command not found)";
        }
        for (String line : out.split("\n")) {
            line = line.trim();
            if (line.toLowerCase().contains("sqoop")) {
                return "FOUND     | " + line;
            }
        }
        return "NOT FOUND | (command not found)";
    }

    private static String parseWslMongo(String out) {
        if (out == null || out.toLowerCase().contains("not found")) {
            return "NOT FOUND | (command not found)";
        }
        for (String line : out.split("\n")) {
            line = line.trim();
            if (line.startsWith("db version v") || line.contains("MongoDB server version")) {
                return "FOUND     | " + line;
            }
        }
        return "NOT FOUND | (command not found)";
    }

    private static String parseWslMysql(String out) {
        if (out == null || out.toLowerCase().contains("not found") || out.contains("Command 'mysql'")) {
            return "NOT FOUND | (command not found)";
        }
        for (String line : out.split("\n")) {
            line = line.trim();
            if (line.toLowerCase().contains("mysql  ver") || line.toLowerCase().contains("distribution")) {
                return "FOUND     | " + line;
            }
        }
        return "NOT FOUND | (command not found)";
    }

    private static String parseWslIp(String out) {
        if (out != null && !out.isEmpty()) {
            String[] tokens = out.trim().split("\\s+");
            if (tokens.length > 0 && !tokens[0].isEmpty()) {
                return tokens[0];
            }
        }
        return "127.0.0.1";
    }

    public static CommandResult runWindowsCommand(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", command);
            pb.redirectErrorStream(true);

            String path = pb.environment().get("PATH");
            if (path == null) path = "";
            String mavenBin = "C:\\Users\\manis\\apache-maven-3.9.16\\bin";
            if (!path.contains("apache-maven")) {
                pb.environment().put("PATH", mavenBin + ";" + path);
            }

            Process process = pb.start();

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            int exitCode = finished ? process.exitValue() : -1;
            return new CommandResult(exitCode, sb.toString());
        } catch (Exception e) {
            return new CommandResult(-1, e.getMessage());
        }
    }

    public static CommandResult runWslCommand(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder("wsl", "-d", "Ubuntu", "bash", "-ic", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            int exitCode = finished ? process.exitValue() : -1;
            return new CommandResult(exitCode, sb.toString());
        } catch (Exception e) {
            return new CommandResult(-1, e.getMessage());
        }
    }

    private static void generateSetupGuide(List<String> missing) {
        File reportDir = new File("report");
        if (!reportDir.exists()) {
            reportDir.mkdirs();
        }
        File guideFile = new File(reportDir, "setup_guide.md");

        StringBuilder sb = new StringBuilder();
        sb.append("# WSL Ubuntu Setup Guide\n\n");

        sb.append("## Install Apache Pig 0.17.0\n");
        sb.append("  cd ~\n");
        sb.append("  wget https://archive.apache.org/dist/pig/pig-0.17.0/pig-0.17.0.tar.gz\n");
        sb.append("  tar -xzf pig-0.17.0.tar.gz\n");
        sb.append("  echo 'export PIG_HOME=$HOME/pig-0.17.0' >> ~/.bashrc\n");
        sb.append("  echo 'export PATH=$PATH:$PIG_HOME/bin' >> ~/.bashrc\n");
        sb.append("  source ~/.bashrc\n");
        sb.append("  pig --version\n\n");

        sb.append("## Install Apache Sqoop 1.4.7\n");
        sb.append("  cd ~\n");
        sb.append("  wget https://archive.apache.org/dist/sqoop/1.4.7/sqoop-1.4.7.bin__hadoop-2.6.0.tar.gz\n");
        sb.append("  tar -xzf sqoop-1.4.7.bin__hadoop-2.6.0.tar.gz\n");
        sb.append("  echo 'export SQOOP_HOME=$HOME/sqoop-1.4.7.bin__hadoop-2.6.0' >> ~/.bashrc\n");
        sb.append("  echo 'export PATH=$PATH:$SQOOP_HOME/bin' >> ~/.bashrc\n");
        sb.append("  source ~/.bashrc\n");
        sb.append("  sqoop version\n\n");

        sb.append("## Install MongoDB 7.0\n");
        sb.append("  # Follow official MongoDB WSL guide:\n");
        sb.append("  # https://www.mongodb.com/docs/manual/tutorial/install-mongodb-on-ubuntu/\n");
        sb.append("  sudo apt-get install gnupg curl -y\n");
        sb.append("  curl -fsSL https://www.mongodb.org/static/pgp/server-7.0.asc | sudo gpg -o /usr/share/keyrings/mongodb-server-7.0.gpg --dearmor --yes\n");
        sb.append("  echo \"deb [ arch=amd64,arm64 signed-by=/usr/share/keyrings/mongodb-server-7.0.gpg ] https://repo.mongodb.org/apt/ubuntu jammy/mongodb-org/7.0 multiverse\" | sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list\n");
        sb.append("  sudo apt-get update\n");
        sb.append("  sudo apt-get install -y mongodb-org\n");
        sb.append("  sudo service mongod start\n");
        sb.append("  mongod --version\n\n");

        sb.append("## Install MySQL 8.0\n");
        sb.append("  sudo apt update\n");
        sb.append("  sudo apt install mysql-server -y\n");
        sb.append("  sudo service mysql start\n");
        sb.append("  mysql --version\n\n");

        sb.append("## Extract Hive 4.0.0 (already downloaded)\n");
        sb.append("  cd ~\n");
        sb.append("  tar -xzf apache-hive-4.0.0-bin.tar.gz\n");
        sb.append("  mv apache-hive-4.0.0-bin hive-4.0.0\n");
        sb.append("  echo 'export HIVE_HOME=$HOME/hive-4.0.0' >> ~/.bashrc\n");
        sb.append("  echo 'export PATH=$PATH:$HIVE_HOME/bin' >> ~/.bashrc\n");
        sb.append("  source ~/.bashrc\n");
        sb.append("  hive --version\n");

        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(guideFile), StandardCharsets.UTF_8)) {
            writer.write(sb.toString());
        } catch (IOException ignored) {
        }
    }
}