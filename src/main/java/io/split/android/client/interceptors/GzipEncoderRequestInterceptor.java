package io.split.android.client.interceptors;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;


import java.io.IOException;

/**
 * Created by adilaijaz on 5/22/15.
 */
public class GzipEncoderRequestInterceptor implements HttpRequestInterceptor {
    @Override
    public void process(HttpRequest request, HttpContext httpContext) throws HttpException, IOException {
        if (!request.containsHeader("Accept-Encoding")) {
            request.addHeader("Accept-Encoding", "gzip");
        }
    }
}
