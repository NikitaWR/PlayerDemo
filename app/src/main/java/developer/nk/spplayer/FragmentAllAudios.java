package developer.nk.spplayer;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Timer;

public class FragmentAllAudios extends Fragment {
    private RecyclerView recyclerView;
    private View v;
    private ArrayList<Audio> arrayList;
    private Context context;
    private RVAdapterAudio adapter;

    public FragmentAllAudios() {
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        v = inflater.inflate(R.layout.allaudios_fragment, container, false);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        recyclerView = getView().findViewById(R.id.recyclerview);
        arrayList = InstanceOfMainActivity.mainActivity.getArray();
        context = getContext();
        if (recyclerView != null&& arrayList !=null){
        if (arrayList.size() > 0) {
            adapter = new RVAdapterAudio(arrayList, context);
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            recyclerView.setVisibility(View.VISIBLE);
            recyclerView.addOnItemTouchListener(new CustomTouchListener(getContext(), new onItemClickListener() {
                @Override
                public void onClick(View view, int index) {
                    InstanceOfMainActivity.mainActivity.playAudio(index, arrayList);
                }
            }));
            // Toast.makeText(context,"new rv",Toast.LENGTH_SHORT).show();
        }
    }
}
    public void notifyDataSetChanged(){
        adapter.notifyDataSetChanged();
    }

    public void updateRV(final ArrayList updatedArr){
        //if (recyclerView != null) {
       // recyclerView = getView().findViewById(R.id.recyclerview);

            if (updatedArr.size() > 0) {
                adapter = new RVAdapterAudio(updatedArr, context);
                recyclerView.setAdapter(adapter);
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
               // recyclerView.setVisibility(View.VISIBLE);
               // Toast.makeText(context,"rv update",Toast.LENGTH_SHORT).show();
                recyclerView.addOnItemTouchListener(new CustomTouchListener(getContext(), new onItemClickListener() {
                    @Override
                    public void onClick(View view, int index) {
                        InstanceOfMainActivity.mainActivity.playAudio(index, updatedArr);
                    }
                }));
                // Toast.makeText(context,"new rv",Toast.LENGTH_SHORT).show();
            //}
        }
         else Toast.makeText(context,getResources().getString(R.string.nothing_found),Toast.LENGTH_SHORT).show();

    }

}
