insert into server_settings(settingKey, settingValue) values('jlawyer.server.drebis.techuser','h4OdYLfx+Op8K62y9UsGCHMnwihi6cyOPMcH') ON DUPLICATE KEY UPDATE settingValue     = 'h4OdYLfx+Op8K62y9UsGCHMnwihi6cyOPMcH';
insert into server_settings(settingKey, settingValue) values('jlawyer.server.drebis.techpwd','u6WzXu+u39d2JeyEAC3O/EwHo5enY7BV3whNig==') ON DUPLICATE KEY UPDATE settingValue     = 'u6WzXu+u39d2JeyEAC3O/EwHo5enY7BV3whNig==';

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','1.14.0.7') ON DUPLICATE KEY UPDATE settingValue     = '1.14.0.7';
commit;