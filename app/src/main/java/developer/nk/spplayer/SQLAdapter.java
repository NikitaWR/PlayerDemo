package developer.nk.spplayer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import static developer.nk.spplayer.SQLAdapter.Audios.COLUMN_NAME_ALBUM;
import static developer.nk.spplayer.SQLAdapter.Audios.COLUMN_NAME_ARTIST;
import static developer.nk.spplayer.SQLAdapter.Audios.COLUMN_NAME_ARTPATH;
import static developer.nk.spplayer.SQLAdapter.Audios.COLUMN_NAME_DATA;
import static developer.nk.spplayer.SQLAdapter.Audios.COLUMN_NAME_DURATION;
import static developer.nk.spplayer.SQLAdapter.Audios.COLUMN_NAME_TITLE;
import static developer.nk.spplayer.SQLAdapter.Audios.TABLE_NAME;


public class SQLAdapter {
    private  static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "Audio.db";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    Audios._ID + " INTEGER PRIMARY KEY," +
                    COLUMN_NAME_DATA + " TEXT," +
                    Audios.COLUMN_NAME_TITLE + " TEXT," +
                    Audios.COLUMN_NAME_ALBUM + " TEXT," +
                    Audios.COLUMN_NAME_ARTIST + " TEXT," +
                    Audios.COLUMN_NAME_ARTPATH + " TEXT,"+
                    Audios.COLUMN_NAME_DURATION + " TEXT)"
            ;
    public static class Audios implements BaseColumns {
        public static final String TABLE_NAME = "audio";
        public static final String COLUMN_NAME_DATA = "data";
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_ALBUM = "album";
        public static final String COLUMN_NAME_ARTIST = "artist";
        public static final String COLUMN_NAME_ARTPATH = "artPath";
        public static final String COLUMN_NAME_DURATION = "duration";
    }
    String TAG = "SQLadapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private String orderBy = null;

    public class DatabaseHelper extends SQLiteOpenHelper {
        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + TABLE_NAME;

        private DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_ENTRIES);
        }
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL(SQL_DELETE_ENTRIES);
            onCreate(db);
        }
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            onUpgrade(db, oldVersion, newVersion);
        }
    }
    private final Context mCtx;
    SQLAdapter (Context mCtx){
        this.mCtx = mCtx;
    }
    public SQLAdapter open() throws SQLException {
        mDbHelper = new SQLAdapter.DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        if (mDbHelper != null) {
            mDbHelper.close();
        }
    }
    public long createAudio(String data, String title, String album, String artist, String artPath, String duration) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(COLUMN_NAME_DATA, data);
        initialValues.put(COLUMN_NAME_TITLE, title);
        initialValues.put(COLUMN_NAME_ALBUM, album);
        initialValues.put(COLUMN_NAME_ARTIST, artist);
        initialValues.put(COLUMN_NAME_ARTPATH, artPath);
        initialValues.put(COLUMN_NAME_DURATION, duration);

        return mDb.insert(TABLE_NAME, null, initialValues);
    }
    public Cursor fetchAllAudios() {
        Cursor mCursor = mDb.query(Audios.TABLE_NAME, new String[]{Audios._ID, COLUMN_NAME_DATA,
                Audios.COLUMN_NAME_ARTIST, Audios.COLUMN_NAME_TITLE,Audios.COLUMN_NAME_ALBUM, Audios.COLUMN_NAME_ARTPATH,Audios.COLUMN_NAME_DURATION}, null, null, null, null, orderBy, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }

        return mCursor;
    }
    public Cursor fetchAudiosByTitleAndArtist(String inputText) throws SQLException {
        Log.w(TAG, "Szukamy: " + inputText);

        Cursor mCursor;

        if (inputText == null || inputText.length () == 0) {
            mCursor = mDb.query(Audios.TABLE_NAME, new String[]{Audios._ID, COLUMN_NAME_DATA,
                    Audios.COLUMN_NAME_ARTIST, Audios.COLUMN_NAME_TITLE,Audios.COLUMN_NAME_ALBUM,Audios.COLUMN_NAME_ARTPATH,Audios.COLUMN_NAME_DURATION},
                    null, null, null, null, orderBy, null);

        }
        mCursor = mDb.query(Audios.TABLE_NAME, new String[]{Audios._ID, COLUMN_NAME_DATA,
                        Audios.COLUMN_NAME_ARTIST, Audios.COLUMN_NAME_TITLE,Audios.COLUMN_NAME_ALBUM,Audios.COLUMN_NAME_ARTPATH,Audios.COLUMN_NAME_DURATION},
                Audios.COLUMN_NAME_ARTIST + " like '%" + (inputText) +  "%'" + " OR " +
                        Audios.COLUMN_NAME_TITLE+ " like '%" + inputText + "%'",
                null, null, null, orderBy, null);


        if (mCursor != null) {
            mCursor.moveToFirst();
        }

        return mCursor;
    }
    public boolean deleteAllAudios() {
        int doneDelete = 0;
        doneDelete = mDb.delete(Audios.TABLE_NAME, null , null);

        Log.w(TAG, Integer.toString(doneDelete));   // quantity of deleted rows
        return doneDelete > 0;
    }
    public void setOrderBy(String txt) {
        orderBy = txt;
    }


}
