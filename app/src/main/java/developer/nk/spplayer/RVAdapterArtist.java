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
import java.util.ArrayList;
import java.util.List;

public class RVAdapterArtist extends RecyclerView.Adapter<ViewHolder> implements FastScrollRecyclerView.SectionedAdapter {

    List<Audio> list;
    List<Audio> allAudios;
    Context context;

    public RVAdapterArtist (List<Audio> list, List<Audio> allAudios, Context context) {
        this.list = list;
        this.context = context;
        this.allAudios = allAudios;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //Inflate the layout, initialize the View Holder
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_artist, parent, false);
        ViewHolder holder = new ViewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        //Use the provided View Holder on the onCreateViewHolder method to populate the current row on the RecyclerView
        String text = (list.get(position).getArtist());
        holder.title.setText(text);
        ////////////setting image
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                loadArtistImage(position,holder);
            }
        });
        thread.start();
                /*//palette
                Palette.from(largeIcon).generate(new Palette.PaletteAsyncListener() {
                    public void onGenerated(Palette palette) {
                        int colorLight = palette.getLightMutedColor(Color.GRAY);
                        // holder.mainL.setBackgroundColor(colorLight);
                        holder.item.setBackgroundColor(colorLight);
                        Log.i("color changed to "+ String.valueOf(colorLight), String.valueOf(position));

                    }
                });*/
            }
    private void loadArtistImage(int position, final ViewHolder holder){
        InputStream is = null;
        final long identity = Binder.clearCallingIdentity();
        try {
            ContentResolver contentResolver = InstanceOfMainActivity.mainActivity.getContentResolver();
            is= contentResolver.openInputStream(Uri.parse(list.get(position).getArtPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            Binder.restoreCallingIdentity(identity);
        }
        Bitmap largeIcon= BitmapFactory.decodeStream(is);
        boolean succeed = true;
        if (largeIcon == null){
            //remember that image load was not succeed
             succeed = false;
            largeIcon = BitmapFactory.decodeResource(InstanceOfMainActivity.mainActivity.getResources(),
                    R.drawable.image); }
        final Bitmap scaled = Bitmap.createScaledBitmap(largeIcon, 100, 100,false);
        final boolean finalSucceed = succeed;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                onBitmapPrepared(scaled,holder, finalSucceed);
            }
        });
        InstanceOfMainActivity.mainActivity.runOnUiThread(thread);
        /*//try to find image again
        if (!succed){
        }*/
    }
    void onBitmapPrepared(Bitmap bitmap, ViewHolder holder, boolean succed){
        holder.imageView.setImageBitmap(bitmap);

    }
/*    private void setOnClickListener(ViewHolder holder, final ArrayList artistAudios){
        holder.itemArtist.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

            }
        });
    }*/
   /* private void loadArtistAudios(int position, ViewHolder holder){
        ArrayList<Audio> artistAudios = new ArrayList<>();
        for (int i = 0 ; i< allAudios.size(); i++){
            if (allAudios.get(i).getArtist().equals(list.get(0).getArtist()))
                artistAudios.add(artistAudios.get(i));
        }
        setOnClickListener(holder,artistAudios);
    }*/




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

/*
class ViewHolderArtist extends RecyclerView.ViewHolder {

    TextView title;
    TextView duration;
    RelativeLayout item;
    ImageView imageView;

    ViewHolderArtist(final View itemView) {
        super(itemView);
        title = itemView.findViewById(R.id.title);
        duration = itemView.findViewById(R.id.duration);
        item = itemView.findViewById(R.id.item);
        imageView = itemView.findViewById(R.id.imageView);

    }
}*/
