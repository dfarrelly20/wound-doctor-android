package util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.media.ExifInterface;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.wounddoctor.ProcessImageActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ProcessImage {

    private static final String TAG = "ProcessImage";

    private String imageFilePath;

    private Bitmap bitmap;

    private Point[] qrCorners;
    private int[] xCoords;
    private int[] yCoords;

    private int boxWidth;
    private int boxHeight;

    boolean isDetected = false;
    FirebaseVisionBarcodeDetectorOptions options = new FirebaseVisionBarcodeDetectorOptions.Builder()
            .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_QR_CODE)
            .build();
    FirebaseVisionBarcodeDetector detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options);
    FirebaseVisionImage firebaseVisionImage;

    double rotationRequired;

    private int colourXPosition;
    private int colourYPosition;
    private int referenceXPosition;
    private int referenceYPosition;

    private int red, green, blue;

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

    public void loadBitmap(String filePath){

        imageFilePath = filePath;
        bitmap = BitmapFactory.decodeFile(imageFilePath);
        bitmap = rotateImage(bitmap);
        firebaseVisionImage = FirebaseVisionImage.fromBitmap(bitmap);

    }

    private void processImage(FirebaseVisionImage image) {
        if (!isDetected) {
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
                            Log.d(TAG, "onFailure: could not detect qr code in firebase image");
                        }
                    });
        }
    }

    private void processResult(List<FirebaseVisionBarcode> firebaseVisionBarcodes) {
        if (firebaseVisionBarcodes.size() > 0) {
            isDetected = true;
            for (FirebaseVisionBarcode item : firebaseVisionBarcodes) {

                int valueType = item.getValueType();
                if (valueType == FirebaseVisionBarcode.TYPE_TEXT) {
                    // Toast.makeText(this, item.getDisplayValue(), Toast.LENGTH_SHORT).show();
                } else {
                    // finish();
                    // Toast.makeText(this, "Not a recognised QR code type", Toast.LENGTH_SHORT).show();
                }

                qrCorners = item.getCornerPoints();
                xCoords = new int[] {
                        qrCorners[0].x,
                        qrCorners[1].x,
                        qrCorners[2].x,
                        qrCorners[3].x
                };
                yCoords = new int[] {
                        qrCorners[0].y,
                        qrCorners[1].y,
                        qrCorners[2].y,
                        qrCorners[3].y
                };

                // rotationRequired = findRotation();
                findRotation();
                // imageView.setRotation((float) rotationRequired);

                // findColour();
            }
        } else {
            // finish();
            // Toast.makeText(this, "Could not read QR code. Please take another image.", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "processResult: could not read qr code. take another image");
        }
    }

    private void findRotation() {

        double shortSide, longSide, base, diagonal;
        double acuteAngle = 0;
        double rotationRequired = 0;

        base = xCoords[1] - xCoords[0];
        Log.d(TAG, "base: " + base);

        if (yCoords[0] < yCoords[1]) {
            shortSide = yCoords[0];
            longSide = yCoords[1];
        } else {
            shortSide = yCoords[1];
            longSide = yCoords[0];
        }

        Log.d(TAG, "short side: " + shortSide);
        Log.d(TAG, "long side: " + longSide);

        // Calculate diagonal of trapezoid
        diagonal = Math.sqrt(Math.pow(longSide - shortSide, 2) + Math.pow(base, 2));
        Log.d(TAG, "diagonal: " + diagonal);

        // Calculating acute angle
        // Need to find which is long side of trapezoid first
        if (shortSide == yCoords[0]) {
            double angle1 = Math.atan2(0 - yCoords[1],
                    xCoords[1] - xCoords[1]);
            double angle2 = Math.atan2(yCoords[0] - yCoords[1],
                    xCoords[0] - xCoords[1]);
            Log.d(TAG, "angle1: " + angle1);
            Log.d(TAG, "angle2: " + angle2);
            acuteAngle = angle1 - angle2;
            rotationRequired = -(90 - Math.toDegrees(acuteAngle));
        } else if (shortSide == yCoords[1]) {
            double angle1 = Math.atan2(0 - yCoords[0],
                    xCoords[0] - xCoords[0]);
            double angle2 = Math.atan2(yCoords[1] - yCoords[0],
                    xCoords[1] - xCoords[0]);
            Log.d(TAG, "angle1: " + angle1);
            Log.d(TAG, "angle2: " + angle2);
            acuteAngle = angle2 - angle1;
            rotationRequired = 90 - Math.toDegrees(acuteAngle);
        }

        Log.d(TAG, "acute: " + acuteAngle);
        Log.d(TAG, "rotationRequired: " + rotationRequired);

        bitmap = rotateBitmap(bitmap, (float) rotationRequired);

        // return rotationRequired;

        if (rotationRequired < 10) {
            findColour();
        } else {
            // Rotation too great
        }
    }

    private void findColour() {

        Log.d(TAG, "Corner points: " + qrCorners[0]);
        Log.d(TAG, "Corner points: " + qrCorners[1]);
        Log.d(TAG, "Corner points: " + qrCorners[2]);
        Log.d(TAG, "Corner points: " + qrCorners[3]);

        int sides = createBox(xCoords, yCoords);

        switch (checkQRPosition(xCoords, yCoords)) {
            case TOP_LEFT:
                Log.d(TAG, "top left");
                colourXPosition = sides + xCoords[0];
                colourYPosition = sides + yCoords[0];
                referenceXPosition = (int) (xCoords[0] + (boxWidth * 0.03));
                referenceYPosition = (int) (yCoords[0] + (boxHeight - (boxWidth * 0.03)));
                break;
            case TOP_RIGHT:
                Log.d(TAG, "top right");
                colourXPosition = xCoords[1] - sides;
                colourYPosition = yCoords[1] + sides;
                referenceXPosition = xCoords[1] - (boxWidth - 100);
                referenceYPosition = yCoords[1] + 100;
                break;
            case BOTTOM_LEFT:
                Log.d(TAG, "bottom left");
                colourXPosition = sides + xCoords[3];
                colourYPosition = yCoords[3] - sides;
                referenceXPosition = xCoords[3] + (boxWidth - 100);
                referenceYPosition = yCoords[3] - 100;
                break;
            case BOTTOM_RIGHT:
                Log.d(TAG, "bottom right");
                colourXPosition = xCoords[2] - sides;
                colourYPosition = yCoords[2] - sides;
                referenceXPosition = xCoords[2] - 100;
                referenceYPosition = yCoords[2] - (boxHeight - 100);
                break;
            default:
                break;
        }

        int[] rotatedPoint = findRotatedPoint(colourXPosition, colourYPosition);
        // int[] rotatedReference = findRotatedPoint(referenceXPosition, referenceYPosition);

        float[] hsv = findRawColour(rotatedPoint[0], rotatedPoint[1]);

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
        // setText(newRed, newGreen, newBlue);

        this.red = newRed;
        this.green = newGreen;
        this.blue = newBlue;
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

    private int[] findRotatedPoint(int x, int y) {

        Log.d(TAG, "findRotatedPoint: x in = " + x);
        Log.d(TAG, "findRotatedPoint: y in = " + y);

        double toRotate = -(Math.toRadians(rotationRequired));

        int[] rotatedPoint = new int[]{
                (int) ((x * (Math.cos(toRotate))) - (y * (Math.sin(toRotate)))),
                (int) ((y * (Math.cos(toRotate))) + (x * (Math.sin(toRotate))))
        };

        Log.d(TAG, "findRotatedPoint: x out = " + rotatedPoint[0]);
        Log.d(TAG, "findRotatedPoint: y out = " + rotatedPoint[1]);

        return rotatedPoint;

    }

    private float[] findRawColour(int findColourX, int findColourY) {

        int colour = bitmap.getPixel(findColourX, findColourY);

        Log.d(TAG, "find colour bitmap width: " + bitmap.getWidth());
        Log.d(TAG, "find colour bitmap height: " + bitmap.getHeight());

        int red = (colour >> 16) & 0xFF;
        int green = (colour >> 8) & 0xFF;
        int blue = colour & 0xFF;

        // setText(red, green, blue);

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

        return offset;

    }

    private Bitmap rotateImage(Bitmap bitmap) {

        ExifInterface exifInterface = null;

        try {
            exifInterface = new ExifInterface(imageFilePath);
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

        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, 1000,
                1000, matrix, true);

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(imageFilePath, bmOptions);

        int cameraImageWidth = bmOptions.outWidth;
        int cameraImageHeight = bmOptions.outHeight;
        Log.d("Rotated dimensions", "Width, height: " + cameraImageWidth + ", " + cameraImageHeight);


        return rotatedBitmap;

    }

    public static Bitmap rotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    enum Quarter {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
    }

}
