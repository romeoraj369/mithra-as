package com.gamor.mithrax.domain.recording;

/**
 * Local capture and transcription state. No cloud processing.
 */
public enum ProcessingStatus {
    RECORDING,
    TRANSCRIBING,
    SUMMARIZING,
    COMPLETED,
    FAILED
}
