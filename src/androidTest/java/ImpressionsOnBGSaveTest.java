import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.ProcessLifecycleOwner;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import fake.HttpClientStub;
import fake.MySegmentsCacheStub;
import fake.RefreshableMySegmentsFetcherProviderStub;
import fake.RefreshableSplitFetcherProviderStub;
import fake.SplitCacheStub;
import fake.TrackClientStub;
import io.split.android.client.impressions.IImpressionsStorage;
import io.split.android.client.impressions.Impression;
import io.split.android.client.impressions.ImpressionsFileStorage;
import io.split.android.client.impressions.ImpressionsManagerConfig;
import io.split.android.client.impressions.ImpressionsManagerImpl;
import io.split.android.client.impressions.ImpressionsStorageManager;
import io.split.android.client.impressions.ImpressionsStorageManagerConfig;
import io.split.android.client.impressions.StoredImpressions;
import io.split.android.client.lifecycle.LifecycleManager;

import androidx.test.platform.app.InstrumentationRegistry;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class ImpressionsOnBGSaveTest {


    @Before
    public void setup() {
    }

    @Test
    public void test() throws URISyntaxException, InterruptedException {

        final String FILE_PREFIX = "SPLITIO.impressions_chunk_id_";
        File rootFolder = InstrumentationRegistry.getInstrumentation().getContext().getCacheDir();
        File folder = new File(rootFolder, "test_folder");
        if(folder.exists()) {
            for(File file : folder.listFiles()){
                file.delete();
            }
            folder.delete();
        }

        LifecycleRegistry lfRegistry = new LifecycleRegistry(ProcessLifecycleOwner.get());

        ImpressionsStorageManagerConfig storageManagerConfig = new ImpressionsStorageManagerConfig();
        storageManagerConfig.setImpressionsMaxSentAttempts(3);
        storageManagerConfig.setImpressionsChunkOudatedTime(99999);
        IImpressionsStorage fileStorage = new ImpressionsFileStorage(rootFolder, "test_folder");
        ImpressionsStorageManager storageManager = new ImpressionsStorageManager(fileStorage, storageManagerConfig);
        ImpressionsManagerConfig config = new ImpressionsManagerConfig(99999L, 99999, 9999, 9999, "server");
        ImpressionsManagerImpl impManager = ImpressionsManagerImpl.instance(new HttpClientStub(), config, storageManager);

        LifecycleManager lifecycleManager = new LifecycleManager(impManager, new TrackClientStub(),
                new RefreshableSplitFetcherProviderStub(), new RefreshableMySegmentsFetcherProviderStub(),
                new SplitCacheStub(), new MySegmentsCacheStub());


        lfRegistry.addObserver(lifecycleManager);

        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

        int chunksCount = 3;
        int impCount = 100;
        for(int c = 0; c < chunksCount; c++) {
            for (int i = 0; i < impCount; i++) {
                impManager.log(newImpression("i" + i));
            }
            impManager.flushImpressions();
        }

        for (int i = 0; i < impCount; i++) {
            impManager.log(newImpression("a" + i));
        }

        List<StoredImpressions> impBefore = new ArrayList(storageManager.getStoredImpressions());

        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);

        List<StoredImpressions> impAfter = new ArrayList(storageManager.getStoredImpressions());


        List<String> files = fileStorage.getAllIds(FILE_PREFIX);

        ImpressionsStorageManager storageManagerLoader = new ImpressionsStorageManager(fileStorage, storageManagerConfig);

        List<StoredImpressions> impLoaded = new ArrayList(storageManagerLoader.getStoredImpressions());
        List<String> afterLoadfiles = fileStorage.getAllIds(FILE_PREFIX);


        Assert.assertEquals(3, impBefore.size()); // Checks impressions on cache
        Assert.assertEquals(4, impAfter.size()); // Checks that impressions were flushed from queue to cache on app pause
        Assert.assertEquals(4, files.size()); // Checks numbers of disk files created on app pause
        Assert.assertEquals(4, impLoaded.size()); // Checks that files created were loaded correctly
        Assert.assertEquals(0, afterLoadfiles.size()); // Checks that old files were removed

    }

    private Impression newImpression(String prefix) {
        return new Impression("key" + prefix, null,
                "split" + prefix, "On", System.currentTimeMillis() / 1000, "default rule",
                99999L, null);
    }
}
