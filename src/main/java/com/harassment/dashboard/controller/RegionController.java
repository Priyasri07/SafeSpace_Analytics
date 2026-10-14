package com.harassment.dashboard.controller;

import com.harassment.dashboard.util.DatabaseHelper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller serving regional protection scores from MySQL.
 */
@RestController
@RequestMapping("/api/regions")
public class RegionController {

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getRegions() {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT region, avg_protection_score, country_count FROM region_protection ORDER BY avg_protection_score ASC";

        try (Connection conn = DatabaseHelper.getMySQLConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("region", rs.getString("region"));
                item.put("avg_protection_score", rs.getDouble("avg_protection_score"));
                item.put("country_count", rs.getInt("country_count"));
                result.add(item);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("[RegionController] Error querying region_protection: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
}
