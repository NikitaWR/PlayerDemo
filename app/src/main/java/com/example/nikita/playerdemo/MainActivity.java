package com.example.nikita.playerdemo;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements  MediaPlayerControl {
    private MediaPlayerService player;
    boolean serviceBound = false;
    private static final int MY_PERMISSIONS_READ_EXTERNAL_STORAGE = 1;
    public static final String Broadcast_PLAY_NEW_AUDIO = "com.example.nikita.playerdemo";
    ArrayList<Audio> audioList;
    RecyclerView recyclerView;
    TextView mTitleTextView;
    SeekBar seekBar;
    ActionBar mActionBar;
    Runnable runnable;
    Handler handler;
    ImageButton play;
    ImageButton previous;
    ImageButton next;
    TextView songCurrentDuration;
    TextView songTotalDuration;
    SQLAdapter dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        checkPermissions();

        mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowTitleEnabled(false);
        LayoutInflater mInflater = LayoutInflater.from(this);
        View mCustomView = mInflater.inflate(R.layout.title_view, null);
        mTitleTextView = mCustomView.findViewById(R.id.title);
        mActionBar.setCustomView(mCustomView);
        mActionBar.setDisplayShowCustomEnabled(true);
        mActionBar.hide();


        recyclerView = findViewById(R.id.recyclerview);
        Piotrek.mainActivity = this;
    }

    void setActionBarText(String myText) {
        mTitleTextView.setText(myText);
    }

    private void initRecyclerView() {
        if (audioList.size() > 0) {
            RecyclerView_Adapter adapter = new RecyclerView_Adapter(audioList, getApplication());
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.addOnItemTouchListener(new CustomTouchListener(this, new onItemClickListener() {
                @Override
                public void onClick(View view, int index) {
                    playAudio(index);
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

            initSeekBar();
            initPlayMenu();
            LinearLayout playMenu = findViewById(R.id.play_menu);
            playMenu.setVisibility(View.VISIBLE);

            handler = new Handler();
            seekPosition();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    private void playAudio(int audioIndex) {
        //Check is service is active
        if (!mActionBar.isShowing())
            mActionBar.show();

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
            storage.storeAudio(audioList);
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
            creatDB();
            while (cursor.moveToNext()) {
                String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                dbHelper.createAudio(data, title, album, artist);
            }

        }
        cursor.close();
    }

    //writes data to DB
    void creatDB() {
        dbHelper = new SQLAdapter(this);
        dbHelper.open();
    }

    //load audio from db
    void loadData() {
        audioList = new ArrayList<>();
        Cursor cursor = dbHelper.fetchAllClients();
        while (cursor.moveToNext()) {
            Audio audio = new Audio((cursor.getString(
                    cursor.getColumnIndex("data"))),
                    cursor.getString(cursor.getColumnIndex("title")),
                    cursor.getString(cursor.getColumnIndex("album")),
                    cursor.getString(cursor.getColumnIndex("artist"))
            );
            audioList.add(audio);
        }
        initRecyclerView();
        cursor.close();
    }

    /**
     * menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.order_id_asc:
                dbHelper.setOrderBy(SQLAdapter.Audios.COLUMN_NAME_TITLE + " ASC");
                loadData();
                initRecyclerView();
                return true;

            case R.id.order_id_desc:
                dbHelper.setOrderBy(SQLAdapter.Audios.COLUMN_NAME_TITLE + " DESC");
                loadData();
                initRecyclerView();
                return true;

            case R.id.order_name_asc:
                dbHelper.setOrderBy(SQLAdapter.Audios.COLUMN_NAME_ARTIST + " ASC");
                loadData();
                initRecyclerView();
                return true;
            case R.id.order_name_desc:
                dbHelper.setOrderBy(SQLAdapter.Audios.COLUMN_NAME_ARTIST + " DESC");
                loadData();
                initRecyclerView();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * life-cycle methods
     */

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
            dbHelper.deleteAllAudios();

        }

    }

    /**
     * methods for bottom menu
     */


    public void start() {
        if (serviceBound)
            player.go();
    }


    public void pause() {
        if (serviceBound)
            player.pausePlayer();
    }


    public int getDuration() {
        if (serviceBound) {
            return player.getDur();
        } else
            return 0;
    }


    public int getCurrentPosition() {
        if (seekBar != null) {
            return player.getPosn();
        } else
            return 0;
    }


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


    /**
     * seekbar zone
     */
    private void initSeekBar() {

        seekBar = findViewById(R.id.seekBar3);
        seekBar.setMax(getDuration());
        /*seekBar.setVisibility(View.VISIBLE);*/

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean input) {
                if (input) {
                    player.seek(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void seekPosition() {
        try { //todo bez try
            long totalDuration = getDuration();
            long currentDuration = getCurrentPosition();


            seekBar.setProgress(player.getPosn());

            songTotalDuration = findViewById(R.id.songTotalDuration);
            songCurrentDuration = findViewById(R.id.songCurrentDuration);
            // Displaying Total Duration time
            // (целое число минут, общее время в сек - целое число минут выраженое в сек)
            songTotalDuration.setText(String.format("%d:%02d", (totalDuration / (1000 * 60)) % 60, totalDuration / 1000 - ((totalDuration / (1000 * 60)) % 60) * 60)
            );
            //Displaying time completed playing
            songCurrentDuration.setText(String.format("%d:%02d", (currentDuration / (1000 * 60)) % 60, currentDuration / 1000 - ((currentDuration / (1000 * 60)) % 60) * 60)
            );
            runnable = new Runnable() {
                @Override
                public void run() {
                    seekPosition();
                }
            };
            handler.postDelayed(runnable, 1000);
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    private void initPlayMenu() {
        LinearLayout playMenu = findViewById(R.id.play_menu_layout);
        play = findViewById(R.id.play);
        previous = findViewById(R.id.previous);
        next = findViewById(R.id.next);

        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.playPrev();
            }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                player.playNext();
            }
        });
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player.isPng()) {
                    pause();
                }
                if (!player.isPng()) {
                    start();
                }
            }

        });
    }

    public void buildNotification(PlaybackStatus playbackStatus) {
        if (serviceBound) {
            if (playbackStatus == PlaybackStatus.PLAYING) {
                play.setImageResource(R.drawable.ic_action_pause);

            } else if (playbackStatus == PlaybackStatus.PAUSED) {
                play.setImageResource(R.drawable.ic_action_play);
            }
        }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_READ_EXTERNAL_STORAGE);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // access granted
                    loadAudio();
                    loadData();
                } else {
                    // access denied
                    closeNow();
                }
                return;
            }
        }
    }

    private void closeNow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            finishAffinity();
        } else {
            finish();
        }
    }
}
