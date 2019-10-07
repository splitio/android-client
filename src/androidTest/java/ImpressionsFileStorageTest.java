import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import fake.HttpClientStub;
import fake.SplitCacheStub;
import io.split.android.client.TrackClient;
import io.split.android.client.TrackClientImpl;
import io.split.android.client.dtos.Event;
import io.split.android.client.impressions.IImpressionsStorage;
import io.split.android.client.impressions.Impression;
import io.split.android.client.impressions.ImpressionsFileStorage;
import io.split.android.client.impressions.ImpressionsManager;
import io.split.android.client.impressions.ImpressionsManagerConfig;
import io.split.android.client.impressions.ImpressionsManagerImpl;
import io.split.android.client.impressions.ImpressionsStorageManager;
import io.split.android.client.impressions.ImpressionsStorageManagerConfig;
import io.split.android.client.track.ITrackStorage;
import io.split.android.client.track.TrackClientConfig;
import io.split.android.client.track.TrackStorageManager;
import io.split.android.client.track.TracksFileStorage;

public class ImpressionsFileStorageTest {


    @Before
    public void setup() {
    }

    @Test
    public void test() throws URISyntaxException, InterruptedException {

        CountDownLatch endLatch = new CountDownLatch(1);
        File rootFolder = InstrumentationRegistry.getInstrumentation().getContext().getCacheDir();
        File folder = new File(rootFolder, "test_folder");
        if(folder.exists()) {
            for(File file : folder.listFiles()){
                file.delete();
            }
            folder.delete();
        }

        IImpressionsStorage fileStorage = new ImpressionsFileStorage(rootFolder, "track_folder_test");
        ImpressionsStorageManager storageManager = new ImpressionsStorageManager(fileStorage, new ImpressionsStorageManagerConfig());

        AtomicBoolean saved = new AtomicBoolean(false);
        AtomicBoolean generate = new AtomicBoolean(true);

        final ImpressionsManagerImpl impressionsManager = ImpressionsManagerImpl.instance(
                new HttpClientStub(),
                new ImpressionsManagerConfig(1000L, 1000, 1000,
                        1000, "ep"),
                storageManager);

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (generate.get()) {
                    impressionsManager.log(newImpression());
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                    }
                }

            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < 10; ++i) {
                        try {
                            Thread.sleep(250);
                        } catch (InterruptedException e) {
                        }
                        storageManager.saveToDisk();
                    }
                    saved.set(true);
                    generate.set(false);
                    endLatch.countDown();
                } catch (Exception e){
                    throw e;
                }
            }
        }).start();

        endLatch.await(60, TimeUnit.SECONDS);
        generate.set(false);
        Assert.assertTrue(saved.get());
    }

    private Impression newImpression() {
        return new Impression(
    "key", null, "sample_feature", "on",
                1000L, "", 111L, null);

    }
}
