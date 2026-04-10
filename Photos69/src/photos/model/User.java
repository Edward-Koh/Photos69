package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/*
 * Represents a non-admin application user
 * Owns a list of albums and a list of custom tag types the user has defined.
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private String username;
    private List<Album> albums;
    private List<String> customTagTypes;

    // there are prewritten tags and users can also add their own tags.
    public static final List<String> DEFAULT_TAG_TYPES = List.of("location", "person");

    // Constructs a new user with the given username.

    public User(String username) {
        this.username = username;
        this.albums = new ArrayList<>();
        this.customTagTypes = new ArrayList<>();
    }

    public String getUsername() {
        return username;
    }

    public List<Album> getAlbums() {
        return albums;
    }

    public List<String> getAllTagTypes() {
        List<String> all = new ArrayList<>(DEFAULT_TAG_TYPES);
        all.addAll(customTagTypes);
        return all;
    }

    public List<String> getCustomTagTypes() {
        return customTagTypes;
    }

    // adds album if there isn't already an album with the same name
    public boolean addAlbum(Album album) {
        if(getAlbumByName(album.getName()) != null) {
            return false;
        }
        albums.add(album);
        return true;
    }

    // removes album if exist
    public boolean removeAlbum(Album album) {
        return albums.remove(album);
    }

    // renames an album as long as it doesn't conflict with an existing album
    public boolean renameAlbum(Album album, String newName) {
        if(getAlbumByName(newName) != null) {
            return false;
        }
        album.setName(newName);
        return true;
    }

    // finds album by name
    public Album getAlbumByName(String name) {
        for (Album a : albums) {
            if (a.getName().equalsIgnoreCase(name)) {
                return a;
            }
        }
        return null;
    }

    // adds a custom tag defined by user if it doesn't already exist
    public boolean addCustomTagType(String tagType) {
        String normalized = tagType.trim().toLowerCase();
        if(getAllTagTypes().contains(normalized)) {
            return false;
        }
        customTagTypes.add(normalized);
        return true;
    }

    public String toString() {
        return username;
    }
}