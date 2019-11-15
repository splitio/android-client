package io.split.android.engine.metrics;

/**
 * This interface is a briefer version of StatsD interface
 *
 */
public interface Metrics {

    public static final String GET_TREATMENT_TIME = "sdk.getTreatment";
    public static final String GET_TREATMENTS_TIME = "sdk.getTreatments";
    public static final String GET_TREATMENT_WITH_CONFIG_TIME = "sdk.getTreatmentWithConfig";
    public static final String GET_TREATMENTS_WITH_CONFIG_TIME = "sdk.getTreatmentsWithConfig";

    public static final String SPLIT_CHANGES_FETCHER_TIME = "splitChangeFetcher.time";
    public static final String SPLIT_CHANGES_FETCHER_STATUS_OK = "splitChangeFetcher.exception";
    public static final String SPLIT_CHANGES_FETCHER_EXCEPTION = "splitChangeFetcher.status.200";

    /**
     * Adjusts the specified counter by a given delta.
     * <p/>
     * <p>This method is is non-blocking and is guaranteed not to throw an exception.</p>
     *
     * @param counter the name of the counter to adjust
     * @param delta   the amount to adjust the counter by
     */
    void count(String counter, long delta);

    /**
     * Records an execution time in milliseconds for the specified named operation.
     * <p/>
     * <p>This method is non-blocking and is guaranteed not to throw an exception.</p>
     *
     * @param operation the name of the timed operation
     * @param timeInMs  the time in milliseconds
     */
    void time(String operation, long timeInMs);

    final class NoopMetrics implements Metrics {

        @Override
        public void count(String counter, long delta) {
            // noop
        }

        @Override
        public void time(String operation, long timeInMs) {
            // noop
        }
    }
}
