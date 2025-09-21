package com.gamor.mithrax;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.gamor.mithrax.databinding.ActivityMainBinding;
import com.gamor.mithrax.fragments.DashboardFragment;
import com.gamor.mithrax.fragments.HelpFragment;
import com.gamor.mithrax.fragments.MeetingsFragment;
import com.gamor.mithrax.fragments.SettingsFragment;
import com.gamor.mithrax.fragments.TasksFragment;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ActivityMainBinding binding;
    private ActionBarDrawerToggle toggle;
    private DrawerLayout drawerLayout;

    // --- Vosk and TTS related variables ---
    // These might be better managed within the relevant Fragment (e.g., DashboardFragment)
    // or through a ViewModel if shared across multiple fragments.
    // For simplicity, if they were in your original MainActivity, you can keep them here
    // but you'll need a way to communicate results to the current Fragment's UI.

    // Example: (If you keep Vosk logic in MainActivity)
    // private Model model;
    // private SpeechService speechService;
    // private TextView currentStatusTextViewInFragment; // To update Fragment's UI

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        drawerLayout = binding.drawerLayout;

        // --- Setup Toolbar ---
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            // The hamburger icon is set via app:navigationIcon in XML or by the toggle
        }

        // --- Setup Navigation Drawer ---
        toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                binding.toolbar, // Pass toolbar here to link hamburger icon
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        binding.navView.setNavigationItemSelectedListener(this);

        if (savedInstanceState == null) {
            replaceFragment(new DashboardFragment(), "Dashboard");
            binding.navView.setCheckedItem(R.id.nav_dashboard);
        }

    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;
        String title = getString(R.string.app_name); // Default title

        int itemId = item.getItemId();
        if (itemId == R.id.nav_dashboard) {
            selectedFragment = new DashboardFragment(); // Create an instance of your Java DashboardFragment
            title = "Dashboard";
        } else if (itemId == R.id.nav_meetings) {
            selectedFragment = new MeetingsFragment(); // Create an instance of your Java MeetingsFragment
            title = "Meetings";
        } else if (itemId == R.id.nav_tasks) {
            selectedFragment = new TasksFragment(); // Create an instance of your Java TasksFragment
            title = "Tasks";
        } else if (itemId == R.id.nav_settings) {
            selectedFragment = new SettingsFragment(); // Create an instance of your Java SettingsFragment
            title = "Settings";
        } else if (itemId == R.id.nav_help) {
            selectedFragment = new HelpFragment(); // Create an instance of your Java HelpFragment
            title = "Help";
        }

        if (selectedFragment != null) {
            replaceFragment(selectedFragment, title);
        }

        // Highlight the selected item (isChecked is handled by NavigationView with checkableBehavior)
        drawerLayout.closeDrawer(GravityCompat.START);
        return true; // Return true to display the item as the selected item
    }

    private void replaceFragment(Fragment fragment, String title) {
        if (fragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            // Add animations for a smoother look (React Native like)
            fragmentTransaction.setCustomAnimations(
                    R.anim.slide_in_right, // Enter animation for new fragment
                    R.anim.slide_out_left, // Exit animation for old fragment
                    R.anim.slide_in_left,  // Enter animation for old fragment (when popping back stack)
                    R.anim.slide_out_right // Exit animation for new fragment (when popping back stack)
            );
            fragmentTransaction.replace(R.id.nav_host_fragment_container, fragment);
            // fragmentTransaction.addToBackStack(null); // Optional: if you want back navigation between fragments
            fragmentTransaction.commit();

            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle(title); // Update toolbar title
            }
        }
    }


    // Handle the hamburger icon click (part of ActionBarDrawerToggle)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (toggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

//    @Override
//    public void onBackPressed() {
//        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
//            drawerLayout.closeDrawer(GravityCompat.START);
//        } else {
//            // If you're using addToBackStack for fragments, handle that first:
//            // if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
//            //     getSupportFragmentManager().popBackStack();
//            // } else {
//            //     super.onBackPressed();
//            // }
//            super.onBackPressed(); // Default behavior
//        }
//    }

    // --- Methods for Vosk, TTS, Permissions etc. (Your existing Java logic) ---
    // Example: (If Vosk is initialized here and needs to update DashboardFragment's UI)
    // public void updateFragmentStatus(String status) {
    //     Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_container);
    //     if (currentFragment instanceof DashboardFragment) {
    //         ((DashboardFragment) currentFragment).updateDashboardStatus(status);
    //     }
    //     // Add similar checks for other fragments if they also display status
    // }

    // public void startRecognitionInFragment() {
    //    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_container);
    //    if (currentFragment instanceof DashboardFragment) {
    //        // Perhaps the start button is in the fragment, or you trigger STT from activity
    //        // and the fragment is responsible for showing the "Listening..." state
    //    }
    // }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        // --- Shutdown Vosk & TTS ---
        // if (speechService != null) {
        //     speechService.stop();
        //     speechService.shutdown();
        //     speechService = null;
        // }
        // if (textToSpeech != null) {
        //     textToSpeech.stop();
        //     textToSpeech.shutdown();
        //     textToSpeech = null;
        // }
    }
}
