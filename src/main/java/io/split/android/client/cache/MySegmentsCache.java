package io.split.android.client.cache;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

import io.split.android.client.dtos.MySegment;
import io.split.android.client.storage.IStorage;
import io.split.android.client.utils.Json;

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

    @Override
    public boolean saveMySegments(List<MySegment> mySegments) {
        try {
            _storage.write(getMySegmentsId(), Json.toJson(mySegments));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<MySegment> getMySegments() {
        try {
            String storedMySegments = _storage.read(getMySegmentsId());
            Type listType = new TypeToken<List<MySegment>>() {
            }.getType();

            return Json.fromJson(storedMySegments, listType);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
