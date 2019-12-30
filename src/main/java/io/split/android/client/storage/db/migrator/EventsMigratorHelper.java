package io.split.android.client.storage.db.migrator;

import java.util.List;

import io.split.android.client.storage.db.EventEntity;

public interface EventsMigratorHelper {
    List<EventEntity> loadLegacyEventsAsEntities();
}
