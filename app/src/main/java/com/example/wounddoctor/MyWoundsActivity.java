package com.example.wounddoctor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import model.Wound;
import util.PatientManager;
import view.MyWoundsRecyclerAdapter;

public class MyWoundsActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MyWoundsActivity";

    private TextView noWoundsTextView;
    private Button registerWoundBandage;

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser currentUser;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = db.collection("wounds");

    private List<Wound> woundList;
    private RecyclerView recyclerView;
    private MyWoundsRecyclerAdapter woundsRecyclerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_wounds);

        Objects.requireNonNull(getSupportActionBar()).setTitle("My Wounds");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        noWoundsTextView = findViewById(R.id.myWounds_NoWoundsTextView);
        // registerWoundBandage = findViewById(R.id.myWounds_RegisterButton);
        // registerWoundBandage.setOnClickListener(this);

        woundList = new ArrayList<>();

        recyclerView = findViewById(R.id.myWounds_RecyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
//            case R.id.myWounds_RegisterButton:
//                startActivity(new Intent(MyWoundsActivity.this,
//                        LimbListActivity.class));
//                break;
//            case R.id.myWoundsRow_ChangeTextView:
//                startActivity(new Intent(MyWoundsActivity.this,
//                        CameraActivity.class));
//                break;
        }
    }

//    public void changeBandage(View view){
//        startActivity(new Intent(MyWoundsActivity.this,
//                        CameraActivity.class));
//    }

    public void registerWound(View view){
        startActivity(new Intent(MyWoundsActivity.this,
                        LimbListActivity.class));
    }

    @Override
    protected void onStart() {
        super.onStart();

        collectionReference.whereEqualTo("userId",
                PatientManager.getInstance().getPatientId())
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                        if (!queryDocumentSnapshots.isEmpty()){

                            for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots){

                                Wound wound = snapshot.toObject(Wound.class);
                                woundList.add(wound);

                                Log.d(TAG, "wound id: " + wound.getWoundId());
                                Log.d(TAG, "bandage id: " + wound.getBandageId());

                            }

                            woundsRecyclerAdapter = new MyWoundsRecyclerAdapter(MyWoundsActivity.this, woundList);
                            recyclerView.setAdapter(woundsRecyclerAdapter);
                            woundsRecyclerAdapter.notifyDataSetChanged();

                        } else {
                            noWoundsTextView.setVisibility(View.VISIBLE);
                        }

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();

        woundList.clear();
    }

}
