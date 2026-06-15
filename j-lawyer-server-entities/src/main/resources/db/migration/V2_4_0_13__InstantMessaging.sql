CREATE TABLE instantmessage (
`id` VARCHAR(50) BINARY NOT NULL, 
`sender` VARCHAR(50) BINARY, 
`sent` datetime default NULL, 
`content` VARCHAR(3000) BINARY, 
`case_id` VARCHAR(50) BINARY DEFAULT NULL, 
`document_id` VARCHAR(50) BINARY DEFAULT NULL, 
CONSTRAINT `pk_messages` PRIMARY KEY (`id`), 
FOREIGN KEY (case_id) REFERENCES cases(id) ON DELETE SET NULL, 
FOREIGN KEY (document_id) REFERENCES case_documents(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE instantmessage_mention (
`id` VARCHAR(50) BINARY NOT NULL, 
`message_id` VARCHAR(50) BINARY NOT NULL, 
`principal` VARCHAR(50) BINARY, 
`done` TINYINT default 0, 
CONSTRAINT `pk_message_mentions` PRIMARY KEY (`id`), 
FOREIGN KEY (message_id) REFERENCES instantmessage(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.4.0.13') ON DUPLICATE KEY UPDATE settingValue     = '2.4.0.13';
commit;