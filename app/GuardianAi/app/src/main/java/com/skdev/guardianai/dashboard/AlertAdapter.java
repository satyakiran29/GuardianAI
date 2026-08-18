package com.skdev.guardianai.dashboard;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.skdev.guardianai.R;
import com.skdev.guardianai.data.IncidentAlert;
import com.skdev.guardianai.data.SafetyModelEngine;

import java.util.List;

/**
 * Adapter for real-time hazard alerts and live advisory feeds.
 */
public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.AlertViewHolder> {

    private final List<IncidentAlert> alerts;

    public AlertAdapter(List<IncidentAlert> alerts) {
        this.alerts = alerts;
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_incident_alert, parent, false);
        return new AlertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
        IncidentAlert alert = alerts.get(position);
        holder.tvTitle.setText(alert.getTitle());
        holder.tvLocationTime.setText(alert.getLocation() + " • " + alert.getTimeAgo());
        holder.tvAdvisory.setText(alert.getAdvisory());

        if (alert.getSeverity() == SafetyModelEngine.RiskLevel.LOW) {
            holder.tvBadge.setText("SAFE ZONE");
            holder.tvBadge.setTextColor(Color.parseColor("#10B981"));
            holder.tvBadge.setBackgroundResource(R.drawable.badge_safe);
            holder.ivIcon.setImageResource(R.drawable.ic_shield_logo);
        } else if (alert.getSeverity() == SafetyModelEngine.RiskLevel.MEDIUM) {
            holder.tvBadge.setText("MODERATE");
            holder.tvBadge.setTextColor(Color.parseColor("#F59E0B"));
            holder.tvBadge.setBackgroundResource(R.drawable.badge_moderate);
            holder.ivIcon.setImageResource(R.drawable.ic_crime);
        } else if (alert.getSeverity() == SafetyModelEngine.RiskLevel.HIGH) {
            holder.tvBadge.setText("HIGH RISK");
            holder.tvBadge.setTextColor(Color.parseColor("#F97316"));
            holder.tvBadge.setBackgroundResource(R.drawable.badge_high);
            holder.ivIcon.setImageResource(R.drawable.ic_crime);
        } else {
            holder.tvBadge.setText("CRITICAL");
            holder.tvBadge.setTextColor(Color.parseColor("#EF4444"));
            holder.tvBadge.setBackgroundResource(R.drawable.badge_critical);
            holder.ivIcon.setImageResource(R.drawable.ic_crime);
        }
    }

    @Override
    public int getItemCount() {
        return alerts.size();
    }

    static class AlertViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvTitle, tvLocationTime, tvBadge, tvAdvisory;

        public AlertViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_alert_icon);
            tvTitle = itemView.findViewById(R.id.tv_alert_title);
            tvLocationTime = itemView.findViewById(R.id.tv_alert_location_time);
            tvBadge = itemView.findViewById(R.id.tv_alert_badge);
            tvAdvisory = itemView.findViewById(R.id.tv_alert_advisory);
        }
    }
}
