package io.split.android.client.network;

import java.security.cert.Certificate;

public class HttpResponseImpl extends BaseHttpResponseImpl implements HttpResponse {

    private final String mData;
    private final Certificate[] mServerCertificates;

    HttpResponseImpl(int httpStatus) {
        this(httpStatus, (String) null);
    }

    HttpResponseImpl(int httpStatus, Certificate[] serverCertificates) {
        this(httpStatus, null, serverCertificates);
    }

    public HttpResponseImpl(int httpStatus, String data) {
        this(httpStatus, data, null);
    }
    
    public HttpResponseImpl(int httpStatus, String data, Certificate[] serverCertificates) {
        super(httpStatus);
        mData = data;
        mServerCertificates = serverCertificates;
    }

    @Override
    public String getData() {
        return mData;
    }
    
    @Override
    public Certificate[] getServerCertificates() {
        return mServerCertificates;
    }
}
