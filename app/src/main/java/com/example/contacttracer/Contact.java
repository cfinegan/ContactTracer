package com.example.contacttracer;

import android.location.Location;

import java.util.Date;

public class Contact {
    public String name;
    public String address;
    public Date accessTime;
    public Location location;
    public Contact(String name, String address, Date accessTime, Location location) {
        this.name = name;
        this.address = address;
        this.accessTime = accessTime;
        this.location = location;
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name == null ? "Unknown Device" : name);
        sb.append("    {");
        sb.append(address);
        sb.append('}');
        return sb.toString();
    }
}
