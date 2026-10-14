package com.harassment.ingestion;

import com.harassment.utils.ConfigLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Downloads the EEOC harassment charges dataset or generates a realistic synthetic
 * dataset of 195,000 rows when remote endpoints are unavailable.
 */
public class EEOCDataDownloader {

    private static final String PRIMARY_URL = "https://www.eeoc.gov/data/charge-statistics-fy-1997-through-fy-2023";
    private static final String DIRECT_ZIP_URL = "https://www.eeoc.gov/sites/default/files/2024-01/EEOC_Charge_Data.zip";

    private static final int TARGET_ROW_COUNT = 195000;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Industry mappings (NAICS code and description)
    private static final class Industry {
        final int code;
        final String description;
        Industry(int code, String description) {
            this.code = code;
            this.description = description;
        }
    }

    private static final List<Industry> INDUSTRIES = Arrays.asList(
            new Industry(72, "Accommodation and Food Services"),
            new Industry(44, "Retail Trade"),
            new Industry(62, "Health Care and Social Assistance"),
            new Industry(31, "Manufacturing"),
            new Industry(54, "Professional Scientific and Technical Services"),
            new Industry(61, "Educational Services"),
            new Industry(23, "Construction"),
            new Industry(42, "Wholesale Trade"),
            new Industry(56, "Administrative and Support Services"),
            new Industry(92, "Public Administration")
    );

    // Races
    private static final String[] RACES = {"Black", "White", "Hispanic", "Asian", "Other"};
    private static final double[] RACE_CUMULATIVE_WEIGHTS = {0.33, 0.63, 0.83, 0.93, 1.0};

    // Base company prefixes and suffixes for entity resolution testing
    private static final String[] COMPANY_BASES = {
            "Apex Logistics", "Beacon Health", "Horizon Hospitality", "Summit Retail",
            "Vanguard Technologies", "Pinnacle Manufacturing", "Heritage Senior Living",
            "Metro Dining Group", "Cascade Construction", "Global Financial Solutions",
            "Silverline Care", "Bluecrest Academy", "Golden State Foods", "Empire Distribution",
            "Pacific Hospitality Partners", "Northwest Medical Center", "Omni Staffing Solutions",
            "Frontier Energy", "Trinity Health Systems", "Keystone Freight", "Paramount Hospitality",
            "Evergreen Education Group", "Cardinal Health Services", "Titan Industrial Services",
            "Crestview Holdings", "Liberty Protective Services", "Sterling Hotel Group",
            "Nexis Software", "Prime Retail Solutions", "Atlas Mechanical"
    };

    private static final String[] DBA_BRANDS = {
            "Grand Bistro", "Express Mart", "Cornerstone Cafe", "Sunrise Inn",
            "Urban Care Clinic", "Rapid Freight", "Elite Fitness", "Premier Cleaners"
    };

    /**
     * Attempts to download EEOC charge data or falls back to synthetic generation.
     *
     * @return Number of records processed/generated
     */
    public static long download() {
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("[EEOC] Initiating EEOC harassment charges dataset acquisition...");
        System.out.println("--------------------------------------------------------------------------------");

        File rawDir = new File(ConfigLoader.getLocalRawPath());
        if (!rawDir.exists()) {
            boolean created = rawDir.mkdirs();
            if (created) {
                System.out.println("[EEOC] Created target raw directory: " + rawDir.getAbsolutePath());
            }
        }

        File targetFile = new File(rawDir, ConfigLoader.getEeocFile());

        // Attempt HTTP download
        boolean downloadSuccess = tryHttpDownload(PRIMARY_URL, targetFile);
        if (!downloadSuccess) {
            downloadSuccess = tryHttpDownload(DIRECT_ZIP_URL, targetFile);
        }

        if (downloadSuccess && targetFile.exists() && targetFile.length() > 50000) {
            System.out.println("[EEOC] Successfully downloaded official dataset to: " + targetFile.getAbsolutePath());
            return countLines(targetFile) - 1;
        }

        System.out.println("[EEOC] Online dataset not directly downloadable as CSV (or returned HTML).");
        System.out.println("[EEOC] Falling back to generating a realistic synthetic dataset of " + TARGET_ROW_COUNT + " rows...");

        return generateSyntheticDataset(targetFile);
    }

    private static boolean tryHttpDownload(String urlStr, File destination) {
        System.out.println("[EEOC] Attempting download from: " + urlStr);
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) BigDataIngestion/1.0");

            int responseCode = conn.getResponseCode();
            System.out.println("[EEOC] Response code: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                String contentType = conn.getContentType();
                if (contentType != null && (contentType.contains("csv") || contentType.contains("zip") || contentType.contains("octet-stream"))) {
                    try (InputStream in = conn.getInputStream();
                         FileOutputStream out = new FileOutputStream(destination)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                    return true;
                }
            }
        } catch (Exception e) {
            System.out.println("[EEOC] Download attempt error: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return false;
    }

    private static long generateSyntheticDataset(File targetFile) {
        Random rand = new Random(42); // Seeded for reproducibility and consistency
        long startTime = System.currentTimeMillis();

        LocalDate startDate = LocalDate.of(1997, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 12, 31);
        long startDay = startDate.toEpochDay();
        long endDay = endDate.toEpochDay();

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(targetFile), StandardCharsets.UTF_8), 32768)) {

            // CSV Header
            writer.write("CHARGE_FILING_DATE,CP_SEX,CP_RACE_STRING,R_NAICS_CODE,R_NAICS_DESCRIPTION,R_NUMBER_OF_EMPLOYEES,RESPONDENT_NAME\n");

            for (int i = 1; i <= TARGET_ROW_COUNT; i++) {
                // 1. Filing Date between 1997 and 2023
                long randomDay = startDay + (long) (rand.nextDouble() * (endDay - startDay + 1));
                String filingDate = LocalDate.ofEpochDay(randomDay).format(DATE_FORMATTER);

                // 2. CP_SEX (Female: 83%, Male: 17%)
                String cpSex = (rand.nextDouble() < 0.83) ? "Female" : "Male";

                // 3. CP_RACE_STRING
                double raceRoll = rand.nextDouble();
                String cpRace = RACES[RACES.length - 1];
                for (int r = 0; r < RACES.length; r++) {
                    if (raceRoll < RACE_CUMULATIVE_WEIGHTS[r]) {
                        cpRace = RACES[r];
                        break;
                    }
                }

                // 4 & 5. R_NAICS_CODE & R_NAICS_DESCRIPTION
                Industry industry = INDUSTRIES.get(rand.nextInt(INDUSTRIES.size()));

                // 6. R_NUMBER_OF_EMPLOYEES (1-50,000, weighted toward smaller companies)
                int employees;
                double empRoll = rand.nextDouble();
                if (empRoll < 0.60) {
                    // Small business: 1 - 100
                    employees = 1 + rand.nextInt(100);
                } else if (empRoll < 0.85) {
                    // Mid-small: 101 - 500
                    employees = 101 + rand.nextInt(400);
                } else if (empRoll < 0.96) {
                    // Medium: 501 - 2,500
                    employees = 501 + rand.nextInt(2000);
                } else {
                    // Large enterprise: 2,501 - 50,000
                    employees = 2501 + rand.nextInt(47499);
                }

                // 7. RESPONDENT_NAME (with variations for text analytics / entity resolution)
                String baseCompany = COMPANY_BASES[rand.nextInt(COMPANY_BASES.length)];
                String respondentName = formatCompanyVariation(baseCompany, rand);

                // CSV line writing with quote escaping if necessary
                writer.write(filingDate);
                writer.write(',');
                writer.write(cpSex);
                writer.write(',');
                writer.write(cpRace);
                writer.write(',');
                writer.write(String.valueOf(industry.code));
                writer.write(',');
                writer.write('"');
                writer.write(industry.description);
                writer.write("\",");
                writer.write(String.valueOf(employees));
                writer.write(',');
                writer.write('"');
                writer.write(respondentName.replace("\"", "\"\""));
                writer.write("\"\n");

                if (i % 10000 == 0 || i == TARGET_ROW_COUNT) {
                    System.out.printf("[EEOC] Progress: %,d / %,d rows generated (%.1f%%)%n",
                            i, TARGET_ROW_COUNT, (i * 100.0 / TARGET_ROW_COUNT));
                }
            }

            writer.flush();
        } catch (IOException e) {
            System.err.println("[EEOC] Error writing EEOC charges CSV: " + e.getMessage());
            return 0;
        }

        long durationMs = System.currentTimeMillis() - startTime;
        double fileSizeMb = targetFile.length() / (1024.0 * 1024.0);
        DecimalFormat df = new DecimalFormat("#.##");

        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("[EEOC] Generation Completed in %d ms%n", durationMs);
        System.out.printf("[EEOC] Total Rows: %,d%n", TARGET_ROW_COUNT);
        System.out.printf("[EEOC] File Path  : %s%n", targetFile.getAbsolutePath());
        System.out.printf("[EEOC] File Size  : %s MB (%,d bytes)%n", df.format(fileSizeMb), targetFile.length());
        System.out.println("--------------------------------------------------------------------------------");

        return TARGET_ROW_COUNT;
    }

    private static String formatCompanyVariation(String baseName, Random rand) {
        int variant = rand.nextInt(8);
        switch (variant) {
            case 0:
                return baseName + " Corp";
            case 1:
                return baseName + " Corp.";
            case 2:
                return (baseName + " CORP").toUpperCase();
            case 3:
                return baseName + " Corporation";
            case 4:
                String brand = DBA_BRANDS[rand.nextInt(DBA_BRANDS.length)];
                return baseName + " Corp d/b/a " + brand;
            case 5:
                return baseName + " Inc.";
            case 6:
                return baseName + " LLC";
            case 7:
            default:
                return baseName + " Group";
        }
    }

    private static long countLines(File file) {
        long count = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new java.io.FileInputStream(file), StandardCharsets.UTF_8))) {
            while (reader.readLine() != null) {
                count++;
            }
        } catch (IOException ignored) {
        }
        return count;
    }
}