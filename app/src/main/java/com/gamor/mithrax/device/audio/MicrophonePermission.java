package com.gamor.mithrax.device.audio;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public final class MicrophonePermission {

    private MicrophonePermission() {
    }

    public static boolean isGranted(@NonNull Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }
}
