-- Default catalogue of contact relationship types (Kontaktbeziehungsarten).
--
-- The relationship types ship with the product so that the feature is usable the moment it is
-- installed: a firm should be able to record that one contact is the mother of another, or the
-- Betreuer, or the Versicherer, without first having to invent and type a catalogue of its own.
-- What is seeded here is the wording a German law firm would expect to find, ordered by the five
-- categories the administration groups them into.
--
-- The ids are fixed rather than generated, so re-running this seed cannot create a second copy of
-- a type. Every row is inserted only if its id is not present yet, which means an installation
-- that has already renamed, recoloured, reordered or deactivated a type keeps its own version -
-- nothing here overwrites an existing row.
--
-- The labels are German master data, not identifiers: they are what a lawyer reads in the contact
-- ("ist Mutter von", "wird betreut von"), and a firm is free to rename them. Nothing in the code
-- matches on a label or on a name; the relationships reference the id.
--
-- label_from is what the contact on the from side is to the other one, label_to is the same
-- relationship read from the other end. For symmetric types - the ones that read the same in both
-- directions, such as Ehepartner or Nachbar - label_to is identical to label_from, as the service
-- expects.
--
-- color is 0 for every seeded row: the client then falls back to its default colour, so the
-- catalogue looks consistent until a firm decides to mark individual types.

-- Familie

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-elternteil-kind' AS id, 'Elternteil – Kind' AS name,
    'ist Elternteil von' AS label_from, 'ist Kind von' AS label_to,
    0 AS symmetric, 'Familie' AS category, 0 AS color, 100 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-elternteil-kind');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-mutter-kind' AS id, 'Mutter – Kind' AS name,
    'ist Mutter von' AS label_from, 'ist Kind von' AS label_to,
    0 AS symmetric, 'Familie' AS category, 0 AS color, 110 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-mutter-kind');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-vater-kind' AS id, 'Vater – Kind' AS name,
    'ist Vater von' AS label_from, 'ist Kind von' AS label_to,
    0 AS symmetric, 'Familie' AS category, 0 AS color, 120 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-vater-kind');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-ehepartner' AS id, 'Ehepartner' AS name,
    'ist Ehepartner von' AS label_from, 'ist Ehepartner von' AS label_to,
    1 AS symmetric, 'Familie' AS category, 0 AS color, 130 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-ehepartner');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-eingetragener-lebenspartner' AS id, 'eingetragener Lebenspartner' AS name,
    'ist eingetragener Lebenspartner von' AS label_from, 'ist eingetragener Lebenspartner von' AS label_to,
    1 AS symmetric, 'Familie' AS category, 0 AS color, 140 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-eingetragener-lebenspartner');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-lebensgefaehrte' AS id, 'Lebensgefährte' AS name,
    'ist Lebensgefährte von' AS label_from, 'ist Lebensgefährte von' AS label_to,
    1 AS symmetric, 'Familie' AS category, 0 AS color, 150 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-lebensgefaehrte');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-geschiedener-ehepartner' AS id, 'geschiedener Ehepartner' AS name,
    'ist geschiedener Ehepartner von' AS label_from, 'ist geschiedener Ehepartner von' AS label_to,
    1 AS symmetric, 'Familie' AS category, 0 AS color, 160 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-geschiedener-ehepartner');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-geschwister' AS id, 'Geschwister' AS name,
    'ist Geschwister von' AS label_from, 'ist Geschwister von' AS label_to,
    1 AS symmetric, 'Familie' AS category, 0 AS color, 170 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-geschwister');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-halbgeschwister' AS id, 'Halbgeschwister' AS name,
    'ist Halbgeschwister von' AS label_from, 'ist Halbgeschwister von' AS label_to,
    1 AS symmetric, 'Familie' AS category, 0 AS color, 180 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-halbgeschwister');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-grosselternteil-enkel' AS id, 'Großelternteil – Enkel' AS name,
    'ist Großelternteil von' AS label_from, 'ist Enkel von' AS label_to,
    0 AS symmetric, 'Familie' AS category, 0 AS color, 190 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-grosselternteil-enkel');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-grossmutter-enkel' AS id, 'Großmutter – Enkel' AS name,
    'ist Großmutter von' AS label_from, 'ist Enkel von' AS label_to,
    0 AS symmetric, 'Familie' AS category, 0 AS color, 200 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-grossmutter-enkel');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-grossvater-enkel' AS id, 'Großvater – Enkel' AS name,
    'ist Großvater von' AS label_from, 'ist Enkel von' AS label_to,
    0 AS symmetric, 'Familie' AS category, 0 AS color, 210 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-grossvater-enkel');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-onkel-tante-nichte-neffe' AS id, 'Onkel/Tante – Nichte/Neffe' AS name,
    'ist Onkel/Tante von' AS label_from, 'ist Nichte/Neffe von' AS label_to,
    0 AS symmetric, 'Familie' AS category, 0 AS color, 220 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-onkel-tante-nichte-neffe');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-cousin-cousine' AS id, 'Cousin/Cousine' AS name,
    'ist Cousin/Cousine von' AS label_from, 'ist Cousin/Cousine von' AS label_to,
    1 AS symmetric, 'Familie' AS category, 0 AS color, 230 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-cousin-cousine');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-stiefelternteil-stiefkind' AS id, 'Stiefelternteil – Stiefkind' AS name,
    'ist Stiefelternteil von' AS label_from, 'ist Stiefkind von' AS label_to,
    0 AS symmetric, 'Familie' AS category, 0 AS color, 240 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-stiefelternteil-stiefkind');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-adoptivelternteil-adoptivkind' AS id, 'Adoptivelternteil – Adoptivkind' AS name,
    'ist Adoptivelternteil von' AS label_from, 'ist Adoptivkind von' AS label_to,
    0 AS symmetric, 'Familie' AS category, 0 AS color, 250 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-adoptivelternteil-adoptivkind');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-pflegeelternteil-pflegekind' AS id, 'Pflegeelternteil – Pflegekind' AS name,
    'ist Pflegeelternteil von' AS label_from, 'ist Pflegekind von' AS label_to,
    0 AS symmetric, 'Familie' AS category, 0 AS color, 260 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-pflegeelternteil-pflegekind');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-schwiegerelternteil-schwiegerkind' AS id, 'Schwiegerelternteil – Schwiegerkind' AS name,
    'ist Schwiegerelternteil von' AS label_from, 'ist Schwiegerkind von' AS label_to,
    0 AS symmetric, 'Familie' AS category, 0 AS color, 270 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-schwiegerelternteil-schwiegerkind');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-schwager-schwaegerin' AS id, 'Schwager/Schwägerin' AS name,
    'ist Schwager/Schwägerin von' AS label_from, 'ist Schwager/Schwägerin von' AS label_to,
    1 AS symmetric, 'Familie' AS category, 0 AS color, 280 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-schwager-schwaegerin');

-- Vertretung

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-gesetzlicher-vertreter' AS id, 'gesetzlicher Vertreter – Vertretener' AS name,
    'ist gesetzlicher Vertreter von' AS label_from, 'wird gesetzlich vertreten durch' AS label_to,
    0 AS symmetric, 'Vertretung' AS category, 0 AS color, 200 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-gesetzlicher-vertreter');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-vormund-muendel' AS id, 'Vormund – Mündel' AS name,
    'ist Vormund von' AS label_from, 'steht unter der Vormundschaft von' AS label_to,
    0 AS symmetric, 'Vertretung' AS category, 0 AS color, 210 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-vormund-muendel');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-betreuer-betreuter' AS id, 'Betreuer – Betreuter' AS name,
    'ist Betreuer von' AS label_from, 'wird betreut von' AS label_to,
    0 AS symmetric, 'Vertretung' AS category, 0 AS color, 220 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-betreuer-betreuter');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-bevollmaechtigter-vollmachtgeber' AS id, 'Bevollmächtigter – Vollmachtgeber' AS name,
    'ist Bevollmächtigter von' AS label_from, 'hat Vollmacht erteilt an' AS label_to,
    0 AS symmetric, 'Vertretung' AS category, 0 AS color, 230 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-bevollmaechtigter-vollmachtgeber');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-verfahrensbeistand-kind' AS id, 'Verfahrensbeistand – vertretenes Kind' AS name,
    'ist Verfahrensbeistand von' AS label_from, 'wird im Verfahren vertreten durch' AS label_to,
    0 AS symmetric, 'Vertretung' AS category, 0 AS color, 240 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-verfahrensbeistand-kind');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-testamentsvollstrecker-erblasser' AS id, 'Testamentsvollstrecker – Erblasser' AS name,
    'ist Testamentsvollstrecker von' AS label_from, 'hat zum Testamentsvollstrecker bestellt' AS label_to,
    0 AS symmetric, 'Vertretung' AS category, 0 AS color, 250 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-testamentsvollstrecker-erblasser');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-erbe-erblasser' AS id, 'Erbe – Erblasser' AS name,
    'ist Erbe von' AS label_from, 'wird beerbt von' AS label_to,
    0 AS symmetric, 'Vertretung' AS category, 0 AS color, 260 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-erbe-erblasser');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-nachlasspfleger-nachlass' AS id, 'Nachlasspfleger – Nachlass des Erblassers' AS name,
    'ist Nachlasspfleger für den Nachlass von' AS label_from, 'dessen Nachlass wird verwaltet von' AS label_to,
    0 AS symmetric, 'Vertretung' AS category, 0 AS color, 270 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-nachlasspfleger-nachlass');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-insolvenzverwalter-schuldner' AS id, 'Insolvenzverwalter – Insolvenzschuldner' AS name,
    'ist Insolvenzverwalter von' AS label_from, 'steht unter der Insolvenzverwaltung von' AS label_to,
    0 AS symmetric, 'Vertretung' AS category, 0 AS color, 280 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-insolvenzverwalter-schuldner');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-rechtsanwalt-mandant' AS id, 'Rechtsanwalt – Mandant' AS name,
    'ist Rechtsanwalt von' AS label_from, 'ist Mandant von' AS label_to,
    0 AS symmetric, 'Vertretung' AS category, 0 AS color, 290 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-rechtsanwalt-mandant');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-steuerberater-mandant' AS id, 'Steuerberater – Mandant' AS name,
    'ist Steuerberater von' AS label_from, 'ist Mandant von' AS label_to,
    0 AS symmetric, 'Vertretung' AS category, 0 AS color, 300 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-steuerberater-mandant');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-wirtschaftspruefer-mandant' AS id, 'Wirtschaftsprüfer – Mandant' AS name,
    'ist Wirtschaftsprüfer von' AS label_from, 'ist Mandant von' AS label_to,
    0 AS symmetric, 'Vertretung' AS category, 0 AS color, 310 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-wirtschaftspruefer-mandant');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-notar-beteiligter' AS id, 'Notar – Beteiligter' AS name,
    'ist beurkundender Notar für' AS label_from, 'ist Beteiligter der Beurkundung von' AS label_to,
    0 AS symmetric, 'Vertretung' AS category, 0 AS color, 320 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-notar-beteiligter');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-dolmetscher-beteiligter' AS id, 'Dolmetscher – Beteiligter' AS name,
    'ist Dolmetscher für' AS label_from, 'nimmt Dolmetscherdienste in Anspruch von' AS label_to,
    0 AS symmetric, 'Vertretung' AS category, 0 AS color, 330 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-dolmetscher-beteiligter');

-- Unternehmen

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-geschaeftsfuehrer' AS id, 'Geschäftsführer – Gesellschaft' AS name,
    'ist Geschäftsführer von' AS label_from, 'hat als Geschäftsführer' AS label_to,
    0 AS symmetric, 'Unternehmen' AS category, 0 AS color, 300 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-geschaeftsfuehrer');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-vorstand' AS id, 'Vorstand – Gesellschaft' AS name,
    'ist Vorstand von' AS label_from, 'hat als Vorstand' AS label_to,
    0 AS symmetric, 'Unternehmen' AS category, 0 AS color, 310 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-vorstand');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-aufsichtsratsmitglied' AS id, 'Aufsichtsratsmitglied – Gesellschaft' AS name,
    'ist Aufsichtsratsmitglied von' AS label_from, 'hat als Aufsichtsratsmitglied' AS label_to,
    0 AS symmetric, 'Unternehmen' AS category, 0 AS color, 320 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-aufsichtsratsmitglied');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-gesellschafter' AS id, 'Gesellschafter – Gesellschaft' AS name,
    'ist Gesellschafter von' AS label_from, 'hat als Gesellschafter' AS label_to,
    0 AS symmetric, 'Unternehmen' AS category, 0 AS color, 330 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-gesellschafter');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-kommanditist' AS id, 'Kommanditist – Gesellschaft' AS name,
    'ist Kommanditist von' AS label_from, 'hat als Kommanditisten' AS label_to,
    0 AS symmetric, 'Unternehmen' AS category, 0 AS color, 340 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-kommanditist');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-komplementaer' AS id, 'Komplementär – Gesellschaft' AS name,
    'ist Komplementär von' AS label_from, 'hat als Komplementär' AS label_to,
    0 AS symmetric, 'Unternehmen' AS category, 0 AS color, 350 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-komplementaer');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-prokurist' AS id, 'Prokurist – Unternehmen' AS name,
    'ist Prokurist von' AS label_from, 'hat Prokura erteilt an' AS label_to,
    0 AS symmetric, 'Unternehmen' AS category, 0 AS color, 360 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-prokurist');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-inhaber' AS id, 'Inhaber – Unternehmen' AS name,
    'ist Inhaber von' AS label_from, 'hat als Inhaber' AS label_to,
    0 AS symmetric, 'Unternehmen' AS category, 0 AS color, 370 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-inhaber');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-ansprechpartner' AS id, 'Ansprechpartner – Unternehmen' AS name,
    'ist Ansprechpartner bei' AS label_from, 'hat als Ansprechpartner' AS label_to,
    0 AS symmetric, 'Unternehmen' AS category, 0 AS color, 380 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-ansprechpartner');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-arbeitgeber-arbeitnehmer' AS id, 'Arbeitgeber – Arbeitnehmer' AS name,
    'ist Arbeitgeber von' AS label_from, 'ist Arbeitnehmer von' AS label_to,
    0 AS symmetric, 'Unternehmen' AS category, 0 AS color, 390 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-arbeitgeber-arbeitnehmer');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-muttergesellschaft-tochtergesellschaft' AS id, 'Muttergesellschaft – Tochtergesellschaft' AS name,
    'ist Muttergesellschaft von' AS label_from, 'ist Tochtergesellschaft von' AS label_to,
    0 AS symmetric, 'Unternehmen' AS category, 0 AS color, 400 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-muttergesellschaft-tochtergesellschaft');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-verbundenes-unternehmen' AS id, 'verbundenes Unternehmen' AS name,
    'ist verbundenes Unternehmen von' AS label_from, 'ist verbundenes Unternehmen von' AS label_to,
    1 AS symmetric, 'Unternehmen' AS category, 0 AS color, 410 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-verbundenes-unternehmen');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-zweigstelle-hauptniederlassung' AS id, 'Zweigstelle – Hauptniederlassung' AS name,
    'ist Zweigstelle von' AS label_from, 'ist Hauptniederlassung von' AS label_to,
    0 AS symmetric, 'Unternehmen' AS category, 0 AS color, 420 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-zweigstelle-hauptniederlassung');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-rechtsvorgaenger-rechtsnachfolger' AS id, 'Rechtsvorgänger – Rechtsnachfolger' AS name,
    'ist Rechtsvorgänger von' AS label_from, 'ist Rechtsnachfolger von' AS label_to,
    0 AS symmetric, 'Unternehmen' AS category, 0 AS color, 430 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-rechtsvorgaenger-rechtsnachfolger');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-betriebsratsmitglied' AS id, 'Betriebsratsmitglied – Unternehmen' AS name,
    'ist Betriebsratsmitglied bei' AS label_from, 'hat als Betriebsratsmitglied' AS label_to,
    0 AS symmetric, 'Unternehmen' AS category, 0 AS color, 440 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-betriebsratsmitglied');

-- Vertrag

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-versicherer-versicherungsnehmer' AS id, 'Versicherer – Versicherungsnehmer' AS name,
    'ist Versicherer von' AS label_from, 'ist Versicherungsnehmer bei' AS label_to,
    0 AS symmetric, 'Vertrag' AS category, 0 AS color, 400 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-versicherer-versicherungsnehmer');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-versicherungsnehmer-versicherte-person' AS id, 'Versicherungsnehmer – versicherte Person' AS name,
    'ist Versicherungsnehmer für' AS label_from, 'ist versicherte Person von' AS label_to,
    0 AS symmetric, 'Vertrag' AS category, 0 AS color, 410 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-versicherungsnehmer-versicherte-person');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-vermieter-mieter' AS id, 'Vermieter – Mieter' AS name,
    'ist Vermieter von' AS label_from, 'ist Mieter von' AS label_to,
    0 AS symmetric, 'Vertrag' AS category, 0 AS color, 420 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-vermieter-mieter');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-verpaechter-paechter' AS id, 'Verpächter – Pächter' AS name,
    'ist Verpächter von' AS label_from, 'ist Pächter von' AS label_to,
    0 AS symmetric, 'Vertrag' AS category, 0 AS color, 430 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-verpaechter-paechter');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-verwalter-eigentuemer' AS id, 'Verwalter – Eigentümer' AS name,
    'ist Verwalter für' AS label_from, 'hat als Verwalter' AS label_to,
    0 AS symmetric, 'Vertrag' AS category, 0 AS color, 440 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-verwalter-eigentuemer');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-darlehensgeber-darlehensnehmer' AS id, 'Darlehensgeber – Darlehensnehmer' AS name,
    'ist Darlehensgeber von' AS label_from, 'ist Darlehensnehmer von' AS label_to,
    0 AS symmetric, 'Vertrag' AS category, 0 AS color, 450 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-darlehensgeber-darlehensnehmer');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-buerge-hauptschuldner' AS id, 'Bürge – Hauptschuldner' AS name,
    'ist Bürge für' AS label_from, 'ist Hauptschuldner der Bürgschaft von' AS label_to,
    0 AS symmetric, 'Vertrag' AS category, 0 AS color, 460 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-buerge-hauptschuldner');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-glaeubiger-schuldner' AS id, 'Gläubiger – Schuldner' AS name,
    'ist Gläubiger von' AS label_from, 'ist Schuldner von' AS label_to,
    0 AS symmetric, 'Vertrag' AS category, 0 AS color, 470 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-glaeubiger-schuldner');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-vertragspartner' AS id, 'Vertragspartner' AS name,
    'ist Vertragspartner von' AS label_from, 'ist Vertragspartner von' AS label_to,
    1 AS symmetric, 'Vertrag' AS category, 0 AS color, 480 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-vertragspartner');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-auftraggeber-auftragnehmer' AS id, 'Auftraggeber – Auftragnehmer' AS name,
    'ist Auftraggeber von' AS label_from, 'ist Auftragnehmer von' AS label_to,
    0 AS symmetric, 'Vertrag' AS category, 0 AS color, 490 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-auftraggeber-auftragnehmer');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-kaeufer-verkaeufer' AS id, 'Käufer – Verkäufer' AS name,
    'hat gekauft von' AS label_from, 'hat verkauft an' AS label_to,
    0 AS symmetric, 'Vertrag' AS category, 0 AS color, 500 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-kaeufer-verkaeufer');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-zessionar-zedent' AS id, 'Zessionar – Zedent' AS name,
    'ist Zessionar von' AS label_from, 'ist Zedent von' AS label_to,
    0 AS symmetric, 'Vertrag' AS category, 0 AS color, 510 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-zessionar-zedent');

-- Sonstige

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-nachbar' AS id, 'Nachbar' AS name,
    'ist Nachbar von' AS label_from, 'ist Nachbar von' AS label_to,
    1 AS symmetric, 'Sonstige' AS category, 0 AS color, 500 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-nachbar');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-kontaktperson' AS id, 'Kontaktperson' AS name,
    'ist Kontaktperson für' AS label_from, 'hat als Kontaktperson' AS label_to,
    0 AS symmetric, 'Sonstige' AS category, 0 AS color, 510 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-kontaktperson');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-hausarzt-patient' AS id, 'Hausarzt – Patient' AS name,
    'ist Hausarzt von' AS label_from, 'ist Patient von' AS label_to,
    0 AS symmetric, 'Sonstige' AS category, 0 AS color, 520 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-hausarzt-patient');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-sachverstaendiger-auftraggeber' AS id, 'Sachverständiger – Auftraggeber' AS name,
    'ist Sachverständiger für' AS label_from, 'hat als Sachverständigen beauftragt' AS label_to,
    0 AS symmetric, 'Sonstige' AS category, 0 AS color, 530 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-sachverstaendiger-auftraggeber');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-unfallbeteiligter' AS id, 'Unfallbeteiligter' AS name,
    'ist am selben Unfall beteiligt wie' AS label_from, 'ist am selben Unfall beteiligt wie' AS label_to,
    1 AS symmetric, 'Sonstige' AS category, 0 AS color, 540 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-unfallbeteiligter');

INSERT INTO contact_relation_types (id, name, label_from, label_to, symmetric, category,
    color, sequence_no, active)
SELECT * FROM (SELECT 'seed-crt-zeuge-beteiligter' AS id, 'Zeuge – Beteiligter' AS name,
    'ist Zeuge für' AS label_from, 'hat als Zeugen benannt' AS label_to,
    0 AS symmetric, 'Sonstige' AS category, 0 AS color, 550 AS sequence_no, 1 AS active) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM contact_relation_types WHERE id = 'seed-crt-zeuge-beteiligter');

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.29') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.29';
commit;
