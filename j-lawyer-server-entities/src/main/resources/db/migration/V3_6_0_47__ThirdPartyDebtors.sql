-- Die Drittschuldner einer Pfändung.
--
-- Gepfändet wird nicht beim Schuldner, sondern bei dem, der ihm etwas schuldet: dem Arbeitgeber,
-- der Bank, dem Mieter. Der Drittschuldner ist nicht Partei des Forderungskontos - er schuldet dem
-- Gläubiger nichts -, sondern hängt an der einzelnen Maßnahme. Deshalb eine eigene Tabelle und
-- keine weitere Rolle in claimledger_parties.
--
-- Die Bezeichnung und die Anschrift werden wie bei der Maßnahme festgeschrieben: was einmal in
-- einem Beschluss stand, muss nachvollziehbar bleiben, auch wenn der Kontakt später gepflegt wird.
--
-- § 840 Abs. 1 ZPO gibt dem Drittschuldner zwei Wochen ab Zustellung des Beschlusses, um zu
-- erklären, ob und was er schuldet. Die Frist steht als eigenes Datum daneben, nicht nur als
-- Rechnung aus dem Zustelldatum: sie kann im Einzelfall anders laufen, und was ueberwacht wird,
-- gehoert sichtbar gespeichert.

CREATE TABLE IF NOT EXISTS `enforcement_third_party_debtors` (
`id` VARCHAR(50) BINARY NOT NULL,
`measure_id` VARCHAR(50) BINARY NOT NULL,
`contact_id` VARCHAR(50) BINARY,
`designation` VARCHAR(500) BINARY,             -- festgeschrieben wie eine Parteibezeichnung
`address` VARCHAR(1000) BINARY,
`attached_claim` VARCHAR(30) NOT NULL DEFAULT 'OTHER',
`attached_claim_detail` VARCHAR(250) BINARY,   -- IBAN, Personalnummer, Bezeichnung der Forderung
`served_date` DATE,                            -- Zustellung des Beschlusses an den Drittschuldner
`declaration_due` DATE,                        -- § 840 Abs. 1 ZPO: zwei Wochen ab Zustellung
`declaration_received` DATE,
`declaration_note` VARCHAR(2000) BINARY,       -- was er erklaert hat
`review_id` VARCHAR(50) BINARY,                -- die Wiedervorlage zur Erklaerungsfrist
`notes` VARCHAR(2000) BINARY,
CONSTRAINT `pk_enforcement_tpd` PRIMARY KEY (`id`),
CONSTRAINT `fk_tpd_measure` FOREIGN KEY (measure_id) REFERENCES enforcement_measures(id) ON DELETE CASCADE,
CONSTRAINT `fk_tpd_contact` FOREIGN KEY (contact_id) REFERENCES contacts(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table enforcement_third_party_debtors add index `IDX_TPD_MEASURE` (measure_id);

-- Wer gezahlt hat, gehoert an die Buchung. Der Schuldner sagt, wer schuldet; dass das Geld vom
-- Arbeitgeber kam, sagt er nicht - und genau das ist bei einer Pfaendung die Nachricht.
alter table claimledger_entries add column `third_party_debtor_id` VARCHAR(50) BINARY DEFAULT NULL;
alter table claimledger_entries add constraint `fk_entry_tpd`
    FOREIGN KEY (third_party_debtor_id) REFERENCES enforcement_third_party_debtors(id) ON DELETE SET NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.47') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.47';
commit;
