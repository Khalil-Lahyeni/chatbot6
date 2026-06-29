-- Flyway migration V2
-- Adds the is_critical flag to train_message so the consumer can mark
-- messages that contain critical keywords (Failure, Brake, Emergency, etc.)

ALTER TABLE train_message
    ADD COLUMN is_critical BOOLEAN NOT NULL DEFAULT false;
