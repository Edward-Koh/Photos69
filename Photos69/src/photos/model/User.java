package photos.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

 // Represents one user in the photo application.
 // A user has:
 // a username
 // a list of albums
 // a list of available tag types
 // Example default tag types:
 // person
 // location

public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    // The username for this user
    private String username;

    // List of albums owned by this user
    private List<Album> albums;

    // List of tag types this user can use
    // Example: person, location, event
    private List<String> tagTypes;

    /*
     * Creates a new user with the given username.
     * Also adds the default tag types.
     */
    public User(String username) {
        this.username = username == null ? "" : username.trim();
        this.albums = new ArrayList<>();
        this.tagTypes = new ArrayList<>();

        // Default tag types required/expected for the project
        tagTypes.add("person");
        tagTypes.add("location");
    }

    // Getters and Setters

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username == null ? "" : username.trim();
    }

    public List<Album> getAlbums() {
        return albums;
    }

    public List<String> getTagTypes() {
        return tagTypes;
    }

    // Album Operations

    // Adds a new album if the name is not blank
    // and does not already exist for this user.
    
    public boolean addAlbum(String albumName) {
        if (albumName == null || albumName.trim().isEmpty()) {
            return false;
        }

        if (getAlbum(albumName) != null) {
            return false; // duplicate album name
        }

        albums.add(new Album(albumName.trim()));
        return true;
    }

    
    // Removes an album by name.
    public boolean removeAlbum(String albumName) {
        Album album = getAlbum(albumName);
        if (album == null) {
            return false;
        }
        return albums.remove(album);
    }
    // Finds and returns an album by name.
    // Returns null if not found.

    public Album getAlbum(String albumName) {
        if (albumName == null) {
            return null;
        }

        for (Album album : albums) {
            if (album.getName().equalsIgnoreCase(albumName.trim())) {
                return album;
            }
        }

        return null;
    }
     // Renames an album if:
     // the old album exists
     // the new name is not blank
     // the new name is not already being used by another album
    
    public boolean renameAlbum(String oldName, String newName) {
        Album album = getAlbum(oldName);

        if (album == null) {
            return false;
        }

        if (newName == null || newName.trim().isEmpty()) {
            return false;
        }

        Album existing = getAlbum(newName);
        if (existing != null && existing != album) {
            return false;
        }

        album.setName(newName.trim());
        return true;
    }

    // Tag Type Operations
    // Adds a new tag type for this user.
    // Example: "weather", "event", "mood"
    // Returns false if blank or duplicate.

    public boolean addTagType(String tagType) {
        if (tagType == null || tagType.trim().isEmpty()) {
            return false;
        }

        String cleanType = tagType.trim().toLowerCase();

        for (String type : tagTypes) {
            if (type.equalsIgnoreCase(cleanType)) {
                return false; // duplicate tag type
            }
        }

        tagTypes.add(cleanType);
        return true;
    }
    // Returns true if this user already has the given tag type.

    public boolean hasTagType(String tagType) {
        if (tagType == null) {
            return false;
        }

        for (String type : tagTypes) {
            if (type.equalsIgnoreCase(tagType.trim())) {
                return true;
            }
        }

        return false;
    }
    // Returns username when displayed in UI lists.
    
    @Override
    public String toString() {
        return username;
    }
}