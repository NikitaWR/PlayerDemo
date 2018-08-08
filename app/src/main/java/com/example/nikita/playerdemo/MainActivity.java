package com.example.nikita.playerdemo;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.internal.BottomNavigationMenu;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.Toolbar;
import android.widget.MediaController.MediaPlayerControl;

import java.util.ArrayList;
import java.util.List;

import javax.xml.datatype.Duration;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener, MediaPlayerControl{
    private MediaPlayerService player;
    boolean serviceBound = false;
    public static final String Broadcast_PLAY_NEW_AUDIO = "com.example.nikita.playerdemo";
    public static final String Broadcast_PAUSE_AUDIO = "com.example.nikita.playerdemo";
    public static final String Broadcast_RESUME_AUDIO = "com.example.nikita.playerdemo";
    ArrayList<Audio> audioList;
    private MusicController controller;
    RecyclerView recyclerView;
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()){
            case R.id.play_song:
//todo
            case R.id.pause_song:

                pauseAudio();
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadAudio();
        initRecyclerView();

        BottomNavigationView myToolbar = (BottomNavigationView) findViewById(R.id.navigationView);
        myToolbar.setOnNavigationItemSelectedListener(this);

        setController();






    }
    private void initRecyclerView() {
        if (audioList.size() > 0) {
            recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
            RecyclerView_Adapter adapter = new RecyclerView_Adapter(audioList, getApplication());
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.addOnItemTouchListener(new CustomTouchListener(this, new onItemClickListener() {
                @Override
                public void onClick(View view, int index) {
                    playAudio(index);
                    controller.show();
                }
            }));

        }
    }
    //Binding this Client to the AudioPlayer Service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            player = binder.getService();
            serviceBound = true;

            Toast.makeText(MainActivity.this, "Service Bound", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };
    private void playAudio(int audioIndex) {
        //Check is service is active

        if (!serviceBound) {
            //Store Serializable audioList to SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudio(audioList);
            storage.storeAudioIndex(audioIndex);

            Intent playerIntent = new Intent(this, MediaPlayerService.class);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            //Store the new audioIndex to SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudioIndex(audioIndex);

            //Service is active
            //Send a broadcast to the service -> PLAY_NEW_AUDIO
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        }
    }


    //retrieves the data from the device in ascending order
    private void loadAudio() {
        ContentResolver contentResolver = getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cursor = contentResolver.query(uri, null, selection, null, sortOrder);

        if (cursor != null && cursor.getCount() > 0) {
            audioList = new ArrayList<>();
            while (cursor.moveToNext()) {
                String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));

                // Save to audioList
                audioList.add(new Audio(data, title, album, artist));
            }
        }
        cursor.close();
    }
    /**menu*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /** life-cycle methods*/

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("ServiceState", serviceBound);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        serviceBound = savedInstanceState.getBoolean("ServiceState");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceBound) {
            unbindService(serviceConnection);
            //service is active
            player.stopSelf();
        }
    }

/**methods for MediaController*/

private void setController(){
    controller = new MusicController(this);
    controller.setMediaPlayer(this);
    controller.setAnchorView(recyclerView);
    controller.setEnabled(true);






    controller.setPrevNextListeners(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            player.playNext();

        }
    },
            new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            player.playPrev();

        }
    });

}

    @Override
    public void start() {
        if (serviceBound)
            player.go();
    }

    @Override
    public void pause() {
        if (serviceBound)
            player.pausePlayer();
    }

    @Override
    public int getDuration() {
        if (serviceBound){
            return player.getDur();
        }
        else
            return 0;
    }

    @Override
    public int getCurrentPosition() {
    if (serviceBound){
        return player.getPosn();
    }
    else
        return 0;
    }

    @Override
    public void seekTo(int pos) {
        if (serviceBound)
            player.seek(pos);


    }

    @Override
    public boolean isPlaying() {
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

}
