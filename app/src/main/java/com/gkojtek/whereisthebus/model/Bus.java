package com.gkojtek.whereisthebus.model;

import com.google.android.gms.maps.model.LatLng;

public class Bus {
    private LatLng latLng;
    private String line;

    public LatLng getLatLng() {
        return latLng;
    }

    public String getLine() {
        return line;
    }

    public Bus(LatLng latLng, String line) {
        this.latLng = latLng;
        this.line = line;
    }


}
