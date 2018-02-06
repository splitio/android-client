package io.split.android.client;

public interface SplitFactory {
    SplitClient client();
    SplitManager manager();
    void destroy();
    void flush();
}
