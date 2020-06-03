package tests.storage.migrator.mocks;

import java.util.List;

import io.split.android.client.storage.db.EventEntity;
import io.split.android.client.storage.db.migrator.EventsMigratorHelper;

public class EventsMigratorHelperMock implements EventsMigratorHelper {
    private List<EventEntity> mEntities;

    public void setEvents(List<EventEntity> entities) {
        mEntities = entities;
    }

    @Override
    public List<EventEntity> loadLegacyEventsAsEntities() {
        return mEntities;
    }
}
