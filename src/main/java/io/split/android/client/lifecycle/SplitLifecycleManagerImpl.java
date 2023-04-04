package io.split.android.client.lifecycle;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import io.split.android.client.service.synchronizer.ThreadUtils;
import io.split.android.client.utils.logger.Logger;

public class SplitLifecycleManagerImpl implements DefaultLifecycleObserver, SplitLifecycleManager {

    private final List<SplitLifecycleAware> mComponents;

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
        mComponents.add(component);
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
        for (SplitLifecycleAware reference : mComponents) {
            if (reference != null) {
                SplitLifecycleAware component = reference;//reference.get();
                if (component != null) {
                    if (run) {
//                        Logger.w("NETWORK: Resuming component because of lifecycle: " + component.getClass().getSimpleName());
                        component.resume();
                    } else {
//                        Logger.w("NETWORK: Pausing component because of lifecycle: " + component.getClass().getSimpleName());
                        component.pause();
                    }
                } else {
                    Logger.w("NETWORK: Component is null");
                }
            } else {
                Logger.w("NETWORK: Reference is null");
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
