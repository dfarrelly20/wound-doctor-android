package model;

import com.google.firebase.Timestamp;

public class Wound {

    /**
     * The id of the wound
     */
    private String woundId;

    /**
     * The id if the user who registered the wound
     */
    private String userId;

    /**
     * The name of the limb where the wound is located
     */
    private String limbName;

    /**
     * The id of the bandage that is on the wound. THis is got from the unique QR code on the bandage
     */
    private String bandageId;

    /**
     * The date on which the wound was first registered by the user
     */
    private Timestamp date;

    /**
     * The date that the wound was last checked
     */
    private Timestamp lastChecked;

    /**
     * The number of hours between each health check for this wound
     */
    private int hoursUntilCheck;

    public Wound() {
    }

    public Wound(String woundId, String userId, String limbName, String bandageId, Timestamp date, int hoursUntilCheck) {
        this.woundId = woundId;
        this.userId = userId;
        this.limbName = limbName;
        this.bandageId = bandageId;
        this.date = date;
        this.hoursUntilCheck = hoursUntilCheck;
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

    public int getHoursUntilCheck() {
        return hoursUntilCheck;
    }

    public void setHoursUntilCheck(int hoursUntilCheck) {
        this.hoursUntilCheck = hoursUntilCheck;
    }
}
