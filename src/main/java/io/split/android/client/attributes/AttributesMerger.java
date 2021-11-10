package io.split.android.client.attributes;

import java.util.Map;

public interface AttributesMerger {

    Map<String, Object> merge(Map<String, Object> storedAttributes, Map<String, Object>oneTimeAttributes);
}
