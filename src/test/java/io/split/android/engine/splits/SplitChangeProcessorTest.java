package io.split.android.engine.splits;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.dtos.Status;
import io.split.android.client.storage.splits.ProcessedSplitChange;
import io.split.android.client.service.splits.SplitChangeProcessor;

public class SplitChangeProcessorTest {

    SplitChangeProcessor mProcessor;

    @Before
    public void setup() {
        mProcessor = new SplitChangeProcessor();
    }

    @Test
    public void process() {
        List<Split> activeSplits = createSplits(1, 10, Status.ACTIVE);
        List<Split> archivedSplits = createSplits(100, 5, Status.ARCHIVED);
        SplitChange change = new SplitChange();
        change.splits = activeSplits;
        change.splits.addAll(archivedSplits);

        ProcessedSplitChange result = mProcessor.process(change);

        Assert.assertEquals(5, result.getArchivedSplits().size());
        Assert.assertEquals(10, result.getActiveSplits().size());
    }

    @Test
    public void processNoArchived() {
        List<Split> activeSplits = createSplits(1, 10, Status.ACTIVE);
        SplitChange change = new SplitChange();
        change.splits = activeSplits;

        ProcessedSplitChange result = mProcessor.process(change);

        Assert.assertEquals(0, result.getArchivedSplits().size());
        Assert.assertEquals(10, result.getActiveSplits().size());
    }

    @Test
    public void processNoActive() {
        List<Split> archivedSplits = createSplits(100, 5, Status.ARCHIVED);
        SplitChange change = new SplitChange();
        change.splits = archivedSplits;

        ProcessedSplitChange result = mProcessor.process(change);

        Assert.assertEquals(5, result.getArchivedSplits().size());
        Assert.assertEquals(0, result.getActiveSplits().size());
    }

    @Test
    public void processNullSplits() {
        SplitChange change = new SplitChange();

        ProcessedSplitChange result = mProcessor.process(change);

        Assert.assertEquals(0, result.getArchivedSplits().size());
        Assert.assertEquals(0, result.getActiveSplits().size());
    }

    @Test
    public void processNullNames() {
        List<Split> activeSplits = createSplits(1, 10, Status.ACTIVE);
        List<Split> archivedSplits = createSplits(100, 5, Status.ARCHIVED);

        activeSplits.get(0).name = null;
        archivedSplits.get(0).name = null;
        SplitChange change = new SplitChange();
        change.splits = activeSplits;
        change.splits.addAll(archivedSplits);

        ProcessedSplitChange result = mProcessor.process(change);

        Assert.assertEquals(4, result.getArchivedSplits().size());
        Assert.assertEquals(9, result.getActiveSplits().size());
    }


    private List<Split> createSplits(int from, int count, Status status) {
        List<Split> splits = new ArrayList<>();
        for(int i=from; i<count + from; i++) {
            Split split = newSplit("split_" + i, status);
            splits.add(split);
        }
        return splits;
    }

    private Split newSplit(String name, Status status) {
        Split split = new Split();
        split.name = name;
        split.status = status;
        return split;
    }
}
