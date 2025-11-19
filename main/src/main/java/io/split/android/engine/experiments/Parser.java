package io.split.android.engine.experiments;

import androidx.annotation.Nullable;

interface Parser<I, O> {

    @Nullable
    O parse(I input, String matchingKey);
}
