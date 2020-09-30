package com.example.wounddoctor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.ColorUtils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import model.PatientImage;
import model.Wound;
import util.AlertReceiver;
import util.PatientManager;

/**
 * This class takes care of the feedback to be calculated after a patient has undergone a health
 * check for a wound. The class is started from ProcessImageActivity.java after a patients image has been
 * processed under REQUEST_CAMERA_ACTION. The Rgb data calculated during ProcessImageActivity.java
 * is passed into this class along with the image file path and the ID of the bandage imaged.
 * <p></p>
 * There are 4 steps to the patient feedback process:
 * <br>
 * Step 1: Calculate the change in the wounds condition data since the last health check. This requires
 * that the previous Rgb colour data calculated for the wound be retrieved from Firestore and compared
 * to the new Rgb colour data.
 * <br>
 * Step 2: Post the latest wound feedback data to Firestore. This includes the new Rgb data measured
 * as well as the change calculated in the wounds condition.
 * <br>
 * Step 3: Schedule the date and time of the next health update for this wound. The date and time will
 * be based on the change in the wounds condition that is found. If the wound has changed significantly
 * the next health check will be set at more regular intervals.
 * <br>
 * Step 4: Display the feedback as a diagnosis of the wound to the user. This includes visual & audio feedback
 * that can be in 1 of 3 forms, depending on the change in the wounds condition.
 *
 * @author David Farrelly
 */
public class PatientFeedbackActivity extends AppCompatActivity {

    /**
     * Debug tag for this activity
     */
    private static final String TAG = "PatientFeedbackActivity";

    /**
     * The ProgressBar element that is displayed as the feedback is being calculated
     */
    private ProgressBar progressBar;

    /**
     * The loading text that is displayed below the ProgressBar. This will change depending
     * on which stage of the feedback process is happening
     */
    private TextView loadingTextView;

    /**
     * The ID of the patient currently signed in
     */
    private String currentPatientId;

    /**
     * The name of the patient currently signed in
     */
    private String currentPatientName;


    // private FirebaseAuth firebaseAuth;
    // private FirebaseAuth.AuthStateListener authStateListener;
    // private FirebaseUser currentUser;

    /**
     * Making a connection to Firebase Firestore
     */
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * A reference to Firebase Cloud Storage for uploading the patients image
     */
    private StorageReference storageReference;

    /**
     * Creating a reference to the feedback collection in Firestore
     */
    private CollectionReference feedbackReference = db.collection("feedback");

    /**
     * Creating a reference to the wounds collection in Firestore
     */
    private CollectionReference woundReference = db.collection("wounds");

    /**
     * The absolute file path of the patients image on the device. Passed in as extra data from
     * ProcessImageActivity.java
     */
    private String imageFilePath;

    /**
     * The ID of the wound that was imaged. This is wound that will tha patient underwent a health
     * check on
     */
    private String woundId;

    /**
     * The ID of the bandage that the patient imaged. This ID is passed in from ProcessImageActivity.java
     * and is needed for getting the patients wound from the wounds collection in Firestore
     */
    private String bandageId;

    /**
     * Th#he Rgb RED component calculated from the patients image during ProcessImageActivity.java
     */
    private int redValue;

    /**
     * Th#he Rgb GREEN component calculated from the patients image during ProcessImageActivity.java
     */
    private int greenValue;

    /**
     * Th#he Rgb BLUE component calculated from the patients image during ProcessImageActivity.java
     */
    private int blueValue;

    /**
     * The Delta-E value calculated for this wound. This represents the wounds change in condition
     * since the last health check and is measured by comparing the previous Rgb colour data with
     * the new Rgb data
     */
    private double woundChangeSinceLast;

    /**
     * A instance of a WoundHealth enum
     */
    private WoundHealth woundHealth;

    /**
     * An instance of MediaPlayer which is used to play the audio feedback to the user during their
     * diagnosis. The audio will be played from 1 of 3 files depending on the calculated change in
     * the wounds condition. The files can be found in the raw resource folder
     */
    private MediaPlayer mediaPlayer;

    /**
     * The date of the next health check scheduled for this wound. Displayed to the user during
     * their diagnosis
     */
    String nextHealthUpdate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hide the app toolbar for this activity
        Objects.requireNonNull(this.getSupportActionBar()).hide();
        setContentView(R.layout.activity_patient_feedback);
        // Instantiate the reference to Firebase Storage
        storageReference = FirebaseStorage.getInstance().getReference();
        // firebaseAuth = FirebaseAuth.getInstance();

        // Find the ProgressBar element in activity_patient_feedback.xml
        progressBar = findViewById(R.id.postImage_ProgressBar);
        // Find the loading TextView element in activity_patient_feedback.xml
        loadingTextView = findViewById(R.id.feedback_LoadingTextView);
        if (PatientManager.getInstance() != null) {
            // Get the ID of the patient signed in from PatientManager
            currentPatientId = PatientManager.getInstance().getPatientId();
            // Get the name of the patient signed in from PatientManager
            currentPatientName = PatientManager.getInstance().getPatientName();
        }
//        authStateListener = new FirebaseAuth.AuthStateListener() {
//            @Override
//            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
//                currentUser = firebaseAuth.getCurrentUser();
//            }
//        };
        // Instantiate the MediaPlayer
        mediaPlayer = new MediaPlayer();
        // Get the extra data passed in from ProcessImageActivity.java
        Bundle extra = getIntent().getExtras();
        if (extra != null) {
            // The image file path that is passed in
            imageFilePath = extra.getString("image captured");
            // woundId = extra.getString("woundId");
            // The Rgb RED value that is passed in
            redValue = extra.getInt("redValue");
            // The Rgb GREEN value that is passed in
            greenValue = extra.getInt("greenValue");
            // The Rgb BLUE value that is passed in
            blueValue = extra.getInt("blueValue");
            // The ID of the bandage that is passed in
            bandageId = extra.getString("bandageId");
        }
        // Start the first step in the patient feedback process by calling calculateFeedback
        calculateFeedback();
    }

    /**
     * A key referring to the woundId field of the wounds collection
     */
    private static final String KEY_WOUND_ID = "woundId";

    private static final String KEY_BANDAGE_ID = "bandageId";

    /**
     * This method calculates the new Delta-E value for the wound. Delta-E is a measure of
     * how much the wounds condition has changed since the last health check. First the previous
     * Rgb colour data taken during the last health check must be retrieved. This is then compared
     * to the new Rgb colour data passed in from ProcessImageActivity.java in order to calculate
     * the latest Delta-E value
     */
    private void calculateFeedback() {
        // Cancel the previous alarm that had been scheduled to stop it from being sent again
        cancelAlarm();
        // Display the progress bar while making calls to Firebase
        progressBar.setVisibility(View.VISIBLE);
        // Display the first loading text to the user
        loadingTextView.setText(R.string.feedback_diagnosing);

        // Create a hash map that will store the woundId to be added to the feedback collection
        // final Map<String, Object> updateData = new HashMap<>();

        // Call the reference to the wound collection and find the wound document which has a bandageId
        // equal to the bandageId passed in from ProcessImageActivity.java
        woundReference
                .whereEqualTo("bandageId", bandageId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    /**
                     * Method is called if the wounds collection was queried successfully
                     * @param queryDocumentSnapshots - the documents that have been returned
                     */
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        // Check that the query has returned a document from the wounds collection
                        if (!queryDocumentSnapshots.isEmpty()) {
                            // If a document has been returned, loop through the list
                            // There should be only one document returned, as there is only one
                            // wound with a reference to the bandageId that was passed in
                            for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                                // Create a new Wound object to represent the returned wound data
                                Wound wound = snapshot.toObject(Wound.class);
                                // Get the ID of this wound
                                woundId = wound.getWoundId();

                                // Put the ID of the wound into the map for later
                                // updateData.put(KEY_WOUND_ID, woundId);

                                // Call the reference to the feedback collection and search for all
                                // the feedback documents that relate to the ID of the wound
                                feedbackReference
                                        .whereEqualTo("woundId", woundId)
                                        .get()
                                        .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                            /**
                                             * Method is called if the feedback collection was queried successfully
                                             *
                                             * @param queryDocumentSnapshots - the documents that have been returned
                                             */
                                            @Override
                                            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                                // Check that the query has not returned an empty list
                                                if (!queryDocumentSnapshots.isEmpty()) {
                                                    // Will need to find the previous feedback posted for this wound
                                                    // to get the previous Rgb colour data
                                                    // To do this, will need to find the feedback document that has the
                                                    // largest Firebase Timestamp in it i.e. the date of the most recent feedback
                                                    long largestTimestamp = 0;
                                                    // Loop through all the feedback documents pertaining to this wound
                                                    for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                                                        // Create a new PatientImage object to represent the returned feedback data
                                                        PatientImage previousImage = snapshot.toObject(PatientImage.class);
                                                        // Check if the Timestamp date when this feedback was posted is the largest
                                                        if (previousImage.getDate().getSeconds() > largestTimestamp) {
                                                            // If it is the largest, set as the new largestTimestamp
                                                            largestTimestamp = previousImage.getDate().getSeconds();
                                                        }
                                                    }
                                                    // Loop through all the feedback documents pertaining to this wound nonce more,
                                                    // to find the feedback document that has the largest timestamp
                                                    for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                                                        // Create a new PatientImage object to represent the returned feedback data
                                                        PatientImage previousImage = snapshot.toObject(PatientImage.class);
                                                        // If this document == largestTimestamp this is the previous feedback
                                                        if (previousImage.getDate().getSeconds() == largestTimestamp) {
                                                            // Get the previous Rgb RED colour data from this feedback document
                                                            int previousRedFeedback = previousImage.getRed();
                                                            // Get the previous Rgb GREEN colour data from this feedback document
                                                            int previousGreenFeedback = previousImage.getGreen();
                                                            // Get the previous Rgb BLUE colour data from this feedback document
                                                            int previousBlueFeedback = previousImage.getBlue();
                                                            // Calculate the Delta-E value by passing in the previous
                                                            // Rgb colour data to calculateWoundChange
                                                            woundChangeSinceLast = calculateWoundChange(previousRedFeedback,
                                                                    previousGreenFeedback, previousBlueFeedback);
                                                            // Change loading text that is displayed for step 2
                                                            loadingTextView.setText(R.string.feedback_posting);
                                                            // Post the new wound feedback data by calling postPatientFeedback
                                                            postPatientFeedback();
                                                        }
                                                    }
                                                } else {
                                                    // First time processing a patients wound, so no wound change to calculate
                                                    // Set change to 0 and post this wounds first colour data
                                                    woundChangeSinceLast = 0;
                                                    postPatientFeedback();
                                                }
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                // Here if the feedback collection could not be queried
                                                Log.d(TAG, "onFailure: " + e.toString());
                                            }
                                        });
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Here if the wounds collection could not be queried
                        Log.d(TAG, "onFailure: " + e.toString());
                    }
                });
    }

    /**
     * Method is called to compare the previous Rgb colour retrieved from Firestore to the new
     * feedback data measured in ProcessImageActivity.java. This method itself calls
     * calculateEuclideanDistance, which calculates the Delta-E value for the wound based on the
     * change in the Rgb colour data. Before passing to calculateEuclideanDistance, the previous
     * and the new Rgb colours are converted to the LAB colour space.
     *
     * @param previousRedFeedback   - the previous Rgb RED component value
     * @param previousGreenFeedback - the previous Rgb GREEN component value
     * @param previousBlueFeedback  - the previous Rgb BLUE component value
     * @return - the Delta-E value to return
     */
    private double calculateWoundChange(int previousRedFeedback, int previousGreenFeedback,
                                        int previousBlueFeedback) {
        // A double array to store the previous LAB colour data
        double[] previousRGBtoLAB = new double[3];
        // Convert the previous Rgb colour data to the lAB colour space
        ColorUtils.RGBToLAB(previousRedFeedback, previousGreenFeedback, previousBlueFeedback, previousRGBtoLAB);
        // A double array to store the new LAB colour data
        double[] newRGBToLAB = new double[3];
        // Convert the new Rgb colour data to the lAB colour space
        ColorUtils.RGBToLAB(redValue, greenValue, blueValue, newRGBToLAB);
        // Return the Delta-E value calculated by passing the LAB colours to calculateEuclideanDistance
        return calculateEuclideanDistance(previousRGBtoLAB, newRGBToLAB);
    }

    /**
     * This method calculates the Delta-E change in a wounds condition by comparing the
     * previous LAB colour data returned from Firestore to the new LAB colour data measured during
     * the current health check
     *
     * Reference to the code that was used to calculate Delta-E from two LAB colours:
     * https://stackoverflow.com/questions/45821450/calculating-distance-between-two-lab-colors-java
     *
     * @param previousLab - the previous LAB colour data
     * @param newLab - the new LAB colour data
     * @return - the Delta-E value to return
     */
    private double calculateEuclideanDistance(double[] previousLab, double[] newLab) {
        // Measure the difference in the lightness between the two colours
        double L = previousLab[0] - newLab[0];
        // Measure the difference from green to red between the two colours
        double A = previousLab[1] - newLab[1];
        // Measure the difference from blue to yellow between the two colours
        double B = previousLab[2] - newLab[2];
        // Return the Delta-E value based on the following formula
        return Math.sqrt((L * L) + (A * A) + (B * B));
    }

    /**
     * Method is used to post the new wound feedback data to the wounds collection of Firestore.
     * The data to post includes the new Rgb colours measured from ProcessImageActivity.java,
     * the Delta-E change in the wounds condition, the image file path in Firebase Storage, the date
     * that the feedback data was posted on, the ID of the bandage that was imaged and the ID of
     * the patient posting the feedback data.
     */
    private void postPatientFeedback() {
        // Create a path name for uploading the patients image to Firebase Storage
        // The path name is made up as 'patient_images(patientId)_image_(current Timestamp)'
        final StorageReference storagePath = storageReference
                .child("patient_images")
                .child(currentPatientId + "_image_"
                        + Timestamp.now().getSeconds());
        // Create a Uri from the absolute image path passed in from ProcessImageActivity.java
        Uri imageUri = Uri.fromFile(new File(imageFilePath));
        // Upload the image Uri to Firebase Storage
        storagePath.putFile(imageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    /**
                     * Method is called if the image upload was successful
                     * @param taskSnapshot - the task that is returned
                     */
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get the path of the image in Firebase Storage
                        storagePath.getDownloadUrl()
                                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        // Create a String from the image Uri file path that is
                                        // returned from Firebase Storage
                                        String imageUrl = uri.toString();
                                        // Create a new PatientImage object to represent the feedback data
                                        PatientImage patientImage = new PatientImage(
                                                currentPatientId,
                                                woundId,
                                                redValue,
                                                greenValue,
                                                blueValue,
                                                imageUrl,
                                                bandageId,
                                                new Timestamp(new Date()),
                                                woundChangeSinceLast);
                                        // Call the reference to the feedback collection and add the new PatientImage
                                        feedbackReference
                                                .add(patientImage)
                                                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                    @Override
                                                    public void onSuccess(DocumentReference documentReference) {
                                                        // Feedback posted -- move to step 3 of patient feedback process
                                                        scheduleNextUpdate();
                                                        // Update loading text displayed
                                                        loadingTextView.setText(R.string.feedback_scheduling);
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
                                                        // Here if the feedback reference couldn't be queried
                                                        Log.d(TAG, "onFailure: " + e.toString());
                                                    }
                                                });
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressBar.setVisibility(View.INVISIBLE);
                        Log.d(TAG, "onFailure: " + e.toString());
                    }
                });
    }

    /**
     * A key referencing the nextHealthCheck field in the wounds collection
     */
    private static final String KEY_NEXT_UPDATE = "nextHealthCheck";

    /**
     * A key referencing the woundInDanger field in the wounds collection
     */
    private static final String KEY_WOUND_DANGER = "woundInDanger";

    /**
     * A key referencing the hoursUntilCheck field in the wounds collection
     */
    private static final String KEY_HOURS_UNTIL_CHECK = "hoursUntilCheck";

    /**
     * This method is used to do two things. The first step is to update the Firestore document of
     * the wound that has undergone a health check with:
     * <br>
     * 1: the date and time of the next health check.
     * <br>
     * 2: whether or not the wound is in danger. This is based on the Delta-E value calculated for
     * the wound. If Delta-E is found to be > 25, the wound is in danger and this is indicated
     * in the wound document/
     * <p></p>
     * The second step in this method is to schedule a notification to send to the patients device
     * when it is time for the next health check. This done by creating an AlarmManger Intent
     */
    private void scheduleNextUpdate() {
        // progressBar.setVisibility(View.VISIBLE);
        // Create a hash map that will store the update data to be added to the wounds document
        final Map<String, Object> updateData = new HashMap<>();
        // Call the reference to the wound collection and get the wound document that pertains to
        // bandage ID passed in from ProcessImageActivity.java
        woundReference
                .whereEqualTo("bandageId", bandageId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    /**
                     * Method is called if the wound collection has been queried successfully
                     *
                     * @param queryDocumentSnapshots - the documents that have been returned
                     */
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        // Check that the list of documents returned is not empty
                        if (!queryDocumentSnapshots.isEmpty()) {
                            // If a document has been returned, loop through the list
                            // There should be only one document returned, as there is only one
                            // wound with a reference to the bandageId that was passed in
                            for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                                // Next, get the Delta-E value calculated earlier for this wound
                                // First, evaluate Delta-E to determine if the wound is in danger
                                // Second, set the number of hours until the next health check
                                // Last, set the WoundHealth enum for the diagnosis based on Delta-E
                                if (woundChangeSinceLast <= 10) {
                                    // Wound NOT in danger - place false in update data
                                    updateData.put(KEY_WOUND_DANGER, false);
                                    // Wound health OK so 24 hours until next health check
                                    updateData.put(KEY_HOURS_UNTIL_CHECK, 24);
                                    // Set WoundHealth enum for providing diagnosis later
                                    woundHealth = WoundHealth.OK;
                                } else if (woundChangeSinceLast > 10
                                        && woundChangeSinceLast <= 25) {
                                    // Wound NOT in danger - place false in update data
                                    updateData.put(KEY_WOUND_DANGER, false);
                                    // Wound health WARNING so 18 hours until next health check
                                    updateData.put(KEY_HOURS_UNTIL_CHECK, 18);
                                    // Set WoundHealth enum for providing diagnosis later
                                    woundHealth = WoundHealth.WARNING;
                                } else if (woundChangeSinceLast > 25) {
                                    // Wound IN DANGER - place true in update data
                                    updateData.put(KEY_WOUND_DANGER, true);
                                    // Wound health WARNING so 12 hours until next health check
                                    updateData.put(KEY_HOURS_UNTIL_CHECK, 12);
                                    // Set WoundHealth enum for providing diagnosis later
                                    woundHealth = WoundHealth.DANGER;
                                }
                                // Create an instance of a Wound to retrieve and represent the wound data
                                Wound wound = snapshot.toObject(Wound.class);

                                /*
                                The following code is used to work out when the next health check
                                should be scheduled for this wound.
                                 */

                                // First, create an instance of Calendar using the current date and time
                                Calendar nextUpdate = Calendar.getInstance();
                                // Convert the Calendar date/time to milliseconds as required by Firestore
                                // in order to store a Timestamp
                                long timeInMillis = nextUpdate.getTimeInMillis();
                                // Create a new Date object. Get the number of hours until the next health check, convert
                                // to milliseconds and add this to the current time in milliseconds
                                Date timeToAddToCollection = new Date(timeInMillis + (wound.getHoursUntilCheck() * 3600000));
                                // Create a new Firebase Timestamp from the Date object above
                                Timestamp timestamp = new Timestamp(timeToAddToCollection);
                                // Put this Timestamp in the HashMap to update the wounds document
                                // with the date of the next health check
                                updateData.put(KEY_NEXT_UPDATE, timestamp);

                                /*
                                Next, using the date of the next health check calculated to schedule
                                a notification for the patient
                                 */
                                // Set the TextView element that displays the date of the next health update
                                // to the patient
                                nextHealthUpdate = new SimpleDateFormat("EEE, d MMM 'at' HH:mm", Locale.UK).format(timeToAddToCollection);
                                // Call the calendar object again and set as it as a new Date object
                                nextUpdate.setTime(new Date());
                                // Add the number of hours to set from the current time
                                nextUpdate.add(Calendar.HOUR, wound.getHoursUntilCheck());
                                // Schedule the notification
                                startAlarm(nextUpdate);
                            }
                        }
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    /**
                     * If the wound document has been returned successfully, the data stored in the HashMap
                     * above can be used to update the wounds document here
                     *
                     * @param task - the task that has been completed
                     */
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                // Call the reference to the wound collection and update the document
                                // with the data stored in the HashMap
                                woundReference
                                        .document(document.getId())
                                        .update(updateData)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                // Hide the ProgressBar
                                                progressBar.setVisibility(View.INVISIBLE);
                                                // Hide the loading text
                                                loadingTextView.setVisibility(View.INVISIBLE);
                                                // Start step 4 of the feedback -- displayFeedback()
                                                displayFeedback();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                // Here if could not add update data to wound document
                                                Log.d(TAG, "onFailure: " + e.getMessage());
                                            }
                                        });
                            }

                        }
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
     * Method is used to schedule a notification to send to the patient when it is time for their
     * wounds next health check. Uses the AlarmManager class to create a broadcast to the system
     * at a certain time in the future.
     *
     * @param cal - the Calendar instance of the next health check
     */
    private void startAlarm(Calendar cal) {
        // Get an instance of AlarmManager from the system to access alarm services
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        // Create the Intent to the AlertReceiver class
        Intent intent = new Intent(this, AlertReceiver.class);
        // Create the PendingIntent to broadcast to AlertReceiver. Pass in the context of the activity,
        // a request code to track the broadcast and the Intent to the AlertReceiver class
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, intent, 0);
        assert alarmManager != null;
        // Set the time to send the notification to the user
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
    }

    /**
     * Method is used to cancel a notification that has been scheduled using AlarmManager.
     */
    private void cancelAlarm() {
        // Get an instance of AlarmManager from the system to access alarm services
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        // Create the Intent to the AlertReceiver class
        Intent intent = new Intent(this, AlertReceiver.class);
        // Create the PendingIntent to broadcast to AlertReceiver. Importantly, pass in the request code
        // of the broadcast that is to be cancelled
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, intent, 0);
        assert alarmManager != null;
        // Cancel the alarm using the PendingIntent
        alarmManager.cancel(pendingIntent);
    }

    private void displayFeedback() {
        switch (woundHealth) {
            case OK:
                setContentView(R.layout.health_ok_layout);
                TextView feedBackOKDateTextView = findViewById(R.id.feedbackOK_ShowDateTextView);
                feedBackOKDateTextView.setText(nextHealthUpdate);
                TextView feedBackOKTitleTextView = findViewById(R.id.feedbackOK_TitleTextView);
                feedBackOKTitleTextView.setText(currentPatientName + "s Diagnosis");
                mediaPlayer = MediaPlayer.create(this, R.raw.positive_feedback);
                playAudioFeedback();
                break;
            case WARNING:
                setContentView(R.layout.health_warning_layout);
                TextView feedBackWarningDateTextView = findViewById(R.id.feedbackWarning_ShowDateTextView);
                feedBackWarningDateTextView.setText(nextHealthUpdate);
                TextView feedBackWarningTitleTextView = findViewById(R.id.feedbackWarning_TitleTextView);
                feedBackWarningTitleTextView.setText(currentPatientName + "s Diagnosis");
                mediaPlayer = MediaPlayer.create(this, R.raw.warning_feedback);
                playAudioFeedback();
                break;
            case DANGER:
                setContentView(R.layout.health_danger_layout);
                TextView feedBackDangerDateTextView = findViewById(R.id.feedbackDanger_ShowDateTextView);
                feedBackDangerDateTextView.setText(nextHealthUpdate);
                TextView feedBackDangerTitleTextView = findViewById(R.id.feedbackDanger_TitleTextView);
                feedBackDangerTitleTextView.setText(currentPatientName + "s Diagnosis");
                mediaPlayer = MediaPlayer.create(this, R.raw.danger_feedback);
                playAudioFeedback();
                break;
            default:
        }
    }

    private void playAudioFeedback() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
    }

    private enum WoundHealth {
        OK,
        WARNING,
        DANGER
    }

    public void returnToHome(View v) {
        startActivity(new Intent(PatientFeedbackActivity.this,
                MyWoundsActivity.class));
    }

//    @Override
//    protected void onStart() {
//        super.onStart();
//
//        currentUser = firebaseAuth.getCurrentUser();
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
}
