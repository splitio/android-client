package io.split.android.fake;

import java.util.Map;

import io.split.android.client.impressions.IImpressionsStorage;
import io.split.android.client.impressions.StoredImpressions;
import io.split.android.client.storage.MemoryStorage;

public class ImpressionsFileStorageStub extends MemoryStorage implements IImpressionsStorage {

    @Override
    public Map<String, StoredImpressions> read() {
        return null;
    }

    @Override
    public void write(Map<String, StoredImpressions> impressions) {

    }
}
