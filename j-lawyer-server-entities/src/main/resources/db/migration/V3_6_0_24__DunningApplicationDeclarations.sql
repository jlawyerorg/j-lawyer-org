-- Die Angaben, die der Prozessbevollmächtigte je Mahnbescheidsantrag macht (Satz ASPVA00, C10 der
-- EDA-Satzbeschreibung Satzart 01).
--
-- Sie stehen hier und nicht in den Kanzleistammdaten, weil die Satzbeschreibung sie ausdrücklich
-- je Antrag verlangt: "Die definierten Daten sind im Bedarfsfall zu jedem einzelnen
-- Mahnbescheidsantrag zu machen und können daher nicht in einer Kennziffer hinterlegt werden."
--
-- order_date ist das Datum der Beauftragung, also der Tag, an dem ein unbedingtes Mandat erteilt
-- wurde. Es ist keine Formalie: nach der Übergangsvorschrift des § 60 Abs. 1 S. 1 RVG richtet sich
-- danach, welche Fassung des RVG das Gericht auf die festzusetzenden Gebühren anwendet.
--
-- offset_amount ist der Betrag, der nach Vorbem. 3 Abs. 4 VV RVG aus einer vorgerichtlichen
-- Geschäftsgebühr (Nr. 2300/2302 VV RVG) auf die Verfahrensgebühr Nr. 3305 VV RVG anzurechnen ist.
-- Anzugeben ist nur der anzurechnende Teil, nicht die gesamte vorgerichtliche Vergütung.
--
-- special_effort ist die Versicherung nach Feld VV2300M, dass die Angelegenheit besonders
-- umfangreich oder schwierig war.
--
-- file_name merkt sich den sechsstelligen EDA-Dateinamen des letzten Exports (Feld EDAID im
-- Dateivorsatz), damit ein Wiederholungsexport nicht unter einem beliebigen neuen Namen läuft.

alter table dunning_cases add column `order_date` DATE DEFAULT NULL;
alter table dunning_cases add column `offset_amount` DECIMAL(12,2) DEFAULT NULL;
alter table dunning_cases add column `special_effort` TINYINT(1) NOT NULL DEFAULT 0;
alter table dunning_cases add column `file_name` VARCHAR(6) BINARY DEFAULT NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.24') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.24';
commit;
