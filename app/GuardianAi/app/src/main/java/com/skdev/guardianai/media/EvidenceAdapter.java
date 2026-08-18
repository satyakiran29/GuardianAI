package com.skdev.guardianai.media;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.skdev.guardianai.R;

import java.util.List;

/**
 * Adapter for evidence items in Evidence Vault.
 */
public class EvidenceAdapter extends RecyclerView.Adapter<EvidenceAdapter.EvidenceViewHolder> {

    public interface OnEvidenceActionListener {
        void onOpenMedia(MediaItem item);
        void onDeleteMedia(MediaItem item);
    }

    private final List<MediaItem> items;
    private final OnEvidenceActionListener listener;

    public EvidenceAdapter(List<MediaItem> items, OnEvidenceActionListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EvidenceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_evidence_card, parent, false);
        return new EvidenceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EvidenceViewHolder holder, int position) {
        MediaItem item = items.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvMeta.setText(item.getTimestamp() + " • " + item.getLocationLabel() + " • " + item.getFormattedSize());

        if (item.getType() == MediaItem.MediaType.PHOTO) {
            holder.ivType.setImageResource(R.drawable.ic_camera);
        } else if (item.getType() == MediaItem.MediaType.VIDEO) {
            holder.ivType.setImageResource(R.drawable.ic_video);
        } else {
            holder.ivType.setImageResource(R.drawable.ic_audio);
        }

        holder.btnOpen.setOnClickListener(v -> {
            if (listener != null) listener.onOpenMedia(item);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteMedia(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class EvidenceViewHolder extends RecyclerView.ViewHolder {
        ImageView ivType;
        TextView tvTitle, tvMeta;
        ImageButton btnOpen, btnDelete;

        public EvidenceViewHolder(@NonNull View itemView) {
            super(itemView);
            ivType = itemView.findViewById(R.id.iv_media_type);
            tvTitle = itemView.findViewById(R.id.tv_media_title);
            tvMeta = itemView.findViewById(R.id.tv_media_meta);
            btnOpen = itemView.findViewById(R.id.btn_play_open);
            btnDelete = itemView.findViewById(R.id.btn_delete_media);
        }
    }
}
