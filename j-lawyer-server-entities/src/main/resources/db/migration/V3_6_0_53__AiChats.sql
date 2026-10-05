-- Stores the chats conducted with the AI assistant (Ingo).
--
-- ai_chats holds one row per chat: the optional case it belongs to, its creator, its title and
-- what is needed to continue it with the same assistant capability. ai_chat_messages holds the
-- conversation in order. Saving a chat replaces all of its message rows, so seq is dense and
-- starts at 0 on every save.
--
-- case_id cascades on delete: removing a case removes its chats, and through the second cascade
-- their messages. Chats without a case (case_id NULL) are private to their owner; they are removed
-- by the service when the owner is deleted.
--
-- message_version is incremented by every message save. A save states the version it is based on;
-- a save based on an outdated version is stored as a new chat instead of overwriting the messages
-- another user or dialog saved in between.
--
-- To continue a chat, the next request needs the assistant configuration, request type, action
-- and model, plus the system prompt and the prompt configuration (e.g. temperature) of the custom
-- prompt the chat was started with. The latter two are not part of the message list - they are sent
-- separately with every request - so they are stored with the chat. capability_name is for display
-- only.
--
-- principal_id of a message is the user whose request produced it: the user who wrote a user
-- message, and the user whose request led to an assistant or tool message. When the message list of
-- a chat is replaced, messages that were stored before keep their author, so a case chat that was
-- continued by several users shows who asked what.
--
-- The tables are utf8 so that case_id matches cases(id) for the foreign key. Only the free-text
-- columns are utf8mb4, because LLM output regularly contains emoji and other characters outside the
-- BMP (see V3_6_0_7__InstantMessageContentUtf8mb4.sql).

CREATE TABLE ai_chats (
`id` VARCHAR(50) BINARY NOT NULL,
`case_id` VARCHAR(50) BINARY DEFAULT NULL,
`owner` VARCHAR(50) BINARY NOT NULL,
`title` VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
`title_custom` TINYINT(1) NOT NULL DEFAULT 0,
`first_message` LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
`word_count` INT NOT NULL DEFAULT 0,
`message_version` INT NOT NULL DEFAULT 0,
`assistant_config_id` VARCHAR(50) BINARY DEFAULT NULL,
`request_type` VARCHAR(50) BINARY DEFAULT NULL,
`action_id` VARCHAR(250) BINARY DEFAULT NULL,
`model_ref` VARCHAR(250) BINARY DEFAULT NULL,
`capability_name` VARCHAR(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
`system_prompt` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
`configuration_values` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
`created` DATETIME DEFAULT NULL,
`last_activity` DATETIME DEFAULT NULL,
CONSTRAINT `pk_ai_chats` PRIMARY KEY (`id`),
CONSTRAINT `fk_aichats_case` FOREIGN KEY (case_id) REFERENCES cases(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table ai_chats add index `IDX_AICHATS_CASE` (case_id);
alter table ai_chats add index `IDX_AICHATS_OWNER` (owner);

CREATE TABLE ai_chat_messages (
`id` VARCHAR(50) BINARY NOT NULL,
`chat_id` VARCHAR(50) BINARY NOT NULL,
`seq` INT NOT NULL,
`role` VARCHAR(20) BINARY DEFAULT NULL,
`content` LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
`tool_call_id` VARCHAR(250) BINARY DEFAULT NULL,
`tool_name` VARCHAR(250) BINARY DEFAULT NULL,
`model_ref` VARCHAR(250) BINARY DEFAULT NULL,
`principal_id` VARCHAR(50) BINARY DEFAULT NULL,
CONSTRAINT `pk_ai_chat_messages` PRIMARY KEY (`id`),
CONSTRAINT `fk_aichatmessages_chat` FOREIGN KEY (chat_id) REFERENCES ai_chats(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table ai_chat_messages add index `IDX_AICHATMESSAGES_CHAT` (chat_id, seq);

insert into server_settings(settingKey, settingValue) values('jlawyer.server.database.version','3.6.0.53') ON DUPLICATE KEY UPDATE settingValue = '3.6.0.53';
commit;
