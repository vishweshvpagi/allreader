package com.example.allreader.ui.fragments;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.allreader.R;
import com.example.allreader.utils.Constants;
import com.example.allreader.viewmodel.FileViewModel;

public class SettingsFragment extends Fragment {

    private Switch switchDarkMode;
    private SeekBar seekBarFontSize;
    private TextView tvFontSize;
    private Button btnClearHistory;
    private FileViewModel viewModel;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        switchDarkMode = view.findViewById(R.id.switchDarkMode);
        seekBarFontSize = view.findViewById(R.id.seekBarFontSize);
        tvFontSize = view.findViewById(R.id.tvFontSize);
        btnClearHistory = view.findViewById(R.id.btnClearHistory);

        viewModel = new ViewModelProvider(this).get(FileViewModel.class);
        prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, 0);

        setupListeners();
        loadSettings();

        return view;
    }

    private void setupListeners() {
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(Constants.PREF_THEME, isChecked).apply();
        });

        seekBarFontSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int fontSize = progress + 12;
                tvFontSize.setText(fontSize + " sp");
                prefs.edit().putInt(Constants.PREF_FONT_SIZE, fontSize).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnClearHistory.setOnClickListener(v -> {
            viewModel.deleteAll();
        });
    }

    private void loadSettings() {
        boolean darkMode = prefs.getBoolean(Constants.PREF_THEME, false);
        switchDarkMode.setChecked(darkMode);

        int fontSize = prefs.getInt(Constants.PREF_FONT_SIZE, 16);
        seekBarFontSize.setProgress(fontSize - 12);
        tvFontSize.setText(fontSize + " sp");
    }
}
