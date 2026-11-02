package com.android.sheguard.ui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.sheguard.R;
import com.android.sheguard.model.RiskZoneModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RiskZoneAdapter extends RecyclerView.Adapter<RiskZoneAdapter.ViewHolder> {

    public interface OnRiskZoneClickListener {
        void onZoneClicked(RiskZoneModel model);
        void onZoneFocusClicked(RiskZoneModel model);
    }

    private final Context context;
    private final List<RiskZoneModel> items = new ArrayList<>();
    private final OnRiskZoneClickListener listener;

    public RiskZoneAdapter(@NonNull Context context, List<RiskZoneModel> items, OnRiskZoneClickListener listener) {
        this.context = context;
        if (items != null) {
            this.items.addAll(items);
        }
        this.listener = listener;
    }

    public void updateList(List<RiskZoneModel> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_risk_zone, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RiskZoneModel model = items.get(position);

        holder.tvZoneName.setText(model.getName());
        holder.tvRiskScore.setText(String.format(Locale.US, "%.0f%%", model.getRiskScore()));
        holder.tvDistance.setText(model.getFormattedDistance());

        int badgeColor = 0xFF10B981;
        String badgeText = "LOW RISK";
        if (model.isHighRisk()) {
            badgeColor = 0xFFEF4444;
            badgeText = "HIGH RISK";
        } else if (model.isMediumRisk()) {
            badgeColor = 0xFFF59E0B;
            badgeText = "MEDIUM RISK";
        }

        holder.tvRiskBadge.setText(badgeText);
        holder.tvRiskBadge.setTextColor(badgeColor);
        holder.tvRiskScore.setTextColor(badgeColor);

        // Circular background for score badge
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setShape(GradientDrawable.OVAL);
        badgeBg.setColor(Color.argb(35, Color.red(badgeColor), Color.green(badgeColor), Color.blue(badgeColor)));
        badgeBg.setStroke(2, badgeColor);
        holder.flScoreBadge.setBackground(badgeBg);

        // Risk Category Chip background
        GradientDrawable chipBg = new GradientDrawable();
        chipBg.setCornerRadius(12f);
        chipBg.setColor(Color.argb(30, Color.red(badgeColor), Color.green(badgeColor), Color.blue(badgeColor)));
        holder.tvRiskBadge.setBackground(chipBg);

        // Factor summary
        StringBuilder sb = new StringBuilder();
        if (model.getCommonCrimes() != null && !model.getCommonCrimes().isEmpty()) {
            sb.append("🚨 Common: ").append(String.join(", ", model.getCommonCrimes())).append("\n");
        }
        if (model.getRiskFactors() != null && !model.getRiskFactors().isEmpty()) {
            sb.append("⚠️ ").append(model.getRiskFactors().get(0));
        } else {
            sb.append("Historical crime and environmental telemetry verified.");
        }
        holder.tvFactor.setText(sb.toString().trim());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onZoneClicked(model);
        });

        holder.btnFocus.setOnClickListener(v -> {
            if (listener != null) listener.onZoneFocusClicked(model);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        FrameLayout flScoreBadge;
        TextView tvRiskScore;
        TextView tvZoneName;
        TextView tvRiskBadge;
        TextView tvDistance;
        TextView tvFactor;
        ImageView btnFocus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            flScoreBadge = itemView.findViewById(R.id.fl_score_badge);
            tvRiskScore = itemView.findViewById(R.id.tv_item_risk_score);
            tvZoneName = itemView.findViewById(R.id.tv_item_zone_name);
            tvRiskBadge = itemView.findViewById(R.id.tv_item_risk_badge);
            tvDistance = itemView.findViewById(R.id.tv_item_distance);
            tvFactor = itemView.findViewById(R.id.tv_item_factor);
            btnFocus = itemView.findViewById(R.id.btn_focus_zone);
        }
    }
}

