package io.split.android.client.validators;

import java.util.List;

public interface SplitFilterValidator {

    List<String> cleanup(List<String> sets);
}
