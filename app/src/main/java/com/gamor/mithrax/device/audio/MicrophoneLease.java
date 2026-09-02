package com.gamor.mithrax.device.audio;

import androidx.annotation.NonNull;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Ensures only one feature holds the microphone. Recording and STT cannot run together.
 */
public class MicrophoneLease {

    public interface Holder {
        void releaseMicrophone();
    }

    private final CopyOnWriteArrayList<Holder> holders = new CopyOnWriteArrayList<>();

    public void register(@NonNull Holder holder) {
        holders.add(holder);
    }

    public void unregister(@NonNull Holder holder) {
        holders.remove(holder);
    }

    public void releaseAll() {
        for (Holder holder : holders) {
            holder.releaseMicrophone();
        }
    }
}
