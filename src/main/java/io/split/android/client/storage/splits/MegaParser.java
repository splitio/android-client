package io.split.android.client.storage.splits;

import static io.split.android.client.utils.Utils.partition;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
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

    public Map<String, Split> parse(Map<String, SimpleSplit> unparsedSplits) {
        List<Future<Map<String, Split>>> results = new ArrayList<>();
        int availableThreads = Runtime.getRuntime().availableProcessors();
        int partitionSize = availableThreads > 0 ? unparsedSplits.size() / availableThreads : 1;
        List<Map<String, SimpleSplit>> partitions = partition(unparsedSplits, partitionSize);
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

    private static @NonNull List<Callable<Map<String, Split>>> getCallables(List<Map<String, SimpleSplit>> partitions) {
        List<Callable<Map<String, Split>>> taskList = new ArrayList<>(partitions.size());
        for (Map<String, SimpleSplit> partition : partitions) {
            taskList.add(() -> getStringSplitMap(partition));
        }
        return taskList;
    }

    static @NonNull Map<String, Split> getStringSplitMap(Map<String, SimpleSplit> partition) {
        Map<String, Split> splits = new ConcurrentHashMap<>();
        for (Map.Entry<String, SimpleSplit> split : partition.entrySet()) {
            if (split == null) {
                continue;
            }

            if (split.getValue() instanceof Split) {
                splits.put(split.getValue().name, (Split) split.getValue());
            } else {
                Split parsedSplit = Json.fromJson(split.getValue().originalJson, Split.class);
                parsedSplit.name = split.getValue().name;
                parsedSplit.originalJson = null;
                splits.put(split.getValue().name, parsedSplit);
            }
        }
        return splits;
    }
}
