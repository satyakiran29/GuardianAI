package com.skdev.guardianai.chatbot;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.skdev.guardianai.R;

import java.util.List;

/**
 * Adapter for AI Safety Chatbot messages.
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private final List<ChatMessage> messages;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        if (msg.isUser()) {
            holder.layoutBot.setVisibility(View.GONE);
            holder.layoutUser.setVisibility(View.VISIBLE);
            holder.tvUserText.setText(msg.getMessage());
            holder.tvUserTime.setText(msg.getTimestamp());
        } else {
            holder.layoutUser.setVisibility(View.GONE);
            holder.layoutBot.setVisibility(View.VISIBLE);
            holder.tvBotText.setText(msg.getMessage());
            holder.tvBotTime.setText(msg.getTimestamp());
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        View layoutBot, layoutUser;
        TextView tvBotText, tvBotTime, tvUserText, tvUserTime;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutBot = itemView.findViewById(R.id.layout_bot_message);
            layoutUser = itemView.findViewById(R.id.layout_user_message);
            tvBotText = itemView.findViewById(R.id.tv_bot_text);
            tvBotTime = itemView.findViewById(R.id.tv_bot_time);
            tvUserText = itemView.findViewById(R.id.tv_user_text);
            tvUserTime = itemView.findViewById(R.id.tv_user_time);
        }
    }
}
