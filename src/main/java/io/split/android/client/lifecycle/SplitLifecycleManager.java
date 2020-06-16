package io.split.android.client.lifecycle;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import io.split.android.client.service.sseclient.PushNotificationManager;

import static com.google.common.base.Preconditions.checkNotNull;

public class SplitLifecycleManager implements LifecycleObserver {
    
    PushNotificationManager mPushNotificationManager;

    private List<WeakReference<SplitLifecycleAware>> mComponents;

    public SplitLifecycleManager() {
        mComponents = new ArrayList<>();
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }

    public void register(SplitLifecycleAware component) {
        mComponents.add(new WeakReference<>(component));
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private void onPause() {
        changeRunningStatus(false);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private void onResume() {
        changeRunningStatus(true);
    }

    private void changeRunningStatus(boolean run) {
        for(WeakReference<SplitLifecycleAware> reference : mComponents) {
            SplitLifecycleAware component = reference.get();
            if(component != null) {
                if(run) {
                    component.resume();
                } else {
                    component.pause();
                }
            }
        }
    }

    public void destroy() {
        ProcessLifecycleOwner.get().getLifecycle().removeObserver(this);
    }

}
