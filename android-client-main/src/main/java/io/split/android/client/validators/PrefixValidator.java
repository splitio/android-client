package io.split.android.client.validators;

import androidx.annotation.Nullable;

interface PrefixValidator {

    @Nullable
    ValidationErrorInfo validate(@Nullable String prefix);
}
