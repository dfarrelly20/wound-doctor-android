package com.example.wounddoctor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

import util.Constants;
import util.PatientManager;

/**
 * This is the class that represents a patients 'account' page. If there is already a
 * user logged in when the app is opened, the user is brought to this activity where they can
 * view their patient account.
 * <p>
 * This activity acts as a 'main menu,' whereby the user who is signed in can navigate to the main
 * parts of the app.
 *
 * @author David Farrelly
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    /**
     * Debug tag for this activity
     */
    private static final String TAG = "MainActivity";

    /**
     * The CardView element that opens the camera when clicked
     */
    private CardView takePictureCard;

    /**
     * The CardView element that opens the 'My Wounds' page when clicked
     */
    private CardView myWoundsCard;

    /**
     * The CardView element that signs a user out of the app when clicked
     */
    private CardView signOutCard;

    /**
     * A reference to FirebaseAuth, which provides methods to manage Firebase user accounts
     */
    private FirebaseAuth firebaseAuth;

    /**
     * An interface that listens for changes in the authentication state of a user
     */
    private FirebaseAuth.AuthStateListener authStateListener;

    /**
     * A representation of the current user who is authorised to be signed in to the app
     */
    private FirebaseUser currentUser;

    /**
     * A reference to an instance of FirebaseFirestore, where the database is hosted
     */
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create an instance of FirebaseAuth
        firebaseAuth = FirebaseAuth.getInstance();
        // Create an instance of the Firebase AuthStateListener -- used here to listen for a user
        // signing out of the app
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                currentUser = firebaseAuth.getCurrentUser();
                if (currentUser != null) {
                    // A user is signed in already
                    currentUser = firebaseAuth.getCurrentUser();
                    // final String currentUserId = currentUser.getUid();
                } else {
                    // Currently no user signed in
                    startActivity(new Intent(MainActivity.this,
                            LoginActivity.class));
                }
            }
        };

        // Creating the activity toolbar
        Objects.requireNonNull(getSupportActionBar()).setElevation(0);
        // Get the patients name from PatientManager and place here in the toolbar
        Objects.requireNonNull(getSupportActionBar())
                .setTitle(PatientManager.getInstance().getPatientName() +
                        "'s Account");

        // The CardView element for takePictureCard in the xml
        takePictureCard = findViewById(R.id.main_takePictureCard);
        // Listen for click events on the takePictureCard
        takePictureCard.setOnClickListener(this);
        // The CardView element for myWoundsCard in the xml
        myWoundsCard = findViewById(R.id.main_myWoundsCard);
        // Listen for click events on the myWoundsCard
        myWoundsCard.setOnClickListener(this);
        // The CardView element for signOutCard in the xml
        signOutCard = findViewById(R.id.main_SignOutCard);
        // Listen for click events on the signOutCard
        signOutCard.setOnClickListener(this);
    }

    /**
     * Method is called when a click event occurs on one of the CardView elements
     *
     * @param v - The button that was clicked
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.main_takePictureCard:
                // Start CameraActivity
                startActivity(new Intent(MainActivity.this,
                        CameraActivity.class)
                        .putExtra("action requested", Constants.REQUEST_CAMERA_ACTION));
                break;
            case R.id.main_myWoundsCard:
                // Start MyWoundsActivity
                startActivity(new Intent(MainActivity.this,
                        MyWoundsActivity.class));
                break;
            case R.id.main_SignOutCard:
                // Start LoginActivity
                startActivity(new Intent(MainActivity.this,
                        LoginActivity.class));
                // Call method from FirebaseAuth to sign user out of app
                firebaseAuth.signOut();
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Use FirebaseAuth to get details of current authenticated user
        currentUser = firebaseAuth.getCurrentUser();
        // Add listener to FirebaseAuth reference when activity starts
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Remove the AuthStateListener if user moves away from activity
        if (firebaseAuth != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }

}
