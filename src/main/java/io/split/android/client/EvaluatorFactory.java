package io.split.android.client;

import io.split.android.engine.experiments.SplitParser;

public interface EvaluatorFactory {

    Evaluator getEvaluator(SplitParser splitParser);
}
