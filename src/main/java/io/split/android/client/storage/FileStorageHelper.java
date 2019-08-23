package io.split.android.client.storage;

import com.google.common.base.Strings;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

import io.split.android.client.dtos.ChunkHeader;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

public class FileStorageHelper {

    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    public static final String UTF8_CHARSET = "UTF-8";

    public List<ChunkHeader> readAndParseChunkHeadersFile(IStorage storage, String fileName) {
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
        fileWriter.write(String.format(jsonChunkHeader));
        fileWriter.write(LINE_SEPARATOR);
    }

    private ChunkHeader newHeaderChunk() {
        return new ChunkHeader(UUID.randomUUID().toString(), 1);
    }


}