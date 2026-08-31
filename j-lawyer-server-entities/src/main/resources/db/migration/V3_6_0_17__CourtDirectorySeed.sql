-- Seed of the twelve central dunning courts.
--
-- The values are the ones carried in BundledDunningCourtDirectory and were generated from it, so
-- the seed and the class it replaces cannot drift apart. Their origin is documented there and in
-- the package documentation of com.jdimension.jlawyer.referencedata: the assignment and the
-- addresses come from mahngerichte.de, the XJustiz identifiers from the code list gds.gerichte.
--
-- Seeding is repeatable: a court is inserted only if no court with the same XJustiz identifier
-- exists, so an installation that already holds a seeded or hand-corrected court keeps it. The ids
-- are fixed for the same reason.
--
-- Only the dunning scope is seeded. Whether one of these Amtsgerichte also acts as an enforcement
-- or litigation court is for the firm to record, not something to assert here.

INSERT INTO courts (id, xjustiz_id, name, additional_designation, postal_street, postal_code, city,
    house_street, house_postal_code, house_city, phone, fax, email, web, active)
SELECT * FROM (SELECT 'seed-court-w1101' AS id, 'W1101' AS xjustiz_id, 'Amtsgericht Aschersleben' AS name,
    'Gemeinsames Mahngericht der Länder Sachsen-Anhalt, Sachsen und Thüringen' AS additional_designation,
    'Lehrter Str. 15' AS postal_street, '39418' AS postal_code, 'Staßfurt' AS city,
    NULL AS house_street, NULL AS house_postal_code, NULL AS house_city,
    '03925/876-0' AS phone, '03925/876-252' AS fax, 'Mahngericht@Justiz.sachsen-anhalt.de' AS email, NULL AS web,
    1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM courts c WHERE c.xjustiz_id = 'W1101');

INSERT INTO court_scopes (id, court_id, scope)
SELECT * FROM (SELECT 'seed-scope-w1101' AS id, 'seed-court-w1101' AS court_id, 'DUNNING' AS scope) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM court_scopes s WHERE s.court_id = 'seed-court-w1101' AND s.scope = 'DUNNING');

INSERT INTO courts (id, xjustiz_id, name, additional_designation, postal_street, postal_code, city,
    house_street, house_postal_code, house_city, phone, fax, email, web, active)
SELECT * FROM (SELECT 'seed-court-h1101' AS id, 'H1101' AS xjustiz_id, 'Amtsgericht Bremen' AS name,
    'Mahnabteilung' AS additional_designation,
    'Ostertorstr. 25-31' AS postal_street, '28195' AS postal_code, 'Bremen' AS city,
    NULL AS house_street, NULL AS house_postal_code, NULL AS house_city,
    '0421 / 361 – 6115' AS phone, '0421 / 496 – 34851' AS fax, 'mahnabteilung@amtsgericht.bremen.de' AS email, 'www.amtsgericht.bremen.de' AS web,
    1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM courts c WHERE c.xjustiz_id = 'H1101');

INSERT INTO court_scopes (id, court_id, scope)
SELECT * FROM (SELECT 'seed-scope-h1101' AS id, 'seed-court-h1101' AS court_id, 'DUNNING' AS scope) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM court_scopes s WHERE s.court_id = 'seed-court-h1101' AND s.scope = 'DUNNING');

INSERT INTO courts (id, xjustiz_id, name, additional_designation, postal_street, postal_code, city,
    house_street, house_postal_code, house_city, phone, fax, email, web, active)
SELECT * FROM (SELECT 'seed-court-d4401' AS id, 'D4401' AS xjustiz_id, 'Amtsgericht Coburg' AS name,
    'Zentrales Mahngericht' AS additional_designation,
    NULL AS postal_street, '96441' AS postal_code, 'Coburg' AS city,
    'Heiligkreuzstraße 22' AS house_street, '96450' AS house_postal_code, 'Coburg' AS house_city,
    '(09561) 878-5' AS phone, '09621 / 962 414 232' AS fax, 'poststelle.zentrales.mahngericht@ag-co.bayern.de' AS email, 'www.justiz.bayern.de/gericht/ag/co-zema/' AS web,
    1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM courts c WHERE c.xjustiz_id = 'D4401');

INSERT INTO court_scopes (id, court_id, scope)
SELECT * FROM (SELECT 'seed-scope-d4401' AS id, 'seed-court-d4401' AS court_id, 'DUNNING' AS scope) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM court_scopes s WHERE s.court_id = 'seed-court-d4401' AND s.scope = 'DUNNING');

INSERT INTO courts (id, xjustiz_id, name, additional_designation, postal_street, postal_code, city,
    house_street, house_postal_code, house_city, phone, fax, email, web, active)
SELECT * FROM (SELECT 'seed-court-r3203' AS id, 'R3203' AS xjustiz_id, 'Amtsgericht Euskirchen' AS name,
    'Zentrale Mahnabteilung' AS additional_designation,
    NULL AS postal_street, '53878' AS postal_code, 'Euskirchen' AS city,
    'Kölner Str. 40 – 42' AS house_street, '53879' AS house_postal_code, 'Euskirchen' AS house_city,
    '02251 951 – 0' AS phone, '02251 951 – 2900' AS fax, 'poststelle@ag-euskirchen.nrw.de' AS email, 'www.ag-euskirchen.nrw.de' AS web,
    1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM courts c WHERE c.xjustiz_id = 'R3203');

INSERT INTO court_scopes (id, court_id, scope)
SELECT * FROM (SELECT 'seed-scope-r3203' AS id, 'seed-court-r3203' AS court_id, 'DUNNING' AS scope) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM court_scopes s WHERE s.court_id = 'seed-court-r3203' AND s.scope = 'DUNNING');

INSERT INTO courts (id, xjustiz_id, name, additional_designation, postal_street, postal_code, city,
    house_street, house_postal_code, house_city, phone, fax, email, web, active)
SELECT * FROM (SELECT 'seed-court-r2602' AS id, 'R2602' AS xjustiz_id, 'Amtsgericht Hagen' AS name,
    'Zentrale Mahnabteilung' AS additional_designation,
    NULL AS postal_street, '58081' AS postal_code, 'Hagen' AS city,
    'Hagener Str. 145' AS house_street, '58099' AS house_postal_code, 'Hagen' AS house_city,
    '02331 967 – 5' AS phone, '02331 967 – 700' AS fax, 'poststelle.zema@ag-hagen.nrw.de' AS email, 'www.ag-hagen.nrw.de' AS web,
    1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM courts c WHERE c.xjustiz_id = 'R2602');

INSERT INTO court_scopes (id, court_id, scope)
SELECT * FROM (SELECT 'seed-scope-r2602' AS id, 'seed-court-r2602' AS court_id, 'DUNNING' AS scope) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM court_scopes s WHERE s.court_id = 'seed-court-r2602' AND s.scope = 'DUNNING');

INSERT INTO courts (id, xjustiz_id, name, additional_designation, postal_street, postal_code, city,
    house_street, house_postal_code, house_city, phone, fax, email, web, active)
SELECT * FROM (SELECT 'seed-court-k1102' AS id, 'K1102' AS xjustiz_id, 'Amtsgericht Hamburg-Altona' AS name,
    'gemeinsames Mahngericht der Länder Hamburg und Mecklenburg-Vorpommern' AS additional_designation,
    NULL AS postal_street, '22747' AS postal_code, 'Hamburg' AS city,
    'Max-Brauer-Allee 89' AS house_street, '22765' AS house_postal_code, 'Hamburg' AS house_city,
    '(040) 42811 – 1462' AS phone, '(040) 4279 – 83264' AS fax, NULL AS email, 'www.justiz.hamburg.de/mahnsachen' AS web,
    1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM courts c WHERE c.xjustiz_id = 'K1102');

INSERT INTO court_scopes (id, court_id, scope)
SELECT * FROM (SELECT 'seed-scope-k1102' AS id, 'seed-court-k1102' AS court_id, 'DUNNING' AS scope) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM court_scopes s WHERE s.court_id = 'seed-court-k1102' AND s.scope = 'DUNNING');

INSERT INTO courts (id, xjustiz_id, name, additional_designation, postal_street, postal_code, city,
    house_street, house_postal_code, house_city, phone, fax, email, web, active)
SELECT * FROM (SELECT 'seed-court-m1307' AS id, 'M1307' AS xjustiz_id, 'Amtsgericht Hünfeld' AS name,
    'Mahnabteilung' AS additional_designation,
    NULL AS postal_street, '36084' AS postal_code, 'Hünfeld' AS city,
    'Hauptstraße 24' AS house_street, '36088' AS house_postal_code, 'Hünfeld' AS house_city,
    '06652/600-01' AS phone, '0611 327618-206' AS fax, NULL AS email, 'www.ag-huenfeld-justiz.hessen.de' AS web,
    1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM courts c WHERE c.xjustiz_id = 'M1307');

INSERT INTO court_scopes (id, court_id, scope)
SELECT * FROM (SELECT 'seed-scope-m1307' AS id, 'seed-court-m1307' AS court_id, 'DUNNING' AS scope) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM court_scopes s WHERE s.court_id = 'seed-court-m1307' AND s.scope = 'DUNNING');

INSERT INTO courts (id, xjustiz_id, name, additional_designation, postal_street, postal_code, city,
    house_street, house_postal_code, house_city, phone, fax, email, web, active)
SELECT * FROM (SELECT 'seed-court-t2213' AS id, 'T2213' AS xjustiz_id, 'Amtsgericht Mayen' AS name,
    'Zentrale Mahnabteilung' AS additional_designation,
    NULL AS postal_street, '56723' AS postal_code, 'Mayen' AS city,
    'St. Veit-Straße 38' AS house_street, '56727' AS house_postal_code, 'Mayen' AS house_city,
    '02651/403-0' AS phone, '02651/403-100' AS fax, 'amtsgericht.mayen@ko.jm.rlp.de' AS email, 'www.agmy.justiz.rlp.de' AS web,
    1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM courts c WHERE c.xjustiz_id = 'T2213');

INSERT INTO court_scopes (id, court_id, scope)
SELECT * FROM (SELECT 'seed-scope-t2213' AS id, 'seed-court-t2213' AS court_id, 'DUNNING' AS scope) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM court_scopes s WHERE s.court_id = 'seed-court-t2213' AND s.scope = 'DUNNING');

INSERT INTO courts (id, xjustiz_id, name, additional_designation, postal_street, postal_code, city,
    house_street, house_postal_code, house_city, phone, fax, email, web, active)
SELECT * FROM (SELECT 'seed-court-x1119' AS id, 'X1119' AS xjustiz_id, 'Amtsgericht Schleswig' AS name,
    'Zentrales Mahngericht' AS additional_designation,
    'Postfach 1170' AS postal_street, '24821' AS postal_code, 'Schleswig' AS city,
    'Lollfuß 78' AS house_street, '24837' AS house_postal_code, 'Schleswig' AS house_city,
    '04621 / 815 – 0' AS phone, '04621 / 815 -333' AS fax, 'Mahnabteilung@AG-Schleswig.LandSH.de' AS email, 'www.mahngericht.schleswig-holstein.de' AS web,
    1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM courts c WHERE c.xjustiz_id = 'X1119');

INSERT INTO court_scopes (id, court_id, scope)
SELECT * FROM (SELECT 'seed-scope-x1119' AS id, 'seed-court-x1119' AS court_id, 'DUNNING' AS scope) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM court_scopes s WHERE s.court_id = 'seed-court-x1119' AND s.scope = 'DUNNING');

INSERT INTO courts (id, xjustiz_id, name, additional_designation, postal_street, postal_code, city,
    house_street, house_postal_code, house_city, phone, fax, email, web, active)
SELECT * FROM (SELECT 'seed-court-b2609' AS id, 'B2609' AS xjustiz_id, 'Amtsgericht Stuttgart' AS name,
    'Zentrales Mahngericht' AS additional_designation,
    NULL AS postal_street, '70154' AS postal_code, 'Stuttgart' AS city,
    'Hauffstraße 5' AS house_street, '70190' AS house_postal_code, 'Stuttgart' AS house_city,
    '0711 / 921 – 3567' AS phone, '0711 / 921 – 3400' AS fax, 'Poststelle@mahngstuttgart.justiz.bwl.de' AS email, 'www.amtsgericht-stuttgart.de' AS web,
    1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM courts c WHERE c.xjustiz_id = 'B2609');

INSERT INTO court_scopes (id, court_id, scope)
SELECT * FROM (SELECT 'seed-scope-b2609' AS id, 'seed-court-b2609' AS court_id, 'DUNNING' AS scope) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM court_scopes s WHERE s.court_id = 'seed-court-b2609' AND s.scope = 'DUNNING');

INSERT INTO courts (id, xjustiz_id, name, additional_designation, postal_street, postal_code, city,
    house_street, house_postal_code, house_city, phone, fax, email, web, active)
SELECT * FROM (SELECT 'seed-court-p2510' AS id, 'P2510' AS xjustiz_id, 'Amtsgericht Uelzen' AS name,
    'Zentrales Mahngericht' AS additional_designation,
    'Rosenmauer 2' AS postal_street, '29525' AS postal_code, 'Uelzen' AS city,
    NULL AS house_street, NULL AS house_postal_code, NULL AS house_city,
    '0581 / 8851 – 0' AS phone, '0581 / 8851 – 200' AS fax, NULL AS email, 'www.amtsgericht-uelzen.niedersachsen.de' AS web,
    1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM courts c WHERE c.xjustiz_id = 'P2510');

INSERT INTO court_scopes (id, court_id, scope)
SELECT * FROM (SELECT 'seed-scope-p2510' AS id, 'seed-court-p2510' AS court_id, 'DUNNING' AS scope) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM court_scopes s WHERE s.court_id = 'seed-court-p2510' AND s.scope = 'DUNNING');

INSERT INTO courts (id, xjustiz_id, name, additional_designation, postal_street, postal_code, city,
    house_street, house_postal_code, house_city, phone, fax, email, web, active)
SELECT * FROM (SELECT 'seed-court-f1102' AS id, 'F1102' AS xjustiz_id, 'Amtsgericht Wedding' AS name,
    'Zentrales Mahngericht Berlin-Brandenburg' AS additional_designation,
    NULL AS postal_street, '13343' AS postal_code, 'Berlin' AS city,
    'Schönstedtstr. 5' AS house_street, '13357' AS house_postal_code, 'Berlin' AS house_city,
    '030 90156 – 0' AS phone, '030 90156 – 203/233/402' AS fax, NULL AS email, 'www.berlin.de/ag-wedding' AS web,
    1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM courts c WHERE c.xjustiz_id = 'F1102');

INSERT INTO court_scopes (id, court_id, scope)
SELECT * FROM (SELECT 'seed-scope-f1102' AS id, 'seed-court-f1102' AS court_id, 'DUNNING' AS scope) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM court_scopes s WHERE s.court_id = 'seed-court-f1102' AND s.scope = 'DUNNING');

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.17') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.17';
commit;
