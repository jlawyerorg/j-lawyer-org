-- Die Anlagen der ZVFV richtigstellen, und die Forderungsaufstellung als eigenen Schlüssel führen.
--
-- V3_6_0_38 hat die Anlagennummern aus dem Gedächtnis vergeben und dabei danebengegriffen. Die
-- Verordnung über Formulare für die Zwangsvollstreckung (ZVFV 2022) führt acht Anlagen:
--
--   1  Vollstreckungsauftrag an Gerichtsvollzieher
--   2  Antrag auf richterliche Durchsuchungsanordnung (auch Nachtzeit, Sonn- und Feiertage)
--   3  Entwurf einer richterlichen Durchsuchungsanordnung
--   4  Antrag auf Erlass eines Pfändungsbeschlusses und eines Pfändungs- und Überweisungsbeschlusses
--   5  Entwurf eines Pfändungsbeschlusses und eines Pfändungs- und Überweisungsbeschlusses
--   6  Aufstellung von Forderungen für Vollstreckungsaufträge an Gerichtsvollzieher
--   7  Aufstellung von Forderungen, die keine gesetzlichen Unterhaltsansprüche sind, für den PfÜB
--   8  Aufstellung von Forderungen bei gesetzlichen Unterhaltsansprüchen für den PfÜB
--
-- Daraus folgt zweierlei. Der PfÜB hat *ein* Antragsformular, die Anlage 4, gleich ob es um
-- Unterhalt geht oder nicht - verschieden ist allein die beigefügte Forderungsaufstellung, Anlage 7
-- gegen Anlage 8. Deshalb bekommt die Maßnahmeart einen zweiten Schlüssel. Und die
-- Durchsuchungsanordnung hat sehr wohl ein Formular, die Anlage 2, die ich für den PfÜB gehalten
-- hatte.
--
-- Geändert werden nur Zeilen, die noch den falschen Wert von V3_6_0_38 tragen. Wer ihn in der
-- Zwischenzeit selbst richtiggestellt hat, behält seine Fassung.

alter table enforcement_measure_types add column `itemisation_form_key` VARCHAR(50) DEFAULT NULL;

update enforcement_measure_types set form_key = 'ANLAGE_2'
 where id = 'seed-measure-search-order' and form_key is null;

update enforcement_measure_types set form_key = 'ANLAGE_4', itemisation_form_key = 'ANLAGE_7'
 where id = 'seed-measure-pfueb' and form_key = 'ANLAGE_2';

update enforcement_measure_types set form_key = 'ANLAGE_4', itemisation_form_key = 'ANLAGE_8'
 where id = 'seed-measure-pfueb-maintenance' and form_key = 'ANLAGE_3';

update enforcement_measure_types set form_key = 'ANLAGE_4', itemisation_form_key = 'ANLAGE_7'
 where id = 'seed-measure-wage-attachment' and form_key = 'ANLAGE_2';

update enforcement_measure_types set form_key = 'ANLAGE_4', itemisation_form_key = 'ANLAGE_7'
 where id = 'seed-measure-account-attachment' and form_key = 'ANLAGE_2';

-- Der Vollstreckungsauftrag und seine Optionen laufen über die Anlage 1 und bekommen die Anlage 6
-- als Forderungsaufstellung. Vermögensauskunft und Haftbefehlsantrag sind Ankreuzfelder dieses
-- Auftrags und keine eigenen Formulare; sie behalten die Anlage 1.
update enforcement_measure_types set itemisation_form_key = 'ANLAGE_6'
 where id in ('seed-measure-bailiff', 'seed-measure-asset-disclosure', 'seed-measure-arrest-warrant')
   and form_key = 'ANLAGE_1' and itemisation_form_key is null;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.39') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.39';
commit;
