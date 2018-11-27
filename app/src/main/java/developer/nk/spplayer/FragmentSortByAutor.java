package developer.nk.spplayer;

import android.content.Context;
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


public class FragmentSortByAutor extends Fragment {
    View v;
    private RecyclerView recyclerView;
    private Context context;
    private RVAdapterArtist adapter;
    public FragmentSortByAutor() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.autorsort_fragment,container,false);
        return v;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        recyclerView = getView().findViewById(R.id.recyclerview);
       ArrayList <Audio> allAudio = InstanceOfMainActivity.mainActivity.getArray();
       ArrayList <Audio> artistList = new ArrayList<>();
        artistList.add(allAudio.get(0));
        boolean exist = false;
       for (int i =0; i< allAudio.size();i++){
           for (int n = 0; n< artistList.size();n++) {
               if (allAudio.get(i).getArtist().equals((artistList.get(n).getArtist()))) {
                   exist = true;
               }
           }
           if (!exist)
               artistList.add(allAudio.get(i));
           exist = false;
       }
        context = getContext();
        if (recyclerView != null){
            if (artistList.size() > 0) {
                adapter = new RVAdapterArtist(artistList,allAudio, context);
                recyclerView.setAdapter(adapter);
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
                recyclerView.setVisibility(View.VISIBLE);
                recyclerView.addOnItemTouchListener(new CustomTouchListener(getContext(), new onItemClickListener() {
                    @Override
                    public void onClick(View view, int index) {
                        //InstanceOfMainActivity.mainActivity.playAudio(index, artistList);
                    }
                }));
                //
            }
            else  Toast.makeText(context,"artistList =0",Toast.LENGTH_SHORT).show();
        }


    }
}
