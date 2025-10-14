package com.example.allreader.ui.activities;




import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.allreader.R;
import com.example.allreader.ui.fragments.FilePickerFragment;
import com.example.allreader.ui.fragments.HomeFragment;
import com.example.allreader.ui.fragments.SettingsFragment;
import com.example.allreader.utils.PermissionUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        com.example.allreader.utils.PermissionUtils PermissionUtils = null;
        if (!PermissionUtils.hasStoragePermissions(this)) {
            PermissionUtils.requestStoragePermissions(this);
        }

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (itemId == R.id.nav_files) {
                fragment = new FilePickerFragment();
            } else if (itemId == R.id.nav_settings) {
                fragment = new SettingsFragment();
            }

            return loadFragment(fragment);
        });
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }
}
