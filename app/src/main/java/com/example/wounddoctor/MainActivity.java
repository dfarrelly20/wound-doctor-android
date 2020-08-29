package com.example.wounddoctor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Objects;

import util.Constants;
import util.PatientManager;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private CardView takePictureCard;
    private CardView myWoundsCard;

    private boolean newPatientSignUp = false;

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser currentUser;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = db.collection("patients");
    private CollectionReference woundCollectionReference = db.collection("wounds");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                currentUser = firebaseAuth.getCurrentUser();
                if (currentUser != null) {
                    // A user is signed in already
                    currentUser = firebaseAuth.getCurrentUser();
                    final String currentUserId = currentUser.getUid();

//                    collectionReference
//                            .whereEqualTo("patientId", currentUserId)
//                            .addSnapshotListener(new EventListener<QuerySnapshot>() {
//                                @Override
//                                public void onEvent(@Nullable QuerySnapshot value,
//                                                    @Nullable FirebaseFirestoreException error) {
//                                    if (error != null){
//                                    }
//                                    if (!value.isEmpty()){
//                                        for (QueryDocumentSnapshot snapshot : value){
//                                            PatientManager patientManager = PatientManager.getInstance();
//                                            patientManager.setPatientId(currentUserId);
//                                            patientManager.setPatientName(snapshot.getString("fName"));
//                                        }
//                                    }
//                                }
//                            });
                } else {
                    // Currently no user signed in
                    startActivity(new Intent(MainActivity.this,
                            LoginActivity.class));
                }
            }
        };

        Objects.requireNonNull(getSupportActionBar()).setElevation(0);
        Objects.requireNonNull(getSupportActionBar())
                .setTitle(PatientManager.getInstance().getPatientName() +
                        "'s Account");

        takePictureCard = findViewById(R.id.main_takePictureCard);
        takePictureCard.setOnClickListener(this);
        myWoundsCard = findViewById(R.id.main_myWoundsCard);
        myWoundsCard.setOnClickListener(this);

        if (newPatientSignUp){
            fadeAnimation();
        }
    }

    private void fadeAnimation(){
        final CardView cardView = findViewById(R.id.main_myWoundsCard);

        AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.0f);

        alphaAnimation.setDuration(350);
        alphaAnimation.setRepeatCount(Animation.INFINITE);
        alphaAnimation.setRepeatMode(Animation.REVERSE);

        cardView.setAnimation(alphaAnimation);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.main_takePictureCard :
                startActivity(new Intent(MainActivity.this,
                        CameraActivity.class)
                .putExtra("action requested", Constants.REQUEST_CAMERA_ACTION));
                break;
            case R.id.main_myWoundsCard:
                startActivity(new Intent(MainActivity.this,
                        MyWoundsActivity.class));
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.menuAction_SignOut :
                // Sign the user out
                if (currentUser != null && firebaseAuth != null){
                    firebaseAuth.signOut();
                    startActivity(new Intent(MainActivity.this,
                            LoginActivity.class));
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart() {
        super.onStart();
        currentUser = firebaseAuth.getCurrentUser();
        firebaseAuth.addAuthStateListener(authStateListener);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (firebaseAuth != null){
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }

}
