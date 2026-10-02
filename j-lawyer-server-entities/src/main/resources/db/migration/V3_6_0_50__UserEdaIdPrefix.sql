-- Das Buchstabenkürzel, aus dem die EDA-ID der Antragsdatei gebildet wird.
--
-- Das Mahngericht teilt es zusammen mit der Kennziffer zu (drei Buchstaben, etwa "FSR"); die
-- EDA-ID besteht aus ihm und einer dreistelligen fortlaufenden Nummer. Es steht deshalb neben
-- der Kennziffer am Benutzer und nicht an der Kanzlei.
--
-- Der Zähler dazu steht bewusst NICHT hier: mehrere Anwälte einer Kanzlei führen in aller Regel
-- dasselbe Kürzel, und ein Zähler je Benutzer vergäbe dieselben Nummern mehrfach. Er liegt in
-- server_settings unter eda.<KÜRZEL>.lastused, also je Kürzel.

alter table security_users add column `dunning_eda_prefix` VARCHAR(3) BINARY DEFAULT NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.50') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.50';
commit;
