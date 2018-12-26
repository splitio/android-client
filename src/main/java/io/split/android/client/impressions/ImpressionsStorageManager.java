package io.split.android.client.impressions;

import android.annotation.SuppressLint;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.dtos.MySegment;
import io.split.android.client.dtos.TestImpressions;
import io.split.android.client.storage.IStorage;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

/**
 * Created by guillermo on 1/18/18.
 */

public class ImpressionsStorageManager implements LifecycleObserver {

    private static final String IMPRESSIONS_FILE_NAME = "SPLITIO.impressions";

    private IStorage mFileStorage;
    Map<String, StoredImpressions> mImpressionsToSend;

    public ImpressionsStorageManager(IStorage storage) {
        mFileStorage = storage;
        mImpressionsToSend = Collections.synchronizedMap(new HashMap<>());
        loadImpressionsFromDisk();
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

        String chunkId = UUID.randomUUID().toString();
        mImpressionsToSend.put(chunkId, StoredImpressions.from(chunkId, toShip, System.currentTimeMillis()));
    }

    public List<StoredImpressions> getStoredImpressions() {
        return new ArrayList<>(mImpressionsToSend.values());
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
            return mFileStorage.read(chunkId);
        } catch (IOException e) {
            Logger.e(e, "Could not read chunk %s", chunkId);
        }
        return null;
    }

    private void chunkSucceeded(String chunkId) {
        mImpressionsToSend.remove(chunkId);
    }

    @SuppressLint("DefaultLocale")
    private void chunkFailed(String chunkId) {

        if (Strings.isNullOrEmpty(chunkId)) {
            return;
        }

        StoredImpressions failedChunk = mImpressionsToSend.get(chunkId);
        if (failedChunk.getAttempts() >= 3 || failedChunk.isDeprecated()) {
            mImpressionsToSend.remove(chunkId);
        } else {
            failedChunk.addAttempt();
        }

    }

    private void loadImpressionsFromDisk(){

        try {
            String storedImpressions = mFileStorage.read(IMPRESSIONS_FILE_NAME);
            if(storedImpressions == null || storedImpressions.trim().equals("")) return;

            Type dataType = new TypeToken<Map<String, StoredImpressions>>() {
            }.getType();

            Map<String, StoredImpressions> impressions = Json.fromJson(storedImpressions, dataType);
            for (Map.Entry<String, StoredImpressions> entry : impressions.entrySet()) {
                if(!entry.getValue().isDeprecated()){
                    mImpressionsToSend.put(entry.getKey(), entry.getValue());
                }
            }

        } catch (IOException e) {
            Logger.e(e, "Unable to load impressions from disk: " + e.getLocalizedMessage());
        } catch (JsonSyntaxException syntaxException) {
            Logger.e(syntaxException, "Unable to parse saved impressions: " + syntaxException.getLocalizedMessage());
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private void saveToDisk() {
        try {
            String json = Json.toJson(mImpressionsToSend);
            mFileStorage.write(IMPRESSIONS_FILE_NAME, json);
        } catch (IOException e) {
            Logger.e(e, "Could not save my segments");
        } catch (JsonSyntaxException syntaxException) {
            Logger.e(syntaxException, "Unable to parse segments to save");
        }
    }
}
