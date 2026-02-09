package io.split.android.client.network;

public interface HttpRequest {
    HttpResponse execute() throws HttpException;
}
