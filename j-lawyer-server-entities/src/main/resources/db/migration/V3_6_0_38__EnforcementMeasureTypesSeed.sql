-- Der Katalog der Vollstreckungsarten, wie ihn § 802a ZPO und die ZVFV vorzeichnen.
--
-- Feste IDs statt erzeugter: ein zweiter Lauf des Seeds kann so keine zweite Kopie anlegen, und
-- eine Kanzlei, die einen Eintrag angepasst oder abgeschaltet hat, behält ihre Fassung.
--
-- Die Reihenfolge ist ungefähr die, in der die Maßnahmen üblicherweise ergriffen werden: erst die
-- Androhung, dann der Gerichtsvollzieher mit seinen Optionen nach § 802a Abs. 2 ZPO, dann die
-- Forderungspfändung, dann die Verwertung von Grundbesitz und zuletzt die Auskünfte.
--
-- form_key benennt die Anlage der ZVFV, nicht eine konkrete Vorlage. Vorlagen tragen
-- Gültigkeitszeiträume, und maßgeblich ist die zum Zeitpunkt der Maßnahme geltende Fassung.
-- Wo nichts steht, gibt es kein vorgeschriebenes Formular: eine Vollstreckungsandrohung und die
-- meisten Auskunftsersuchen sind gewöhnliche Schreiben.

INSERT INTO enforcement_measure_types (id, name, sequence_number, addressee_type, form_key,
    requires_title, involves_third_party, follow_up_days, active, description)
SELECT * FROM (SELECT
 'seed-measure-warning' AS id,
 'Vollstreckungsandrohung' AS name,
 10 AS sequence_number,
 'OTHER' AS addressee_type,
 NULL AS form_key,
 0 AS requires_title,
 0 AS involves_third_party,
 14 AS follow_up_days,
 1 AS active,
 'Letzte Ankündigung vor der Zwangsvollstreckung. Sie setzt keinen Titel voraus, weil sie vor der Vollstreckung steht, und ist kein Formular, sondern ein Schreiben.' AS description
) AS tmp WHERE NOT EXISTS (SELECT 1 FROM enforcement_measure_types WHERE id = 'seed-measure-warning');

INSERT INTO enforcement_measure_types (id, name, sequence_number, addressee_type, form_key,
    requires_title, involves_third_party, follow_up_days, active, description)
SELECT * FROM (SELECT
 'seed-measure-bailiff' AS id,
 'Vollstreckungsauftrag an den Gerichtsvollzieher' AS name,
 20 AS sequence_number,
 'BAILIFF' AS addressee_type,
 'ANLAGE_1' AS form_key,
 1 AS requires_title,
 0 AS involves_third_party,
 42 AS follow_up_days,
 1 AS active,
 'Der amtliche Auftrag nach § 753 ZPO mit seinen Optionen nach § 802a Abs. 2 ZPO: Sachpfändung, gütliche Erledigung nach § 802b ZPO, Abnahme der Vermögensauskunft nach § 802c ZPO, Haftbefehlsantrag, Zustellung des Titels.' AS description
) AS tmp WHERE NOT EXISTS (SELECT 1 FROM enforcement_measure_types WHERE id = 'seed-measure-bailiff');

INSERT INTO enforcement_measure_types (id, name, sequence_number, addressee_type, form_key,
    requires_title, involves_third_party, follow_up_days, active, description)
SELECT * FROM (SELECT
 'seed-measure-asset-disclosure' AS id,
 'Abnahme der Vermögensauskunft (§ 802c ZPO)' AS name,
 30 AS sequence_number,
 'BAILIFF' AS addressee_type,
 'ANLAGE_1' AS form_key,
 1 AS requires_title,
 0 AS involves_third_party,
 42 AS follow_up_days,
 1 AS active,
 'Nach fruchtloser Pfändung oder von vornherein. Die Auskunft ist zugleich die Voraussetzung für die Eintragung ins Schuldnerverzeichnis (§ 882c ZPO).' AS description
) AS tmp WHERE NOT EXISTS (SELECT 1 FROM enforcement_measure_types WHERE id = 'seed-measure-asset-disclosure');

INSERT INTO enforcement_measure_types (id, name, sequence_number, addressee_type, form_key,
    requires_title, involves_third_party, follow_up_days, active, description)
SELECT * FROM (SELECT
 'seed-measure-arrest-warrant' AS id,
 'Haftbefehlsantrag (§ 802g ZPO)' AS name,
 40 AS sequence_number,
 'ENFORCEMENT_COURT' AS addressee_type,
 'ANLAGE_1' AS form_key,
 1 AS requires_title,
 0 AS involves_third_party,
 42 AS follow_up_days,
 1 AS active,
 'Wenn der Schuldner zum Termin zur Abgabe der Vermögensauskunft nicht erscheint oder sie ohne Grund verweigert.' AS description
) AS tmp WHERE NOT EXISTS (SELECT 1 FROM enforcement_measure_types WHERE id = 'seed-measure-arrest-warrant');

INSERT INTO enforcement_measure_types (id, name, sequence_number, addressee_type, form_key,
    requires_title, involves_third_party, follow_up_days, active, description)
SELECT * FROM (SELECT
 'seed-measure-search-order' AS id,
 'Antrag auf richterliche Durchsuchungsanordnung (§ 758a ZPO)' AS name,
 50 AS sequence_number,
 'ENFORCEMENT_COURT' AS addressee_type,
 NULL AS form_key,
 1 AS requires_title,
 0 AS involves_third_party,
 30 AS follow_up_days,
 1 AS active,
 'Die Wohnung des Schuldners darf gegen seinen Willen nur mit richterlicher Anordnung durchsucht werden.' AS description
) AS tmp WHERE NOT EXISTS (SELECT 1 FROM enforcement_measure_types WHERE id = 'seed-measure-search-order');

INSERT INTO enforcement_measure_types (id, name, sequence_number, addressee_type, form_key,
    requires_title, involves_third_party, follow_up_days, active, description)
SELECT * FROM (SELECT
 'seed-measure-pfueb' AS id,
 'Pfändungs- und Überweisungsbeschluss (Geldforderung)' AS name,
 60 AS sequence_number,
 'ENFORCEMENT_COURT' AS addressee_type,
 'ANLAGE_2' AS form_key,
 1 AS requires_title,
 1 AS involves_third_party,
 30 AS follow_up_days,
 1 AS active,
 'Pfändung einer Geldforderung des Schuldners gegen einen Dritten, §§ 829, 835 ZPO. Der Drittschuldner hat nach § 840 ZPO binnen zwei Wochen ab Zustellung zu erklären.' AS description
) AS tmp WHERE NOT EXISTS (SELECT 1 FROM enforcement_measure_types WHERE id = 'seed-measure-pfueb');

INSERT INTO enforcement_measure_types (id, name, sequence_number, addressee_type, form_key,
    requires_title, involves_third_party, follow_up_days, active, description)
SELECT * FROM (SELECT
 'seed-measure-pfueb-maintenance' AS id,
 'Pfändungs- und Überweisungsbeschluss (Unterhalt)' AS name,
 70 AS sequence_number,
 'ENFORCEMENT_COURT' AS addressee_type,
 'ANLAGE_3' AS form_key,
 1 AS requires_title,
 1 AS involves_third_party,
 30 AS follow_up_days,
 1 AS active,
 'Eigenes Formular für Unterhaltsforderungen: sie sind bevorrechtigt und unterliegen anderen Pfändungsgrenzen (§ 850d ZPO).' AS description
) AS tmp WHERE NOT EXISTS (SELECT 1 FROM enforcement_measure_types WHERE id = 'seed-measure-pfueb-maintenance');

INSERT INTO enforcement_measure_types (id, name, sequence_number, addressee_type, form_key,
    requires_title, involves_third_party, follow_up_days, active, description)
SELECT * FROM (SELECT
 'seed-measure-wage-attachment' AS id,
 'Lohnpfändung' AS name,
 80 AS sequence_number,
 'ENFORCEMENT_COURT' AS addressee_type,
 'ANLAGE_2' AS form_key,
 1 AS requires_title,
 1 AS involves_third_party,
 30 AS follow_up_days,
 1 AS active,
 'Pfändung des Arbeitseinkommens beim Arbeitgeber als Drittschuldner, mit den Pfändungsgrenzen der §§ 850 ff. ZPO.' AS description
) AS tmp WHERE NOT EXISTS (SELECT 1 FROM enforcement_measure_types WHERE id = 'seed-measure-wage-attachment');

INSERT INTO enforcement_measure_types (id, name, sequence_number, addressee_type, form_key,
    requires_title, involves_third_party, follow_up_days, active, description)
SELECT * FROM (SELECT
 'seed-measure-account-attachment' AS id,
 'Kontopfändung' AS name,
 90 AS sequence_number,
 'ENFORCEMENT_COURT' AS addressee_type,
 'ANLAGE_2' AS form_key,
 1 AS requires_title,
 1 AS involves_third_party,
 30 AS follow_up_days,
 1 AS active,
 'Pfändung des Kontoguthabens bei der Bank als Drittschuldnerin. Das Pfändungsschutzkonto nach § 850k ZPO bleibt in Höhe des Freibetrags unberührt.' AS description
) AS tmp WHERE NOT EXISTS (SELECT 1 FROM enforcement_measure_types WHERE id = 'seed-measure-account-attachment');

INSERT INTO enforcement_measure_types (id, name, sequence_number, addressee_type, form_key,
    requires_title, involves_third_party, follow_up_days, active, description)
SELECT * FROM (SELECT
 'seed-measure-mortgage' AS id,
 'Zwangssicherungshypothek' AS name,
 100 AS sequence_number,
 'LAND_REGISTRY' AS addressee_type,
 NULL AS form_key,
 1 AS requires_title,
 0 AS involves_third_party,
 30 AS follow_up_days,
 1 AS active,
 'Eintragung einer Sicherungshypothek auf das Grundstück des Schuldners, §§ 866, 867 ZPO. Erst ab 750 Euro zulässig (§ 866 Abs. 3 ZPO).' AS description
) AS tmp WHERE NOT EXISTS (SELECT 1 FROM enforcement_measure_types WHERE id = 'seed-measure-mortgage');

INSERT INTO enforcement_measure_types (id, name, sequence_number, addressee_type, form_key,
    requires_title, involves_third_party, follow_up_days, active, description)
SELECT * FROM (SELECT
 'seed-measure-forced-sale' AS id,
 'Antrag auf Zwangsversteigerung' AS name,
 110 AS sequence_number,
 'ENFORCEMENT_COURT' AS addressee_type,
 NULL AS form_key,
 1 AS requires_title,
 0 AS involves_third_party,
 60 AS follow_up_days,
 1 AS active,
 'Verwertung des Grundstücks nach dem ZVG. Der langsamste und teuerste Weg, aber bei werthaltigem Grundbesitz der wirksamste.' AS description
) AS tmp WHERE NOT EXISTS (SELECT 1 FROM enforcement_measure_types WHERE id = 'seed-measure-forced-sale');

INSERT INTO enforcement_measure_types (id, name, sequence_number, addressee_type, form_key,
    requires_title, involves_third_party, follow_up_days, active, description)
SELECT * FROM (SELECT
 'seed-measure-forced-administration' AS id,
 'Antrag auf Zwangsverwaltung' AS name,
 120 AS sequence_number,
 'ENFORCEMENT_COURT' AS addressee_type,
 NULL AS form_key,
 1 AS requires_title,
 0 AS involves_third_party,
 60 AS follow_up_days,
 1 AS active,
 'Verwertung der Erträge des Grundstücks statt seiner Substanz - bei vermietetem Grundbesitz oft das Mittel der Wahl.' AS description
) AS tmp WHERE NOT EXISTS (SELECT 1 FROM enforcement_measure_types WHERE id = 'seed-measure-forced-administration');

INSERT INTO enforcement_measure_types (id, name, sequence_number, addressee_type, form_key,
    requires_title, involves_third_party, follow_up_days, active, description)
SELECT * FROM (SELECT
 'seed-measure-residents-register' AS id,
 'Einwohnermeldeamtsanfrage' AS name,
 130 AS sequence_number,
 'REGISTRY' AS addressee_type,
 NULL AS form_key,
 0 AS requires_title,
 0 AS involves_third_party,
 21 AS follow_up_days,
 1 AS active,
 'Anschrift des Schuldners ermitteln. Setzt keinen Titel voraus, sondern ein berechtigtes Interesse.' AS description
) AS tmp WHERE NOT EXISTS (SELECT 1 FROM enforcement_measure_types WHERE id = 'seed-measure-residents-register');

INSERT INTO enforcement_measure_types (id, name, sequence_number, addressee_type, form_key,
    requires_title, involves_third_party, follow_up_days, active, description)
SELECT * FROM (SELECT
 'seed-measure-debtor-register' AS id,
 'Auskunft aus dem Schuldnerverzeichnis' AS name,
 140 AS sequence_number,
 'REGISTRY' AS addressee_type,
 NULL AS form_key,
 0 AS requires_title,
 0 AS involves_third_party,
 21 AS follow_up_days,
 1 AS active,
 'Abfrage nach § 882f ZPO, ob und weshalb der Schuldner eingetragen ist. Sagt vor einer teuren Maßnahme, ob sie sich lohnt.' AS description
) AS tmp WHERE NOT EXISTS (SELECT 1 FROM enforcement_measure_types WHERE id = 'seed-measure-debtor-register');

INSERT INTO enforcement_measure_types (id, name, sequence_number, addressee_type, form_key,
    requires_title, involves_third_party, follow_up_days, active, description)
SELECT * FROM (SELECT
 'seed-measure-inquiry' AS id,
 'Sonstige Auskunft (§ 802l ZPO)' AS name,
 150 AS sequence_number,
 'REGISTRY' AS addressee_type,
 NULL AS form_key,
 1 AS requires_title,
 0 AS involves_third_party,
 21 AS follow_up_days,
 1 AS active,
 'Der Gerichtsvollzieher erhebt Daten bei Rentenversicherung, Bundeszentralamt für Steuern oder Kraftfahrt-Bundesamt, wenn die Vermögensauskunft nichts ergeben hat.' AS description
) AS tmp WHERE NOT EXISTS (SELECT 1 FROM enforcement_measure_types WHERE id = 'seed-measure-inquiry');

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.38') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.38';
commit;
