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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
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
    private SharedPreferences prefs;
    private FileViewModel viewModel;

    private static final int MIN_FONT_SIZE = 12;
    private static final int MAX_FONT_SIZE = 32;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        switchDarkMode = view.findViewById(R.id.switchDarkMode);
        seekBarFontSize = view.findViewById(R.id.seekBarFontSize);
        tvFontSize = view.findViewById(R.id.tvFontSize);
        btnClearHistory = view.findViewById(R.id.btnClearHistory);

        prefs = requireContext().getSharedPreferences(Constants.PREFS_NAME, 0);
        viewModel = new ViewModelProvider(requireActivity()).get(FileViewModel.class);

        loadSettings();
        setupListeners();

        return view;
    }

    private void loadSettings() {
        boolean darkMode = prefs.getBoolean(Constants.PREF_THEME, false);
        switchDarkMode.setChecked(darkMode);

        int fontSize = prefs.getInt(Constants.PREF_FONT_SIZE, 16);
        fontSize = Math.max(MIN_FONT_SIZE, Math.min(fontSize, MAX_FONT_SIZE));
        seekBarFontSize.setMax(MAX_FONT_SIZE - MIN_FONT_SIZE);
        seekBarFontSize.setProgress(fontSize - MIN_FONT_SIZE);
        tvFontSize.setText(fontSize + " sp");
    }

    private void setupListeners() {
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean(Constants.PREF_THEME, isChecked).apply();
            applyTheme(isChecked);
            requireActivity().recreate();
        });

        seekBarFontSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int fontSize = MIN_FONT_SIZE + progress;
                tvFontSize.setText(fontSize + " sp");
                if (fromUser) {
                    prefs.edit().putInt(Constants.PREF_FONT_SIZE, fontSize).apply();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(requireContext(), "Font size set to " + tvFontSize.getText(),
                        Toast.LENGTH_SHORT).show();
            }
        });

        btnClearHistory.setOnClickListener(v -> showClearHistoryDialog());
    }

    private void applyTheme(boolean isDarkMode) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void showClearHistoryDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Clear History")
                .setMessage("Are you sure you want to clear all reading history?")
                .setPositiveButton("Clear", (dialog, which) -> {
                    viewModel.deleteAll();
                    Toast.makeText(requireContext(), "History cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
