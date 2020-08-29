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
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
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
 * Class verifies and creates a new account on the server.
 */
public class CreateAccountActivity extends AppCompatActivity {

    private static final String TAG = "CreateAccountActivity";

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser currentUser;

    // Firestore connection
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = db.collection("patients");
    private CollectionReference verificationCollection = db.collection("verification");

    // The ID of the doctor who sent the patient the verification ID
    private String doctorIdToAdd;

    private LinearLayout verifyLinearLayout;
    private LinearLayout formLinearLayout;
    private EditText verificationIdEditText;
    private EditText emailEditText;
    private EditText fNameEditText;
    private EditText lNameEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private EditText phoneEditText;
    private Button confirmPatientIdButton;
    private Button createAccountButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);
        // Creating the toolbar
        Objects.requireNonNull(getSupportActionBar()).setTitle("Create Account");
        // Set back button on toolbar to return to LoginActivity
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        firebaseAuth = FirebaseAuth.getInstance();

        verificationIdEditText = findViewById(R.id.createAccount_PatientIdEditText);
        emailEditText = findViewById(R.id.createAccount_EmailEditText);
        fNameEditText = findViewById(R.id.createAccount_FNameEditText);
        lNameEditText = findViewById(R.id.createAccount_LNameEditText);
        passwordEditText = findViewById(R.id.createAccount_PasswordEditText);
        confirmPasswordEditText = findViewById(R.id.createAccount_ConfirmPasswordEditText);
        phoneEditText = findViewById(R.id.createAccount_PhoneEditText);
        verifyLinearLayout = findViewById(R.id.createAccount_VerifyLayout);
        formLinearLayout = findViewById(R.id.createAccount_FormLayout);
        progressBar = findViewById(R.id.createAccountProgressBar);

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                currentUser = firebaseAuth.getCurrentUser();
                if (currentUser != null) {
                    // User is already logged in
                } else {
                    // No user logged in yet
                }
            }
        };
        confirmPatientIdButton = findViewById(R.id.createAccount_ConfirmButton);
        confirmPatientIdButton.setOnClickListener(new View.OnClickListener() {
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
        createAccountButton = findViewById(R.id.createAccount_CreateButton);
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

                    if (password.equals(confirmPassword)){
                        // If the user has completed the register form, call this method to create
                        // account
                        createNewUserAccount(email, password, fName, lName, phone);
                    } else {
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
     * Method again checks that neither field in the verification form is empty. The reference to
     * the verification collection in the firestore collection is then used to check if the
     * email address entered by the user corresponds to a verification ID on thr server. If a match
     * is found, the doctor ID from that document reference is stored in the doctorIdToAdd string
     * variable. The verification form is hidden and the register form displayed so that user
     * can continue with the registration process.
     *
     * @param verificationId - String
     * @param email          - String
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
                                                "Verification ID not valid",
                                                Toast.LENGTH_LONG).show();
                                    }
                                }
                            } else {
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
     *
     * @param email     - String
     * @param password- String
     * @param fName-    String
     * @param lName-    String
     * @param phone-    String
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
                                // Initialise the authorised current FirebaseUser
                                currentUser = firebaseAuth.getCurrentUser();
                                assert currentUser != null;
                                // Create a string to store the ID of the current user
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
                                                                    progressBar.setVisibility(View.INVISIBLE);

                                                                    String fName = task.getResult().getString("fname");

                                                                    PatientManager patientManager = PatientManager.getInstance();
                                                                    patientManager.setPatientId(currentUserId);
                                                                    patientManager.setPatientName(fName);

                                                                    deleteVerificationId(email);

                                                                    Intent intent = new Intent(CreateAccountActivity.this,
                                                                            MainActivity.class);
                                                                     intent.putExtra("fName", fName);
                                                                     intent.putExtra("patientId", currentUserId);
                                                                    // intent.putExtra("new patient", true);
                                                                    startActivity(intent);
                                                                } else {
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
                                    Toast.makeText(CreateAccountActivity.this, "The email address you entered is already in use", Toast.LENGTH_LONG).show();
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
            Toast.makeText(this, "Please fill in all fields on the registration form", Toast.LENGTH_LONG).show();
        }
    }

    private void deleteVerificationId(String email) {
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
                                                Log.d(TAG, "onSuccess: verifcation ID deleted");
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

        currentUser = firebaseAuth.getCurrentUser();
        firebaseAuth.addAuthStateListener(authStateListener);
    }
}
