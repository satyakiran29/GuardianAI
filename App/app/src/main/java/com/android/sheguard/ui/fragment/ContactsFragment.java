package com.android.sheguard.ui.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.android.sheguard.R;
import com.android.sheguard.SheGuard;
import com.android.sheguard.common.Constants;
import com.android.sheguard.config.Prefs;
import com.android.sheguard.databinding.FragmentContactsBinding;
import com.android.sheguard.model.ContactModel;
import com.android.sheguard.util.SmsHelper;
import com.android.sheguard.ui.adapter.ContactsAdapter;
import com.android.sheguard.ui.adapter.NewContactAdapter;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@SuppressLint({"StaticFieldLeak"})
@SuppressWarnings("FieldCanBeLocal")
public class ContactsFragment extends Fragment {

    public static ArrayList<ContactModel> contacts;
    public static View tvEmptyList;
    public static ContactsAdapter adapter;
    private FragmentContactsBinding binding;

    private final ActivityResultLauncher<Intent> contactPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri contactUri = result.getData().getData();
                    extractAndAddContact(contactUri);
                }
            });

    @SuppressLint("NotifyDataSetChanged")
    public static void removeContact(Context context, int idx) {
        View tvEmptyList = ((AppCompatActivity) context).findViewById(R.id.tv_empty_list);
        new MaterialAlertDialogBuilder(context, R.style.MaterialComponents_MaterialAlertDialog)
                .setTitle("Remove Contact")
                .setMessage(context.getString(R.string.remove_contact_confirmation))
                .setCancelable(false)
                .setPositiveButton(context.getString(R.string.yes), (dialog, which) -> {
                    if (contacts.size() == 0 || idx >= contacts.size()) {
                        return;
                    }

                    contacts.remove(idx);
                    adapter.notifyDataSetChanged();

                    Gson gson = SheGuard.GSON;
                    String jsonContacts = gson.toJson(contacts);
                    Prefs.putString(Constants.CONTACTS_LIST, jsonContacts);

                    tvEmptyList.setVisibility(contacts.size() == 0 ? View.VISIBLE : View.GONE);
                    Snackbar.make(((AppCompatActivity) context).findViewById(R.id.fragmentContainerView), context.getString(R.string.contact_removed_successfully), Snackbar.LENGTH_SHORT).show();
                })
                .setNegativeButton(context.getString(R.string.no), (dialog, which) -> dialog.dismiss())
                .show();
    }

    public static boolean isPhoneNumberExists(String newNumber) {
        for (ContactModel contact : contacts) {
            if (contact.getPhone().equals(newNumber)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentContactsBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        ((AppCompatActivity) requireActivity()).setSupportActionBar(binding.header.toolbar);
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            binding.header.collapsingToolbar.setTitle("Trusted Safety Circle");
            binding.header.collapsingToolbar.setSubtitle("Emergency Alert Network");
        }

        tvEmptyList = view.findViewById(R.id.tv_empty_list);

        contacts = new ArrayList<>();
        Gson gson = SheGuard.GSON;
        String jsonContacts = Prefs.getString(Constants.CONTACTS_LIST, "");
        if (!jsonContacts.isEmpty()) {
            Type type = new TypeToken<List<ContactModel>>() {}.getType();
            try {
                List<ContactModel> loaded = gson.fromJson(jsonContacts, type);
                if (loaded != null) {
                    for (ContactModel c : loaded) {
                        if (c != null && !SmsHelper.isEmergencyHelplineNumber(c.getPhone())) {
                            contacts.add(c);
                        }
                    }
                }
            } catch (Exception ignored) {}
            // Save sanitized list without 112
            Prefs.putString(Constants.CONTACTS_LIST, gson.toJson(contacts));
        }

        if (contacts.isEmpty() && Prefs.getBoolean(Constants.IS_DEMO_MODE, false)) {
            contacts.add(new ContactModel("Mom", "+15552345678", "Family", true));
            contacts.add(new ContactModel("Alex", "+15558765432", "Roommate", false));
            contacts.add(new ContactModel("Campus Security", "+15559990000", "Security", false));
            Prefs.putString(Constants.CONTACTS_LIST, gson.toJson(contacts));
        }

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ContactsAdapter(requireContext(), contacts);
        ConcatAdapter concatAdapter = new ConcatAdapter(
                new NewContactAdapter(requireContext(), view, this::pickContactFromPhonebook),
                adapter
        );
        binding.recyclerView.setAdapter(concatAdapter);
        binding.recyclerView.setHasFixedSize(false);

        binding.tvEmptyList.setVisibility(contacts.size() == 0 ? View.VISIBLE : View.GONE);

        return view;
    }

    private void pickContactFromPhonebook() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        try {
            contactPickerLauncher.launch(pickIntent);
        } catch (Exception e) {
            Snackbar.make(binding.getRoot(), "Could not open contacts: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
        }
    }

    private void extractAndAddContact(Uri contactUri) {
        if (getContext() == null || contactUri == null) return;
        String[] projection = new String[]{
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };

        try (Cursor cursor = requireContext().getContentResolver().query(contactUri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                String name = nameIdx != -1 ? cursor.getString(nameIdx) : "Trusted Contact";
                String number = numIdx != -1 ? cursor.getString(numIdx) : "";

                if (number != null) {
                    number = number.replaceAll("[\\s\\-\\(\\)]", "");
                }

                if (number == null || number.isEmpty()) {
                    Snackbar.make(binding.getRoot(), "Selected contact has no valid phone number", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                if (isPhoneNumberExists(number)) {
                    Snackbar.make(binding.getRoot(), "This contact is already in your circle", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                if (contacts.size() >= 10) {
                    Snackbar.make(binding.getRoot(), "Safety circle full (maximum 10 contacts)", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                boolean isPrimary = contacts.isEmpty();
                contacts.add(new ContactModel(name, number, "Family", isPrimary));
                adapter.notifyDataSetChanged();

                Gson gson = SheGuard.GSON;
                Prefs.putString(Constants.CONTACTS_LIST, gson.toJson(contacts));

                binding.tvEmptyList.setVisibility(contacts.isEmpty() ? View.VISIBLE : View.GONE);
                Snackbar.make(binding.getRoot(), "Added " + name + " to your Safety Circle", Snackbar.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Snackbar.make(binding.getRoot(), "Error reading contact: " + e.getMessage(), Snackbar.LENGTH_SHORT).show();
        }
    }
}