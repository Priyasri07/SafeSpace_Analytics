package com.harassment;

import com.harassment.hdfs.HDFSRunner;
import com.harassment.ingestion.DataIngestionRunner;
import com.harassment.mongodb.MongoDBRunner;
import com.harassment.hive.HiveRunner;
import com.harassment.sqoop.SqoopRunner;
import com.harassment.dashboard.DashboardApplication;
import com.harassment.pig.PigRunner;
import com.harassment.utils.EnvironmentChecker;

import java.util.Scanner;

/**
 * Main application entry point for Workplace Harassment Pattern Analytics.
 * Provides a menu-driven console interface for executing pipeline stages.
 */
public class Main {

    public static void main(String[] args) {
        // If a command-line argument is passed, execute the choice directly (useful for batch scripts)
        if (args != null && args.length > 0) {
            String choice = args[0].trim();
            executeChoice(choice);
            return;
        }

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            printMenu();
            System.out.print("Enter your choice: ");

            String input;
            try {
                if (!scanner.hasNextLine()) {
                    break;
                }
                input = scanner.nextLine().trim();
            } catch (Exception e) {
                break;
            }

            if (input.isEmpty()) {
                continue;
            }

            if ("9".equals(input)) {
                System.out.println("\nExiting Workplace Harassment Pattern Analytics. Goodbye!");
                running = false;
            } else {
                executeChoice(input);
                System.out.println();
            }
        }

        scanner.close();
    }

    private static void printMenu() {
        System.out.println("============================================");
        System.out.println("   Workplace Harassment Pattern Analytics   ");
        System.out.println("============================================");
        System.out.println("0. Check Environment (Windows + WSL)");
        System.out.println("1. Download Datasets (EEOC + WORLD)");
        System.out.println("2. Upload to HDFS");
        System.out.println("3. Load WORLD data into MongoDB");
        System.out.println("4. Run Pig Analysis");
        System.out.println("5. Run Hive Analysis");
        System.out.println("6. Sqoop Import/Export");
        System.out.println("7. Start Web Dashboard (Spring Boot)");
        System.out.println("8. Risk Scoring");
        System.out.println("9. Exit");
    }

    private static void executeChoice(String choice) {
        switch (choice) {
            case "0":
                EnvironmentChecker.checkEnvironment();
                break;
            case "1":
                System.out.println("\n[Executing Option 1: Download Datasets]");
                DataIngestionRunner.run();
                break;
            case "2":
                System.out.println("\n[Executing Option 2: Upload to HDFS]");
                HDFSRunner.run();
                break;
            case "3":
                System.out.println("\n[Executing Option 3: Load WORLD data into MongoDB]");
                MongoDBRunner.run();
                break;
            case "4":
                System.out.println("\n[Executing Option 4: Run Pig Analysis]");
                PigRunner.run();
                break;
            case "5":
                System.out.println("\n[Executing Option 5: Run Hive Analysis]");
                HiveRunner.run();
                break;
            case "6":
                System.out.println("\n[Executing Option 6: Sqoop Import/Export]");
                SqoopRunner.run();
                break;
            case "7":
                System.out.println("\n[Executing Option 7: Start Web Dashboard (Spring Boot)]");
                System.out.println("Starting Spring Boot Dashboard on http://localhost:8080");
                Thread dashboardThread = new Thread(() -> {
                    try {
                        DashboardApplication.main(new String[]{});
                    } catch (Exception e) {
                        System.err.println("Error running Spring Boot Dashboard: " + e.getMessage());
                    }
                }, "Dashboard-Server-Thread");
                dashboardThread.setDaemon(false);
                dashboardThread.start();
                break;
            case "8":
                System.out.println("\nComing soon...");
                break;
            case "9":
                System.out.println("\nExiting application. Goodbye!");
                break;
            default:
                System.out.println("\nInvalid choice. Please enter a number between 0 and 9.");
                break;
        }
    }
}