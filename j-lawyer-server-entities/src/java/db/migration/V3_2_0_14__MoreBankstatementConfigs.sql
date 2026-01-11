insert into config_bankstatement (id, display_name, csv_delimiter, number_format, number_locale, header_lines, col_date, col_name, col_bookingtype, col_iban, col_purpose, col_amount, col_currency) values ('starmoney-csv','Starmoney',';','#,##0.00', 'DE', 1, 0, 1, -1, -1, 2, 3, -1);
update config_bankstatement set display_name='Sparkasse CAMT-V2', col_date=1, col_name=11, col_bookingtype=3, col_iban=12, col_purpose=4, col_amount=14, col_currency=15 where id='sparkasse-csv';

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.2.0.14') ON DUPLICATE KEY UPDATE settingValue     = '3.2.0.14';
commit;