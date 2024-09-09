package io.split.android.client.storage.splits;

import static io.split.android.client.utils.Utils.partition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.split.android.client.dtos.Split;
import io.split.android.client.utils.Json;

public class MegaParser {

    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public Map<String, Split> parse(Map<String, Split> unparsedSplits) {
        List<Future<Map<String, Split>>> results = new ArrayList<>();
        int availableThreads = Runtime.getRuntime().availableProcessors();
        int partitionSize = availableThreads > 0 ? unparsedSplits.size() / availableThreads : 1;
        List<Map<String, Split>> partitions = partition(unparsedSplits, partitionSize);
        List<Callable<Map<String, Split>>> taskList = new ArrayList<>(partitions.size());
        for (Map<String, Split> partition : partitions) {
            taskList.add(() -> {
                Map<String, Split> splits = new ConcurrentHashMap<>();
                for (Map.Entry<String, Split> split : partition.entrySet()) {
                    if (split == null) {
                        continue;
                    }

                    if (split.getValue().parsed) {
                        splits.put(split.getValue().name, split.getValue());
                    } else {
                        Split parsedSplit = Json.fromJson(split.getValue().originalJson, Split.class);
                        parsedSplit.name = split.getValue().name;
                        parsedSplit.originalJson = null;
                        parsedSplit.parsed = true;
                        splits.put(split.getValue().name, parsedSplit);
                    }
                }
                return splits;
            });
        }

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
}
