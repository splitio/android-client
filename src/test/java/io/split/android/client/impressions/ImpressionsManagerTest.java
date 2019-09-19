package io.split.android.client.impressions;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URISyntaxException;
import java.util.List;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.dtos.TestImpressions;
import io.split.android.fake.ImpressionsFileStorageStub;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ImpressionsManagerTest {

    @Captor
    private ArgumentCaptor<List<TestImpressions>> impressionsCaptor;

    @Test
    @Ignore
    public void works() throws URISyntaxException {

        ImpressionsManagerConfig config = new ImpressionsManagerConfig(100, 100, 4, 999, "nowhere.com");

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        ImpressionsStorageManager storageMock = new ImpressionsStorageManager(new ImpressionsFileStorageStub(), storageConfig());

        ImpressionsManagerImpl treatmentLog = ImpressionsManagerImpl.instanceForTest(null, config, senderMock, storageMock);

        KeyImpression ki1 = keyImpression("test1", "adil", "on", 1L, null);
        KeyImpression ki2 = keyImpression("test1", "adil", "on", 2L, 1L);
        KeyImpression ki3 = keyImpression("test1", "pato", "on", 3L, 2L);
        KeyImpression ki4 = keyImpression("test2", "pato", "on", 4L, 3L);

        treatmentLog.log(new Impression(ki1.keyName, null, ki1.feature, ki1.treatment, ki1.time, null, ki1.changeNumber, null));
        treatmentLog.log(new Impression(ki2.keyName, null, ki2.feature, ki2.treatment, ki2.time, null, ki2.changeNumber, null));
        treatmentLog.log(new Impression(ki3.keyName, null, ki3.feature, ki3.treatment, ki3.time, null, ki3.changeNumber, null));
        treatmentLog.log(new Impression(ki4.keyName, null, ki4.feature, ki4.treatment, ki4.time, null, ki4.changeNumber, null));

        // Do what the scheduler would do.
        treatmentLog.run();

        verify(senderMock).post(impressionsCaptor.capture());

        List<TestImpressions> captured = impressionsCaptor.getValue();

        assertThat(captured.size(), is(equalTo(2)));
    }

    @Test
    @Ignore
    public void worksButDropsImpressions() throws URISyntaxException {

        ImpressionsManagerConfig config = new ImpressionsManagerConfig(100, 100, 3, 999, "nowhere.com");

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        ImpressionsStorageManager storageMock = new ImpressionsStorageManager(new ImpressionsFileStorageStub(), storageConfig());

        ImpressionsManagerImpl treatmentLog = ImpressionsManagerImpl.instanceForTest(null, config, senderMock, storageMock);

        // These 4 unique test name will cause 4 entries but we are caping at the first 3.
        KeyImpression ki1 = keyImpression("test1", "adil", "on", 1L, null);
        KeyImpression ki2 = keyImpression("test2", "adil", "on", 2L, null);
        KeyImpression ki3 = keyImpression("test3", "pato", "on", 3L, null);
        KeyImpression ki4 = keyImpression("test4", "pato", "on", 4L, null);

        treatmentLog.log(new Impression(ki1.keyName, null, ki1.feature, ki1.treatment, ki1.time, null, null, null));
        treatmentLog.log(new Impression(ki2.keyName, null, ki2.feature, ki2.treatment, ki2.time, null, null, null));
        treatmentLog.log(new Impression(ki3.keyName, null, ki3.feature, ki3.treatment, ki3.time, null, null, null));
        treatmentLog.log(new Impression(ki4.keyName, null, ki4.feature, ki4.treatment, ki4.time, null, null, null));

        // Do what the scheduler would do.
        treatmentLog.run();

        verify(senderMock).post(impressionsCaptor.capture());

        List<TestImpressions> captured = impressionsCaptor.getValue();

        assertThat(captured.size(), is(equalTo(3)));
    }

    @Test
    @Ignore
    public void works4ImpressionsInOneTest() throws URISyntaxException {

        ImpressionsManagerConfig config = new ImpressionsManagerConfig(100, 100, 10, 999, "nowhere.com");

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        ImpressionsStorageManager storageMock = new ImpressionsStorageManager(new ImpressionsFileStorageStub(), storageConfig());

        ImpressionsManagerImpl treatmentLog = ImpressionsManagerImpl.instanceForTest(null, config, senderMock, storageMock);

        // These 4 unique test name will cause 4 entries but we are caping at the first 3.
        KeyImpression ki1 = keyImpression("test1", "adil", "on", 1L, 1L);
        KeyImpression ki2 = keyImpression("test1", "adil", "on", 2L, 1L);
        KeyImpression ki3 = keyImpression("test1", "pato", "on", 3L, 1L);
        KeyImpression ki4 = keyImpression("test1", "pato", "on", 4L, 1L);

        treatmentLog.log(new Impression(ki1.keyName, null, ki1.feature, ki1.treatment, ki1.time, null, 1L, null));
        treatmentLog.log(new Impression(ki2.keyName, null, ki2.feature, ki2.treatment, ki2.time, null, 1L, null));
        treatmentLog.log(new Impression(ki3.keyName, null, ki3.feature, ki3.treatment, ki3.time, null, 1L, null));
        treatmentLog.log(new Impression(ki4.keyName, null, ki4.feature, ki4.treatment, ki4.time, null, 1L, null));

        // Do what the scheduler would do.
        treatmentLog.run();

        verify(senderMock).post(impressionsCaptor.capture());

        List<TestImpressions> captured = impressionsCaptor.getValue();

        assertThat(captured.size(), is(equalTo(1)));
        assertThat(captured.get(0).keyImpressions.size(), is(equalTo(4)));
        assertThat(captured.get(0).keyImpressions.get(0), is(equalTo(ki1)));
    }

    @Test
    @Ignore
    public void worksNoImpressions() throws URISyntaxException {

        ImpressionsManagerConfig config = new ImpressionsManagerConfig(100, 100, 10, 999, "nowhere.com");

        ImpressionsSender senderMock = Mockito.mock(ImpressionsSender.class);
        ImpressionsStorageManager storageMock = new ImpressionsStorageManager(new ImpressionsFileStorageStub(), storageConfig());

        ImpressionsManagerImpl treatmentLog = ImpressionsManagerImpl.instanceForTest(null, config, senderMock, storageMock);

        // There are no impressions to post.

        // Do what the scheduler would do.
        treatmentLog.run();

        verify(senderMock, never()).post(impressionsCaptor.capture());
    }

    private KeyImpression keyImpression(String feature, String key, String treatment, long time, Long changeNumber) {
        KeyImpression result = new KeyImpression();
        result.feature = feature;
        result.keyName = key;
        result.treatment = treatment;
        result.time = time;
        result.changeNumber = changeNumber;
        return result;
    }

    private ImpressionsStorageManagerConfig storageConfig() {
        ImpressionsStorageManagerConfig config = new ImpressionsStorageManagerConfig();
        config.setImpressionsChunkOudatedTime(3600000);
        config.setImpressionsMaxSentAttempts(3);
        return config;
    }

}