package io.split.android.client.storage.legacy;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.split.android.client.utils.Logger;

/**
 * Created by guillermo on 11/22/17.
 */


public class FileStorage implements IStorage {

    protected final File _dataFolder;

    public FileStorage(@NotNull File rootFolder, @NotNull String folderName) {
        _dataFolder = new File(rootFolder, folderName);
        if(!_dataFolder.exists()) {
            if(!_dataFolder.mkdir()) {
                Logger.e("There was a problem creating Split cache folder");
            }
        }
    }

    /**
     * read the file content returning it as String. Could return null if file not found or could not be opened
     * @param elementId Identifier for the element to be read
     * @return String | null
     * @throws IOException
     */
    @Override
    public String read(String elementId) throws IOException {

        File file = new File(_dataFolder, elementId);
        FileInputStream fileInputStream;
        try {
            fileInputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            Logger.d(e.getMessage());
            return null;
        }

        StringBuilder fileContent = new StringBuilder();

        byte[] buffer = new byte[1024];
        int n;

        try {
            while ((n = fileInputStream.read(buffer)) != -1) {
                fileContent.append(new String(buffer, 0, n));
            }
            return fileContent.toString();
        } catch (IOException e) {
            Logger.e(e, "Can't read file");
            throw e;
        }
    }

    @Override
    public boolean write(String elementId, String content) throws IOException {
        File file = new File(_dataFolder, elementId);
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(content.getBytes());
        } catch (FileNotFoundException e) {
            Logger.e(e, "Failed to write content");
            throw e;
        } catch (IOException e) {
            Logger.e(e, "Failed to write content");
            throw e;
        } finally {
            try {
                if(fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                Logger.e(e, "Failed to close file");
            }
        }
        return true;
    }

    @Override
    public void delete(String elementId) {
        File fileToDelete = new File(_dataFolder, elementId);
        if(!fileToDelete.delete()) {
            Logger.e("There was a problem removing Split cache file");
        }
    }

    @Override
    public void delete(List<String> files) {
        for(String fileName : files) {
            delete(fileName);
        }
    }

    @Override
    public String[] getAllIds() {
        File dataFolder = new File(_dataFolder, ".");
        File[] fileList = dataFolder.listFiles();
        if(fileList == null) {
            return new String[0];
        }
        String[] nameList = new String[fileList.length];
        int i = 0;
        for(File file : fileList) {
            nameList[i] = file.getName();
            i++;
        }
        return nameList;
    }

    @Override
    public List<String> getAllIds(String fileNamePrefix) {
        List<String> fileNames = new ArrayList<>();
        String[] fileIds = getAllIds();
        for(String fileName : fileIds) {
            if(fileName.startsWith(fileNamePrefix)){
                fileNames.add(fileName);
            }
        }
        return fileNames;
    }

    @Override
    public boolean rename(String currentId, String newId) {
        File oldFile = new File(_dataFolder, currentId);
        File newFile = new File(_dataFolder, newId);
        return oldFile.renameTo(newFile);
    }

    @Override
    public boolean exists(String elementId) {
        File file = new File(_dataFolder, elementId);
        return file.exists();
    }

    public long fileSize(String elementId) {
        return new File(_dataFolder, elementId).length();
    }
}
