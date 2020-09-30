package com.example.wounddoctor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import util.PatientManager;

/**
 * This class is the entry point of the application. It is the first activity that starts when the
 * application is opened. It acts as a landing page to display a ProgressBar element to the user
 * while the software determines if there is an authorised user signed in already.
 * <p>
 * If a user is signed in when the app starts, they are sent to the MainActivity, which acts as
 * a patient account page. If no user is currently signed in, the LoginActivity starts.
 *
 * @author David Farrelly
 */
public class SplashScreenActivity extends AppCompatActivity {

    /**
     * A reference to FirebaseAuth. Has methods to authenticate users on Firebase
     */
    private FirebaseAuth firebaseAuth;

    /**
     * Callback that listens for changes in the authentication state of a user
     */
    private FirebaseAuth.AuthStateListener authStateListener;

    /**
     * A reference to the FirebaseUser who is currently signed in
     */
    private FirebaseUser currentUser;

    /**
     * Create a connection to Firebase Firestore
     */
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * A reference to the patients collection in Firestore
     */
    private CollectionReference collectionReference = db.collection("patients");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hide the application toolbar on this page
        this.getSupportActionBar().hide();
        // Display the xml file that holds the ProgressBar to the user
        setContentView(R.layout.activity_splash_screen);

        // Initialise FirebaseAuth
        firebaseAuth = FirebaseAuth.getInstance();
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                currentUser = firebaseAuth.getCurrentUser();
                // Check if there is currently no Firebase authorised user signed in
                if (currentUser != null) {
                    // Here if an authorised user is signed in already
                    currentUser = firebaseAuth.getCurrentUser();
                    // Get the uid of the user who is signed in -- this is the patientId for that user
                    final String currentUserId = currentUser.getUid();
                    // Call the reference to the patients collection and find the document
                    // relating to the patient who is signed in
                    collectionReference
                            .whereEqualTo("patientId", currentUserId)
                            .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                @Override
                                public void onEvent(@Nullable QuerySnapshot value,
                                                    @Nullable FirebaseFirestoreException error) {
                                    if (!value.isEmpty()) {
                                        // Loop through the returned documents (patients) -- should
                                        // be only one
                                        for (QueryDocumentSnapshot snapshot : value) {
                                            // Create an instance of PatientManager
                                            PatientManager patientManager = PatientManager.getInstance();
                                            // Set the patientId of the patient singleton
                                            patientManager.setPatientId(currentUserId);
                                            // Set the first name of the patient singleton
                                            patientManager.setPatientName(snapshot.getString("fName"));
                                            // Bring the user to the MainActivity where their patient account is
                                            startActivity(new Intent(SplashScreenActivity.this,
                                                    MainActivity.class));
                                        }
                                    }
                                }
                            });
                } else {
                    // Currently no user signed in --  send the user to the LoginActivity
                    startActivity(new Intent(SplashScreenActivity.this,
                            LoginActivity.class));
                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        // When the app is opened, set the current FirebaseUser as the user who is signed in
        currentUser = firebaseAuth.getCurrentUser();
        // Set the AuthStateListener to listen for changes in FirebaseAuth
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // When the app is closed or paused, release the FirebaseAuth.AuthStateListener
        if (firebaseAuth != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }
}
