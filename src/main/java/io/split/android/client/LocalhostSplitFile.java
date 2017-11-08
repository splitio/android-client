package io.split.android.client;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import timber.log.Timber;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class LocalhostSplitFile extends Thread {

    private final LocalhostSplitFactory _splitFactory;
    private final File _file;
    private final WatchService _watcher;
    private final AtomicBoolean _stop;

    public LocalhostSplitFile(LocalhostSplitFactory splitFactory, String directory, String fileName) throws IOException {
        Preconditions.checkNotNull(directory);
        Preconditions.checkNotNull(fileName);

        _splitFactory = Preconditions.checkNotNull(splitFactory);
        _file = new File(directory, fileName);
        _watcher = FileSystems.getDefault().newWatchService();
        _stop = new AtomicBoolean(false);
    }

    private boolean isStopped() {
        return _stop.get();
    }

    public void stopThread() {
        _stop.set(true);
    }

    public void registerWatcher() throws IOException {
        Path path = _file.toPath().toAbsolutePath().getParent();
        path.register(_watcher, new WatchEvent.Kind[]{StandardWatchEventKinds.ENTRY_MODIFY});
    }

    @Override
    public void run() {
        try {
            while (!isStopped()) {
                WatchKey key;
                try {
                    key = _watcher.poll(250, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    stopThread();
                    return;
                }
                if (key == null) {
                    Thread.yield();
                    continue;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();

                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        Thread.yield();
                        continue;
                    } else if (kind == java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
                            && filename.toString().equals(_file.getName())) {
                        Map<String, String> featureToSplitMap = readOnSplits();
                        _splitFactory.updateFeatureToTreatmentMap(featureToSplitMap);
                        Timber.i("Detected change in Local Splits file - Splits Reloaded! file=%s", _file.getPath());
                    }
                    boolean valid = key.reset();
                    if (!valid) {
                        break;
                    }
                }
                Thread.yield();
            }
        } catch (IOException e) {
            Timber.e(e, "Error reading file: path=%s", _file.getPath());
            stopThread();
        }
    }

    public Map<String, String> readOnSplits() throws IOException {
        Map<String, String> onSplits = Maps.newHashMap();

        try (BufferedReader reader = new BufferedReader(new FileReader(_file))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] feature_treatment = line.split("\\s+");

                if (feature_treatment.length != 2) {
                    Timber.i("Ignoring line since it does not have exactly two columns: %s", line);
                    continue;
                }

                onSplits.put(feature_treatment[0], feature_treatment[1]);
                Timber.i("100%% of keys will see %s for %s", feature_treatment[1], feature_treatment[0]);
            }
        } catch (FileNotFoundException e) {
            Timber.w(e, "There was no file named %s found. " +
                    "We created a split client that returns default " +
                    "treatments for all features for all of your users. " +
                    "If you wish to return a specific treatment for a feature, " +
                    "enter the name of that feature name and treatment name separated " +
                    "by whitespace in %s; one pair per line. Empty lines or lines " +
                    "starting with '#' are considered comments", _file.getPath(), _file.getPath());
        }

        return onSplits;
    }
}