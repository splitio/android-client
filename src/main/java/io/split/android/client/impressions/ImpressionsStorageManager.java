package io.split.android.client.impressions;

import android.annotation.SuppressLint;

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

/**
 * Created by guillermo on 1/18/18.
 */

public class ImpressionsStorageManager {

    private IStorage _storage;


    public ImpressionsStorageManager(IStorage storage) {
        _storage = storage;
    }

    @SuppressLint("DefaultLocale")
    public String writeChunk(List<KeyImpression> impressions) {
        if (impressions == null || impressions.isEmpty()) {
            return null; // Nothing to write
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

        String entity = Json.toJson(impressions);

        String chunkId = String.format("impressions_%d_0.json", System.currentTimeMillis());

        try {
            _storage.write(chunkId, entity);
            return chunkId;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String readStringChunk(String chunkId) {
        try {
            return _storage.read(chunkId);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String[] getAllChunkNames() {
        String[] names = _storage.getAllIds();
        for (String chunkId :
                names) {
            int idxStart = chunkId.indexOf("_");
            int idxEnd = chunkId.lastIndexOf("_");

            String timestampStr = chunkId.substring(idxStart + 1, idxEnd);

            long diff = System.currentTimeMillis() - Long.parseLong(timestampStr);

            long oneDayMillis = 3600 * 1000;
            if (diff > oneDayMillis) {
                _storage.delete(chunkId);
            }
        }

        return _storage.getAllIds();
    }

    public void chunkSucceeded(String chunkId) {
        _storage.delete(chunkId);
    }

    @SuppressLint("DefaultLocale")
    public void chunkFailed(String chunkId) {
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
