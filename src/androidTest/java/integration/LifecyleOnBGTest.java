package integration;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.ProcessLifecycleOwner;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fake.SyncManagerStub;
import io.split.android.client.lifecycle.LifecycleManager;
import io.split.android.client.service.SyncManager;

public class LifecyleOnBGTest {

    LifecycleManager mLifecycleManager;
    SyncManagerStub mSyncManagerStub;

    @Before
    public void setup() {
        SyncManager syncManager = new SyncManagerStub();
        mSyncManagerStub = (SyncManagerStub) syncManager;
        mSyncManagerStub.pauseCalled = false;
        mSyncManagerStub.resumeCalled = false;
        mLifecycleManager = new LifecycleManager(syncManager);
    }

    @Test
    public void onResume() {
        LifecycleRegistry lfRegistry = new LifecycleRegistry(ProcessLifecycleOwner.get());
        lfRegistry.addObserver(mLifecycleManager);
        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

        Assert.assertTrue(mSyncManagerStub.resumeCalled);
    }

    @Test
    public void onPause() {
        LifecycleRegistry lfRegistry = new LifecycleRegistry(ProcessLifecycleOwner.get());
        lfRegistry.addObserver(mLifecycleManager);
        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);

        Assert.assertTrue(mSyncManagerStub.pauseCalled);
        Assert.assertTrue(mSyncManagerStub.resumeCalled);
    }

    @Test
    public void onPauseAndResume() {
        LifecycleRegistry lfRegistry = new LifecycleRegistry(ProcessLifecycleOwner.get());
        lfRegistry.addObserver(mLifecycleManager);
        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE);
        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START);
        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);
        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE);
        mSyncManagerStub.resumeCalled = false;

        boolean pauseCalled = mSyncManagerStub.pauseCalled;
        lfRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME);

        Assert.assertTrue(pauseCalled);
        Assert.assertTrue(mSyncManagerStub.resumeCalled);
    }

}
