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

public class CreateAccountActivity extends AppCompatActivity {

    private static final String TAG = "CreateAccountActivity";

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser currentUser;

    // Firestore connection
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = db.collection("patients");
    private CollectionReference verificationCollection = db.collection("verification");

    private LinearLayout verifyLinearLayout;
    private LinearLayout formLinearLayout;

    private EditText verificationIdEditText;
    private EditText emailEditText;
    private EditText fNameEditText;
    private EditText lNameEditText;
    private EditText passwordEditText;
    private EditText phoneEditText;

    private Button confirmPatientIdButton;
    private Button createAccountButton;

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        Objects.requireNonNull(getSupportActionBar()).setTitle("Create Account");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        firebaseAuth = FirebaseAuth.getInstance();

        verificationIdEditText = findViewById(R.id.createAccount_PatientIdEditText);
        emailEditText = findViewById(R.id.createAccount_EmailEditText);
        fNameEditText = findViewById(R.id.createAccount_FNameEditText);
        lNameEditText = findViewById(R.id.createAccount_LNameEditText);
        passwordEditText = findViewById(R.id.createAccount_PasswordEditText);
        phoneEditText = findViewById(R.id.createAccount_PhoneEditText);

        verifyLinearLayout = findViewById(R.id.createAccount_VerifyLayout);
        formLinearLayout = findViewById(R.id.createAccount_FormLayout);

        progressBar = findViewById(R.id.createAccountProgressBar);

        confirmPatientIdButton = findViewById(R.id.createAccount_ConfirmButton);
        createAccountButton = findViewById(R.id.createAccount_CreateButton);

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

        confirmPatientIdButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(verificationIdEditText.getText().toString())){

                    String verificationId = verificationIdEditText.getText().toString();
                    String email = emailEditText.getText().toString();

                    verifyEmail(verificationId, email);

                } else {
                    Toast.makeText(CreateAccountActivity.this, "Please enter your verification ID", Toast.LENGTH_SHORT).show();
                }
            }
        });

        createAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!TextUtils.isEmpty(emailEditText.getText().toString())
                        && !TextUtils.isEmpty(fNameEditText.getText().toString())
                        && !TextUtils.isEmpty(lNameEditText.getText().toString())
                        && !TextUtils.isEmpty(passwordEditText.getText().toString())
                        && !TextUtils.isEmpty(phoneEditText.getText().toString())) {

                    String email = emailEditText.getText().toString().trim();
                    String password = passwordEditText.getText().toString().trim();
                    String fName = fNameEditText.getText().toString().trim();
                    String lName = lNameEditText.getText().toString().trim();
                    String phone = phoneEditText.getText().toString();

                    createNewUserAccount(email, password, fName, lName, phone);

                } else {
                    Toast.makeText(CreateAccountActivity.this, "Please ensure all fields are filled in", Toast.LENGTH_SHORT).show();
                }

            }
        });

    }

    private void verifyEmail(final String verificationId, String email){

        if (!TextUtils.isEmpty(verificationId)
                && !TextUtils.isEmpty(email)){

            verificationCollection.whereEqualTo("patientEmail", email)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                            if (!queryDocumentSnapshots.isEmpty()){

                                for (QueryDocumentSnapshot patient : queryDocumentSnapshots){

                                    String returnedId = patient.getString("verificationId");
                                    if (verificationId.equals(returnedId)){

                                        verifyLinearLayout.setVisibility(View.INVISIBLE);
                                        formLinearLayout.setVisibility(View.VISIBLE);

                                    } else {
                                        Toast.makeText(CreateAccountActivity.this, "Verification ID not valid", Toast.LENGTH_SHORT).show();
                                    }

                                }

                            }

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                        }
                    });

        }

    }

    private void createNewUserAccount(final String email, String password, final String fName, final String lName, final String phone) {
        if (!TextUtils.isEmpty(email)
                && !TextUtils.isEmpty(password)
                && !TextUtils.isEmpty(fName)
                && !TextUtils.isEmpty(lName)
                && !TextUtils.isEmpty(phone)) {

            progressBar.setVisibility(View.VISIBLE);

            firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            if (task.isSuccessful()) {

                                currentUser = firebaseAuth.getCurrentUser();
                                assert currentUser != null;
                                final String currentUserId = currentUser.getUid();

                                // Create a user map to create a new user in the patient collection
                                Map<String, String> patientObject = new HashMap<>();
                                patientObject.put("patientId", currentUserId);
                                patientObject.put("fName", fName);
                                patientObject.put("lName", lName);
                                patientObject.put("email", email);
                                patientObject.put("phone", phone);

                                // Save new patient to Firestore db
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

                                                                    Intent intent = new Intent(CreateAccountActivity.this,
                                                                            MainActivity.class);
                                                                    intent.putExtra("fName", fName);
                                                                    intent.putExtra("patientId", currentUserId);
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
                                Log.d(TAG, "onComplete: create user task not successful");
                            }

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d(TAG, "onFailure: " + e.toString());
                }
            });

        } else {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onStart() {
        super.onStart();

        currentUser = firebaseAuth.getCurrentUser();
        firebaseAuth.addAuthStateListener(authStateListener);
    }
}
