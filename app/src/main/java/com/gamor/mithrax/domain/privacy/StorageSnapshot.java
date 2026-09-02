package com.gamor.mithrax.domain.privacy;

/**
 * Counts of data stored on this device. No remote inventory.
 */
public final class StorageSnapshot {

    public final int conversationCount;
    public final int memoryCount;
    public final int audioFileCount;
    public final long audioBytes;
    public final long databaseBytes;

    public StorageSnapshot(int conversationCount,
                           int memoryCount,
                           int audioFileCount,
                           long audioBytes,
                           long databaseBytes) {
        this.conversationCount = conversationCount;
        this.memoryCount = memoryCount;
        this.audioFileCount = audioFileCount;
        this.audioBytes = audioBytes;
        this.databaseBytes = databaseBytes;
    }
}
