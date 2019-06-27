package io.split.android.client.network;

public interface HttpResponse {

    boolean isSuccess();
    int getHttpStatus();
    String getData();
}
