package com.harassment.sqoop;

import com.harassment.hdfs.WSLIPResolver;
import com.harassment.utils.ConfigLoader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages MySQL database and table lifecycle for Sqoop import/export operations.
 */
public class MySQLSchemaManager {

    private final String wslIp;
    private final String dbUser;
    private final String dbPassword;

    public MySQLSchemaManager() {
        this.wslIp = WSLIPResolver.getWSLIP();
        this.dbUser = ConfigLoader.getProperty("mysql.user", "root");
        this.dbPassword = ConfigLoader.getProperty("mysql.password", "root");
    }

    public MySQLSchemaManager(String wslIp, String dbUser, String dbPassword) {
        this.wslIp = wslIp;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    /**
     * Gets a JDBC connection to MySQL server or specific database.
     */
    public Connection getConnection(boolean includeDb) throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("com.mysql.jdbc.Driver");
            } catch (ClassNotFoundException ex) {
                System.err.println("[MySQLSchemaManager] Warning: MySQL JDBC driver class not explicitly found, relying on SPI.");
            }
        }

        String dbPath = includeDb ? "harassment_db" : "";
        String url = "jdbc:mysql://" + wslIp + ":3306/" + dbPath + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        return DriverManager.getConnection(url, dbUser, dbPassword);
    }

    /**
     * Creates database harassment_db and recreates all required tables.
     */
    public boolean createDatabaseAndTables() {
        System.out.println("\n[MySQLSchemaManager] Initializing MySQL Database & Schema on " + wslIp + ":3306...");

        try (Connection conn = getConnection(false);
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS harassment_db");
            System.out.println("  ✔ Database 'harassment_db' verified/created.");

            stmt.execute("USE harassment_db");

            // Drop existing tables
            String[] tablesToDrop = {
                    "industry_risk",
                    "sex_distribution",
                    "race_distribution",
                    "yearly_trend",
                    "region_protection",
                    "company_metadata"
            };

            for (String table : tablesToDrop) {
                stmt.executeUpdate("DROP TABLE IF EXISTS " + table);
            }
            System.out.println("  ✔ Cleaned previous table definitions.");

            // 1. industry_risk
            stmt.executeUpdate(
                    "CREATE TABLE industry_risk (" +
                    "    id INT AUTO_INCREMENT PRIMARY KEY," +
                    "    naics_description VARCHAR(255)," +
                    "    charge_count INT" +
                    ")"
            );
            System.out.println("  ✔ Created table: industry_risk");

            // 2. sex_distribution
            stmt.executeUpdate(
                    "CREATE TABLE sex_distribution (" +
                    "    id INT AUTO_INCREMENT PRIMARY KEY," +
                    "    cp_sex VARCHAR(50)," +
                    "    charge_count INT" +
                    ")"
            );
            System.out.println("  ✔ Created table: sex_distribution");

            // 3. race_distribution
            stmt.executeUpdate(
                    "CREATE TABLE race_distribution (" +
                    "    id INT AUTO_INCREMENT PRIMARY KEY," +
                    "    cp_race VARCHAR(50)," +
                    "    charge_count INT" +
                    ")"
            );
            System.out.println("  ✔ Created table: race_distribution");

            // 4. yearly_trend
            stmt.executeUpdate(
                    "CREATE TABLE yearly_trend (" +
                    "    year INT PRIMARY KEY," +
                    "    charge_count INT" +
                    ")"
            );
            System.out.println("  ✔ Created table: yearly_trend");

            // 5. region_protection
            stmt.executeUpdate(
                    "CREATE TABLE region_protection (" +
                    "    region VARCHAR(50) PRIMARY KEY," +
                    "    avg_protection_score FLOAT," +
                    "    country_count INT" +
                    ")"
            );
            System.out.println("  ✔ Created table: region_protection");

            // 6. company_metadata
            stmt.executeUpdate(
                    "CREATE TABLE company_metadata (" +
                    "    company_id INT AUTO_INCREMENT PRIMARY KEY," +
                    "    company_name VARCHAR(255)," +
                    "    industry VARCHAR(255)," +
                    "    num_employees INT," +
                    "    country VARCHAR(100)" +
                    ")"
            );
            System.out.println("  ✔ Created table: company_metadata");

            System.out.println("MySQL schema ready: 6 tables created");
            return true;

        } catch (SQLException e) {
            System.err.println("[MySQLSchemaManager] ✖ Error setting up MySQL schema: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Inserts 20 realistic sample companies into company_metadata for Sqoop import.
     */
    public boolean insertSampleCompanyMetadata() {
        System.out.println("\n[MySQLSchemaManager] Inserting 20 sample company metadata records...");

        String insertSql = "INSERT INTO company_metadata (company_name, industry, num_employees, country) VALUES (?, ?, ?, ?)";

        Object[][] sampleCompanies = {
                {"Apex Global Technologies", "Professional Scientific and Technical Services", 15400, "United States"},
                {"Horizon Health Systems", "Health Care and Social Assistance", 8200, "United Kingdom"},
                {"Beacon Hospitality Group", "Accommodation and Food Services", 12500, "France"},
                {"Summit Construction Partners", "Construction", 4300, "Germany"},
                {"Crestview Retail Stores", "Retail Trade", 24000, "Canada"},
                {"Vanguard Manufacturing", "Manufacturing", 9600, "Japan"},
                {"Pinnacle Logistics Corp", "Wholesale Trade", 6100, "Australia"},
                {"Starlight Educational Academy", "Educational Services", 3200, "India"},
                {"Metro Public Transit Authority", "Public Administration", 18500, "United States"},
                {"Silverline Business Services", "Administrative and Support Services", 5400, "Singapore"},
                {"Nova Care Medical Center", "Health Care and Social Assistance", 7100, "Netherlands"},
                {"Prime Hospitality Suites", "Accommodation and Food Services", 8900, "Spain"},
                {"BuildCraft Engineering", "Construction", 3800, "Italy"},
                {"OmniStore Supercenters", "Retail Trade", 35000, "United States"},
                {"Precision Steel Works", "Manufacturing", 11200, "South Korea"},
                {"Global Freight Solutions", "Wholesale Trade", 4700, "Germany"},
                {"Evergreen University Network", "Educational Services", 6500, "United Kingdom"},
                {"Civic Services Division", "Public Administration", 14300, "Canada"},
                {"Integra IT Consulting", "Professional Scientific and Technical Services", 9200, "Sweden"},
                {"FirstClass Facilities Management", "Administrative and Support Services", 6800, "Australia"}
        };

        try (Connection conn = getConnection(true);
             PreparedStatement pstmt = conn.prepareStatement(insertSql)) {

            conn.setAutoCommit(false);

            for (Object[] company : sampleCompanies) {
                pstmt.setString(1, (String) company[0]);
                pstmt.setString(2, (String) company[1]);
                pstmt.setInt(3, (Integer) company[2]);
                pstmt.setString(4, (String) company[3]);
                pstmt.addBatch();
            }

            int[] results = pstmt.executeBatch();
            conn.commit();

            System.out.println("  ✔ Inserted " + results.length + " company metadata records into company_metadata.");
            return true;

        } catch (SQLException e) {
            System.err.println("[MySQLSchemaManager] ✖ Error inserting sample company metadata: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verifies all 6 tables by counting rows.
     */
    public void verifyTables() {
        System.out.println("\n[MySQLSchemaManager] Verifying MySQL Table Counts:");
        System.out.println("--------------------------------------------------");
        System.out.printf("%-25s | %-10s\n", "Table Name", "Row Count");
        System.out.println("--------------------------------------------------");

        String[] tables = {
                "industry_risk",
                "sex_distribution",
                "race_distribution",
                "yearly_trend",
                "region_protection",
                "company_metadata"
        };

        try (Connection conn = getConnection(true);
             Statement stmt = conn.createStatement()) {

            for (String table : tables) {
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    if (rs.next()) {
                        int count = rs.getInt(1);
                        System.out.printf("%-25s | %-10d\n", table, count);
                    }
                }
            }
            System.out.println("--------------------------------------------------");

        } catch (SQLException e) {
            System.err.println("[MySQLSchemaManager] ✖ Error verifying tables: " + e.getMessage());
        }
    }
}
