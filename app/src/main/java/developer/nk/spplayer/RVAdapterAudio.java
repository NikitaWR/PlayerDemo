package developer.nk.spplayer;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Binder;
import android.support.annotation.NonNull;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class RVAdapterAudio extends RecyclerView.Adapter<ViewHolder> implements FastScrollRecyclerView.SectionedAdapter {

   private List<Audio> list;
   private Context context;
   private int audioIndex;




    public RVAdapterAudio(List<Audio> list, Context context) {
        this.list = list;
        this.context = context;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //Inflate the layout, initialize the View Holder
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_audio, parent, false);
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

    @NonNull
    @Override
    public String getSectionName(int position) {
        return String.valueOf(position+1);
    }
}

