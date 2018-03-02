package io.split.android.client.cache;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import io.split.android.client.api.Key;
import io.split.android.client.dtos.MySegment;
import io.split.android.client.storage.IStorage;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

/**
 * Created by guillermo on 12/28/17.
 */

public class MySegmentsCache implements IMySegmentsCache {

    private static final String MY_SEGMENTS_FILE_PREFIX = "SPLITIO.mysegments";

    private final IStorage _storage;

    public MySegmentsCache(IStorage storage) {
        _storage = storage;
    }

    private String getMySegmentsId() {
        return MY_SEGMENTS_FILE_PREFIX;
    }

    private String getMySegmentsKeyId() {
        return MY_SEGMENTS_FILE_PREFIX + ".currentKey";
    }

    @Override
    public boolean saveMySegments(String key, List<MySegment> mySegments) {
        try {
            _storage.write(getMySegmentsKeyId(), key);
            _storage.write(getMySegmentsId(), Json.toJson(mySegments));
            return true;
        } catch (IOException e) {
            Logger.e(e, "Could not save my segments");
            return false;
        }
    }

    @Override
    public List<MySegment> getMySegments(String key) {
        try {
            String savedKey = _storage.read(getMySegmentsKeyId());
            if (savedKey.equals(key)) {
                String storedMySegments = _storage.read(getMySegmentsId());
                Type listType = new TypeToken<List<MySegment>>() {
                }.getType();

                return Json.fromJson(storedMySegments, listType);
            } else {
                _storage.delete(getMySegmentsId());
            }
        } catch (IOException e) {
            Logger.e(e, "Unable to get my segments");
        }
        return null;
    }

    @Override
    public void deleteMySegments() {
        _storage.delete(getMySegmentsId());
    }
}
