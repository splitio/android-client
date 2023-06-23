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

    @Test
    public void processSingleActiveSplit() {
        Split activeSplit = createSplits(1, 1, Status.ACTIVE).get(0);

        ProcessedSplitChange result = mProcessor.process(activeSplit, 14500);

        Assert.assertEquals(0, result.getArchivedSplits().size());
        Assert.assertEquals(1, result.getActiveSplits().size());
        Assert.assertEquals(14500, result.getChangeNumber());

        Split split = result.getActiveSplits().get(0);
        Assert.assertEquals(activeSplit.name, split.name);
        Assert.assertEquals(activeSplit.status, split.status);
        Assert.assertEquals(activeSplit.trafficTypeName, split.trafficTypeName);
        Assert.assertEquals(activeSplit.trafficAllocation, split.trafficAllocation);
        Assert.assertEquals(activeSplit.trafficAllocationSeed, split.trafficAllocationSeed);
        Assert.assertEquals(activeSplit.seed, split.seed);
        Assert.assertEquals(activeSplit.conditions, split.conditions);
        Assert.assertEquals(activeSplit.defaultTreatment, split.defaultTreatment);
        Assert.assertEquals(activeSplit.configurations, split.configurations);
        Assert.assertEquals(activeSplit.algo, split.algo);
        Assert.assertEquals(activeSplit.changeNumber, split.changeNumber);
        Assert.assertEquals(activeSplit.killed, split.killed);
    }

    @Test
    public void processSingleArchivedSplit() {
        Split archivedSplit = createSplits(1, 1, Status.ARCHIVED).get(0);

        ProcessedSplitChange result = mProcessor.process(archivedSplit, 14500);

        Assert.assertEquals(1, result.getArchivedSplits().size());
        Assert.assertEquals(0, result.getActiveSplits().size());
        Assert.assertEquals(14500, result.getChangeNumber());

        Split split = result.getArchivedSplits().get(0);
        Assert.assertEquals(archivedSplit.name, split.name);
        Assert.assertEquals(archivedSplit.status, split.status);
        Assert.assertEquals(archivedSplit.trafficTypeName, split.trafficTypeName);
        Assert.assertEquals(archivedSplit.trafficAllocation, split.trafficAllocation);
        Assert.assertEquals(archivedSplit.trafficAllocationSeed, split.trafficAllocationSeed);
        Assert.assertEquals(archivedSplit.seed, split.seed);
        Assert.assertEquals(archivedSplit.conditions, split.conditions);
        Assert.assertEquals(archivedSplit.defaultTreatment, split.defaultTreatment);
        Assert.assertEquals(archivedSplit.configurations, split.configurations);
        Assert.assertEquals(archivedSplit.algo, split.algo);
        Assert.assertEquals(archivedSplit.changeNumber, split.changeNumber);
        Assert.assertEquals(archivedSplit.killed, split.killed);
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
