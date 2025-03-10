package io.split.android.client.service.executor;

import io.split.android.client.dtos.Split;
import io.split.android.client.service.CleanUpDatabaseTask;
import io.split.android.client.service.events.EventsRecorderTask;
import io.split.android.client.service.impressions.ImpressionsTaskFactory;
import io.split.android.client.service.rules.LoadRuleBasedSegmentsTask;
import io.split.android.client.service.splits.FilterSplitsInCacheTask;
import io.split.android.client.service.splits.LoadSplitsTask;
import io.split.android.client.service.splits.SplitInPlaceUpdateTask;
import io.split.android.client.service.splits.SplitKillTask;
import io.split.android.client.service.splits.SplitsSyncTask;
import io.split.android.client.service.splits.SplitsUpdateTask;
import io.split.android.client.service.telemetry.TelemetryTaskFactory;
import io.split.android.client.storage.cipher.EncryptionMigrationTask;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.SplitRoomDatabase;

public interface SplitTaskFactory extends TelemetryTaskFactory, ImpressionsTaskFactory {

    EventsRecorderTask createEventsRecorderTask();

    SplitsSyncTask createSplitsSyncTask(boolean checkCacheExpiration);

    LoadSplitsTask createLoadSplitsTask();

    LoadRuleBasedSegmentsTask createLoadRuleBasedSegmentsTask();

    SplitKillTask createSplitKillTask(Split split);

    SplitsUpdateTask createSplitsUpdateTask(Long since, Long rbsSince);

    SplitInPlaceUpdateTask createSplitsUpdateTask(Split featureFlag, long since);

    FilterSplitsInCacheTask createFilterSplitsInCacheTask();

    CleanUpDatabaseTask createCleanUpDatabaseTask(long maxTimestamp);

    EncryptionMigrationTask createEncryptionMigrationTask(String sdkKey, SplitRoomDatabase splitRoomDatabase, boolean encryptionEnabled, SplitCipher splitCipher);
}
