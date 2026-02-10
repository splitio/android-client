package io.split.android.client.network;

import java.security.cert.Certificate;

public interface HttpResponse extends BaseHttpResponse {
    String getData();

    Certificate[] getServerCertificates();
}
