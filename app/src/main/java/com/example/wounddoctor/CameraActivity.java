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
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaScannerConnection;
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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import util.ImageSaver;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener {

    /**
     * Tag for the debug log.
     */
    private static final String TAG = "CameraActivity";

    private ProgressBar progressBar;

    /**
     * Button to capture an image
     */
    private ImageButton btnCapture;

    /**
     * Button to toggle flash on/off
     */
    private Button btnFlash;

    /**
     * TextureView for camera preview. TextureView is the view which renders captured
     * camera image data
     */
    private TextureView textureView;

    private int actionRequested;

    private String limbName;

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
     * The id of the camera device being used - 0 for back-facing
     */
    private String cameraId;

    /**
     * A reference to the opened {@link android.hardware.camera2.CameraDevice}.
     */
    private CameraDevice cameraDevice;

    /**
     * Reference to the current {@link android.hardware.camera2.CameraCaptureSession} being used.
     * It is a configured capture session for a {@link CameraDevice}, used for capturing
     * images from the camera or reprocessing images captured from the camera in the same
     * session previously.
     */
    private CameraCaptureSession cameraCaptureSessions;

    /**
     * {@link CaptureRequest.Builder} for the camera preview. A CaptureRequest is a package of
     * settings and outputs needed to capture an image from the camera device.
     */
    private CaptureRequest.Builder captureRequestBuilder;

    private Size imageDimension;

    /**
     * An {@link ImageReader} that handles still image capture. The ImageReader
     * class allows direct application access to image data rendered into a Surface
     */
    private ImageReader imageReader;

    /**
     * The {@link android.util.Size} of the camera preview.
     */
    private Size mPreviewSize;

    private static final int REQUEST_CAMERA_PERMISSION = 0;

    /**
     * Whether the current camera device supports Flash or not.
     */
    private boolean mFlashSupported;

    private boolean isTorchOn = false;

    /**
     * A handler for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * The output file for the picture.
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
        try{
            this.getSupportActionBar().hide();
        } catch (NullPointerException e){
        }
        setContentView(R.layout.activity_camera);

        Bundle extra = getIntent().getExtras();
        if (extra != null){
            actionRequested = extra.getInt("action requested");
            limbName = extra.getString("limb name");
            Toast.makeText(this, limbName, Toast.LENGTH_SHORT).show();
        }


        // Check if this device has an existing directory for this app to store images - if not,
        // create one
        createImageFolder();

        textureView = findViewById(R.id.camera_TextureView);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);

        progressBar = findViewById(R.id.camera_ProgressBar);

        btnFlash = findViewById(R.id.camera_FlashButton);
        btnFlash.setOnClickListener(this);

        btnCapture = findViewById(R.id.camera_CaptureButton);
        btnCapture.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.camera_CaptureButton:
                takePicture();
                break;
            case R.id.camera_FlashButton:
                toggleFlash();
                break;
        }
    }

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its status.
     * Used to check the camera device state and is required to open a camera
     */
    CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            // Set cameraDevice to the camera that has been opened
            cameraDevice = camera;
            // This method is called when the camera is opened.  We start camera preview here.
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
     *
     */
    private void takePicture() {
        // Check if there is an available camera on the device
        if (cameraDevice == null)
            return;

        // Camera Manager is a class that controls camera operations. It can be used to
        // access the camera device characteristics
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {

            // CameraCharacteristics are are the properties describing the current camera device
            // Create an object here to find the cameraId of the back-facing camera
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());

            Size[] jpegSizes = null;
            if (characteristics != null)
                jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);

            // Capture image with custom size
            int width = 640;
            int height = 480;
            if (jpegSizes != null && jpegSizes.length > 0) {
                width = jpegSizes[0].getWidth();
                height = jpegSizes[0].getHeight();
            }

            final ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurface = new ArrayList<>(2);
            outputSurface.add(reader.getSurface());
            outputSurface.add(new Surface(textureView.getSurfaceTexture()));

            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
            // captureBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CameraMetadata.NOISE_REDUCTION_MODE_FAST);
            // captureBuilder.set(CaptureRequest.EDGE_MODE, CameraMetadata.EDGE_MODE_FAST);
            // captureBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            if (isTorchOn) {
                captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                captureBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_SINGLE);
            }
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            // captureBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START);

            // Check orientation base on device
            final int rotation = getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            try {
                file = createImageFileName();
            } catch (IOException e) {
                e.printStackTrace();
            }

            /**
             * This a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
             * still image is ready to be saved. Returns an image when CameraCaptureSession completes capture.
             */
            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader imageReader) {

                    ImageSaver imageSaver = new ImageSaver(reader.acquireNextImage(), file);
                    mBackgroundHandler.post(imageSaver);

                }

                /**
                 * Saves a JPEG {@link Image} into the specified {@link File}.
                 */
//                private void save(byte[] bytes) throws IOException {
//                    OutputStream outputStream = null;
//                    try {
//                        outputStream = new FileOutputStream(file);
//                        outputStream.write(bytes);
//                    } finally {
//                        if (outputStream != null)
//                            outputStream.close();
//
//                    }
//                }
            };

            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);

            /**
             * A {@link CameraCaptureSession.CaptureCallback} that handles events related to JPEG
             * capture. This callback manages captured sessions.
             */
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);

                    showProgressBar();

                    // Need to refresh gallery after adding image in gallery.
                    // Tell the media scanner about the new file so that it is
                    // immediately available to the user in the phone's gallery app.
                    MediaScannerConnection.scanFile(getApplicationContext(),
                            new String[]{file.getPath()},
                            new String[]{"image/jpeg"},
                            null);

                    if (actionRequested == 1){
                        Intent sendPictureToProcess = new Intent(CameraActivity.this,
                                ProcessImageActivity.class)
                                .putExtra("image captured", mImageFileName)
                                .putExtra("action requested", actionRequested);
                        startActivity(sendPictureToProcess);
                    } else if (actionRequested == 2){
                        Intent registerWound = new Intent(CameraActivity.this,
                                ProcessImageActivity.class)
                                .putExtra("image captured", mImageFileName)
                                .putExtra("limb name", limbName)
                                .putExtra("action requested", actionRequested);
                        startActivity(registerWound);
                    }

                    hideProgressBar();
                }
            };

            cameraDevice.createCaptureSession(outputSurface, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try {
                        cameraCaptureSession.capture(captureBuilder.build(),
                                captureListener,
                                mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, mBackgroundHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void showProgressBar(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // btnCapture.setVisibility(View.INVISIBLE);
                // btnFlash.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                // setContentView(R.layout.activity_process_image);
            }
        });
    }

    private void hideProgressBar(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // btnCapture.setVisibility(View.VISIBLE);
                // btnFlash.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.INVISIBLE);
                // setContentView(R.layout.activity_camera);
            }
        });
    }

    /**
     * Start the camera preview.
     */
    private void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            captureRequestBuilder.addTarget(surface);
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (cameraDevice == null)
                        return;
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.i(TAG, "Changed");
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if (cameraDevice == null)
            Log.i(TAG, "Error updating camera preview");
        // captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        captureRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 10);
        // captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
        // captureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_TWILIGHT);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(),
                    null,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Tries to open a {@link CameraDevice}. The result is listened by `mStateCallback`.
     */
    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = manager.getCameraIdList()[0];
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];

            // Check realtime permission if run higher than API 23
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, REQUEST_CAMERA_PERMISSION);
                return;
            }

            manager.openCamera(cameraId, stateCallback, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            // Set up camera when texture view available
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "You can't use camera without permission", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    /**
     *
     */
    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (textureView.isAvailable())
            openCamera();
        else
            textureView.setSurfaceTextureListener(textureListener);
    }

    /**
     * Method determines what happens if the app is paused. Most importantly it frees the camera to
     * be used by other devices and stop the background thread.
     */
    @Override
    protected void onPause() {
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Closes the current {@link CameraDevice}.
     * <p>
     * If the application does not properly release
     * the camera, all subsequent attempts to access the camera, including those by this
     * application, will fail and may cause other applications to be shut down.
     */
    private void closeCamera() {
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    private void toggleFlash() {
//        if (isTorchOn) {
//            btnFlash.setImageResource(R.drawable.flash);
//            isTorchOn = false;
//        } else {
//            btnFlash.setImageResource(R.drawable.flash);
//            isTorchOn = true;
//        }
        if (isTorchOn){
            btnFlash.setText("Flash Off");
            isTorchOn = false;
        } else {
            btnFlash.setText("Flash On");
            isTorchOn = true;
        }
    }

    /**
     * Create a directory to store the images taken with this app.
     */
    private void createImageFolder() {
        // The getExternalStoragePublicDirectory method was deprecated in API level 29,
        // but works fine here
        File imageFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        // Create the folder as a file called Camera2 API App
        mImageFolder = new File(imageFile, "Wound Doctor Images");

        // If the directory does not already exist, create it here
        if (!mImageFolder.exists()) {
            mImageFolder.mkdirs();
        }
    }

    /**
     * Create a file name for the image to be saved. First creates a timestamp of when the
     * image was captured.
     *
     * @return
     * @throws IOException
     */
    private File createImageFileName() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        Log.i(TAG, timestamp);
        String prepend = "JPEG_" + timestamp + "_";
        File imageFile = File.createTempFile(prepend, ".jpg", mImageFolder);
        mImageFileName = imageFile.getAbsolutePath();
        Log.i(TAG, mImageFileName);
        return imageFile;
    }

}
