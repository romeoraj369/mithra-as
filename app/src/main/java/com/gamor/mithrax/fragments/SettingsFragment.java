package com.gamor.mithrax.fragments;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.gamor.mithrax.R; // Make sure your R file is imported correctly

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences_settings, rootKey);

        // --- Example: Find and handle a SwitchPreference ---
        SwitchPreferenceCompat notificationsSwitch = findPreference("notifications_enabled");
        if (notificationsSwitch != null) {
            notificationsSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean isEnabled = (Boolean) newValue;
                if (isEnabled) {
                    Toast.makeText(getContext(), "Notifications Enabled", Toast.LENGTH_SHORT).show();
                    // Add logic to actually enable notifications
                } else {
                    Toast.makeText(getContext(), "Notifications Disabled", Toast.LENGTH_SHORT).show();
                    // Add logic to actually disable notifications
                }
                return true; // True to update the state of the Preference with the new value
            });
        }

        // --- Example: Find and handle a ListPreference ---
        ListPreference syncFrequencyList = findPreference("sync_frequency");
        if (syncFrequencyList != null) {
            // The summary is automatically updated by the ListPreference if "%s" is used.
            // You can add a listener if you need to do more when it changes.
            syncFrequencyList.setOnPreferenceChangeListener((preference, newValue) -> {
                String selectedValue = (String) newValue;
                Toast.makeText(getContext(), "Sync frequency set to: " + selectedValue + " minutes", Toast.LENGTH_SHORT).show();
                // Add logic to act on the new sync frequency
                return true;
            });
        }

        // --- Example: Find and handle a regular Preference (for actions) ---
        Preference editProfilePreference = findPreference("edit_profile");
        if (editProfilePreference != null) {
            editProfilePreference.setOnPreferenceClickListener(preference -> {
                Toast.makeText(getContext(), "Edit Profile clicked!", Toast.LENGTH_SHORT).show();
                // Navigate to an Edit Profile screen or show a dialog
                return true;
            });
        }

        Preference logOutPreference = findPreference("log_out");
        if (logOutPreference != null) {
            logOutPreference.setOnPreferenceClickListener(preference -> {
                Toast.makeText(getContext(), "Log Out clicked!", Toast.LENGTH_SHORT).show();
                // Implement your log out logic here
                return true;
            });
        }
    }
}
