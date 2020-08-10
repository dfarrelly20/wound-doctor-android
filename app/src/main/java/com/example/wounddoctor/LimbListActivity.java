package com.example.wounddoctor;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import model.Limb;
import util.Constants;
import view.LimbListRecyclerAdapter;

public class LimbListActivity extends AppCompatActivity implements View.OnClickListener {

    // private FirebaseFirestore db = FirebaseFirestore.getInstance();
    // private StorageReference storageReference;
    // private CollectionReference collectionReference = db.collection("limbs");

    // private List<Limb> limbList;
    // private RecyclerView recyclerView;
    // private LimbListRecyclerAdapter recyclerAdapter;

    // private final int REQUEST_WOUND_ACTIVITY = 2;

    private ConstraintLayout frontLayout;
    private ConstraintLayout backLayout;
    // private ConstraintLayout selectedLayout;
    private LinearLayout selectedLayout;

    private TextView titleTextView;
    private TextView selectedTextView;
    private TextView confirmTextView;

    private ImageView selectedImageView;

    private boolean frontView = true;
    private Button frontViewButton;
    private Button backViewButton;
    private ImageButton confirmImageButton;

    private Button head;
    private Button neck;

    private Button currentlySelected;
    private String currentlySelectedText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_limb_list);

        Objects.requireNonNull(getSupportActionBar()).setTitle("Register Wound");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        frontLayout = findViewById(R.id.limbList_FrontLayout);
        backLayout = findViewById(R.id.limbList_BackLayout);
        // selectedLayout = findViewById(R.id.limbList_SelectedLayout);
        selectedLayout = findViewById(R.id.limbList_SelectedLimbLayout);

        titleTextView = findViewById(R.id.limbList_TitleTextView);
        selectedTextView = findViewById(R.id.limbList_SelectedTextView);

        frontViewButton = findViewById(R.id.limbList_FrontButton);
        frontViewButton.setPaintFlags(frontViewButton.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        frontViewButton.setOnClickListener(this);
        backViewButton = findViewById(R.id.limbList_BackButton);
        backViewButton.setOnClickListener(this);

        head = findViewById(R.id.limbList_Head);
        head.setOnClickListener(this);
        neck = findViewById(R.id.limbList_Neck);
        neck.setOnClickListener(this);

        //currentlySelected = null;

        // limbList = new ArrayList<>();

        // recyclerView = findViewById(R.id.limbList_RecyclerView);
        // recyclerView.setHasFixedSize(true);
        // recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.limbList_FrontButton:
                toggleToFront();
                break;
            case R.id.limbList_BackButton:
                toggleToBack();
                break;
            case R.id.limbList_Head:
                updateUI(head, Limb.HEAD);
                break;
            case R.id.limbList_Neck:
                updateUI(neck, Limb.NECK);
                break;
        }
    }

    private void updateUI(Button button, Limb limb) {

        String limbText = limb.toString().toLowerCase();

        if (currentlySelected != null) {
            currentlySelected.setBackground(getDrawable(R.drawable.round_button));
        }

        currentlySelected = button;
        currentlySelectedText = limbText;
        button.setBackground(getDrawable(R.drawable.round_button_selected));
        titleTextView.setVisibility(View.INVISIBLE);
        selectedLayout.setVisibility(View.VISIBLE);
        selectedTextView.setText(limbText + " selected.");

    }

    public interface LimbSelector{
        void updateUI();
    }

    private void toggleToBack() {

        if (frontView) {
            frontView = false;
            backViewButton.setPaintFlags(backViewButton.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            frontViewButton.setPaintFlags(frontViewButton.getPaintFlags() & (~Paint.UNDERLINE_TEXT_FLAG));
            backViewButton.setTextColor(getResources().getColor(R.color.colorAccent));
            frontViewButton.setTextColor(getResources().getColor(R.color.black));
            backLayout.setVisibility(View.VISIBLE);
            frontLayout.setVisibility(View.INVISIBLE);
        }

    }

    private void toggleToFront() {

        if (!frontView) {
            frontView = true;
            frontViewButton.setPaintFlags(frontViewButton.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            backViewButton.setPaintFlags(backViewButton.getPaintFlags() & (~Paint.UNDERLINE_TEXT_FLAG));
            backViewButton.setTextColor(getResources().getColor(R.color.black));
            frontViewButton.setTextColor(getResources().getColor(R.color.colorAccent));
            frontLayout.setVisibility(View.VISIBLE);
            backLayout.setVisibility(View.INVISIBLE);
        }

    }

    public void confirmSelection(View view) {

        displayAlertMessage(
                "You have indicated that your wound is on your " + currentlySelectedText.toLowerCase() + ". Is this correct?",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                Intent cameraIntent = new Intent(LimbListActivity.this,
                                        CameraActivity.class);
                                cameraIntent.putExtra("limb name", currentlySelectedText);
                                cameraIntent.putExtra("action requested", Constants.REQUEST_REGISTER_WOUND);
                                startActivity(cameraIntent);
                                break;
                        }
                    }
                });

    }

    public void displayAlertMessage(String message, DialogInterface.OnClickListener onClickListener) {
        new AlertDialog.Builder(LimbListActivity.this)
                .setMessage(message)
                .setPositiveButton("Confirm", onClickListener)
                .setNegativeButton("Cancel", null).create()
                .show();
    }

    public enum Limb {
        HEAD,
        NECK
    }

}
