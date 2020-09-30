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

/**
 * This class is accessed from MyWoundsActivity when the Register Wound Button is clicked.
 * The purpose of the class is to present the user who is registering a wound with a selection
 * of limbs to choose from. Users can select a limb by clicking one of the Buttons.
 *
 * @author David Farrelly
 */
public class LimbListActivity extends AppCompatActivity {

    /**
     * ConstraintLayout element that holds the front facing ImageView of the body
     */
    private ConstraintLayout frontLayout;

    /**
     * ConstraintLayout element that holds the back facing ImageView of the body
     */
    private ConstraintLayout backLayout;

    /**
     * LinearLayout that holds the confirmation text above the ImageView. This layout is displayed
     * when the user clicks a limb Button
     */
    private LinearLayout selectedLayout;

    /**
     * The TextView that is displayed above the ImageView when the activity starts. Requests that
     * the user selects a limb
     */
    private TextView titleTextView;

    /**
     * The TextView that is displayed in the selectedLayout when a user clicks a limb. Holds the
     * name of the limb selected
     */
    private TextView selectedTextView;

    /**
     * Whether or not the front facing ImageView is displayed. Set to true when activity starts
     */
    private boolean frontView = true;

    /**
     * The Button element to toggle to the front facing ImageView
     */
    private Button frontViewButton;

    /**
     * The Button element to toggle to the back facing ImageView
     */
    private Button backViewButton;

    /**
     * The limb Button that is currently selected by the user
     */
    private Button currentlySelected;

    /**
     * A reference to the name of the limb selected after the user clicks a limb Button
     */
    private String currentlySelectedText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_limb_list);
        // Create the toolbar and set the title
        Objects.requireNonNull(getSupportActionBar()).setTitle("Register Wound");
        // Set the toolbar back button to return to MyWoundsActivity
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Find the ConstraintLayout in the XML file that holds the front facing ImageView
        frontLayout = findViewById(R.id.limbList_FrontLayout);
        // Find the ConstraintLayout in the XML file that holds the back facing ImageView
        backLayout = findViewById(R.id.limbList_BackLayout);
        // Find the LinearLayout in the XML file that is displayed after the user selects a limb
        selectedLayout = findViewById(R.id.limbList_SelectedLimbLayout);
        // Find the TextView element in the XMl file that holds the initial title text
        titleTextView = findViewById(R.id.limbList_TitleTextView);
        // Find the TextView element in the XMl file that holds the name of the limb selected
        selectedTextView = findViewById(R.id.limbList_SelectedTextView);
        // Find the Button that toggles to the front facing ImageView in the XML file
        frontViewButton = findViewById(R.id.limbList_FrontButton);
        // As the front facing ImageView is displayed when the activity starts, the Front View
        // button should be displayed as selected. This is done here by drawing a line below the Button text
        frontViewButton.setPaintFlags(frontViewButton.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        // Find the Button that toggles to the back facing ImageView in the XML file
        backViewButton = findViewById(R.id.limbList_BackButton);
    }

    /**
     * This method is called when the user clicks a limb Button. First check if another Button has
     * already been clicked. Then set a currentlySelected Button that was just clicked.
     *
     * @param view - the limb Button that was clicked
     */
    public void selectLimb(View view){
        // Check if there is a Button previously clicked on by the user
        if (currentlySelected != null) {
            // If there is, reset the background of that previous button to the default style
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

    /**
     * This method is called from activity_limb_list.xml when the user clicks
     * the 'Back View' button. Method changes the image displayed to the user to
     * the back facing view
     *
     * @param view - the button that was clicked
     */
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

    /**
     * This method is called from activity_limb_list.xml when the user clicks
     * the 'Front View' button. Method changes the image displayed to the user to
     * the front facing view
     *
     * @param view - the button that was clicked
     */
    public void toggleToFront(View view) {
        // Check if the front view is currently being displayed
        if (!frontView) {
            // If not, user wants to change to the front view -- set frontView to true
            frontView = true;
            // Remove the underline from the back view button and replace on front view button
            frontViewButton.setPaintFlags(frontViewButton.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            backViewButton.setPaintFlags(backViewButton.getPaintFlags() & (~Paint.UNDERLINE_TEXT_FLAG));
            // Switch the color of the text on the buttons
            backViewButton.setTextColor(getResources().getColor(R.color.black));
            frontViewButton.setTextColor(getResources().getColor(R.color.colorAccent));
            // Display the front view layout and hide the back view layout
            frontLayout.setVisibility(View.VISIBLE);
            backLayout.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Method is called when the user clicks the Confirm text from activity_limb_list.xml, after
     * selecting a limb button. Creates a popup dialog box that asks the user to confirm their
     * selection once more
     *
     * @param view - the button that was clicked
     */
    public void confirmSelection(View view) {
        // Call this method to create the popup dialog
        // Pass in the message -- the message is concatenated with the name of the limb that the
        // user has selected
        // Also pass in a new DialogInterface.OnClickListener to listen for click events in the popup
        displayAlertMessage(
                "You have indicated that your wound is on your " + currentlySelectedText.toLowerCase() + ". Is this correct?",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == DialogInterface.BUTTON_POSITIVE) {
                            // Here if OK option clicked in popup
                            // Send user to camera under REQUEST_REGISTER_WOUND action and pass
                            // in the name of the limb they selected
                            Intent cameraIntent = new Intent(LimbListActivity.this,
                                    CameraActivity.class);
                            cameraIntent.putExtra("limb name", currentlySelectedText);
                            cameraIntent.putExtra("action requested", Constants.REQUEST_REGISTER_WOUND);
                            startActivity(cameraIntent);
                        }
                    }
                });
    }

    /**
     * Method that is used to build and display a popup dialog box
     *
     * @param message - the message to display to the user
     * @param onClickListener - listen for click events in the popup
     */
    public void displayAlertMessage(String message, DialogInterface.OnClickListener onClickListener) {
        // Create a new AlertDialog.Builder to build the dialog box
        // Set the message to display as well as the text to display on the button options
        new AlertDialog.Builder(LimbListActivity.this)
                .setMessage(message)
                .setPositiveButton("Confirm", onClickListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

}
