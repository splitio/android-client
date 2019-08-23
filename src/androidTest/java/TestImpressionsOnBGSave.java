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
import io.split.android.client.SplitClientConfig;
import io.split.android.client.impressions.IImpressionsStorage;
import io.split.android.client.impressions.Impression;
import io.split.android.client.impressions.ImpressionsFileStorage;
import io.split.android.client.impressions.ImpressionsManager;
import io.split.android.client.impressions.ImpressionsStorageManager;
import io.split.android.client.impressions.ImpressionsStorageManagerConfig;
import io.split.android.client.impressions.StoredImpressions;
import androidx.test.platform.app.InstrumentationRegistry;

public class TestImpressionsOnBGSave {


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
        SplitClientConfig clientConfig = SplitClientConfig.builder().impressionsRefreshRate(99999).impressionsQueueSize(9999).impressionsChunkSize(999999) .build();
        ImpressionsManager impManager = ImpressionsManager.instance(new HttpClientStub(), clientConfig, storageManager);

        lfRegistry.addObserver(impManager);

        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

        int chunksCount = 3;
        int impCount = 100;
        for(int c = 0; c<chunksCount; c++) {
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


        Assert.assertEquals(3, impBefore.size());
        Assert.assertEquals(4, impAfter.size());
        Assert.assertEquals(4, files.size());
        Assert.assertEquals(4, impLoaded.size());
        Assert.assertEquals(0, afterLoadfiles.size());


    }

    private Impression newImpression(String prefix) {
        return new Impression("key" + prefix, null,
                "split" + prefix, "On", System.currentTimeMillis() / 1000, "default rule",
                99999L, null);
    }
}
