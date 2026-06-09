insert into config_bankstatement (id, display_name, csv_delimiter, number_format, number_locale, header_lines, col_date, col_name, col_bookingtype, col_iban, col_purpose, col_amount, col_currency) values ('fyrst-csv','FYRST',';','#,##0.00', 'DE', 8, 0, 3, 2, 5, 4, 11, 17);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.2.0.11') ON DUPLICATE KEY UPDATE settingValue     = '3.2.0.11';
commit;