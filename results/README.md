# Benchmark Results

This directory stores the raw JSON outputs from SentinelCore benchmark campaigns.

Each campaign run creates a timestamped subdirectory:

```
results/
  20260426_143022_gemini-2.0-flash/
    01_create.json    — benchmark creation response
    02_execute.json   — execution summary
    03_report.json    — full metrics report with per-strategy breakdown
```

## Running a campaign

```bash
# Make sure PostgreSQL and the app are running, then:
./scripts/run_benchmark.sh

# Pass --label to tag the output directory with a human-readable name:
./scripts/run_benchmark.sh --label gemini-2.0-flash
```

To benchmark a different provider, update `sentinelcore.llm.provider` (and `sentinelcore.llm.model`) in `application-local.yml` and restart the app before running the script.

## Result files are gitignored

Actual result JSON files are excluded from version control (see `.gitignore`).
The summary numbers are published in the main [README](../README.md).
