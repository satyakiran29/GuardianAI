package com.android.sheguard.model;

import java.io.Serializable;
import java.util.Locale;

public class SafeHavenModel implements Serializable {

    public static final String CAT_ALL = "ALL";
    public static final String CAT_POLICE = "POLICE";
    public static final String CAT_HOSPITAL = "HOSPITAL";
    public static final String CAT_PHARMACY = "PHARMACY";
    public static final String CAT_TRANSIT = "TRANSIT";
    public static final String CAT_SAFE_HAVEN = "SAFE_HAVEN";

    private String id;
    private String name;
    private String category;
    private String categoryBadge;
    private String categoryColor;
    private String address;
    private String phone;
    private double latitude;
    private double longitude;
    private double distanceKm;
    private String openStatus;
    private boolean isVerified;
    private String description;

    public SafeHavenModel() {
    }

    public SafeHavenModel(String id, String name, String category, String categoryBadge,
                          String categoryColor, String address, String phone,
                          double latitude, double longitude, double distanceKm,
                          String openStatus, boolean isVerified, String description) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.categoryBadge = categoryBadge;
        this.categoryColor = categoryColor;
        this.address = address;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distanceKm = distanceKm;
        this.openStatus = openStatus;
        this.isVerified = isVerified;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategoryBadge() {
        return categoryBadge;
    }

    public void setCategoryBadge(String categoryBadge) {
        this.categoryBadge = categoryBadge;
    }

    public String getCategoryColor() {
        return categoryColor;
    }

    public void setCategoryColor(String categoryColor) {
        this.categoryColor = categoryColor;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public String getOpenStatus() {
        return openStatus;
    }

    public void setOpenStatus(String openStatus) {
        this.openStatus = openStatus;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFormattedDistance() {
        if (distanceKm < 1.0) {
            int meters = (int) Math.round(distanceKm * 1000);
            return meters + " m away";
        } else {
            return String.format(Locale.US, "%.1f km away", distanceKm);
        }
    }
}
