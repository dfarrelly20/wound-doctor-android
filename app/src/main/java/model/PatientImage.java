package model;

import android.graphics.Bitmap;
import android.graphics.Point;

import com.google.firebase.Timestamp;

public class PatientImage {

    private String userId;
    private int red;
    private int green;
    private int blue;
    private String imageUrl;
    private String qrId;
    private Timestamp date;

    public PatientImage(){}

    public PatientImage(String userId, int red, int green, int blue, String imageUrl, String qrId, Timestamp date) {
        this.userId = userId;
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.imageUrl = imageUrl;
        this.qrId = qrId;
        this.date = date;
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

    public String getQrId() {
        return qrId;
    }

    public void setQrId(String qrId) {
        this.qrId = qrId;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }
}
