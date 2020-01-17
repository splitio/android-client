package storage.migrator.mocks;

import java.util.List;

import io.split.android.client.storage.db.ImpressionEntity;
import io.split.android.client.storage.db.migrator.ImpressionsMigratorHelper;

public class ImpressionsMigratorHelperMock implements ImpressionsMigratorHelper {
    List<ImpressionEntity> mEntities;

    public void setImpressions(List<ImpressionEntity> entities) {
        mEntities = entities;
    }

    @Override
    public List<ImpressionEntity> loadLegacyImpressionsAsEntities() {
        return mEntities;
    }
}
