package developer.nk.spplayer;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Binder;
import android.provider.CalendarContract;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import static android.content.Intent.getIntent;
import static android.content.Intent.makeMainActivity;

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
        ViewHolder holder = new ViewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        //Use the provided View Holder on the onCreateViewHolder method to populate the current row on the RecyclerView
        String text = (list.get(position).getArtist() +" - "+ list.get(position).getTitle());
        String duration = (list.get(position).getDuration());
        holder.title.setText(text);
        holder.duration.setText(duration);
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
            }
        });
        if (audioIndex!= -1) {
            if (position == audioIndex) {
                holder.title.setSingleLine(false);
                ////////////setting image
                InputStream is = null;
                final long identity = Binder.clearCallingIdentity();
                try {
                    ContentResolver contentResolver = InstanceOfMainActivity.mainActivity.getContentResolver();
                    Audio activeAudio = list.get(audioIndex);
                    is= contentResolver.openInputStream(Uri.parse(activeAudio.getArtPath()));
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    Binder.restoreCallingIdentity(identity);
                }
                Bitmap largeIcon= BitmapFactory.decodeStream(is);
                if (largeIcon == null){
                    largeIcon = BitmapFactory.decodeResource(InstanceOfMainActivity.mainActivity.getResources(),
                            R.drawable.image); }
                Bitmap scaled = Bitmap.createScaledBitmap(largeIcon, 100, 100,false);
                //palette
                Palette.from(largeIcon).generate( new Palette.PaletteAsyncListener() {
                    public void onGenerated(Palette palette) {
                        int colorLight = palette.getLightMutedColor(Color.GRAY);
                          // holder.mainL.setBackgroundColor(colorLight);
                           holder.item.setBackgroundColor(colorLight);
                        Log.i("color changed to "+ String.valueOf(colorLight), String.valueOf(position));

                    }
                });
                holder.imageView.setImageBitmap(scaled);
                holder.imageView.setVisibility(View.VISIBLE);
            } else {
                //revert back to single line
                holder.title.setSingleLine(true);
                //revert back to regular color
                holder.item.setBackgroundResource(R.color.colorPrimary);
                Log.i("color changed to " +String.valueOf(holder.item.getDrawingCacheBackgroundColor()), String.valueOf(position));
                //revert back to regular image
                holder.imageView.setImageBitmap(BitmapFactory.decodeResource(InstanceOfMainActivity.mainActivity.getResources(),
                        R.drawable.small));
                holder.imageView.setVisibility(View.INVISIBLE);
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
    TextView duration;
    RelativeLayout item;
    ImageView imageView;
    //LinearLayout mainL;
    ViewHolder(final View itemView) {
        super(itemView);
        title = itemView.findViewById(R.id.title);
        duration = itemView.findViewById(R.id.duration);
        item = itemView.findViewById(R.id.item);
        imageView = itemView.findViewById(R.id.imageView);
        //mainL = InstanceOfMainActivity.mainActivity.findViewById(R.id.main_play_menu_layout);



    }
}