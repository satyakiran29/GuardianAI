package com.skdev.guardianai.sos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.skdev.guardianai.R;
import com.skdev.guardianai.data.EmergencyContact;

import java.util.List;

/**
 * Adapter for managing unlimited emergency contacts.
 */
public class EmergencyContactAdapter extends RecyclerView.Adapter<EmergencyContactAdapter.ContactViewHolder> {

    public interface ContactActionListener {
        void onCall(EmergencyContact contact);
        void onDelete(EmergencyContact contact);
    }

    private final List<EmergencyContact> contacts;
    private final ContactActionListener listener;

    public EmergencyContactAdapter(List<EmergencyContact> contacts, ContactActionListener listener) {
        this.contacts = contacts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_emergency_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        EmergencyContact contact = contacts.get(position);
        holder.tvName.setText(contact.getName());
        holder.tvMeta.setText(contact.getPhoneNumber() + " • " + contact.getRelationship());
        holder.tvPrimary.setVisibility(contact.isPrimary() ? View.VISIBLE : View.GONE);

        holder.btnCall.setOnClickListener(v -> {
            if (listener != null) listener.onCall(contact);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(contact);
        });
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvMeta, tvPrimary;
        ImageButton btnCall, btnDelete;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_contact_name);
            tvMeta = itemView.findViewById(R.id.tv_contact_meta);
            tvPrimary = itemView.findViewById(R.id.tv_primary_badge);
            btnCall = itemView.findViewById(R.id.btn_call_contact);
            btnDelete = itemView.findViewById(R.id.btn_delete_contact);
        }
    }
}
