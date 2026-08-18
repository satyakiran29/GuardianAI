package com.skdev.guardianai.sos;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.skdev.guardianai.R;
import com.skdev.guardianai.data.EmergencyContact;
import com.skdev.guardianai.data.EmergencyContactManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Full CRUD Activity for managing unlimited emergency contacts.
 */
public class EmergencyContactsActivity extends AppCompatActivity implements EmergencyContactAdapter.ContactActionListener {

    private RecyclerView rvContacts;
    private EmergencyContactAdapter adapter;
    private final List<EmergencyContact> contactList = new ArrayList<>();
    private EmergencyContactManager contactManager;

    @Override
    protected void attachBaseContext(android.content.Context newBase) {
        super.attachBaseContext(com.skdev.guardianai.utils.LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        contactManager = EmergencyContactManager.getInstance(this);

        initViews();
        loadContacts();
    }

    private void initViews() {
        ImageButton btnBack = findViewById(R.id.btn_back_contacts);
        btnBack.setOnClickListener(v -> finish());

        Button btnAdd = findViewById(R.id.btn_add_new_contact);
        btnAdd.setOnClickListener(v -> showAddContactDialog());

        rvContacts = findViewById(R.id.rv_manage_contacts);
        rvContacts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EmergencyContactAdapter(contactList, this);
        rvContacts.setAdapter(adapter);
    }

    private void loadContacts() {
        contactList.clear();
        contactList.addAll(contactManager.getContacts());
        adapter.notifyDataSetChanged();
    }

    private void showAddContactDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_contact, null, false);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        EditText etName = dialogView.findViewById(R.id.et_dialog_name);
        EditText etPhone = dialogView.findViewById(R.id.et_dialog_phone);
        EditText etRel = dialogView.findViewById(R.id.et_dialog_relation);
        CheckBox cbPrimary = dialogView.findViewById(R.id.cb_dialog_primary);
        Button btnSave = dialogView.findViewById(R.id.btn_dialog_save);
        Button btnCancel = dialogView.findViewById(R.id.btn_dialog_cancel);

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String rel = etRel.getText().toString().trim();
            boolean isPrimary = cbPrimary.isChecked();

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Please enter contact name and phone number", Toast.LENGTH_SHORT).show();
                return;
            }

            if (rel.isEmpty()) {
                rel = "Emergency Contact";
            }

            contactManager.addContact(name, phone, rel, isPrimary);
            loadContacts();
            Toast.makeText(this, "Emergency Contact added successfully!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    public void onCall(EmergencyContact contact) {
        if (contact.getPhoneNumber() != null) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + contact.getPhoneNumber()));
            startActivity(intent);
        }
    }

    @Override
    public void onDelete(EmergencyContact contact) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Contact")
                .setMessage("Are you sure you want to remove " + contact.getName() + " from emergency contacts?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    contactManager.deleteContact(contact.getId());
                    loadContacts();
                    Toast.makeText(this, "Contact removed", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
