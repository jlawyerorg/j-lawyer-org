SET @id_counter = 1;

INSERT INTO timesheet_tpls_allowed (id, timesheet_id, pos_tpl_id)
SELECT 
    CAST(@id_counter := @id_counter + 1 AS CHAR) AS id, 
    t.id AS timesheet_id, 
    tpls.id AS pos_tpl_id
FROM 
    timesheet_position_tpls tpls
CROSS JOIN 
    timesheets t;


insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','2.7.0.4') ON DUPLICATE KEY UPDATE settingValue     = '2.7.0.4';
commit;