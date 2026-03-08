package io.split.android.client.service;

import androidx.annotation.NonNull;

import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.service.http.HttpRecorderException;
import io.split.android.client.service.http.HttpStatus;
import io.split.android.client.submitter.RecorderException;
import io.split.android.client.submitter.RecorderSubmitter;

public class HttpRecorderSubmitterAdapter<T> implements RecorderSubmitter<T> {
    private final HttpRecorder<T> mHttpRecorder;

    public HttpRecorderSubmitterAdapter(@NonNull HttpRecorder<T> httpRecorder) {
        mHttpRecorder = httpRecorder;
    }

    @Override
    public void execute(@NonNull T data) throws RecorderException {
        try {
            mHttpRecorder.execute(data);
        } catch (HttpRecorderException e) {
            Integer httpStatus = e.getHttpStatus();
            boolean retryable = !HttpStatus.isNotRetryable(HttpStatus.fromCode(httpStatus));
            throw new RecorderException(e.getMessage(), httpStatus, retryable);
        }
    }
}
