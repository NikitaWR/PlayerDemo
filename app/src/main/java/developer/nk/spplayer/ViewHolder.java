package developer.nk.spplayer;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

   // one lazy view holder for every view ¯\_(ツ)_/¯
    class ViewHolder extends RecyclerView.ViewHolder {

        TextView title;
        TextView autor;
        TextView duration;
        RelativeLayout item;
        RelativeLayout itemArtist;
        ImageView imageView;

        ViewHolder(final View itemView) {
            super(itemView);
            autor = itemView.findViewById(R.id.autor);
            title = itemView.findViewById(R.id.title);
            duration = itemView.findViewById(R.id.duration);
            item = itemView.findViewById(R.id.item);
            itemArtist= itemView.findViewById(R.id.itemArtist);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }

