package io.split.android.fake;

import io.split.android.client.SplitClient;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitManager;

public class SplitFactoryStub implements SplitFactory {
    @Override
    public SplitClient client() {
        return null;
    }

    @Override
    public SplitManager manager() {
        return null;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void flush() {

    }

    @Override
    public boolean isReady() {
        return false;
    }
}
