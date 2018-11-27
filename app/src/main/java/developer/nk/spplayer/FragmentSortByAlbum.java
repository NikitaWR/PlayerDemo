package developer.nk.spplayer;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;

public class FragmentSortByAlbum extends Fragment {
    View v;
    Context context;
    RecyclerView recyclerView;
    RVAdapterAlbum adapter;
    ArrayList <Audio> albumList;
    private ArrayList<Audio> allAudio;
    public FragmentSortByAlbum() {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.albumsort_fragment,container,false);
        return v;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        recyclerView = getView().findViewById(R.id.recyclerviewAlbum);
        allAudio = InstanceOfMainActivity.mainActivity.getArray();
        albumList = new ArrayList<>();
        albumList.add(allAudio.get(0));
        boolean exist = false;
        for (int i =0; i< allAudio.size();i++){
            for (int n = 0; n< albumList.size();n++) {
                if (allAudio.get(i).getAlbum().equals((albumList.get(n).getAlbum()))) {
                    exist = true;
                }
            }
            if (!exist)
                albumList.add(allAudio.get(i));
            exist = false;
        }
        context = getContext();
        if (recyclerView != null){
            if (albumList.size() > 0) {
                adapter = new RVAdapterAlbum(albumList, context);
                recyclerView.setAdapter(adapter);
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
                recyclerView.setVisibility(View.VISIBLE);
                recyclerView.addOnItemTouchListener(new CustomTouchListener(getContext(), new onItemClickListener() {
                    @Override
                    public void onClick(View view, int index) {
                        showAlbumAudio(albumList, index);
                    }
                }));
            }
            else  Toast.makeText(context,"artistList =0",Toast.LENGTH_SHORT).show();
        }
    }
    private void showAlbumAudio(ArrayList<Audio> albumList, int index){
        ArrayList<Audio> albumAudios = new ArrayList<>();
        for (int i = 0 ; i< allAudio.size(); i++){
            if (allAudio.get(i).getAlbum().equals(albumList.get(index).getAlbum()))
                albumAudios.add(allAudio.get(i));
        }
        startActivityAudio(index);
    }
    private void startActivityAudio(int index){
        Intent intent = new Intent(context,AudioGroupActivity.class);
        intent.putExtra("Title",String.valueOf(albumList.get(index).getAlbum()));
        startActivity(intent);

    }

    /*private void updateRV(final ArrayList updatedArr){
        if (updatedArr.size() > 0) {
            RVAdapterAudio adapter = new RVAdapterAudio(updatedArr, context);
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            recyclerView.addOnItemTouchListener(new CustomTouchListener(getContext(), new onItemClickListener() {
                @Override
                public void onClick(View view, int index) {
                   InstanceOfMainActivity.mainActivity.playAudio(index, updatedArr);
                }
            }));

        }
        else Toast.makeText(context,getResources().getString(R.string.nothing_found),Toast.LENGTH_SHORT).show();

    }*/
}
