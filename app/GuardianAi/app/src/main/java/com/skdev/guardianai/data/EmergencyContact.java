package com.skdev.guardianai.data;

import java.io.Serializable;

/**
 * Model representing an emergency contact.
 */
public class EmergencyContact implements Serializable {
    private String id;
    private String name;
    private String phoneNumber;
    private String relationship;
    private boolean isPrimary;

    public EmergencyContact(String id, String name, String phoneNumber, String relationship, boolean isPrimary) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.relationship = relationship;
        this.isPrimary = isPrimary;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getRelationship() { return relationship; }
    public boolean isPrimary() { return isPrimary; }

    public void setName(String name) { this.name = name; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setRelationship(String relationship) { this.relationship = relationship; }
    public void setPrimary(boolean primary) { isPrimary = primary; }
}
