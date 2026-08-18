package com.skdev.guardianai.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Manages storage and operations on unlimited emergency contacts using local persistence.
 */
public class EmergencyContactManager {

    private static final String PREF_NAME = "guardian_emergency_contacts_pref";
    private static final String KEY_CONTACTS = "contacts_list_json";

    private static EmergencyContactManager instance;
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();
    private final List<EmergencyContact> contacts = new ArrayList<>();

    private EmergencyContactManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadContacts();
        if (contacts.isEmpty()) {
            initDefaultContacts();
        }
    }

    public static synchronized EmergencyContactManager getInstance(Context context) {
        if (instance == null) {
            instance = new EmergencyContactManager(context);
        }
        return instance;
    }

    private void loadContacts() {
        String json = prefs.getString(KEY_CONTACTS, null);
        if (json != null) {
            try {
                Type type = new TypeToken<List<EmergencyContact>>() {}.getType();
                List<EmergencyContact> loaded = gson.fromJson(json, type);
                if (loaded != null) {
                    contacts.clear();
                    contacts.addAll(loaded);
                }
            } catch (Exception ignored) {}
        }
    }

    private void saveContacts() {
        String json = gson.toJson(contacts);
        prefs.edit().putString(KEY_CONTACTS, json).apply();
    }

    private void initDefaultContacts() {
        contacts.add(new EmergencyContact(UUID.randomUUID().toString(), "National Emergency (Police)", "112", "Emergency Police", true));
        contacts.add(new EmergencyContact(UUID.randomUUID().toString(), "Women Helpline (India)", "1091", "24/7 Helpline", false));
        contacts.add(new EmergencyContact(UUID.randomUUID().toString(), "Primary Guardian / Family", "9876543210", "Guardian", false));
        saveContacts();
    }

    public List<EmergencyContact> getContacts() {
        return new ArrayList<>(contacts);
    }

    public void addContact(String name, String phone, String relationship, boolean isPrimary) {
        if (isPrimary) {
            for (EmergencyContact c : contacts) {
                c.setPrimary(false);
            }
        }
        EmergencyContact newContact = new EmergencyContact(UUID.randomUUID().toString(), name, phone, relationship, isPrimary);
        contacts.add(0, newContact);
        saveContacts();
    }

    public void updateContact(EmergencyContact contact) {
        for (int i = 0; i < contacts.size(); i++) {
            if (contacts.get(i).getId().equals(contact.getId())) {
                contacts.set(i, contact);
                break;
            }
        }
        saveContacts();
    }

    public void deleteContact(String id) {
        for (int i = 0; i < contacts.size(); i++) {
            if (contacts.get(i).getId().equals(id)) {
                contacts.remove(i);
                break;
            }
        }
        saveContacts();
    }

    public EmergencyContact getPrimaryContact() {
        for (EmergencyContact c : contacts) {
            if (c.isPrimary()) return c;
        }
        return contacts.isEmpty() ? null : contacts.get(0);
    }
}
