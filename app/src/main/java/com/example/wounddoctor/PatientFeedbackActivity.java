package com.example.wounddoctor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.Date;

import model.PatientImage;
import util.PatientManager;
import util.ProcessImage;

public class PatientFeedbackActivity extends AppCompatActivity {

    private static final String TAG = "PatientFeedbackActivity";

    private ImageView positiveImageView;
    private ImageView negativeImageView;
    private TextView titleTextView;
    private TextView positiveTextView;
    private TextView negativeTextView;
    private TextView redTextView;
    private TextView greenTextView;
    private TextView blueTextView;
    private Button homeButton;
    private ProgressBar progressBar;

    // Get the current user from PatientManager
    private String currentPatientId;
    private String currentPatientName;

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser currentUser;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private StorageReference storageReference;
    private CollectionReference collectionReference = db.collection("feedback");

    private String imageFilePath;
    private String qrCodeId = "100abc";
    private int redValue = 200;
    private int greenValue = 100;
    private int blueValue = 120;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_feedback);

        storageReference = FirebaseStorage.getInstance().getReference();
        firebaseAuth = FirebaseAuth.getInstance();

        positiveImageView = findViewById(R.id.postImage_CheckmarkIcon);
        negativeImageView = findViewById(R.id.postImage_WarningIcon);
        titleTextView = findViewById(R.id.postImage_TitleTextView);
        positiveTextView = findViewById(R.id.postImage_PositiveTextView);
        negativeTextView = findViewById(R.id.postImage_WarningTextView);
        redTextView = findViewById(R.id.feedback_RedTextView);
        greenTextView = findViewById(R.id.feedback_GreenTextView);
        blueTextView = findViewById(R.id.feedback_BlueTextView);
        homeButton = findViewById(R.id.postImage_HomeButton);
        progressBar = findViewById(R.id.postImage_ProgressBar);

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
            //redValue = extra.getInt("red value");
            //greenValue = extra.getInt("green value");
            //blueValue = extra.getInt("blue value");
            //qrCodeId = extra.getString("qr id");

            //redTextView.setText(String.valueOf(redValue));
            //greenTextView.setText(String.valueOf(greenValue));
            //blueTextView.setText(String.valueOf(blueValue));
        }

        postPatientFeedback();

    }

    private void postPatientFeedback() {

        progressBar.setVisibility(View.VISIBLE);

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
                                                redValue,
                                                greenValue,
                                                blueValue,
                                                imageUrl,
                                                qrCodeId,
                                                new Timestamp(new Date()));

                                        collectionReference.add(patientImage)
                                                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                    @Override
                                                    public void onSuccess(DocumentReference documentReference) {
                                                        progressBar.setVisibility(View.INVISIBLE);

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

    private void displayFeedback(){

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
