package io.split.android.client.attributes;

import java.util.HashMap;
import java.util.Map;

public class AttributesMergerImpl implements AttributesMerger {

    @Override
    public Map<String, Object> merge(final Map<String, Object> storedAttributes, final Map<String, Object> oneTimeAttributes) {
        if (storedAttributes == null) {
            if (oneTimeAttributes != null) return oneTimeAttributes;
            else return new HashMap<>();
        } else if (oneTimeAttributes == null) {
            return storedAttributes;
        } else {
            storedAttributes.putAll(oneTimeAttributes);

            return storedAttributes;
        }
    }
}
