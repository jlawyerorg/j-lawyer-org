CREATE TABLE address_documents (
`id` VARCHAR(250) BINARY NOT NULL,
`name` VARCHAR(250) BINARY,
`size` BIGINT DEFAULT 0,
`creationDate` DATETIME,
`date_changed` DATETIME,
`contact_id` VARCHAR(250) BINARY,
`deleted_by` VARCHAR(250) BINARY,
`deletion_date` DATETIME,
`deleted` BIT(1) DEFAULT 0,
CONSTRAINT `pk_address_documents` PRIMARY KEY (`id`),
FOREIGN KEY (`contact_id`) REFERENCES contacts(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table address_documents add index `idx_address_documents_contact` (contact_id);
alter table address_documents add index `idx_address_documents_deleted` (deleted);
alter table address_documents add index `IDX_ADDRDOCDATECHANGED` (date_changed);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.3') ON DUPLICATE KEY UPDATE settingValue     = '3.6.0.3';
commit;
