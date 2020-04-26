package com.example.contacttracer;

import android.location.Location;

import java.util.Date;

public class Contact {
    private String name;
    private String address;
    private Date accessTime;
    private double latitude;
    private double longitude;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public Date getAccessTime() { return accessTime; }
    public void setAccessTime(Date accessTime) { this.accessTime = accessTime; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public Contact() {}
    public Contact(String name, String address, Date accessTime, double latitude, double longitude) {
        this.name = name;
        this.address = address;
        this.accessTime = accessTime;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name == null ? "Unknown Device" : name);
        sb.append("  {");
        sb.append(address);
        sb.append("}\n");
        sb.append(accessTime.toString());
        return sb.toString();
    }
}
