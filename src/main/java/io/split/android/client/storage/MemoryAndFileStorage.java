package io.split.android.client.storage;

import android.content.Context;

import java.io.IOException;
import java.util.List;

/**
 * Created by guillermo on 11/24/17.
 */

public class MemoryAndFileStorage implements IStorage {

    private final MemoryStorage _memoryStorage;
    private final FileStorage _fileStorage;

    public MemoryAndFileStorage(Context context, String dataFolder) {
        _memoryStorage = new MemoryStorage();
        _fileStorage = new FileStorage(context, dataFolder);
    }

    @Override
    public String read(String elementId) throws IOException {
        String result = _memoryStorage.read(elementId);
        if (result != null) {
            return result;
        }

        result = _fileStorage.read(elementId);
        if (result != null) {
            _memoryStorage.write(elementId, result);
            return result;
        }

        return null;
    }

    @Override
    public boolean write(String elementId, String content) throws IOException {
        _memoryStorage.write(elementId, content);
        _fileStorage.write(elementId, content);
        return true;
    }

    @Override
    public void delete(String elementId) {
        _memoryStorage.delete(elementId);
        _fileStorage.delete(elementId);
    }

    @Override
    public String[] getAllIds() {
        return _fileStorage.getAllIds();
    }

    @Override
    public List<String> getAllIds(String fileNamePrefix) {
        return _fileStorage.getAllIds(fileNamePrefix);
    }

    @Override
    public boolean rename(String currentId, String newId) {
        if (_fileStorage.rename(currentId, newId)) {
            _memoryStorage.rename(currentId, newId);
            return true;
        }
        return false;
    }
}
