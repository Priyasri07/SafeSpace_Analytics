-- EEOC Industry Analysis in Pig
eeoc_raw = LOAD '/user/harassment/eeoc/eeoc_charges.csv'
       USING PigStorage(',') AS (
         charge_filing_date:chararray,
         cp_sex:chararray,
         cp_race:chararray,
         naics_code:chararray,
         naics_description:chararray,
         num_employees:chararray,
         respondent_name:chararray);

-- Filter out header if present
eeoc = FILTER eeoc_raw BY charge_filing_date != 'CHARGE_FILING_DATE';

-- Count charges by industry
by_industry = GROUP eeoc BY naics_description;
industry_count = FOREACH by_industry GENERATE
    group AS industry, COUNT(eeoc) AS total_charges;
sorted_industries = ORDER industry_count BY total_charges DESC;
STORE sorted_industries INTO '/user/harassment/outputs/pig_industry_count'
      USING PigStorage(',');

-- Count charges by sex
by_sex = GROUP eeoc BY cp_sex;
sex_count = FOREACH by_sex GENERATE group AS sex, COUNT(eeoc) AS count;
STORE sex_count INTO '/user/harassment/outputs/pig_sex_count'
      USING PigStorage(',');
