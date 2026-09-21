-- Die gesetzlichen Vertreter einer Partei als geordnete Kette statt als einzelner Verweis.
--
-- Das Format lässt bis zu sechs zu: "Zu jedem Antragsteller können maximal 6 gesetzliche Vertreter
-- (ASGV_01/ASGV_02) eingetragen werden! Gesetzliche Vertreter werden immer dem unmittelbar
-- vorausgegangenen Antragsteller zugeordnet!"
--
-- Das ist kein Randfall. Eine GmbH & Co. KG - eine der häufigsten deutschen Rechtsformen - wird von
-- ihrer persönlich haftenden Gesellschafterin vertreten (§ 161 Abs. 2 i. V. m. § 125 HGB), also von
-- der Komplementär-GmbH, und diese handelt durch ihren Geschäftsführer. Zwei Ebenen. Mit einem
-- einzigen Feld ließe sich nur eine davon eintragen, und jede allein ist falsch.
--
-- Der Vertreter kann eine Person oder eine Gesellschaft sein; beides steht in contacts. Die
-- Funktion steht als Text daneben - die Gerichte führen dafür eine geschlossene Liste, gegen die
-- noch nicht geprüft wird (siehe 5.3b, zweite Hälfte).

CREATE TABLE IF NOT EXISTS `claimledger_party_representatives` (
`id` VARCHAR(50) BINARY NOT NULL,
`party_id` VARCHAR(50) BINARY NOT NULL,
`contact_id` VARCHAR(50) BINARY,
`sequence_number` INT NOT NULL DEFAULT 1,
`function_designation` VARCHAR(35) BINARY,
PRIMARY KEY (`id`),
KEY `idx_ledgerpartyrep_party` (`party_id`),
CONSTRAINT `fk_ledgerpartyrep_party` FOREIGN KEY (party_id) REFERENCES claimledger_parties(id) ON DELETE CASCADE,
CONSTRAINT `fk_ledgerpartyrep_contact` FOREIGN KEY (contact_id) REFERENCES contacts(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Was bisher als einzelner Vertreter erfasst war, wird die erste Stufe der Kette.
--
-- Die Partei-ID dient zugleich als ID der Zeile: sie ist eindeutig, und zu jeder Partei entsteht
-- hier genau eine. Ein Präfix davorzusetzen wäre lesbarer, könnte aber über die 50 Zeichen der
-- Spalte hinauslaufen - und eine Migration, die an der Feldlänge scheitert, ist teurer als ein
-- Schlüssel, der zweimal dasselbe Zeichenmuster trägt.
insert into claimledger_party_representatives (id, party_id, contact_id, sequence_number, function_designation)
select p.id, p.id, p.legal_representative_id, 1, null
  from claimledger_parties p
 where p.legal_representative_id is not null;

alter table claimledger_parties drop foreign key `fk_ledgerparty_legalrep`;
alter table claimledger_parties drop column `legal_representative_id`;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.36') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.36';
commit;
