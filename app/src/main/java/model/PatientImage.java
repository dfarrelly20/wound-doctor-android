package model;

import com.google.firebase.Timestamp;

/**
 * Class represents a PatientImage object. This is used to create the wound feedback data to send to the
 * feedback collection in Firestore. The feedback is gathered when a users wound image is processed for a
 * health check.
 *
 * @author David Farrelly
 */
public class PatientImage {

    /**
     * The ID of the patient who took the image
     */
    private String userId;

    /**
     * The ID of the wound undergoing a health check
     */
    private String woundId;

    /**
     * The Rgb RED value measured after processing the wound image
     */
    private int red;

    /**
     * The Rgb GREEN value measured after processing the wound image
     */
    private int green;

    /**
     * The Rgb BLUE value measured after processing the wound image
     */
    private int blue;

    /**
     * A reference to url of the wound image in Firebase Storage
     */
    private String imageUrl;

    /**
     * The ID of the bandage that is on the patient's wound.
     * The ID is retrieved from the QR code on the bandage and can be used as a reference of which
     * wound was processed during the health check
     */
    private String bandageId;

    /**
     * A Firebase Timestamp representing the date when the wound was processed for feedback
     */
    private Timestamp date;

    /**
     * The current Delta-E value of the wound. Calculated by comparing the previous Rgb colour data
     * to the latest Rgb colour data from the latest health check. The colour change represents
     * how much the condition of the wound has changed since it was last processed
     */
    private double woundChangeSinceLast;

    /**
     * Empty constructor is needed for creating document reference in Firestore
     */
    public PatientImage() {
    }

    /**
     * Constructor used to create a PatientImage with arguments
     *
     * @param userId               String - The ID of the patient
     * @param woundId              String - The ID of the wound
     * @param red                  int - the Rgb RED component
     * @param green                int - the Rgb GREEN component
     * @param blue                 int - the Rgb BLUE component
     * @param imageUrl             String - url of the wound image in Firebase Storage
     * @param bandageId            String - the ID of the bandage
     * @param date                 Timestamp - the date of the health check
     * @param woundChangeSinceLast double - the current Delta-E value of the wound
     */
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
