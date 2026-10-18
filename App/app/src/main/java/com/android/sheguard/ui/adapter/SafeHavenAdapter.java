package com.android.sheguard.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import androidx.core.widget.ImageViewCompat;
import com.android.sheguard.R;
import com.android.sheguard.model.SafeHavenModel;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class SafeHavenAdapter extends RecyclerView.Adapter<SafeHavenAdapter.ViewHolder> {

    public interface OnSafeHavenActionListener {
        void onCallClicked(SafeHavenModel model);
        void onDirectionsClicked(SafeHavenModel model);
        void onSafeRouteClicked(SafeHavenModel model);
        void onCardClicked(SafeHavenModel model);
    }

    private final Context context;
    private final List<SafeHavenModel> items = new ArrayList<>();
    private final OnSafeHavenActionListener listener;

    public SafeHavenAdapter(@NonNull Context context, List<SafeHavenModel> items, OnSafeHavenActionListener listener) {
        this.context = context;
        if (items != null) {
            this.items.addAll(items);
        }
        this.listener = listener;
    }

    public void updateList(List<SafeHavenModel> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.view_safe_haven_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SafeHavenModel model = items.get(position);

        holder.tvName.setText(model.getName());
        holder.tvAddress.setText(model.getAddress());
        holder.tvDistance.setText("⚡ " + model.getFormattedDistance());
        holder.tvStatus.setText(model.getOpenStatus());
        holder.tvCategoryBadge.setText(model.getCategoryBadge());

        // Category Icon, Tint & Background Glow
        int iconRes = R.drawable.ic_logo_guardian_shield;
        int iconColor = ContextCompat.getColor(context, R.color.colorPinkNeon);
        int boxBgColor = Color.parseColor("#26FF2E93");

        String category = model.getCategory() != null ? model.getCategory().toUpperCase() : "";
        if (SafeHavenModel.CAT_POLICE.equals(category)) {
            iconRes = R.drawable.ic_shield;
            iconColor = Color.parseColor("#3B82F6"); // Bright Blue
            boxBgColor = Color.parseColor("#263B82F6"); // Translucent Blue
        } else if (SafeHavenModel.CAT_HOSPITAL.equals(category)) {
            iconRes = R.drawable.ic_shield_heart_vector;
            iconColor = Color.parseColor("#EF4444"); // Red Cross
            boxBgColor = Color.parseColor("#26EF4444"); // Translucent Red
        } else if (SafeHavenModel.CAT_PHARMACY.equals(category)) {
            iconRes = R.drawable.ic_safe_places_pin;
            iconColor = Color.parseColor("#10B981"); // Emerald
            boxBgColor = Color.parseColor("#2610B981"); // Translucent Emerald
        } else if (SafeHavenModel.CAT_TRANSIT.equals(category)) {
            iconRes = R.drawable.ic_route_safety;
            iconColor = Color.parseColor("#8B5CF6"); // Purple
            boxBgColor = Color.parseColor("#268B5CF6"); // Translucent Purple
        }

        holder.ivCategoryIcon.setImageResource(iconRes);
        ImageViewCompat.setImageTintList(holder.ivCategoryIcon, ColorStateList.valueOf(iconColor));

        GradientDrawable iconBoxDrawable = new GradientDrawable();
        iconBoxDrawable.setShape(GradientDrawable.OVAL);
        iconBoxDrawable.setColor(boxBgColor);
        int strokePx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.2f, context.getResources().getDisplayMetrics());
        iconBoxDrawable.setStroke(strokePx, iconColor);
        holder.flIconBox.setBackground(iconBoxDrawable);

        holder.ivVerifiedBadge.setVisibility(model.isVerified() ? View.VISIBLE : View.GONE);

        // Click Listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCardClicked(model);
        });

        holder.btnCall.setOnClickListener(v -> {
            if (listener != null) listener.onCallClicked(model);
        });

        holder.btnDirections.setOnClickListener(v -> {
            if (listener != null) listener.onDirectionsClicked(model);
        });

        holder.btnSafeWalk.setOnClickListener(v -> {
            if (listener != null) listener.onSafeRouteClicked(model);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        FrameLayout flIconBox;
        ImageView ivCategoryIcon;
        TextView tvName;
        ImageView ivVerifiedBadge;
        TextView tvCategoryBadge;
        TextView tvDistance;
        TextView tvAddress;
        TextView tvStatus;
        MaterialButton btnCall;
        MaterialButton btnDirections;
        MaterialButton btnSafeWalk;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            flIconBox = itemView.findViewById(R.id.fl_icon_box);
            ivCategoryIcon = itemView.findViewById(R.id.iv_category_icon);
            tvName = itemView.findViewById(R.id.tv_haven_name);
            ivVerifiedBadge = itemView.findViewById(R.id.iv_verified_badge);
            tvCategoryBadge = itemView.findViewById(R.id.tv_category_badge);
            tvDistance = itemView.findViewById(R.id.tv_haven_distance);
            tvAddress = itemView.findViewById(R.id.tv_haven_address);
            tvStatus = itemView.findViewById(R.id.tv_haven_status);
            btnCall = itemView.findViewById(R.id.btn_call_haven);
            btnDirections = itemView.findViewById(R.id.btn_directions_haven);
            btnSafeWalk = itemView.findViewById(R.id.btn_safe_walk_haven);
        }
    }
}
