package io.split.android.client.attributes;

import java.util.HashMap;
import java.util.Map;

public class AttributesMergerImpl implements AttributesMerger {

    @Override
    public Map<String, Object> merge(Map<String, Object> storedAttributes, Map<String, Object> oneTimeAttributes) {
        if (storedAttributes == null) storedAttributes = new HashMap<>();

        if (oneTimeAttributes == null) oneTimeAttributes = new HashMap<>();

        storedAttributes.putAll(oneTimeAttributes);

        return storedAttributes;
    }
}
