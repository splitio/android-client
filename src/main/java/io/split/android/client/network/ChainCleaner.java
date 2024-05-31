package io.split.android.client.network;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

public interface ChainCleaner {

    List<X509Certificate> clean(String host, Certificate[] chain);
}
