-- 1 = can log in, 0 = disabled by an admin
ALTER TABLE users ADD COLUMN enabled INT NOT NULL DEFAULT 1;
