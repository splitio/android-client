package io.split.android.client;

import java.util.Locale;

/**
 * Describes a cache seed for the timing harness. Pure Java apart from the database name, so the
 * rules run on the JVM. Lives in the package of {@link SplitFactoryHelper} to reach its
 * package-private database naming.
 */
public final class PerfSeedSpec {

    /** A throwaway key of 27 characters; the cipher needs at least 16 for its IV. Database: perf0001. */
    public static final String SDK_KEY = "perf000aaaabbbbccccdddd0001";
    public static final String USER_KEY = "perf_user_1";
    public static final long FLAGS_CN = 1_700_000_000_000L;
    public static final long RBS_CN = 1_700_000_000_000L;
    public static final long MEMBERSHIPS_CN = 1_700_000_000_000L;

    /** The segments the seeded user belongs to; the stub server answers memberships with the same. */
    public static final String[] SEGMENTS = {"seg_0", "seg_1", "seg_2", "seg_3", "seg_4",
            "seg_5", "seg_6", "seg_7", "seg_8", "seg_9"};

    private static final int DEFAULT_FLAGS = 1000;
    private static final int FLAGS_PER_RULE_BASED_SEGMENT = 20;

    private PerfSeedSpec() {
    }

    public static final class Spec {
        public final boolean encrypted;
        public final int flags;
        public final int rbs;
        public final boolean flagSets;

        public Spec(boolean encrypted, int flags, boolean flagSets) {
            this.encrypted = encrypted;
            this.flags = flags;
            this.rbs = Math.max(1, flags / FLAGS_PER_RULE_BASED_SEGMENT);
            this.flagSets = flagSets;
        }

        /**
         * Builds a spec from the instrumentation arguments. A null argument takes its default
         * (not encrypted, 1000 flags, no flag sets); anything malformed is rejected.
         */
        public static Spec parse(String enc, String flags, String sets) {
            boolean encrypted = parseSwitch("enc", enc);
            boolean flagSets = parseSwitch("sets", sets);
            int count = DEFAULT_FLAGS;
            if (flags != null) {
                try {
                    count = Integer.parseInt(flags);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("flags must be a number: " + flags);
                }
                if (count <= 0) {
                    throw new IllegalArgumentException("flags must be positive: " + flags);
                }
            }
            return new Spec(encrypted, count, flagSets);
        }

        public String id() {
            return "enc" + (encrypted ? 1 : 0) + "-n" + flags + "-sets" + (flagSets ? 1 : 0);
        }

        private static boolean parseSwitch(String name, String value) {
            if (value == null || value.equals("0")) {
                return false;
            }
            if (value.equals("1")) {
                return true;
            }
            throw new IllegalArgumentException(name + " must be 0 or 1: " + value);
        }
    }

    public static String flagName(int i) {
        return String.format(Locale.US, "flag_%05d", i);
    }

    /** The database name the factory derives for {@link #SDK_KEY}. */
    public static String databaseName(SplitClientConfig config) {
        return SplitFactoryHelper.buildDatabaseName(config, SDK_KEY);
    }

    public static void requireCount(String table, long actual, long expected) {
        if (actual != expected) {
            throw new IllegalStateException(table + ": " + actual + " rows, expected " + expected);
        }
    }

    public static void requireLong(String name, long actual, long expected) {
        if (actual != expected) {
            throw new IllegalStateException(name + " is " + actual + ", expected " + expected);
        }
    }
}
