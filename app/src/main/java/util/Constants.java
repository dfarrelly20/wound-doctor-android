package util;

/**
 * This class contains constant values that are used throughout the software
 *
 * @author David Farrelly
 */
public class Constants {


    // -------- These are the 3 request codes that govern the actions that the user requests the camera for --------
    /**
     * User has requested the camera to take a picture of one of their registered wounds to post colour data
     */
    public static final int REQUEST_CAMERA_ACTION = 1;
    /**
     * User has requested the camera to register a new wound and has to take a picture of their bandage
     */
    public static final int REQUEST_REGISTER_WOUND = 2;
    /**
     * User has requested the camera to change the bandage on one of their wounds by updating the bandage ID
     */
    public static final int REQUEST_UPDATE_BANDAGE = 3;

    /**
     * The factor that represents how much bigger the red boundary box on the patient bandage is
     * compared to the QR code width
     */
    public static final int BANDAGE_BOX_FACTOR = 5;


    // -------- Here the Rgb colour values of the reference colour on the bandage can be hardcoded in --------
    /**
     * The red value of the reference colour
     */
    public static final int referenceRedValue = 255;
    /**
     * The green value of the reference colour
     */
    public static final int referenceGreenValue = 174;
    /**
     * The blue value of the reference colour
     */
    public static final int referenceBlueValue = 201;

}
