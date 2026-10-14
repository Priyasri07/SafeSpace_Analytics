package com.harassment.mongodb;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.bson.json.JsonWriterSettings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads world_laws.json dataset into MongoDB, creates performance indexes,
 * and verifies document integrity.
 */
public class WorldLawsLoader {

    private final ObjectMapper objectMapper;

    public WorldLawsLoader() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Loads the raw JSON dataset into the target MongoDB collection.
     * Drops existing collection for idempotency.
     *
     * @param jsonPath   Local path to data/raw/world_laws.json
     * @param collection MongoCollection to populate
     * @return Total count of documents inserted
     */
    public int loadJsonFile(String jsonPath, MongoCollection<Document> collection) throws IOException {
        File jsonFile = new File(jsonPath);
        if (!jsonFile.exists()) {
            // Check fallback to base directory
            jsonFile = new File("C:/Users/manis/Workplace-Harassment-Analytics", jsonPath);
        }

        if (!jsonFile.exists()) {
            throw new IOException("world_laws.json dataset not found at: " + jsonPath);
        }

        System.out.println("\n[WorldLawsLoader] Reading world laws dataset from: " + jsonFile.getAbsolutePath());
        List<Map<String, Object>> countryList = objectMapper.readValue(
                jsonFile, new TypeReference<List<Map<String, Object>>>() {});

        System.out.println("[WorldLawsLoader] Parsed " + countryList.size() + " country objects from JSON.");

        // Drop existing collection first for idempotency
        System.out.println("[WorldLawsLoader] Dropping existing collection '" + collection.getNamespace().getCollectionName() + "'...");
        collection.drop();

        List<Document> documents = new ArrayList<>();
        int count = 0;

        for (Map<String, Object> countryMap : countryList) {
            Document doc = new Document(countryMap);
            documents.add(doc);
            count++;

            if (count % 50 == 0 || count == countryList.size()) {
                System.out.println("[WorldLawsLoader] Progress: Prepared " + count + " / " + countryList.size() + " countries...");
            }
        }

        if (!documents.isEmpty()) {
            collection.insertMany(documents);
        }

        System.out.println("[WorldLawsLoader] SUCCESS: Total documents inserted: " + count);
        return count;
    }

    /**
     * Creates analytical and query indexes on the world_laws collection.
     *
     * @param collection Target collection
     */
    public void createIndexes(MongoCollection<Document> collection) {
        System.out.println("\n[WorldLawsLoader] Creating indexes on collection '" + collection.getNamespace().getCollectionName() + "'...");

        String idxCountry = collection.createIndex(Indexes.ascending("country"));
        System.out.println("[WorldLawsLoader] \u2714 Index created: " + idxCountry + " on field 'country' (ascending)");

        String idxCode = collection.createIndex(Indexes.ascending("country_code"));
        System.out.println("[WorldLawsLoader] \u2714 Index created: " + idxCode + " on field 'country_code' (ascending)");

        String idxScore = collection.createIndex(Indexes.descending("protection_score"));
        System.out.println("[WorldLawsLoader] \u2714 Index created: " + idxScore + " on field 'protection_score' (descending)");

        String idxRegion = collection.createIndex(Indexes.ascending("region"));
        System.out.println("[WorldLawsLoader] \u2714 Index created: " + idxRegion + " on field 'region' (ascending)");
    }

    /**
     * Verifies the loaded data by printing the total document count and a sample document.
     *
     * @param collection Target collection
     * @return true if count equals 193
     */
    public boolean verifyLoad(MongoCollection<Document> collection) {
        long totalCount = collection.countDocuments();
        System.out.println("\n[WorldLawsLoader] Verification:");
        System.out.println("  Total Documents Stored: " + totalCount + " (Expected: 193)");

        Document sample = collection.find().first();
        if (sample != null) {
            JsonWriterSettings settings = JsonWriterSettings.builder().indent(true).build();
            System.out.println("\n[WorldLawsLoader] Sample Document from MongoDB:");
            System.out.println(sample.toJson(settings));
        } else {
            System.err.println("[WorldLawsLoader] Warning: No sample document found in collection.");
        }

        return totalCount == 193;
    }
}