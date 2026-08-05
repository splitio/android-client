package io.split.android.client.storage.attributes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.split.android.client.service.attributes.LoadAttributesTask;
import io.split.android.client.service.attributes.UpdateAttributesInPersistentStorageTask;

public class AttributesLoadPersistRaceIntegrationTest {

    private static final String MATCHING_KEY = "matching_key_1";

    private AttributesStorageImpl attributesStorage;
    private FakePersistentAttributesStorage persistentAttributesStorage;

    @Before
    public void setUp() {
        attributesStorage = new AttributesStorageImpl();
        persistentAttributesStorage = new FakePersistentAttributesStorage();
    }

    @Test
    public void setThenLoad_dirtyKeyWinsOverPersistedValue_siblingKeyMerged() {
        givenPersisted("A", "old", "SIBLING", "persisted-value");

        // App sets a newer value for "A" BEFORE the delayed load task runs.
        attributesStorage.set("A", "new");

        runLoadTask();

        // Memory correct: dirty key "A" retains the newer value, sibling key merged from DB.
        assertEquals("new", attributesStorage.get("A"));
        assertEquals("persisted-value", attributesStorage.get("SIBLING"));
    }

    @Test
    public void loadThenSet_dirtyKeyWinsOverPersistedValue_siblingKeyMerged() {
        givenPersisted("A", "old", "SIBLING", "persisted-value");

        // Load task executes first (merges A=old, SIBLING=persisted-value into memory).
        runLoadTask();

        // App then sets a newer overlapping value for "A" right after the load completes.
        attributesStorage.set("A", "new");

        // Memory correct: "A" reflects the newer app-set value, sibling key untouched.
        assertEquals("new", attributesStorage.get("A"));
        assertEquals("persisted-value", attributesStorage.get("SIBLING"));
    }

    @Test
    public void persistAfterMerge_delayedPersistWritesFullyMergedState_doesNotWipeSibling() {
        // Arrange: DB pre-existing state, then the "early overlapping set" race, then the load.
        givenPersisted("A", "old", "SIBLING", "persisted-value");

        attributesStorage.set("A", "new");

        runLoadTask();

        // Sanity check on the merged in-memory state before the persist task runs.
        assertEquals("new", attributesStorage.get("A"));
        assertEquals("persisted-value", attributesStorage.get("SIBLING"));

        // Act: the delayed (e.g. 5-second) persist task finally runs AFTER the merge completed.
        runPersistTask();

        // Assert: the persisted state reflects the fully-merged, correct in-memory snapshot.
        Map<String, Object> persistedAfterUpdate = persistentAttributesStorage.getAll(MATCHING_KEY);
        assertEquals("new", persistedAfterUpdate.get("A"));
        assertEquals("persisted-value", persistedAfterUpdate.get("SIBLING"));
    }

    @Test
    public void clearBeforeLoad_persistedKeysDoNotResurrectAcrossLoadTaskBoundary() {
        givenPersisted("OLD_KEY", "old-value", "ANOTHER_OLD_KEY", "another-old-value");

        // App clears attributes (e.g. on logout) before the delayed load task runs.
        attributesStorage.clear();

        // Delayed load task finally executes with the stale persisted map.
        runLoadTask();

        // Sticky clear holds across the load-task boundary: none of the old keys resurrect.
        assertNull(attributesStorage.get("OLD_KEY"));
        assertNull(attributesStorage.get("ANOTHER_OLD_KEY"));
        assertTrue(attributesStorage.getAll().isEmpty());
    }

    @Test
    public void clearBeforeLoad_thenSetNewValue_resumesMergeForNonDirtyKeysButProtectsDirtyOnes() {
        givenPersisted("OLD_KEY", "old-value", "FRESH_KEY", "persisted-stale-value");

        attributesStorage.clear();
        attributesStorage.set("FRESH_KEY", "fresh-value");

        runLoadTask();

        // FRESH_KEY is dirty (app just set it) and persisted stale value must not clobber it.
        assertEquals("fresh-value", attributesStorage.get("FRESH_KEY"));
        // OLD_KEY was never in memory before the clear, so it wasn't tombstoned: normal merge
        // resumed and it comes back from persistence.
        assertEquals("old-value", attributesStorage.get("OLD_KEY"));
        assertEquals(2, attributesStorage.getAll().size());
    }

    /** Seeds the fake DB with the given key/value pairs, as if written by a previous session. */
    private void givenPersisted(String key1, Object value1, String key2, Object value2) {
        Map<String, Object> persisted = new HashMap<>();
        persisted.put(key1, value1);
        persisted.put(key2, value2);
        persistentAttributesStorage.set(MATCHING_KEY, persisted);
    }

    /** Runs the real load task, as the delayed scheduled task eventually would. */
    private void runLoadTask() {
        new LoadAttributesTask(MATCHING_KEY, attributesStorage, persistentAttributesStorage).execute();
    }

    /** Runs the real persist task, as the delayed scheduled task eventually would. */
    private void runPersistTask() {
        new UpdateAttributesInPersistentStorageTask(MATCHING_KEY, persistentAttributesStorage, attributesStorage).execute();
    }

    /**
     * In-memory fake of {@link PersistentAttributesStorage}, backed by a simple map keyed by matching key.
     */
    private static class FakePersistentAttributesStorage implements PersistentAttributesStorage {

        private final Map<String, Map<String, Object>> mBackingStore = new HashMap<>();

        @Override
        public void set(String matchingKey, @Nullable Map<String, Object> attributes) {
            if (attributes == null) {
                return;
            }
            mBackingStore.put(matchingKey, new HashMap<>(attributes));
        }

        @NonNull
        @Override
        public Map<String, Object> getAll(String matchingKey) {
            Map<String, Object> stored = mBackingStore.get(matchingKey);
            return stored == null ? new HashMap<>() : new HashMap<>(stored);
        }

        @Override
        public void clear(String matchingKey) {
            mBackingStore.remove(matchingKey);
        }
    }
}
