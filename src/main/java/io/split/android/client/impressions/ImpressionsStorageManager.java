package io.split.android.client.impressions;

import android.annotation.SuppressLint;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.dtos.TestImpressions;
import io.split.android.client.storage.IStorage;
import io.split.android.client.utils.Json;
import timber.log.Timber;

/**
 * Created by guillermo on 1/18/18.
 */

public class ImpressionsStorageManager {

    private static final String IMPRESSIONS_CHUNK_FILE_PREFIX = "impressions";

    private IStorage _storage;


    public ImpressionsStorageManager(IStorage storage) {
        _storage = storage;
    }

    @SuppressLint("DefaultLocale")
    public void storeImpressions(List<KeyImpression> impressions) throws IOException {
        if (impressions == null || impressions.isEmpty()) {
            return; // Nothing to write
        }

        Map<String, List<KeyImpression>> tests = new HashMap<>();

        for (KeyImpression ki : impressions) {
            List<KeyImpression> impressionsForTest = tests.get(ki.feature);
            if (impressionsForTest == null) {
                impressionsForTest = new ArrayList<>();
                tests.put(ki.feature, impressionsForTest);
            }
            impressionsForTest.add(ki);
        }

        List<TestImpressions> toShip = Lists.newArrayList();

        for (Map.Entry<String, List<KeyImpression>> entry : tests.entrySet()) {
            String testName = entry.getKey();
            List<KeyImpression> keyImpressions = entry.getValue();

            TestImpressions testImpressionsDTO = new TestImpressions();
            testImpressionsDTO.testName = testName;
            testImpressionsDTO.keyImpressions = keyImpressions;

            toShip.add(testImpressionsDTO);
        }

        String entity = Json.toJson(toShip);

        Timber.d("Entity to store: %s", entity);

        String chunkId = String.format("%s_%d_0.json", IMPRESSIONS_CHUNK_FILE_PREFIX, System.currentTimeMillis());

        _storage.write(chunkId, entity);
    }

    public List<StoredImpressions> getStoredImpressions() {
        List<String> ids = getAllChunkIds();
        List<StoredImpressions> result = Lists.newArrayList();
        for(String id: ids) {
            String stored = readStringChunk(id);
            List<TestImpressions> testImpressions = Json.fromJsonList(stored, TestImpressions.class);
            result.add(StoredImpressions.from(id, testImpressions));
        }
        return result;
    }

    public void failedStoredImpression(StoredImpressions storedImpression) {
        if (chunkCanBeStored(storedImpression)) {
            chunkFailed(storedImpression.id());
        }
    }

    public void succeededStoredImpression(StoredImpressions storedImpression) {
        if (chunkCanBeStored(storedImpression)) {
            chunkSucceeded(storedImpression.id());
        }
    }

    private boolean chunkCanBeStored(StoredImpressions storedImpressions) {
        if (storedImpressions == null) {
            return false;
        }
        if (storedImpressions.id() == null || storedImpressions.id().isEmpty()) {
            return false;
        }
        if (storedImpressions.impressions() == null || storedImpressions.impressions().isEmpty()) {
            return false;
        }
        return true;
    }


    private String readStringChunk(String chunkId) {
        try {
            return _storage.read(chunkId);
        } catch (IOException e) {
            Timber.e(e, "Could not read chunk %s", chunkId);
        }
        return null;
    }

    private List<String> getAllChunkIds() {
        List<String> names = Lists.newArrayList(_storage.getAllIds());
        List<String> chunkIds = Lists.newArrayList();

        for (String name :
                names) {
            if (name.startsWith(IMPRESSIONS_CHUNK_FILE_PREFIX)) {
                chunkIds.add(name);
            }
        }

        List<String> resultChunkIds = Lists.newArrayList(chunkIds);

        for (String chunkId :
                chunkIds) {
            int idxStart = chunkId.indexOf("_");
            int idxEnd = chunkId.lastIndexOf("_");

            String timestampStr = chunkId.substring(idxStart + 1, idxEnd);

            long diff = System.currentTimeMillis() - Long.parseLong(timestampStr);

            long oneDayMillis = 3600 * 1000;
            if (diff > oneDayMillis) {
                resultChunkIds.remove(chunkId);
                _storage.delete(chunkId);
            }
        }

        return resultChunkIds;
    }

    private void chunkSucceeded(String chunkId) {
        _storage.delete(chunkId);
    }

    @SuppressLint("DefaultLocale")
    private void chunkFailed(String chunkId) {
        if (Strings.isNullOrEmpty(chunkId)) {
            return;
        }
        int idxStart = chunkId.lastIndexOf("_");
        int idxEnd = chunkId.lastIndexOf(".json");
        String attemptsStr = chunkId.substring(idxStart + 1, idxEnd);
        int attempt = Integer.parseInt(attemptsStr);
        if (attempt >= 3) {
            _storage.delete(chunkId);
        } else {
            String oldPart = String.format("_%d.json", attempt);
            String newPart = String.format("_%d.json", attempt + 1);
            String newChunkId = chunkId.replace(oldPart, newPart);
            _storage.rename(chunkId, newChunkId);
        }
    }
}
