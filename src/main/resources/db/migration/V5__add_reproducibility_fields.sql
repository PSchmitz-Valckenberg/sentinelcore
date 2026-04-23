-- Run snapshot: preserve the system prompt and canary token when the run is created.
-- Captured via SystemPromptBuilder.build() so the snapshot reflects the exact prompt
-- sent to the LLM, including the appended canary token.
ALTER TABLE evaluation_runs
    ADD COLUMN system_prompt_snapshot TEXT,
    ADD COLUMN canary_token_snapshot  VARCHAR(200);

-- Case-suite fingerprint: SHA-256 over all case IDs + user inputs at execution time.
ALTER TABLE evaluation_runs
    ADD COLUMN case_suite_fingerprint VARCHAR(64);

-- Migrate timestamps to TIMESTAMP WITH TIME ZONE for timezone-safe UTC storage.
-- NOTE: Historical LocalDateTime values were written on a server running in UTC
-- (Docker default). The AT TIME ZONE 'UTC' conversion is correct for this deployment.
-- If redeploying on a non-UTC server, verify the original timezone before running.
ALTER TABLE evaluation_runs
    ALTER COLUMN created_at  TYPE TIMESTAMPTZ USING created_at  AT TIME ZONE 'UTC',
    ALTER COLUMN started_at  TYPE TIMESTAMPTZ USING started_at  AT TIME ZONE 'UTC',
    ALTER COLUMN finished_at TYPE TIMESTAMPTZ USING finished_at AT TIME ZONE 'UTC';

ALTER TABLE benchmarks
    ALTER COLUMN created_at  TYPE TIMESTAMPTZ USING created_at  AT TIME ZONE 'UTC',
    ALTER COLUMN started_at  TYPE TIMESTAMPTZ USING started_at  AT TIME ZONE 'UTC',
    ALTER COLUMN finished_at TYPE TIMESTAMPTZ USING finished_at AT TIME ZONE 'UTC';