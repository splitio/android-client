package io.split.android.client.network;

public interface AuthenticatedRequest<T> {

    void setHeader(String name, String value);

    String getHeader(String name);

    T getRequest();
}
