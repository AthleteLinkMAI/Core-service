CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TYPE request_status AS ENUM ('PLANNED', 'ACTIVE', 'COMPLETED', 'CANCELLED', 'FROZEN');
CREATE TYPE participant_role AS ENUM ('ORGANIZER', 'PLAYER');
CREATE TYPE participant_status AS ENUM ('PENDING', 'ACCEPTED', 'KICKED');

CREATE TABLE sports (
    name VARCHAR(100) PRIMARY KEY,
    min_players INT NOT NULL DEFAULT 2,
    max_players INT NOT NULL DEFAULT 32,
    icon_url VARCHAR(255),
    is_playable BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_sports_player_limits CHECK (min_players > 0 AND max_players >= min_players)
);

CREATE TABLE activity_requests (
    id UUID PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    sport_id VARCHAR(100) NOT NULL REFERENCES sports(name),
    max_players INT NOT NULL,
    event_date TIMESTAMP WITH TIME ZONE NOT NULL,
    address_text VARCHAR(255) NOT NULL,
    number_of_rounds INT NOT NULL DEFAULT 1 CHECK (number_of_rounds BETWEEN 1 AND 10),
    location_point geometry(Point, 4326) NOT NULL,
    status request_status NOT NULL DEFAULT 'PLANNED',
    organizer_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    registration_open BOOLEAN NOT NULL DEFAULT TRUE,
    first_reminder_enqueued_at TIMESTAMP WITH TIME ZONE,
    second_reminder_enqueued_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_activity_requests_location ON activity_requests USING GIST (location_point);
CREATE INDEX idx_activity_requests_planned_event_date
    ON activity_requests (event_date, id) WHERE status = 'PLANNED';

CREATE TABLE request_participants (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL REFERENCES activity_requests(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    role participant_role NOT NULL,
    status participant_status NOT NULL,
    joined_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_request_participant_user UNIQUE (request_id, user_id)
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    topic VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_outbox_events_pending
    ON outbox_events(created_at, id) WHERE published_at IS NULL;

INSERT INTO sports (name, min_players, max_players) VALUES
('Футбол', 10, 22),
('Баскетбол', 6, 10),
('Волейбол', 4, 12),
('Шахматы', 2, 2),
('Большой теннис', 2, 2),
('Настольный теннис', 2, 2);
