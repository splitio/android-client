package io.split.android.client.storage.db.migrator;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.split.android.client.cache.MySegmentsCacheMigrator;
import io.split.android.client.dtos.MySegment;
import io.split.android.client.storage.db.MySegmentEntity;
import io.split.android.client.utils.StringHelper;
import io.split.android.client.utils.TimeUtils;

import static com.google.common.base.Preconditions.checkNotNull;

public class MySegmentsMigratorHelperImpl implements MySegmentsMigratorHelper {
    MySegmentsCacheMigrator mMySegmentsCacheMigrator;
    TimeUtils mTimeUtils;
    StringHelper mStringHelper;

    public MySegmentsMigratorHelperImpl(@NotNull MySegmentsCacheMigrator mySegmentsCacheMigrator,
                                        StringHelper stringHelper) {
        mMySegmentsCacheMigrator = checkNotNull(mySegmentsCacheMigrator);
        mStringHelper = checkNotNull(stringHelper);
        mTimeUtils = new TimeUtils();
    }

    @Override
    public List<MySegmentEntity> loadLegacySegmentsAsEntities() {
        Map<String, List<MySegment>> mySegments = mMySegmentsCacheMigrator.getAllMySegments();
        List<MySegmentEntity> entities = new ArrayList<>();
        for(Map.Entry<String, List<MySegment>> entry : mySegments.entrySet()) {
            MySegmentEntity mySegmentEntity = createMySegmentEntity(entry.getKey(), entry.getValue());
            entities.add(mySegmentEntity);
        }
        mMySegmentsCacheMigrator.deleteAllFiles();
        return entities;
    }

    private MySegmentEntity createMySegmentEntity(String key, List<MySegment> mySegments) {
        MySegmentEntity entity = new MySegmentEntity();
        entity.setUserKey(key);
        entity.setSegmentList(joinMySegmentsNames(mySegments));
        entity.setUpdatedAt(mTimeUtils.timeInSeconds());
        return entity;
    }

    private String joinMySegmentsNames(List<MySegment> mySegments) {
        List<String> names = new ArrayList<>();
        for(MySegment mySegment : mySegments) {
            names.add(mySegment.name);
        }
        return mStringHelper.join(",", names);
    }
}
