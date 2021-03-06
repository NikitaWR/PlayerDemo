package developer.nk.spplayer;

import java.io.Serializable;

public class Audio implements Serializable {
    private String data;
    private String title;
    private String album;
    private String artist;
    private String artPath;
    private String duration;

    public Audio(String data, String title, String album, String artist, String artPath, String duration) {
        this.data = data;
        this.title = title;
        this.album = album;
        this.artist = artist;
        this.artPath = artPath;
        this.duration = duration;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAlbum() {
        return album;
    }
    public String getArtPath() {
        return artPath;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }
    public void setDuration(String duration){this.duration = duration;}
    public String getDuration(){return duration;}
}
