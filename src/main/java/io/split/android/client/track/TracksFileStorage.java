package io.split.android.client.track;

import com.google.common.base.Strings;
import com.google.gson.JsonSyntaxException;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import io.split.android.client.dtos.ChunkHeader;
import io.split.android.client.dtos.Event;
import io.split.android.client.storage.legacy.FileStorage;
import io.split.android.client.storage.legacy.FileStorageHelper;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

public class TracksFileStorage extends FileStorage implements ITrackStorage {

    private static final String FILE_NAME_PREFIX = "SPLITIO.events_chunk_id_";
    private static final String FILE_NAME_TEMPLATE = FILE_NAME_PREFIX + "%s.jsonl";
    private FileStorageHelper _fileStorageHelper;

    public TracksFileStorage(@NotNull File rootFolder, @NotNull String folderName) {
        super(rootFolder, folderName);
        _fileStorageHelper = new FileStorageHelper();
    }

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
