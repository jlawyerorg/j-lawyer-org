CREATE TABLE `mailbox_setup` (
  `id` VARCHAR(50) BINARY NOT NULL, 
  `display_name` VARCHAR(100) BINARY,
  `emailAddress` varchar(80) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `emailInType` varchar(15) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `emailInServer` varchar(80) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `emailInUser` varchar(75) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `emailInPwd` varchar(75) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `emailOutServer` varchar(80) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `emailOutUser` varchar(75) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `emailOutPwd` varchar(75) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `emailSenderName` varchar(150) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `emailSignature` varchar(3500) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `emailInSsl` tinyint DEFAULT NULL,
  `emailOutSsl` tinyint DEFAULT NULL,
  `emailStartTls` tinyint DEFAULT NULL,
  `emailOutPort` varchar(30) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  CONSTRAINT `pk_mailbox_setup` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

CREATE TABLE security_mailbox_access (
`id` VARCHAR(50) BINARY NOT NULL, 
`principal_id` VARCHAR(50) BINARY NOT NULL, 
`mailbox_id` VARCHAR(50) BINARY NOT NULL,
CONSTRAINT `pk_mailbox_access` PRIMARY KEY (`id`), 
FOREIGN KEY (principal_id) REFERENCES security_users(principalId) ON DELETE CASCADE, 
FOREIGN KEY (mailbox_id) REFERENCES mailbox_setup(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.0.0.1') ON DUPLICATE KEY UPDATE settingValue     = '2.0.0.1';
commit;