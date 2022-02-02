package io.split.android.client.storage.splits;

import java.util.List;

public interface SplitListTransformer<I, O> {

    List<O> transform(List<I> inputList);
}
