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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import model.Wound;
import util.Constants;
import util.PatientManager;

public class ProcessImageActivity extends AppCompatActivity {

    // Debug tag for this activity
    private static final String TAG = "ProcessTestActivity";

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser firebaseUser;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = db.collection("wounds");

    private TextView redTextView;
    private TextView greenTextView;
    private TextView blueTextView;
    private TextView textView;
    // private ProgressBar registerProgressBar;
    private LinearLayout progressLayout;
    private LinearLayout registerSuccessLayout;
    private Button initialCheckButton;

    //private ImageView imageView;
    private String imageFromCameraResult;
    private Bitmap bitmap;
    private Point[] qrCorners;
    private int boxWidth;
    private int boxHeight;
    private int colourXPosition;
    private int colourYPosition;
    private int referenceXPosition;
    private int referenceYPosition;

    FirebaseVisionBarcodeDetectorOptions options = new FirebaseVisionBarcodeDetectorOptions
            .Builder()
            .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_QR_CODE)
            .build();
    FirebaseVisionBarcodeDetector detector = FirebaseVision
            .getInstance()
            .getVisionBarcodeDetector(options);
    FirebaseVisionImage firebaseVisionImage;
    double rotationRequired;

    private String bandageId;
    private String limbName;
    private String woundId;

    private int actionRequested;
    private int imageProcessedCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            this.getSupportActionBar().hide();
        } catch (NullPointerException e) {
            Log.d(TAG, "onCreate: " + e.toString());
        }
        setContentView(R.layout.activity_process_image);

        firebaseAuth = FirebaseAuth.getInstance();
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                firebaseUser = firebaseAuth.getCurrentUser();
                if (firebaseUser != null) {
                } else {
                }
            }
        };

        // UI elements
        textView = findViewById(R.id.processImage_TextView);
        redTextView = findViewById(R.id.processImage_RedTextView);
        greenTextView = findViewById(R.id.processImage_GreenTextView);
        blueTextView = findViewById(R.id.processImage_BlueTextView);
        //imageView = findViewById(R.id.processImage_ImageView);
        progressLayout = findViewById(R.id.processImage_ProgressLayout);
        registerSuccessLayout = findViewById(R.id.processImage_RegisterSuccessLayout);
        initialCheckButton = findViewById(R.id.processImage_InitialCheckButton);
        // registerProgressBar = findViewById(R.id.processImage_RegisterProgressBar);

        // Collecting data passed into activity
        Bundle extra = getIntent().getExtras();
        if (extra != null) {
            // Get the requested action
            actionRequested = extra.getInt("action requested");
            limbName = extra.getString("limb name");
            woundId = extra.getString("woundId");
            imageFromCameraResult = extra.getString("image captured");

            bitmap = BitmapFactory.decodeFile(imageFromCameraResult);
            bitmap = rotateImage(bitmap);
            //imageView.setImageBitmap(bitmap);
            firebaseVisionImage = FirebaseVisionImage.fromBitmap(bitmap);

            processImage(firebaseVisionImage);
        }
    }

    private void processImage(FirebaseVisionImage image) {
        imageProcessedCount++;
        detector.detectInImage(image)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionBarcode>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionBarcode> firebaseVisionBarcodes) {
                        processResult(firebaseVisionBarcodes);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ProcessImageActivity.this,
                                "" + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void processResult(List<FirebaseVisionBarcode> firebaseVisionBarcodes) {
        if (firebaseVisionBarcodes.size() > 0) {
            for (FirebaseVisionBarcode item : firebaseVisionBarcodes) {
                // Find the ID of the bandage on the image from the QR code value
                int valueType = item.getValueType();
                switch (valueType) {
                    case FirebaseVisionBarcode.TYPE_TEXT: {
                        // Set the bandage ID to the QR code value
                        bandageId = item.getDisplayValue();
                    }
                    break;
                    default:
                        // Here if the QR code value is anything but a text type
                        Log.d(TAG, "Not a recognised QR code type.");
                        break;
                }
                // Checking which action has been requested by the user
                // -- First case is if the user has selected 'Health Check'
                // -- Second case is if the user is registering a new wound
                // -- Third case is if user is changing the bandage on an existing wound
                switch (actionRequested) {
                    case Constants.REQUEST_CAMERA_ACTION:
                        if (imageProcessedCount == 1) {
                            // Get the initial coordinates of the QR code before rotation
                            qrCorners = item.getCornerPoints();
                            assert qrCorners != null;
                            // Calculate how much the image needs to be rotated so that the QR code
                            // is aligned correctly
                            rotationRequired = calculateRotation(qrCorners);
                            // Rotate the image view
                            //imageView.setRotation((float) rotationRequired);
                            // Rotate the bitmap holding the image by the required amount
                            bitmap = rotateBitmap(bitmap, (float) rotationRequired);
                            // Reset the firebase vision image to the newly rotated bitmap
                            firebaseVisionImage = FirebaseVisionImage.fromBitmap(bitmap);
                            // Process the firebase vision image for a second time -- this time the
                            // QR code will be orientated correctly
                            processImage(firebaseVisionImage);
                        } else if (imageProcessedCount == 2) {
                            // Find the new coordinates of the QR code on the rotated image
                            qrCorners = item.getCornerPoints();
                            // Pass these coordinates into this method to find the colour
                            findColourOnBitmap(qrCorners);
                        }
                        break;
                    case Constants.REQUEST_REGISTER_WOUND:
                        // Register new wound by adding it to collection
                        registerWound();
                        break;
                    case Constants.REQUEST_UPDATE_BANDAGE:
                        // Update the collection record for an existing wound
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

    private double calculateRotation(Point[] corners) {
        // Array holds the x coordinate values of the QR code
        int[] x = {corners[0].x, corners[1].x, corners[2].x, corners[3].x};
        // Array holds the y coordinate values of the QR code
        int[] y = {corners[0].y, corners[1].y, corners[2].y, corners[3].y};

        double shortSide;
        double acuteAngle;
        double rotationRequired = 0;

        // Calculate the long and short sides of the trapezoid
        if (y[0] < y[1]) {
            shortSide = y[0];
        } else {
            shortSide = y[1];
        }
        // Calculating the acute angle of trapezoid
        // Need to find which is short side of trapezoid first
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
        return rotationRequired;
    }

    private void findColourOnBitmap(Point[] corners) {
        // Array holds the x coordinate values of the QR code
        int[] x = {qrCorners[0].x, qrCorners[1].x, qrCorners[2].x, qrCorners[3].x};
        // Array holds the y coordinate values of the QR code
        int[] y = {qrCorners[0].y, qrCorners[1].y, qrCorners[2].y, qrCorners[3].y};

        boxWidth = (x[1] - x[0]) * 5;
        int hypotenuse = (int) (Math.sqrt(2) * boxWidth) / 2;
        int sides = (int) Math.sqrt((Math.pow(hypotenuse, 2)) / 2);

        // Check which quarter of the screen the QR code is in
        switch (Objects.requireNonNull(checkQRPosition(x, y))) {
            case TOP_LEFT:
                Log.d(TAG, "top left");
                colourXPosition = sides + x[0];
                colourYPosition = sides + y[0];
                referenceXPosition = (x[0] + 100);
                referenceYPosition = (y[0] + (boxWidth - 100));
                calculateColourValue(colourXPosition, colourYPosition, referenceXPosition,
                        referenceYPosition);
                break;
            case TOP_RIGHT:
                Log.d(TAG, "top right");
                colourXPosition = x[1] - sides;
                colourYPosition = y[1] + sides;
                referenceXPosition = x[1] - (boxWidth - 100);
                referenceYPosition = y[1] + 100;
                calculateColourValue(colourXPosition, colourYPosition, referenceXPosition,
                        referenceYPosition);
                break;
            case BOTTOM_LEFT:
                Log.d(TAG, "bottom left");
                colourXPosition = sides + x[3];
                colourYPosition = y[3] - sides;
                referenceXPosition = x[3] + (boxWidth - 100);
                referenceYPosition = y[3] - 100;
                calculateColourValue(colourXPosition, colourYPosition, referenceXPosition,
                        referenceYPosition);
                break;
            case BOTTOM_RIGHT:
                Log.d(TAG, "bottom right");
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
                                      int referenceXPosition, int referenceYPosition){

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

        // createMarker(referenceXPosition, referenceYPosition);
        // setText(trueRed, trueGreen, trueBlue);
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

        // The RGB values of the pink colour hardcoded here to test with
        int actualRed = 255;
        int actualGreen = 174;
        int actualBlue = 201;

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
        collectionReference.add(wound)
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

    @Override
    protected void onStart() {
        super.onStart();

        firebaseUser = firebaseAuth.getCurrentUser();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (firebaseAuth != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }

    // Methods that can be deleted when finished

    private void setText(int red, int green, int blue) {
        redTextView.setText("Red: " + red);
        greenTextView.setText("Green: " + green);
        blueTextView.setText("Blue:" + blue);
    }

    private void createMarker(int findColourX, int findColourY) {
        float[] point = new float[2];
        point[0] = findColourX;
        point[1] = findColourY;
        //imageView.getImageMatrix().mapPoints(point);
        textView.setX(point[0]);
        textView.setY(point[1]);
        textView.setText("@");
        textView.setTextColor(Color.RED);
    }

}
