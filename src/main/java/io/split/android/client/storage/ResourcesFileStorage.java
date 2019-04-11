package io.split.android.client.storage;

import android.content.Context;

import java.io.IOException;
import java.util.List;

import io.split.android.client.utils.FileUtils;

public class ResourcesFileStorage implements IStorage {

    private FileUtils mFileUtils;

    public ResourcesFileStorage() {
        mFileUtils = new FileUtils();
    }

    @Override
    public String read(String elementId) throws IOException {
        return mFileUtils.loadFileContent(elementId);
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
}
