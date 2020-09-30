package view;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wounddoctor.CameraActivity;
import com.example.wounddoctor.R;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import model.Wound;
import util.Constants;
import util.PatientManager;

/**
 * The MyWoundsRecyclerAdapter forms the connection that allows wound data from Firestore to be
 * rendered in a list to the user in MyWoundsActivity.java.
 *
 * The wound data is retrieved in MyWoundsActivity.java using a reference to the 'wounds' collection
 * in Firestore. This data is stored in as an ArrayList of wounds, where it is used in this class
 * to populate empty list rows with the data for each wound.
 *
 * The template for each list item is built from the elements in the 'my_wounds_row.xml' resource
 * file. The inner class, ViewHolder, gets the elements that make up this template and uses them to
 * create empty list items in the method onCreateViewHolder(). The empty list items are then
 * populated with the data from the ArrayList of wounds in the method onBindViewHolder().
 *
 * @author David Farrelly
 */
public class MyWoundsRecyclerAdapter extends RecyclerView.Adapter<MyWoundsRecyclerAdapter.ViewHolder> {

    /**
     * The context of the activity that calls this adapter
     */
    private Context context;

    /**
     * The list of wounds registered to the user who is signed in
     * Retrieved in MyWoundsActivity.java from the 'wounds' collection
     */
    private List<Wound> woundList;

    /**
     * The constructor that is called to create this adapter
     *
     * @param context - the context of the activity
     * @param woundList - the ArrayList of wounds
     */
    public MyWoundsRecyclerAdapter(Context context, List<Wound> woundList) {
        this.context = context;
        this.woundList = woundList;
    }

    /**
     * This method is where the empty list items are created using the ViewHolder class prior to
     * being populated with data. The template for the list items comes from my_wounds_row.xml
     *
     * @param parent
     * @param viewType
     * @return - ViewHolder - the empty list item to be populated with the next data from the ArrayList
     */
    @NonNull
    @Override
    public MyWoundsRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Create a View for the list item by inflating the template my_wounds_row
        View view = LayoutInflater.from(context).inflate(R.layout.my_wounds_row, parent, false);
        // Cast the created View as a ViewHolder
        return new ViewHolder(view, context);
    }

    /**
     * This method populates the empty list items as they are created by ViewHolder with the data
     * retrieved from the RecyclerAdapter.
     *
     * @param holder - the ViewHolder that creates the template for each new list item
     * @param position -  the position of the item in the list
     */
    @Override
    public void onBindViewHolder(@NonNull MyWoundsRecyclerAdapter.ViewHolder holder, int position) {
        // Get the position of the item in the list and use this to find the wound data in the
        // ArrayList
        Wound wound = woundList.get(position);
        // Use this to get the ID of the patient who is signed into the app
        PatientManager currentUser = PatientManager.getInstance();
        // Create a Calendar object
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        // Get the date that this wound last had a bandage change
        cal.setTimeInMillis(wound.getBandageLastChanged().getSeconds() * 1000);
        // Format the date of the bandage change for displaying in the list item
        String dateChanged = DateFormat.format("EE, dd MMM", cal).toString();
        // Get the date of the last health check for this wound
        cal.setTimeInMillis(wound.getNextHealthCheck().getSeconds() * 1000);
        // Format the date of the last health check for displaying in the list item
        String nextHealthCheck = DateFormat.format("h:mma, EE, dd MMM", cal).toString();

        // Checking if the wound in this position of the ArrayList is in danger
        if (wound.isWoundInDanger()){
            // Here if wound is in danger
            // Set the TextView element accordingly
            holder.condition.setText(R.string.adapter_condition_danger);
            holder.condition.setTextColor(Color.RED);
        } else {
            // Here if wound is not in danger
            // Set the TextView element accordingly
            holder.condition.setText(R.string.adapter_condition_OK);
            holder.condition.setTextColor(Color.GREEN);
        }

        // Setting the limb name for the empty list item
        holder.limbName.setText(wound.getLimbName());
        // Setting the bandage last changed date for the empty list item
        holder.bandageChanged.append(dateChanged);
        // Setting the date of the next health check for the empty list item
        holder.nextHealthCheck.append(nextHealthCheck);
        // Setting the patient ID who signed in
        holder.userId = currentUser.getPatientId();
        // Setting the ID of the wound for the list item.
        // This is important as when the 'Change Bandage' button is clicked, the ID of the wound
        // is passed to the camera as extra data
        holder.woundId = wound.getWoundId();
        // Setting the ID of the bandage for the empty list item
        holder.bandageId = wound.getBandageId();
    }

    /**
     * Return the size of the ArrayList of wounds
     * @return - the size of the ArrayList
     */
    @Override
    public int getItemCount() {
        return woundList.size();
    }

    /**
     * This inner class holds the Views, created from the template my_wounds_row.xml file,
     * that will be used to create empty list items in the RecyclerView. These empty Views
     * are populated with data from the RecyclerView.Adapter and recycled to the user interface
     * in MyWoundsActivity.java using the LayoutManager
     */
    public class ViewHolder extends RecyclerView.ViewHolder {
        // A holder for the TextView elements from the my_wounds_row.xml
        // Will display the condition (of the wound), the date that the bandage was last
        // changed and the date of the next health check for the wound an the name of the limb
        public TextView condition, limbName, bandageChanged, nextHealthCheck;
        // A holder for the ID of the wound. This will be important for passing into
        // CameraActivity.java to indicated which wound is being imaged for a bandage change
        public String woundId;
        // A holder for the ID of the user currently signed
        public String userId;
        // A holder for the ID of the bandage that is on the wound
        public String bandageId;
        // A holder for the 'Change Bandage' button from the my_wounds_row.xml template
        public TextView changeBandageButton;

        /**
         * Constructor for the ViewHolder class
         *
         * @param itemView - the my_wounds_row.xml template
         * @param ctx -  the context of the activity
         */
        public ViewHolder(@NonNull View itemView, Context ctx) {
            super(itemView);
            // Set the context of the activity that is calling MyWoundsRecyclerAdapter.java
            context = ctx;
            // Find the condition TextView element in the my_wounds_row.xml file
            condition = itemView.findViewById(R.id.myWoundsRow_ConditionTextView);
            // Find the limb name TextView element in the my_wounds_row.xml file
            limbName = itemView.findViewById(R.id.myWoundsRow_LimbNameTextView);
            // Find the bandage changed date TextView element in the my_wounds_row.xml file
            bandageChanged = itemView.findViewById(R.id.myWoundsRow_DateTextView);
            // Find the next health check date TextView element in the my_wounds_row.xml file
            nextHealthCheck = itemView.findViewById(R.id.myWoundsRow_NextCheckTextView);
            // Find the 'Change Bandage' TextView Button in the my_wounds_row.xml file
            changeBandageButton = itemView.findViewById(R.id.myWoundsRow_ChangeTextView);
            // Listen for events on the 'Change Bandage' button
            changeBandageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // When one of the 'Change Bandage' buttons is clicked, open the camera
                    // under the REQUEST_UPDATE_BANDAGE action. Importantly, the ID of the
                    // wound that was clicked is passed in so that the app knows which wound is
                    // having a bandage change
                    context.startActivity(new Intent(context,
                            CameraActivity.class)
                            .putExtra("action requested", Constants.REQUEST_UPDATE_BANDAGE)
                            .putExtra("woundId", woundId));
                }
            });
        }
    }
}
