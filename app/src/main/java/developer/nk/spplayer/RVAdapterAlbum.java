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
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class RVAdapterAlbum extends RecyclerView.Adapter<ViewHolder> implements FastScrollRecyclerView.SectionedAdapter {

    List<Audio> list;
    Context context;

    public RVAdapterAlbum (List<Audio> list, Context context) {
        this.list = list;
        this.context = context;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //Inflate the layout, initialize the View Holder
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_album, parent, false);
        ViewHolder holder = new ViewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        //Use the provided View Holder on the onCreateViewHolder method to populate the current row on the RecyclerView
        String text = (list.get(position).getAlbum());
        String autor = list.get(position).getArtist();
        holder.title.setText(text);
        holder.autor.setText(autor);
        ////////////setting image
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                loadAlbumImage(position,holder);
            }
        });
        thread.start();
    }
   private void loadAlbumImage(int position, final ViewHolder holder){
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
        if (largeIcon == null){
            Log.i("ccc",list.get(position).getArtPath());
            largeIcon = BitmapFactory.decodeResource(InstanceOfMainActivity.mainActivity.getResources(),
                    R.drawable.image); }
        final Bitmap scaled = Bitmap.createScaledBitmap(largeIcon, 100, 100,false);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                onBitmapPrepared(scaled,holder);
            }
        });
        InstanceOfMainActivity.mainActivity.runOnUiThread(thread);
    }
    void onBitmapPrepared(Bitmap bitmap, ViewHolder holder){
        holder.imageView.setImageBitmap(bitmap);
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

/*
class ViewHolderAlbum extends RecyclerView.ViewHolder {

    TextView title;
    TextView duration;
    RelativeLayout item;
    ImageView imageView;

    ViewHolderAlbum(final View itemView) {
        super(itemView);
        title = itemView.findViewById(R.id.title);
        duration = itemView.findViewById(R.id.duration);
        item = itemView.findViewById(R.id.item);
        imageView = itemView.findViewById(R.id.imageView);

    }
}*/
