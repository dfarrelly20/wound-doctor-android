package com.example.wounddoctor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import util.PatientManager;

/**
 * Class verifies and creates a new account on the server. Class is called from
 * LoginActivity when the user clicks the 'Create Account' button
 * <p>
 * User is required to fill in two forms to create an account. These are the verification form
 * and the register form respectively. The verification form requires the user to verify their email
 * address against a unique verification ID they have been sent. Upon successful verification, the
 * register form can be completed and the patient is authenticated by FirebaseAuth
 *
 * @author David Farrelly
 */
public class CreateAccountActivity extends AppCompatActivity {

    /**
     * Debug tag for this class
     */
    private static final String TAG = "CreateAccountActivity";

    /**
     * Reference to FirebaseAuth -- needed to authenticate a user on Firebase
     */
    private FirebaseAuth firebaseAuth;

    /**
     * Callback that listens for changes in the authentication state of a user
     */
    private FirebaseAuth.AuthStateListener authStateListener;

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

    /**
     * Creating a reference to the verification collection in Firestore
     */
    private CollectionReference verificationCollection = db.collection("verification");

    /**
     * The ID of the doctor who sent the patient their verification ID
     */
    private String doctorIdToAdd;

    /**
     * LinearLayout element that holds the verification form
     */
    private LinearLayout verifyLinearLayout;

    /**
     * LinearLayout element that holds the registration form
     */
    private LinearLayout formLinearLayout;

    /**
     * EditText element for the verification ID field
     */
    private EditText verificationIdEditText;

    /**
     * EditText element for the email address field
     */
    private EditText emailEditText;

    /**
     * EditText element for the first name field
     */
    private EditText fNameEditText;

    /**
     * EditText element for the last name field
     */
    private EditText lNameEditText;

    /**
     * EditText element for the password field
     */
    private EditText passwordEditText;

    /**
     * EditText element for the confirm password field
     */
    private EditText confirmPasswordEditText;

    /**
     * EditText element for the phone number field
     */
    private EditText phoneEditText;

    /**
     * Button element for submitting the patients verification form
     */
    private Button confirmVerificationButton;

    /**
     * Button element used for submitting the patients registration form
     */
    private Button createAccountButton;

    /**
     * ProgressBar element to display when making calls to Firebase
     */
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);
        // Creating the toolbar
        Objects.requireNonNull(getSupportActionBar()).setTitle("Create Account");
        // Set back button on toolbar to return to LoginActivity.java
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Create an instance of Firebase Auth -- needed to sign a user up to the app
        firebaseAuth = FirebaseAuth.getInstance();

        // The EditText field for verification ID
        verificationIdEditText = findViewById(R.id.createAccount_PatientIdEditText);
        // The EditText field for email address
        emailEditText = findViewById(R.id.createAccount_EmailEditText);
        // The EditText field for first name
        fNameEditText = findViewById(R.id.createAccount_FNameEditText);
        // The EditText field for last name
        lNameEditText = findViewById(R.id.createAccount_LNameEditText);
        // The EditText field for the users password
        passwordEditText = findViewById(R.id.createAccount_PasswordEditText);
        // The EditText field for confirming the users password
        confirmPasswordEditText = findViewById(R.id.createAccount_ConfirmPasswordEditText);
        // The EditText field for phone number
        phoneEditText = findViewById(R.id.createAccount_PhoneEditText);
        // The LinearLayout element for the verification form
        verifyLinearLayout = findViewById(R.id.createAccount_VerifyLayout);
        // The LinearLayout element for the registration form
        formLinearLayout = findViewById(R.id.createAccount_FormLayout);
        // The ProgressBar element
        progressBar = findViewById(R.id.createAccountProgressBar);

        // Instantiate the AuthStateListener
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                currentUser = firebaseAuth.getCurrentUser();
            }
        };

        // The button element for submitting the verification form
        confirmVerificationButton = findViewById(R.id.createAccount_ConfirmButton);
        // Callback to listen for when the confirm button is clicked
        confirmVerificationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Checking that the user has not left the verification ID or the email field empty
                if (!TextUtils.isEmpty(verificationIdEditText.getText().toString())
                        && !TextUtils.isEmpty(emailEditText.getText().toString())) {
                    // The verification ID entered by the user
                    String verificationId = verificationIdEditText.getText().toString();
                    // The email address entered by the user
                    String email = emailEditText.getText().toString();
                    // If both the ID and the email have been filled in, call this method to
                    // verify the user credentials
                    verifyEmail(verificationId, email);
                } else {
                    // User has left either or both of the fields empty
                    Toast.makeText(CreateAccountActivity.this,
                            "Please complete both fields to verify your email address",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
        // The button element for submitting the registration form
        createAccountButton = findViewById(R.id.createAccount_CreateButton);
        // Callback to listen for when the create account button is clicked
        createAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Checking that the user has not left any fields in the register form empty
                if (!TextUtils.isEmpty(emailEditText.getText().toString())
                        && !TextUtils.isEmpty(fNameEditText.getText().toString())
                        && !TextUtils.isEmpty(lNameEditText.getText().toString())
                        && !TextUtils.isEmpty(passwordEditText.getText().toString())
                        && !TextUtils.isEmpty(confirmPasswordEditText.getText().toString())
                        && !TextUtils.isEmpty(phoneEditText.getText().toString())) {
                    // The email address entered by the user from the verification form
                    String email = emailEditText.getText().toString().trim();
                    // The password entered by the user
                    String password = passwordEditText.getText().toString().trim();
                    String confirmPassword = confirmPasswordEditText.getText().toString().trim();
                    // The first name entered by the user
                    String fName = fNameEditText.getText().toString().trim();
                    // The last name entered by the user
                    String lName = lNameEditText.getText().toString().trim();
                    // The phone number entered by the user
                    String phone = phoneEditText.getText().toString();

                    // Here checking that the password has been confirmed correctly
                    if (password.equals(confirmPassword)) {
                        // If the user has completed the register form, including all fields, call
                        // this method to create account
                        createNewUserAccount(email, password, fName, lName, phone);
                    } else {
                        // The user has entered two different passwords
                        Toast.makeText(CreateAccountActivity.this,
                                "Passwords do not match. Please try again",
                                Toast.LENGTH_LONG).show();
                    }
                } else {
                    // The user has left one or more fields in the register form empty
                    Toast.makeText(CreateAccountActivity.this,
                            "Please ensure all fields are filled in correctly",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /**
     * The method that is called when the user has completed both fields in the verification form
     * and clicked the 'Verify' button. The verification ID and the user's email address are
     * passed in as arguments.
     * <p>
     * Method again checks that neither field in the verification form is empty. A reference to
     * the verification collection in firestore is then used to check if the
     * email address entered by the user corresponds to a verification ID on the server. If a match
     * is found, the doctor ID from that document reference is stored in the doctorIdToAdd String
     * variable. The verification form is hidden and the register form displayed so that user
     * can continue with the registration process.
     *
     * @param verificationId String - the verification ID entered by the user
     * @param email          String - the email address to verify
     */
    private void verifyEmail(final String verificationId, final String email) {
        // Checking that the user has not left the verification ID or the email field empty
        if (!TextUtils.isEmpty(verificationId)
                && !TextUtils.isEmpty(email)) {
            // Call the reference to the verification collection where the field 'patientEmail'
            // equals the email address that the user entered
            verificationCollection
                    .whereEqualTo("patientEmail", email)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                            // Checking that a document matching the where clause has been found
                            if (!queryDocumentSnapshots.isEmpty()) {
                                // Looping through the returned documents
                                for (QueryDocumentSnapshot patient : queryDocumentSnapshots) {
                                    // Get the verification ID from the found document
                                    String returnedId = patient.getString("verificationId");
                                    // Check that the verification ID returned from the document
                                    // matches the ID entered by the user
                                    if (verificationId.equals(returnedId)) {
                                        // ID match confirmed -- get the doctorID from the document
                                        doctorIdToAdd = patient.getString("doctorId");
                                        // Hide the verification form
                                        verifyLinearLayout.setVisibility(View.INVISIBLE);
                                        // Display the next register form
                                        formLinearLayout.setVisibility(View.VISIBLE);
                                    } else {
                                        // Here if the ID entered by the user does not match the
                                        // verification ID found in the returned document
                                        Toast.makeText(CreateAccountActivity.this,
                                                "Verification ID not valid. Please ensure the ID matches the one sent to you by doctor",
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                            } else {
                                // Here if the user has entered an email address that does not exist
                                // in the verification collection
                                Toast.makeText(CreateAccountActivity.this,
                                        "Email address not recognised. Please use the email address that you gave to your doctor",
                                        Toast.LENGTH_LONG).show();
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Here if there was a problem retrieving the document or querying the
                            // server
                            Log.d(TAG, "onFailure: " + e.toString());
                        }
                    });
        }
    }

    /**
     * Method that is called when the user has completed all fields on the register form. First
     * checks again that all fields have been completed.
     * <p>
     * The method createUserWithEmailAndPassword from FirebaseAuth is used to authenticate
     * the user on Firebase. When authenticated the user can user the email and password they
     * entered to log into the app in the future. If this method is successful the user is
     * authenticated on Firebase and is signed into the app. The user is brought to MyWoundsActivity
     * to register their first wound as a new patient
     *
     * @param email    String -  the email entered by the user
     * @param password String - the password entered by the user
     * @param fName    String - the first name of the user
     * @param lName    String - the last name of the user
     * @param phone    String - the phone number of the user
     */
    private void createNewUserAccount(final String email, String password, final String fName,
                                      final String lName, final String phone) {
        // Checking that user has left no fields on the register form empty
        if (!TextUtils.isEmpty(email)
                && !TextUtils.isEmpty(password)
                && !TextUtils.isEmpty(fName)
                && !TextUtils.isEmpty(lName)
                && !TextUtils.isEmpty(phone)) {
            // Display the progress bar while making calls to Firebase
            progressBar.setVisibility(View.VISIBLE);
            // Attempt to create the user's account using this method from firebase auth
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            // Here if no problem making call to server
                            if (task.isSuccessful()) {
                                createAccountButton.setEnabled(false);
                                // Get the authenticated FirebaseUser from FirebaseAuth
                                currentUser = firebaseAuth.getCurrentUser();
                                assert currentUser != null;
                                // Create a string to store the ID of the current authenticated user
                                // This is needed to create the patient object map
                                final String currentUserId = currentUser.getUid();
                                // Create a patient object map to create a new document in the
                                // patients collection
                                Map<String, String> patientObject = new HashMap<>();
                                patientObject.put("patientId", currentUserId);
                                patientObject.put("fName", fName);
                                patientObject.put("lName", lName);
                                patientObject.put("email", email);
                                patientObject.put("phone", phone);
                                patientObject.put("doctorId", doctorIdToAdd);
                                // Save new patient to Firestore collection
                                collectionReference.add(patientObject)
                                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                            @Override
                                            public void onSuccess(DocumentReference documentReference) {
                                                documentReference.get()
                                                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                                if (Objects.requireNonNull(task.getResult()).exists()) {
                                                                    // Hide the ProgressBar element as call to Firebase complete
                                                                    progressBar.setVisibility(View.INVISIBLE);
                                                                    // Get the first name of the patient from the returned document
                                                                    String fName = task.getResult().getString("fName");
                                                                    // Create a new instance of PatientManager
                                                                    PatientManager patientManager = PatientManager.getInstance();
                                                                    // Set the ID of the patient currently signing in
                                                                    patientManager.setPatientId(currentUserId);
                                                                    // Set the first name of the patient currently signing in
                                                                    patientManager.setPatientName(fName);
                                                                    // Call this method to delete the verification ID used by the patient
                                                                    deleteVerificationId(email);
                                                                    // Create an intent to bring the user to MyWoundsActivity
                                                                    // so that they can register their first wound
                                                                    Intent intent = new Intent(CreateAccountActivity.this,
                                                                            MyWoundsActivity.class);
                                                                    intent.putExtra("fName", fName);
                                                                    intent.putExtra("patientId", currentUserId);
                                                                    startActivity(intent);
                                                                    // Finish this activity so user cannot return
                                                                    finish();
                                                                } else {
                                                                    // Hide the ProgressBar element as call to Firebase complete
                                                                    progressBar.setVisibility(View.INVISIBLE);
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
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.d(TAG, "onFailure: " + e.toString());
                                            }
                                        });
                            } else {
                                try {
                                    throw Objects.requireNonNull(task.getException());
                                } catch (FirebaseAuthUserCollisionException e) {
                                    // Here if the user has entered an email address that has already
                                    // been authenticated in Firebase -- ie. email has been used
                                    // to create an account already
                                    Toast.makeText(CreateAccountActivity.this,
                                            "The email address you entered is already in use",
                                            Toast.LENGTH_LONG).show();
                                } catch (FirebaseAuthWeakPasswordException e) {
                                    // The password entered by the user is not strong enough
                                    // FirebaseAuth requires passwords to be at least 6 characters
                                    Toast.makeText(CreateAccountActivity.this,
                                            e.getReason(),
                                            Toast.LENGTH_LONG).show();
                                } catch (FirebaseNetworkException e) {
                                    // Here if there has been a network error -- user must be connected
                                    // to internet to log in
                                    Toast.makeText(CreateAccountActivity.this,
                                            "Network connection required to create account. Please connect to the internet and try again",
                                            Toast.LENGTH_LONG).show();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d(TAG, "onFailure: " + e.toString());
                }
            });
        } else {
            // Here is user has left one or more fields in the registration form empty
            Toast.makeText(this,
                    "Please fill in all fields on the registration form",
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * This method is used to delete the verification ID that the patient has used to create their
     * account. If this method is called, the patients account has been successfully created, so the
     * verification ID is no longer needed and can be removed from the verification collection.
     *
     * @param email
     */
    private void deleteVerificationId(String email) {
        // Call reference to verification collection and find document relating to the patient
        verificationCollection
                .whereEqualTo("patientEmail", email)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                verificationCollection.document(document.getId())
                                        .delete()
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                Log.d(TAG, "onSuccess: verification ID deleted");
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
        // This is needed in case the user is part way through registering and pauses the app
        currentUser = firebaseAuth.getCurrentUser();
        // Add listener to FirebaseAuth reference when activity starts
        firebaseAuth.addAuthStateListener(authStateListener);
    }

}
