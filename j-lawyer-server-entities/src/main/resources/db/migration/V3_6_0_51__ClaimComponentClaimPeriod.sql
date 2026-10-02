-- Der Tag, aus dem der Anspruch entstanden ist - und bei Zeitraumforderungen dessen Ende.
--
-- Das Mahngericht hat unseren Antrag beanstandet, weil im Anspruchssatz das Vom-Datum fehlte: der
-- Mahnbescheid nennt es neben der Begründung ("aus Rechnung Nr. 4711 vom 15.09.2025"), und in
-- sämtlichen Beispieldateien des Mahngerichtsportals ist es gesetzt - auch bei Einzelforderungen.
--
-- Bis dahin gab es im Forderungskonto überhaupt kein Datum der Anspruchsentstehung; wer es angeben
-- wollte, schrieb es in den Belegtext. Das Bis-Datum bleibt leer, außer die Forderung läuft über
-- einen Zeitraum (Miete, Pacht, wiederkehrende Beträge).

alter table claimcomponents add column `claim_from` DATE DEFAULT NULL;
alter table claimcomponents add column `claim_to` DATE DEFAULT NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.51') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.51';
commit;
