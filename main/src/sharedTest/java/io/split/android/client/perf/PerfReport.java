package io.split.android.client.perf;

import com.google.gson.JsonObject;

/**
 * Builds the one-line JSON a launch reports. Times come in as nanoseconds and go out as
 * milliseconds. Pure Java apart from Gson, so it runs on the JVM.
 */
public final class PerfReport {

    public static final String SCHEMA = "split-perf/1";

    private static final double NANOS_PER_MILLI = 1_000_000.0;

    private PerfReport() {
    }

    public static String toJson(String git, int run, String seed, int flags, int rbs, boolean encrypted,
                                boolean sets, int latencyMs, JsonObject device, int pid,
                                long buildNs, long readyFromCacheNs, long readyNs,
                                String readyFromCachePath, Long firstFlagsSince, int splitNames,
                                JsonObject seedManifest) {
        JsonObject scenario = new JsonObject();
        scenario.addProperty("seed", seed);
        scenario.addProperty("enc", encrypted);
        scenario.addProperty("flags", flags);
        scenario.addProperty("rbs", rbs);
        scenario.addProperty("sets", sets);
        scenario.addProperty("latencyMs", latencyMs);

        JsonObject report = new JsonObject();
        report.addProperty("schema", SCHEMA);
        report.addProperty("git", git);
        report.addProperty("run", run);
        report.add("scenario", scenario);
        report.add("device", device);
        report.addProperty("pid", pid);
        report.addProperty("buildMs", buildNs / NANOS_PER_MILLI);
        report.addProperty("readyFromCacheMs", readyFromCacheNs / NANOS_PER_MILLI);
        report.addProperty("readyMs", readyNs / NANOS_PER_MILLI);
        report.addProperty("readyFromCachePath", readyFromCachePath);
        report.addProperty("firstFlagsSince", firstFlagsSince);
        report.addProperty("splitNames", splitNames);
        report.add("seedManifest", seedManifest);
        return report.toString();
    }
}
