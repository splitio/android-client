#!/usr/bin/env bash
# Runs the ready-timing launch for one scenario on the connected device and summarizes it.
#
# Usage: ready-timing.sh [--enc 0|1] [--flags N] [--sets 0|1] [--latency MS]
#                        [--runs N] [--no-seed] [--package TEST_PACKAGE]
#
#   --runs N      launches to run, at least 10 (default 12); the first is discarded as a warm-up
#   --no-seed     reuse the template already on the device instead of seeding first
#   --package     the instrumentation package; default io.split.android.client.main.test, which is
#                 the :main namespace (io.split.android.client.main) with the ".test" suffix AGP
#                 gives the androidTest APK
#
# Each launch is its own `am instrument` call, so each is a fresh process. The report of a launch is
# read from the `INSTRUMENTATION_STATUS: splitPerf=<json>` line of the `-r` output; the
# `SPLITPERF_RESULT <json>` logcat line carries the same JSON but is not read.
#
# Environment: PERF_RESULTS_DIR (default ./perf-results), ADB (default adb).
set -euo pipefail

ENC=0
FLAGS=1000
SETS=0
LATENCY=0
RUNS=12
SEED=1
PACKAGE=io.split.android.client.main.test
ADB=${ADB:-adb}
RESULTS_DIR=${PERF_RESULTS_DIR:-./perf-results}
HERE=$(cd "$(dirname "$0")" && pwd)

die() {
  echo "ready-timing: $*" >&2
  exit 1
}

while [ $# -gt 0 ]; do
  case "$1" in
    --enc) ENC=${2:?--enc needs a value}; shift 2 ;;
    --flags) FLAGS=${2:?--flags needs a value}; shift 2 ;;
    --sets) SETS=${2:?--sets needs a value}; shift 2 ;;
    --latency) LATENCY=${2:?--latency needs a value}; shift 2 ;;
    --runs) RUNS=${2:?--runs needs a value}; shift 2 ;;
    --package) PACKAGE=${2:?--package needs a value}; shift 2 ;;
    --no-seed) SEED=0; shift ;;
    *) die "unknown argument: $1" ;;
  esac
done

case "$ENC" in 0|1) ;; *) die "--enc must be 0 or 1: $ENC" ;; esac
case "$SETS" in 0|1) ;; *) die "--sets must be 0 or 1: $SETS" ;; esac
case "$FLAGS" in ''|*[!0-9]*|0) die "--flags must be a positive number: $FLAGS" ;; esac
case "$LATENCY" in ''|*[!0-9]*) die "--latency must be a non-negative number: $LATENCY" ;; esac
case "$RUNS" in ''|*[!0-9]*) die "--runs must be a number: $RUNS" ;; esac
[ "$RUNS" -ge 10 ] || die "--runs must be at least 10: $RUNS"

GIT=$(git rev-parse --short HEAD 2>/dev/null || echo unknown)
SCENARIO="enc${ENC}-n${FLAGS}-sets${SETS}-lat${LATENCY}"
RUNNER="$PACKAGE/androidx.test.runner.AndroidJUnitRunner"

# Prints the output of one test; fails when the instrumentation did not finish cleanly.
instrument() {
  local test_name=$1 run=$2 out
  out=$("$ADB" shell am instrument -w -r \
    -e class "io.split.android.client.perf.ReadyTimingTest#$test_name" \
    -e perf 1 -e enc "$ENC" -e flags "$FLAGS" -e sets "$SETS" \
    -e latencyMs "$LATENCY" -e run "$run" -e git "$GIT" \
    "$RUNNER") || { printf '%s\n' "$out" >&2; die "$test_name failed: adb returned non-zero"; }
  if printf '%s\n' "$out" | grep -qE 'FAILURES|INSTRUMENTATION_CODE: 0|INSTRUMENTATION_STATUS_CODE: -[1-9]|shortMsg='; then
    printf '%s\n' "$out" >&2
    die "$test_name failed"
  fi
  if ! printf '%s\n' "$out" | grep -q 'INSTRUMENTATION_CODE: -1'; then
    printf '%s\n' "$out" >&2
    die "$test_name did not finish"
  fi
  printf '%s\n' "$out"
}

mkdir -p "$RESULTS_DIR"
STAMP=$(date +%Y%m%d-%H%M%S)
RESULTS="$RESULTS_DIR/$SCENARIO-$GIT-$STAMP.jsonl"
: > "$RESULTS"

if [ "$SEED" -eq 1 ]; then
  echo "seeding $SCENARIO"
  instrument seed 0 > /dev/null
fi

for ((run = 0; run < RUNS; run++)); do
  out=$(instrument launch "$run")
  json=$(printf '%s\n' "$out" | tr -d '\r' | sed -n 's/^INSTRUMENTATION_STATUS: splitPerf=//p' | head -n 1)
  [ -n "$json" ] || { printf '%s\n' "$out" >&2; die "launch $run produced no splitPerf result"; }
  printf '%s\n' "$json" >> "$RESULTS"
  echo "run $run done"
done

echo "results: $RESULTS"
python3 -I "$HERE/summarize-results.py" "$RESULTS"
