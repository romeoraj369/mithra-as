package com.gamor.mithrax.device.recording;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Process-wide recording UI state. Recording never starts unless a user action sets this.
 */
public class RecordingSession {

    public static final class State {
        public final boolean recording;
        @Nullable
        public final String recordingId;
        public final long startedAtMillis;
        @Nullable
        public final String errorMessage;

        private State(boolean recording, @Nullable String recordingId, long startedAtMillis,
                      @Nullable String errorMessage) {
            this.recording = recording;
            this.recordingId = recordingId;
            this.startedAtMillis = startedAtMillis;
            this.errorMessage = errorMessage;
        }

        @NonNull
        public static State idle() {
            return new State(false, null, 0L, null);
        }

        @NonNull
        public static State recording(@NonNull String recordingId, long startedAtMillis) {
            return new State(true, recordingId, startedAtMillis, null);
        }

        @NonNull
        public static State error(@NonNull String message) {
            return new State(false, null, 0L, message);
        }
    }

    private final MutableLiveData<State> state = new MutableLiveData<>(State.idle());

    @NonNull
    public LiveData<State> observe() {
        return state;
    }

    @NonNull
    public State current() {
        State value = state.getValue();
        return value == null ? State.idle() : value;
    }

    public boolean isRecording() {
        return current().recording;
    }

    public void setRecording(@NonNull String recordingId, long startedAtMillis) {
        state.postValue(State.recording(recordingId, startedAtMillis));
    }

    public void setIdle() {
        state.postValue(State.idle());
    }

    public void setError(@NonNull String message) {
        state.postValue(State.error(message));
    }
}
