package com.android.sheguard.model;

@SuppressWarnings("unused")
public class ContactModel {

    String name, phone;
    String relationship;
    boolean isPrimary;

    public ContactModel(String name, String phone) {
        this(name, phone, "Family", false);
    }

    public ContactModel(String name, String phone, String relationship, boolean isPrimary) {
        this.name = name;
        this.phone = phone;
        this.relationship = relationship != null && !relationship.isEmpty() ? relationship : "Family";
        this.isPrimary = isPrimary;
    }

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone != null ? phone : "";
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRelationship() {
        return relationship != null && !relationship.isEmpty() ? relationship : "Family";
    }

    public void setRelationship(String relationship) {
        this.relationship = relationship;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }
}
