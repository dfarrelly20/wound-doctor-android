package com.example.wounddoctor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import util.PatientManager;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button captureButton;
    private Button myWoundsButton;
    private CardView takePictureCard;
    private CardView myWoundsCard;

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser currentUser;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = db.collection("patients");

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

                    collectionReference
                            .whereEqualTo("patientId", currentUserId)
                            .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                @Override
                                public void onEvent(@Nullable QuerySnapshot value,
                                                    @Nullable FirebaseFirestoreException error) {

                                    if (error != null){

                                    }

                                    String name;

                                    if (!value.isEmpty()){

                                        for (QueryDocumentSnapshot snapshot : value){

                                            PatientManager patientManager = PatientManager.getInstance();
                                            patientManager.setPatientId(currentUserId);
                                            patientManager.setPatientName(snapshot.getString("fname"));

                                        }

                                    }

                                }
                            });
                } else {
                    // Currently no user signed in
                    startActivity(new Intent(MainActivity.this,
                            LoginActivity.class));
                }
            }
        };

        takePictureCard = findViewById(R.id.main_takePictureCard);
        takePictureCard.setOnClickListener(this);
        myWoundsCard = findViewById(R.id.main_myWoundsCard);
        myWoundsCard.setOnClickListener(this);

        //captureButton = findViewById(R.id.main_TakePictureButton);
        //captureButton.setOnClickListener(this);
        //myWoundsButton = findViewById(R.id.main_MyWoundsButton);
        //myWoundsButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
//        switch (v.getId()){
//            case R.id.main_TakePictureButton :
//                startActivity(new Intent(MainActivity.this,
//                        CameraActivity.class));
//                break;
//            case R.id.main_MyWoundsButton :
//                startActivity(new Intent(MainActivity.this,
//                        LimbListActivity.class));
//                break;
//        }
        switch (v.getId()){
            case R.id.main_takePictureCard :
                startActivity(new Intent(MainActivity.this,
                        CameraActivity.class));
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
            case R.id.menuAction_Add :
                // Take user to take picture
                if (currentUser != null && firebaseAuth != null){
                    startActivity(new Intent(MainActivity.this,
                            CameraActivity.class));
                }
                break;
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
