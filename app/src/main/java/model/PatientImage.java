package model;

import com.google.firebase.Timestamp;

public class PatientImage {

    private String userId;
    private String woundId;
    private int red;
    private int green;
    private int blue;
    private String imageUrl;
    private String bandageId;
    private Timestamp date;
    private double woundChangeSinceLast;

    public PatientImage(){}

    public PatientImage(String userId, String woundId, int red, int green, int blue, String imageUrl,
                        String bandageId, Timestamp date, double woundChangeSinceLast) {
        this.userId = userId;
        this.woundId = woundId;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.imageUrl = imageUrl;
        this.bandageId = bandageId;
        this.date = date;
        this.woundChangeSinceLast = woundChangeSinceLast;
    }

    public String getWoundId() {
        return woundId;
    }

    public void setWoundId(String woundId) {
        this.woundId = woundId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getRed() {
        return red;
    }

    public void setRed(int red) {
        this.red = red;
    }

    public int getGreen() {
        return green;
    }

    public void setGreen(int green) {
        this.green = green;
    }

    public int getBlue() {
        return blue;
    }

    public void setBlue(int blue) {
        this.blue = blue;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getBandageId() {
        return bandageId;
    }

    public void setBandageId(String bandageId) {
        this.bandageId = bandageId;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    public double getWoundChangeSinceLast() {
        return woundChangeSinceLast;
    }

    public void setWoundChangeSinceLast(double woundChangeSinceLast) {
        this.woundChangeSinceLast = woundChangeSinceLast;
    }
}
