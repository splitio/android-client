package io.split.android.client.storage;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
    public boolean write(String elementId, String content) throws IOException {
        _storage.put(elementId, content);
        return true;
    }

    @Override
    public void delete(String elementId) {
        _storage.remove(elementId);
    }

    @Override
    public String[] getAllIds() {
        Set<String> allIds = _storage.keySet();
        return allIds.toArray(new String[allIds.size()]);
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
