-- Die Gebührenpositionen der Zwangsvollstreckung.
--
-- Dieselbe Bauart wie V3_6_0_22: die Wertgebührentabelle liefert die 1,0-Gebühr, das Verzeichnis
-- sagt, welcher Satz daraus wird. Für die Zwangsvollstreckung sind das:
--
--   Nr. 3309 VV RVG   0,3 Verfahrensgebühr für die Vollstreckung - je Vollstreckungsmaßnahme
--   Nr. 3310 VV RVG   0,3 Terminsgebühr, wenn ein Termin stattfindet (Nr. 3310 Abs. 1 Nr. 1 und 2)
--   Nr. 2111 KV GKG   20,00 Euro für den Antrag auf Erlass eines Pfändungs- und
--                     Überweisungsbeschlusses - ein Festbetrag ohne Wertbezug
--
-- Die Post- und Telekommunikationspauschale (Nr. 7002) und die Umsatzsteuer (Nr. 7008) stehen
-- bereits aus V3_6_0_22 bereit; sie gelten in jeder Angelegenheit und werden nicht doppelt geführt.
--
-- Nicht enthalten sind die Kosten des Gerichtsvollziehers nach dem GvKostG. Sie hängen an der
-- einzelnen Amtshandlung und nicht am Gegenstandswert; ihre Tabelle ist Gegenstand von Aufgabe 0.3.
-- Bis dahin trägt der Vorschlag sie als Position ohne Betrag, den die Kanzlei einträgt - eine Zahl
-- zu erfinden wäre schlechter als keine.
--
-- Stand: KostBRÄG 2025, in Kraft seit 1. Juni 2025. Quellen: Anlage 1 zum RVG, Anlage 1 zum GKG.

INSERT INTO fee_items (id, item_key, name, legal_basis, scale_key, rate, percentage,
    minimum_amount, maximum_amount, max_rate, valid_from, active, notes)
SELECT * FROM (SELECT 'seed-feeitem-rvg-vv-3309-2025' AS id, 'RVG_VV_3309' AS item_key, 'Verfahrensgebühr Zwangsvollstreckung' AS name, 'Nr. 3309 VV RVG' AS legal_basis,
    'RVG_13' AS scale_key, 0.30 AS rate, NULL AS percentage,
    NULL AS minimum_amount, NULL AS maximum_amount, NULL AS max_rate,
    '2025-06-01' AS valid_from, 1 AS active, 'Vertretung in der Zwangsvollstreckung. Die Gebühr entsteht für jede Vollstreckungsmaßnahme gesondert.' AS notes) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM fee_items f WHERE f.id = 'seed-feeitem-rvg-vv-3309-2025');

INSERT INTO fee_items (id, item_key, name, legal_basis, scale_key, rate, percentage,
    minimum_amount, maximum_amount, max_rate, valid_from, active, notes)
SELECT * FROM (SELECT 'seed-feeitem-rvg-vv-3310-2025' AS id, 'RVG_VV_3310' AS item_key, 'Terminsgebühr Zwangsvollstreckung' AS name, 'Nr. 3310 VV RVG' AS legal_basis,
    'RVG_13' AS scale_key, 0.30 AS rate, NULL AS percentage,
    NULL AS minimum_amount, NULL AS maximum_amount, NULL AS max_rate,
    '2025-06-01' AS valid_from, 1 AS active, 'Nur wenn ein Termin stattfindet - etwa die Teilnahme an der Abnahme der Vermögensauskunft oder an einem Verteilungstermin.' AS notes) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM fee_items f WHERE f.id = 'seed-feeitem-rvg-vv-3310-2025');

INSERT INTO fee_items (id, item_key, name, legal_basis, scale_key, rate, percentage,
    minimum_amount, maximum_amount, max_rate, valid_from, active, notes)
SELECT * FROM (SELECT 'seed-feeitem-gkg-kv-2111-2025' AS id, 'GKG_KV_2111' AS item_key, 'Gerichtsgebühr Pfändungs- und Überweisungsbeschluss' AS name, 'Nr. 2111 KV GKG' AS legal_basis,
    NULL AS scale_key, NULL AS rate, NULL AS percentage,
    20.00 AS minimum_amount, 20.00 AS maximum_amount, NULL AS max_rate,
    '2025-06-01' AS valid_from, 1 AS active, 'Festbetrag ohne Wertbezug: Verfahren über Anträge auf Vollstreckungsmaßnahmen vor dem Vollstreckungsgericht. Mindest- und Höchstbetrag sind derselbe Betrag, weil die Gebühr nicht vom Wert abhängt.' AS notes) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM fee_items f WHERE f.id = 'seed-feeitem-gkg-kv-2111-2025');

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.46') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.46';
commit;
