-- Das Gericht, das bei Widerspruch das streitige Verfahren führt (Satz C16 der EDA-Satzbeschreibung
-- Satzart 01, Felder PGM, PGPLZ, PGO).
--
-- § 690 Abs. 1 Nr. 5 ZPO verlangt die Bezeichnung dieses Gerichts bereits im Mahnbescheidsantrag.
-- Ohne sie ist der Antrag unvollständig; jede Referenzdatei des Online-Mahnantrags trägt den Satz.
--
-- Die Angabe steht je Antragsgegner und nicht je Antrag, weil das Format sie so führt: auf jeden
-- Antragsgegner folgt sein eigener C16. Das ist nicht überflüssig - haben zwei Antragsgegner ihren
-- allgemeinen Gerichtsstand an verschiedenen Orten (§§ 12, 13 ZPO), sind es zwei verschiedene
-- Gerichte. In der Regel wird für alle dasselbe eingetragen.

alter table claimledger_parties add column `litigation_court_type` VARCHAR(30) BINARY DEFAULT NULL;
alter table claimledger_parties add column `litigation_court_postalcode` VARCHAR(5) BINARY DEFAULT NULL;
alter table claimledger_parties add column `litigation_court_city` VARCHAR(30) BINARY DEFAULT NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.35') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.35';
commit;
