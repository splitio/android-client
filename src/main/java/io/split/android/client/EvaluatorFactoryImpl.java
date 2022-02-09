package io.split.android.client;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.engine.experiments.SplitParser;

public class EvaluatorFactoryImpl implements EvaluatorFactory {

    private final SplitsStorage mSplitsStorage;

    public EvaluatorFactoryImpl(@NonNull SplitsStorage splitsStorage) {
        mSplitsStorage = checkNotNull(splitsStorage);
    }

    @Override
    public Evaluator getEvaluator(SplitParser splitParser) {
        return new EvaluatorImpl(mSplitsStorage, splitParser);
    }
}
