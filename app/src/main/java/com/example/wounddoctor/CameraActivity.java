package com.example.wounddoctor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;

import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import util.Constants;

/**
 * The camera feature for the application. This application makes use of the camera2 API and as such
 * any device that is running on Android 5.0 or lower will not be able to run the camera feature.
 * Application makes use of the back-facing camera and it's properties to capture a still image
 * that is sent to ProcessImageActivity for processing.
 * <p>
 * If this is the first time the user has opened CameraActivity they will first be asked to grant
 * permissions for the application to access the camera hardware and the external storage directory
 * on their device.
 * <p>
 * The user will use the camera feature during 1 of 3 possible use cases, tracked by a request code
 * passed in from the activity that called CameraActivity:
 * <p>
 * Use case 1: Health Check -- REQUEST_CAMERA_ACTION -- passed in from MainActivity
 * <p>
 * Use case 2: Register Wound -- REQUEST_REGISTER_WOUND -- passed in from LimbListActivity
 * <p>
 * Use case 3: Change Bandage -- REQUEST_UPDATE_BANDAGE -- passed in from MyWoundsActivity
 *
 * @author David Farrelly
 */
public class CameraActivity extends AppCompatActivity implements View.OnClickListener {

    /**
     * Tag for the debug log
     */
    private static final String TAG = "CameraActivity";

    /**
     * TextureView for camera preview to render image data to user
     */
    private TextureView textureView;

    /**
     * The ProgressBar element to display when the capture button is clicked.
     */
    private ProgressBar progressBar;

    /**
     * Button to capture an image.
     */
    private ImageButton btnCapture;

    /**
     * Button to toggle flash on/off.
     */
    private Button btnFlash;

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
     * The name of the limb the patient is registering a wound on.
     * This variable is used during the Register Wound use case.
     * <p>
     * If the action requested by the user is REQUEST_REGISTER_WOUND,
     * the name of the limb will have been passed in as extra data
     * from LimbListActivity when the user selects a limb.
     */
    private String limbName;

    /**
     * The ID of the wound being photographed.
     * This is used during the Change Bandage use case.
     * <p>
     * If the action requested by the user is REQUEST_UPDATE_BANDAGE,
     * the ID of the wound needs to be updated will have been passed in
     * as extra data from MyWoundsActivity when the user clicks
     * 'Change Bandage' on one of their wounds from the list of wounds.
     */
    private String woundId;

    /**
     * Check state orientation of output image
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * The id of the camera device being used - 0 for back-facing camera
     */
    private String cameraId;

    /**
     * A reference to the opened camera device
     */
    private CameraDevice cameraDevice;

    /**
     * Reference to the current CameraCaptureSession being used by the CameraDevice.
     * Used for capturing a still image from the camera or reprocessing images captured from the
     * camera in the same session
     */
    private CameraCaptureSession cameraCaptureSessions;

    /**
     * Reference to a CaptureRequest.Builder for the camera preview. This builder is used in the
     * activity for creating the capture session for the camera preview
     */
    private CaptureRequest.Builder captureRequestBuilder;

    /**
     * A reference to the Size dimensions of the preview image
     */
    private Size imageDimension;

    /**
     * An ImageReader that handles still image capture. The ImageReader
     * class allows direct application access to image data rendered into a Surface
     */
    // private ImageReader imageReader;

    /**
     * The {@link android.util.Size} of the camera preview.
     */
    // private Size mPreviewSize;

    /**
     * A request code used to track if the user has provided permissions for the application to use
     * the camera
     */
    private static final int REQUEST_CAMERA_PERMISSION = 0;

    /**
     * A reference to whether the camera flash is on or off.
     * False - flash is off.
     * True - flash is on.
     */
    private boolean isTorchOn = true;

    /**
     * A handler for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    /**
     * An additional thread for running tasks that shouldn't block the main UI thread.
     */
    private HandlerThread mBackgroundThread;

    /**
     * The output File for the picture.
     */
    private static File file;

    /**
     * The output folder to save an image
     */
    private File mImageFolder;

    /**
     * The file path for the image that is captured
     */
    private String mImageFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hide the toolbar for this activity
        Objects.requireNonNull(this.getSupportActionBar()).hide();
        setContentView(R.layout.activity_camera);
        // Get extra data from activity that called CameraActivity
        Bundle extra = getIntent().getExtras();
        if (extra != null) {
            // Get the action requested by the user
            actionRequested = extra.getInt("action requested");
            // The name of the limb from LimbListActivity during Register Wound use case
            limbName = extra.getString("limb name");
            // The ID of the wound from MyWoundsActivity during Change Bandage use case
            woundId = extra.getString("woundId");
        }
        // Check if this device has an existing directory for this app to store images
        // If not, create one
        createImageFolder();
        // A reference to the TextureView in the xml file
        textureView = findViewById(R.id.camera_TextureView);
        assert textureView != null;
        // Create a SurfaceTextureListener interface to notify the camera when the TextureView
        // is ready to stream image data
        textureView.setSurfaceTextureListener(textureListener);
        // A reference to the ProgressBar in the xml file
        progressBar = findViewById(R.id.camera_ProgressBar);
        // A reference to the flash Button in the xml file
        btnFlash = findViewById(R.id.camera_FlashButton);
        // Listen for click events on the flash button
        btnFlash.setOnClickListener(this);
        // A reference to the capture button in the xml file
        btnCapture = findViewById(R.id.camera_CaptureButton);
        // Listen for click events on the capture button
        btnCapture.setOnClickListener(this);
    }

    /**
     * The SurfaceTextureListener handles several activity lifecycle events on a TextureView.
     * A TextureView allows data from the camera to be viewed as a preview by the user. To use a
     * TextureView for this purpose it is necessary to wait for it to inflate, to get the width
     * and the height of the surface that is to be passed to the camera. For this purpose, this
     * SurfaceTextureListener interface is set up, which implements a number of overloaded methods.
     * The listener can then be used to notify the camera when the associated surface is available
     * to render image data.
     * <p>
     * The key method overridden from the SurfaceTextureListener interface
     * is the onSurfaceTextureAvailable() callback. When the TextureView is available, this method
     * is invoked and provides the dimensions of the TextureView to the openCamera() method,
     * which is used to set up the camera device.
     */
    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            // Set up camera when TextureView is available
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Nothing happens here but method must be overridden to use listener
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            // Nothing happens here but method must be overridden to use listener
        }
    };

    /**
     * The onResume() method is called when the activity starts. Used here to start the background
     * thread and to initialise the TextureView.
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Start the background thread for moving camera functions off of the main UI thread
        startBackgroundThread();
        // When the user moves away the app while CameraActivity is running, the TextureView
        // is already available, and the SurfaceTextureListener wis not needed. In this case,
        // the camera can be opened here. If the CameraActivity is starting for the first time,
        // the TextureView is initialed here using the SurfaceTextureListener
        if (textureView.isAvailable())
            openCamera();
        else
            textureView.setSurfaceTextureListener(textureListener);
    }

    /**
     * Method determines what happens if the app is paused. Most importantly it frees the camera to
     * be used by other devices and stop running the background thread.
     */
    @Override
    protected void onPause() {
        // Close the connected camera device
        closeCamera();
        // Stop running the background thread
        stopBackgroundThread();
        super.onPause();
    }

    /**
     * Method attempts to set up and open the back-facing CameraDevice. The result is
     * listened to by stateCallback.
     */
    private void openCamera() {
        // Create an instance of CameraManger to communicate to available cameras on the device
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            assert manager != null;
            // Store the ID of the back-facing CameraDevice
            cameraId = manager.getCameraIdList()[0];
            // Create an instance of CameraCharacteristics to find the properties of the back-facing
            // CameraDevice
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            // Create an instance of a StreamConfigurationMap from CameraCharacteristics.
            // The StreamConfigurationMap class will have a list of all the sizes of the image
            // output formats that are supported by the back-facing CameraDevice
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            // Set the Size of the image preview dimensions to the dimensions at the first position
            // of the StreamConfigurationMap
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
            // Check runtime permission if device running on OS higher than API 23. If this is the
            // first time the user has opened CameraActivity, they will be required to grant
            // the application permissions to use the camera and access device storage
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, REQUEST_CAMERA_PERMISSION);
                return;
            }
            // Open the back-facing CameraDevice using the ID of the camera and a
            // CameraDevice.StateCallback. Also shift the task onto the background thread.
            manager.openCamera(cameraId, stateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            // Here if there was a problem connecting to the back-facing CameraDevice
            e.printStackTrace();
        }
    }

    /**
     * This method checks if the request granted by the user to use the camera and device storage
     * has been successful
     *
     * @param requestCode  - The request code tracked for requesting permissions from Android system
     * @param permissions  - The array of permissions requested
     * @param grantResults - The result of the request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                // Here if user has not granted permissions
                // User cannot use the camera until they have done so
                Toast.makeText(this,
                        "You can't use the camera without first granting permissions",
                        Toast.LENGTH_SHORT).show();
                // Finish CameraActivity.java
                finish();
            }
        }
    }

    /**
     * This callback is used to check the state of the CameraDevice and is required to open a camera.
     * The method onOpened is called from openCamera() when the camera has been set up.
     * The methods onDisconnected and onError are used to close the camera and release any
     * camera resources if an error is encountered. Possible errors include the camera device
     * being disconnected or the app crashing.
     */
    CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            // Set cameraDevice variable to the back-facing camera that has been opened
            cameraDevice = camera;
            // This method is called after the camera is opened. The camera preview is started here.
            createCameraPreview();
        }
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            // Close the camera device if it is disconnected
            cameraDevice.close();
        }
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            // Close the camera device if there is an error
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    /**
     * Start the camera preview to be displayed to the user on the TextureView. Method is called
     * after the CameraDevice.StateCallback has detected that a CameraDevice has been opened.
     */
    private void createCameraPreview() {
        try {
            // SurfaceTexture object is set up using the TextureView that has already been initialised
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            // Configure the default buffer size of the SurfaceTexture dimensions using the
            // Size of the preview image dimensions from the StreamConfigurationMap
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            // Create a Surface object and pass the SurfaceTexture created in.
            // This Surface can be used as an output destination for the CameraDevice
            Surface surface = new Surface(texture);
            // Hand the created Surface over to the CameraDevice to create a preview display by
            // means of a CaptureRequest. The CameraDevice can be used to call the method
            // createCaptureRequest(), which will create the RequestBuilder by passing in a template
            // for the target type of the desired capture request (CameraDevice.TEMPLATE_PREVIEW).
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // Add the Surface as a target to the RequestBuilder
            captureRequestBuilder.addTarget(surface);
            // Create a CaptureSession via the CameraDevice by calling createCaptureSession()
            // Pass in the target output Surface along with a new CameraCaptureSession.StateCallback
            // The StateCallback listens for updates concerning the state of the camera capture
            // session that has been requested. Lastly move the CaptureSession onto the background thread
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    // Check if CameraDevice still connected, else leave method to set it up
                    if (cameraDevice == null)
                        return;
                    // The CameraCaptureSession that is returned by the CameraDevice, once
                    // configured, can be used to instantiate the CameraCaptureSession for
                    // the preview session.
                    cameraCaptureSessions = cameraCaptureSession;
                    // Method is called every time the preview capture session is fired.
                    // This will be when the user moves the camera
                    updatePreview();
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    // Here if capture session could not be configured
                    Log.i(TAG, "Changed configuration on preview request");
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            // Here if connection to back-facing camera has been lost
            e.printStackTrace();
        }
    }

    /**
     * This method is called every time the CaptureSession set up for the camera preview is fired.
     * The code in this method allows control over the back-facing CameraDevice properties.
     */
    private void updatePreview() {
        // Check that CameraDevice is still connected
        if (cameraDevice == null)
            Log.i(TAG, "Camera has been closed on preview update");
        // CONTROL_MODE determines how the camera will auto-focus as the user moves the device
        // CONTROL_AF_MODE_CONTINUOUS_PICTURE allows for the preview image to be continually re-focused
        // by modifying the camera lens position
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        // Control the auto-exposure level so that the user can see the preview even in low
        // lighting. This essentially increases the brightness, but will not affect the picture quality
        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 5);
        try {
            // Repeat the preview CaptureRequest continually in order to provide a seamless camera preview.
            // Put this repeating request on the background thread
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            // Here if connection lost to CameraDevice
            e.printStackTrace();
        }
    }

    /**
     * This method is used to listen for button click events.
     * There are two buttons that can be clicked in this activity, the capture button
     * and the flash button.
     *
     * @param v - The Button that has been clicked
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.camera_CaptureButton:
                // Capture button clicked -- capture a still image
                takePicture();
                break;
            case R.id.camera_FlashButton:
                // Toggle flash on/off
                toggleFlash();
                break;
        }
    }

    /**
     * Method is called when the capture button is clicked and is used to capture a still image.
     */
    private void takePicture() {
        // Check if the CameraDevice is still connected. If not, leave this method to set up camera
        if (cameraDevice == null)
            return;
        // Create an instance of CameraManager to access CameraCharacteristics
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            assert manager != null;
            // Access CameraCharacteristics of the back-facing CameraDevice
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());
            // Create a Size array to hold the available image sizes for a still capture
            Size[] jpegSizes;
            // Get all the image sizes available to the back-facing camera
            jpegSizes = Objects.requireNonNull(
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ).getOutputSizes(ImageFormat.JPEG);
            // Set up some custom image dimensions to use in case the Size array is empty
            int width = 640;
            int height = 480;
            // Check the Size array to make sure it is not empty
            if (jpegSizes != null && jpegSizes.length > 0) {
                // The image dimensions at position 0 of the Size array are the best to use, as
                // they are the largest. This will give the highest possible image quality
                // Set the width of the image to capture
                width = jpegSizes[0].getWidth();
                // Set the height of the image to capture
                height = jpegSizes[0].getHeight();
            }
            // Set up an ImageReader to retrieve image data from the camera, using the indicated
            // dimensions of the image. Also indicated the format that the image should be saved as
            final ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurface = new ArrayList<>(2);
            outputSurface.add(reader.getSurface());
            outputSurface.add(new Surface(textureView.getSurfaceTexture()));

            // Set up a new CaptureRequest.Builder to create the CameraCaptureSession for still image capture
            // The builder is set up with TEMPLATE_STILL_CAPTURE
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            // Add the surface data to be captured from the ImageReader
            captureBuilder.addTarget(reader.getSurface());
            // Set the auto-focus to trigger of the capture session
            captureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
            // Check here if Flash needs to be on or off when capture session fires
            if (isTorchOn) {
                // Here is Flash needs to be on
                captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                captureBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE);
            }
            // Control the auto-exposure of the capture session that fires
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            // Check the orientation base on the device using getWindowManager
            final int rotation = getWindowManager().getDefaultDisplay().getRotation();
            // Realign the captured image
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            try {
                file = createImageFileName();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // The ImageReader listener that is called when a still image is ready to be saved
            // Passes data from the camera surface to the ImageSave class when an image has been read
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader imageReader) {
                    // Create an instance of ImageSave to pass the image data into
                    // The image data is retrieved from the camera surface using acquireNextImage()
                    ImageSave imageSave = new ImageSave(reader.acquireNextImage());
                    // Put the ImageSave class onto the background thread
                    mBackgroundHandler.post(imageSave);
                }
            };
            // Pass the listener onto the ImageReader
            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);

            /**
             * A CameraCaptureSession.CaptureCallback that handles events related to still JPEG
             * capture. This callback manages captured sessions.
             */
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    // Here if image has been captured successfully
                    super.onCaptureCompleted(session, request, result);
                    // Display the ProgressBar to user
                    showProgressBar();
                    // Check which camera action has been requested by the user. Depending on the result
                    // pass the necessary data into ProcessImageActivity
                    if (actionRequested == Constants.REQUEST_CAMERA_ACTION) {
                        /*
                        REQUEST_CAMERA_ACTION
                        Data to pass in:
                        -- Absolute file path of image captured
                        -- Action requested
                         */
                        Intent sendPictureToProcess = new Intent(CameraActivity.this,
                                ProcessImageActivity.class)
                                .putExtra("image captured", mImageFileName)
                                .putExtra("action requested", actionRequested);
                        startActivity(sendPictureToProcess);
                    } else if (actionRequested == Constants.REQUEST_REGISTER_WOUND) {
                        /*
                        REQUEST_REGISTER_WOUND
                        Data to pass in:
                        -- Absolute file path of image captured
                        -- Name of limb where wound is situated
                        -- Action requested
                         */
                        Intent registerWound = new Intent(CameraActivity.this,
                                ProcessImageActivity.class)
                                .putExtra("image captured", mImageFileName)
                                .putExtra("limb name", limbName)
                                .putExtra("action requested", actionRequested);
                        startActivity(registerWound);
                    } else if (actionRequested == Constants.REQUEST_UPDATE_BANDAGE) {
                        /*
                        REQUEST_UPDATE_BANDAGE
                        Data to pass in:
                        -- Absolute file path of image captured
                        -- The ID of the wound for which the user is changing a bandage
                        -- Action requested
                         */
                        Intent updateWoundIntent = new Intent(CameraActivity.this,
                                ProcessImageActivity.class)
                                .putExtra("image captured", mImageFileName)
                                .putExtra("wound id", woundId)
                                .putExtra("action requested", actionRequested);
                        startActivity(updateWoundIntent);
                    }
                    // Hide the ProgressBar once CameraActivity closes, as the user may need to return
                    // to take another picture if the image they took is of insufficient quality
                    // to read the QR code
                    hideProgressBar();
                }
            };

            cameraDevice.createCaptureSession(outputSurface, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try {
                        cameraCaptureSession.capture(captureBuilder.build(), captureListener,
                                mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        // Here if CameraDevice has lost connection
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.d(TAG, "onConfigureFailed: could not configure session for still capture");
                }
            }, mBackgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to show the ProgressBar when the capture buttojn is clicked.
     * This is required to access the ProgressBar as the element belongs to the main Ui thread
     */
    private void showProgressBar() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Display the ProgressBar element
                progressBar.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * Method to hide the ProgressBar when the CameraActivity is finished.
     * This is required to access the ProgressBar as the element belongs to the main Ui thread
     */
    private void hideProgressBar() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Hide the ProgressBar element
                progressBar.setVisibility(View.INVISIBLE);
            }
        });
    }

    /**
     * This class is used to save an image that is passed to it from an ImageReader. It implements
     * Runnable so that when it is called it can be put on the background thread.
     */
    private static class ImageSave implements Runnable {
        // The image that is passed to this class from the ImageReader that calls it
        private final Image mImage;
        // Constructor for ImageSave
        public ImageSave(Image image) {
            mImage = image;
        }
        @Override
        public void run() {
            // Create a byte buffer that contains the image data returned from the camera surface
            ByteBuffer byteBuffer = mImage.getPlanes()[0].getBuffer();
            // Set up an array of bytes to pass the data into
            byte[] bytes = new byte[byteBuffer.remaining()];
            // Copy the data from the byte buffer into the byte array
            byteBuffer.get(bytes);
            // Create an OutputStream to pass data into a File
            OutputStream fileOutputStream = null;
            try {
                // Set the target File of the data
                fileOutputStream = new FileOutputStream(file);
                // Write the data to the File from the byte array
                fileOutputStream.write(bytes);
                // Close the image that the data was read from to free up resources
                mImage.close();
            } catch (IOException e) {
                // Here if problem writing data to File
                e.printStackTrace();
            } finally {
                if (fileOutputStream != null) {
                    try {
                        // Close the OutputStream to free up resources
                        fileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Stops the background HandlerThread and its corresponding Handler.
     */
    private void stopBackgroundThread() {
        // Close the HandlerThread
        mBackgroundThread.quitSafely();
        try {
            // Block the current thread that is running (UI thread) until HandlerThread is properly closed
            mBackgroundThread.join();
            // Set HandlerThread and Handler to null
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            // Here if thread was interrupted before it could be finished
            e.printStackTrace();
        }
    }

    /**
     * Starts a background HandlerThread and its corresponding Handler for shifting time
     * consuming tasks off the main UI thread. Method ios called in onResume() to start the thread
     * when the activity opens
     */
    private void startBackgroundThread() {
        // Create a new HandlerThread and set a name
        mBackgroundThread = new HandlerThread("Camera Background");
        // Start the HandlerThread
        mBackgroundThread.start();
        // Create a new Handler to pass the looper from the HandlerThread into the constructor to
        // keep the HandlerThread running over the lifecycle of the activity
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Closes the connection to the back-facing CameraDevice.
     * <p>
     * The application needs to properly release the camera, otherwise future attempts to
     * access the CameraDevice will fail and may cause other applications to close.
     */
    private void closeCamera() {
        // First check that CameraDevice is still connected
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    /**
     * Method to toggle flash capabilities on/off by changing the boolean isTorchOn.
     * This method is called when the user clicks the Flash Button
     */
    private void toggleFlash() {
        // First check if Flash is already on
        if (isTorchOn) {
            // Change Button text to off and set boolean to false
            btnFlash.setText(R.string.main_flash_off_text);
            isTorchOn = false;
        } else {
            // Change Button text to On and set boolean to true
            btnFlash.setText(R.string.main_flash_on_text);
            isTorchOn = true;
        }
    }

    /**
     * Create a directory to store the images taken with this app. All image Files saved using the app will
     * be stored in this directory. This method is called when CameraActivity starts, to check if
     * the folder has ben created previously. If this is the first time CameraActivity has been
     * loaded, the directory is created on the device.
     */
    private void createImageFolder() {
        // The getExternalStoragePublicDirectory method was deprecated in API level 29,
        // but works fine here to access the external storage directory of the device
        File imageFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        // Create the folder as a file called Camera2 API App
        mImageFolder = new File(imageFile, "Wound Doctor Images");
        // If the directory does not already exist, create it here
        if (!mImageFolder.exists()) {
            mImageFolder.mkdirs();
        }
    }

    /**
     * Create a file name for the image that is to be saved. First creates a new timestamp of when the
     * image was captured.
     *
     * @return File - the File object holding the saved image
     * @throws IOException
     */
    private File createImageFileName() throws IOException {
        // Create a timestamp of the current time
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        // Create prepend of file name using timestamp
        String prepend = "JPEG_" + timestamp + "_";
        // Create the full file name by adding .jpg suffix and add the destination directory
        File imageFile = File.createTempFile(prepend, ".jpg", mImageFolder);
        // Get the absolute file path of where the File is stored and set as String
        mImageFileName = imageFile.getAbsolutePath();
        // Return the File
        return imageFile;
    }

}
