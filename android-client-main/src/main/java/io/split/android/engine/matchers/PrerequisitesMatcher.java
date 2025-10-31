package io.split.android.engine.matchers;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.split.android.client.EvaluationResult;
import io.split.android.client.Evaluator;
import io.split.android.client.dtos.Prerequisite;

public class PrerequisitesMatcher implements Matcher {

    @NonNull
    private final List<Prerequisite> mPrerequisites;

    public PrerequisitesMatcher(List<Prerequisite> prerequisites) {
        mPrerequisites = prerequisites == null ? new ArrayList<>() : prerequisites;
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
