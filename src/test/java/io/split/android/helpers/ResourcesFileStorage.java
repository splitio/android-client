package io.split.android.helpers;

import java.io.IOException;
import java.util.List;

import io.split.android.client.storage.legacy.IStorage;

public class ResourcesFileStorage implements IStorage {
    @Override
    public String read(String elementId) throws IOException {
        return new FileHelper().loadFileContent(elementId);
    }

    @Override
    public boolean write(String elementId, String content) throws IOException {
        return false;
    }

    @Override
    public void delete(String elementId) {

    }

    @Override
    public String[] getAllIds() {
        return new String[0];
    }

    @Override
    public List<String> getAllIds(String fileNamePrefix) {
        return null;
    }

    @Override
    public boolean rename(String currentId, String newId) {
        return false;
    }

    @Override
    public boolean exists(String elementId) {
        return false;
    }

    @Override
    public long fileSize(String elementId) {
        return 0L;
    }

    @Override
    public void delete(List<String> files) {
    }

    @Override
    public long lastModified(String elementId) {
        return 0;
    }
}
