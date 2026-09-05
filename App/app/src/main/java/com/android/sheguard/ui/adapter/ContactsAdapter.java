package com.android.sheguard.ui.adapter;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
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
import com.android.sheguard.model.ContactModel;
import com.android.sheguard.ui.fragment.ContactsFragment;

import java.util.ArrayList;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolder> {

    Context context;
    ArrayList<ContactModel> contacts;

    public ContactsAdapter(@NonNull Context context, ArrayList<ContactModel> contacts) {
        this.contacts = contacts;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.view_contacts_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ContactModel c = contacts.get(position);
        String name = c.getName();
        String initial = !name.isEmpty() ? name.substring(0, 1).toUpperCase() : "?";
        holder.initials.setText(initial);
        holder.contact.setText(name);
        holder.number.setText(c.getPhone());
        holder.tvRelationship.setText(c.getRelationship());

        if (c.isPrimary() || position == 0) {
            holder.tvPrimaryBadge.setVisibility(View.VISIBLE);
        } else {
            holder.tvPrimaryBadge.setVisibility(View.GONE);
        }

        if (holder.btnCall != null) {
            holder.btnCall.setOnClickListener(v -> {
                Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + c.getPhone()));
                context.startActivity(dialIntent);
            });
        }

        holder.copy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Activity.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Contact", c.getName() + ": " + c.getPhone());
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        holder.delete.setOnClickListener(v -> ContactsFragment.removeContact(context, position));
        holder.itemView.setOnClickListener(v -> context.startActivity(new Intent(Intent.ACTION_DIAL).setData(Uri.parse("tel:" + c.getPhone()))));
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView initials, contact, number, tvRelationship, tvPrimaryBadge;
        View copy, delete, btnCall;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            initials = itemView.findViewById(R.id.initials);
            contact = itemView.findViewById(R.id.name);
            number = itemView.findViewById(R.id.number);
            tvRelationship = itemView.findViewById(R.id.tv_relationship);
            tvPrimaryBadge = itemView.findViewById(R.id.tv_primary_badge);
            btnCall = itemView.findViewById(R.id.btn_call_contact);
            copy = itemView.findViewById(R.id.copy);
            delete = itemView.findViewById(R.id.delete);
        }
    }
}
