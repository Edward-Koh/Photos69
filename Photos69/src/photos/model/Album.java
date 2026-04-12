package photos.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

// Represents a named album containing a list of photos.
public class Album implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private List<Photo> album;

    // Constructs an empty album with the given name.
    public Album(String name) {
        this.name = name;
        this.album = new ArrayList<>();
    }

            // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Photo> getPhotos() {
        return album;
    }


            // Photo Operations

    // adds photos to album if not duplicate
    public boolean addPhoto(Photo photo) {
        for(Photo p : album) {
            if(p.getFilePath().equals(photo.getFilePath())) {
                return false;
            }
        }
        album.add(photo);
        return true;
    }

    // removes photo from this album unless not found.
    public boolean removePhoto(Photo photo) {
        return album.remove(photo);
    }


            // Album properties for displaying
    
    public int getPhotoCount() {
        return album.size();
    }

    // returns earliest photo by date
    public Calendar getEarliestDate() {
        if(album.isEmpty()) return null;
        Calendar earliest = album.get(0).getDateTaken();
        for(Photo p : album) {
            if(p.getDateTaken().before(earliest)) {
                earliest = p.getDateTaken();
            }
        }
        return earliest;
    }

    // returns latest photo by date
    public Calendar getLatestDate() {
        if(album.isEmpty()) return null;
        Calendar latest = album.get(0).getDateTaken();
        for(Photo p : album) {
            if(p.getDateTaken().after(latest)) {
                latest = p.getDateTaken();
            }
        }
        return latest;
    }

    public String toString() {
        return name;
    }
}