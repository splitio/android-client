package io.split.android.client.storage;

import com.google.common.base.Strings;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.UUID;

import io.split.android.client.dtos.ChunkHeader;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

public class FileStorageHelper {

    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    public static final String UTF8_CHARSET = "UTF-8";

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
        fileWriter.write(String.format(jsonChunkHeader));
        fileWriter.write(LINE_SEPARATOR);
    }

    private ChunkHeader newHeaderChunk() {
        return new ChunkHeader(UUID.randomUUID().toString(), 1);
    }


}