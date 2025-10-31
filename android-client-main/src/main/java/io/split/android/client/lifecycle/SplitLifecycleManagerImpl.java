package io.split.android.client.lifecycle;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import io.split.android.client.service.synchronizer.ThreadUtils;

public class SplitLifecycleManagerImpl implements DefaultLifecycleObserver, SplitLifecycleManager {

    private final List<WeakReference<SplitLifecycleAware>> mComponents;

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

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        changeRunningStatus(false);
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        changeRunningStatus(true);
    }

    private void changeRunningStatus(boolean run) {
        for (WeakReference<SplitLifecycleAware> reference : mComponents) {
            if (reference != null) {
                SplitLifecycleAware component = reference.get();
                if (component != null) {
                    if (run) {
                        component.resume();
                    } else {
                        component.pause();
                    }
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
