package com.gkojtek.whereisthebus.model;

import com.google.android.gms.maps.model.LatLng;

public class Bus {
    private LatLng currentLatLng;
    private LatLng previousLatLng;
    private String line;
    private String brigade;

    public Bus(LatLng currentLatLng, String line, String brigade) {
        this.currentLatLng = currentLatLng;
        this.line = line;
        this.brigade = brigade;
    }

    public LatLng getCurrentLatLng() {
        return currentLatLng;
    }

    public String getLine() {
        return line;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Bus bus = (Bus) o;

        if (!line.equals(bus.line)) return false;
        return brigade.equals(bus.brigade);

    }

    public void updatePosition(LatLng latLng) {
        previousLatLng = currentLatLng;
        currentLatLng = latLng;
    }


    @Override
    public int hashCode() {
        int result = line.hashCode();
        result = 31 * result + brigade.hashCode();
        return result;
    }
}
