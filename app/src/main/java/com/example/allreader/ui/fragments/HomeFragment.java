package com.example.allreader.ui.fragments;



import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.allreader.R;
import com.example.allreader.ui.adapters.RecentFilesAdapter;
import com.example.allreader.viewmodel.FileViewModel;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private RecentFilesAdapter adapter;
    private FileViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewRecent);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);

        adapter = new RecentFilesAdapter();
        recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(FileViewModel.class);
        viewModel.getAllRecentFiles().observe(getViewLifecycleOwner(), recentFiles -> {
            adapter.setFiles(recentFiles);
        });

        return view;
    }
}
