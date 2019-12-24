package io.split.android.client.storage.db.migrator;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.dtos.TestImpressions;
import io.split.android.client.impressions.StoredImpressions;
import io.split.android.client.storage.db.EventEntity;
import io.split.android.client.storage.db.ImpressionEntity;
import io.split.android.client.storage.db.StorageRecordStatus;
import io.split.android.client.storage.impressions.PersistentImpressionsStorage;
import io.split.android.client.storage.legacy.ImpressionsStorageManager;
import io.split.android.client.storage.legacy.TrackStorageManager;
import io.split.android.client.track.EventsChunk;
import io.split.android.client.utils.Json;

import static com.google.common.base.Preconditions.checkNotNull;

public class ImpressionsMigratorHelper {
    ImpressionsStorageManager mImpressionsStorageManager;

    public ImpressionsMigratorHelper(@NotNull ImpressionsStorageManager impressionsStorageManager) {
        mImpressionsStorageManager = checkNotNull(impressionsStorageManager);
    }

    public List<ImpressionEntity> loadLegacyImpressionsAsEntities() {
        List<StoredImpressions> impressionsChuncks = mImpressionsStorageManager.getStoredImpressions();
        List<ImpressionEntity> entities = new ArrayList<>();
        for(StoredImpressions chunk : impressionsChuncks) {
            List<TestImpressions> testImpressions = chunk.impressions();
            for(TestImpressions testImpression : testImpressions) {
                List<KeyImpression> impressions = testImpression.keyImpressions;
                for(KeyImpression impression : impressions) {
                    ImpressionEntity impressionEntity = createImpressionEntity(impression);
                    entities.add(impressionEntity);
                }
            }
        }
        return entities;
    }

    private ImpressionEntity createImpressionEntity(KeyImpression impression) {
        ImpressionEntity entity = new ImpressionEntity();
        entity.setTestName(impression.feature);
        entity.setBody(Json.toJson(impression));
        entity.setCreatedAt(impression.time);
        entity.setStatus(StorageRecordStatus.ACTIVE);
        return entity;
    }
}
