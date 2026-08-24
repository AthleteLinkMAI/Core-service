CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TYPE request_status AS ENUM ('PLANNED', 'ACTIVE', 'CONFIRMATION', 'COMPLETED', 'CANCELLED', 'FROZEN');
CREATE TYPE participant_role AS ENUM ('ORGANIZER', 'PLAYER');
CREATE TYPE participant_status AS ENUM ('PENDING', 'ACCEPTED', 'KICKED', 'LEFT');

CREATE TABLE sports (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    min_players INT NOT NULL DEFAULT 2,
    max_players INT NOT NULL DEFAULT 32,
    icon_url VARCHAR(255)
);

CREATE TABLE activity_requests (
    id UUID PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description TEXT,

    sport_id BIGINT NOT NULL REFERENCES sports(id),
    event_date TIMESTAMP WITH TIME ZONE NOT NULL,
    address_text VARCHAR(255) NOT NULL,

    location_point geometry(Point, 4326) NOT NULL,
    status request_status NOT NULL DEFAULT 'PLANNED',

    organizer_id UUID NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_activity_requests_location ON activity_requests USING GIST (location_point);


CREATE TABLE request_participants (
    id UUID PRIMARY KEY,
    request_id UUID NOT NULL REFERENCES activity_requests(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    
    role participant_role NOT NULL,
    status participant_status NOT NULL,

    joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (request_id, user_id)
);


INSERT INTO sports (name, min_players, max_players) VALUES
('Футбол', 10, 22),
('Баскетбол', 6, 10),
('Волейбол', 4, 12),
('Шахматы', 2, 2),
('Большой теннис', 2, 2),
('Настольный теннис', 2, 2);