package io.split.android.client.cache;



import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.split.android.client.dtos.MySegment;
import io.split.android.client.storage.legacy.IStorage;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

/**
 * Created by guillermo on 12/28/17.
 */

public class MySegmentsCache implements IMySegmentsCache {

    private static final String MY_SEGMENTS_FILE_NAME = "SPLITIO.mysegments";

    private final IStorage mFileStorage;
    private Map<String, List<MySegment>> mSegments;

    public MySegmentsCache(IStorage storage) {
        mFileStorage = storage;
        mSegments = new ConcurrentHashMap<>(new HashMap<String, List<MySegment>>());
        loadSegmentsFromDisk();
    }

    private String getMySegmentsFileName() {
        return MY_SEGMENTS_FILE_NAME;
    }

    @Override
    public void setMySegments(String key, List<MySegment> mySegments) {
        mSegments.put(key, mySegments);
    }

    @Override
    public List<MySegment> getMySegments(String key) {
        return mSegments.get(key);
    }

    @Override
    public void deleteMySegments(String key) {
        mSegments.remove(key);
    }

    // TODO: This methods should use new db storage implementation
    private void loadSegmentsFromDisk(){

        try {
            String storedMySegments = mFileStorage.read(getMySegmentsFileName());
            if(storedMySegments == null || storedMySegments.trim().equals("")) return;
            Type listType = new TypeToken<Map<String, List<MySegment>>>() {
            }.getType();

            Map<String, List<MySegment>> segments = Json.fromJson(storedMySegments, listType);
            Set<String> keys = segments.keySet();
            for (String key : keys) {
                List<MySegment> keySegments = segments.get(key);
                if(keySegments != null) {
                    mSegments.put(key, Collections.synchronizedList(keySegments));
                }
            }

        } catch (IOException e) {
            Logger.e(e, "Unable to get my segments");
        } catch (JsonSyntaxException syntaxException) {
            Logger.e(syntaxException, "Unable to parse saved segments");
        }
    }

    // TODO: This methods should use new db storage implementation
    public void saveToDisk() {
        try {
            String json = Json.toJson(mSegments);
            mFileStorage.write(getMySegmentsFileName(), json);
        } catch (IOException e) {
            Logger.e(e, "Could not save my segments");
        } catch (JsonSyntaxException syntaxException) {
            Logger.e(syntaxException, "Unable to parse segments to save");
        }
    }

}
