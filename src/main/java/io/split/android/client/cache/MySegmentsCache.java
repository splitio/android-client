package io.split.android.client.cache;

import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import io.split.android.client.api.Key;
import io.split.android.client.dtos.MySegment;
import io.split.android.client.storage.IStorage;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;
import io.split.android.client.utils.Utils;

/**
 * Created by guillermo on 12/28/17.
 */

public class MySegmentsCache implements IMySegmentsCache, LifecycleObserver {

    private static final String MY_SEGMENTS_FILE_PREFIX = "SPLITIO.mysegments";

    private final IStorage _storage;
    private final Set<String> _segments;

    public MySegmentsCache(IStorage storage) {
        _storage = storage;
        _segments = Collections.synchronizedSet(new HashSet<>());
    }

    private String getMySegmentsId(String key) {
        return MY_SEGMENTS_FILE_PREFIX + "_" + Utils.sanitizeForFileName(key);
    }

    private String getMySegmentsKeyId() {
        return MY_SEGMENTS_FILE_PREFIX + ".currentKey";
    }

    @Override
    public boolean saveMySegments(String key, List<MySegment> mySegments) {
        try {
            _storage.write(getMySegmentsKeyId(), key);
            _storage.write(getMySegmentsId(key), Json.toJson(mySegments));
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
            if (savedKey != null && savedKey.equals(key)) {
                String storedMySegments = _storage.read(getMySegmentsId(key));
                Type listType = new TypeToken<List<MySegment>>() {
                }.getType();

                return Json.fromJson(storedMySegments, listType);
            } else {
                _storage.delete(getMySegmentsId(key));
            }
        } catch (IOException e) {
            Logger.e(e, "Unable to get my segments");
        }
        return null;
    }

    @Override
    public void deleteMySegments(String key) {
        _storage.delete(getMySegmentsId(key));
    }


    public void loadFromFile(String key) {
        try {
            String savedKey = _storage.read(getMySegmentsKeyId());
            if (savedKey != null && savedKey.equals(key)) {
                String storedMySegments = _storage.read(getMySegmentsId());
                Type listType = new TypeToken<List<MySegment>>() {
                }.getType();

                List<String> segments = Json.fromJson(storedMySegments, listType);
                _segments.addAll(segments);

            } else {
                _storage.delete(getMySegmentsId());
            }
        } catch (IOException e) {
            Logger.e(e, "Unable to get my segments. It could be no segments for this key on disk");
        }
    }

    // Lifecyle observer
    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void disconnectListener() {

    }

}
