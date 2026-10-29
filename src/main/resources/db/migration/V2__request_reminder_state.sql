ALTER TABLE activity_requests
    ADD COLUMN first_reminder_enqueued_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN second_reminder_enqueued_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX idx_activity_requests_planned_event_date
    ON activity_requests (event_date, id) WHERE status = 'PLANNED';
