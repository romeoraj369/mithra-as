package com.gamor.mithrax;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.gamor.mithrax.databinding.ActivityMainBinding;
import com.gamor.mithrax.device.recording.RecordingService;
import com.gamor.mithrax.device.recording.RecordingSession;
import com.gamor.mithrax.ui.ask.AskFragment;
import com.gamor.mithrax.ui.dashboard.DashboardFragment;
import com.gamor.mithrax.ui.help.HelpFragment;
import com.gamor.mithrax.ui.meetings.MeetingsFragment;
import com.gamor.mithrax.ui.meetings.RecordingPresentation;
import com.gamor.mithrax.ui.search.SearchFragment;
import com.gamor.mithrax.ui.settings.SettingsFragment;
import com.gamor.mithrax.ui.tasks.TasksFragment;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String STATE_NAV_ID = "state_nav_id";
    private static final String TAG_DASHBOARD = "dashboard";
    private static final String TAG_MEETINGS = "meetings";
    private static final String TAG_SEARCH = "search";
    private static final String TAG_ASK = "ask";
    private static final String TAG_TASKS = "tasks";
    private static final String TAG_SETTINGS = "settings";
    private static final String TAG_HELP = "help";

    private ActivityMainBinding binding;
    private ActionBarDrawerToggle toggle;
    private DrawerLayout drawerLayout;
    private int currentNavId = R.id.nav_dashboard;
    private final Handler bannerHandler = new Handler(Looper.getMainLooper());
    private final Runnable bannerTicker = this::updateRecordingBanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        drawerLayout = binding.drawerLayout;
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                binding.toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        binding.navView.setNavigationItemSelectedListener(this);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else if (currentNavId != R.id.nav_dashboard) {
                    showSection(R.id.nav_dashboard);
                    binding.navView.setCheckedItem(R.id.nav_dashboard);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        int navId = savedInstanceState != null
                ? savedInstanceState.getInt(STATE_NAV_ID, R.id.nav_dashboard)
                : R.id.nav_dashboard;
        showSection(navId);
        binding.navView.setCheckedItem(navId);

        binding.recordingBannerStop.setOnClickListener(v -> RecordingService.stop(this));
        ((MithraXApplication) getApplication()).getRecordingSession()
                .observe()
                .observe(this, this::onRecordingState);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_NAV_ID, currentNavId);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        showSection(item.getItemId());
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showSection(int itemId) {
        String tag = tagFor(itemId);
        if (tag == null) {
            return;
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
        );

        for (Fragment fragment : fragmentManager.getFragments()) {
            String fragmentTag = fragment.getTag();
            if (fragmentTag != null && isSectionTag(fragmentTag) && !tag.equals(fragmentTag)) {
                transaction.hide(fragment);
            }
        }

        Fragment selected = fragmentManager.findFragmentByTag(tag);
        if (selected == null) {
            transaction.add(R.id.nav_host_fragment_container, createSectionFragment(itemId), tag);
        } else {
            transaction.show(selected);
        }
        transaction.commit();

        currentNavId = itemId;
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(titleFor(itemId));
        }
    }

    @NonNull
    private Fragment createSectionFragment(int itemId) {
        if (itemId == R.id.nav_meetings) {
            return new MeetingsFragment();
        }
        if (itemId == R.id.nav_search) {
            return new SearchFragment();
        }
        if (itemId == R.id.nav_ask) {
            return new AskFragment();
        }
        if (itemId == R.id.nav_tasks) {
            return new TasksFragment();
        }
        if (itemId == R.id.nav_settings) {
            return new SettingsFragment();
        }
        if (itemId == R.id.nav_help) {
            return new HelpFragment();
        }
        return new DashboardFragment();
    }

    @NonNull
    private String titleFor(int itemId) {
        if (itemId == R.id.nav_meetings) {
            return getString(R.string.nav_meetings);
        }
        if (itemId == R.id.nav_search) {
            return getString(R.string.nav_search);
        }
        if (itemId == R.id.nav_ask) {
            return getString(R.string.nav_ask);
        }
        if (itemId == R.id.nav_tasks) {
            return getString(R.string.nav_tasks);
        }
        if (itemId == R.id.nav_settings) {
            return getString(R.string.nav_settings);
        }
        if (itemId == R.id.nav_help) {
            return getString(R.string.nav_help);
        }
        return getString(R.string.nav_dashboard);
    }

    @Nullable
    private String tagFor(int itemId) {
        if (itemId == R.id.nav_dashboard) {
            return TAG_DASHBOARD;
        }
        if (itemId == R.id.nav_meetings) {
            return TAG_MEETINGS;
        }
        if (itemId == R.id.nav_search) {
            return TAG_SEARCH;
        }
        if (itemId == R.id.nav_ask) {
            return TAG_ASK;
        }
        if (itemId == R.id.nav_tasks) {
            return TAG_TASKS;
        }
        if (itemId == R.id.nav_settings) {
            return TAG_SETTINGS;
        }
        if (itemId == R.id.nav_help) {
            return TAG_HELP;
        }
        return null;
    }

    private boolean isSectionTag(@NonNull String tag) {
        return TAG_DASHBOARD.equals(tag)
                || TAG_MEETINGS.equals(tag)
                || TAG_SEARCH.equals(tag)
                || TAG_ASK.equals(tag)
                || TAG_TASKS.equals(tag)
                || TAG_SETTINGS.equals(tag)
                || TAG_HELP.equals(tag);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onRecordingState(@NonNull RecordingSession.State state) {
        bannerHandler.removeCallbacks(bannerTicker);
        if (state.recording) {
            ((MithraXApplication) getApplication()).getSpeechPlaybackController().stop();
            binding.recordingBanner.setVisibility(View.VISIBLE);
            updateRecordingBanner();
            bannerHandler.postDelayed(bannerTicker, 500L);
        } else {
            binding.recordingBanner.setVisibility(View.GONE);
        }
    }

    private void updateRecordingBanner() {
        RecordingSession.State state = ((MithraXApplication) getApplication())
                .getRecordingSession()
                .current();
        if (!state.recording) {
            binding.recordingBanner.setVisibility(View.GONE);
            return;
        }
        long elapsed = Math.max(0L, System.currentTimeMillis() - state.startedAtMillis);
        binding.recordingBannerText.setText(
                getString(R.string.recording_banner_text, RecordingPresentation.formatDuration(elapsed)));
        bannerHandler.postDelayed(bannerTicker, 500L);
    }

    @Override
    protected void onDestroy() {
        bannerHandler.removeCallbacks(bannerTicker);
        super.onDestroy();
    }
}
