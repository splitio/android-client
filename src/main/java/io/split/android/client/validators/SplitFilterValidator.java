package io.split.android.client.validators;

import java.util.List;

public interface SplitFilterValidator {

    List<String> cleanup(List<String> values);

    boolean isValid(String value);
}
