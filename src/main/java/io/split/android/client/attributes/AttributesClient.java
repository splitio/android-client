package io.split.android.client.attributes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;

public interface AttributesClient {

    boolean setAttribute(String attributeName, Object value);

    @Nullable
    Object getAttribute(String attributeName);

    boolean setAttributes(Map<String, Object> attributes);

    @NonNull
    Map<String, Object> getAllAttributes();

    boolean removeAttribute(String attributeName);

    boolean clearAttributes();
}
