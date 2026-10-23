package com.android.sheguard.ui.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.sheguard.R;
import com.android.sheguard.model.HelplineModel;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HelplineAdapter extends RecyclerView.Adapter<HelplineAdapter.ViewHolder> {

    private static final String[] CATEGORIES = new String[]{
            "National SOS & Police",
            "Women Crisis & Safety",
            "Women Domestic Distress",
            "National Commission (NCW)",
            "Police Control Desk",
            "Emergency Medical & Ambulance",
            "Fire & Disaster Rescue",
            "National Cyber Crime",
            "Childline 24/7 Protection",
            "KIRAN Mental Health Support",
            "RPF Railway Security",
            "Highway Emergency Patrol"
    };

    private static final String[] STATUSES = new String[]{
            "24/7 Toll Free",
            "24/7 Rapid Response",
            "24/7 Distress Desk",
            "24/7 Active Intervention",
            "24/7 Immediate Dispatch",
            "24/7 Emergency Ambulance",
            "24/7 Immediate Dispatch",
            "24/7 Citizen Portal",
            "24/7 Child Protection",
            "24/7 Psychological Care",
            "24/7 Rail Madad",
            "24/7 Highway Patrol"
    };

    private static final int[] ICON_COLORS = new int[]{
            Color.parseColor("#FF2E93"), // Pink
            Color.parseColor("#EF4444"), // Red
            Color.parseColor("#38BDF8"), // Blue
            Color.parseColor("#A78BFA"), // Purple
            Color.parseColor("#34D399"), // Green
            Color.parseColor("#34D399"), // Green
            Color.parseColor("#F59E0B"), // Amber
            Color.parseColor("#EC4899"), // Pink
            Color.parseColor("#8B5CF6"), // Violet
            Color.parseColor("#3B82F6"), // Blue
            Color.parseColor("#10B981"), // Emerald
            Color.parseColor("#F97316")  // Orange
    };

    private final Context context;
    private final ArrayList<HelplineModel> helplines;

    public HelplineAdapter(@NonNull Context context, ArrayList<HelplineModel> helplines) {
        this.helplines = helplines;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.view_helpline_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HelplineModel item = helplines.get(position);

        holder.name.setText(item.getName());
        holder.number.setText(item.getNumber());
        holder.details.setText(item.getDetails());

        String category = position < CATEGORIES.length ? CATEGORIES[position] : "Emergency";
        holder.tvCategory.setText(category);

        String status = position < STATUSES.length ? STATUSES[position] : "24/7 Active";
        holder.tvStatusBadge.setText(status);

        int iconColor = position < ICON_COLORS.length ? ICON_COLORS[position] : Color.parseColor("#10B981");
        holder.ivHelplineIcon.setImageTintList(ColorStateList.valueOf(iconColor));

        final String rawNumber = item.getNumber();
        final String dialable = extractDialableNumber(rawNumber);

        holder.btnCall.setOnClickListener(v -> {
            if (!dialable.isEmpty()) {
                try {
                    Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + dialable));
                    context.startActivity(dialIntent);
                } catch (Exception e) {
                    Toast.makeText(context, "Cannot open phone dialer", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, "No dialable number found", Toast.LENGTH_SHORT).show();
            }
        });

        holder.btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("Helpline Number", rawNumber);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "Helpline number copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String extractDialableNumber(String text) {
        if (text == null || text.isEmpty()) return "";
        Pattern pattern = Pattern.compile("\\+?[0-9]{3,14}");
        Matcher matcher = pattern.matcher(text.replace("-", "").replace(" ", ""));
        if (matcher.find()) {
            return matcher.group();
        }
        return text.split("\n")[0].trim();
    }

    @Override
    public int getItemCount() {
        return helplines.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivHelplineIcon;
        TextView name, tvCategory, tvStatusBadge, number, details;
        MaterialButton btnCall, btnCopy;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivHelplineIcon = itemView.findViewById(R.id.iv_helpline_icon);
            name = itemView.findViewById(R.id.name);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvStatusBadge = itemView.findViewById(R.id.tv_status_badge);
            number = itemView.findViewById(R.id.number);
            details = itemView.findViewById(R.id.details);
            btnCall = itemView.findViewById(R.id.btn_call);
            btnCopy = itemView.findViewById(R.id.btn_copy);
        }
    }
}
