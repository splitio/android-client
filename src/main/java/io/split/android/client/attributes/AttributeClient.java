package io.split.android.client.attributes;

import androidx.annotation.Nullable;

import java.util.Map;

public interface AttributeClient {

    boolean setAttribute(String attributeName, Object value);

    @Nullable
    Object getAttribute(String attributeName);

    boolean setAttributes(Map<String, Object> attributes);

    Map<String, Object> getAllAttributes();

    void removeAttribute(String attributeName);

    void clearAttributes();

    void destroy();
}
