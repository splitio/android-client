package io.split.android.client.attributes;

import java.util.Map;

public class AttributesMergerImpl implements AttributesMerger {

    @Override
    public Map<String, Object> merge(Map<String, Object> storedAttributes, Map<String, Object> oneTimeAttributes) {
        storedAttributes.putAll(oneTimeAttributes);

        return storedAttributes;
    }
}
