-- Stores text blocks ("Bausteine") that users insert into e-mails and beA messages while composing
-- them.
--
-- A text block has a plain-text and an HTML variant; the composer inserts the variant matching its
-- current editor mode. folder is an optional path of segments separated by '/', normalised by the
-- service (trimmed segments, no empty segments, NULL for the top level). Folders have no table of
-- their own, they exist through the blocks using them.
--
-- The combination of folder and name is unique. This is checked by the service, because a unique
-- index over both utf8mb4 columns would exceed the maximum index key length.

CREATE TABLE text_blocks (
`id` VARCHAR(50) BINARY NOT NULL,
`name` VARCHAR(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
`folder` VARCHAR(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
`content_text` MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
`content_html` MEDIUMTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_bin DEFAULT NULL,
CONSTRAINT `pk_text_blocks` PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
