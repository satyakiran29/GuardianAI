package com.android.sheguard.ui.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.sheguard.R;

import java.util.ArrayList;
import java.util.Locale;

public class SafetyTipsAdapter extends RecyclerView.Adapter<SafetyTipsAdapter.ViewHolder> {

    private static final String[] TIP_CATEGORIES = new String[]{
            "Situational Awareness",
            "Shopping & Crowds",
            "Situational Awareness",
            "Personal Belongings",
            "Vehicle & Valuables",
            "Night & Parking",
            "Vehicle Safety",
            "Night Transit",
            "Privacy & Identity",
            "Hotel & Lodging",
            "Hotel & Lodging",
            "Hotel & Lodging",
            "Hotel & Lodging",
            "Street Directions",
            "Party & Drinks",
            "Safe Commute",
            "Party & Drinks",
            "Buddy System",
            "Digital & Social",
            "Home Security",
            "Self Defense"
    };

    private final Context context;
    private final ArrayList<String> safetyTips;

    public SafetyTipsAdapter(@NonNull Context context, ArrayList<String> safetyTips) {
        this.safetyTips = safetyTips;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.view_safety_tips_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String tipText = safetyTips.get(position);
        holder.tips.setText(tipText);

        holder.tvTipIndex.setText(String.format(Locale.getDefault(), "%02d", position + 1));

        String category = position < TIP_CATEGORIES.length ? TIP_CATEGORIES[position] : "Safety Practice";
        holder.tvTipCategory.setText(category);

        View.OnClickListener copyAction = v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("Safety Tip", tipText);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "Tip copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        };

        holder.btnCopyTip.setOnClickListener(copyAction);
        holder.itemView.setOnClickListener(copyAction);
    }

    @Override
    public int getItemCount() {
        return safetyTips.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvTipIndex, tvTipCategory, tips;
        ImageButton btnCopyTip;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTipIndex = itemView.findViewById(R.id.tv_tip_index);
            tvTipCategory = itemView.findViewById(R.id.tv_tip_category);
            tips = itemView.findViewById(R.id.tips);
            btnCopyTip = itemView.findViewById(R.id.btn_copy_tip);
        }
    }
}
