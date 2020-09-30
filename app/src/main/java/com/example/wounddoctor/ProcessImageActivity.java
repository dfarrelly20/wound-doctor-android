package com.example.wounddoctor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.media.ExifInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import model.Wound;
import util.Constants;
import util.PatientManager;

/**
 * This class processes an image taken using the camera feature of the application.
 * <p>
 * Processing an image taken by the user covers 3 use cases tracked by a request code passed in
 * from the class that calls ProcessImageActivity. These are:
 * <p>
 * Use case 1 - Request code: REQUEST_CAMERA_ACTION - The user has taken an image of one of their
 * wounds for health check. This class is tasked with gathering Rgb colour data from the image and
 * sending this data to PatientFeedbackActivity
 * <p>
 * Use case 2 - Request code: REQUEST_REGISTER_WOUND - The user has taken an image of their wound
 * in order to register it as a new wound on the system. This class is tasked with adding the new
 * wound to Firestore and sending the user back to CameraActivity for an initial health check
 * <p>
 * Use case 3 - Request code: REQUEST_UPDATE_BANDAGE - The user has changed the bandage on one of
 * wounds and needs to update the wound record with the new bandage ID. This class is tasked with
 * getting the new bandage ID and updating the Firestore wound document
 *
 * @author David Farrelly
 */
public class ProcessImageActivity extends AppCompatActivity {

    /**
     * Debug tag for this activity
     */
    private static final String TAG = "ProcessTestActivity";

//    private FirebaseAuth firebaseAuth;
//    private FirebaseAuth.AuthStateListener authStateListener;
//    private FirebaseUser firebaseUser;

    /**
     * Create a connection to Firebase Firestore
     */
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * A reference to the wounds collection in Firestore
     */
    private CollectionReference collectionReference = db.collection("wounds");

     private TextView redTextView;
     private TextView greenTextView;
     private TextView blueTextView;
    // private TextView textView;

    /**
     * The LinearLayout that is displayed when this activity starts. Has a ProgressBar with
     * some loading text
     */
    private LinearLayout progressLayout;

    /**
     * LinearLayout that is displayed to the user who has successfully registered a wound
     */
    private LinearLayout registerSuccessLayout;

    /**
     * The Button element that is displayed on registerSuccessLayout LinearLayout. When clicked
     * will bring the user to CameraActivity with the REQUEST_CAMERA_ACTION
     */
    private Button initialCheckButton;

    private ImageView imageView;

    /**
     * The absolute file path on device storage of the image that was taken
     */
    private String imageFromCameraResult;

    /**
     * A Bitmap used to hold the image passed in
     */
    private Bitmap bitmap;

    /**
     * A Point array holding the coordinates of the QR code corners
     */
    private Point[] qrCorners;

    /**
     * The width of the red boundary box on the Bitmap
     */
    private int boxWidth;

    private int boxHeight;

    /**
     * The X coordinate of the center coloured circle on the Bitmap
     */
    private int colourXPosition;

    /**
     * The Y coordinate of the center coloured circle on the Bitmap
     */
    private int colourYPosition;

    /**
     * The X coordinate of the reference colour square on the Bitmap
     */
    private int referenceXPosition;

    /**
     * The Y coordinate of the reference colour square on the Bitmap
     */
    private int referenceYPosition;

    /*
    The following code configures the Firebase ML kit BarcodeDetector which is used for detecting
    QR codes
     */
    /**
     * An instance of FirebaseVisionBarcodeDetectorOptions. This is used to set the required
     * barcode format to FORMAT_QR_CODE, to make the FirebaseVisionBarcodeDetector more efficient
     */
    FirebaseVisionBarcodeDetectorOptions options = new FirebaseVisionBarcodeDetectorOptions
            .Builder()
            .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_QR_CODE)
            .build();

    /**
     * Create an instance of the FirebaseVisionBarcodeDetector and pass in the
     * FirebaseVisionBarcodeDetectorOptions set up
     */
    FirebaseVisionBarcodeDetector detector = FirebaseVision
            .getInstance()
            .getVisionBarcodeDetector(options);

    /**
     * A reference to the FirebaseVisionImage which the FirebaseVisionBarcodeDetector scans for
     * QR codes. The FirebaseVisionImage is created from the Bitmap holding the image
     */
    FirebaseVisionImage firebaseVisionImage;

    /**
     * The rotation required to orientate the bitmap holding the image for processing. The bitmap
     * must by rotated so that the QR code on the image is parallel to the device screen
     */
    double rotationRequired;

    /**
     * The ID of the bandage that was photographed by the user. The ID is retrieved from the
     * text value stored in the QR code of the bandage
     */
    private String bandageId;

    /**
     * The name of the limb where the patients wound is located. This is passed in from
     * LimbListActivity during the Register Wound use case
     */
    private String limbName;

    /**
     * The ID of the wound that the patient is changing a bandage on. This is passed in from
     * MyWoundsActivity after the user has clicked 'Change Bandage' on one of their wounds
     */
    private String woundId;

    /**
     * The camera action requested by the user.
     * Can have 1 of 3 values:
     * -- REQUEST_CAMERA_ACTION -- the user requests a health check
     * -- REQUEST_REGISTER_WOUND -- the user requests a wound registration
     * -- REQUEST_UPDATE_BANDAGE -- the user request a bandage change
     * These request codes are stored in the Constants class of package Util.
     */
    private int actionRequested;

    /**
     * A count of how many times the FirebaseVisionImage has been scanned for a QR code
     */
    private int imageProcessedCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hide the toolbar for this activity
        Objects.requireNonNull(this.getSupportActionBar()).hide();
        setContentView(R.layout.activity_process_image);

//        firebaseAuth = FirebaseAuth.getInstance();
//        authStateListener = new FirebaseAuth.AuthStateListener() {
//            @Override
//            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
//                firebaseUser = firebaseAuth.getCurrentUser();
//            }
//        };


        // ------ To be removed ------
        // textView = findViewById(R.id.processImage_TextView);
         redTextView = findViewById(R.id.processImage_RedTextView);
         greenTextView = findViewById(R.id.processImage_GreenTextView);
         blueTextView = findViewById(R.id.processImage_BlueTextView);
         imageView = findViewById(R.id.processImage_ImageView);
        // ------ To be removed ------


        // Find the LinearLayout holding the initial loading animation in the XML file
        progressLayout = findViewById(R.id.processImage_ProgressLayout);
        // Find the LinearLayout holding the register success layout in the XML file
        registerSuccessLayout = findViewById(R.id.processImage_RegisterSuccessLayout);
        // Find the Initial Check Button in the XMl file
        initialCheckButton = findViewById(R.id.processImage_InitialCheckButton);
        // Collecting data passed into activity. Depending on which action has been requested by
        // the user, different sets of data will be passed in
        Bundle extra = getIntent().getExtras();
        if (extra != null) {
            // Get the requested action (passed in during all 3 requests)
            actionRequested = extra.getInt("action requested");
            // Get the image file url (passed in during all 3 requests)
            imageFromCameraResult = extra.getString("image captured");
            // Get the name of the limb from LimbListActivity
            limbName = extra.getString("limb name");
            // Get the ID of wound from MyWoundsActivity
            woundId = extra.getString("woundId");
            // Build the Bitmap object from the image file path
            bitmap = BitmapFactory.decodeFile(imageFromCameraResult);
            // Pass this Bitmap into this method to ensure correct orientation
            bitmap = rotateImage(bitmap);
            imageView.setImageBitmap(bitmap);
            // Create the FirebaseVisionImage from the rotated Bitmap
            firebaseVisionImage = FirebaseVisionImage.fromBitmap(bitmap);
            // Pass the FirebaseVisionImage to the FirebaseVisionBarcodeDetector for scanning
            processImage(firebaseVisionImage);
        }
    }

    /**
     * This method is the first step in detecting the QR code. Takes a FirebaseVisionImage and runs
     * it through the FirebaseVisionBarcodeDetector.
     * <p>
     * If successful, a list of type FirebaseVisionBarcode will contain the detected QR code
     *
     * @param image - the FirebaseVisionImage to scan
     */
    private void processImage(FirebaseVisionImage image) {
        // Increment the number of times the image has been scanned
        imageProcessedCount++;
        // Pass the FirebaseVisionImage to the FirebaseVisionBarcodeDetector
        detector.detectInImage(image)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionBarcode>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionBarcode> firebaseVisionBarcodes) {
                        // Here if image successfully scanned. Pass the returned list to
                        // processResult()
                        processResult(firebaseVisionBarcodes);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: " + e.getMessage());
                    }
                });
    }

    /**
     * This method first checks if the list of FirebaseVisionBarcode passed in is not empty. If
     * it's not the QR code was detected successfully and the QR code can be read to retrieve the
     * ID of the bandage on the Bitmap. The action requested by the user is then evaluated to
     * determine what course to take
     *
     * @param firebaseVisionBarcodes - the list containing the detected QR code
     */
    private void processResult(List<FirebaseVisionBarcode> firebaseVisionBarcodes) {
        // Check to make a sure a QR code was detected
        if (firebaseVisionBarcodes.size() > 0) {
            // Loop through the list -- should only be 1 QR code if the user has taken a picture
            // of their bandage only
            for (FirebaseVisionBarcode item : firebaseVisionBarcodes) {
                // Find the ID of the bandage on the image from the QR code value
                int valueType = item.getValueType();
                // Check if the format of the QR is TYPE_TEXT and no other
                if (valueType == FirebaseVisionBarcode.TYPE_TEXT) {
                    // Set the bandage ID to the QR code value
                    bandageId = item.getDisplayValue();
                } else {
                    // Here if the QR code value is anything but a text type
                    Log.d(TAG, "Not a recognised QR code type.");
                    // Close the activity so that the image is not processed
                    finish();
                }
                // Checking which action has been requested by the user
                // -- First case is if the user has selected 'Health Check'
                // -- Second case is if the user is registering a new wound
                // -- Third case is if user is changing the bandage on an existing wound
                switch (actionRequested) {
                    case Constants.REQUEST_CAMERA_ACTION:
                        if (imageProcessedCount == 1) {
                            // Get the initial coordinates of the QR code corners before rotation
                            qrCorners = item.getCornerPoints();
                            assert qrCorners != null;
                            // Calculate how much the Bitmap needs to be rotated so that the QR code
                            // is aligned correctly
                            rotationRequired = calculateRotation(qrCorners);
                            // Rotate the image view
                            imageView.setRotation((float) rotationRequired);
                            // Rotate the bitmap holding the image by the required amount
                            bitmap = rotateBitmap(bitmap, (float) rotationRequired);
                            // Reset the FirebaseVisionImage to the newly rotated bitmap
                            firebaseVisionImage = FirebaseVisionImage.fromBitmap(bitmap);
                            // Process the FirebaseVisionImage for a second time -- this time the
                            // QR code will be orientated correctly
                            processImage(firebaseVisionImage);
                        } else if (imageProcessedCount == 2) {
                            // Here after the second scan of the FirebaseVisionImage. Find the new
                            // coordinates of the QR code corners on the rotated Bitmap
                            qrCorners = item.getCornerPoints();
                            // Pass these coordinates into this method to find the location of
                            // the center circle colour
                            findColourOnBitmap(qrCorners);
                        }
                        break;
                    case Constants.REQUEST_REGISTER_WOUND:
                        // Register new wound by adding it to wounds collection in Firestore
                        registerWound();
                        break;
                    case Constants.REQUEST_UPDATE_BANDAGE:
                        // Update the document for an existing wound in the wounds collection
                        updateWoundRecord();
                        break;
                }
            }
        } else {
            // Here if the QR code could not be read -- the image was of insufficient quality
            // Finish this activity and revert to the CameraActivity for another image
            finish();
            // Inform the user of the problem and request another image
            Toast.makeText(this, "Could not read QR code", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Method calculates how much the Bitmap needs to be rotated by in order to orientate the QR
     * code parallel to the screen device. The method works by visualising the shape that the QR
     * code makes with the screen as a trapezoid and calculating the angles of the shape.
     *
     * @param corners - the first QR code corner coordinates
     * @return double - the required rotation
     */
    private double calculateRotation(Point[] corners) {
        // Array holds the x coordinate values of the QR code corners
        int[] x = {corners[0].x, corners[1].x, corners[2].x, corners[3].x};
        // Array holds the y coordinate values of the QR code corners
        int[] y = {corners[0].y, corners[1].y, corners[2].y, corners[3].y};
        // The short side of the trapezium
        double shortSide;
        // The acute angle of the trapezium opposite the short side
        double acuteAngle;
        // The angle of rotation to find
        double rotationRequired = 0;
        // Find which side of the trapezium the short side is on
        shortSide = Math.min(y[0], y[1]);
//        if (y[0] < y[1]) {
//            shortSide = y[0];
//        } else {
//            shortSide = y[1];
//        }
        // Calculating the acute angle of trapezoid
        // Need to know which is short side of trapezoid first
        if (shortSide == y[0]) {
            double angle1 = Math.atan2(0 - y[1], 0);
            double angle2 = Math.atan2(y[0] - y[1], x[0] - x[1]);
            acuteAngle = angle1 - angle2;
            rotationRequired = -(90 - Math.toDegrees(acuteAngle));
        } else if (shortSide == y[1]) {
            double angle1 = Math.atan2(0 - y[0], 0);
            double angle2 = Math.atan2(y[1] - y[0], x[1] - x[0]);
            acuteAngle = angle2 - angle1;
            rotationRequired = 90 - Math.toDegrees(acuteAngle);
        }
        // Return the final rotation calculated
        return rotationRequired;
    }

    /**
     * Method is used to locate the coordinates of the centre circle colour on the Bitmap, after it
     * has been rotated. First the width of the red boundary box on the Bitmap is calculated.
     *
     * @param corners - the QR code corner coordinates of the rotated Bitmap
     */
    private void findColourOnBitmap(Point[] corners) {
        // Array holds the x coordinate values of the QR code
        int[] x = {corners[0].x, corners[1].x, corners[2].x, corners[3].x};
        // Array holds the y coordinate values of the QR code
        int[] y = {corners[0].y, corners[1].y, corners[2].y, corners[3].y};

        // Calculate the width of the red boundary box on the Bitmap. First find the width of the
        // QR code on the Bitmap (x[1] - x[0]) and then multiply by the BANDAGE_BOX_FACTOR factor
        boxWidth = (x[1] - x[0]) * Constants.BANDAGE_BOX_FACTOR;
        // Calculate the hypotenuse from the diagonal of the box
        int hypotenuse = (int) (Math.sqrt(2) * boxWidth) / 2;
        // Use the hypotenuse to calculate the width of the
        int sides = (int) Math.sqrt((Math.pow(hypotenuse, 2)) / 2);

        // Check which Quarter of the screen the QR code is in
        switch (Objects.requireNonNull(checkQRPosition(x, y))) {
            case TOP_LEFT:
                colourXPosition = sides + x[0];
                colourYPosition = sides + y[0];
                referenceXPosition = (x[0] + 100);
                referenceYPosition = (y[0] + (boxWidth - 100));
                calculateColourValue(colourXPosition, colourYPosition, referenceXPosition,
                        referenceYPosition);
                break;
            case TOP_RIGHT:
                colourXPosition = x[1] - sides;
                colourYPosition = y[1] + sides;
                referenceXPosition = x[1] - (boxWidth - 100);
                referenceYPosition = y[1] + 100;
                calculateColourValue(colourXPosition, colourYPosition, referenceXPosition,
                        referenceYPosition);
                break;
            case BOTTOM_LEFT:
                colourXPosition = sides + x[3];
                colourYPosition = y[3] - sides;
                referenceXPosition = x[3] + (boxWidth - 100);
                referenceYPosition = y[3] - 100;
                calculateColourValue(colourXPosition, colourYPosition, referenceXPosition,
                        referenceYPosition);
                break;
            case BOTTOM_RIGHT:
                colourXPosition = x[2] - sides;
                colourYPosition = y[2] - sides;
                referenceXPosition = x[2] - 100;
                referenceYPosition = y[2] - (boxHeight - 100);
                calculateColourValue(colourXPosition, colourYPosition, referenceXPosition,
                        referenceYPosition);
                break;
            default:
                break;
        }
    }

    private void calculateColourValue(int colourXPosition, int colourYPosition,
                                      int referenceXPosition, int referenceYPosition) {

        float[] hsv = findRawColour(colourXPosition, colourYPosition);
        float[] offset = checkReferenceOffset(referenceXPosition, referenceYPosition);

        float[] trueRGB = new float[3];
        trueRGB[0] = hsv[0] + offset[0];
        trueRGB[1] = hsv[1] + offset[1];
        trueRGB[2] = hsv[2] + offset[2];

        // Using bitwise to get more accurate RGB values
        int trueColour = Color.HSVToColor(trueRGB);
        int trueRed = (trueColour >> 16) & 0xFF;
        int trueGreen = (trueColour >> 8) & 0xFF;
        int trueBlue = trueColour & 0xFF;

        Log.d("RGB true", "rgb : " + trueRed + ", " + trueGreen + ", " + trueBlue);

        // createMarker(referenceXPosition, referenceYPosition);
        setText(trueRed, trueGreen, trueBlue);

        startActivity(new Intent(ProcessImageActivity.this,
                PatientFeedbackActivity.class)
                .putExtra("image captured", imageFromCameraResult)
                .putExtra("redValue", trueRed)
                .putExtra("greenValue", trueGreen)
                .putExtra("blueValue", trueBlue)
                .putExtra("bandageId", bandageId)
                .putExtra("woundId", woundId)
        );
    }

    private float[] findRawColour(int findColourX, int findColourY) {
        // Find the raw rgb value of the pixel at the found position
        int colour = bitmap.getPixel(findColourX, findColourY);
        int red = (colour >> 16) & 0xFF;
        int green = (colour >> 8) & 0xFF;
        int blue = colour & 0xFF;

        Log.d("RGB raw", "rgb : " + red + ", " + green + ", " + blue);

        // Convert the raw rgb value to hsv and store in an array
        float[] rawHSVFound = new float[3];
        Color.RGBToHSV(red, green, blue, rawHSVFound);
        return rawHSVFound;
    }

    private float[] checkReferenceOffset(int findReferenceX, int findReferenceY) {
        // Find the rgb value of the reference colour on the image
        int colour = bitmap.getPixel(findReferenceX, findReferenceY);
        int refRed = (colour >> 16) & 0xFF;
        int refGreen = (colour >> 8) & 0xFF;
        int refBlue = colour & 0xFF;

        // Convert reference to hsv
        float[] refHsv = new float[3];
        Color.RGBToHSV(refRed, refGreen, refBlue, refHsv);

        // The RGB values of the reference colour -- taken from the constants class in util
        int actualRed = Constants.referenceRedValue;
        int actualGreen = Constants.referenceGreenValue;
        int actualBlue = Constants.referenceBlueValue;

        float[] realHsv = new float[3];
        Color.RGBToHSV(actualRed, actualGreen, actualBlue, realHsv);

        float[] offset = new float[3];
        offset[0] = realHsv[0] - refHsv[0];
        offset[1] = realHsv[1] - refHsv[1];
        offset[2] = realHsv[2] - refHsv[2];

        Log.d("RGB at reference", "rgb : " + refRed + ", " + refGreen + ", " + refBlue);
        Log.d("RGB offset at reference", "rgb : " + offset[0] + ", " + offset[1] + ", " + offset[2]);
        return offset;
    }

    private void registerWound() {
        // Display loading animation while making calls to Firestore collection
        // registerProgressBar.setVisibility(View.VISIBLE);

        // Get the ID of the logged in user from the singleton class
        String userId = PatientManager.getInstance().getPatientId();
        // Create a unique ID for the new wound combining the user's ID and the current time
        final String newWoundId = userId + "_" + Timestamp.now().getSeconds();

        Timestamp today = new Timestamp(new Date());

        // Create the Wound object to be added to the collection
        Wound wound = new Wound();
        // Set the patient who registered the wound
        wound.setUserId(userId);
        // Bandage ID comes from the value read from the QR code
        wound.setBandageId(bandageId);
        // Limb name is passed in from the LimbListActivity
        wound.setLimbName(limbName);
        // Set the date that the wound was registered
        wound.setDate(today);
        // Set the new wounds ID
        wound.setWoundId(newWoundId);
        // Each new wound will require an update picture once a day
        wound.setHoursUntilCheck(24);
        // Set the date when the bandage was changed -- today
        wound.setBandageLastChanged(today);

        Calendar date = Calendar.getInstance();
        long t = date.getTimeInMillis();
        Date afterAddOneDay = new Date(t + (24 * 3600000));
        Timestamp timestamp = new Timestamp(afterAddOneDay);
        wound.setNextHealthCheck(timestamp);

        // Add the wound to the collection
        collectionReference
                .add(wound)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        // Call to Firestore is complete - hide progress bar
                        // registerProgressBar.setVisibility(View.INVISIBLE);
                        progressLayout.setVisibility(View.INVISIBLE);
                        registerSuccessLayout.setVisibility(View.VISIBLE);

                        initialCheckButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // Return the user to the MyWoundsActivity
                                startActivity(new Intent(ProcessImageActivity.this,
                                        CameraActivity.class)
                                        .putExtra("woundId", newWoundId)
                                        .putExtra("action requested", Constants.REQUEST_CAMERA_ACTION));
                                // End the current activity so the user can not return
                                finish();
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: " + e.getMessage());
                    }
                });
    }

    // Key relating to the field bandageId in the wounds collection
    public static final String KEY_BANDAGE_ID = "bandageId";
    // Key relating to the field date in the wounds collection
    public static final String KEY_DATE_CHANGED = "date";

    private void updateWoundRecord() {
        // Display loading animation while making calls to Firestore collection
        // registerProgressBar.setVisibility(View.VISIBLE);

        // Create a Hash map to update relevant document in collection with new bandage ID
        final Map<String, Object> updateData = new HashMap<>();
        // Put the new bandage ID got from the QR code and the new date into the map
        updateData.put(KEY_BANDAGE_ID, bandageId);
        updateData.put(KEY_DATE_CHANGED, new Timestamp(new Date()));

        // Make call to collection and get the relevant document
        collectionReference
                .whereEqualTo("woundId", woundId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                collectionReference
                                        .document(document.getId())
                                        .update(updateData)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                // registerProgressBar.setVisibility(View.INVISIBLE);
                                                startActivity(new Intent(ProcessImageActivity.this,
                                                        MyWoundsActivity.class));
                                                finish();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.d(TAG, "onFailure: could not update document" + e.toString());
                                            }
                                        });
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: " + e.toString());
                    }
                });
    }

    private Quarter checkQRPosition(int[] x, int[] y) {
        if (x[0] < bitmap.getWidth() / 2 && (y[0] < bitmap.getHeight() / 2)) {
            return Quarter.TOP_LEFT;
        } else if (x[0] > bitmap.getWidth() / 2 && (y[0] < bitmap.getHeight() / 2)) {
            return Quarter.TOP_RIGHT;
        } else if ((x[0] < bitmap.getWidth() / 2) && (y[0] > bitmap.getHeight() / 2)) {
            return Quarter.BOTTOM_LEFT;
        } else if ((x[0] > bitmap.getWidth() / 2) && (y[0] > bitmap.getHeight() / 2)) {
            return Quarter.BOTTOM_RIGHT;
        } else {
            return null;
        }
    }

    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private Bitmap rotateImage(Bitmap bitmap) {
        ExifInterface exifInterface = null;
        try {
            exifInterface = new ExifInterface(imageFromCameraResult);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED);

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            default:
        }

        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imageFromCameraResult, bmOptions);
        return rotatedBitmap;
    }

    enum Quarter {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//
//        firebaseUser = firebaseAuth.getCurrentUser();
//        firebaseAuth.addAuthStateListener(authStateListener);
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//
//        if (firebaseAuth != null) {
//            firebaseAuth.removeAuthStateListener(authStateListener);
//        }
//    }

    // Methods that can be deleted when finished

    private void setText(int red, int green, int blue) {
        redTextView.setText("Red: " + red);
        greenTextView.setText("Green: " + green);
        blueTextView.setText("Blue:" + blue);
    }
//
//    private void createMarker(int findColourX, int findColourY) {
//        float[] point = new float[2];
//        point[0] = findColourX;
//        point[1] = findColourY;
//        //imageView.getImageMatrix().mapPoints(point);
//        textView.setX(point[0]);
//        textView.setY(point[1]);
//        textView.setText("@");
//        textView.setTextColor(Color.RED);
//    }

}
