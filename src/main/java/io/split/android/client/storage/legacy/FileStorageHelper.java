package io.split.android.client.storage.legacy;

import com.google.common.base.Strings;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import io.split.android.client.dtos.ChunkHeader;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;
import io.split.android.client.utils.MemoryUtils;
import io.split.android.client.utils.MemoryUtilsImpl;

@Deprecated
public class FileStorageHelper {

    public static final String LINE_SEPARATOR = System.getProperty("line.separator") != null ? System.getProperty("line.separator") : "\n";
    public static final String UTF8_CHARSET = "UTF-8";
    private static final int MEMORY_ALLOCATION_TIMES = 2;

    private final MemoryUtils mMemoryUtils;

    public FileStorageHelper() {
        this(new MemoryUtilsImpl());
    }

    public FileStorageHelper(MemoryUtils memoryUtils) {
        mMemoryUtils = memoryUtils;
    }

    public List<ChunkHeader> readAndParseChunkHeadersFile(String fileName, IStorage storage) {
        if(isOutdated(storage.lastModified(fileName))) {
            return null;
        }
        List<ChunkHeader> headers = null;
        try {
            String headerContent = storage.read(fileName);
            if(headerContent != null) {
                headers = Json.fromJson(headerContent, ChunkHeader.CHUNK_HEADER_TYPE);
            }
        } catch (IOException ioe) {
            Logger.e(ioe, "Unable chunks headers information from disk: " + ioe.getLocalizedMessage());
        } catch (JsonSyntaxException syntaxException) {
            Logger.e(syntaxException, "Unable to parse saved chunks headers: " + syntaxException.getLocalizedMessage());
        } catch (Exception e) {
            Logger.e(e, "Error loading chunk headers from disk: " + e.getLocalizedMessage());
        }
        return  headers;
    }

    public ChunkHeader chunkFromLine(String jsonChunk) {

        if (Strings.isNullOrEmpty(jsonChunk)) {
            return newHeaderChunk();
        }

        ChunkHeader chunkHeader;
        try {
            chunkHeader = Json.fromJson(jsonChunk, ChunkHeader.class);
        } catch (JsonSyntaxException e) {
            chunkHeader = newHeaderChunk();
        }
        return chunkHeader;
    }

    public FileWriter fileWriterFrom(File dataFolder, String fileName) throws IOException {
        File file = new File(dataFolder, fileName);
        return new FileWriter(file);
    }

    public void closeFileInputStream(FileInputStream stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                Logger.w("Error closing file input stream: " + e.getLocalizedMessage());
            }
        }
    }

    public void closeScanner(Scanner scanner) {
        if (scanner != null) {
            scanner.close();
        }
    }

    public void closeFileWriter(FileWriter fileWriter) {
        if (fileWriter != null) {
            try {
                fileWriter.close();
            } catch (IOException e) {
                Logger.w("Error closing file writer: " + e.getLocalizedMessage());
            }
        }
    }

    public void logIfScannerException(Scanner scanner, String message) {
        if (scanner.ioException() != null) {
            Logger.e(message + scanner.ioException().getLocalizedMessage());
        }
    }

    public void writeChunkHeaderLine(ChunkHeader chunkHeader, FileWriter fileWriter) throws IOException {
        String jsonChunkHeader = Json.toJson(chunkHeader);
        fileWriter.write(jsonChunkHeader);
        fileWriter.write(LINE_SEPARATOR);
    }

    public String checkMemoryAndReadFile(String name, IStorage storage) {
        if(isOutdated(storage.lastModified(name))) {
            return null;
        }
        String fileContent = null;
        long fileSize = storage.fileSize(name);
        if(fileSize > 0 && mMemoryUtils.isMemoryAvailableToAllocate(fileSize, MEMORY_ALLOCATION_TIMES)) {
            try {
                fileContent = storage.read(name);
            } catch (IOException e) {
                Logger.e(e, "Unable to load file from disk: " + name + " error: " + e.getLocalizedMessage());
            }
        } else {
            Logger.w("Unable to parse file " + name + ". Memory not available");
        }
        return fileContent;
    }

    private ChunkHeader newHeaderChunk() {
        return new ChunkHeader(UUID.randomUUID().toString(), 1);
    }

    public boolean isOutdated(long timestamp) {
        long now = System.currentTimeMillis() / 1000;
        return (now - ServiceConstants.RECORDED_DATA_EXPIRATION_PERIOD > timestamp);
    }

}