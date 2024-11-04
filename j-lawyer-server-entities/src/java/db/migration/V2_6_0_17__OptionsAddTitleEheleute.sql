INSERT IGNORE INTO server_options (id, optionGroup, value)
SELECT 'title.eheleute', 'address.title', 'Eheleute'
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM server_options
    WHERE optionGroup = 'address.title' and value='Eheleute'
);

INSERT IGNORE INTO server_options (id, optionGroup, value)
SELECT 'titleia.eheleute', 'address.titleinaddress', 'Eheleute'
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM server_options
    WHERE optionGroup = 'address.titleinaddress' and value='Eheleute'
);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.6.0.17') ON DUPLICATE KEY UPDATE settingValue     = '2.6.0.17';
commit;