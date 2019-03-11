package io.split.android.client.storage;

import android.content.Context;

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

    private final Context _context;
    private final File _dataFolder;

    public FileStorage(@NotNull Context context, @NotNull String folderName) {
        _context = context;
        _dataFolder = _context.getDir(folderName, Context.MODE_PRIVATE);
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

        StringBuilder fileContent = new StringBuilder("");

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
                fileOutputStream.close();
            } catch (IOException e) {
                Logger.e(e, "Failed to close file");
            }
        }
        return true;
    }

    @Override
    public void delete(String elementId) {
        _context.deleteFile(elementId);
    }

    @Override
    public String[] getAllIds() {
        return _context.fileList();
    }

    @Override
    public List<String> getAllIds(String fileNamePrefix) {
        List<String> fileNames = new ArrayList<>();
        String[] fileList = _context.fileList();

        for(String fileName : fileList) {
            if(fileName.startsWith(fileNamePrefix)){
                fileNames.add(fileName);
            }
        }
        return fileNames;
    }

    @Override
    public boolean rename(String currentId, String newId) {
        File oldFile = _context.getFileStreamPath(currentId);
        File newFile = _context.getFileStreamPath(newId);
        return oldFile.renameTo(newFile);
    }
}
