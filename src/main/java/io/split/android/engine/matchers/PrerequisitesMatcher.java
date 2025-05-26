package io.split.android.engine.matchers;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.split.android.client.EvaluationResult;
import io.split.android.client.Evaluator;
import io.split.android.client.dtos.Prerequisite;

public class PrerequisitesMatcher implements Matcher {

    @NonNull
    private final Set<Prerequisite> mPrerequisites;

    public PrerequisitesMatcher(Set<Prerequisite> prerequisites) {
        mPrerequisites = prerequisites == null ? new HashSet<>() : prerequisites;
    }

    @Override
    public boolean match(Object matchValue, String bucketingKey, Map<String, Object> attributes, Evaluator evaluator) {
        if (!(matchValue instanceof String)) {
            return false;
        }

        for (Prerequisite prerequisite : mPrerequisites) {
            EvaluationResult treatment = evaluator.getTreatment((String) matchValue, bucketingKey, prerequisite.getFlagName(), attributes);
            if (treatment == null || !prerequisite.getTreatments().contains(treatment.getTreatment())) {
                return false;
            }
        }
        return true;
    }
}
