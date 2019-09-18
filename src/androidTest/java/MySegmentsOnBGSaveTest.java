import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleRegistry;
import android.arch.lifecycle.ProcessLifecycleOwner;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import fake.ImpressionsManagerStub;
import fake.MySegmentsCacheStub;
import fake.RefreshableMySegmentsFetcherProviderStub;
import fake.RefreshableSplitFetcherProviderStub;
import fake.SplitCacheStub;
import fake.TrackClientStub;
import io.split.android.client.cache.MySegmentsCache;
import io.split.android.client.cache.SplitCache;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.Status;
import io.split.android.client.lifecycle.LifecycleManager;
import io.split.android.client.track.ITrackStorage;
import io.split.android.client.track.TracksFileStorage;

public class MySegmentsOnBGSaveTest {


    @Before
    public void setup() {
    }

    @Test
    public void test() throws URISyntaxException, InterruptedException, IOException {

        final String FILE_NAME = "SPLITIO.mysegments";
        File rootFolder = InstrumentationRegistry.getInstrumentation().getContext().getCacheDir();
        File folder = new File(rootFolder, "test_folder");
        if(folder.exists()) {
            for(File file : folder.listFiles()){
                file.delete();
            }
            folder.delete();
        }

        ITrackStorage fileStorage = new TracksFileStorage(rootFolder, "myseg_folder_test");
        MySegmentsCache mySegmentsCache = new MySegmentsCache(fileStorage);

        LifecycleRegistry lfRegistry = new LifecycleRegistry(ProcessLifecycleOwner.get());

        LifecycleManager lifecycleManager = new LifecycleManager(new ImpressionsManagerStub(), new TrackClientStub(),
                new RefreshableSplitFetcherProviderStub(), new RefreshableMySegmentsFetcherProviderStub(),
                new SplitCacheStub(), mySegmentsCache);

        lfRegistry.addObserver(lifecycleManager);

        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

        mySegmentsCache.setMySegments("mkey", new ArrayList<>());

        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);

        Thread.sleep(2000);

        Assert.assertNotNull(fileStorage.read(FILE_NAME));

    }
}
