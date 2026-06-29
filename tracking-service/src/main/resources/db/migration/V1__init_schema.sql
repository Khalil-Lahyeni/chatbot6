-- Flyway migration V1 — initial schema
-- Target: PostgreSQL 15+

-- ── PostgreSQL ENUM types ─────────────────────────────────────────────────────

CREATE TYPE system_health_status AS ENUM (
    'OK',
    'FAILURE',
    'PARTIALLY_OK',
    'COMMUNICATION_LOST'
);

CREATE TYPE update_status AS ENUM (
    'UPDATED',
    'PENDING'
);

-- ── train (root aggregate — primary key assigned externally, no auto-increment) ─

CREATE TABLE train (
    train_id   BIGINT       NOT NULL,
    train_name VARCHAR(255),
    update_at  TIMESTAMP    NOT NULL DEFAULT now(),
    mission    VARCHAR(255),
    baseline   VARCHAR(255),
    diversity  VARCHAR(255),
    database   VARCHAR(255),
    CONSTRAINT pk_train PRIMARY KEY (train_id)
);

-- ── train_location_status ─────────────────────────────────────────────────────

CREATE TABLE train_location_status (
    id               BIGINT    GENERATED ALWAYS AS IDENTITY,
    update_at        TIMESTAMP NOT NULL DEFAULT now(),
    current_station  VARCHAR(255),
    next_station     VARCHAR(255),
    destination      VARCHAR(255),
    train_id         BIGINT    NOT NULL,
    CONSTRAINT pk_train_location_status PRIMARY KEY (id),
    CONSTRAINT fk_location_train FOREIGN KEY (train_id) REFERENCES train (train_id)
);

CREATE INDEX idx_location_train_id ON train_location_status (train_id);
CREATE INDEX idx_location_update_at ON train_location_status (train_id, update_at DESC);

-- ── train_system_status ───────────────────────────────────────────────────────

CREATE TABLE train_system_status (
    id               BIGINT              GENERATED ALWAYS AS IDENTITY,
    update_at        TIMESTAMP           NOT NULL DEFAULT now(),
    pacis_status     system_health_status,
    cctv_status      system_health_status,
    rear_view_status system_health_status,
    update_status    update_status,
    train_id         BIGINT              NOT NULL,
    CONSTRAINT pk_train_system_status PRIMARY KEY (id),
    CONSTRAINT fk_system_status_train FOREIGN KEY (train_id) REFERENCES train (train_id)
);

CREATE INDEX idx_system_status_train_id ON train_system_status (train_id);
CREATE INDEX idx_system_status_update_at ON train_system_status (train_id, update_at DESC);

-- ── train_configuration ───────────────────────────────────────────────────────

CREATE TABLE train_configuration (
    id        BIGINT    GENERATED ALWAYS AS IDENTITY,
    update_at TIMESTAMP NOT NULL DEFAULT now(),
    visible   BOOLEAN   NOT NULL DEFAULT true,
    ccu1_ip   VARCHAR(45),
    ccu2_ip   VARCHAR(45),
    train_id  BIGINT    NOT NULL,
    CONSTRAINT pk_train_configuration PRIMARY KEY (id),
    CONSTRAINT fk_config_train FOREIGN KEY (train_id) REFERENCES train (train_id)
);

CREATE INDEX idx_config_train_id ON train_configuration (train_id);

-- ── train_message ─────────────────────────────────────────────────────────────

CREATE TABLE train_message (
    message_id   BIGINT       GENERATED ALWAYS AS IDENTITY,
    created_at   TIMESTAMP    NOT NULL DEFAULT now(),
    message_type VARCHAR(100),
    message_name VARCHAR(255),
    train_id     BIGINT       NOT NULL,
    CONSTRAINT pk_train_message PRIMARY KEY (message_id),
    CONSTRAINT fk_message_train FOREIGN KEY (train_id) REFERENCES train (train_id)
);

CREATE INDEX idx_message_train_id ON train_message (train_id);

-- ── media_database ────────────────────────────────────────────────────────────

CREATE TABLE media_database (
    id              BIGINT       GENERATED ALWAYS AS IDENTITY,
    update_at       TIMESTAMP    NOT NULL DEFAULT now(),
    device_ip       VARCHAR(45),
    name            VARCHAR(255) NOT NULL,
    version_number  VARCHAR(100),
    is_active       BOOLEAN,
    activation_date TIMESTAMP,
    train_id        BIGINT       NOT NULL,
    CONSTRAINT pk_media_database PRIMARY KEY (id),
    CONSTRAINT fk_media_train FOREIGN KEY (train_id) REFERENCES train (train_id)
);

CREATE INDEX idx_media_train_id ON media_database (train_id);
