package model;

import com.google.firebase.Timestamp;

public class Wound {

    private String woundId;
    private String userId;
    private String limbName;
    private String bandageId;
    private Timestamp date;

    public Wound() {
    }

    public Wound(String woundId, String userId, String limbName, String bandageId, Timestamp date) {
        this.woundId = woundId;
        this.userId = userId;
        this.limbName = limbName;
        this.bandageId = bandageId;
        this.date = date;
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

    public String getLimbName() {
        return limbName;
    }

    public void setLimbName(String limbName) {
        this.limbName = limbName;
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
}
