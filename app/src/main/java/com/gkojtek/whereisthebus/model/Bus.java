package com.gkojtek.whereisthebus.model;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

public class Bus {
    private LatLng currentLatLng;
    private LatLng previousLatLng;
    private String line;
    private String brigade;
    private double previousHeading;
    private boolean isPositionUpdated;
    private double headingMean;
    private double heading;

    public boolean isPositionUpdated() {
        return isPositionUpdated;
    }

    public float getHeading() {
        return (float) heading;
    }

    private double currentHeading;

    public Bus(LatLng currentLatLng, String line, String brigade) {
        this.currentLatLng = currentLatLng;
        previousLatLng = currentLatLng;
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
        double distance = SphericalUtil.computeDistanceBetween(previousLatLng, currentLatLng);
        Log.d(line + " HASH: " + hashCode() + ": distance: ", String.valueOf(distance) + " : " + previousLatLng.latitude + " i " + currentLatLng.latitude);
        isPositionUpdated = true;

        if (previousHeading != 0.0d) {
            currentHeading = SphericalUtil.computeHeading(previousLatLng, currentLatLng);
            headingMean = (currentHeading + previousHeading) / 2;
            previousHeading = currentHeading;
            heading = headingMean;
        } else {
            previousHeading = currentHeading;
            currentHeading = SphericalUtil.computeHeading(previousLatLng, currentLatLng);
            heading = currentHeading;
        }

    }

    @Override
    public int hashCode() {
        int result = line.hashCode();
        result = 31 * result + brigade.hashCode();
        return result;
    }
}
