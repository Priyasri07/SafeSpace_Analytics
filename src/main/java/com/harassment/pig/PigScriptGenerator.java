package com.harassment.pig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * Generates Apache Pig Latin scripts for analyzing world laws and EEOC charges datasets.
 */
public class PigScriptGenerator {

    private final String scriptsDir;

    public PigScriptGenerator() {
        this("scripts/pig");
    }

    public PigScriptGenerator(String scriptsDir) {
        this.scriptsDir = scriptsDir;
    }

    /**
     * Generates scripts/pig/world_laws_analysis.pig
     *
     * @return File object of generated script
     */
    public File generateWorldLawsAnalysisScript() throws IOException {
        ensureDirectory();
        File file = new File(scriptsDir, "world_laws_analysis.pig");

        String scriptContent =
                "-- World Laws Analysis in Pig\n" +
                "-- Load flattened CSV version of world_laws.json\n\n" +
                "world_raw = LOAD '/user/harassment/world/world_laws_pig.csv'\n" +
                "        USING PigStorage(',') AS (\n" +
                "            country:chararray,\n" +
                "            country_code:chararray,\n" +
                "            region:chararray,\n" +
                "            sexual_harassment_prohibited:chararray,\n" +
                "            covers_all_workers:chararray,\n" +
                "            covers_all_perpetrators:chararray,\n" +
                "            employer_prevention_responsibility:chararray,\n" +
                "            employer_liability:chararray,\n" +
                "            retaliation_protection:chararray,\n" +
                "            protection_score:float);\n\n" +
                "-- Filter out header if present\n" +
                "world = FILTER world_raw BY country != 'country';\n\n" +
                "-- Filter countries where harassment is NOT prohibited\n" +
                "not_prohibited = FILTER world BY sexual_harassment_prohibited == 'false';\n" +
                "STORE not_prohibited INTO '/user/harassment/outputs/pig_not_prohibited'\n" +
                "      USING PigStorage(',');\n\n" +
                "-- Group countries by region and count\n" +
                "by_region = GROUP world BY region;\n" +
                "region_count = FOREACH by_region GENERATE group AS region,\n" +
                "                                       COUNT(world) AS country_count;\n" +
                "sorted_regions = ORDER region_count BY country_count DESC;\n" +
                "STORE sorted_regions INTO '/user/harassment/outputs/pig_region_count'\n" +
                "      USING PigStorage(',');\n\n" +
                "-- Average protection score by region\n" +
                "avg_by_region = FOREACH by_region GENERATE group AS region,\n" +
                "                                           AVG(world.protection_score) AS avg_score;\n" +
                "STORE avg_by_region INTO '/user/harassment/outputs/pig_avg_protection'\n" +
                "      USING PigStorage(',');\n\n" +
                "-- Countries with full protection (all 6 indicators true)\n" +
                "full_protection = FILTER world BY\n" +
                "    sexual_harassment_prohibited == 'true' AND\n" +
                "    covers_all_workers == 'true' AND\n" +
                "    covers_all_perpetrators == 'true' AND\n" +
                "    employer_prevention_responsibility == 'true' AND\n" +
                "    employer_liability == 'true' AND\n" +
                "    retaliation_protection == 'true';\n" +
                "STORE full_protection INTO '/user/harassment/outputs/pig_full_protection'\n" +
                "      USING PigStorage(',');\n";

        writeFile(file, scriptContent);
        System.out.println("[PigScriptGenerator] Generated: " + file.getAbsolutePath());
        return file;
    }

    /**
     * Generates scripts/pig/eeoc_industry_analysis.pig
     *
     * @return File object of generated script
     */
    public File generateEEOCIndustryAnalysisScript() throws IOException {
        ensureDirectory();
        File file = new File(scriptsDir, "eeoc_industry_analysis.pig");

        String scriptContent =
                "-- EEOC Industry Analysis in Pig\n" +
                "eeoc_raw = LOAD '/user/harassment/eeoc/eeoc_charges.csv'\n" +
                "       USING PigStorage(',') AS (\n" +
                "         charge_filing_date:chararray,\n" +
                "         cp_sex:chararray,\n" +
                "         cp_race:chararray,\n" +
                "         naics_code:chararray,\n" +
                "         naics_description:chararray,\n" +
                "         num_employees:chararray,\n" +
                "         respondent_name:chararray);\n\n" +
                "-- Filter out header if present\n" +
                "eeoc = FILTER eeoc_raw BY charge_filing_date != 'CHARGE_FILING_DATE';\n\n" +
                "-- Count charges by industry\n" +
                "by_industry = GROUP eeoc BY naics_description;\n" +
                "industry_count = FOREACH by_industry GENERATE\n" +
                "    group AS industry, COUNT(eeoc) AS total_charges;\n" +
                "sorted_industries = ORDER industry_count BY total_charges DESC;\n" +
                "STORE sorted_industries INTO '/user/harassment/outputs/pig_industry_count'\n" +
                "      USING PigStorage(',');\n\n" +
                "-- Count charges by sex\n" +
                "by_sex = GROUP eeoc BY cp_sex;\n" +
                "sex_count = FOREACH by_sex GENERATE group AS sex, COUNT(eeoc) AS count;\n" +
                "STORE sex_count INTO '/user/harassment/outputs/pig_sex_count'\n" +
                "      USING PigStorage(',');\n";

        writeFile(file, scriptContent);
        System.out.println("[PigScriptGenerator] Generated: " + file.getAbsolutePath());
        return file;
    }

    /**
     * Generates all Pig Latin scripts.
     */
    public void generateAllScripts() throws IOException {
        generateWorldLawsAnalysisScript();
        generateEEOCIndustryAnalysisScript();
    }

    private void ensureDirectory() {
        File dir = new File(scriptsDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private void writeFile(File file, String content) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }
}