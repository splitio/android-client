package io.split.android.engine.matchers;

import java.util.Map;
import java.util.Set;

import io.split.android.client.EvaluationResult;
import io.split.android.client.Evaluator;
import io.split.android.client.dtos.Prerequisite;

public class PrerequisitesMatcher implements Matcher {

    private final Set<Prerequisite> mPrerequisites;

    public PrerequisitesMatcher(Set<Prerequisite> prerequisites) {
        mPrerequisites = prerequisites;
    }

    @Override
    public boolean match(Object matchValue, String bucketingKey, Map<String, Object> attributes, Evaluator splitClient) {
        for (Prerequisite prerequisite : mPrerequisites) {
            EvaluationResult treatment = splitClient.getTreatment((String) matchValue, bucketingKey, prerequisite.getFlagName(), attributes);
            
        }
    }
}
