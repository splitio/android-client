package io.split.android.client.storage.db;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Datasets whose writes were refused on a database, for the rest of the process.
 * While a dataset is refused, its change number is persisted as -1.
 */
@RestrictTo(LIBRARY)
public final class RefusedWrites {

    private static final Set<SplitRoomDatabase> FLAGS = newSet();
    private static final Set<SplitRoomDatabase> RULE_BASED_SEGMENTS = newSet();

    public static void refuseFlags(@NonNull SplitRoomDatabase database) {
        FLAGS.add(database);
    }

    public static boolean flagsRefused(@NonNull SplitRoomDatabase database) {
        return FLAGS.contains(database);
    }

    public static void refuseRuleBasedSegments(@NonNull SplitRoomDatabase database) {
        RULE_BASED_SEGMENTS.add(database);
    }

    public static boolean ruleBasedSegmentsRefused(@NonNull SplitRoomDatabase database) {
        return RULE_BASED_SEGMENTS.contains(database);
    }

    @VisibleForTesting
    public static void resetForTesting() {
        FLAGS.clear();
        RULE_BASED_SEGMENTS.clear();
    }

    // ConcurrentHashMap.newKeySet() needs API 24
    private static Set<SplitRoomDatabase> newSet() {
        return Collections.synchronizedSet(
                Collections.newSetFromMap(new WeakHashMap<SplitRoomDatabase, Boolean>()));
    }

    private RefusedWrites() {
    }
}
