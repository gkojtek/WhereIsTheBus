package com.gkojtek.whereisthebus.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Result {

    @SerializedName("Lat")
    @Expose
    private Double lat;
    @SerializedName("Lon")
    @Expose
    private Double lon;
    @SerializedName("Time")
    @Expose
    private String time;
    @SerializedName("Lines")
    @Expose
    private String lines;
    @SerializedName("Brigade")
    @Expose
    private String brigade;

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Result withLat(Double lat) {
        this.lat = lat;
        return this;
    }

    public Double getLon() {
        return lon;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public Result withLon(Double lon) {
        this.lon = lon;
        return this;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public Result withTime(String time) {
        this.time = time;
        return this;
    }

    public String getLines() {
        return lines;
    }

    public void setLines(String lines) {
        this.lines = lines;
    }

    public Result withLines(String lines) {
        this.lines = lines;
        return this;
    }

    public String getBrigade() {
        return brigade;
    }

    public void setBrigade(String brigade) {
        this.brigade = brigade;
    }

    public Result withBrigade(String brigade) {
        this.brigade = brigade;
        return this;
    }

}
