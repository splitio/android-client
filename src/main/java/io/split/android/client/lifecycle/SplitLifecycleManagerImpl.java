package io.split.android.client.lifecycle;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import io.split.android.client.service.synchronizer.ThreadUtils;

import static com.google.common.base.Preconditions.checkNotNull;

public class SplitLifecycleManagerImpl implements LifecycleObserver, SplitLifecycleManager {

    private List<WeakReference<SplitLifecycleAware>> mComponents;

    public SplitLifecycleManagerImpl() {
        mComponents = new ArrayList<>();
        ThreadUtils.runInMainThread(new Runnable() {
            @Override
            public void run() {
                ProcessLifecycleOwner.get().getLifecycle().addObserver(SplitLifecycleManagerImpl.this);
            }
        });
    }

    @Override
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

    @Override
    public void destroy() {
        ThreadUtils.runInMainThread(new Runnable() {
            @Override
            public void run() {
                ProcessLifecycleOwner.get().getLifecycle().removeObserver(SplitLifecycleManagerImpl.this);
            }
        });
    }

}
