insert into config_bankstatement (id, display_name, csv_delimiter, number_format, number_locale, has_header, col_date, col_name, col_bookingtype, col_iban, col_purpose, col_amount, col_currency) values ('sparkasse-csv','Sparkasse',';','#,##0.00', 'DE', 1, 1, 5, 3, 6, 4, 8, 9);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.2.0.9') ON DUPLICATE KEY UPDATE settingValue     = '3.2.0.9';
commit;