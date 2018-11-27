package developer.nk.spplayer;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.TreeSet;

public class MainActivity extends AppCompatActivity implements  MediaPlayerControl {

    private MediaPlayerService player;
    final StorageUtil storageUtil = new StorageUtil(this);
    private ArrayList<Audio> audioList;
    boolean serviceBound = false;
    boolean letUseKeyBack = false;
    boolean setShuffle=false;

    private static final int MY_PERMISSIONS_READ_EXTERNAL_STORAGE = 1;
    public static final String Broadcast_PLAY_NEW_AUDIO = "developer.nk.spplayer";
    private String actionBarText;


    private TextView mTitleTextView;
    private SeekBar seekBar;
    private ActionBar mActionBar;
    private Runnable runnable;
    private Handler handler;

    private ImageButton play;
    private ImageButton previous;
    private ImageButton next;
    private ImageButton shuffle;
    private ImageButton search;
    private ProgressBar progressBar;
    private TextView loadingData;
    private TextView songCurrentDuration;
    private TextView songTotalDuration;

    private SQLAdapter dbHelper;


    FragmentAllAudios fragmentAllAudios;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private ViewPagerAdapter viewPagerAdapteradapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


        tabLayout = findViewById(R.id.tablayout_id);
        viewPager = findViewById(R.id.viewpager_id);
        viewPagerAdapteradapter = new ViewPagerAdapter(getSupportFragmentManager());

        //add fragment here
        fragmentAllAudios = new FragmentAllAudios();
        viewPagerAdapteradapter.addFragment(fragmentAllAudios,"Songs");
        viewPagerAdapteradapter.addFragment(new FragmentSortByAutor(),"Autors");
        viewPagerAdapteradapter.addFragment(new FragmentSortByAlbum(),"Albums");

        viewPager.setAdapter(viewPagerAdapteradapter);

        mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowTitleEnabled(false);
        LayoutInflater mInflater = LayoutInflater.from(this);
        View mCustomView = mInflater.inflate(R.layout.title_view, null);
        mTitleTextView = mCustomView.findViewById(R.id.title);
        mActionBar.setCustomView(mCustomView);
        mActionBar.setDisplayShowCustomEnabled(true);
        progressBar = findViewById(R.id.progressBar);
       // progressBar.setVisibility(View.VISIBLE);
        loadingData = findViewById(R.id.loadingData);
        //loadingData.setVisibility(View.VISIBLE);
        setShuffle = false;

       // checkPermissions();

        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_READ_EXTERNAL_STORAGE);
        }else {
            audioList = storageUtil.loadAudio();
            if (audioList!=null){
                setActionBarText((audioList.get(storageUtil.loadAudioIndex()).getArtist()) + " : " +
                        audioList.get(storageUtil.loadAudioIndex()).getTitle());
            }
            initDB();
            //load from DB
            //audioList = storageUtil.loadAudio();
           // if (audioList==null){
            loadFromDB();
            //}
           // else {


        }
        InstanceOfMainActivity.mainActivity = this;
        initPlayMenu();
        makeActionOverflowMenuShown();
        LinearLayout playMenu = findViewById(R.id.play_menu);
        playMenu.setVisibility(View.VISIBLE);
    }

    void setActionBarText(String myText) {
        actionBarText=myText;
        mTitleTextView.setText(myText);
    }

/*    private void initRecyclerView() {

        if (audioList.size() > 0) {
            progressBar.setVisibility(View.INVISIBLE);
            loadingData.setVisibility(View.INVISIBLE);
           //fragmentAllAudios.updateRV();
        }
        else Toast.makeText(this,getResources().getString(R.string.nothing_found),Toast.LENGTH_SHORT).show();


    }*/

    //Binding this Client to the AudioPlayer Service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
            player = binder.getService();
            serviceBound = true;
            initSeekBar();
            handler = new Handler();
            seekPosition();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    /*private*/ void playAudio(int audioIndex, ArrayList<Audio> audioList) {
        if (player==null){
            //Store Serializable audioList to SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudio(audioList);
            storage.storeAudioIndex(audioIndex);

            Intent playerIntent = new Intent(this, MediaPlayerService.class);
            startService(playerIntent);
            bindService(playerIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            //Store the new audioIndex to SharedPreferences
            Log.i("playAudio",String.valueOf(audioList.get(audioIndex).getTitle()));

            StorageUtil storage = new StorageUtil(getApplicationContext());
            storage.storeAudio(audioList);
            storage.storeAudioIndex(audioIndex);

            //Service is active
            //Send a broadcast to the service -> PLAY_NEW_AUDIO
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        }
    }
      public void notifyDataSetChanged(){
      fragmentAllAudios.notifyDataSetChanged();
    }

    //retrieves the data from the device in ascending order
    private boolean  loadAudio() {

        Log.i("load audio", "la");
        progressBar.setVisibility(View.VISIBLE);
        ContentResolver contentResolver = getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cursor = contentResolver.query(uri, null, selection, null, sortOrder);
        dbHelper.deleteAllAudios();

        if (cursor != null && cursor.getCount() > 0) {
            int size = 0;
            while (cursor.moveToNext()) {
                String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                Long albumId = Long.valueOf(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)));
                Integer intDuration = Integer.valueOf(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)));


                final  Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
                Uri artPath = ContentUris.withAppendedId(sArtworkUri, albumId);
                String duration =(String.format("%d:%02d", (intDuration / (1000 * 60)) % 60, intDuration / 1000 - ((intDuration / (1000 * 60)) % 60) * 60));
                dbHelper.createAudio(data, title, album, artist,artPath.toString(), duration);
                size++;
            }
            Log.i("load audio list size", String.valueOf(size));

            cursor.close();
            loadFromDB();
            return true;
        } else {
            Log.i("Load audio", "AlertDialog");
            creatAlertDialog();
            return false;}
    }

    private void initDB() {
        dbHelper = new SQLAdapter(this);
        dbHelper.open();
    }

    //load audio from db to array
    void loadFromDB() {
        audioList = new ArrayList<>();
        Cursor cursor
                = dbHelper.fetchAllAudios();
        int size = 0;
        if (cursor.getCount()>0){

        cursor.moveToFirst();
         do {
             String album = cursor.getString(cursor.getColumnIndex("album"));
             String artist = cursor.getString(cursor.getColumnIndex("artist"));
            Audio audio = new Audio(cursor.getString(
                    cursor.getColumnIndex("data")),
                    cursor.getString(cursor.getColumnIndex("title")),
                    album, artist,
                    cursor.getString(cursor.getColumnIndex("artPath")),
                    cursor.getString(cursor.getColumnIndex("duration")));
            audioList.add(audio);
            size++;
        }while (cursor.moveToNext());
         cursor.close();
        }
        else loadAudio();

    }
    void fetchByArtistAndTitle(String search){
        ArrayList audioListSearch = new ArrayList();
        Cursor cursor = dbHelper.fetchAudiosByTitleAndArtist(search);
        while (cursor.moveToNext()) {
            Audio audio = new Audio((cursor.getString(
                    cursor.getColumnIndex("data"))),
                    cursor.getString(cursor.getColumnIndex("title")),
                    cursor.getString(cursor.getColumnIndex("album")),
                    cursor.getString(cursor.getColumnIndex("artist")),
                    cursor.getString(cursor.getColumnIndex("artPath")),
                    cursor.getString(cursor.getColumnIndex("duration")));
            audioListSearch.add(audio);
        }
        if (audioListSearch.size()>0)
        mTitleTextView.setText((getResources().getString(R.string.search) +" " + "\"" +  search + "\""));

        cursor.close();
        fragmentAllAudios.updateRV(audioListSearch);
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
            case R.id.reset_db:
                progressBar.setVisibility(View.VISIBLE);
                loadingData.setVisibility(View.VISIBLE);
                final Runnable runLoadData = new Runnable() {
                    @Override
                    public void run() {
                        loadFromDB();
                    }
                };
                final Runnable runInitRV = new Runnable() {
                    @Override
                    public void run() {

                        fragmentAllAudios.updateRV(audioList);
                        progressBar.setVisibility(View.INVISIBLE);
                        loadingData.setVisibility(View.INVISIBLE);
                    }
                };
                Thread mythread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        dbHelper.deleteAllAudios();
                        loadAudio();
                        MainActivity.this.runOnUiThread(runLoadData);
                        MainActivity.this.runOnUiThread(runInitRV);
                    }
                });
                mythread.start();
                return true;
            case R.id.home:
                // go back to audioList intstead of audioListSearch
                fragmentAllAudios.updateRV(audioList);
                //hide up button
                mActionBar.setDisplayHomeAsUpEnabled(false);
                return true;

            case R.id.order_id_asc:
                dbHelper.setOrderBy(SQLAdapter.Audios.COLUMN_NAME_TITLE + " ASC");
                loadFromDB();
                fragmentAllAudios.updateRV(audioList);
                return true;

            case R.id.order_id_desc:
                dbHelper.setOrderBy(SQLAdapter.Audios.COLUMN_NAME_TITLE + " DESC");
                loadFromDB();
                fragmentAllAudios.updateRV(audioList);
                return true;

            case R.id.order_name_asc:
                dbHelper.setOrderBy(SQLAdapter.Audios.COLUMN_NAME_ARTIST + " ASC");
                loadFromDB();
                fragmentAllAudios.updateRV(audioList);
                return true;
            case R.id.order_name_desc:
                dbHelper.setOrderBy(SQLAdapter.Audios.COLUMN_NAME_ARTIST + " DESC");
                loadFromDB();
                fragmentAllAudios.updateRV(audioList);
                return true;
            case  R.id.privacy:
                Intent intent = new Intent(this,AboutActivity.class);
                startActivity(intent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)&& letUseKeyBack)
        {
            // go back to audioList intstead of audioListSearch
            fragmentAllAudios.updateRV(audioList);
            //hide up button
            //mActionBar.setDisplayHomeAsUpEnabled(false);
            letUseKeyBack=false;
            setActionBarText(actionBarText);

            return true;
        }
        //default
        return super.onKeyDown(keyCode, event);
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
        //if (serviceBound) {
        if (player!=null){
            unbindService(serviceConnection);
            //service is active
            player.stopSelf();
           // dbHelper.close();

        }

    }

    /**
     * methods for bottom menu
     */


    public void start() {
        if (player!=null)
            player.go();
    }


    public void pause() {
        if (player!=null)
            player.pausePlayer();
    }


    public int getDuration() {
        if (player!=null && seekBar != null && player!=null) {
            return player.getDur();
        } else
            return 0;
    }


    public int getCurrentPosition() {
        if (seekBar != null && player!=null) {
            return player.getPosn();
        } else
            return 0;
    }


    public void seekTo(int pos) {
        Log.d("seekTo", String.valueOf(pos));
        if (seekBar != null && player!=null)
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
        Log.d("setMax", String.valueOf(getDuration()));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean input) {
                if (input) {

                    Log.d("onProgressChanged", "progress "+String.valueOf(progress)+ "   duration " + getDuration());
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

        try {
            long totalDuration = getDuration();
            long currentDuration = getCurrentPosition();
            seekBar.setMax(getDuration());
            seekBar.setProgress(getCurrentPosition());


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
            handler.postDelayed(runnable, 200);
        }
        catch (Exception e){e.printStackTrace();}

    }

    private void initPlayMenu() {
        play = findViewById(R.id.play);
        previous = findViewById(R.id.previous);
        next = findViewById(R.id.next);
        shuffle= findViewById(R.id.shuffle_play);
        search=findViewById(R.id.search);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(R.string.search);

                View viewInflated = LayoutInflater.from(getApplicationContext()).inflate(R.layout.search_window, null);

                final EditText bodyInput = viewInflated.findViewById(R.id.searchWindow);
                builder.setView(viewInflated);


                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        String body = bodyInput.getText().toString();
                        fetchByArtistAndTitle(body);
                        letUseKeyBack=true;
                    }
                });
                builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.show();
            }
        });
        shuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setShuffle = !setShuffle;
                if (player!=null)
                player.setShuffle(setShuffle);
                if (setShuffle){
                    shuffle.setBackgroundColor(getResources().getColor(R.color.colorOrange));
                }
                else {
                shuffle.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
                }
            }
        });
        previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player!=null)
                player.playPrev();
            }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player!=null)
                player.playNext();
            }
        });
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player==null) {
                    int audioIndex=storageUtil.loadAudioIndex();
                    if (audioIndex >= 0) {
                        playAudio(audioIndex, audioList);
                        buildNotification(PlaybackStatus.PLAYING);
                    }
                }
                else {
                if (player.isPng()) {
                    pause();
                    player.pausedByUser(true);
                }
                if (!player.isPng()) {
                    start();
                    player.pausedByUser(false);
                }
                }
            }

        });
    }
    public boolean getShuffle(){
        return setShuffle;
    }


    public void buildNotification(PlaybackStatus playbackStatus) {

            if (playbackStatus == PlaybackStatus.PLAYING) {
                play.setImageResource(R.drawable.ic_action_pause);

            } else if (playbackStatus == PlaybackStatus.PAUSED) {
                play.setImageResource(R.drawable.ic_action_play);
            }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // access granted
                    initDB();
                    loadFromDB();
                    fragmentAllAudios.updateRV(audioList);
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
    public ArrayList<Audio> getArray(){
        return audioList;
    }


    private void creatAlertDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("No audio found");


        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                closeNow();
            }
        });
        builder.show();
    }
    private void makeActionOverflowMenuShown() {
        //devices with hardware menu button (e.g. Samsung Note) don't show action overflow menu
        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if (menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception e) {
            Log.d("ActionOverflowMenuShown", e.getLocalizedMessage());
        }
    }

}
