-- Die Gebührensätze, die auf den Wertgebührentabellen aufsetzen.
--
-- Die Tabellen aus V3_6_0_21 liefern die 1,0-Gebuehr zu einem Wert. Was daraus im einzelnen
-- Verfahren wird, bestimmen die Verzeichnisse: eine 1,0 Verfahrensgebuehr nach Nr. 3305 VV RVG,
-- eine 0,5 nach Nr. 3308, eine 0,5 Gerichtsgebuehr nach Nr. 1100 KV GKG. Diese Saetze stehen
-- ebenfalls im Gesetz und aendern sich mit ihm, weshalb sie hier als Daten liegen und nicht im Code.
--
-- Die Positionen haben unterschiedliche Gestalt, und die Spalten bilden das ab:
--   rate            ein Gebuehrensatz auf die Wertgebuehr (1,0 / 0,5 / 0,3)
--   percentage      ein Prozentsatz auf die Gebuehren (Nr. 7002: 20 %)
--   minimum_amount  ein Mindestbetrag (Nr. 1100 KV GKG: 38,00 Euro)
--   maximum_amount  ein Hoechstbetrag (Nr. 7002: 20,00 Euro)
--   max_rate        eine Obergrenze in Gebuehrensaetzen (Nr. 1008 Abs. 3: hoechstens 2,0;
--                   Vorbem. 3 Abs. 4: Anrechnung hoechstens 0,75)
-- Nicht jede Position nutzt jede Spalte; leer heisst, dass die Vorschrift dazu nichts sagt.
--
-- Gesaet ist der Stand nach dem KostBRÄG 2025 (in Kraft seit 1. Juni 2025). Die Werte stammen aus
-- den amtlichen Texten: Anlage 1 zum RVG (Vergütungsverzeichnis) und Anlage 1 zum GKG
-- (Kostenverzeichnis) auf gesetze-im-internet.de.

CREATE TABLE fee_items (
`id` VARCHAR(50) BINARY NOT NULL,
`item_key` VARCHAR(50) BINARY NOT NULL,        -- z. B. RVG_VV_3305, GKG_KV_1100
`name` VARCHAR(250) BINARY NOT NULL,
`legal_basis` VARCHAR(250) BINARY DEFAULT NULL,
`scale_key` VARCHAR(50) BINARY DEFAULT NULL,   -- welche Wertgebuehrentabelle zugrunde liegt
`rate` DECIMAL(6,2) DEFAULT NULL,
`percentage` DECIMAL(6,2) DEFAULT NULL,
`minimum_amount` DECIMAL(10,2) DEFAULT NULL,
`maximum_amount` DECIMAL(10,2) DEFAULT NULL,
`max_rate` DECIMAL(6,2) DEFAULT NULL,
`valid_from` DATE DEFAULT NULL,
`valid_to` DATE DEFAULT NULL,
`active` TINYINT(1) DEFAULT 1 NOT NULL,
`notes` VARCHAR(1000) BINARY DEFAULT NULL,
CONSTRAINT `pk_fee_items` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table fee_items add index `IDX_FEEITEMS_KEY` (item_key);

INSERT INTO fee_items (id, item_key, name, legal_basis, scale_key, rate, percentage,
    minimum_amount, maximum_amount, max_rate, valid_from, active, notes)
SELECT * FROM (SELECT 'seed-feeitem-rvg-vv-3305-2025' AS id, 'RVG_VV_3305' AS item_key, 'Verfahrensgebühr Mahnverfahren' AS name, 'Nr. 3305 VV RVG' AS legal_basis,
    'RVG_13' AS scale_key, 1.00 AS rate, NULL AS percentage,
    NULL AS minimum_amount, NULL AS maximum_amount, NULL AS max_rate,
    '2025-06-01' AS valid_from, 1 AS active, 'Vertretung des Antragstellers im Mahnverfahren. Wird auf die Verfahrensgebühr eines nachfolgenden Rechtsstreits angerechnet.' AS notes) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM fee_items f WHERE f.id = 'seed-feeitem-rvg-vv-3305-2025');

INSERT INTO fee_items (id, item_key, name, legal_basis, scale_key, rate, percentage,
    minimum_amount, maximum_amount, max_rate, valid_from, active, notes)
SELECT * FROM (SELECT 'seed-feeitem-rvg-vv-3308-2025' AS id, 'RVG_VV_3308' AS item_key, 'Verfahrensgebühr Vollstreckungsbescheid' AS name, 'Nr. 3308 VV RVG' AS legal_basis,
    'RVG_13' AS scale_key, 0.50 AS rate, NULL AS percentage,
    NULL AS minimum_amount, NULL AS maximum_amount, NULL AS max_rate,
    '2025-06-01' AS valid_from, 1 AS active, 'Entsteht neben Nr. 3305 nur, wenn innerhalb der Widerspruchsfrist kein Widerspruch erhoben oder er nach § 703a Abs. 2 Nr. 4 ZPO beschränkt wurde.' AS notes) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM fee_items f WHERE f.id = 'seed-feeitem-rvg-vv-3308-2025');

INSERT INTO fee_items (id, item_key, name, legal_basis, scale_key, rate, percentage,
    minimum_amount, maximum_amount, max_rate, valid_from, active, notes)
SELECT * FROM (SELECT 'seed-feeitem-rvg-vv-1008-2025' AS id, 'RVG_VV_1008' AS item_key, 'Erhöhung bei mehreren Auftraggebern' AS name, 'Nr. 1008 VV RVG' AS legal_basis,
    'RVG_13' AS scale_key, 0.30 AS rate, NULL AS percentage,
    NULL AS minimum_amount, NULL AS maximum_amount, 2.00 AS max_rate,
    '2025-06-01' AS valid_from, 1 AS active, 'Je weiterer Person 0,3 auf die Verfahrens- oder Geschäftsgebühr. Nur soweit der Gegenstand derselbe ist; berechnet nach dem Betrag, an dem die Personen gemeinschaftlich beteiligt sind. Mehrere Erhöhungen dürfen 2,0 nicht übersteigen (Anm. Abs. 3).' AS notes) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM fee_items f WHERE f.id = 'seed-feeitem-rvg-vv-1008-2025');

INSERT INTO fee_items (id, item_key, name, legal_basis, scale_key, rate, percentage,
    minimum_amount, maximum_amount, max_rate, valid_from, active, notes)
SELECT * FROM (SELECT 'seed-feeitem-rvg-vv-3305-anrechnung-2025' AS id, 'RVG_VV_3305_ANRECHNUNG' AS item_key, 'Anrechnung der Geschäftsgebühr' AS name, 'Vorbem. 3 Abs. 4 VV RVG' AS legal_basis,
    'RVG_13' AS scale_key, NULL AS rate, 50.00 AS percentage,
    NULL AS minimum_amount, NULL AS maximum_amount, 0.75 AS max_rate,
    '2025-06-01' AS valid_from, 1 AS active, 'Eine Geschäftsgebühr nach Teil 2 wegen desselben Gegenstands wird zur Hälfte, bei Wertgebühren höchstens mit 0,75, auf die Verfahrensgebühr angerechnet. Sind mehrere entstanden, ist die zuletzt entstandene maßgebend; die Anrechnung erfolgt nach dem Wert, der auch Gegenstand des gerichtlichen Verfahrens ist.' AS notes) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM fee_items f WHERE f.id = 'seed-feeitem-rvg-vv-3305-anrechnung-2025');

INSERT INTO fee_items (id, item_key, name, legal_basis, scale_key, rate, percentage,
    minimum_amount, maximum_amount, max_rate, valid_from, active, notes)
SELECT * FROM (SELECT 'seed-feeitem-rvg-vv-7002-2025' AS id, 'RVG_VV_7002' AS item_key, 'Post- und Telekommunikationspauschale' AS name, 'Nr. 7002 VV RVG' AS legal_basis,
    NULL AS scale_key, NULL AS rate, 20.00 AS percentage,
    NULL AS minimum_amount, 20.00 AS maximum_amount, NULL AS max_rate,
    '2025-06-01' AS valid_from, 1 AS active, '20 % der Gebühren, höchstens 20,00 Euro. Kann in jeder Angelegenheit anstelle der tatsächlichen Auslagen nach Nr. 7001 gefordert werden.' AS notes) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM fee_items f WHERE f.id = 'seed-feeitem-rvg-vv-7002-2025');

INSERT INTO fee_items (id, item_key, name, legal_basis, scale_key, rate, percentage,
    minimum_amount, maximum_amount, max_rate, valid_from, active, notes)
SELECT * FROM (SELECT 'seed-feeitem-gkg-kv-1100-2025' AS id, 'GKG_KV_1100' AS item_key, 'Gerichtsgebühr Mahnverfahren' AS name, 'Nr. 1100 KV GKG' AS legal_basis,
    'GKG_34' AS scale_key, 0.50 AS rate, NULL AS percentage,
    38.00 AS minimum_amount, NULL AS maximum_amount, NULL AS max_rate,
    '2025-06-01' AS valid_from, 1 AS active, 'Verfahren über den Antrag auf Erlass eines Mahnbescheids oder eines Europäischen Zahlungsbefehls. Mindestbetrag seit 1. Juni 2025: 38,00 Euro.' AS notes) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM fee_items f WHERE f.id = 'seed-feeitem-gkg-kv-1100-2025');

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.22') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.22';
commit;
