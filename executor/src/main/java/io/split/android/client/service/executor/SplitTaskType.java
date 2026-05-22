package io.split.android.client.service.executor;

public interface SplitTaskType {
    SplitTaskType GENERIC_TASK = new SplitTaskType() {
        @Override
        public String toString() {
            return "GENERIC_TASK";
        }
    };
}
