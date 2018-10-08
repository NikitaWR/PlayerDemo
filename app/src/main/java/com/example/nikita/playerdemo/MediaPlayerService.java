package com.example.nikita.playerdemo;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.AudioAttributesCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.MediaController;
import android.widget.Toast;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

//import static com.example.nikita.playerdemo.MainActivity.Broadcast_PAUSE_AUDIO;
import static com.example.nikita.playerdemo.MainActivity.Broadcast_PLAY_NEW_AUDIO;

public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener, AudioManager.OnAudioFocusChangeListener {

    private MediaPlayer mediaPlayer;
    //path to the audio file
    private String mediaFile;
    //Used to pause/resume MediaPlayer
    private int resumePosition;
    private AudioManager audioManager;
    //Handle incoming phone calls
    private boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;
    private boolean pausedByUser = false;
    boolean released = true;
    private boolean shuffle=false;
    private Random rand;
    public final static String Broadcast_NOTIFY_DATA_SET_CHANGED = "com.example.nikita.playerdemo";

    //List of available Audio files
    private ArrayList<Audio> audioList;
    private int audioIndex = -1;
    private Audio activeAudio; //an object of the currently playing audio


// Binder given to clients
private final IBinder iBinder = new LocalBinder();
    public static final String ACTION_PLAY = "com.example.nikita.playerdemo.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.example.nikita.playerdemo.ACTION_PAUSE";
    public static final String ACTION_PREVIOUS = "com.example.nikita.playerdemo.ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "com.example.nikita.playerdemo.ACTION_NEXT";
    public static final String ACTION_STOP = "com.example.nikita.playerdemo.ACTION_STOP";

    //MediaSession
    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;

    //AudioPlayer notification ID
    private static final int NOTIFICATION_ID = 101;
    private boolean mpIsPrepared = false;


    //nm
    NotificationManager notificationManager;
    @Override
    public void onCreate() {
        super.onCreate();
        // Perform one-time setup procedures
        // Manage incoming phone calls during playback.
        // Pause MediaPlayer on incoming call,
        // Resume on hangup.
        callStateListener();
        //ACTION_AUDIO_BECOMING_NOISY -- change in audio outputs -- BroadcastReceiver
        registerBecomingNoisyReceiver();
        //Listen for new Audio to play -- BroadcastReceiver
        register_playNewAudio();
        rand=new Random();
        Log.i("Service", "onCreate");
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.release();
            released = true;
        }
        removeAudioFocus();
        //Disable the PhoneStateListener
        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        removeNotification();
        //unregister BroadcastReceivers
        unregisterReceiver(becomingNoisyReceiver);
        unregisterReceiver(playNewAudio);
        //clear cached playlist
      //  new StorageUtil(getApplicationContext()).clearCachedAudioPlaylist();
    }
@Override
public IBinder onBind(Intent intent) {
        return iBinder;
        }

    public class PlayerServiceBinder extends Binder {
        public MediaSessionCompat.Token getMediaSessionToken() {
            return mediaSession.getSessionToken();
        }
    }

@Override
public void onBufferingUpdate(MediaPlayer mp, int percent) {
        //Invoked indicating buffering status of
        //a media resource being streamed over the network.
        }

@Override
public void onCompletion(MediaPlayer mp) {
        //Invoked when playback of a media source has completed.

    //stopMedia();
    transportControls.skipToNext();
    //stop the service
    //stopSelf();
        }


    //Handle errors
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        //Invoked when there has been an error during an asynchronous operation
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d("MediaPlayer Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("MediaPlayer Error", "MEDIA ERROR SERVER DIED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("MediaPlayer Error", "MEDIA ERROR UNKNOWN " + extra);
                break;
        }
        return false;
    }

@Override
public boolean onInfo(MediaPlayer mp, int what, int extra) {
        //Invoked to communicate some info.
        return false;
        }

    @Override
    public void onPrepared(MediaPlayer mp) {
        //Invoked when the media source is ready for playback.
            mpIsPrepared = true;
            Log.i("MediaPlayerService", "onPrepared");
            playMedia();
    }
@Override
public void onSeekComplete(MediaPlayer mp) {
        //Invoked indicating the completion of a seek operation.
        }

    @Override
    public void onAudioFocusChange(int focusState) {
        //Invoked when the audio focus of the system is updated.

        switch (focusState) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (!pausedByUser) {
                    Log.i("AUDIOFOCUS_GAIN", "pausedByUser: " + String.valueOf(pausedByUser));
                    if (mediaPlayer == null) {
                        initMediaPlayer();
                    } else if (!mediaPlayer.isPlaying()) {
                        resumeMedia();
                        // mediaPlayer.setVolume(1.0f, 1.0f);
                    }
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                //if (mediaPlayer.isPlaying()) mediaPlayer.stop();
               // stopMedia();
                //mediaPlayer.release();
                //mediaPlayer = null;
                pauseMedia();
                Log.i("onAudioFocusChange", "AUDIOFOCUS_LOSS");
                break;


            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                Log.i("onAudioFocusChange", "AUDIOFOCUS_LOSS_TRANSIENT");
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                Log.i("onAudioFocusChange", "AUDIOFOCUS_LOSS_CAN_DUCK");
                break;
        }
    }

    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        //Could not gain focus
    }

    private boolean removeAudioFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                audioManager.abandonAudioFocus(this);
    }

public class LocalBinder extends Binder {
    public MediaPlayerService getService() {
        return MediaPlayerService.this;
    }
}
//initialize media player
    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        //Set up MediaPlayer event listeners
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
        //Reset so that the MediaPlayer is not pointing to another data source
        mediaPlayer.reset();
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build());
        try {
            mediaPlayer.setDataSource(activeAudio.getData());
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }
        try{
        mediaPlayer.prepareAsync();}
        catch (IllegalStateException exc){
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.not_found),Toast.LENGTH_LONG).show();
        }
    }

  //  if statements to make sure there are no problems while playing media
  private void playMedia() {
      Log.i("playmedia", "MediaPlayer.start()");
      if (!mediaPlayer.isPlaying()) {
          mediaPlayer.start();
          released=false;
         /* Intent broadcastIntent = new Intent(Broadcast_NOTIFY_DATA_SET_CHANGED);
          sendBroadcast(broadcastIntent);*/
          Piotrek.mainActivity.notifyDataSetChanged();

      }
  }

    private void stopMedia() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            released=true;
        }
    }

    private void pauseMedia() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                Log.i("pauseMedia", "pause");
                resumePosition = mediaPlayer.getCurrentPosition();
                mediaPlayer.pause();
            }
        }
    }

    private void resumeMedia() {

Log.i("MediaPlayerService","resume media");
        if (!mediaPlayer.isPlaying()&& !released) {
            mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();
        }
    }


    //function for headphones
/*
    The BroadcastReceiver instance will pause the MediaPlayer when the system makes
     an ACTION_AUDIO_BECOMING_NOISY call. To make the BroadcastReceiver available you must register it.
     The registerBecomingNoisyReceiver() function handles this and specifies the intent action BECOMING_NOISY
      which will trigger this BroadcastReceiver.
*/
    //Becoming noisy
    private BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //pause audio on ACTION_AUDIO_BECOMING_NOISY
            pauseMedia();
            buildNotification(PlaybackStatus.PAUSED);
        }
    };

    private void registerBecomingNoisyReceiver() {
        //register after getting audio focus
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(becomingNoisyReceiver, intentFilter);
    }
    //Handle incoming phone calls
    private void callStateListener() {
        // Get the telephony manager
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //Starting listening for PhoneState changes
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    //if at least one call exists or the phone is ringing
                    //pause the MediaPlayer
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mediaPlayer != null) {
                            pauseMedia();
                            ongoingCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Phone idle. Start playing.
                        if (mediaPlayer != null) {
                            if (ongoingCall) {
                                ongoingCall = false;
                            }
                            if (!pausedByUser){
                                resumeMedia();
                            }
                        }
                        break;
                }
            }
        };
        // Register the listener with the telephony manager
        // Listen for changes to the device call state.
        telephonyManager.listen(phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);
    }
    private BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(Broadcast_PLAY_NEW_AUDIO)) {
                Log.i("PlaynewAudio","play");

                //Get the new media index form SharedPreferences
                audioList = new StorageUtil(getApplicationContext()).loadAudio();
                audioIndex = new StorageUtil(getApplicationContext()).loadAudioIndex();
                if (audioIndex != -1 && audioIndex < audioList.size()) {
                    //index is in a valid range
                    activeAudio = audioList.get(audioIndex);

                } else {
                    stopSelf();
                }

                //A PLAY_NEW_AUDIO action received
                //reset mediaPlayer to play the new Audio
                stopMedia();
                mediaPlayer.reset();
                initMediaPlayer();
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING);
            }
        }
    };


    private void register_playNewAudio() {
        //Register playNewMedia receiver
        IntentFilter filter = new IntentFilter(Broadcast_PLAY_NEW_AUDIO);
        registerReceiver(playNewAudio, filter);
    }
 /*   private void register_pauseAudio() {
        //Register  receiver
        IntentFilter filter = new IntentFilter(MainActivity.Broadcast_PAUSE_AUDIO);
        registerReceiver(pauseAudio, filter);
    }*/

    private void initMediaSession() {
        if (mediaSessionManager != null) return; //mediaSessionManager exists

        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        // Create a new MediaSession
        mediaSession = new MediaSessionCompat(getApplicationContext(), "AudioPlayer");
        //Get MediaSessions transport controls
        transportControls = mediaSession.getController().getTransportControls();
        //set MediaSession -> ready to receive media commands
            Log.i("audiofocus", String.valueOf(requestAudioFocus()));
        mediaSession.setActive(true);
        //indicate that the MediaSession handles transport control commands
        // through its MediaSessionCompat.Callback.
        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                        | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        Intent activityIntent = new Intent(getApplicationContext(), MainActivity.class);
        mediaSession.setSessionActivity(
                PendingIntent.getActivity(getApplicationContext(), 0, activityIntent, 0));

        //Set mediaSession's MetaData
        updateMetaData();
        Intent mediaButtonIntent = new Intent(
                Intent.ACTION_MEDIA_BUTTON, null, getApplicationContext(), MediaButtonReceiver.class);
        mediaSession.setMediaButtonReceiver(
                PendingIntent.getBroadcast(getApplicationContext(), 0, mediaButtonIntent, 0));
        // Attach Callback to receive MediaSession updates
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            // Implement callbacks
            @Override
            public void onPlay() {
                Log.i("onPlay","play media");
                super.onPlay();
                resumeMedia();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onPause() {
                Log.i("onPause","pause media");
              //  super.onPause();
                pauseMedia();
                buildNotification(PlaybackStatus.PAUSED);
            }

            @Override
            public void onSkipToNext() {
                super.onSkipToNext();
                skipToNext();
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onSkipToPrevious() {
                super.onSkipToPrevious();
                skipToPrevious();
                updateMetaData();
                buildNotification(PlaybackStatus.PLAYING);
            }

            @Override
            public void onStop() {
                super.onStop();
                removeNotification();
                //Stop the service
                stopSelf();
            }

            @Override
            public void onSeekTo(long position) {
                if (!released)
                    Log.i("MediaPlayerService","seekTo");
                super.onSeekTo(position);
            }
        });
    }

    private void updateMetaData() {
        Bitmap albumArt = BitmapFactory.decodeResource(getResources(),R.drawable.image); //replace with medias albumArt
        // Update the current metadata
        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, activeAudio.getArtist())
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, activeAudio.getAlbum())
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, activeAudio.getTitle())
                .build());

        String text = activeAudio.getArtist() + " : " + activeAudio.getTitle();
        Piotrek.mainActivity.setActionBarText(text);

    }
    private void skipToNext() {
        if (shuffle) {
            audioIndex = rand.nextInt(audioList.size());
        }
            if (audioIndex == audioList.size() - 1  ) {
                //if last in playlist
                audioIndex = 0;
                activeAudio = audioList.get(audioIndex);
            } else {
                //get next in playlist
                activeAudio = audioList.get(++audioIndex);
            }
            //Update stored index
            new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);
            stopMedia();
            //reset mediaPlayer
            mediaPlayer.reset();
            released = true;
            initMediaPlayer();
    }
    private void skipToPrevious() {

        if (audioIndex == 0) {
            //if first in playlist
            //set index to the last of audioList
            audioIndex = audioList.size() - 1;
            activeAudio = audioList.get(audioIndex);
        } else {
            //get previous in playlist
            activeAudio = audioList.get(--audioIndex);
        }

        //Update stored index
        new StorageUtil(getApplicationContext()).storeAudioIndex(audioIndex);

        stopMedia();
        //reset mediaPlayer
        mediaPlayer.reset();
        released = true;
        initMediaPlayer();
    }
    private void buildNotification(PlaybackStatus playbackStatus) {
        int notificationAction = android.R.drawable.ic_media_pause;//needs to be initialized
       // Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent play_pauseAction = null;
        Piotrek.mainActivity.buildNotification(playbackStatus);

        //Build a new notification according to the current state of the MediaPlayer
        String PlayStatus = "PAUSE";
        if (playbackStatus == PlaybackStatus.PLAYING) {
            notificationAction = android.R.drawable.ic_media_pause;
            //create the pause action
            play_pauseAction = playbackAction(1);
        } else if (playbackStatus == PlaybackStatus.PAUSED) {
            PlayStatus = "PLAY";
            notificationAction = android.R.drawable.ic_media_play;
            //create the play action
            play_pauseAction = playbackAction(0);
        }

        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(),
                R.drawable.image); //replace with your own image
        //creat notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            final String CHANNEL_ID = "my_channel_01";
            CharSequence name = "my_channel";
            String Description = "This is my channel";
            int importance = NotificationManager.IMPORTANCE_LOW;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mChannel.setDescription(Description);
            mChannel.enableLights(true);
            mChannel.setLightColor(Color.RED);
            mChannel.enableVibration(true);
            mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            mChannel.setShowBadge(true);
            notificationManager.createNotificationChannel(mChannel);
        }
        // Create a new Notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this,"my_channel_01")
                .setShowWhen(false)
                // Set the Notification style
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                // Attach our MediaSession token
                        .setMediaSession(mediaSession.getSessionToken())
                // Show our playback controls in the compact notification view.
                .setShowActionsInCompactView(0, 1, 2))
                //
                //.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
               // .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set the Notification color
                .setColor(getResources().getColor(R.color.colorPrimary))
                // Set the large and small icons
                .setLargeIcon(largeIcon)
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                // Set Notification content information
                .setContentText(activeAudio.getArtist())
                .setContentTitle(activeAudio.getAlbum())
                .setContentInfo(activeAudio.getTitle())
                // Add playback actions
                .addAction(android.R.drawable.ic_media_previous, "previous", playbackAction(3))
                .addAction(notificationAction, PlayStatus, play_pauseAction)
                .addAction(android.R.drawable.ic_media_next, "next", playbackAction(2))
                .setOnlyAlertOnce(true);
                //.setCategory(NotificationCompat.CATEGORY_SERVICE)
                //.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT));
         /*       .setDeleteIntent(
                MediaButtonReceiver.buildMediaButtonPendingIntent(getApplicationContext(), PlaybackStateCompat.ACTION_STOP));*/
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void removeNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }
    private PendingIntent playbackAction(int actionNumber) {
        Intent playbackAction = new Intent(this, MediaPlayerService.class);
        switch (actionNumber) {
            case 0:
                // Play
                playbackAction.setAction(ACTION_PLAY);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);

            case 1:
                // Pause
                playbackAction.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 2:
                // Next track
                playbackAction.setAction(ACTION_NEXT);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 3:
                // Previous track
                playbackAction.setAction(ACTION_PREVIOUS);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        return null;
    }
    private void handleIncomingActions(Intent playbackAction) {
        if (playbackAction == null || playbackAction.getAction() == null) return;

        String actionString = playbackAction.getAction();
        if (actionString.equalsIgnoreCase(ACTION_PLAY)) {
            pausedByUser = false;
            transportControls.play();
        } else if (actionString.equalsIgnoreCase(ACTION_PAUSE)) {
            pausedByUser = true;
            transportControls.pause();
        } else if (actionString.equalsIgnoreCase(ACTION_NEXT)) {
            transportControls.skipToNext();
        } else if (actionString.equalsIgnoreCase(ACTION_PREVIOUS)) {
            transportControls.skipToPrevious();
        } else if (actionString.equalsIgnoreCase(ACTION_STOP)) {
            transportControls.stop();
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {
            //Load data from SharedPreferences
            StorageUtil storage = new StorageUtil(getApplicationContext());
            audioList = storage.loadAudio();
            audioIndex = storage.loadAudioIndex();

            if (audioIndex != -1 && audioIndex < audioList.size()) {
                //index is in a valid range
                activeAudio = audioList.get(audioIndex);
            } else {
                stopSelf();
            }
        } catch (NullPointerException e) {
            stopSelf();
        }

        //Request audio focus
        if (requestAudioFocus() == false) {
            //Could not gain focus
            stopSelf();
        }

        if (mediaSessionManager == null) {
            try {
                initMediaSession();
                initMediaPlayer();
            } catch (Exception e) {
                e.printStackTrace();
                stopSelf();
            }
            buildNotification(PlaybackStatus.PLAYING);

    }
        MediaButtonReceiver.handleIntent(mediaSession, intent);
        //Handle Intent action from MediaSession.TransportControls
        handleIncomingActions(intent);
        return super.onStartCommand(intent, flags, startId);
    }
    /**methods for musiccontroller*/
    public int getPosn() {
            if (!released) {
            return mediaPlayer.getCurrentPosition();
            }
            else{
            Log.i("getPosn()", "released");
            return  -1;
        }
    }


    public int getDur() {
        if (!released){
            Log.i("MediaPlayerService","getDur");
        return mediaPlayer.getDuration();}
        else {
            return -1;}
    }

    public boolean isPng(){
        return mediaPlayer.isPlaying();
    }

    public void pausePlayer(){
        transportControls.pause();
    }

    public void seek(int posn){
        if (!released) {
            mediaPlayer.seekTo(posn);
            resumePosition = posn;
            Log.i("MediaPlayerService","seek");}
           // else  mediaPlayer.seekTo(resumePosition);

    }
    public void setShuffle(boolean shuf){
        shuffle = shuf;
    }

    public void go(){
        transportControls.play();
    }
    public void playPrev(){
       transportControls.skipToPrevious();
    }
    //skip to next
    public void playNext(){
        transportControls.skipToNext();
    }
    public void pausedByUser(boolean mess){
       pausedByUser = mess ;

    }
}



