package io.split.android.client.network;

public interface HttpResponse {

    public final static int REQUEST_STATUS_OK = 0;
    public final static int REQUEST_STATUS_FAIL = 1;

    int getRequestStatus();
    int getHttpStatus();
    String getData();
}
