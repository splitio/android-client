package io.split.android.client;

import org.junit.Assert;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.split.android.client.storage.FileStorage;
import io.split.android.client.storage.IStorage;

public class FileStorageTest {

    private static final String ROOT_FOLDER = "./build";
    private static final String FOLDER = "thefolder";
    private static final String PREFIX = "the-prefix.";
    private static final String CONTENT = "{\"value\": 1}";

    private IStorage storage;

    @Before
    public void setup(){

        File rootFolder = new File(ROOT_FOLDER);
        File folder = new File(rootFolder, FOLDER);
        if(folder.exists()) {
            for(File file : folder.listFiles()){
                file.delete();
            }
           folder.delete();
        }

        storage = new FileStorage(rootFolder, FOLDER);
        try {
            storage.write("f1", CONTENT);
            storage.write("f2", CONTENT);
            storage.write(PREFIX + "f2", CONTENT);
            storage.write(PREFIX + "f1", CONTENT);
        } catch (IOException e) {
        }
    }

    @Test
    public void createStorageWhenFolderExists() {
        File rootFolder = new File(ROOT_FOLDER);
        IStorage storage = new FileStorage(rootFolder, FOLDER);
        String c1 = null;

        try {
            c1 = storage.read("f2");
        } catch (IOException e) {
            e.printStackTrace();
        }

        Assert.assertNotNull(c1);
    }

    @Test
    public void testReadFiles(){
        String c1 = null;
        String c2 = null;
        try {
            c1 = storage.read("f1");
            c2 = storage.read(PREFIX + "f2");
        } catch (IOException e) {
        }

        Assert.assertEquals(CONTENT, c1);
        Assert.assertEquals(CONTENT, c2);

    }

    @Test
    public void testRemoveFile() {
        storage.delete("f1");
        String c1 = null;
        try {
            c1 = storage.read("f1");
        } catch (IOException e) {
        }

        Assert.assertNull(c1);
    }

    @Test
    public void testRenameFile() {
        String c1 = null;
        storage.rename("f1", "f1copy");
        try {
            c1 = storage.read("f1copy");
        } catch (IOException e) {
        }
        Assert.assertNotNull(c1);
    }

    @Test
    public void testAllIdsNoPrefix() {
        String[] ids = storage.getAllIds();
        Set<String> setIds = new HashSet<>(Arrays.asList(ids));

        Assert.assertTrue(setIds.contains("f1"));
        Assert.assertTrue(setIds.contains("f2"));
        Assert.assertTrue(setIds.contains(PREFIX + "f1"));
        Assert.assertTrue(setIds.contains(PREFIX + "f2"));
        Assert.assertFalse(setIds.contains("OTHER"));
    }

    @Test
    public void testAllIdsWithPrefix() {
        List<String> ids = storage.getAllIds(PREFIX);
        Set<String> setIds = new HashSet<>(ids);

        Assert.assertFalse(setIds.contains("f1"));
        Assert.assertFalse(setIds.contains("f2"));
        Assert.assertTrue(setIds.contains(PREFIX + "f1"));
        Assert.assertTrue(setIds.contains(PREFIX + "f2"));

    }

}
