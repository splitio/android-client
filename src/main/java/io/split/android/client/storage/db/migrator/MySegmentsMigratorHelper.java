package io.split.android.client.storage.db.migrator;

import java.util.List;

import io.split.android.client.storage.db.MySegmentEntity;

public interface MySegmentsMigratorHelper {
    List<MySegmentEntity> loadLegacySegmentsAsEntities();
}
