package io.split.android.client.storage.splits;

import static io.split.android.client.utils.Utils.checkNotNull;
import static io.split.android.client.utils.Utils.partition;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import io.split.android.client.dtos.Split;
import io.split.android.client.service.executor.parallel.SplitDeferredTaskItem;
import io.split.android.client.service.executor.parallel.SplitParallelTaskExecutor;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;

public class SplitToSplitEntityTransformer implements SplitListTransformer<Split, SplitEntity> {

    private final SplitParallelTaskExecutor<List<SplitEntity>> mTaskExecutor;
    private final SplitCipher mSplitCipher;

    private final Object mTrafficTypesLock = new Object();
    private final Object mFlagSetsLock = new Object();
    
    public SplitToSplitEntityTransformer(@NonNull SplitParallelTaskExecutor<List<SplitEntity>> taskExecutor,
                                         @NonNull SplitCipher splitCipher) {
        mTaskExecutor = checkNotNull(taskExecutor);
        mSplitCipher = checkNotNull(splitCipher);
    }

    public TransformResult transformWithMetadata(List<Split> splits) {
        if (splits == null) {
            return TransformResult.empty();
        }

        Map<String, Integer> trafficTypesMap = new ConcurrentHashMap<>();
        Map<String, Set<String>> flagSetsMap = new ConcurrentHashMap<>();

        List<SplitEntity> splitEntities = new ArrayList<>();

        int splitsSize = splits.size();
        if (splitsSize > mTaskExecutor.getAvailableThreads()) {
            List<SplitDeferredTaskItem<List<SplitEntity>>> tasks = getSplitEntityTasks(splits, splitsSize, trafficTypesMap, flagSetsMap);
            List<List<SplitEntity>> subLists = mTaskExecutor.execute(tasks);

            for (List<SplitEntity> subList : subLists) {
                splitEntities.addAll(subList);
            }

        } else {
            splitEntities = getSplitEntities(splits, mSplitCipher, trafficTypesMap, flagSetsMap);
        }

        Map<String, Integer> resultTrafficTypes = new HashMap<>(trafficTypesMap);
        
        Map<String, Set<String>> resultFlagSets = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : flagSetsMap.entrySet()) {
            resultFlagSets.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }

        return new TransformResult(splitEntities, resultTrafficTypes, resultFlagSets);
    }

    @Override
    public List<SplitEntity> transform(List<Split> splits) {
        // Create maps that won't be returned (just for compatibility)
        Map<String, Integer> trafficTypesMap = new ConcurrentHashMap<String, Integer>();
        Map<String, Set<String>> flagSetsMap = new ConcurrentHashMap<String, Set<String>>();
        
        List<SplitEntity> splitEntities = new ArrayList<>();

        if (splits == null) {
            return splitEntities;
        }

        int splitsSize = splits.size();
        if (splitsSize > mTaskExecutor.getAvailableThreads()) {
            List<SplitDeferredTaskItem<List<SplitEntity>>> tasks = getSplitEntityTasks(splits, splitsSize, trafficTypesMap, flagSetsMap);
            List<List<SplitEntity>> subLists = mTaskExecutor.execute(tasks);

            for (List<SplitEntity> subList : subLists) {
                splitEntities.addAll(subList);
            }

            return splitEntities;

        } else {
            return getSplitEntities(splits, mSplitCipher, trafficTypesMap, flagSetsMap);
        }
    }

    @Override
    public List<SplitEntity> transform(Map<String, Split> allNamesAndBodies) {
        return Collections.emptyList();
    }

    @NonNull
    private List<SplitEntity> getSplitEntities(List<Split> partition, SplitCipher cipher, Map<String, Integer> trafficTypesMap, Map<String, Set<String>> flagSetsMap) {
        List<SplitEntity> result = new ArrayList<>();

        for (Split split : partition) {
            // Process traffic type
            if (split.trafficTypeName != null) {
                String ttLower = split.trafficTypeName.toLowerCase();
                // Thread-safe update of traffic type count
                synchronized (mTrafficTypesLock) {
                    Integer count = trafficTypesMap.get(ttLower);
                    trafficTypesMap.put(ttLower, (count == null) ? 1 : count + 1);
                }
            }
            
            // Process flag sets
            if (split.sets != null && !split.sets.isEmpty()) {
                for (String set : split.sets) {
                    // Thread-safe update of flag sets
                    synchronized (mFlagSetsLock) {
                        Set<String> splitNames = flagSetsMap.get(set);
                        if (splitNames == null) {
                            splitNames = new HashSet<>();
                            flagSetsMap.put(set, splitNames);
                        }
                        splitNames.add(split.name);
                    }
                }
            }
            
            // Create entity
            String encryptedName = cipher.encrypt(split.name);
            String encryptedJson = cipher.encrypt(Json.toJson(split));
            if (encryptedName == null || encryptedJson == null) {
                Logger.e("Error encrypting split: " + split.name);
                continue;
            }
            SplitEntity entity = new SplitEntity();
            entity.setName(encryptedName);
            entity.setBody(encryptedJson);
            
            entity.setUpdatedAt(System.currentTimeMillis() / 1000);
            result.add(entity);
        }

        return result;
    }

    @NonNull
    private List<SplitDeferredTaskItem<List<SplitEntity>>> getSplitEntityTasks(List<Split> splits, int splitsSize, Map<String, Integer> trafficTypesMap, Map<String, Set<String>> flagSetsMap) {
        int availableThreads = mTaskExecutor.getAvailableThreads();
        int partitionSize = splitsSize / availableThreads;
        List<List<Split>> partitions = partition(splits, partitionSize);
        List<SplitDeferredTaskItem<List<SplitEntity>>> taskList = new ArrayList<>(partitions.size());

        for (List<Split> partition : partitions) {
            taskList.add(new SplitDeferredTaskItem<>(
                    new Callable<List<SplitEntity>>() {
                        @Override
                        public List<SplitEntity> call() {
                            return getSplitEntities(partition, mSplitCipher, trafficTypesMap, flagSetsMap);
                        }
                    }));
        }

        return taskList;
    }

    /**
     * Result class containing the transformed entities and metadata
     */
    public static class TransformResult {
        private final List<SplitEntity> entities;
        private final Map<String, Integer> trafficTypesMap;
        private final Map<String, Set<String>> flagSetsMap;

        public TransformResult(List<SplitEntity> entities, Map<String, Integer> trafficTypesMap, Map<String, Set<String>> flagSetsMap) {
            this.entities = entities;
            this.trafficTypesMap = trafficTypesMap;
            this.flagSetsMap = flagSetsMap;
        }

        public static TransformResult empty() {
            return new TransformResult(new ArrayList<>(), new HashMap<>(), new HashMap<>());
        }

        public List<SplitEntity> getEntities() {
            return entities;
        }

        public Map<String, Integer> getTrafficTypesMap() {
            return trafficTypesMap;
        }

        public Map<String, Set<String>> getFlagSetsMap() {
            return flagSetsMap;
        }
    }
}
