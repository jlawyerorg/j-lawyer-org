CREATE TABLE calendar_setup (
`id` VARCHAR(50) BINARY NOT NULL, 
`name` VARCHAR(100) BINARY, 
`display_name` VARCHAR(100) BINARY,
`background` INTEGER NOT NULL, 
`event_type` INTEGER NOT NULL, 
CONSTRAINT `pk_calendar_setup` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE calendar_setup ADD index `idx_calsetup_displayname`(`display_name`);

alter table calendar_setup add `cloudHost` VARCHAR(250) BINARY;
alter table calendar_setup add `cloudPort` INTEGER NOT NULL;
alter table calendar_setup add `cloudSsl` TINYINT;
alter table calendar_setup add `cloudUser` VARCHAR(50) BINARY;
alter table calendar_setup add `cloudPassword` VARCHAR(50) BINARY;
alter table calendar_setup add `cloudPath` VARCHAR(250) BINARY;

insert into calendar_setup (id, display_name, background, event_type, cloudPort, cloudSsl) values ('wiedervorlagen-id', 'Wiedervorlagen', -6832371, 10, 443, 1);
insert into calendar_setup (id, display_name, background, event_type, cloudPort, cloudSsl) values ('fristen-id', 'Fristen', -2215621, 20, 443, 1);
insert into calendar_setup (id, display_name, background, event_type, cloudPort, cloudSsl) values ('termine-id', 'Termine', -15830347, 30, 443, 1);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','1.15.0.4') ON DUPLICATE KEY UPDATE settingValue     = '1.15.0.4';
commit;