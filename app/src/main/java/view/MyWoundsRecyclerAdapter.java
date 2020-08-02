package view;

import android.content.Context;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wounddoctor.R;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import model.Wound;
import util.PatientManager;

public class MyWoundsRecyclerAdapter extends RecyclerView.Adapter<MyWoundsRecyclerAdapter.ViewHolder> {

    private Context context;
    private List<Wound> woundList;

    public MyWoundsRecyclerAdapter(Context context, List<Wound> woundList) {
        this.context = context;
        this.woundList = woundList;
    }

    @NonNull
    @Override
    public MyWoundsRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.my_wounds_row, parent, false);
        return new ViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(@NonNull MyWoundsRecyclerAdapter.ViewHolder holder, int position) {

        Wound wound = woundList.get(position);
        PatientManager currentUser = PatientManager.getInstance();

        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(wound.getDate().getSeconds() * 1000);
        // String dateChanged = DateFormat.format("dd-MM-yyyy", cal).toString();
        String dateChanged = DateFormat.format("EEEE, dd MMMM", cal).toString();

        holder.limbName.setText(wound.getLimbName());
        holder.date.append(dateChanged);
        holder.userId = currentUser.getPatientId();
        holder.woundId = wound.getWoundId();
        holder.bandageId = wound.getBandageId();

    }

    @Override
    public int getItemCount() {
        return woundList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        public TextView limbName, date;
        public String woundId;
        public String userId;
        public String bandageId;

        public ViewHolder(@NonNull View itemView, Context ctx) {
            super(itemView);

            context = ctx;

            limbName = itemView.findViewById(R.id.myWoundsRow_LimbNameTextView);
            date = itemView.findViewById(R.id.myWoundsRow_DateTextView);
        }

    }
}
