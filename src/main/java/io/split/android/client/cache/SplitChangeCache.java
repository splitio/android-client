package io.split.android.client.cache;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.service.splits.SplitChangeProcessor;
import io.split.android.client.storage.splits.ProcessedSplitChange;
import io.split.android.client.storage.splits.SplitsSnapshot;
import io.split.android.client.storage.splits.SplitsStorage;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by guillermo on 11/23/17.
 */

@Deprecated
public class SplitChangeCache implements ISplitChangeCache {

    private final SplitsStorage mSplitStorage;
    private final SplitChangeProcessor mSplitChangeProcessor;

    public SplitChangeCache(@NonNull SplitsStorage splitsStorage) {
        checkNotNull(splitsStorage);
        mSplitStorage = splitsStorage;
        mSplitChangeProcessor = new SplitChangeProcessor();
    }

    @Override
    public boolean addChange(SplitChange splitChange) {
        ProcessedSplitChange processedSplitChange = mSplitChangeProcessor.process(splitChange);
        mSplitStorage.update(processedSplitChange);
        return true;
    }

    @Override
    public SplitChange getChanges(long since) {
        SplitChange splitChange = new SplitChange();
        splitChange.splits = new ArrayList<>();
        splitChange.splits.addAll(mSplitStorage.getAll().values());
        splitChange.since = splitChange.till = mSplitStorage.getTill();

        return splitChange;
    }
}
