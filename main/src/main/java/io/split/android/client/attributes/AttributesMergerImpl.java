package io.split.android.client.attributes;

import java.util.HashMap;
import java.util.Map;

public class AttributesMergerImpl implements AttributesMerger {

    @Override
    public Map<String, Object> merge(final Map<String, Object> storedAttributes, final Map<String, Object> oneTimeAttributes) {
        Map<String, Object> mergedAttributes = new HashMap<>();

        if (storedAttributes != null) {
            mergedAttributes.putAll(storedAttributes);
        }

        if (oneTimeAttributes != null) {
            mergedAttributes.putAll(oneTimeAttributes);
        }

        return mergedAttributes;
    }
}
