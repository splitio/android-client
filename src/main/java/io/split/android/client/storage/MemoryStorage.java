package io.split.android.client.storage;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by guillermo on 11/23/17.
 */

public class MemoryStorage implements IStorage {

    private final Map<String, String> _storage = new HashMap<>();

    @Override
    public String read(String elementId) throws IOException {
        return _storage.get(elementId);
    }

    @Override
    public void write(String elementId, String content) throws IOException {
        _storage.put(elementId, content);
    }

    @Override
    public void delete(String elementId) {
        _storage.remove(elementId);
    }

    @Override
    public String[] getAllIds() {
        Collection<String> ids = _storage.values();
        return ids.toArray(new String[ids.size()]);
    }

    @Override
    public boolean rename(String currentId, String newId) {
        if (_storage.containsKey(currentId)) {
            _storage.put(newId, _storage.get(currentId));
            _storage.remove(currentId);
            return true;
        }
        return false;
    }
}
