package com.gamor.mithrax.ui.privacy;

import androidx.annotation.NonNull;

import com.gamor.mithrax.domain.privacy.StorageSnapshot;

import java.util.Locale;

public final class PrivacyPresentation {

    private PrivacyPresentation() {
    }

    @NonNull
    public static String formatBytes(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        double kib = bytes / 1024.0;
        if (kib < 1024.0) {
            return String.format(Locale.US, "%.1f KB", kib);
        }
        double mib = kib / 1024.0;
        return String.format(Locale.US, "%.1f MB", mib);
    }

    @NonNull
    public static String formatSnapshot(@NonNull StorageSnapshot snapshot) {
        return snapshot.conversationCount + " conversations\n"
                + snapshot.memoryCount + " memories\n"
                + snapshot.audioFileCount + " audio files ("
                + formatBytes(snapshot.audioBytes) + ")\n"
                + "Database " + formatBytes(snapshot.databaseBytes);
    }
}
