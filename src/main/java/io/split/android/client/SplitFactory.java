package io.split.android.client;

public interface SplitFactory {
    SplitClient client();
    void destroy();
    void flush();
    boolean isReady();
}
