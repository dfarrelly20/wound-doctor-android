package view;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.wounddoctor.MainActivity;
import com.example.wounddoctor.R;

import java.util.List;

import model.Limb;

public class LimbListRecyclerAdapter extends RecyclerView.Adapter<LimbListRecyclerAdapter.ViewHolder> {

    private Context context;
    private List<Limb> limbList;

    public LimbListRecyclerAdapter(Context context, List<Limb> limbList) {
        this.context = context;
        this.limbList = limbList;
    }

    @NonNull
    @Override
    public LimbListRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.limb_list_row, parent, false);
        return new ViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(@NonNull LimbListRecyclerAdapter.ViewHolder holder, int position) {

        Limb limb = limbList.get(position);
        String imageUrl;

        holder.limbId = limb.getLimbId();
        holder.limbName.setText(limb.getLimbName());

    }

    @Override
    public int getItemCount() {
        return limbList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        public String limbId;
        public TextView limbName;
        public Button selectButton;

        public ViewHolder(@NonNull View itemView, Context ctx) {
            super(itemView);
            context = ctx;

            limbName = itemView.findViewById(R.id.limbRow_LimbNameTextView);
            selectButton = itemView.findViewById(R.id.limbRow_Button);
            selectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    context.startActivity(new Intent(context, MainActivity.class));
                }
            });
        }
    }

}
