package io.split.android.client.attributes;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import io.split.android.client.service.attributes.ClearAttributesInPersistentStorageTask;
import io.split.android.client.service.attributes.AttributeTaskFactory;
import io.split.android.client.service.attributes.UpdateAttributesInPersistentStorageTask;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.storage.attributes.AttributesStorage;
import io.split.android.client.storage.attributes.PersistentAttributesStorage;
import io.split.android.client.validators.AttributesValidator;
import io.split.android.client.validators.ValidationMessageLogger;

public class AttributesManagerImplTest {

    private static final long PERSIST_DELAY = 5L;

    @Mock
    AttributesStorage attributesStorage;
    @Mock
    AttributesValidator attributesValidator;
    @Mock
    ValidationMessageLogger validationMessageLogger;
    @Mock
    PersistentAttributesStorage persistentAttributesStorage;
    @Mock
    AttributeTaskFactory attributeTaskFactory;
    @Mock
    SplitTaskExecutor splitTaskExecutor;

    private AttributesManagerImpl attributeClient;
    private Map<String, Object> testValues;
    // The manager wraps every task in a fresh lambda before handing it to the executor (so the
    // scheduled slot is cleared at the START of execution rather than on completion), so the object
    // actually passed to schedule() is never the original task mock and can't be matched via eq().
    // Tests that need schedule() to return a specific id enqueue it here (FIFO, matching call order).
    private final Queue<String> scheduledTaskIds = new LinkedList<>();

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        attributeClient = new AttributesManagerImpl(attributesStorage,
                attributesValidator,
                validationMessageLogger,
                persistentAttributesStorage,
                attributeTaskFactory,
                splitTaskExecutor);
        testValues = getDefaultValues();
        when(splitTaskExecutor.schedule(any(SplitTask.class), eq(PERSIST_DELAY), any()))
                .thenAnswer(invocation -> scheduledTaskIds.poll());
    }

    @Test
    public void setAttributeUpdatesValueInStorageIfAttributeValueIsValid() {
        String name = "key";
        String attribute = "value";
        when(attributesValidator.isValid(attribute)).thenReturn(true);

        attributeClient.setAttribute(name, attribute);

        verify(attributesStorage).set(name, attribute);
    }

    @Test
    public void setAttributeReturnsTrueIfAttributeValueIsValid() {
        String name = "key";
        String attribute = "value";
        when(attributesValidator.isValid(attribute)).thenReturn(true);

        boolean result = attributeClient.setAttribute(name, attribute);

        Assert.assertTrue(result);
    }

    @Test
    public void setAttributeLaunchesAttributeUpdateTaskIfValueIsValid() {
        String name = "key";
        String attribute = "value";
        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put(name, attribute);

        UpdateAttributesInPersistentStorageTask updateAttributesInPersistentStorageTask = mock(UpdateAttributesInPersistentStorageTask.class);
        when(attributesStorage.getAll()).thenReturn(attributeMap);
        when(attributesValidator.isValid(attribute)).thenReturn(true);
        when(attributeTaskFactory.createAttributeUpdateTask(persistentAttributesStorage)).thenReturn(updateAttributesInPersistentStorageTask);

        attributeClient.setAttribute(name, attribute);

        verify(attributeTaskFactory).createAttributeUpdateTask(persistentAttributesStorage);
        verify(splitTaskExecutor).schedule(any(SplitTask.class), eq(5L), any());
    }

    @Test
    public void setAttributeDoesNotSaveAttributeInStorageIfAttributeValueIsNotValid() {
        String name = "key";
        String attribute = "value";
        when(attributesValidator.isValid(attribute)).thenReturn(false);

        attributeClient.setAttribute(name, attribute);

        Mockito.verifyNoInteractions(attributesStorage);
    }

    @Test
    public void setAttributeReturnsFalseIfAttributeValueNotIsValid() {
        String name = "key";
        String attribute = "value";
        when(attributesValidator.isValid(attribute)).thenReturn(false);

        boolean result = attributeClient.setAttribute(name, attribute);

        Assert.assertFalse(result);
    }

    @Test
    public void setAttributeLogsWarningMessageIfValueIsNotValid() {
        when(attributesValidator.isValid(any(Object.class))).thenReturn(false);

        attributeClient.setAttribute("key", "invalidValue");

        verify(validationMessageLogger).w(eq("You passed an invalid attribute value for key, acceptable types are String, double, float, long, int, boolean or Collections"), any());
    }

    @Test
    public void getReturnsValueFetchedFromStorage() {
        String name = "key";
        int attribute = 100;
        when(attributesStorage.get(name)).thenReturn(attribute);

        Object retrievedAttribute = attributeClient.getAttribute(name);

        Assert.assertEquals(attribute, retrievedAttribute);
    }

    @Test
    public void setAttributesCallsSetOnStorage() {
        when(attributesValidator.isValid(any(Object.class))).thenReturn(true);

        attributeClient.setAttributes(testValues);

        verify(attributesStorage).set(testValues);
    }

    @Test
    public void setAttributesReturnsTrueIfAttributeValuesAreValid() {
        when(attributesValidator.isValid(any(Object.class))).thenReturn(true);

        boolean result = attributeClient.setAttributes(testValues);

        Assert.assertTrue(result);
    }

    @Test
    public void setAttributesLaunchesAttributeUpdateTaskIfValuesAreValid() {
        String name = "key";
        String attribute = "value";
        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put(name, attribute);

        UpdateAttributesInPersistentStorageTask updateAttributesInPersistentStorageTask = mock(UpdateAttributesInPersistentStorageTask.class);
        when(attributesStorage.getAll()).thenReturn(attributeMap);
        when(attributesValidator.isValid(attribute)).thenReturn(true);
        when(attributeTaskFactory.createAttributeUpdateTask(persistentAttributesStorage)).thenReturn(updateAttributesInPersistentStorageTask);

        attributeClient.setAttributes(attributeMap);

        verify(attributeTaskFactory).createAttributeUpdateTask(persistentAttributesStorage);
        verify(splitTaskExecutor).schedule(any(SplitTask.class), eq(5L), any());
    }

    @Test
    public void setAttributesDoesNotSaveAttributesInStorageIfAttributeValuesAreNotValid() {
        when(attributesValidator.isValid(any(Object.class))).thenReturn(false);

        attributeClient.setAttributes(testValues);

        Mockito.verifyNoInteractions(attributesStorage);
    }

    @Test
    public void setAttributesReturnsFalseIfAttributeValuesAreNotValid() {
        when(attributesValidator.isValid(any(Object.class))).thenReturn(false);

        boolean result = attributeClient.setAttributes(testValues);

        Assert.assertFalse(result);
    }

    @Test
    public void setAttributesLogsWarningMessageIfValueIsNotValid() {
        when(attributesValidator.isValid(any(Object.class))).thenReturn(false);

        attributeClient.setAttributes(testValues);

        verify(validationMessageLogger).w(eq("You passed an invalid attribute value for key1, acceptable types are String, double, float, long, int, boolean or Collections"), any());
    }

    @Test
    public void getAllAttributesFetchesValuesFromStorage() {
        when(attributesStorage.getAll()).thenReturn(testValues);

        Map<String, Object> allAttributes = attributeClient.getAllAttributes();

        Assert.assertEquals(testValues, allAttributes);
    }

    @Test
    public void clearAttributesCallsClearOnStorage() {

        attributeClient.clearAttributes();

        verify(attributesStorage).clear();
    }

    @Test
    public void clearLaunchesAttributeClearTask() {
        ClearAttributesInPersistentStorageTask clearAttributesInPersistentStorageTask = mock(ClearAttributesInPersistentStorageTask.class);
        when(attributeTaskFactory.createAttributeClearTask(persistentAttributesStorage)).thenReturn(clearAttributesInPersistentStorageTask);

        attributeClient.clearAttributes();

        verify(attributeTaskFactory).createAttributeClearTask(persistentAttributesStorage);
        verify(splitTaskExecutor).schedule(any(SplitTask.class), eq(5L), any());
    }

    @Test
    public void removeCallsRemoveOnStorage() {

        attributeClient.removeAttribute("key");

        verify(attributesStorage).remove("key");
    }

    @Test
    public void removeLaunchesAttributeUpdateTask() {
        Map<String, Object> attributeMap = new HashMap<>();
        attributeMap.put("key", "value");
        attributeMap.put("key2", 100);

        UpdateAttributesInPersistentStorageTask updateAttributesInPersistentStorageTask = mock(UpdateAttributesInPersistentStorageTask.class);
        when(attributesStorage.getAll()).thenReturn(attributeMap);
        when(attributesValidator.isValid(any())).thenReturn(true);
        when(attributeTaskFactory.createAttributeUpdateTask(persistentAttributesStorage)).thenReturn(updateAttributesInPersistentStorageTask);

        attributeClient.removeAttribute("key");

        verify(attributeTaskFactory).createAttributeUpdateTask(persistentAttributesStorage);
        verify(splitTaskExecutor).schedule(any(SplitTask.class), eq(5L), any());
    }

    @Test
    public void settingAttributeTwiceDoesNotCancelFirstScheduledTaskSinceBothAreUpdates() {
        // Both calls schedule the SAME kind of task (update). Per the coalesce-starvation fix,
        // same-kind calls must NOT cancel the already-scheduled task - it reads live state via
        // getAll() at execute() time, so it will already reflect key2 once it fires. Only a
        // differing kind (update vs clear) triggers cancel+reschedule.
        givenUpdateTask("update-task");

        attributeClient.setAttribute("key1", "value1");
        attributeClient.setAttribute("key2", "value2");

        verify(splitTaskExecutor, never()).stopTask(any());
        verify(splitTaskExecutor, times(1)).schedule(any(SplitTask.class), eq(PERSIST_DELAY), any());
    }

    @Test
    public void clearingAttributesTwiceDoesNotCancelWhenBothAreClears() {
        givenClearTask("clear-task");

        attributeClient.clearAttributes();
        attributeClient.clearAttributes();

        verify(splitTaskExecutor, never()).stopTask(any());
        verify(splitTaskExecutor, times(1)).schedule(any(SplitTask.class), eq(PERSIST_DELAY), any());
    }

    @Test
    public void clearThenSetCancelsScheduledClearTask() {
        givenClearTask("clear-task");
        givenUpdateTask("update-task");

        attributeClient.clearAttributes();
        attributeClient.setAttribute("key1", "value1");

        InOrder inOrder = Mockito.inOrder(splitTaskExecutor);
        inOrder.verify(splitTaskExecutor).schedule(any(SplitTask.class), eq(PERSIST_DELAY), any());
        inOrder.verify(splitTaskExecutor).stopTask("clear-task");
        inOrder.verify(splitTaskExecutor).schedule(any(SplitTask.class), eq(PERSIST_DELAY), any());
    }

    @Test
    public void setThenClearCancelsScheduledUpdateTask() {
        givenUpdateTask("update-task");
        givenClearTask("clear-task");

        attributeClient.setAttribute("key1", "value1");
        attributeClient.clearAttributes();

        InOrder inOrder = Mockito.inOrder(splitTaskExecutor);
        inOrder.verify(splitTaskExecutor).schedule(any(SplitTask.class), eq(PERSIST_DELAY), any());
        inOrder.verify(splitTaskExecutor).stopTask("update-task");
        inOrder.verify(splitTaskExecutor).schedule(any(SplitTask.class), eq(PERSIST_DELAY), any());
    }

    @Test
    public void settingAttributeAgainAfterScheduledTaskRanSchedulesNewTask() {
        UpdateAttributesInPersistentStorageTask firstTask = mock(UpdateAttributesInPersistentStorageTask.class);
        UpdateAttributesInPersistentStorageTask secondTask = mock(UpdateAttributesInPersistentStorageTask.class);
        when(firstTask.execute()).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK));
        when(attributesValidator.isValid(any())).thenReturn(true);
        when(attributeTaskFactory.createAttributeUpdateTask(persistentAttributesStorage))
                .thenReturn(firstTask, secondTask);
        scheduledTaskIds.add("task-1");
        scheduledTaskIds.add("task-2");

        attributeClient.setAttribute("key1", "value1");
        simulateScheduledTaskExecuted();
        attributeClient.setAttribute("key2", "value2");

        verify(splitTaskExecutor, times(2)).schedule(any(SplitTask.class), eq(PERSIST_DELAY), any());
        verify(attributeTaskFactory, times(2)).createAttributeUpdateTask(persistentAttributesStorage);
    }

    @Test
    public void clearingAttributesAgainAfterScheduledTaskRanSchedulesNewTask() {
        ClearAttributesInPersistentStorageTask firstTask = mock(ClearAttributesInPersistentStorageTask.class);
        ClearAttributesInPersistentStorageTask secondTask = mock(ClearAttributesInPersistentStorageTask.class);
        when(firstTask.execute()).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK));
        when(attributeTaskFactory.createAttributeClearTask(persistentAttributesStorage))
                .thenReturn(firstTask, secondTask);
        scheduledTaskIds.add("clear-task-1");
        scheduledTaskIds.add("clear-task-2");

        attributeClient.clearAttributes();
        simulateScheduledTaskExecuted();
        attributeClient.clearAttributes();

        verify(splitTaskExecutor, times(2)).schedule(any(SplitTask.class), eq(PERSIST_DELAY), any());
        verify(attributeTaskFactory, times(2)).createAttributeClearTask(persistentAttributesStorage);
    }

    @Test
    public void concurrentMutationDuringTaskExecutionIsNotCoalescedAway() {
        // Reproduces the coalescing race: while task-1's execute() is still running (slot claimed),
        // a concurrent mutation must not be silently dropped - it must schedule a fresh persist task.
        UpdateAttributesInPersistentStorageTask task1 = mock(UpdateAttributesInPersistentStorageTask.class);
        UpdateAttributesInPersistentStorageTask task2 = mock(UpdateAttributesInPersistentStorageTask.class);
        when(attributesValidator.isValid(any())).thenReturn(true);
        when(attributeTaskFactory.createAttributeUpdateTask(persistentAttributesStorage))
                .thenReturn(task1, task2);

        final boolean[] firstInvocation = {true};
        final SplitTask[] scheduledTaskHolder = new SplitTask[1];
        when(splitTaskExecutor.schedule(any(SplitTask.class), eq(PERSIST_DELAY), any())).thenAnswer(invocation -> {
            if (firstInvocation[0]) {
                firstInvocation[0] = false;
                // Capture whatever was actually handed to the executor for task-1 (the raw task
                // pre-fix, or the slot-clearing wrapper post-fix), to be executed later - once the
                // slot has already been marked as claimed by the caller, just like the real executor.
                scheduledTaskHolder[0] = invocation.getArgument(0);
                return "task-1";
            } else {
                return "task-2";
            }
        });
        // Simulate the concurrent mutation arriving WHILE task-1's execute() is in flight.
        when(task1.execute()).thenAnswer(invocation -> {
            attributeClient.setAttribute("key2", "value2");
            return SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK);
        });
        when(task2.execute()).thenReturn(SplitTaskExecutionInfo.success(SplitTaskType.GENERIC_TASK));

        attributeClient.setAttribute("key1", "value1");
        // Now the slot is claimed by task-1. Simulate the executor actually running it.
        scheduledTaskHolder[0].execute();

        verify(splitTaskExecutor, times(2)).schedule(any(SplitTask.class), eq(PERSIST_DELAY), any());
    }

    private UpdateAttributesInPersistentStorageTask givenUpdateTask(String taskId) {
        UpdateAttributesInPersistentStorageTask task = mock(UpdateAttributesInPersistentStorageTask.class);
        when(attributesValidator.isValid(any())).thenReturn(true);
        when(attributeTaskFactory.createAttributeUpdateTask(persistentAttributesStorage)).thenReturn(task);
        scheduledTaskIds.add(taskId);

        return task;
    }

    private ClearAttributesInPersistentStorageTask givenClearTask(String taskId) {
        ClearAttributesInPersistentStorageTask task = mock(ClearAttributesInPersistentStorageTask.class);
        when(attributeTaskFactory.createAttributeClearTask(persistentAttributesStorage)).thenReturn(task);
        scheduledTaskIds.add(taskId);

        return task;
    }

    /**
     * Simulates the executor running the most-recently-scheduled wrapped task (which clears the
     * slot at the start of execution, before delegating to the underlying task's {@code execute()}).
     */
    private void simulateScheduledTaskExecuted() {
        ArgumentCaptor<SplitTask> wrappedTaskCaptor = ArgumentCaptor.forClass(SplitTask.class);
        verify(splitTaskExecutor, atLeastOnce()).schedule(wrappedTaskCaptor.capture(), eq(PERSIST_DELAY), any());
        java.util.List<SplitTask> capturedTasks = wrappedTaskCaptor.getAllValues();
        capturedTasks.get(capturedTasks.size() - 1).execute();
    }

    private Map<String, Object> getDefaultValues() {
        int[] array = new int[] { 1, 2, 3 };
        Map<String, Object> values = new HashMap<>();

        values.put("key1", 100);
        values.put("key2", "value2");
        values.put("key3", array);

        return values;
    }
}
