-- Rechtskraft eines Titels (#3789).
--
-- Die 30-jährige Verjährung titulierter Ansprüche (§ 197 Abs. 1 Nr. 3 und 4 BGB) beginnt nach
-- § 201 BGB mit der Rechtskraft der Entscheidung bzw. mit der Errichtung des vollstreckbaren
-- Titels - nicht mit dem Erlass. Bei Urteil, Vollstreckungsbescheid oder Kostenfestsetzungsbeschluss
-- liegt zwischen Erlass und Rechtskraft mindestens die Rechtsmittel- bzw. Einspruchsfrist.
--
-- Ist das Datum der Rechtskraft erfasst, wird die Verjährung ab diesem Tag berechnet; sonst wie
-- bisher ab dem Erlassdatum. Bestehende Titel behalten daher ihr Verjährungsdatum.

alter table enforcement_titles add column `finality_date` DATE DEFAULT NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.62') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.62';
commit;
