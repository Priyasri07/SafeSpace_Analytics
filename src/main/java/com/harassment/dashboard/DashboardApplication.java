package com.harassment.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot Application entry point for Workplace Harassment Analytics Dashboard.
 */
@SpringBootApplication
public class DashboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(DashboardApplication.class, args);
        System.out.println("========================================================================");
        System.out.println("  Workplace Harassment Analytics Dashboard Server Started!             ");
        System.out.println("  Access the REST API & Dashboard at: http://localhost:8080            ");
        System.out.println("========================================================================");
    }
}
