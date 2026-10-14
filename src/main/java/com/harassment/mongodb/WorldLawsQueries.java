package com.harassment.mongodb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes 6 analytical queries on the world_laws MongoDB collection,
 * prints tabular outputs, and exports all results to outputs/mongodb_query_results.json.
 */
public class WorldLawsQueries {

    private final MongoCollection<Document> collection;
    private final ObjectMapper objectMapper;

    public WorldLawsQueries(MongoCollection<Document> collection) {
        this.collection = collection;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Executes all 6 analytical queries and saves results to JSON.
     *
     * @param outputPath Path to output JSON file (e.g., outputs/mongodb_query_results.json)
     * @return Map containing all query results
     */
    public Map<String, Object> runAllQueries(String outputPath) throws IOException {
        Map<String, Object> allResults = new LinkedHashMap<>();

        System.out.println("\n================================================================================");
        System.out.println("                 MONGODB ANALYTICAL QUERIES ON WORLD LAWS                       ");
        System.out.println("================================================================================");

        // Q1: Countries where sexual_harassment_prohibited = false
        List<Map<String, Object>> q1 = runQ1ProhibitedFalse();
        allResults.put("Q1_prohibited_false", q1);

        // Q2: Top 10 countries by protection_score (descending)
        List<Map<String, Object>> q2 = runQ2Top10ByScore();
        allResults.put("Q2_top10_by_score", q2);

        // Q3: Countries with retaliation_protection = false
        List<Map<String, Object>> q3 = runQ3RetaliationFalse();
        allResults.put("Q3_retaliation_false", q3);

        // Q4: Count of countries by region (sorted descending)
        List<Map<String, Object>> q4 = runQ4CountByRegion();
        allResults.put("Q4_count_by_region", q4);

        // Q5: Average protection_score by region (aggregation pipeline)
        List<Map<String, Object>> q5 = runQ5AvgScoreByRegion();
        allResults.put("Q5_avg_score_by_region", q5);

        // Q6: Countries where ALL 6 legal indicators are true
        List<Map<String, Object>> q6 = runQ6AllIndicatorsTrue();
        allResults.put("Q6_all_indicators_true", q6);

        // Save to JSON
        saveResultsToJson(outputPath, allResults);

        return allResults;
    }

    /**
     * Q1: Countries where sexual_harassment_prohibited = false
     */
    public List<Map<String, Object>> runQ1ProhibitedFalse() {
        System.out.println("\n[Q1] Countries where sexual_harassment_prohibited = false");
        System.out.println("------------------------------------------------------------------");
        System.out.printf("%-45s | %-12s\n", "Country", "Country Code");
        System.out.println("------------------------------------------------------------------");

        List<Map<String, Object>> results = new ArrayList<>();
        Bson filter = Filters.eq("sexual_harassment_prohibited", false);

        for (Document doc : collection.find(filter).sort(Sorts.ascending("country"))) {
            String country = doc.getString("country");
            String code = doc.getString("country_code");

            System.out.printf("%-45s | %-12s\n", country, code);

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("country", country);
            map.put("country_code", code);
            results.add(map);
        }

        System.out.println("------------------------------------------------------------------");
        System.out.println("Total Countries: " + results.size());
        return results;
    }

    /**
     * Q2: Top 10 countries by protection_score (descending)
     */
    public List<Map<String, Object>> runQ2Top10ByScore() {
        System.out.println("\n[Q2] Top 10 countries by protection_score (descending)");
        System.out.println("------------------------------------------------------------------");
        System.out.printf("%-6s | %-42s | %-12s\n", "Rank", "Country", "Score");
        System.out.println("------------------------------------------------------------------");

        List<Map<String, Object>> results = new ArrayList<>();
        int rank = 1;

        for (Document doc : collection.find().sort(Sorts.descending("protection_score")).limit(10)) {
            String country = doc.getString("country");
            Number scoreNum = (Number) doc.get("protection_score");
            double score = scoreNum != null ? scoreNum.doubleValue() : 0.0;

            System.out.printf("%-6d | %-42s | %-12.2f\n", rank, country, score);

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("rank", rank);
            map.put("country", country);
            map.put("protection_score", score);
            results.add(map);
            rank++;
        }

        System.out.println("------------------------------------------------------------------");
        return results;
    }

    /**
     * Q3: Countries with retaliation_protection = false
     */
    public List<Map<String, Object>> runQ3RetaliationFalse() {
        System.out.println("\n[Q3] Countries with retaliation_protection = false");
        System.out.println("------------------------------------------------------------------");
        System.out.printf("%-45s | %-20s\n", "Country", "Region");
        System.out.println("------------------------------------------------------------------");

        List<Map<String, Object>> results = new ArrayList<>();
        Bson filter = Filters.eq("retaliation_protection", false);

        for (Document doc : collection.find(filter).sort(Sorts.ascending("country"))) {
            String country = doc.getString("country");
            String region = doc.getString("region");

            System.out.printf("%-45s | %-20s\n", country, region);

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("country", country);
            map.put("region", region);
            results.add(map);
        }

        System.out.println("------------------------------------------------------------------");
        System.out.println("Total Countries without Retaliation Protection: " + results.size());
        return results;
    }

    /**
     * Q4: Count of countries by region (sorted descending)
     */
    public List<Map<String, Object>> runQ4CountByRegion() {
        System.out.println("\n[Q4] Count of countries by region (sorted descending)");
        System.out.println("------------------------------------------------------------------");
        System.out.printf("%-35s | %-15s\n", "Region", "Country Count");
        System.out.println("------------------------------------------------------------------");

        List<Map<String, Object>> results = new ArrayList<>();
        List<Bson> pipeline = Arrays.asList(
                Aggregates.group("$region", Accumulators.sum("count", 1)),
                Aggregates.sort(Sorts.descending("count"))
        );

        for (Document doc : collection.aggregate(pipeline)) {
            String region = doc.getString("_id");
            int count = doc.getInteger("count", 0);

            System.out.printf("%-35s | %-15d\n", region, count);

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("region", region);
            map.put("count", count);
            results.add(map);
        }

        System.out.println("------------------------------------------------------------------");
        return results;
    }

    /**
     * Q5: Average protection_score by region (aggregation pipeline)
     */
    public List<Map<String, Object>> runQ5AvgScoreByRegion() {
        System.out.println("\n[Q5] Average protection_score by region (aggregation pipeline)");
        System.out.println("------------------------------------------------------------------");
        System.out.printf("%-35s | %-15s\n", "Region", "Average Score");
        System.out.println("------------------------------------------------------------------");

        List<Map<String, Object>> results = new ArrayList<>();
        List<Bson> pipeline = Arrays.asList(
                Aggregates.group("$region", Accumulators.avg("avg_score", "$protection_score")),
                Aggregates.sort(Sorts.descending("avg_score"))
        );

        for (Document doc : collection.aggregate(pipeline)) {
            String region = doc.getString("_id");
            Number avgNum = (Number) doc.get("avg_score");
            double avgScore = avgNum != null ? avgNum.doubleValue() : 0.0;
            double roundedAvg = Math.round(avgScore * 100.0) / 100.0;

            System.out.printf("%-35s | %-15.2f\n", region, roundedAvg);

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("region", region);
            map.put("avg_score", roundedAvg);
            results.add(map);
        }

        System.out.println("------------------------------------------------------------------");
        return results;
    }

    /**
     * Q6: Countries where ALL 6 legal indicators are true
     */
    public List<Map<String, Object>> runQ6AllIndicatorsTrue() {
        System.out.println("\n[Q6] Countries where ALL 6 legal indicators are true");
        System.out.println("------------------------------------------------------------------");
        System.out.printf("%-35s | %-12s | %-15s | %-8s\n", "Country", "Code", "Region", "Score");
        System.out.println("------------------------------------------------------------------");

        List<Map<String, Object>> results = new ArrayList<>();
        Bson filter = Filters.and(
                Filters.eq("sexual_harassment_prohibited", true),
                Filters.eq("covers_all_workers", true),
                Filters.eq("covers_all_perpetrators", true),
                Filters.eq("employer_prevention_responsibility", true),
                Filters.eq("employer_liability", true),
                Filters.eq("retaliation_protection", true)
        );

        for (Document doc : collection.find(filter).sort(Sorts.descending("protection_score"))) {
            String country = doc.getString("country");
            String code = doc.getString("country_code");
            String region = doc.getString("region");
            Number scoreNum = (Number) doc.get("protection_score");
            double score = scoreNum != null ? scoreNum.doubleValue() : 0.0;

            System.out.printf("%-35s | %-12s | %-15s | %-8.2f\n", country, code, region, score);

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("country", country);
            map.put("country_code", code);
            map.put("region", region);
            map.put("protection_score", score);
            results.add(map);
        }

        System.out.println("------------------------------------------------------------------");
        System.out.println("Total Countries with Full Legal Protections: " + results.size());
        return results;
    }

    private void saveResultsToJson(String outputPath, Map<String, Object> results) throws IOException {
        File file = new File(outputPath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        objectMapper.writeValue(file, results);
        System.out.println("\n[WorldLawsQueries] \u2714 Successfully exported all query results to: " + file.getAbsolutePath());
    }
}