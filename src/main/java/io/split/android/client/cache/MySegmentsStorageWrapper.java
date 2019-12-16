package io.split.android.client.cache;

import androidx.annotation.NonNull;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.split.android.client.dtos.MySegment;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by Javier Avrudsky on 16/12/2019.
 */

public class MySegmentsStorageWrapper implements IMySegmentsCache {

    private final MySegmentsStorage mMySegmentsStorage;

    public MySegmentsStorageWrapper(@NonNull MySegmentsStorage mySegmentsStorage) {
        mMySegmentsStorage = checkNotNull(mySegmentsStorage);
    }

    @Override
    public void setMySegments(String key, List<MySegment> mySegments) {
        mMySegmentsStorage.set(buildMySegmentNames(mySegments));
    }

    @Override
    public List<MySegment> getMySegments(String key) {
        return buildMySegments(mMySegmentsStorage.getAll());
    }

    @Override
    public void deleteMySegments(String key) {
        mMySegmentsStorage.clear();
    }

    @Override
    public void saveToDisk() {
    }

    private List<String> buildMySegmentNames(List<MySegment> mySegments) {
        List<String> names = new ArrayList<>();
        for(MySegment mySegment : mySegments) {
            names.add(mySegment.name);
        }
        return names;
    }

    private List<MySegment> buildMySegments(Set<String> names) {
        List<MySegment> mySegments = new ArrayList<>();
        for(String name : names) {
            MySegment mySegment = new MySegment();
            mySegment.id = UUID.randomUUID().toString();
            mySegment.name = name;
            mySegments.add(mySegment);
        }
        return mySegments;
    }
}
