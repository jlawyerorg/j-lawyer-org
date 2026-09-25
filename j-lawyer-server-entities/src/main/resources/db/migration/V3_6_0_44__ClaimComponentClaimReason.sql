-- Die Anspruchsbegründung an der Forderungsposition.
--
-- Das Mahngericht druckt sie: "aus Rechnung Nr. 4711 vom 15.09.2025". Die Austauschdatei führt sie
-- in einem eigenen Feld (ASPGR), und der Online-Mahnantrag bietet dafür eine geschlossene Liste an:
-- Schreiben, Rechnung, Mahnung, Kontoauszug, Aufstellung, Vertrag, Stromrechnung, Gasrechnung,
-- andere.
--
-- Bis hierher stand im Feld der *Name der Position* - "PKW". Das benennt die Sache, nicht den
-- Grund, und was dort steht, druckt der Mahnbescheid. Aufgefallen ist es erst, als die erzeugte
-- Datei so dargestellt wurde, wie das Gericht sie darstellt.
--
-- Die Spalte bleibt leer, wo niemand etwas gesagt hat; der Erzeuger leitet dann ab, was er
-- verantworten kann, und die Oberfläche schlägt dasselbe vor.

alter table claimcomponents add column `claim_reason` varchar(30) default NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.44') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.44';
commit;
