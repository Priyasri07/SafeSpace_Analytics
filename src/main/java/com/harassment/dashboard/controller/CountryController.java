package com.harassment.dashboard.controller;

import com.harassment.dashboard.util.DatabaseHelper;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller serving country protection law data from MongoDB.
 */
@RestController
@RequestMapping("/api/countries")
public class CountryController {

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getCountries() {
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            MongoCollection<Document> collection = DatabaseHelper.getMongoDBCollection();
            FindIterable<Document> docs = collection.find().sort(new Document("country", 1));

            for (Document doc : docs) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("country", doc.getString("country"));
                item.put("region", doc.getString("region"));
                Object scoreObj = doc.get("protection_score");
                double score = 0.0;
                if (scoreObj instanceof Number) {
                    score = ((Number) scoreObj).doubleValue();
                }
                item.put("score", score);
                result.add(item);
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("[CountryController] Error querying MongoDB world_laws: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
}
