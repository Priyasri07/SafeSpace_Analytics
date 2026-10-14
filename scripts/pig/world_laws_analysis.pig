-- World Laws Analysis in Pig
-- Load flattened CSV version of world_laws.json

world_raw = LOAD '/user/harassment/world/world_laws_pig.csv'
        USING PigStorage(',') AS (
            country:chararray,
            country_code:chararray,
            region:chararray,
            sexual_harassment_prohibited:chararray,
            covers_all_workers:chararray,
            covers_all_perpetrators:chararray,
            employer_prevention_responsibility:chararray,
            employer_liability:chararray,
            retaliation_protection:chararray,
            protection_score:float);

-- Filter out header if present
world = FILTER world_raw BY country != 'country';

-- Filter countries where harassment is NOT prohibited
not_prohibited = FILTER world BY sexual_harassment_prohibited == 'false';
STORE not_prohibited INTO '/user/harassment/outputs/pig_not_prohibited'
      USING PigStorage(',');

-- Group countries by region and count
by_region = GROUP world BY region;
region_count = FOREACH by_region GENERATE group AS region,
                                       COUNT(world) AS country_count;
sorted_regions = ORDER region_count BY country_count DESC;
STORE sorted_regions INTO '/user/harassment/outputs/pig_region_count'
      USING PigStorage(',');

-- Average protection score by region
avg_by_region = FOREACH by_region GENERATE group AS region,
                                           AVG(world.protection_score) AS avg_score;
STORE avg_by_region INTO '/user/harassment/outputs/pig_avg_protection'
      USING PigStorage(',');

-- Countries with full protection (all 6 indicators true)
full_protection = FILTER world BY
    sexual_harassment_prohibited == 'true' AND
    covers_all_workers == 'true' AND
    covers_all_perpetrators == 'true' AND
    employer_prevention_responsibility == 'true' AND
    employer_liability == 'true' AND
    retaliation_protection == 'true';
STORE full_protection INTO '/user/harassment/outputs/pig_full_protection'
      USING PigStorage(',');
