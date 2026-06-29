-- Sequence used by the REST API to auto-generate train IDs.
-- Starts at 1000 to stay well above any IDs assigned by Kafka-originated placeholder trains.
CREATE SEQUENCE IF NOT EXISTS train_id_seq
    START WITH 1000
    INCREMENT BY 1
    NO CYCLE;
