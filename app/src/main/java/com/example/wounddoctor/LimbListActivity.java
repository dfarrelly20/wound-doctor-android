package com.example.wounddoctor;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Objects;
import util.Constants;

public class LimbListActivity extends AppCompatActivity {

    private ConstraintLayout frontLayout;
    private ConstraintLayout backLayout;
    private LinearLayout selectedLayout;

    private TextView titleTextView;
    private TextView selectedTextView;

    private boolean frontView = true;
    private Button frontViewButton;
    private Button backViewButton;

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
        selectedLayout = findViewById(R.id.limbList_SelectedLimbLayout);

        titleTextView = findViewById(R.id.limbList_TitleTextView);
        selectedTextView = findViewById(R.id.limbList_SelectedTextView);

        frontViewButton = findViewById(R.id.limbList_FrontButton);
        frontViewButton.setPaintFlags(frontViewButton.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        backViewButton = findViewById(R.id.limbList_BackButton);
    }

    public void selectLimb(View view){
        // Check if there is a button previously clicked on by the user
        if (currentlySelected != null) {
            // If there is, reset the background of that button to the default style
            currentlySelected.setBackground(getDrawable(R.drawable.round_button));
        }
        // Cast the view that the user clicked to a button
        currentlySelected = (Button) view;
        // Change the background of the view to the selected style
        currentlySelected.setBackground(getDrawable(R.drawable.round_button_selected));
        // Get the xml tag from the view element
        currentlySelectedText = view.getTag().toString();
        // Hide the title 'Where is your located?'
        titleTextView.setVisibility(View.INVISIBLE);
        // Display the layout that holds the confirmation text
        selectedLayout.setVisibility(View.VISIBLE);
        // Concatenate the name of the selected limb to the confirmation text
        selectedTextView.setText(currentlySelectedText + " selected.");
    }

    public void toggleToBack(View view) {
        // Check if the front view is currently being displayed
        if (frontView) {
            // If so, user wants to change to the back view -- set frontView to false
            frontView = false;
            // Remove the underline from the front view button and replace on back view button
            backViewButton.setPaintFlags(backViewButton.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            frontViewButton.setPaintFlags(frontViewButton.getPaintFlags() & (~Paint.UNDERLINE_TEXT_FLAG));
            // Switch the color of the text on the buttons
            backViewButton.setTextColor(getResources().getColor(R.color.colorAccent));
            frontViewButton.setTextColor(getResources().getColor(R.color.black));
            // Display the back view layout and hide the front view layout
            backLayout.setVisibility(View.VISIBLE);
            frontLayout.setVisibility(View.INVISIBLE);
        }
    }

    public void toggleToFront(View view) {

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

}
