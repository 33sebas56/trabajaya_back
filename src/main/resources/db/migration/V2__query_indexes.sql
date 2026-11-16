-- Additive migration: no rows are removed and no existing migration is rewritten.
CREATE INDEX idx_price_history_job ON price_history(job_id,id);
CREATE INDEX idx_media_job ON media(job_id,created_at);
CREATE INDEX idx_media_assignment ON media(assignment_id);
CREATE INDEX idx_job_events_job ON job_events(job_id,id);
CREATE INDEX idx_reviews_target ON reviews(target_id,id);
CREATE INDEX idx_reports_author ON reports(author_id,id);
CREATE INDEX idx_resets_user ON password_resets(user_id);
CREATE INDEX idx_sessions_expiry ON auth_sessions(expires_at);
CREATE INDEX idx_domain_events_time ON domain_events(created_at);
CREATE INDEX idx_users_avatar ON users(avatar_id);
