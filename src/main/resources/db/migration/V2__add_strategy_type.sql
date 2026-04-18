ALTER TABLE evaluation_runs
    ADD COLUMN strategy_type VARCHAR(50) NOT NULL DEFAULT 'NONE';
