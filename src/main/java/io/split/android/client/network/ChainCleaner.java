package io.split.android.client.network;

import androidx.annotation.NonNull;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

public interface ChainCleaner {

    @NonNull
    List<X509Certificate> clean(String host, Certificate[] chain);
}
