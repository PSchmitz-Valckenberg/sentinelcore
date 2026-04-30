#!/usr/bin/env bash
# run_benchmark.sh — runs a full 5-strategy benchmark campaign against a
# locally running SentinelCore instance and saves the report to results/.
#
# Prerequisites:
#   - docker compose up -d          (PostgreSQL)
#   - mvn spring-boot:run -Dspring-boot.run.profiles=local   (app on :8080)
#   - jq installed (brew install jq)
#
# Usage:
#   ./scripts/run_benchmark.sh
#   ./scripts/run_benchmark.sh --label gemini-2.5-flash
#   ./scripts/run_benchmark.sh --label gemini-2.5-flash --repetitions 5
#
# --repetitions N  How many times each strategy is run (default: 3).
#                  Mean and stddev are reported per strategy. N=1 gives no
#                  stddev (marked as null in the JSON report).
#
# Note: the active LLM provider is configured in application-local.yml
# (sentinelcore.llm.provider / sentinelcore.llm.model). --label is a free-form
# string used only as a human-readable tag in the output directory name and in
# the benchmark metadata sent to the API. Restart the app with a different
# provider config to benchmark a different model.

set -euo pipefail

BASE_URL="http://localhost:8080"
LABEL="gemini-2.5-flash"
REPETITIONS=3
RESULTS_DIR="$(dirname "$0")/../results"

# Number of strategies sent in the request (NONE is always auto-prepended by the
# server, so the actual run count is STRATEGY_COUNT + 1). Update this when
# strategyTypes in the curl payload below changes.
STRATEGY_COUNT=4

# Total seed cases — update when cases are added or removed from src/main/resources/seed/cases/.
CASE_COUNT=30

while [[ $# -gt 0 ]]; do
  case "$1" in
    --label)
      [[ $# -ge 2 ]] || { echo "Error: --label requires an argument"; exit 1; }
      LABEL="$2"; shift 2 ;;
    --repetitions)
      [[ $# -ge 2 ]] || { echo "Error: --repetitions requires an argument"; exit 1; }
      REPETITIONS="$2"; shift 2 ;;
    *) echo "Unknown argument: $1"; exit 1 ;;
  esac
done

if ! [[ "$REPETITIONS" =~ ^[1-9][0-9]*$ ]]; then
  echo "Error: --repetitions must be a positive integer, got: '$REPETITIONS'"; exit 1
fi

command -v jq >/dev/null 2>&1 || { echo "jq is required but not installed. Run: brew install jq"; exit 1; }
curl -sfS "$BASE_URL/actuator/health" >/dev/null 2>&1 \
  || curl -sfS "$BASE_URL/v3/api-docs" >/dev/null 2>&1 \
  || { echo "SentinelCore does not appear to be running on $BASE_URL"; exit 1; }

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
OUT_DIR="$RESULTS_DIR/${TIMESTAMP}_${LABEL}"
mkdir -p "$OUT_DIR"

echo "=== SentinelCore Benchmark Campaign ==="
echo "Label:       $LABEL"
echo "Repetitions: $REPETITIONS"
echo "Output dir:  $OUT_DIR"
echo ""

# ── 1. Create benchmark ──────────────────────────────────────────────────────
echo "[1/3] Creating benchmark..."
CREATE_RESPONSE=$(curl -sfS -X POST "$BASE_URL/api/benchmarks" \
  -H "Content-Type: application/json" \
  -d "$(jq -n --arg model "$LABEL" --argjson reps "$REPETITIONS" \
       '{model: $model, strategyTypes: ["INPUT_FILTER","INPUT_OUTPUT","PROMPT_HARDENING","RAG_CONTENT_FILTER"], repetitions: $reps}')")

BENCHMARK_ID=$(echo "$CREATE_RESPONSE" | jq -r '.benchmarkId')
echo "      Benchmark ID: $BENCHMARK_ID"
echo "$CREATE_RESPONSE" | jq . > "$OUT_DIR/01_create.json"

# ── 2. Execute benchmark (synchronous — may take several minutes) ─────────────
# Timeout = repetitions × (requested strategies + 1 for NONE baseline) × cases × 6s/call.
# Clamped to [600, 7200]. Raise the per-call estimate if your provider runs slower.
TOTAL_STRATEGIES=$(( STRATEGY_COUNT + 1 ))
MAX_TIME=$(( REPETITIONS * TOTAL_STRATEGIES * CASE_COUNT * 6 ))
if [[ $MAX_TIME -lt 600 ]]; then MAX_TIME=600; fi
if [[ $MAX_TIME -gt 7200 ]]; then MAX_TIME=7200; fi
echo "[2/3] Executing benchmark (timeout: ${MAX_TIME}s — please wait)..."
EXECUTE_RESPONSE=$(curl -sfS -X POST "$BASE_URL/api/benchmarks/$BENCHMARK_ID/execute" \
  --max-time $MAX_TIME)

STATUS=$(echo "$EXECUTE_RESPONSE" | jq -r '.status')
echo "      Status: $STATUS"
echo "$EXECUTE_RESPONSE" | jq . > "$OUT_DIR/02_execute.json"

if [[ "$STATUS" != "COMPLETED" ]]; then
  echo "Benchmark did not complete successfully (status=$STATUS). Check $OUT_DIR/02_execute.json."
  exit 1
fi

# ── 3. Fetch report ───────────────────────────────────────────────────────────
echo "[3/3] Fetching report..."
REPORT=$(curl -sfS "$BASE_URL/api/benchmarks/$BENCHMARK_ID/report")
echo "$REPORT" | jq . > "$OUT_DIR/03_report.json"

# ── Summary table ─────────────────────────────────────────────────────────────
REPS=$(echo "$REPORT" | jq '.repetitions')
echo ""
echo "=== Results (N=$REPS repetitions per strategy) ==="
echo "Mean per strategy:"
echo "$REPORT" | jq -r '
  ["Strategy", "ASR", "FPR", "Refusal", "Latency(ms)"],
  (.runs[] | [
    .strategyType,
    (.aggregated.attackSuccessRateMean | tostring),
    (.aggregated.falsePositiveRateMean | tostring),
    (.aggregated.refusalRateMean | tostring),
    (.aggregated.avgLatencyMsMean | tostring)
  ]) | @tsv' | column -t

echo ""
echo "Stddev per strategy (null = N=1, not computable):"
echo "$REPORT" | jq -r '
  ["Strategy", "ASR-stddev", "FPR-stddev", "Refusal-stddev", "Latency-stddev(ms)"],
  (.runs[] | [
    .strategyType,
    (.aggregated.attackSuccessRateStddev // "null" | tostring),
    (.aggregated.falsePositiveRateStddev // "null" | tostring),
    (.aggregated.refusalRateStddev // "null" | tostring),
    (.aggregated.avgLatencyMsStddev // "null" | tostring)
  ]) | @tsv' | column -t

echo ""
echo "Full report saved to: $OUT_DIR/03_report.json"
echo ""
echo "To add these results to the README, copy the numbers from the table above"
echo "into the 'Benchmark Results' section in README.md."
