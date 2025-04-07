package io.split.android.client.storage.splits;

import java.util.List;
import java.util.Map;

/**
 * Used to map a list of values of type {@link I} to a list of type {@link O}
 *
 * @param <I> List input type.
 * @param <O> List output type.
 */
public interface SplitListTransformer<I, O> {

    List<O> transform(List<I> inputList);

    List<O> transform(Map<String, String> allNamesAndBodies);
}
