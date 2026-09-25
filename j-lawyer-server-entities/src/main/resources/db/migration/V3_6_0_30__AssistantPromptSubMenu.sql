-- Optional submenu label for custom assistant prompts. Empty means the prompt stays on the
-- top level of the assistant menus. Several submenus can be listed separated by semicolons,
-- which shows the same prompt in each of them. Only one level of nesting is supported.
ALTER TABLE assistant_prompts ADD COLUMN sub_menu VARCHAR(250) BINARY DEFAULT NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.30') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.30';
commit;
