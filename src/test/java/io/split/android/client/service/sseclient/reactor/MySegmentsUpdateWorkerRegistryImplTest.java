package io.split.android.client.service.sseclient.reactor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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

        registry.start();
        registry.stop();

        verify(mySegmentsUpdateWorker).stop();
        verify(anyKeyUpdateWorker).stop();
    }

    @Test
    public void workerIsStartedWhenRegisteredIfRegistryHasAlreadyStarted() {
        MySegmentsUpdateWorker mySegmentsUpdateWorker = mock(MySegmentsUpdateWorker.class);
        registry.registerMySegmentsUpdateWorker("some_key", mySegmentsUpdateWorker);

        registry.start();

        verify(mySegmentsUpdateWorker).start();

        MySegmentsUpdateWorker anyKeyUpdateWorker = mock(MySegmentsUpdateWorker.class);
        registry.registerMySegmentsUpdateWorker("any_key", anyKeyUpdateWorker);

        verify(anyKeyUpdateWorker).start();
    }

    @Test
    public void workersAreNotStartedWhenRegisteredIfRegistryHasNotStarted() {
        MySegmentsUpdateWorker mySegmentsUpdateWorker = mock(MySegmentsUpdateWorker.class);
        MySegmentsUpdateWorker anyKeyUpdateWorker = mock(MySegmentsUpdateWorker.class);
        registry.registerMySegmentsUpdateWorker("some_key", mySegmentsUpdateWorker);
        registry.registerMySegmentsUpdateWorker("any_key", anyKeyUpdateWorker);

        verify(mySegmentsUpdateWorker, times(0)).start();
        verify(anyKeyUpdateWorker, times(0)).start();
    }

    @Test
    public void workerIsStoppedBeforeBeingUnregistered() {
        MySegmentsUpdateWorker mySegmentsUpdateWorker = mock(MySegmentsUpdateWorker.class);
        registry.registerMySegmentsUpdateWorker("some_key", mySegmentsUpdateWorker);

        registry.start();

        registry.unregisterMySegmentsUpdateWorker("some_key");

        verify(mySegmentsUpdateWorker).stop();
    }
}
