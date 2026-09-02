package com.gamor.mithrax.ui.privacy;

import com.gamor.mithrax.domain.privacy.StorageSnapshot;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PrivacyPresentationTest {

    @Test
    public void formatsBytesAndSnapshot() {
        assertEquals("512 B", PrivacyPresentation.formatBytes(512L));
        assertEquals("1.5 KB", PrivacyPresentation.formatBytes(1536L));
        StorageSnapshot snapshot = new StorageSnapshot(2, 5, 1, 2048L, 4096L);
        String text = PrivacyPresentation.formatSnapshot(snapshot);
        assertTrue(text.contains("2 conversations"));
        assertTrue(text.contains("5 memories"));
        assertTrue(text.contains("1 audio files"));
    }
}
