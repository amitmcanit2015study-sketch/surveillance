package com.securityrecorder.app.ui.main;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import com.securityrecorder.app.data.model.VideoItem;
import com.securityrecorder.app.databinding.ItemVideoGridBinding;
import com.securityrecorder.app.databinding.ItemVideoListBinding;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * RecyclerView adapter supporting dynamic toggle between Grid and List item layouts and multi-select batch deletion.
 */
public class VideoAdapter extends ListAdapter<VideoItem, VideoViewHolder> {

    public static final int VIEW_TYPE_GRID = 1;
    public static final int VIEW_TYPE_LIST = 2;

    public interface OnVideoItemClickListener {
        void onVideoClick(VideoItem video);
        void onFavoriteToggle(VideoItem video);
        void onVideoLongClick(VideoItem video);
        void onSelectionChanged(VideoItem video, boolean isSelected);
    }

    private boolean isGridLayout = true;
    private boolean isSelectionMode = false;
    private final OnVideoItemClickListener listener;

    public VideoAdapter(boolean isGridLayout, OnVideoItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.isGridLayout = isGridLayout;
        this.listener = listener;
    }

    public void setGridLayout(boolean gridLayout) {
        if (this.isGridLayout != gridLayout) {
            this.isGridLayout = gridLayout;
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    public boolean isGridLayout() {
        return isGridLayout;
    }

    public boolean isSelectionMode() {
        return isSelectionMode;
    }

    public void setSelectionMode(boolean selectionMode) {
        this.isSelectionMode = selectionMode;
        if (!selectionMode) {
            clearSelection();
        } else {
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    public void selectAll(boolean select) {
        for (int i = 0; i < getItemCount(); i++) {
            VideoItem item = getItem(i);
            if (item != null) {
                item.setSelected(select);
            }
        }
        notifyItemRangeChanged(0, getItemCount());
    }

    public void clearSelection() {
        for (int i = 0; i < getItemCount(); i++) {
            VideoItem item = getItem(i);
            if (item != null) {
                item.setSelected(false);
            }
        }
        notifyItemRangeChanged(0, getItemCount());
    }

    public List<VideoItem> getSelectedItems() {
        List<VideoItem> selected = new ArrayList<>();
        for (int i = 0; i < getItemCount(); i++) {
            VideoItem item = getItem(i);
            if (item != null && item.isSelected()) {
                selected.add(item);
            }
        }
        return selected;
    }

    public int getSelectedCount() {
        int count = 0;
        for (int i = 0; i < getItemCount(); i++) {
            VideoItem item = getItem(i);
            if (item != null && item.isSelected()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int getItemViewType(int position) {
        return isGridLayout ? VIEW_TYPE_GRID : VIEW_TYPE_LIST;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_GRID) {
            ItemVideoGridBinding binding = ItemVideoGridBinding.inflate(inflater, parent, false);
            return new VideoViewHolder.GridViewHolder(binding);
        } else {
            ItemVideoListBinding binding = ItemVideoListBinding.inflate(inflater, parent, false);
            return new VideoViewHolder.ListViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        VideoItem item = getItem(position);
        if (item != null) {
            holder.bind(item, isSelectionMode, listener);
        }
    }

    private static final DiffUtil.ItemCallback<VideoItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<VideoItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull VideoItem oldItem, @NonNull VideoItem newItem) {
            return oldItem.getId() == newItem.getId() || Objects.equals(oldItem.getFilePath(), newItem.getFilePath());
        }

        @Override
        public boolean areContentsTheSame(@NonNull VideoItem oldItem, @NonNull VideoItem newItem) {
            return oldItem.isFavorite() == newItem.isFavorite()
                    && oldItem.isSelected() == newItem.isSelected()
                    && Objects.equals(oldItem.getTitle(), newItem.getTitle())
                    && oldItem.getSizeBytes() == newItem.getSizeBytes()
                    && oldItem.getDurationMs() == newItem.getDurationMs();
        }
    };
}
