package io.split.android.client.storage.db.migrator;

import androidx.core.util.Pair;

import java.util.List;

import io.split.android.client.storage.db.SplitEntity;

public interface SplitsMigratorHelper {
    Pair<Long, List<SplitEntity>> loadLegacySplitsAsEntities();
}
