package com.harassment.sqoop;

import com.harassment.hdfs.WSLIPResolver;
import com.harassment.utils.ConfigLoader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Verifies exported data stored in MySQL tables by displaying row counts and sample records.
 */
public class MySQLResultVerifier {

    private final String wslIp;
    private final String dbUser;
    private final String dbPassword;

    public MySQLResultVerifier() {
        this.wslIp = WSLIPResolver.getWSLIP();
        this.dbUser = ConfigLoader.getProperty("mysql.user", "root");
        this.dbPassword = ConfigLoader.getProperty("mysql.password", "root");
    }

    public MySQLResultVerifier(String wslIp, String dbUser, String dbPassword) {
        this.wslIp = wslIp;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    private Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            // Rely on SPI
        }
        String url = "jdbc:mysql://" + wslIp + ":3306/harassment_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        return DriverManager.getConnection(url, dbUser, dbPassword);
    }

    public void verifyAll() {
        System.out.println("\n=======================================================");
        System.out.println("      MySQL Exported Results Verification");
        System.out.println("=======================================================");

        String[] tables = {
                "industry_risk",
                "sex_distribution",
                "race_distribution",
                "yearly_trend",
                "region_protection"
        };

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            for (String table : tables) {
                int count = 0;
                try (ResultSet countRs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
                    if (countRs.next()) {
                        count = countRs.getInt(1);
                    }
                }

                System.out.println("\n-------------------------------------------------------");
                System.out.println(" Table: " + table + " (Total Rows: " + count + ")");
                System.out.println(" Sample Data (LIMIT 5):");
                System.out.println("-------------------------------------------------------");

                try (ResultSet rs = stmt.executeQuery("SELECT * FROM " + table + " LIMIT 5")) {
                    printResultSet(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("[MySQLResultVerifier] ✖ Error during MySQL verification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void printResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();

        StringBuilder header = new StringBuilder();
        StringBuilder divider = new StringBuilder();

        for (int i = 1; i <= colCount; i++) {
            String colName = meta.getColumnLabel(i);
            header.append(String.format("%-25s | ", colName));
            divider.append("--------------------------+--");
        }

        System.out.println(header);
        System.out.println(divider);

        boolean hasRows = false;
        while (rs.next()) {
            hasRows = true;
            StringBuilder row = new StringBuilder();
            for (int i = 1; i <= colCount; i++) {
                String val = rs.getString(i);
                if (val == null) val = "NULL";
                row.append(String.format("%-25s | ", val));
            }
            System.out.println(row);
        }

        if (!hasRows) {
            System.out.println("  (No rows found)");
        }
    }
}
