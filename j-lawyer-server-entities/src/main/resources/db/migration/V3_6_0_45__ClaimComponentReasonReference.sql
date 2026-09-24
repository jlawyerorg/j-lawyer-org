-- Die Nummer zur Anspruchsbegründung an der Forderungsposition.
--
-- Sie gehört zur Forderung, nicht zum Antrag: dieselbe Rechnung nennt die Mahnung, die
-- Forderungsaufstellung und jeder weitere Antrag. Bisher wurde sie in die Tabelle des
-- Mahnverfahren-Dialogs getippt, ging einmal mit dem Antrag hinaus und war danach fort - beim
-- nächsten Erzeugen musste sie erneut getippt werden.
--
-- 35 Zeichen, weil die Austauschdatei für ASPRNR so viele führt. Es ist dieselbe Spalte, die die
-- Zusatzangabe der Katalognummern 36, 42 und 61 belegt; beides zugleich nimmt der Antrag nicht an,
-- und die Prüfung sagt es.

alter table claimcomponents add column `claim_reason_reference` varchar(35) default NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.45') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.45';
commit;
