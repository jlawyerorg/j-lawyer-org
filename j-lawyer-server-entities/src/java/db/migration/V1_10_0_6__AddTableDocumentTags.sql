CREATE TABLE document_tags (
`id` VARCHAR(250) BINARY NOT NULL, 
`documentKey` VARCHAR(250) BINARY, 
`tagName` VARCHAR(250) BINARY, 
CONSTRAINT `pk_document_tags` PRIMARY KEY (`id`),
FOREIGN KEY (documentKey) REFERENCES case_documents(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table document_tags add index `IDX_TAGNAME` (tagName);
alter table document_tags add index `IDX_DOCUMENTKEY` (documentKey);

alter table document_tags add index `IDX_DOCUMENTKEY_TAGNAME` (documentKey, tagName);
alter table document_tags add unique index `IDX_DOCUMENTKEY_TAGNAME_UQ` (documentKey, tagName);

insert into ServerSettingsBean(settingKey, settingValue) values('jlawyer.server.database.version','1.10.0.5') ON DUPLICATE KEY UPDATE settingValue     = '1.10.0.6';
commit;