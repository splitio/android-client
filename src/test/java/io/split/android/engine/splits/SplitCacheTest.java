package io.split.android.engine.splits;

import org.junit.Assert;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.split.android.client.cache.IMySegmentsCache;
import io.split.android.client.cache.ISplitCache;
import io.split.android.client.cache.SplitCache;
import io.split.android.client.dtos.MySegment;
import io.split.android.client.dtos.Split;
import io.split.android.client.storage.IStorage;
import io.split.android.client.storage.MemoryStorage;
import io.split.android.client.utils.Utils;

public class SplitCacheTest {

    ISplitCache mCache = null;

    final static Long INITIAL_CHANGE_NUMBER = 44L;

    @Before
    public void setupUp(){


        final String CHANGE_NUMBER_FILE = "SPLITIO.changeNumber";
        final String FILE_PREFIX = "SPLITIO.split.";
        final String JSON_SPLIT_TEMPLATE = "{\"name\":\"%s\"}";

        IStorage memStorage = new MemoryStorage();

        for(int i = 0; i < 4; i++) {
            String splitName = "split-" + i;
            try {
                memStorage.write(FILE_PREFIX + splitName, String.format(JSON_SPLIT_TEMPLATE, splitName));
            } catch (IOException e) {
            }
        }

        try {
            memStorage.write(CHANGE_NUMBER_FILE, String.valueOf(INITIAL_CHANGE_NUMBER));
        } catch (IOException e) {
        }
        mCache = new SplitCache(memStorage);
    }

    @Test
    public void getSplits() {

        Split split0 = mCache.getSplit("split-0");
        Split split1 = mCache.getSplit("split-1");
        Split split2 = mCache.getSplit("split-2");
        Split split3 = mCache.getSplit("split-3");

        Assert.assertNotNull(split0);
        Assert.assertNotNull(split1);
        Assert.assertNotNull(split2);
        Assert.assertNotNull(split3);
    }

    @Test
    public void addSplits() {
        for(int i=0; i<4; i++) {
            String splitName = "split-test-" + i;
            Split split = new Split();
            split.name = splitName;
            mCache.addSplit(split);
        }

        for(int i=0; i<4; i++) {
            String splitName = "split-test-" + i;
            Split splitTest = mCache.getSplit(splitName);
            Assert.assertNotNull(splitTest);
            Assert.assertEquals(splitName, splitTest.name);
        }
    }

    @Test
    public void initialChangeNumber() {
        Assert.assertTrue(INITIAL_CHANGE_NUMBER == mCache.getChangeNumber());
    }

    @Test
    public void updateChangeNumber() {
        final Long NEW_CHANGE_NUMBER = 70000L;
        mCache.setChangeNumber(NEW_CHANGE_NUMBER);
        Assert.assertTrue(NEW_CHANGE_NUMBER == mCache.getChangeNumber());
    }

}

