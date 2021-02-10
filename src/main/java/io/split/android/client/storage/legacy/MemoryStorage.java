package io.split.android.client.storage.legacy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by guillermo on 11/23/17.
 */
@Deprecated
public class MemoryStorage implements IStorage {

    private final Map<String, String> _storage = new HashMap<>();

    @SuppressWarnings("RedundantThrows")
    @Override
    public String read(String elementId) throws IOException {
        return _storage.get(elementId);
    }

    @SuppressWarnings("RedundantThrows")
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
        return allIds.toArray(new String[0]);
    }

    @Override
    public List<String> getAllIds(String fileNamePrefix) {
        List<String> allIds = new ArrayList<>();
        for(String elementId : _storage.keySet()) {
            if(elementId.startsWith(fileNamePrefix)) {
                allIds.add(elementId);
            }
        }
        return allIds;
    }

    @Override
    public boolean rename(String currentId, String newId) {
        String current = _storage.get(currentId);
        if (current != null) {
            _storage.put(newId, current);
            _storage.remove(currentId);
            return true;
        }
        return false;
    }

    @Override
    public boolean exists(String elementId) {
        return _storage.containsKey(elementId);
    }


    @Override
    public long fileSize(String elementId) {
        String content = _storage.get(elementId);
        if(content != null) {
            return  content.getBytes().length;
        }
        return 0L;
    }

    @Override
    public void delete(List<String> files) {
        for(String fileName : files) {
            delete(fileName);
        }
    }

    @Override
    public long lastModified(String elementId) {
        return System.currentTimeMillis();
    }
}
