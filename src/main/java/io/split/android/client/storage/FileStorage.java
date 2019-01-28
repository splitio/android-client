package io.split.android.client.storage;

import android.content.Context;

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

    public FileStorage(Context context) {
        _context = context;
    }

    /**
     * read the file content returning it as String. Could return null if file not found or could not be opened
     * @param elementId Identifier for the element to be read
     * @return String | null
     * @throws IOException
     */
    @Override
    public String read(String elementId) throws IOException {
        FileInputStream fis;
        try {
            fis = _context.openFileInput(elementId);
        } catch (FileNotFoundException e) {
            Logger.d(e.getMessage());
            return null;
        }

        StringBuilder fileContent = new StringBuilder("");

        byte[] buffer = new byte[1024];
        int n;

        try {
            while ((n = fis.read(buffer)) != -1) {
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
        FileOutputStream fos = null;
        try {
            fos = _context.openFileOutput(elementId, Context.MODE_PRIVATE);
            fos.write(content.getBytes());

        } catch (FileNotFoundException e) {
            Logger.e(e, "Failed to write content");
            throw e;
        } catch (IOException e) {
            Logger.e(e, "Failed to write content");
            throw e;
        } finally {
            try {
                fos.close();
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
