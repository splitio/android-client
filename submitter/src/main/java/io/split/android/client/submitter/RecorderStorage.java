package io.split.android.client.submitter;

import androidx.annotation.NonNull;
import java.util.List;

public interface RecorderStorage<T> {
    List<T> pop(int count);
    void delete(@NonNull List<T> items);
    void setActive(@NonNull List<T> items);
}
