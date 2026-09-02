package com.gamor.mithrax.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

/**
 * Local-only preference keys. Nothing here is synced off-device.
 */
public final class AppPreferences {

    public static final String KEY_SPOKEN_REPLIES = "spoken_replies_enabled";

    private AppPreferences() {
    }

    public static boolean spokenRepliesEnabled(@NonNull Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(KEY_SPOKEN_REPLIES, false);
    }
}
