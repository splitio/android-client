package io.split.android.client.service.sseclient.reactor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;

public class MySegmentsUpdateWorkerRegistryImplTest {

    private MySegmentsUpdateWorkerRegistryImpl registry;

    @Before
    public void setUp() {
        registry = new MySegmentsUpdateWorkerRegistryImpl();
    }

    @Test
    public void startCallsStartOnAllSegmentsWorkers() {
        MySegmentsUpdateWorker mySegmentsUpdateWorker = mock(MySegmentsUpdateWorker.class);
        MySegmentsUpdateWorker anyKeyUpdateWorker = mock(MySegmentsUpdateWorker.class);
        registry.registerMySegmentsUpdateWorker("some_key", mySegmentsUpdateWorker);
        registry.registerMySegmentsUpdateWorker("any_key", anyKeyUpdateWorker);

        registry.start();

        verify(mySegmentsUpdateWorker).start();
        verify(anyKeyUpdateWorker).start();
    }

    @Test
    public void stopCallsStopOnMyAllSegmentsWorkers() {
        MySegmentsUpdateWorker mySegmentsUpdateWorker = mock(MySegmentsUpdateWorker.class);
        MySegmentsUpdateWorker anyKeyUpdateWorker = mock(MySegmentsUpdateWorker.class);
        registry.registerMySegmentsUpdateWorker("some_key", mySegmentsUpdateWorker);
        registry.registerMySegmentsUpdateWorker("any_key", anyKeyUpdateWorker);

        registry.stop();

        verify(mySegmentsUpdateWorker).stop();
        verify(anyKeyUpdateWorker).stop();
    }
}
