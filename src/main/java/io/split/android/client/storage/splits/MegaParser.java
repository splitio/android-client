package io.split.android.client.storage.splits;

import static io.split.android.client.utils.Utils.partition;

import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.split.android.client.dtos.SimpleSplit;
import io.split.android.client.dtos.Split;
import io.split.android.client.utils.Json;

public class MegaParser {

    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public Map<String, Split> parse(Collection<SimpleSplit> unparsedSplits) {
        List<Future<Map<String, Split>>> results = new ArrayList<>();
        int availableThreads = Runtime.getRuntime().availableProcessors();
        int partitionSize = availableThreads > 0 ? unparsedSplits.size() / availableThreads : 1;
        List<List<SimpleSplit>> partitions = partition(new ArrayList<>(unparsedSplits), partitionSize);
        List<Callable<Map<String, Split>>> taskList = getCallables(partitions);

        for (Callable<Map<String, Split>> task : taskList) {
            results.add(executor.submit(task));
        }

        // join result
        Map<String, Split> splits = new ConcurrentHashMap<>();
        for (Future<Map<String, Split>> future : results) {
            try {
                splits.putAll(future.get());
            } catch (Exception e) {
                // log error
            }
        }

        return splits;
    }

    private static @NonNull List<Callable<Map<String, Split>>> getCallables(List<List<SimpleSplit>> partitions) {
        List<Callable<Map<String, Split>>> taskList = new ArrayList<>(partitions.size());
        for (List<SimpleSplit> partition : partitions) {
            taskList.add(() -> getStringSplitMap(partition));
        }
        return taskList;
    }

    static @NonNull Map<String, Split> getStringSplitMap(List<SimpleSplit> partition) {
        Map<String, Split> splits = new ConcurrentHashMap<>();
        for (SimpleSplit split : partition) {
            if (split == null) {
                continue;
            }

            Split parsedSplit = parseSplit(split);

            if (parsedSplit != null) {
                splits.put(split.name, parsedSplit);
            }
        }
        return splits;
    }

    static @Nullable Split parseSplit(SimpleSplit unparsedSplit) {
        Split parsedSplit;
        if (unparsedSplit instanceof Split) {
            parsedSplit = (Split) unparsedSplit;
        } else {
            String originalJson = unparsedSplit.originalJson;
            try {
                parsedSplit = Json.fromJson(originalJson, Split.class);
                parsedSplit.originalJson = null;
            } catch (Exception e) {
                parsedSplit = null;
            }
        }
        return parsedSplit;
    }
}
