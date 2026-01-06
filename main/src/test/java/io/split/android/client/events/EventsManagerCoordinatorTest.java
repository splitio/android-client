package io.split.android.client.events;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import io.split.android.client.events.metadata.EventMetadata;
import io.split.android.client.api.Key;
import io.split.android.client.events.metadata.TypedTaskConverter;

public class EventsManagerCoordinatorTest {

    @Mock
    private ISplitEventsManager mMockChildEventsManager;
    private EventsManagerCoordinator mEventsManager;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mEventsManager = new EventsManagerCoordinator();
    }

    @Test
    public void SPLITS_UPDATEDEventIsPassedDownToChildren() {
        mEventsManager.registerEventsManager(new Key("key", "bucketing"), mMockChildEventsManager);

        mEventsManager.notifyInternalEvent(SplitInternalEvent.SPLITS_UPDATED);

        delay();

        verify(mMockChildEventsManager).notifyInternalEvent(eq(SplitInternalEvent.SPLITS_UPDATED), isNull());
    }

    @Test
    public void RULE_BASED_SEGMENTEventIsPassedDownToChildren() {
        mEventsManager.registerEventsManager(new Key("key", "bucketing"), mMockChildEventsManager);

        mEventsManager.notifyInternalEvent(SplitInternalEvent.RULE_BASED_SEGMENTS_UPDATED);

        delay();

        verify(mMockChildEventsManager).notifyInternalEvent(eq(SplitInternalEvent.RULE_BASED_SEGMENTS_UPDATED), isNull());
    }

    @Test
    public void SPLITS_SYNC_COMPLETEEventIsPassedDownToChildren() {
        mEventsManager.registerEventsManager(new Key("key", "bucketing"), mMockChildEventsManager);

        mEventsManager.notifyInternalEvent(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE);

        delay();

        verify(mMockChildEventsManager).notifyInternalEvent(eq(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE), isNull());
    }

    @Test
    public void SPLITS_LOADED_FROM_STORAGEEventIsPassedDownToChildren() {
        mEventsManager.registerEventsManager(new Key("key", "bucketing"), mMockChildEventsManager);

        mEventsManager.notifyInternalEvent(SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE);

        delay();

        verify(mMockChildEventsManager).notifyInternalEvent(eq(SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE), isNull());
    }

    @Test
    public void SPLIT_KILLED_NOTIFICATIONEventIsPassedDownToChildren() {
        mEventsManager.registerEventsManager(new Key("key", "bucketing"), mMockChildEventsManager);

        mEventsManager.notifyInternalEvent(SplitInternalEvent.SPLIT_KILLED_NOTIFICATION);

        delay();

        verify(mMockChildEventsManager).notifyInternalEvent(eq(SplitInternalEvent.SPLIT_KILLED_NOTIFICATION), isNull());
    }

    @Test
    public void EventIsPassedDownToChildrenIfRegisteredAfterEmission() {
        ISplitEventsManager newMockChildEventsManager = mock(ISplitEventsManager.class);
        mEventsManager.registerEventsManager(new Key("key", "bucketing"), mMockChildEventsManager);

        mEventsManager.notifyInternalEvent(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE);

        delay();

        verify(mMockChildEventsManager).notifyInternalEvent(eq(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE), isNull());

        mEventsManager.registerEventsManager(new Key("new_key", "bucketing"), newMockChildEventsManager);
        verify(newMockChildEventsManager).notifyInternalEvent(eq(SplitInternalEvent.TARGETING_RULES_SYNC_COMPLETE), isNull());
    }

    @Test
    public void SPLITS_UPDATEDEventWithMetadataIsPassedDownToChildren() {
        mEventsManager.registerEventsManager(new Key("key", "bucketing"), mMockChildEventsManager);

        List<String> updatedFlags = Arrays.asList("flag1", "flag2");
        EventMetadata metadata = io.split.android.client.events.metadata.EventMetadataHelpers.createUpdatedFlagsMetadata(updatedFlags);

        mEventsManager.notifyInternalEvent(SplitInternalEvent.SPLITS_UPDATED, metadata);

        delay();

        verify(mMockChildEventsManager).notifyInternalEvent(eq(SplitInternalEvent.SPLITS_UPDATED), argThat(meta -> {
            if (meta == null) return false;
            SdkUpdateMetadata typedMeta = TypedTaskConverter.convertForSdkUpdate(meta);
            List<String> flags = typedMeta.getUpdatedFlags();
            assertNotNull(flags);
            return flags.size() == 2 && flags.contains("flag1") && flags.contains("flag2");
        }));
    }

    @Test
    public void SPLITS_UPDATEDEventWithNullMetadataIsPassedDownToChildren() {
        mEventsManager.registerEventsManager(new Key("key", "bucketing"), mMockChildEventsManager);

        mEventsManager.notifyInternalEvent(SplitInternalEvent.SPLITS_UPDATED, null);

        delay();

        verify(mMockChildEventsManager).notifyInternalEvent(eq(SplitInternalEvent.SPLITS_UPDATED), eq((EventMetadata) null));
    }

    private void delay() {
        boolean shouldStop = false;
        long maxExecutionTime = System.currentTimeMillis() + 1000;
        long intervalExecutionTime = 100;

        while (!shouldStop) {
            try {
                Thread.sleep(intervalExecutionTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Assert.fail();
            }

            maxExecutionTime -= intervalExecutionTime;

            if (System.currentTimeMillis() > maxExecutionTime) {
                shouldStop = true;
            }
        }
    }
}
