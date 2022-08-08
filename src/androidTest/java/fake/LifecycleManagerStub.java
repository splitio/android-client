package fake;

import androidx.lifecycle.LifecycleObserver;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import io.split.android.client.lifecycle.SplitLifecycleAware;
import io.split.android.client.lifecycle.SplitLifecycleManager;
import io.split.android.client.service.synchronizer.ThreadUtils;

public class LifecycleManagerStub implements SplitLifecycleManager {

    private List<WeakReference<SplitLifecycleAware>> mComponents;

    public LifecycleManagerStub() {
        mComponents = new ArrayList<>();
    }

    @Override
    public void register(SplitLifecycleAware component) {
        mComponents.add(new WeakReference<>(component));
    }

    public void simulateOnPause() {
        changeRunningStatus(false);
    }

    public void simulateOnResume() {
        changeRunningStatus(true);
    }

    private void changeRunningStatus(boolean run) {
        for (WeakReference<SplitLifecycleAware> reference : mComponents) {
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

    @Override
    public void destroy() {
        ThreadUtils.runInMainThread(new Runnable() {
            @Override
            public void run() {
                mComponents.clear();
            }
        });
    }

}
