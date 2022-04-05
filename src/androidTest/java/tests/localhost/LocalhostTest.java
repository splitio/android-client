package tests.localhost;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import org.junit.Assert;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.test.platform.app.InstrumentationRegistry;

import helper.FileHelper;
import helper.TestingHelper;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitManager;
import io.split.android.client.api.Key;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.localhost.LocalhostSplitClient;
import io.split.android.client.localhost.LocalhostSplitFactory;
import io.split.android.client.SplitResult;
import io.split.android.client.api.SplitView;

@SuppressWarnings("ConstantConditions")
public class LocalhostTest {

    SplitClientConfig mSplitClientConfig;
    FileHelper mFileHelper;
    Context mContext;

    @Before
    public void  setUp() {
        mSplitClientConfig = SplitClientConfig.builder().build();
        mFileHelper = new FileHelper();
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
    }

    @Test
    public void testUsingYamlFile() throws InterruptedException {

        SplitFactory factory = null;
        SplitClient client = null;
        SplitManager manager = null;

        CountDownLatch readyLatch = new CountDownLatch(1);
        try {
            factory = new LocalhostSplitFactory("key", mContext, mSplitClientConfig);
            client = factory.client();
            manager = factory.manager();
            client.on(SplitEvent.SDK_READY, new TestingHelper.TestEventTask(readyLatch));

        } catch (IOException e) {
        }

        readyLatch.await(5, TimeUnit.SECONDS);

        List<SplitView> splits = manager.splits();
        SplitView sv0 = manager.split("split_0");
        SplitView sv1 = manager.split("split_1");

        SplitView svx = manager.split("x_feature");

        String s0Treatment = client.getTreatment("split_0", null);
        SplitResult s0Result = client.getTreatmentWithConfig("split_0", null);

        String s1Treatment_hasKey = client.getTreatment("split_1", null);
        SplitResult s1Result_hasKey = client.getTreatmentWithConfig("split_1", null);

        String xTreatment_key = client.getTreatment("x_feature", null);
        SplitResult xResult_key = client.getTreatmentWithConfig("x_feature", null);

        String myFeatureTreatment_key = client.getTreatment("my_feature", null);
        SplitResult myFeatureResult_key = client.getTreatmentWithConfig("my_feature", null);

        String nonExistingTreatment = client.getTreatment("nonExistingTreatment", null);

        List<String> splitNames = new ArrayList<>();
        splitNames.add("split_0");
        splitNames.add("x_feature");
        splitNames.add("my_feature");
        Map<String, String> treatments = client.getTreatments(splitNames, null);
        Map<String, SplitResult> results = client.getTreatmentsWithConfig(splitNames, null);

        Assert.assertNotNull(factory);
        Assert.assertNotNull(client);
        Assert.assertNotNull(manager);

        Assert.assertEquals("off", s0Treatment);
        Assert.assertEquals("off", s0Result.treatment());
        Assert.assertEquals("{ \"size\" : 20 }", s0Result.config());

        Assert.assertEquals("control", s1Treatment_hasKey);
        Assert.assertEquals("control", s1Result_hasKey.treatment());
        Assert.assertNull(s1Result_hasKey.config());

        Assert.assertEquals("on", xTreatment_key);
        Assert.assertEquals("on", xResult_key.treatment());
        Assert.assertNull(xResult_key.config());

        Assert.assertEquals("red", myFeatureTreatment_key);
        Assert.assertEquals("red", myFeatureResult_key.treatment());
        Assert.assertNull(myFeatureResult_key.config());

        Assert.assertEquals("control", nonExistingTreatment);

        Assert.assertEquals(9, splits.size());

        Assert.assertNotNull(sv0);
        assertTrue(existsTreatment("off", sv0.treatments));
        Assert.assertEquals("{ \"size\" : 20 }", sv0.configs.get("off"));

        Assert.assertNotNull(sv1);
        assertTrue(existsTreatment("on", sv1.treatments));
        Assert.assertNull(sv1.configs);

        Assert.assertNotNull(svx);
        assertTrue(existsTreatment("red", svx.treatments));
        assertTrue(existsTreatment("on", svx.treatments));
        assertTrue(existsTreatment("off", svx.treatments));
        Assert.assertNull(svx.configs.get("on"));
        Assert.assertEquals("{\"desc\" : \"this applies only to OFF and only for only_key. The rest will receive ON\"}", svx.configs.get("off"));
        Assert.assertNull(svx.configs.get("red"));

        Assert.assertEquals("off", treatments.get("split_0"));
        Assert.assertEquals("on", treatments.get("x_feature"));
        Assert.assertEquals("red", treatments.get("my_feature"));

        Assert.assertEquals("off", results.get("split_0").treatment());
        Assert.assertEquals("{ \"size\" : 20 }", results.get("split_0").config());

        Assert.assertEquals("on", results.get("x_feature").treatment());
        Assert.assertNull(results.get("x_feature").config());

        Assert.assertEquals("red", results.get("my_feature").treatment());
        Assert.assertNull(results.get("my_feature").config());
    }

    @Test
    public void testUsingPropertiesFile() throws InterruptedException {

        SplitFactory factory = null;
        SplitClient client = null;
        SplitManager manager = null;

        CountDownLatch readyLatch = new CountDownLatch(1);
        try {
            factory = new LocalhostSplitFactory("key", mContext, mSplitClientConfig,"splits_test.properties");
            client = (LocalhostSplitClient) factory.client();
            manager = factory.manager();
            client.on(SplitEvent.SDK_READY, new TestingHelper.TestEventTask(readyLatch));

        } catch (IOException e) {
        }

        readyLatch.await(5, TimeUnit.SECONDS);

        List<SplitView> splits = manager.splits();
        SplitView sva = manager.split("split_a");
        SplitView svb = manager.split("split_b");
        SplitView svd = manager.split("split_d");

        Assert.assertNotNull(factory);
        Assert.assertNotNull(client);
        Assert.assertNotNull(manager);

        String ta = client.getTreatment("split_a", null);
        String tb = client.getTreatment("split_b", null);
        String tc = client.getTreatment("split_c", null);
        String td = client.getTreatment("split_d", null);

        String nonExistingTreatment = client.getTreatment("nonExistingTreatment", null);

        Assert.assertEquals("on", ta);
        Assert.assertEquals("off", tb);
        Assert.assertEquals("red", tc);
        Assert.assertEquals("control", td);

        Assert.assertEquals(3, splits.size());

        Assert.assertNotNull(sva);
        assertTrue(existsTreatment("on", sva.treatments));
        Assert.assertNull(sva.configs);

        Assert.assertNotNull(svb);
        assertTrue(existsTreatment("off", svb.treatments));
        Assert.assertNull(svb.configs);

        Assert.assertNull(svd);
    }

    @Test
    public void testNonExistingFile() throws InterruptedException {

        SplitFactory factory = null;
        SplitClient client = null;
        SplitManager manager = null;
        CountDownLatch timeoutLatch = new CountDownLatch(1);
        try {
            factory = new LocalhostSplitFactory("key", mContext, mSplitClientConfig, "splits_test_not_found");
            client = factory.client();
            manager = factory.manager();
            client.on(SplitEvent.SDK_READY_TIMED_OUT, new TestingHelper.TestEventTask(timeoutLatch));

        } catch (IOException e) {
        }
        timeoutLatch.await(5, TimeUnit.SECONDS);
        List<SplitView> splits = manager.splits();
        SplitView sva = manager.split("split_a");

        Assert.assertNotNull(factory);
        Assert.assertNotNull(client);
        Assert.assertNotNull(manager);

        String ta = client.getTreatment("split_a", null);
        String tb = client.getTreatment("split_b", null);
        String tc = client.getTreatment("split_c", null);
        String td = client.getTreatment("split_d", null);

        String nonExistingTreatment = client.getTreatment("nonExistingTreatment", null);

        Assert.assertEquals("control", ta);
        Assert.assertEquals("control", tb);
        Assert.assertEquals("control", tc);
        Assert.assertEquals("control", td);

        Assert.assertEquals(0, splits.size());

        Assert.assertNull(sva);
    }

    @Test
    public void testLoadYmlFile() throws InterruptedException {

        LocalhostSplitFactory factory = null;
        LocalhostSplitClient client = null;

        CountDownLatch readyLatch = new CountDownLatch(1);
        try {
            factory = new LocalhostSplitFactory("key", mContext, mSplitClientConfig, "splits_yml.yml");
            client = (LocalhostSplitClient) factory.client();
            client.on(SplitEvent.SDK_READY, new TestingHelper.TestEventTask(readyLatch));
        } catch (IOException e) {
        }
        readyLatch.await(5, TimeUnit.SECONDS);

        Assert.assertNotNull(factory);
        Assert.assertNotNull(client);

        String t = client.getTreatment("split_0", null);
        Assert.assertEquals("off", t);
    }

    @Test
    public void multipleClientsAreReady() throws InterruptedException, IOException {

        SplitFactory factory;

        CountDownLatch readyLatch = new CountDownLatch(1);
        CountDownLatch readyLatch2 = new CountDownLatch(1);

        AtomicInteger readyCount = new AtomicInteger(0);
        AtomicInteger readyCount2 = new AtomicInteger(0);
        factory = new LocalhostSplitFactory("key", mContext, mSplitClientConfig);

        SplitClient client = factory.client();
        SplitClient client2 = factory.client(new Key("second_key"));

        client.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client) {
                readyCount.addAndGet(1);
                readyLatch.countDown();
            }
        });

        client2.on(SplitEvent.SDK_READY, new SplitEventTask() {
            @Override
            public void onPostExecution(SplitClient client) {
                readyCount2.addAndGet(1);
                readyLatch2.countDown();
            }
        });

        boolean await = readyLatch.await(5, TimeUnit.SECONDS);
        boolean await2 = readyLatch2.await(5, TimeUnit.SECONDS);

        assertTrue(await);
        assertTrue(await2);
        assertEquals(1, readyCount.get());
        assertEquals(1, readyCount2.get());
    }

    private String getFileContent(String fileName) {
        String content = mFileHelper.loadFileContent(mContext, fileName);
        return content;
    }

    private boolean existsTreatment(String treatment, List<String> treatmens) {
        Set<String> set = new HashSet(treatmens);
        return set.contains(treatment);
    }

}
