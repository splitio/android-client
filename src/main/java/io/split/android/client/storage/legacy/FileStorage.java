package io.split.android.client.storage.legacy;

import com.google.common.base.Strings;
import com.google.gson.JsonSyntaxException;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import io.split.android.client.dtos.ChunkHeader;
import io.split.android.client.dtos.Event;
import io.split.android.client.track.EventsChunk;
import io.split.android.client.track.ITrackStorage;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

/**
 * Created by guillermo on 11/22/17.
 */

@Deprecated
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

    @Deprecated
    public static class TracksFileStorage extends FileStorage implements ITrackStorage {

        private static final String FILE_NAME_PREFIX = "SPLITIO.events_chunk_id_";
        private static final String FILE_NAME_TEMPLATE = FILE_NAME_PREFIX + "%s.jsonl";
        private FileStorageHelper _fileStorageHelper;

        public TracksFileStorage(@NotNull File rootFolder, @NotNull String folderName) {
            super(rootFolder, folderName);
            _fileStorageHelper = new FileStorageHelper();
        }

        @Deprecated
        public Map<String, EventsChunk> read() {

            Map<String, EventsChunk> tracks = new HashMap<>();
            List<String> tracksFiles = getAllIds(FILE_NAME_PREFIX);

            for (String fileName : tracksFiles) {
                FileInputStream inputStream = null;
                Scanner scanner = null;
                try {
                    inputStream = new FileInputStream(new File(_dataFolder, fileName));
                    scanner = new Scanner(inputStream, FileStorageHelper.UTF8_CHARSET);
                    EventsChunk eventsChunk = null;
                    if (scanner.hasNextLine()) {
                        ChunkHeader chunkHeader = _fileStorageHelper.chunkFromLine(scanner.nextLine());
                        eventsChunk = new EventsChunk(chunkHeader.getId(), chunkHeader.getAttempt());
                        while (scanner.hasNextLine()) {
                            Event event = eventFromLine(scanner.nextLine());
                            if (event != null) {
                                eventsChunk.addEvent(event);
                            }
                        }
                    }
                    if (eventsChunk != null && eventsChunk.getEvents() != null && eventsChunk.getEvents().size() > 0) {
                        tracks.put(eventsChunk.getId(), eventsChunk);
                    }
                    _fileStorageHelper.logIfScannerException(scanner, "An error occurs parsing track events from JsonL files");
                } catch (FileNotFoundException e) {
                    Logger.w("No cached track files found");
                } finally {
                    _fileStorageHelper.closeFileInputStream(inputStream);
                    _fileStorageHelper.closeScanner(scanner);
                }
            }
            delete(tracksFiles);
            return tracks;
        }

        @Deprecated
        public void write(Map<String, EventsChunk> tracks) {
            List<EventsChunk> savingTracks = new ArrayList<>(tracks.values());
            for (EventsChunk chunk : savingTracks) {
                FileWriter fileWriter = null;
                List<Event> events = chunk.getEvents();
                if (events != null && events.size() > 0) {
                    try {
                        fileWriter = _fileStorageHelper.fileWriterFrom(_dataFolder, String.format(FILE_NAME_TEMPLATE, chunk.getId()));
                        ChunkHeader chunkHeader = new ChunkHeader(chunk.getId(), chunk.getAttempt());
                        _fileStorageHelper.writeChunkHeaderLine(chunkHeader, fileWriter);
                        for (Event event : events) {
                            writeEventLine(event, fileWriter);
                        }
                    } catch (IOException e) {
                        Logger.e("Error writing track events chunk: " + FILE_NAME_TEMPLATE + ": " + e.getLocalizedMessage());
                    } finally {
                        _fileStorageHelper.closeFileWriter(fileWriter);
                    }
                }
            }
        }

        private void writeEventLine(Event event, FileWriter fileWriter) throws IOException {
            String jsonEvent = Json.toJson(event);
            fileWriter.write(jsonEvent);
            fileWriter.write(FileStorageHelper.LINE_SEPARATOR);
        }

        private Event eventFromLine(String jsonEvent) {

            if (Strings.isNullOrEmpty(jsonEvent)) {
                return null;
            }

            Event event = null;
            try {
                event = Json.fromJson(jsonEvent, Event.class);
            } catch (JsonSyntaxException e) {
                Logger.e("Could not parse event: " + jsonEvent);
            }
            return event;
        }
    }
}
