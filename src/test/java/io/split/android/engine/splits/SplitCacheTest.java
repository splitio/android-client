package io.split.android.engine.splits;

import org.junit.Assert;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.split.android.client.cache.IMySegmentsCache;
import io.split.android.client.cache.ISplitCache;
import io.split.android.client.cache.SplitCache;
import io.split.android.client.dtos.MySegment;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.Status;
import io.split.android.client.storage.FileStorage;
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
            split.status = Status.ACTIVE;
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

    @Test
    public void testGetSplitNames() {
        Set<String> names = new HashSet<>(mCache.getSplitNames());

        Assert.assertTrue(names.contains("split-0"));
        Assert.assertTrue(names.contains("split-1"));
        Assert.assertTrue(names.contains("split-2"));
        Assert.assertTrue(names.contains("split-3"));
        Assert.assertFalse(names.contains("other"));

    }

    @Test
    public void testWriteToDiskConcurrency() throws Exception {
        final String ROOT_FOLDER = "./build";
        final String FOLDER = "thefolder";
        final String CHANGE_NUMBER_FILE = "SPLITIO.changeNumber";
        final String FILE_PREFIX = "SPLITIO.split.";
        final String JSON_SPLIT_TEMPLATE = "{\"name\":\"%s\"}";

        File rootFolder = new File(ROOT_FOLDER);
        IStorage storage = new FileStorage(rootFolder, FOLDER);
        //IStorage storage = new MemoryStorage();
        CountDownLatch latch = new CountDownLatch(2);

        for(int i = 0; i < 10000; i++) {
            String splitName = "split-" + i;
            storage.write(FILE_PREFIX + splitName, String.format(JSON_SPLIT_TEMPLATE, splitName));
        }

        try {
            storage.write(CHANGE_NUMBER_FILE, String.valueOf(INITIAL_CHANGE_NUMBER));
        } catch (IOException e) {
        }
        SplitCache cache = new SplitCache(storage);

        new Thread(new Runnable() {
            @Override
            public void run() {
                //System.out.println("TEST SPLIT CACHE: write start ");
                cache.fireWriteToDisk();
                latch.countDown();
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                Split s1 = new Split();
                s1.name = "add_split1";
                s1.status = Status.ACTIVE;
                cache.addSplit(s1);
                //System.out.println("TEST SPLIT CACHE: Adding " + s1.name);

                Split s2 = new Split();
                s2.name = "add_split2";
                s1.status = Status.ACTIVE;
                cache.addSplit(s2);
                //System.out.println("TEST SPLIT CACHE: Adding " + s2.name);

                Split s3 = new Split();
                s3.name = "add_split3";
                s3.status = Status.ACTIVE;
                cache.addSplit(s3);
                //System.out.println("TEST SPLIT CACHE: Adding " + s3.name);

                for(int i = 0; i < 990; i+=10) {
                    Split s = new Split();
                    s.name = "split-" + i;
                    s.status = Status.ARCHIVED;
                    cache.addSplit(s);
                    //System.out.println("TEST SPLIT CACHE: Removing " + s.name);
                }
                latch.countDown();
            }
        }).start();
        latch.await(10, TimeUnit.SECONDS);
        Assert.assertTrue(latch.getCount() == 0);
        Assert.assertEquals(9903, cache.getSplitNames().size());
        Assert.assertNotNull(cache.getSplit("split-11"));
        Assert.assertNotNull(cache.getSplit("split-9901"));
        Assert.assertNull(cache.getSplit("split-90"));
        Assert.assertNull(cache.getSplit("split-980"));

    }

}

