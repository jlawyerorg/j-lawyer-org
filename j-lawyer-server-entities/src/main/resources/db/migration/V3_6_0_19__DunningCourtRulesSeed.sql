-- Seed of the dunning court rules for the twelve central dunning courts.
--
-- Generated from the responsibility list of BundledDunningCourtDirectory, whose origin is
-- documented in the package documentation of com.jdimension.jlawyer.referencedata: the assignment
-- comes from https://www.mahngerichte.de/mahngerichte/.
--
-- The fixed ids follow the selection key - the federal state, plus the restriction where a state is
-- divided - and not the court. A court serves several states (the Amtsgericht Wedding serves Berlin
-- and Brandenburg, the Amtsgericht Aschersleben three states), so a court-derived id would collide.
-- They are kept short enough for the VARCHAR(50) of the id column.
--
-- Two entries carry a restriction instead of covering a whole state: North Rhine-Westphalia is
-- divided along the OLG district of Cologne. No postal-code range is given for them, because the
-- division follows the OLG district and cannot be expressed numerically from what the courts
-- publish - the system offers both candidates and lets the user decide rather than guessing. A firm
-- that knows its own numeric division can add it in the administration.
--
-- The rules attach to the seeded courts through their XJustiz identifier, so a court that was
-- corrected or re-seeded keeps its rule. Seeding is repeatable: a rule is inserted only if no rule
-- for the same court and the same selection key exists.
--
-- Accepted channels are the ones the courts publish; the Amtsgericht Wedding publishes none, so its
-- rule carries none either.

INSERT INTO dunning_court_rules (id, court_id, federal_state, foreign_applicant, restriction,
    accepted_channels, requires_own_kennziffer, direct_debit_nationwide, sequence_number, active)
SELECT * FROM (SELECT 'seed-dcr-baden-wuerttemberg' AS id, c.id AS court_id, 'Baden-Württemberg' AS federal_state,
    0 AS foreign_applicant, NULL AS restriction,
    'Nutzung sicherer Übermittlungswege (beA, beN, beBPo) sowie OSCI-konforme Produkte; online-Mahnantrag; Barcode-Mahnantrag' AS accepted_channels, 0 AS requires_own_kennziffer, 0 AS direct_debit_nationwide,
    0 AS sequence_number, 1 AS active
  FROM courts c WHERE c.xjustiz_id = 'B2609') AS tmp
WHERE NOT EXISTS (SELECT 1 FROM dunning_court_rules r
    WHERE r.court_id = tmp.court_id AND r.federal_state = 'Baden-Württemberg' AND r.restriction IS NULL);

INSERT INTO dunning_court_rules (id, court_id, federal_state, foreign_applicant, restriction,
    accepted_channels, requires_own_kennziffer, direct_debit_nationwide, sequence_number, active)
SELECT * FROM (SELECT 'seed-dcr-bayern' AS id, c.id AS court_id, 'Bayern' AS federal_state,
    0 AS foreign_applicant, NULL AS restriction,
    'Nutzung sicherer Übermittlungswege (beA, beN, beBPo) sowie OSCI-konforme Produkte; online-Mahnantrag; Barcode-Mahnantrag' AS accepted_channels, 0 AS requires_own_kennziffer, 0 AS direct_debit_nationwide,
    0 AS sequence_number, 1 AS active
  FROM courts c WHERE c.xjustiz_id = 'D4401') AS tmp
WHERE NOT EXISTS (SELECT 1 FROM dunning_court_rules r
    WHERE r.court_id = tmp.court_id AND r.federal_state = 'Bayern' AND r.restriction IS NULL);

INSERT INTO dunning_court_rules (id, court_id, federal_state, foreign_applicant, restriction,
    accepted_channels, requires_own_kennziffer, direct_debit_nationwide, sequence_number, active)
SELECT * FROM (SELECT 'seed-dcr-berlin' AS id, c.id AS court_id, 'Berlin' AS federal_state,
    0 AS foreign_applicant, NULL AS restriction,
    NULL AS accepted_channels, 0 AS requires_own_kennziffer, 0 AS direct_debit_nationwide,
    0 AS sequence_number, 1 AS active
  FROM courts c WHERE c.xjustiz_id = 'F1102') AS tmp
WHERE NOT EXISTS (SELECT 1 FROM dunning_court_rules r
    WHERE r.court_id = tmp.court_id AND r.federal_state = 'Berlin' AND r.restriction IS NULL);

INSERT INTO dunning_court_rules (id, court_id, federal_state, foreign_applicant, restriction,
    accepted_channels, requires_own_kennziffer, direct_debit_nationwide, sequence_number, active)
SELECT * FROM (SELECT 'seed-dcr-brandenburg' AS id, c.id AS court_id, 'Brandenburg' AS federal_state,
    0 AS foreign_applicant, NULL AS restriction,
    NULL AS accepted_channels, 0 AS requires_own_kennziffer, 0 AS direct_debit_nationwide,
    0 AS sequence_number, 1 AS active
  FROM courts c WHERE c.xjustiz_id = 'F1102') AS tmp
WHERE NOT EXISTS (SELECT 1 FROM dunning_court_rules r
    WHERE r.court_id = tmp.court_id AND r.federal_state = 'Brandenburg' AND r.restriction IS NULL);

INSERT INTO dunning_court_rules (id, court_id, federal_state, foreign_applicant, restriction,
    accepted_channels, requires_own_kennziffer, direct_debit_nationwide, sequence_number, active)
SELECT * FROM (SELECT 'seed-dcr-bremen' AS id, c.id AS court_id, 'Bremen' AS federal_state,
    0 AS foreign_applicant, NULL AS restriction,
    'Nutzung sicherer Übermittlungswege (beA, beN, beBPo) sowie OSCI-konforme Produkte; online-Mahnantrag; Barcode-Mahnantrag' AS accepted_channels, 0 AS requires_own_kennziffer, 0 AS direct_debit_nationwide,
    0 AS sequence_number, 1 AS active
  FROM courts c WHERE c.xjustiz_id = 'H1101') AS tmp
WHERE NOT EXISTS (SELECT 1 FROM dunning_court_rules r
    WHERE r.court_id = tmp.court_id AND r.federal_state = 'Bremen' AND r.restriction IS NULL);

INSERT INTO dunning_court_rules (id, court_id, federal_state, foreign_applicant, restriction,
    accepted_channels, requires_own_kennziffer, direct_debit_nationwide, sequence_number, active)
SELECT * FROM (SELECT 'seed-dcr-hamburg' AS id, c.id AS court_id, 'Hamburg' AS federal_state,
    0 AS foreign_applicant, NULL AS restriction,
    'Nutzung sicherer Übermittlungswege (beA, beN, beBPo) sowie OSCI-konforme Produkte; online-Mahnantrag; Barcode-Mahnantrag' AS accepted_channels, 0 AS requires_own_kennziffer, 0 AS direct_debit_nationwide,
    0 AS sequence_number, 1 AS active
  FROM courts c WHERE c.xjustiz_id = 'K1102') AS tmp
WHERE NOT EXISTS (SELECT 1 FROM dunning_court_rules r
    WHERE r.court_id = tmp.court_id AND r.federal_state = 'Hamburg' AND r.restriction IS NULL);

INSERT INTO dunning_court_rules (id, court_id, federal_state, foreign_applicant, restriction,
    accepted_channels, requires_own_kennziffer, direct_debit_nationwide, sequence_number, active)
SELECT * FROM (SELECT 'seed-dcr-hessen' AS id, c.id AS court_id, 'Hessen' AS federal_state,
    0 AS foreign_applicant, NULL AS restriction,
    'Nutzung sicherer Übermittlungswege (beA, beN, beBPo) sowie OSCI-konforme Produkte; online-Mahnantrag; Barcode-Mahnantrag' AS accepted_channels, 0 AS requires_own_kennziffer, 0 AS direct_debit_nationwide,
    0 AS sequence_number, 1 AS active
  FROM courts c WHERE c.xjustiz_id = 'M1307') AS tmp
WHERE NOT EXISTS (SELECT 1 FROM dunning_court_rules r
    WHERE r.court_id = tmp.court_id AND r.federal_state = 'Hessen' AND r.restriction IS NULL);

INSERT INTO dunning_court_rules (id, court_id, federal_state, foreign_applicant, restriction,
    accepted_channels, requires_own_kennziffer, direct_debit_nationwide, sequence_number, active)
SELECT * FROM (SELECT 'seed-dcr-mecklenburg-vorpommern' AS id, c.id AS court_id, 'Mecklenburg-Vorpommern' AS federal_state,
    0 AS foreign_applicant, NULL AS restriction,
    'Nutzung sicherer Übermittlungswege (beA, beN, beBPo) sowie OSCI-konforme Produkte; online-Mahnantrag; Barcode-Mahnantrag' AS accepted_channels, 0 AS requires_own_kennziffer, 0 AS direct_debit_nationwide,
    0 AS sequence_number, 1 AS active
  FROM courts c WHERE c.xjustiz_id = 'K1102') AS tmp
WHERE NOT EXISTS (SELECT 1 FROM dunning_court_rules r
    WHERE r.court_id = tmp.court_id AND r.federal_state = 'Mecklenburg-Vorpommern' AND r.restriction IS NULL);

INSERT INTO dunning_court_rules (id, court_id, federal_state, foreign_applicant, restriction,
    accepted_channels, requires_own_kennziffer, direct_debit_nationwide, sequence_number, active)
SELECT * FROM (SELECT 'seed-dcr-niedersachsen' AS id, c.id AS court_id, 'Niedersachsen' AS federal_state,
    0 AS foreign_applicant, NULL AS restriction,
    'Nutzung sicherer Übermittlungswege (beA, beN, beBPo) sowie OSCI-konforme Produkte; online-Mahnantrag; Barcode-Mahnantrag' AS accepted_channels, 0 AS requires_own_kennziffer, 0 AS direct_debit_nationwide,
    0 AS sequence_number, 1 AS active
  FROM courts c WHERE c.xjustiz_id = 'P2510') AS tmp
WHERE NOT EXISTS (SELECT 1 FROM dunning_court_rules r
    WHERE r.court_id = tmp.court_id AND r.federal_state = 'Niedersachsen' AND r.restriction IS NULL);

INSERT INTO dunning_court_rules (id, court_id, federal_state, foreign_applicant, restriction,
    accepted_channels, requires_own_kennziffer, direct_debit_nationwide, sequence_number, active)
SELECT * FROM (SELECT 'seed-dcr-nordrhein-westfalen-olg-bezirk-koeln' AS id, c.id AS court_id, 'Nordrhein-Westfalen' AS federal_state,
    0 AS foreign_applicant, 'OLG-Bezirk Köln' AS restriction,
    'Nutzung sicherer Übermittlungswege (beA, beN, beBPo) sowie OSCI-konforme Produkte; online-Mahnantrag; Barcode-Mahnantrag' AS accepted_channels, 0 AS requires_own_kennziffer, 0 AS direct_debit_nationwide,
    0 AS sequence_number, 1 AS active
  FROM courts c WHERE c.xjustiz_id = 'R3203') AS tmp
WHERE NOT EXISTS (SELECT 1 FROM dunning_court_rules r
    WHERE r.court_id = tmp.court_id AND r.federal_state = 'Nordrhein-Westfalen' AND r.restriction = 'OLG-Bezirk Köln');

INSERT INTO dunning_court_rules (id, court_id, federal_state, foreign_applicant, restriction,
    accepted_channels, requires_own_kennziffer, direct_debit_nationwide, sequence_number, active)
SELECT * FROM (SELECT 'seed-dcr-nordrhein-westfalen-im-ubrigen' AS id, c.id AS court_id, 'Nordrhein-Westfalen' AS federal_state,
    0 AS foreign_applicant, 'Im Übrigen' AS restriction,
    'Nutzung sicherer Übermittlungswege (beA, beN, beBPo) sowie OSCI-konforme Produkte; online-Mahnantrag; Barcode-Mahnantrag' AS accepted_channels, 0 AS requires_own_kennziffer, 0 AS direct_debit_nationwide,
    0 AS sequence_number, 1 AS active
  FROM courts c WHERE c.xjustiz_id = 'R2602') AS tmp
WHERE NOT EXISTS (SELECT 1 FROM dunning_court_rules r
    WHERE r.court_id = tmp.court_id AND r.federal_state = 'Nordrhein-Westfalen' AND r.restriction = 'Im Übrigen');

INSERT INTO dunning_court_rules (id, court_id, federal_state, foreign_applicant, restriction,
    accepted_channels, requires_own_kennziffer, direct_debit_nationwide, sequence_number, active)
SELECT * FROM (SELECT 'seed-dcr-rheinland-pfalz' AS id, c.id AS court_id, 'Rheinland-Pfalz' AS federal_state,
    0 AS foreign_applicant, NULL AS restriction,
    'Nutzung sicherer Übermittlungswege (beA, beN, beBPo) sowie OSCI-konforme Produkte; online-Mahnantrag; Barcode-Mahnantrag' AS accepted_channels, 0 AS requires_own_kennziffer, 0 AS direct_debit_nationwide,
    0 AS sequence_number, 1 AS active
  FROM courts c WHERE c.xjustiz_id = 'T2213') AS tmp
WHERE NOT EXISTS (SELECT 1 FROM dunning_court_rules r
    WHERE r.court_id = tmp.court_id AND r.federal_state = 'Rheinland-Pfalz' AND r.restriction IS NULL);

INSERT INTO dunning_court_rules (id, court_id, federal_state, foreign_applicant, restriction,
    accepted_channels, requires_own_kennziffer, direct_debit_nationwide, sequence_number, active)
SELECT * FROM (SELECT 'seed-dcr-saarland' AS id, c.id AS court_id, 'Saarland' AS federal_state,
    0 AS foreign_applicant, NULL AS restriction,
    'Nutzung sicherer Übermittlungswege (beA, beN, beBPo) sowie OSCI-konforme Produkte; online-Mahnantrag; Barcode-Mahnantrag' AS accepted_channels, 0 AS requires_own_kennziffer, 0 AS direct_debit_nationwide,
    0 AS sequence_number, 1 AS active
  FROM courts c WHERE c.xjustiz_id = 'T2213') AS tmp
WHERE NOT EXISTS (SELECT 1 FROM dunning_court_rules r
    WHERE r.court_id = tmp.court_id AND r.federal_state = 'Saarland' AND r.restriction IS NULL);

INSERT INTO dunning_court_rules (id, court_id, federal_state, foreign_applicant, restriction,
    accepted_channels, requires_own_kennziffer, direct_debit_nationwide, sequence_number, active)
SELECT * FROM (SELECT 'seed-dcr-sachsen' AS id, c.id AS court_id, 'Sachsen' AS federal_state,
    0 AS foreign_applicant, NULL AS restriction,
    'Nutzung sicherer Übermittlungswege (beA, beN, beBPo) sowie OSCI-konforme Produkte; online-Mahnantrag; Barcode-Mahnantrag' AS accepted_channels, 0 AS requires_own_kennziffer, 0 AS direct_debit_nationwide,
    0 AS sequence_number, 1 AS active
  FROM courts c WHERE c.xjustiz_id = 'W1101') AS tmp
WHERE NOT EXISTS (SELECT 1 FROM dunning_court_rules r
    WHERE r.court_id = tmp.court_id AND r.federal_state = 'Sachsen' AND r.restriction IS NULL);

INSERT INTO dunning_court_rules (id, court_id, federal_state, foreign_applicant, restriction,
    accepted_channels, requires_own_kennziffer, direct_debit_nationwide, sequence_number, active)
SELECT * FROM (SELECT 'seed-dcr-sachsen-anhalt' AS id, c.id AS court_id, 'Sachsen-Anhalt' AS federal_state,
    0 AS foreign_applicant, NULL AS restriction,
    'Nutzung sicherer Übermittlungswege (beA, beN, beBPo) sowie OSCI-konforme Produkte; online-Mahnantrag; Barcode-Mahnantrag' AS accepted_channels, 0 AS requires_own_kennziffer, 0 AS direct_debit_nationwide,
    0 AS sequence_number, 1 AS active
  FROM courts c WHERE c.xjustiz_id = 'W1101') AS tmp
WHERE NOT EXISTS (SELECT 1 FROM dunning_court_rules r
    WHERE r.court_id = tmp.court_id AND r.federal_state = 'Sachsen-Anhalt' AND r.restriction IS NULL);

INSERT INTO dunning_court_rules (id, court_id, federal_state, foreign_applicant, restriction,
    accepted_channels, requires_own_kennziffer, direct_debit_nationwide, sequence_number, active)
SELECT * FROM (SELECT 'seed-dcr-schleswig-holstein' AS id, c.id AS court_id, 'Schleswig-Holstein' AS federal_state,
    0 AS foreign_applicant, NULL AS restriction,
    'Nutzung sicherer Übermittlungswege (beA, beN, beBPo) sowie OSCI-konforme Produkte; online-Mahnantrag; Barcode-Mahnantrag' AS accepted_channels, 0 AS requires_own_kennziffer, 0 AS direct_debit_nationwide,
    0 AS sequence_number, 1 AS active
  FROM courts c WHERE c.xjustiz_id = 'X1119') AS tmp
WHERE NOT EXISTS (SELECT 1 FROM dunning_court_rules r
    WHERE r.court_id = tmp.court_id AND r.federal_state = 'Schleswig-Holstein' AND r.restriction IS NULL);

INSERT INTO dunning_court_rules (id, court_id, federal_state, foreign_applicant, restriction,
    accepted_channels, requires_own_kennziffer, direct_debit_nationwide, sequence_number, active)
SELECT * FROM (SELECT 'seed-dcr-thueringen' AS id, c.id AS court_id, 'Thüringen' AS federal_state,
    0 AS foreign_applicant, NULL AS restriction,
    'Nutzung sicherer Übermittlungswege (beA, beN, beBPo) sowie OSCI-konforme Produkte; online-Mahnantrag; Barcode-Mahnantrag' AS accepted_channels, 0 AS requires_own_kennziffer, 0 AS direct_debit_nationwide,
    0 AS sequence_number, 1 AS active
  FROM courts c WHERE c.xjustiz_id = 'W1101') AS tmp
WHERE NOT EXISTS (SELECT 1 FROM dunning_court_rules r
    WHERE r.court_id = tmp.court_id AND r.federal_state = 'Thüringen' AND r.restriction IS NULL);

INSERT INTO dunning_court_rules (id, court_id, federal_state, foreign_applicant, restriction,
    accepted_channels, requires_own_kennziffer, direct_debit_nationwide, sequence_number, active)
SELECT * FROM (SELECT 'seed-dcr-ausland' AS id, c.id AS court_id, NULL AS federal_state,
    1 AS foreign_applicant, 'Antragsteller mit Sitz/Wohnsitz im Ausland' AS restriction,
    NULL AS accepted_channels, 0 AS requires_own_kennziffer, 0 AS direct_debit_nationwide,
    0 AS sequence_number, 1 AS active
  FROM courts c WHERE c.xjustiz_id = 'F1102') AS tmp
WHERE NOT EXISTS (SELECT 1 FROM dunning_court_rules r
    WHERE r.court_id = tmp.court_id AND r.federal_state IS NULL AND r.restriction = 'Antragsteller mit Sitz/Wohnsitz im Ausland');

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.19') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.19';
commit;
