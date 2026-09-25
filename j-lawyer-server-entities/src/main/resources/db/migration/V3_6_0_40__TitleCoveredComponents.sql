-- Welche Positionen des Forderungskontos ein Titel tituliert.
--
-- Die Zwangsvollstreckung läuft auf das, was im Titel steht, und die amtliche
-- Forderungsaufstellung (Anlagen 6 bis 8 der ZVFV) trennt die titulierte Forderung
-- ausdrücklich von der weiteren. Ohne diese Zuordnung müsste das Formular raten - und ein Raten
-- führt entweder dazu, dass etwas beigetrieben werden soll, was der Titel nicht deckt (der
-- Gerichtsvollzieher weist es zurück), oder dazu, dass etwas fehlt, was er deckt.
--
-- Für Titel, die vor dieser Migration erfasst wurden, bleibt die Zuordnung leer. Die Aufstellung
-- sagt das dann, statt sich etwas auszudenken: eine stillschweigende Einordnung wäre eine Aussage
-- über den Titel, die niemand getroffen hat.

CREATE TABLE IF NOT EXISTS `enforcement_title_components` (
`title_id` VARCHAR(50) BINARY NOT NULL,
`component_id` VARCHAR(50) BINARY NOT NULL,
PRIMARY KEY (`title_id`, `component_id`),
CONSTRAINT `fk_titlecomponent_title` FOREIGN KEY (title_id) REFERENCES enforcement_titles(id) ON DELETE CASCADE,
CONSTRAINT `fk_titlecomponent_component` FOREIGN KEY (component_id) REFERENCES claimcomponents(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.40') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.40';
commit;
