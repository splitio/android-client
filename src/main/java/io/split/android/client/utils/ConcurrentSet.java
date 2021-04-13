package io.split.android.client.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentSet<E> implements Set<E> {

    final private Map<E, Integer> mValues;

    public ConcurrentSet() {
        this.mValues = new ConcurrentHashMap<>();
    }

    @Override
    public int size() {
        return mValues.size();
    }

    @Override
    public boolean isEmpty() {
        return mValues.isEmpty();
    }

    @Override
    public boolean contains(@Nullable Object o) {
        return mValues.get(o) != null;
    }

    @NonNull
    @Override
    public Iterator<E> iterator() {
        return mValues.keySet().iterator();
    }

    @Nullable
    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @Override
    public <T> T[] toArray(@Nullable T[] a) {
        return null;
    }

    @Override
    public boolean add(E e) {
        mValues.put(e, 1);
        return true;
    }

    @Override
    public boolean remove(@Nullable Object o) {
        return mValues.remove(o) != null;
    }

    @Override
    public boolean containsAll(@NonNull Collection<?> c) {
        return mValues.keySet().containsAll(c);
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends E> c) {
        Map<E, Integer> values = new HashMap<>();
        for (E element : c) {
            values.put(element, 1);
        }
        mValues.putAll(values);
        return true;
    }

    @Override
    public boolean retainAll(@NonNull Collection<?> c) {
        return false;
    }

    @Override
    public boolean removeAll(@NonNull Collection<?> c) {
        return mValues.keySet().removeAll(c);
    }

    @Override
    public void clear() {
        mValues.clear();
    }
}
