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
 * REST controller serving industry risk distribution data from MySQL.
 */
@RestController
@RequestMapping("/api/industries")
public class IndustryController {

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getIndustries() {
        List<Map<String, Object>> result = new ArrayList<>();
        String sql = "SELECT naics_description, charge_count FROM industry_risk ORDER BY charge_count DESC";

        try (Connection conn = DatabaseHelper.getMySQLConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("industry", rs.getString("naics_description"));
                item.put("count", rs.getInt("charge_count"));
                result.add(item);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("[IndustryController] Error querying industry_risk: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
}
