-- Lesson text was VARCHAR(4000). MEDIUMTEXT holds up to ~16 million characters.
-- Existing lessons are kept as they are.
ALTER TABLE lessons MODIFY content MEDIUMTEXT NULL;
