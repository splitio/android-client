# Fallback module

This module contains the fallback treatment logic for the Split SDK.

It provides the types and resolution strategy used when the SDK is unable to evaluate a feature flag normally (e.g. during initialization or on error), returning a configured fallback treatment instead of the default "control".

Key types:
- `FallbackTreatmentsConfiguration` — builder for configuring global and per-flag fallbacks
- `FallbackTreatment` — represents a fallback treatment value with optional config and label
- `FallbackTreatmentsCalculator` — resolves the applicable fallback for a given flag name
