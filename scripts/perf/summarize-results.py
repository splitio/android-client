#!/usr/bin/env python3
"""Summarizes a results file of one JSON report per line.

The first run is dropped as a warm-up. The median is sorted[n / 2] and the p90 is the nearest
rank, sorted[ceil(0.9 * n) - 1], over the remaining runs.

Usage: python3 -I summarize-results.py <results.jsonl>
"""
import json
import math
import sys

METRICS = ("buildMs", "readyFromCacheMs", "readyMs")
WARM_UP = 1


def median(values):
    ordered = sorted(values)
    return ordered[len(ordered) // 2]


def p90(values):
    ordered = sorted(values)
    return ordered[math.ceil(0.9 * len(ordered)) - 1]


def summarize_file(path):
    with open(path) as f:
        reports = [json.loads(text) for text in f if text.strip()]
    kept = reports[WARM_UP:]
    if not kept:
        raise ValueError("no runs left after discarding the warm-up: %s" % path)
    summary = {"runs": len(kept)}
    for metric in METRICS:
        values = [report[metric] for report in kept]
        summary[metric] = {"median": median(values), "p90": p90(values)}
    return summary


def format_summary(summary):
    lines = ["runs: %d" % summary["runs"]]
    for metric in METRICS:
        lines.append("%s median=%.2f p90=%.2f" % (metric, summary[metric]["median"], summary[metric]["p90"]))
    return "\n".join(lines)


def main(argv):
    if len(argv) != 2:
        sys.stderr.write("usage: summarize-results.py <results.jsonl>\n")
        return 2
    try:
        print(format_summary(summarize_file(argv[1])))
    except (OSError, ValueError, KeyError) as e:
        sys.stderr.write("cannot summarize: %s\n" % e)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
