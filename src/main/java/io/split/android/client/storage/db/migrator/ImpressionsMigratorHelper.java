package io.split.android.client.storage.db.migrator;

import java.util.List;

import io.split.android.client.storage.db.ImpressionEntity;

public interface ImpressionsMigratorHelper {
    List<ImpressionEntity> loadLegacyImpressionsAsEntities();
}
