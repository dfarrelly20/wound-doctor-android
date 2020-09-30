package model;

import com.google.firebase.Timestamp;

/**
 * Class represents a Wound object. This is used to add and retrieve Wound documents from the wounds
 * collection of Firestore
 *
 * @author David Farrelly
 */
public class Wound {

    /**
     * The ID of the wound
     */
    private String woundId;

    /**
     * The ID if the patient who registered the wound
     */
    private String userId;

    /**
     * The name of the limb where the wound is located
     */
    private String limbName;

    /**
     * The ID of the bandage that is on the wound. This is got from the QR code on the bandage and
     * can be used to find the wound in the Firestore wounds collection
     */
    private String bandageId;

    /**
     * A Firebase Timestamp representing when the wound was first registered
     */
    private Timestamp date;

    /**
     * The number of hours between each health check for this wound
     */
    private int hoursUntilCheck;

    /**
     * A Firebase Timestamp representing when the bandage on this wound was last changed
     */
    private Timestamp bandageLastChanged;

    /**
     * A Firebase Timestamp representing when the next health check is scheduled for this wound
     */
    private Timestamp nextHealthCheck;

    /**
     * A boolean representing if a large Delta-E has been detected for the wound. Set as true if
     * Delta-E is a large value and false if Delta-E is low
     */
    private boolean woundInDanger;

    public Wound() {
    }

    /**
     * Constructor used to create a Wound with arguments
     *
     * @param woundId            String - the ID of wound
     * @param userId             String - the ID of the patient
     * @param limbName           String - the name of the limb
     * @param bandageId          String - the ID of the bandage
     * @param date               Timestamp - the date the wound was registered
     * @param hoursUntilCheck    int - the number of hours until the next health check for this wound
     * @param bandageLastChanged Timestamp - the date the bandage was last changed on this wound
     * @param nextHealthCheck    Timestamp - the date of the next health check
     * @param woundInDanger      boolean - whether or not a Delta-E large Delta-E is detected
     */
    public Wound(String woundId, String userId, String limbName, String bandageId, Timestamp date,
                 int hoursUntilCheck, Timestamp bandageLastChanged, Timestamp nextHealthCheck,
                 boolean woundInDanger) {
        this.woundId = woundId;
        this.userId = userId;
        this.limbName = limbName;
        this.bandageId = bandageId;
        this.date = date;
        this.hoursUntilCheck = hoursUntilCheck;
        this.bandageLastChanged = bandageLastChanged;
        this.nextHealthCheck = nextHealthCheck;
        this.woundInDanger = woundInDanger;
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

    public Timestamp getBandageLastChanged() {
        return bandageLastChanged;
    }

    public void setBandageLastChanged(Timestamp bandageLastChanged) {
        this.bandageLastChanged = bandageLastChanged;
    }

    public Timestamp getNextHealthCheck() {
        return nextHealthCheck;
    }

    public void setNextHealthCheck(Timestamp nextHealthCheck) {
        this.nextHealthCheck = nextHealthCheck;
    }

    public boolean isWoundInDanger() {
        return woundInDanger;
    }

    public void setWoundInDanger(boolean woundInDanger) {
        this.woundInDanger = woundInDanger;
    }
}
