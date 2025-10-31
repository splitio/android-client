package io.split.android.client;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

public class EvaluationOptionsTest {

    @Test
    public void equalsWithSamePropertiesReturnsTrue() {
        Map<String, Object> props = mapOf("key1", "value1", "key2", 2);
        EvaluationOptions opt1 = new EvaluationOptions(props);
        EvaluationOptions opt2 = new EvaluationOptions(props);
        assertEquals(opt1, opt2);
        assertEquals(opt1.hashCode(), opt2.hashCode());
    }

    @Test
    public void equalsWithNullPropertiesReturnsTrue() {
        EvaluationOptions opt1 = optionsWithNullProps();
        EvaluationOptions opt2 = optionsWithNullProps();
        assertEquals(opt1, opt2);
        assertEquals(opt1.hashCode(), opt2.hashCode());
    }

    @Test
    public void equalsWithDifferentPropertiesReturnsFalse() {
        EvaluationOptions opt1 = optionsWithProps("key1", "value1");
        EvaluationOptions opt2 = optionsWithProps("key1", "value2");
        assertNotEquals(opt1, opt2);
    }

    @Test
    public void equalsWithNullAndNonNullPropertiesReturnsFalse() {
        EvaluationOptions opt1 = optionsWithProps("k", "v");
        EvaluationOptions opt2 = optionsWithNullProps();
        assertNotEquals(opt1, opt2);
        assertNotEquals(opt2, opt1);
    }

    @Test
    public void inputMapModificationDoesNotAffectInternalState() {
        Map<String, Object> props = mapOf("key", "value");
        EvaluationOptions opt = new EvaluationOptions(props);
        props.put("key2", "value2");
        // opt's properties should not include key2
        Map<String, Object> optProps = opt.getProperties();
        assertFalse(optProps.containsKey("key2"));
    }

    @Test
    public void getPropertiesReturnsDefensiveCopy() {
        EvaluationOptions opt = optionsWithProps("key", "value");
        Map<String, Object> first = opt.getProperties();
        Map<String, Object> second = opt.getProperties();

        assertNotSame(first, second);
        // Modifying the returned map does not affect internal state
        first.put("another", "thing");
        assertFalse(opt.getProperties().containsKey("another"));
    }

    private static Map<String, Object> mapOf(Object... keyValuePairs) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValuePairs.length - 1; i += 2) {
            map.put((String) keyValuePairs[i], keyValuePairs[i+1]);
        }
        return map;
    }

    private static EvaluationOptions optionsWithProps(Object... keyValuePairs) {
        return new EvaluationOptions(mapOf(keyValuePairs));
    }

    private static EvaluationOptions optionsWithNullProps() {
        return new EvaluationOptions(null);
    }
}
