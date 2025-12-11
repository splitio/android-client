package io.split.android.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.api.Key;
import io.split.android.client.attributes.AttributesManager;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.shared.SplitClientContainer;
import io.split.android.client.validators.SplitValidator;
import io.split.android.client.validators.TreatmentManager;
import io.split.android.engine.experiments.SplitParser;

public class SplitClientImplEventRegistrationTest {

    @Mock
    private SplitFactory container;
    @Mock
    private SplitClientContainer clientContainer;
    @Mock
    private SplitParser splitParser;
    @Mock
    private ImpressionListener impressionListener;
    @Mock
    private EventsTracker eventsTracker;
    @Mock
    private AttributesManager attributesManager;
    @Mock
    private SplitValidator splitValidator;
    @Mock
    private TreatmentManager treatmentManager;
    @Mock
    private SplitEventsManager eventsManager;

    private SplitClientImpl splitClient;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        SplitClientConfig splitClientConfig = SplitClientConfig.builder().build();

        splitClient = new SplitClientImpl(
                container,
                clientContainer,
                new Key("test_key"),
                splitParser,
                impressionListener,
                splitClientConfig,
                eventsManager,
                eventsTracker,
                attributesManager,
                splitValidator,
                treatmentManager
        );
    }

    @Test
    public void sdkReadyFromCacheAllowsRegistrationEvenWhenAlreadyTriggered() {
        when(eventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY_FROM_CACHE)).thenReturn(true);
        SplitEventTask task = mock(SplitEventTask.class);

        splitClient.on(SplitEvent.SDK_READY_FROM_CACHE, task);

        verify(eventsManager).register(eq(SplitEvent.SDK_READY_FROM_CACHE), eq(task));
    }

    @Test
    public void sdkReadyAllowsRegistrationEvenWhenAlreadyTriggered() {
        when(eventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY)).thenReturn(true);
        SplitEventTask task = mock(SplitEventTask.class);

        splitClient.on(SplitEvent.SDK_READY, task);

        verify(eventsManager).register(eq(SplitEvent.SDK_READY), eq(task));
    }

    @Test
    public void sdkReadyTimedOutDoesNotRegisterWhenAlreadyTriggered() {
        when(eventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY_TIMED_OUT)).thenReturn(true);
        SplitEventTask task = mock(SplitEventTask.class);

        splitClient.on(SplitEvent.SDK_READY_TIMED_OUT, task);

        verify(eventsManager, never()).register(any(SplitEvent.class), any(SplitEventTask.class));
    }

    @Test
    public void sdkUpdateDoesNotRegisterWhenAlreadyTriggered() {
        when(eventsManager.eventAlreadyTriggered(SplitEvent.SDK_UPDATE)).thenReturn(true);
        SplitEventTask task = mock(SplitEventTask.class);

        splitClient.on(SplitEvent.SDK_UPDATE, task);

        verify(eventsManager, never()).register(any(SplitEvent.class), any(SplitEventTask.class));
    }

    @Test
    public void sdkReadyTimedOutRegistersWhenNotAlreadyTriggered() {
        when(eventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY_TIMED_OUT)).thenReturn(false);
        SplitEventTask task = mock(SplitEventTask.class);

        splitClient.on(SplitEvent.SDK_READY_TIMED_OUT, task);

        verify(eventsManager).register(eq(SplitEvent.SDK_READY_TIMED_OUT), eq(task));
    }

    @Test
    public void sdkUpdateRegistersWhenNotAlreadyTriggered() {
        when(eventsManager.eventAlreadyTriggered(SplitEvent.SDK_UPDATE)).thenReturn(false);
        SplitEventTask task = mock(SplitEventTask.class);

        splitClient.on(SplitEvent.SDK_UPDATE, task);

        verify(eventsManager).register(eq(SplitEvent.SDK_UPDATE), eq(task));
    }
}
