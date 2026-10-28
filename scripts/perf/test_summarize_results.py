import importlib.util
import json
import os
import tempfile
import unittest

_SPEC = importlib.util.spec_from_file_location(
    "summarize_results", os.path.join(os.path.dirname(os.path.abspath(__file__)), "summarize-results.py"))
summarize_results = importlib.util.module_from_spec(_SPEC)
_SPEC.loader.exec_module(summarize_results)


def line(run, build, cache, ready):
    return json.dumps({"run": run, "buildMs": build, "readyFromCacheMs": cache, "readyMs": ready})


class SummarizeTest(unittest.TestCase):
    def test_median_is_sorted_n_over_two(self):
        self.assertEqual(3, summarize_results.median([5, 1, 3, 2, 4]))
        self.assertEqual(3, summarize_results.median([4, 3, 2, 1]))

    def test_p90_is_nearest_rank(self):
        values = list(range(1, 11))
        self.assertEqual(9, summarize_results.p90(values))
        self.assertEqual(10, summarize_results.p90(list(range(1, 12))))

    def test_first_run_is_discarded_as_warm_up(self):
        lines = [line(0, 1000.0, 1000.0, 1000.0)] + [line(i, 10.0 + i, 20.0, 30.0) for i in range(1, 11)]
        with tempfile.NamedTemporaryFile("w", suffix=".jsonl", delete=False) as f:
            f.write("\n".join(lines) + "\n")
            path = f.name
        try:
            summary = summarize_results.summarize_file(path)
        finally:
            os.unlink(path)
        self.assertEqual(10, summary["runs"])
        self.assertEqual(16.0, summary["buildMs"]["median"])
        self.assertEqual(19.0, summary["buildMs"]["p90"])
        self.assertEqual(20.0, summary["readyFromCacheMs"]["median"])

    def test_empty_after_warm_up_is_an_error(self):
        with tempfile.NamedTemporaryFile("w", suffix=".jsonl", delete=False) as f:
            f.write(line(0, 1.0, 1.0, 1.0) + "\n")
            path = f.name
        try:
            with self.assertRaises(ValueError):
                summarize_results.summarize_file(path)
        finally:
            os.unlink(path)

    def test_report_names_every_metric_and_the_run_count(self):
        summary = {"runs": 3, "buildMs": {"median": 1.0, "p90": 2.0},
                   "readyFromCacheMs": {"median": 3.0, "p90": 4.0},
                   "readyMs": {"median": 5.0, "p90": 6.0}}
        text = summarize_results.format_summary(summary)
        self.assertIn("runs: 3", text)
        self.assertIn("buildMs median=1.00 p90=2.00", text)
        self.assertIn("readyFromCacheMs median=3.00 p90=4.00", text)
        self.assertIn("readyMs median=5.00 p90=6.00", text)


if __name__ == "__main__":
    unittest.main()
