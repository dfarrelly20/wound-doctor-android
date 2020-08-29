package com.example.wounddoctor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import model.PatientImage;
import model.Wound;
import util.AlertReceiver;
import util.PatientManager;

public class PatientFeedbackActivity extends AppCompatActivity {

    private static final String TAG = "PatientFeedbackActivity";

    private Button homeButton;
    private ProgressBar progressBar;
    private TextView loadingTextView;

    // Get the current user from PatientManager
    private String currentPatientId;
    private String currentPatientName;

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser currentUser;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private StorageReference storageReference;
    private CollectionReference feedbackReference = db.collection("feedback");
    private CollectionReference woundReference = db.collection("wounds");

    private String imageFilePath;
    private String woundId;
    private String bandageId;
    private int redValue;
    private int greenValue;
    private int blueValue;
    private double woundChangeSinceLast;
    private WoundHealth woundHealth = WoundHealth.OK;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            this.getSupportActionBar().hide();
        } catch (NullPointerException e) {
        }
        setContentView(R.layout.activity_patient_feedback);

        storageReference = FirebaseStorage.getInstance().getReference();
        firebaseAuth = FirebaseAuth.getInstance();

//        homeButton = findViewById(R.id.postImage_HomeButton);
//        homeButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                startActivity(new Intent(PatientFeedbackActivity.this, MainActivity.class));
//            }
//        });
        progressBar = findViewById(R.id.postImage_ProgressBar);
        loadingTextView = findViewById(R.id.feedback_LoadingTextView);

        if (PatientManager.getInstance() != null) {
            currentPatientId = PatientManager.getInstance().getPatientId();
            currentPatientName = PatientManager.getInstance().getPatientName();
        }

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                currentUser = firebaseAuth.getCurrentUser();
                if (currentUser != null) {
                }
            }
        };

        Bundle extra = getIntent().getExtras();
        if (extra != null) {
            imageFilePath = extra.getString("image captured");
            woundId = extra.getString("woundId");
            redValue = extra.getInt("redValue");
            greenValue = extra.getInt("greenValue");
            blueValue = extra.getInt("blueValue");
            bandageId = extra.getString("bandageId");
        }
        calculateFeedback();
    }

    private static final String KEY_WOUND_ID = "woundId";

    /**
     * This method is used to post the RGB feedback that was processed from the patients image
     * to the feedback collection in the Firestore database.
     * First, the ID of the wound in question needs to be found. To do this the bandage ID of the
     * bandage that was photographed by the user is queried in the wound collection. Once a wound
     * that has the bandage ID has been found, the ID of that wound is stored in a hash map.
     */
    private void calculateFeedback() {
        cancelAlarm();

        progressBar.setVisibility(View.VISIBLE);
        loadingTextView.setText("Loading wound...");

        // Create a hash map that will store the woundId to be added tot the feedback collection
        final Map<String, Object> updateData = new HashMap<>();
        // Call the reference to the wound collection where the document == bandage ID
        woundReference
                .whereEqualTo("bandageId", bandageId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                                // Found the wound that corresponds to the bandage ID
                                Wound wound = snapshot.toObject(Wound.class);
                                // Get the ID of this wound
                                woundId = wound.getWoundId();
                                // Put the ID of the wound into the map for later
                                updateData.put(KEY_WOUND_ID, woundId);

                                feedbackReference
                                        .whereEqualTo("woundId", woundId)
                                        .get()
                                        .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                            @Override
                                            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                                                if (!queryDocumentSnapshots.isEmpty()) {
                                                    long largestTimestamp = 0;
                                                    for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                                                        PatientImage previousImage = snapshot.toObject(PatientImage.class);
                                                        if (previousImage.getDate().getSeconds() > largestTimestamp) {
                                                            largestTimestamp = previousImage.getDate().getSeconds();
                                                        }
                                                    }

                                                    for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                                                        PatientImage previousImage = snapshot.toObject(PatientImage.class);
                                                        if (previousImage.getDate().getSeconds() == largestTimestamp) {
                                                            Log.d(TAG, "found: " + snapshot.getId());
                                                            int previousRedFeedback = previousImage.getRed();
                                                            int previousGreenFeedback = previousImage.getGreen();
                                                            int previousBlueFeedback = previousImage.getBlue();
                                                            Log.d(TAG, "previous: (" + previousRedFeedback + ", " + previousGreenFeedback + ", " + previousBlueFeedback);
                                                            woundChangeSinceLast = calculateWoundChange(previousRedFeedback,
                                                                    previousGreenFeedback, previousBlueFeedback);
                                                            Log.d(TAG, "change" + woundChangeSinceLast);
                                                            loadingTextView.setText("Posting feedback...");
                                                            postPatientFeedback();

                                                        }
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
                        }
                    }
                })
//                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                        if (task.isSuccessful()) {
//                            for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
//                                woundReference
//                                        .document(document.getId())
//                                        .update(updateData)
//                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
//                                            @Override
//                                            public void onSuccess(Void aVoid) {
//
//                                            }
//                                        })
//                                        .addOnFailureListener(new OnFailureListener() {
//                                            @Override
//                                            public void onFailure(@NonNull Exception e) {
//                                                Log.d(TAG, "onFailure: " + e.toString());
//                                            }
//                                        });
//                            }
//                        }
//                    }
//                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "onFailure: " + e.toString());
                    }
                });
    }

    private double calculateWoundChange(int previousRedFeedback, int previousGreenFeedback,
                                        int previousBlueFeedback) {

        double averageThisTime = (double) (redValue + blueValue + greenValue) / 3;
        double averagePreviousTime = (double) (previousRedFeedback + previousGreenFeedback + previousBlueFeedback) / 3;

        return 1 - averageThisTime / averagePreviousTime;
    }

    private void postPatientFeedback() {
        final StorageReference storagePath = storageReference
                .child("patient_images")
                .child(currentPatientId + "_image_" + Timestamp.now().getSeconds());
        // Create a Uri from the absolute image path
        Uri imageUri = Uri.fromFile(new File(imageFilePath));
        storagePath.putFile(imageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        storagePath.getDownloadUrl()
                                .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {

                                        String imageUrl = uri.toString();
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

                                        feedbackReference.add(patientImage)
                                                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                    @Override
                                                    public void onSuccess(DocumentReference documentReference) {

                                                        //scheduleNextUpdate();
                                                        // loadingTextView.setText("Scheduling update...");
                                                        progressBar.setVisibility(View.INVISIBLE);
                                                        loadingTextView.setVisibility(View.INVISIBLE);
                                                        displayFeedback();
                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener() {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e) {
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

    private static final String KEY_NEXT_UPDATE = "nextHealthCheck";

    private void scheduleNextUpdate() {
        progressBar.setVisibility(View.VISIBLE);

        final Map<String, Object> updateData = new HashMap<>();
        woundReference
                .whereEqualTo("bandageId", bandageId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {

                                Wound wound = snapshot.toObject(Wound.class);

                                Calendar nextUpdate = Calendar.getInstance();
                                long timeInMillis = nextUpdate.getTimeInMillis();
                                Date timeToAdd = new Date(timeInMillis + (wound.getHoursUntilCheck() * 3600000));
                                Timestamp timestamp = new Timestamp(timeToAdd);

                                updateData.put(KEY_NEXT_UPDATE, timestamp);

                                Calendar cal = Calendar.getInstance();
                                cal.setTime(new Date());
                                cal.add(Calendar.MINUTE, wound.getHoursUntilCheck());

                                // startAlarm(cal);
                            }
                        }
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                woundReference
                                        .document(document.getId())
                                        .update(updateData)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {

                                                progressBar.setVisibility(View.INVISIBLE);
                                                loadingTextView.setText("Done...");
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
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

    private void startAlarm(Calendar cal) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlertReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, intent, 0);

        assert alarmManager != null;
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pendingIntent);
    }

    private void cancelAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlertReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, intent, 0);

        assert alarmManager != null;
        alarmManager.cancel(pendingIntent);
    }

    private void displayFeedback() {
        switch (woundHealth) {
            case OK:
                setContentView(R.layout.health_ok_layout);
                break;
            case WARNING:
                setContentView(R.layout.health_warning_layout);
                break;
            case DANGER:
                setContentView(R.layout.health_danger_layout);
                break;
            default:
        }
    }

    private enum WoundHealth {
        OK,
        WARNING,
        DANGER
    }

    public void returnToHome(View v){
        startActivity(new Intent(PatientFeedbackActivity.this,
                MainActivity.class));
    }

    @Override
    protected void onStart() {
        super.onStart();

        currentUser = firebaseAuth.getCurrentUser();
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
