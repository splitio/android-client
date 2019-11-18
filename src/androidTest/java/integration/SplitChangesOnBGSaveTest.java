package integration;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.ProcessLifecycleOwner;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import fake.ImpressionsManagerStub;
import fake.MySegmentsCacheStub;
import fake.RefreshableMySegmentsFetcherProviderStub;
import fake.RefreshableSplitFetcherProviderStub;
import fake.TrackClientStub;
import io.split.android.client.cache.SplitCache;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.Status;
import io.split.android.client.lifecycle.LifecycleManager;
import io.split.android.client.storage.legacy.FileStorage;
import io.split.android.client.track.ITrackStorage;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class SplitChangesOnBGSaveTest {


    @Before
    public void setup() {
    }

    @Test
    public void test() throws URISyntaxException, InterruptedException {

        final String FILE_PREFIX = "SPLITIO.split.";
        File rootFolder = InstrumentationRegistry.getInstrumentation().getContext().getCacheDir();
        File folder = new File(rootFolder, "test_folder");
        if(folder.exists()) {
            for(File file : folder.listFiles()){
                file.delete();
            }
            folder.delete();
        }

        ITrackStorage fileStorage = new FileStorage.TracksFileStorage(rootFolder, "split_folder_test");
        SplitCache splitCache = new SplitCache(fileStorage);

        LifecycleRegistry lfRegistry = new LifecycleRegistry(ProcessLifecycleOwner.get());

        LifecycleManager lifecycleManager = new LifecycleManager(new ImpressionsManagerStub(), new TrackClientStub(),
                new RefreshableSplitFetcherProviderStub(), new RefreshableMySegmentsFetcherProviderStub(),
                new MySegmentsCacheStub());

        lfRegistry.addObserver(lifecycleManager);

        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);


        for (int i = 0; i < 10; ++i) {
            splitCache.addSplit(newSplit("split" + i));
        }

        List<String> splitNames = new ArrayList(splitCache.getSplitNames());


        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);

        Thread.sleep(2000);

        // A file per split is created on disk on ON_Pause event
        // Here we check that all file exists on disk
        List<String> files = fileStorage.getAllIds(FILE_PREFIX);
        for (int i = 0; i < 10; ++i) {
            String name = splitNames.get(i);
            Assert.assertNotNull(files.stream().filter(file -> name.equals(file)));
        }
    }

    private Split newSplit(String name) {
        Split split = new Split();
        split.name = name;
        split.status = Status.ACTIVE;
        split.trafficTypeName = "custom";
        return split;
    }
}
