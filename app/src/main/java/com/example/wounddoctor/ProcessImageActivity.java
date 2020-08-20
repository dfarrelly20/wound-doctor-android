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
import android.widget.ImageView;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    private ProgressBar registerProgressBar;

    private ImageView imageView;
    private String imageFromCameraResult;
    private Bitmap bitmap;
    private Point[] qrCorners;
    private int boxWidth;
    private int boxHeight;
    private int colourXPosition;
    private int colourYPosition;
    private int referenceXPosition;
    private int referenceYPosition;

    FirebaseVisionBarcodeDetectorOptions options = new FirebaseVisionBarcodeDetectorOptions.Builder()
            .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_QR_CODE)
            .build();
    FirebaseVisionBarcodeDetector detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options);
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

        // UI views
        textView = findViewById(R.id.processImage_TextView);
        redTextView = findViewById(R.id.processImage_RedTextView);
        greenTextView = findViewById(R.id.processImage_GreenTextView);
        blueTextView = findViewById(R.id.processImage_BlueTextView);
        imageView = findViewById(R.id.processImage_ImageView);
        registerProgressBar = findViewById(R.id.processImage_RegisterProgressBar);

        // For screen dimensions
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int height = displayMetrics.heightPixels;
        int width = displayMetrics.widthPixels;
        Log.d(TAG, "onCreate: heigth: " + height);
        Log.d(TAG, "onCreate: width: " + width);

        // Collecting data passed into activity
        Bundle extra = getIntent().getExtras();
        if (extra != null) {
            // Get the requested action
            actionRequested = extra.getInt("action requested");
            limbName = extra.getString("limb name");
            woundId = extra.getString("wound id");
            imageFromCameraResult = extra.getString("image captured");

            bitmap = BitmapFactory.decodeFile(imageFromCameraResult);
            bitmap = rotateImage(bitmap);
            imageView.setImageBitmap(bitmap);
            firebaseVisionImage = FirebaseVisionImage.fromBitmap(bitmap);
            // bitmap = getResizedBitmap(bitmap, height);

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
                        Toast.makeText(ProcessImageActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                            rotationRequired = getCorners(qrCorners);
                            // Rotate the image view
                            imageView.setRotation((float) rotationRequired);
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
                            findColour3(qrCorners);
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
            // Finish this activity and revert to the CameraActivity
            finish();
            // Inform the user of the problem and request another image
            Toast.makeText(this, "Could not read QR code", Toast.LENGTH_LONG).show();
        }
    }

    private double getCorners(Point[] corners) {

        int[] x = {
                corners[0].x,
                corners[1].x,
                corners[2].x,
                corners[3].x
        };

        int[] y = {
                corners[0].y,
                corners[1].y,
                corners[2].y,
                corners[3].y
        };

        Log.d(TAG, "x array: " + Arrays.toString(x));
        Log.d(TAG, "y array: " + Arrays.toString(y));

        double shortSide;
        double longSide;
        double base;
        double diagonal;
        double acuteAngle = 0;
        double rotationRequired = 0;

        base = x[1] - x[0];
        Log.d(TAG, "base: " + base);

        if (y[0] < y[1]) {
            shortSide = y[0];
            longSide = y[1];
        } else {
            shortSide = y[1];
            longSide = y[0];
        }

        Log.d(TAG, "short side: " + shortSide);
        Log.d(TAG, "long side: " + longSide);

        // Calculate diagonal of trapezoid
        diagonal = Math.sqrt(Math.pow(longSide - shortSide, 2) + Math.pow(base, 2));
        Log.d(TAG, "diagonal: " + diagonal);

        // Calculating acute angle
        // Need to find which is long side of trapezoid first
        if (shortSide == y[0]) {
            double angle1 = Math.atan2(0 - y[1],
                    x[1] - x[1]);
            double angle2 = Math.atan2(y[0] - y[1],
                    x[0] - x[1]);
            Log.d(TAG, "angle1: " + angle1);
            Log.d(TAG, "angle2: " + angle2);
            acuteAngle = angle1 - angle2;
            rotationRequired = -(90 - Math.toDegrees(acuteAngle));
        } else if (shortSide == y[1]) {
            double angle1 = Math.atan2(0 - y[0],
                    x[0] - x[0]);
            double angle2 = Math.atan2(y[1] - y[0],
                    x[1] - x[0]);
            Log.d(TAG, "angle1: " + angle1);
            Log.d(TAG, "angle2: " + angle2);
            acuteAngle = angle2 - angle1;
            rotationRequired = 90 - Math.toDegrees(acuteAngle);
        }

        Log.d(TAG, "acute: " + acuteAngle);
        Log.d(TAG, "rotationRequired: " + rotationRequired);

        return rotationRequired;
    }

    private void findColour3(Point[] corners) {

        int[] x = {
                qrCorners[0].x,
                qrCorners[1].x,
                qrCorners[2].x,
                qrCorners[3].x,
        };

        int[] y = {
                qrCorners[0].y,
                qrCorners[1].y,
                qrCorners[2].y,
                qrCorners[3].y
        };

        Log.d(TAG, "Corner points: " + corners[0]);
        Log.d(TAG, "Corner points: " + corners[1]);
        Log.d(TAG, "Corner points: " + corners[2]);
        Log.d(TAG, "Corner points: " + corners[3]);

        int sides = createBox(x, y);

        switch (checkQRPosition(x, y)) {
            case TOP_LEFT:
                Log.d(TAG, "top left");
                colourXPosition = sides + x[0];
                colourYPosition = sides + y[0];
                // referenceXPosition = (int) (x[0] + (boxWidth * 0.03));
                // referenceYPosition = (int) (y[0] + (boxHeight - (boxWidth * 0.03)));
                referenceXPosition = (x[0] + 100);
                referenceYPosition = (y[0] + (boxHeight - 50));
                break;
            case TOP_RIGHT:
                Log.d(TAG, "top right");
                colourXPosition = x[1] - sides;
                colourYPosition = y[1] + sides;
                referenceXPosition = x[1] - (boxWidth - 100);
                referenceYPosition = y[1] + 100;
                break;
            case BOTTOM_LEFT:
                Log.d(TAG, "bottom left");
                colourXPosition = sides + x[3];
                colourYPosition = y[3] - sides;
                referenceXPosition = x[3] + (boxWidth - 100);
                referenceYPosition = y[3] - 100;
                break;
            case BOTTOM_RIGHT:
                Log.d(TAG, "bottom right");
                colourXPosition = x[2] - sides;
                colourYPosition = y[2] - sides;
                referenceXPosition = x[2] - 100;
                referenceYPosition = y[2] - (boxHeight - 100);
                break;
            default:
                break;
        }

        // int[] rotatedPoint = findRotatedPoint(colourXPosition, colourYPosition);
        // int[] rotatedReference = findRotatedPoint(referenceXPosition, referenceYPosition);

        // float[] hsv = findRawColour(rotatedPoint[0], rotatedPoint[1]);
        float[] hsv = findRawColour(colourXPosition, colourYPosition);

        // int referenceColour = bitmap.getPixel(rotatedReference[0], rotatedReference[1]);
        float[] offset = checkReferenceOffset(referenceXPosition, referenceYPosition);

        Log.d(TAG, "hsv offset: " + Arrays.toString(offset));

        float[] newRGB = new float[3];
        newRGB[0] = hsv[0] + offset[0];
        newRGB[1] = hsv[1] + offset[1];
        newRGB[2] = hsv[2] + offset[2];

        int newColour = Color.HSVToColor(newRGB);
        int newRed = (newColour >> 16) & 0xFF;
        int newGreen = (newColour >> 8) & 0xFF;
        int newBlue = newColour & 0xFF;

        // createMarker(rotatedPoint[0], rotatedPoint[1]);
        createMarker(colourXPosition, colourYPosition);
        // textView.setText("@");
        // textView.setX(colourXPosition);
        // textView.setY(colourYPosition);
        // setText(newRed, newGreen, newBlue);

    }

    private int createBox(int[] x, int[] y) {
        int qrWidth = x[1] - x[0];
        int qrHeight = y[2] - y[1];

        Log.d(TAG, "qr width: " + qrWidth);
        Log.d(TAG, "qr height: " + qrHeight);

        boxWidth = qrWidth * 5;
        boxHeight = qrHeight * 5;

        int diagonal = (int) (Math.sqrt(2) * boxWidth);
        int hypotenuse = diagonal / 2;
        int sides = pythagoras(hypotenuse);

        return sides;
    }

    private int pythagoras(int hypotenuse) {
        int sides;
        sides = (int) Math.sqrt((Math.pow(hypotenuse, 2)) / 2);
        return sides;
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

    private float[] findRawColour(int findColourX, int findColourY) {

        int colour = bitmap.getPixel(findColourX, findColourY);

        Log.d(TAG, "find colour bitmap width: " + bitmap.getWidth());
        Log.d(TAG, "find colour bitmap height: " + bitmap.getHeight());

        int red = (colour >> 16) & 0xFF;
        int green = (colour >> 8) & 0xFF;
        int blue = colour & 0xFF;

        setText(red, green, blue);

        Log.d("RGB at found position", "rgb : " + red + ", " + green + ", " + blue);

        float[] hsv = convertToHSV(red, green, blue);

        return hsv;
    }

    private float[] convertToHSV(int red, int green, int blue) {
        float[] hsv = new float[3];
        Color.RGBToHSV(red, green, blue, hsv);
        return hsv;
    }

    private float[] checkReferenceOffset(int findReferenceX, int findReferenceY) {

        int colour = bitmap.getPixel(findReferenceX, findReferenceY);

        int refRed = (colour >> 16) & 0xFF;
        int refGreen = (colour >> 8) & 0xFF;
        int refBlue = colour & 0xFF;

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

    private void setText(int red, int green, int blue) {
        redTextView.setText("Red: " + red);
        greenTextView.setText("Green: " + green);
        blueTextView.setText("Blue:" + blue);
    }

    private void createMarker(int findColourX, int findColourY) {
        float[] point = new float[2];
        point[0] = findColourX;
        point[1] = findColourY;
        imageView.getImageMatrix().mapPoints(point);
        textView.setX(point[0]);
        textView.setY(point[1]);
        textView.setText("@");
        textView.setTextColor(Color.RED);
    }

    private Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        Log.d("Reduced dimensions", "Width, height: " + width + ", " + height);

        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        // matrix.setRotate(angle);
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

    private void registerWound() {
        // Display loading animation while making calls to Firestore collection
        registerProgressBar.setVisibility(View.VISIBLE);

        // Get the ID of the logged in user from the singleton class
        String userId = PatientManager.getInstance().getPatientId();
        // Create a unique ID for the new wound combining the user's ID and the current time
        String woundId = userId + "_" + Timestamp.now().getSeconds();

        // Create the Wound object to be added to the collection
        Wound wound = new Wound();
        wound.setUserId(userId);
        // Bandage ID comes from the value read from the QR code
        wound.setBandageId(bandageId);
        // Limb name is passed in from the LimbListActivity
        wound.setLimbName(limbName);
        wound.setDate(new Timestamp(new Date()));
        wound.setWoundId(woundId);
        // Each new wound will require an update picture once a day
        wound.setHoursUntilCheck(24);

        // Add the wound to the collection
        collectionReference.add(wound)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        // Call to Firestore is complete - hide progress bar
                        registerProgressBar.setVisibility(View.INVISIBLE);
                        // Return the user to the MyWoundsActivity
                        startActivity(new Intent(ProcessImageActivity.this,
                                MyWoundsActivity.class));
                        // End the current activity so the user can not return
                        finish();
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
        registerProgressBar.setVisibility(View.VISIBLE);

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
                                                registerProgressBar.setVisibility(View.INVISIBLE);
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

}
