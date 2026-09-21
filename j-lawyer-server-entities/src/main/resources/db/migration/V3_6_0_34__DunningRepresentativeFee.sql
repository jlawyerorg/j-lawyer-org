-- Welche Vergütung des Prozessbevollmächtigten in den Mahnbescheid aufgenommen werden soll
-- (Kennsatz-Bereich C10, Feld IKUBET der EDA-Satzbeschreibung Satzart 01).
--
-- Das Feld kennt drei Belegungen, die nichts miteinander zu tun haben:
--   leer     = gesetzliche Vergütung nach dem RVG in voller Höhe
--   Betrag   = eine mit der Partei vereinbarte, niedrigere Vergütung (Gesamtbetrag aus Gebühr,
--              Auslagen und ggf. Umsatzsteuer)
--   0,00     = Verzicht auf die Aufnahme einer Vergütung
--
-- Der Unterschied zwischen "leer" und "0,00" ist teuer: eine versehentliche Null verzichtet auf die
-- Vergütung. Deshalb wird die Entscheidung benannt gespeichert und die Kodierung dem Mapper
-- überlassen, statt einen Betrag zu führen, dessen Fehlen zweierlei bedeuten könnte.
--
-- Die Vergütung im Mahnverfahren ist zum 01.06.2025 neu geregelt worden; die Gerichte verlangen
-- die Angabe seither ausdrücklich, statt sie zu unterstellen.

alter table dunning_cases add column `representative_fee_mode` VARCHAR(20) BINARY NOT NULL DEFAULT 'LEGAL';
alter table dunning_cases add column `representative_fee_amount` DECIMAL(12,2) DEFAULT NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.34') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.34';
commit;
