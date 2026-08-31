-- Links a dunning procedure to the calendar entries its statutory deadlines produced.
--
-- Without this table the generated events could be created but never found again: recalculating a
-- deadline after a corrected service date, or closing one that no longer applies, both require
-- knowing which entry belongs to which deadline of which procedure. That is what the type column
-- is for - it identifies the deadline, so the same one is updated instead of a second being added.
--
-- The computed date is kept alongside the reference to the calendar entry. It is what the deadline
-- actually is; the entry may sit earlier by the configured lead time, and a user may move the entry.
-- Comparing the two is how a recalculation notices that something changed.
--
-- Entries the user has already marked as done are not silently rewritten. The done flag lives on the
-- calendar entry, so a recalculation reads it there and reports rather than overwrites.

CREATE TABLE dunning_case_deadlines (
`id` VARCHAR(50) BINARY NOT NULL,
`dunning_case_id` VARCHAR(50) BINARY NOT NULL,
`deadline_type` VARCHAR(30) BINARY NOT NULL,
`deadline_date` DATE NOT NULL,
`review_id` VARCHAR(50) BINARY DEFAULT NULL,
-- set when the deadline no longer applies; the row is kept so the history stays readable
`closed` TINYINT(1) DEFAULT 0 NOT NULL,
`closed_reason` VARCHAR(250) BINARY DEFAULT NULL,
CONSTRAINT `pk_dunning_case_deadlines` PRIMARY KEY (`id`),
CONSTRAINT `fk_dunningdeadline_case` FOREIGN KEY (dunning_case_id) REFERENCES dunning_cases(id) ON DELETE CASCADE,
CONSTRAINT `fk_dunningdeadline_review` FOREIGN KEY (review_id) REFERENCES case_events(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table dunning_case_deadlines add index `IDX_DUNNINGDEADLINES_CASE` (dunning_case_id);
alter table dunning_case_deadlines add unique index `UQ_DUNNINGDEADLINES_CASE_TYPE` (dunning_case_id, deadline_type);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.20') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.20';
commit;
