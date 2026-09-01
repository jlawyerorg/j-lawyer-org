-- Die Kennziffer, unter der ein Anwalt beim Mahngericht als EDA-Teilnehmer geführt wird.
--
-- Sie gehört zum Benutzer und nicht zur Kanzlei: sie identifiziert die einreichende Anwältin oder
-- den einreichenden Anwalt gegenüber dem Gericht, und in einer Kanzlei mit mehreren Berufsträgern
-- hat jede und jeder eine eigene. Sie wird beim Mahngericht beantragt und kann nicht selbst
-- vergeben werden.
--
-- Im EDA-Antrag ersetzt sie die Adresssätze des Prozessbevollmächtigten: wo sie steht, werden diese
-- Sätze weggelassen statt zusätzlich mitgeschickt.

alter table security_users add column `dunning_kennziffer` VARCHAR(20) BINARY DEFAULT NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.23') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.23';
commit;
