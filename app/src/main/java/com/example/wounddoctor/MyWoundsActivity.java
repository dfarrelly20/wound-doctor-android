package com.example.wounddoctor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import model.Wound;
import util.PatientManager;
import view.MyWoundsRecyclerAdapter;

/**
 * The MyWoundsActivity class acts as a section of the app that the patient can visit
 * to oversee the day-to-day management of their wounds. When the patient first registers for the
 * app, this is the first place they are brought.
 * <p></p>
 * The 'Register Wound' button can be used to open LimbListActivity.java and start the
 * Register Wound use case.
 * <p></p>
 * As the patient registers wounds, they are displayed via a RecyclerView. The wounds registered
 * to the patient are retrieved from Firestore and passed into MyWoundsRecyclerAdapter.java as
 * an ArrayList of wounds. MyWoundsRecyclerAdapter.java renders the data of each wound onto
 * template list items, that the RecyclerView used to create a list of wound via a LayoutManager.
 *
 * @author David Farrelly
 */
public class MyWoundsActivity extends AppCompatActivity {

    /**
     * Debug tag for this activity
     */
    private static final String TAG = "MyWoundsActivity";

    /**
     * The text that displays if there are no wounds registered to the patient who is signed in
     */
    private TextView noWoundsTextView;

    // private FirebaseAuth firebaseAuth;
    // private FirebaseUser currentUser;

    /**
     * Making a connection to Firebase Firestore
     */
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Creating a reference to the wounds collection in Firestore
     */
    private CollectionReference collectionReference = db.collection("wounds");

    /**
     * The ArrayList of the wounds registered to a patient
     */
    private List<Wound> woundList;

    /**
     * The RecyclerView element which displays the list of wounds
     */
    private RecyclerView recyclerView;

    /**
     * A reference to the RecyclerAdapter which is used to feed the data from the ArrayList of
     * wounds to the empty list items
     */
    private MyWoundsRecyclerAdapter woundsRecyclerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_wounds);
        // Creating the toolbar
        Objects.requireNonNull(getSupportActionBar()).setTitle("My Wounds");
        // Set back button on toolbar to return to MainActivity.java
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // firebaseAuth = FirebaseAuth.getInstance();
        // currentUser = firebaseAuth.getCurrentUser();

        // Find the noWoundsTextView element from activity_my_wounds.xml
        noWoundsTextView = findViewById(R.id.myWounds_NoWoundsTextView);
        // Initialise the list of wounds as a new ArrayList
        woundList = new ArrayList<>();
        // Find the RecyclerView element from activity_my_wounds.xml
        recyclerView = findViewById(R.id.myWounds_RecyclerView);
        // Optimise the RecyclerView by indicating that it is not affected by changes in the
        // Recycler Adapter
        recyclerView.setHasFixedSize(true);
        // Set the LayoutManager for the RecyclerView. The LayoutManager determines how the
        // template list items created by MyWoundsRecyclerAdapter.java will be displayed. In this
        // case, the list is displayed vertically via a LinearLayoutManager
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    /**
     * This method is called when the 'Register Wound' button is clicked from activity_my_wounds.xml
     *
     * @param view - the 'Register Wound' button that was clicked
     */
    public void registerWound(View view) {
        // Take the user to LimbListActivity.java to select a limb
        startActivity(new Intent(MyWoundsActivity.this,
                LimbListActivity.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Call the reference to the 'wounds' collection and make a query using the ID of the patient
        // who is currently signed in. This ID is retrieved from PatientManager
        collectionReference.whereEqualTo("userId",
                PatientManager.getInstance().getPatientId())
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    /**
                     * Here if query to collection was successful and the wounds registered to
                     * this patient have been found
                     *
                     * @param queryDocumentSnapshots - the result of the query
                     */
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        // Check that the patients wounds have been returned
                        if (!queryDocumentSnapshots.isEmpty()) {
                            // Loop through the wounds registered to the patient
                            for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                                // Create a Wound Object for each wound returned
                                Wound wound = snapshot.toObject(Wound.class);
                                // Add each wound to the ArrayList
                                woundList.add(wound);
                            }
                            // Instantiate MyWoundsRecyclerAdapter.java by passing in the context of
                            // the activity and the ArrayList of wounds
                            woundsRecyclerAdapter = new MyWoundsRecyclerAdapter(MyWoundsActivity.this, woundList);
                            // Set the adapter for the RecyclerView element
                            recyclerView.setAdapter(woundsRecyclerAdapter);
                            // This is used to notify the adapter of the wound list data changing, i.e.
                            // when a new wounds is added, or some of the data changes for a wound
                            woundsRecyclerAdapter.notifyDataSetChanged();
                        } else {
                            // Here if patient has no wounds currently registered
                            noWoundsTextView.setVisibility(View.VISIBLE);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Here if problem making call to wounds collection in Firestore
                        Log.d(TAG, "onFailure: " + e.toString());
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // When the user navigates away from MyWoundsActivity.java, clear the ArrayList of wounds
        // to free up resources
        woundList.clear();
    }

}
