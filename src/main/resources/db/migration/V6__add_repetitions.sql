-- Add repetitions count to benchmarks so the execution loop knows how many
-- times to run each strategy. Default 1 keeps existing benchmark records valid.
ALTER TABLE benchmarks
    ADD COLUMN repetitions INTEGER NOT NULL DEFAULT 1;

-- benchmark_runs previously used (benchmark_id, strategy_type) as primary key,
-- which allowed only one run per strategy per benchmark. With repetitions we need
-- N rows per strategy. Drop the old PK and replace it with a surrogate that
-- includes a repetition_index column.
ALTER TABLE benchmark_runs
    DROP CONSTRAINT benchmark_runs_pkey;

ALTER TABLE benchmark_runs
    ADD COLUMN repetition_index INTEGER NOT NULL DEFAULT 0;

ALTER TABLE benchmark_runs
    ADD PRIMARY KEY (benchmark_id, strategy_type, repetition_index);
