package io.split.android.client.events;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.api.Key;

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

        verify(mMockChildEventsManager).notifyInternalEvent(SplitInternalEvent.SPLITS_UPDATED);
    }

    @Test
    public void SPLITS_FETCHEDEventIsPassedDownToChildren() {
        mEventsManager.registerEventsManager(new Key("key", "bucketing"), mMockChildEventsManager);

        mEventsManager.notifyInternalEvent(SplitInternalEvent.SPLITS_FETCHED);

        delay();

        verify(mMockChildEventsManager).notifyInternalEvent(SplitInternalEvent.SPLITS_FETCHED);
    }

    @Test
    public void SPLITS_LOADED_FROM_STORAGEEventIsPassedDownToChildren() {
        mEventsManager.registerEventsManager(new Key("key", "bucketing"), mMockChildEventsManager);

        mEventsManager.notifyInternalEvent(SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE);

        delay();

        verify(mMockChildEventsManager).notifyInternalEvent(SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE);
    }

    @Test
    public void SPLIT_KILLED_NOTIFICATIONEventIsPassedDownToChildren() {
        mEventsManager.registerEventsManager(new Key("key", "bucketing"), mMockChildEventsManager);

        mEventsManager.notifyInternalEvent(SplitInternalEvent.SPLIT_KILLED_NOTIFICATION);

        delay();

        verify(mMockChildEventsManager).notifyInternalEvent(SplitInternalEvent.SPLIT_KILLED_NOTIFICATION);
    }

    @Test
    public void EventIsPassedDownToChildrenIfRegisteredAfterEmission() {
        ISplitEventsManager newMockChildEventsManager = mock(ISplitEventsManager.class);
        mEventsManager.registerEventsManager(new Key("key", "bucketing"), mMockChildEventsManager);

        mEventsManager.notifyInternalEvent(SplitInternalEvent.SPLITS_FETCHED);

        delay();

        verify(mMockChildEventsManager).notifyInternalEvent(SplitInternalEvent.SPLITS_FETCHED);

        mEventsManager.registerEventsManager(new Key("new_key", "bucketing"), newMockChildEventsManager);
        verify(newMockChildEventsManager).notifyInternalEvent(SplitInternalEvent.SPLITS_FETCHED);
    }

    @Test
    public void EventIsNotDuplicated() {
        ISplitEventsManager firstEventsManager = mock(ISplitEventsManager.class);
        ISplitEventsManager secondEventsManager = mock(ISplitEventsManager.class);

        Thread thread0 = new Thread(() -> mEventsManager.registerEventsManager(new Key("first", "bucketing"), firstEventsManager));
        Thread thread1 = new Thread(() -> mEventsManager.registerEventsManager(new Key("second", "bucketing"), secondEventsManager));

        Thread thread4 = new Thread(() -> mEventsManager.notifyInternalEvent(SplitInternalEvent.SPLITS_FETCHED));
        Thread thread5 = new Thread(this::delay);

        thread0.start();
        thread1.start();
        thread4.start();
        thread5.start();

        try {
            thread0.join();
            thread1.join();
            thread4.join();
            thread5.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        verify(firstEventsManager, times(1)).notifyInternalEvent(SplitInternalEvent.SPLITS_FETCHED);
        verify(secondEventsManager, times(1)).notifyInternalEvent(SplitInternalEvent.SPLITS_FETCHED);
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
