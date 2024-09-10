package io.split.android.client.storage.splits;

public interface SplitTransformer<I, O> {

    O transform(I input);
}
