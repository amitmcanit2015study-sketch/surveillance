package com.securityrecorder.app.ui.main;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import com.securityrecorder.app.data.model.FilterType;
import com.securityrecorder.app.data.model.VideoItem;
import com.securityrecorder.app.data.preferences.AppPreferences;
import com.securityrecorder.app.data.repository.VideoRepository;
import java.io.File;
import java.util.List;

/**
 * Main ViewModel handling video/audio/photo archive queries, vault items, search filtering, and storage synchronization.
 */
public class MainViewModel extends AndroidViewModel {

    private final VideoRepository repository;
    private final AppPreferences preferences;

    private final MutableLiveData<FilterType> currentFilter = new MutableLiveData<>(FilterType.ALL);
    private final MutableLiveData<String> currentSearchQuery = new MutableLiveData<>("");
    private final MutableLiveData<String> vaultSearchQuery = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> isRefreshing = new MutableLiveData<>(false);

    private final MediatorLiveData<List<VideoItem>> videosMediator = new MediatorLiveData<>();
    private LiveData<List<VideoItem>> currentSourceLiveData;

    private final MediatorLiveData<List<VideoItem>> vaultMediator = new MediatorLiveData<>();
    private LiveData<List<VideoItem>> currentVaultSourceLiveData;

    public MainViewModel(@NonNull Application application) {
        super(application);
        this.repository = new VideoRepository(application);
        this.preferences = new AppPreferences(application);

        setupQueryPipeline();
        setupVaultPipeline();
        syncStorage();
    }

    private void setupQueryPipeline() {
        videosMediator.addSource(currentFilter, filter -> updateSource());
        videosMediator.addSource(currentSearchQuery, query -> updateSource());
        updateSource();
    }

    private void updateSource() {
        if (currentSourceLiveData != null) {
            videosMediator.removeSource(currentSourceLiveData);
        }

        FilterType filter = currentFilter.getValue() != null ? currentFilter.getValue() : FilterType.ALL;
        String query = currentSearchQuery.getValue() != null ? currentSearchQuery.getValue() : "";

        currentSourceLiveData = repository.getFilteredVideos(filter, query);
        videosMediator.addSource(currentSourceLiveData, videosMediator::setValue);
    }

    private void setupVaultPipeline() {
        vaultMediator.addSource(vaultSearchQuery, query -> updateVaultSource());
        updateVaultSource();
    }

    private void updateVaultSource() {
        if (currentVaultSourceLiveData != null) {
            vaultMediator.removeSource(currentVaultSourceLiveData);
        }
        String query = vaultSearchQuery.getValue() != null ? vaultSearchQuery.getValue() : "";
        currentVaultSourceLiveData = repository.getVaultVideos(query);
        vaultMediator.addSource(currentVaultSourceLiveData, vaultMediator::setValue);
    }

    public LiveData<List<VideoItem>> getVideos() {
        return videosMediator;
    }

    public LiveData<List<VideoItem>> getVaultVideos() {
        return vaultMediator;
    }

    public LiveData<Boolean> getIsRefreshing() {
        return isRefreshing;
    }

    public void setFilter(FilterType filter) {
        if (currentFilter.getValue() != filter) {
            currentFilter.setValue(filter);
        }
    }

    public void setSearchQuery(String query) {
        if (!query.equals(currentSearchQuery.getValue())) {
            currentSearchQuery.setValue(query);
        }
    }

    public void setVaultSearchQuery(String query) {
        if (!query.equals(vaultSearchQuery.getValue())) {
            vaultSearchQuery.setValue(query);
        }
    }

    public void syncStorage() {
        isRefreshing.setValue(true);
        repository.syncStorageWithDatabase(() -> isRefreshing.setValue(false));
    }

    public void toggleFavorite(VideoItem item) {
        repository.toggleFavorite(item);
    }

    public void toggleFavoriteMultiple(List<VideoItem> items, Runnable onComplete) {
        repository.toggleFavoriteMultiple(items, onComplete);
    }

    public void moveToVault(List<VideoItem> items, Runnable onComplete) {
        repository.moveToVault(items, () -> {
            syncStorage();
            if (onComplete != null) onComplete.run();
        });
    }

    public void restoreFromVault(List<VideoItem> items, Runnable onComplete) {
        repository.restoreFromVault(items, () -> {
            syncStorage();
            if (onComplete != null) onComplete.run();
        });
    }

    public void deleteVideos(List<VideoItem> items, Runnable onComplete) {
        repository.deleteVideos(items, () -> {
            syncStorage();
            if (onComplete != null) onComplete.run();
        });
    }

    public void insertPhoto(File photoFile, String locationStr, Runnable onComplete) {
        repository.insertRecordedMedia(photoFile, "Photo", locationStr != null ? locationStr : "Not available", "image/jpeg", 0, "image");
        if (onComplete != null) onComplete.run();
    }

    public AppPreferences getPreferences() {
        return preferences;
    }
}
