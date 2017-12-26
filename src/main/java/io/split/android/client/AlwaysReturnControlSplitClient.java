package io.split.android.client;

import io.split.android.client.api.Key;
import io.split.android.grammar.Treatments;

import java.util.Map;

/**
 * A SplitClient that ensures that all features are turned off for all users.
 * Useful for testing
 *
 * @author adil
 */
public class AlwaysReturnControlSplitClient implements io.split.android.client.SplitClient {

    @Override
    public String getTreatment(String split) {
        return Treatments.CONTROL;
    }

    @Override
    public String getTreatment(String split, Map<String, Object> attributes) {
        return Treatments.CONTROL;
    }

    @Override
    public void destroy() {

    }

}
