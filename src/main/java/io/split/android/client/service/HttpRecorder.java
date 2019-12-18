package io.split.android.client.service;

import androidx.annotation.NonNull;

import java.util.Map;

public interface HttpRecorder<T> {
    void execute(@NonNull T data) throws HttpRecorderException;
}
