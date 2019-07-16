package io.split.android.client.network;

import java.io.IOException;
import java.net.ProtocolException;

public interface HttpRequest {
    HttpResponse execute() throws HttpException;
}
