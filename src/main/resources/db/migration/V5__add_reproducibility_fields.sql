-- Run snapshot: preserve the system prompt and canary token at the time of execution
ALTER TABLE evaluation_runs
    ADD COLUMN system_prompt_snapshot TEXT,
    ADD COLUMN canary_token_snapshot  VARCHAR(200);

-- Case-suite fingerprint: SHA-256 over all case IDs + user inputs at execution time
ALTER TABLE evaluation_runs
    ADD COLUMN case_suite_fingerprint VARCHAR(64);

-- Migrate timestamps from LocalDateTime (no timezone) to TIMESTAMP WITH TIME ZONE
ALTER TABLE evaluation_runs
    ALTER COLUMN created_at  TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN started_at  TYPE TIMESTAMPTZ USING started_at  AT TIME ZONE 'UTC',
    ALTER COLUMN finished_at TYPE TIMESTAMPTZ USING finished_at AT TIME ZONE 'UTC';

ALTER TABLE benchmarks
    ALTER COLUMN created_at  TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN started_at  TYPE TIMESTAMPTZ USING started_at  AT TIME ZONE 'UTC',
    ALTER COLUMN finished_at TYPE TIMESTAMPTZ USING finished_at AT TIME ZONE 'UTC';