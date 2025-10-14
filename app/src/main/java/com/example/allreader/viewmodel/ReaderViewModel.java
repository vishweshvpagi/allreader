package com.example.allreader.viewmodel;


import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ReaderViewModel extends ViewModel {
    private MutableLiveData<Integer> currentPage = new MutableLiveData<>();
    private MutableLiveData<Integer> totalPages = new MutableLiveData<>();
    private MutableLiveData<Float> fontSize = new MutableLiveData<>();

    public ReaderViewModel() {
        currentPage.setValue(0);
        totalPages.setValue(0);
        fontSize.setValue(16f);
    }

    public MutableLiveData<Integer> getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int page) {
        currentPage.setValue(page);
    }

    public MutableLiveData<Integer> getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int pages) {
        totalPages.setValue(pages);
    }

    public MutableLiveData<Float> getFontSize() {
        return fontSize;
    }

    public void setFontSize(float size) {
        fontSize.setValue(size);
    }
}
