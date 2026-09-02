package com.gamor.mithrax.data.local.recording;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class RecordingFileStoreTest {

    @Test
    public void deleteQuietlyRemovesExistingFileAndIgnoresMissing() throws Exception {
        File file = RecordingFileStore.fileForId(ApplicationProvider.getApplicationContext(), "clip");
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write("abc".getBytes(StandardCharsets.UTF_8));
        }
        assertTrue(file.exists());
        RecordingFileStore.deleteQuietly(file.getAbsolutePath());
        assertFalse(file.exists());
        RecordingFileStore.deleteQuietly("");
        RecordingFileStore.deleteQuietly(null);
    }

    @Test
    public void deleteAllInDirectoryClearsRecordingsFolder() throws Exception {
        File file = RecordingFileStore.fileForId(ApplicationProvider.getApplicationContext(), "one");
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write("abc".getBytes(StandardCharsets.UTF_8));
        }
        RecordingFileStore.deleteAllInDirectory(ApplicationProvider.getApplicationContext());
        assertFalse(file.exists());
        assertEquals(0, RecordingFileStore.fileCount(
                RecordingFileStore.directory(ApplicationProvider.getApplicationContext())));
    }
}
