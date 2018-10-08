package com.example.nikita.playerdemo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

public class RecyclerView_Adapter extends RecyclerView.Adapter<ViewHolder> {

    List<Audio> list = Collections.emptyList();
    Context context;
    int audioIndex;




    public RecyclerView_Adapter(List<Audio> list, Context context) {
        this.list = list;
        this.context = context;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //Inflate the layout, initialize the View Holder
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false);
        ViewHolder holder = new ViewHolder(v,context);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        //Use the provided View Holder on the onCreateViewHolder method to populate the current row on the RecyclerView
        String text = (list.get(position).getArtist() +" - "+ list.get(position).getTitle());
        holder.title.setText(text);
        try {
        audioIndex = new StorageUtil(context).loadAudioIndex();
        }
        catch (Exception e){e.printStackTrace();
        audioIndex = -1;
        }

        holder.item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                audioIndex = position;
               // notifyDataSetChanged();
            }
        });
        if (audioIndex!= -1) {
            if (position == audioIndex) {
                //change color like
                holder.item.setBackgroundResource(R.color.cardview_dark_background);
            } else {
                //revert back to regular color
                holder.item.setBackgroundResource(R.color.cardview_light_background);
            }
        }

    }

    @Override
    public int getItemCount() {
        //returns the number of elements the RecyclerView will display
        return list.size();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

}

class ViewHolder extends RecyclerView.ViewHolder {

    TextView title;
    LinearLayout item;
    ViewHolder(final View itemView, Context context) {
        super(itemView);
        title = itemView.findViewById(R.id.title);
        item = itemView.findViewById(R.id.item);

    }
}