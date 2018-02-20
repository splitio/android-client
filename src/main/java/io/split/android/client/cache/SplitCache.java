package io.split.android.client.cache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.split.android.client.storage.IStorage;
import io.split.android.client.utils.Logger;

/**
 * Created by guillermo on 11/23/17.
 */

public class SplitCache implements ISplitCache {

    private static final String SPLIT_FILE_PREFIX = "SPLITIO.split.";
    private static final String CHANGE_NUMBER_FILE_PREFIX = "SPLITIO.changeNumber";

    private final IStorage _storage;

    SplitCache(IStorage storage) {
        _storage = storage;
    }

    private String getSplitId(String splitName) {
        if (splitName.startsWith(SPLIT_FILE_PREFIX)) {
            return splitName;
        }

        return String.format("%s%s", SPLIT_FILE_PREFIX, splitName);
    }

    private String getChangeNumberId() {
        return CHANGE_NUMBER_FILE_PREFIX;
    }

    @Override
    public boolean addSplit(String splitName, String split) {
        try {
            _storage.write(getSplitId(splitName), split);
            return true;
        } catch (IOException e) {
            Logger.e(e,"Failed to add split %s", splitName);
        }
        return false;
    }

    @Override
    public boolean removeSplit(String splitName) {
        _storage.delete(splitName);
        return true;
    }

    @Override
    public boolean setChangeNumber(long changeNumber) {
        try {
            _storage.write(getChangeNumberId(),String.valueOf(changeNumber));
            return true;
        } catch (IOException e) {
            Logger.e(e, "Failed to set changeNumber");
            return false;
        }
    }

    @Override
    public long getChangeNumber() {
        try {
            return Long.parseLong(_storage.read(getChangeNumberId()));
        } catch (IOException e) {
            Logger.e(e, "Failed to get changeNumber");
            return -1;
        }
    }

    @Override
    public String getSplit(String splitName) {
        try {
            return _storage.read(getSplitId(splitName));
        } catch (IOException e) {
            Logger.e(e, "Failed to get split %s", splitName);
        }
        return null;
    }

    @Override
    public List<String> getSplitNames() {
        String[] array = _storage.getAllIds();
        List<String> storedIds = new ArrayList<>(Arrays.asList(array));
        for (String id :
                array) {
            if (!id.startsWith(SPLIT_FILE_PREFIX)) {
                storedIds.remove(id);
            }
        }
        return storedIds;
    }
}
