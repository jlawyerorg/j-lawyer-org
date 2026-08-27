-- instantmessage was created with DEFAULT CHARSET=utf8 (V2_4_0_13__InstantMessaging.sql).
-- MySQL/MariaDB "utf8" is utf8mb3 and holds at most 3 bytes per character, so any message
-- containing an emoji (or any other character outside the BMP) is rejected with error 1366
-- "Incorrect string value", which fails the flush and rolls back the whole transaction.
--
-- Only the free-text column is converted. A table-wide CONVERT TO CHARACTER SET would also
-- change case_id and document_id, whose foreign keys to cases(id) / case_documents(id)
-- require both sides to share charset and collation - those constraints would break.
-- The id / sender columns hold technical identifiers and stay on utf8.
--
-- 3000 * 4 = 12000 bytes stays well below the InnoDB row limit and content carries no index.

ALTER TABLE instantmessage MODIFY `content` VARCHAR(3000) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL;

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.7') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.7';
commit;
