CREATE TABLE document_name_tpls (
`id` VARCHAR(50) BINARY NOT NULL, 
`display_name` VARCHAR(250) BINARY NOT NULL, 
`schema_syntax` VARCHAR(250) BINARY NOT NULL, 
`default_tpl` BIT(1) DEFAULT 0,
CONSTRAINT `pk_document_name_tpls` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

insert into document_name_tpls (id, display_name, schema_syntax, default_tpl) values ('default-tpl','Standard','yyyy-mm-dd_DATEINAME',1);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.7.0.8') ON DUPLICATE KEY UPDATE settingValue     = '2.7.0.8';
commit;