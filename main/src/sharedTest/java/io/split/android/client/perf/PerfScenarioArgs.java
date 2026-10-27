package io.split.android.client.perf;

/**
 * The launch arguments that are not part of the cache seed. A null argument takes its default;
 * anything malformed is rejected. Pure Java, so it runs on the JVM.
 */
public final class PerfScenarioArgs {

    public final int latencyMs;
    public final int run;
    public final String git;

    private PerfScenarioArgs(int latencyMs, int run, String git) {
        this.latencyMs = latencyMs;
        this.run = run;
        this.git = git;
    }

    public static PerfScenarioArgs parse(String latencyMs, String run, String git) {
        int latency = nonNegative("latencyMs", latencyMs);
        return new PerfScenarioArgs(latency, nonNegative("run", run), git == null ? "unknown" : git);
    }

    private static int nonNegative(String name, String value) {
        if (value == null) {
            return 0;
        }
        int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " must be a non-negative number: " + value);
        }
        if (parsed < 0) {
            throw new IllegalArgumentException(name + " must be a non-negative number: " + value);
        }
        return parsed;
    }
}
