package com.example.wounddoctor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Objects;

import util.PatientManager;

/**
 * This class is used to log a patient into their WoundDoctor account. Patient must be registered
 * on the system in order to login. If they are not, they can call the CreateAccountActivity to
 * register.
 * <p>
 * This class is called from SplashScreenActivity if there is no user currently signed in when
 * the app is opened. It is also called from MainActivity if the user who is logged in signs out
 * of their account.
 *
 * @author David Farrelly
 */
public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    /**
     * Debug tag for this class
     */
    private static final String TAG = "LoginActivity";

    /**
     * Button element to log a user in
     */
    private Button loginButton;

    /**
     * Button element that starts the CreateAccountActivity
     */
    private Button createAccount;

    /**
     * EditText element for the email address field
     */
    private AutoCompleteTextView emailEditText;

    /**
     * EditText element for the password field
     */
    private EditText passwordEditText;

    /**
     * ProgressBar element to display when making calls to Firebase
     */
    private ProgressBar progressBar;

    /**
     * Reference to FirebaseAuth for authenticating a user on Firebase
     */
    private FirebaseAuth firebaseAuth;

    /**
     * The FirebaseUser who is currently signed in
     */
    private FirebaseUser currentUser;

    /**
     * Making a connection to Firebase Firestore
     */
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Creating a reference to the patients collection in Firestore
     */
    private CollectionReference collectionReference = db.collection("patients");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Hide the toolbar for this activity
        Objects.requireNonNull(this.getSupportActionBar()).hide();
        setContentView(R.layout.activity_login);

        // Create an instance of Firebase Auth -- needed to log a user into the app
        firebaseAuth = FirebaseAuth.getInstance();

        // The EditText field for email address
        emailEditText = findViewById(R.id.login_EmailEditText);
        // The EditText field for password
        passwordEditText = findViewById(R.id.login_PasswordEditText);
        // The ProgressBar element
        progressBar = findViewById(R.id.login_ProgressBar);
        // The button element for submitting the users login credentials
        loginButton = findViewById(R.id.login_SignInButton);
        // Callback to listen for when the login button is clicked
        loginButton.setOnClickListener(this);
        // The button element to start the CreateAccountActivity
        createAccount = findViewById(R.id.login_CreateAccountButton);
        // Callback to listen for when the create account button is clicked
        createAccount.setOnClickListener(this);
    }

    /**
     * Method that listens for button click events
     *
     * @param v - the Button that was clicked
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.login_SignInButton:
                // String to hold the email address entered by the user
                String email = emailEditText.getText().toString().trim();
                // String to hold the password entered by the user
                String password = passwordEditText.getText().toString().trim();
                // Call this method to check login credentials of the user
                // Pass in the email and password they have entered
                loginPatientWithEmail(email, password);
                break;
            case R.id.login_CreateAccountButton:
                // User has clicked the button to create an account -- start activity
                startActivity(new Intent(LoginActivity.this,
                        CreateAccountActivity.class));
                break;
        }
    }

    /**
     * This method attempts to log a patient into their account.
     *
     * @param email String - The email address entered by the user
     * @param password String - The password entered by the user
     */
    private void loginPatientWithEmail(String email, String password) {
        // Display the ProgressBar element while making call to Firebase
        progressBar.setVisibility(View.VISIBLE);
        // First check to make sure user has completed both fields in the login form
        if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password)) {
            // If both fields have been completed, call this Firebase Auth method that checks
            // if the credentials entered by the user exist in the Firebase Authentication
            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Here if the user has entered their email and password correctly
                                // User is now signed in
                                currentUser = firebaseAuth.getCurrentUser();
                                assert currentUser != null;
                                // Get the ID of the current user from FirebaseAuth
                                String currentUserId = currentUser.getUid();
                                // Call the collection reference and find the current user
                                collectionReference
                                        .whereEqualTo("patientId", currentUserId)
                                        .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                            @Override
                                            public void onEvent(@Nullable QuerySnapshot value,
                                                                @Nullable FirebaseFirestoreException error) {
                                                assert value != null;
                                                if (!value.isEmpty()) {
                                                    // Here if user was found in patients collection
                                                    // Hide the ProgressBar element as the call to Firebase is complete
                                                    progressBar.setVisibility(View.INVISIBLE);
                                                    for (QueryDocumentSnapshot snapshot : value) {
                                                        // Create an instance of PatientManager
                                                        PatientManager patientManager = PatientManager.getInstance();
                                                        // Set the ID of the patient currently signed in
                                                        patientManager.setPatientId(snapshot.getString("patientId"));
                                                        // Set the first name of the patient currently signed in
                                                        patientManager.setPatientName(snapshot.getString("fName"));
                                                        // Bring the user to the MainActivity
                                                        startActivity(new Intent(LoginActivity.this,
                                                                MainActivity.class));
                                                        // Finish this activity so the user cannot return using the back
                                                        // button on their device
                                                        finish();
                                                    }
                                                }
                                            }
                                        });
                            } else {
                                try {
                                    throw Objects.requireNonNull(task.getException());
                                } catch (FirebaseAuthInvalidUserException e) {
                                    // Here if user has tried to log in with an email address that is
                                    // not authorised
                                    Toast.makeText(LoginActivity.this,
                                            "Please enter a valid email",
                                            Toast.LENGTH_SHORT).show();
                                } catch (FirebaseAuthInvalidCredentialsException e) {
                                    // Here if the user has tried to log in with an incorrect password
                                    Toast.makeText(LoginActivity.this,
                                            "Password not recognised. Please try again",
                                            Toast.LENGTH_SHORT).show();
                                } catch (FirebaseNetworkException e) {
                                    // Here if there has been a network error -- user must be connected
                                    // to internet to log in
                                    Toast.makeText(LoginActivity.this,
                                            "Network connection required to login. Please connect to the internet and try again",
                                            Toast.LENGTH_LONG).show();
                                } catch (Exception e) {
                                    // General exception encountered
                                    Log.d(TAG, "sign in failed: " + e.toString());
                                }
                            }

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Hide the ProgressBar element as the call to Firebase is complete
                            progressBar.setVisibility(View.INVISIBLE);
                            Log.d(TAG, "onFailure: " + e.toString());
                        }
                    });

        } else {
            // Here if the user has left one or both of the fields in the login form empty
            Toast.makeText(this,
                    "Please enter your email and password",
                    Toast.LENGTH_SHORT).show();
            // Hide the ProgressBar element while the user attempts to login again
            progressBar.setVisibility(View.INVISIBLE);
        }

    }
}
