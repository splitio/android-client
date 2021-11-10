package io.split.android.client;

import java.util.Map;

public interface AttributeClient {

    void setAttribute(String attributeName, Object value);

    Object getAttribute(String attributeName);

    void setAttributes(Map<String, Object> attributes);

    Map<String, Object> getAllAttributes();

    void clearAttributes();
}
