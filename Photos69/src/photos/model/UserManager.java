package photos.model;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


// Stores and manages all users in the photo application.
// This class also handles saving and loading the application data
// using Java serialization.

public class UserManager implements Serializable {

    private static final long serialVersionUID = 1L;

    // Folder where app data is stored
    public static final String DATA_FOLDER = "data";

    // Serialized file used to save all users and their albums/photos
    public static final String DATA_FILE = DATA_FOLDER + File.separator + "users.dat";

    // List of all users in the system
    private List<User> users;

    // Creates an empty user manager.
    public UserManager() {
        users = new ArrayList<>();
    }

    // Basic User Operations

    // Returns all users.
    public List<User> getUsers() {
        return users;
    }

     // Finds a user by username.
     // Returns null if not found.
     // 
    public User getUser(String username) {
        if (username == null) {
            return null;
        }

        for (User user : users) {
            if (user.getUsername().equalsIgnoreCase(username.trim())) {
                return user;
            }
        }

        return null;
    }


    // Adds a new user if username is not blank
    // and does not already exist.
    public boolean addUser(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        if (getUser(username) != null) {
            return false;
        }

        users.add(new User(username.trim()));
        return true;
    }

     // Removes a user by username.
    public boolean removeUser(String username) {
        User user = getUser(username);
        if (user == null) {
            return false;
        }

        return users.remove(user);
    }
    // Default Users


    // Makes sure the required special users exist:
    // admin
    // stock
    // Also makes sure stock has an album named "stock".
    public void ensureDefaultUsers() {
        if (getUser("admin") == null) {
            addUser("admin");
        }

        if (getUser("stock") == null) {
            User stockUser = new User("stock");
            stockUser.addAlbum("stock");
            users.add(stockUser);
        }
    }

    // Save / Load

    // Saves the whole UserManager object to disk.
    public void save() throws IOException {
        File folder = new File(DATA_FOLDER);

        // Make sure the data folder exists
        if (!folder.exists()) {
            folder.mkdirs();
        }

        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(DATA_FILE));
        out.writeObject(this);
        out.close();
    }

     // Loads saved data from disk.
     // If no save file exists yet, returns a new empty UserManager.
    public static UserManager load() {
        try {
            File file = new File(DATA_FILE);

            if (!file.exists()) {
                return new UserManager();
            }

            ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
            UserManager manager = (UserManager) in.readObject();
            in.close();

            return manager;
        } catch (Exception e) {
            return new UserManager();
        }
    }

    // Search Helpers

    // Returns all unique photos belonging to a user across all albums.
    // This is important because the same photo may appear in multiple albums,
    // but we only want one copy in search results.

    public List<Photo> getAllUniquePhotos(User user) {
        List<Photo> result = new ArrayList<>();

        if (user == null) {
            return result;
        }

        Set<Photo> seen = new HashSet<>();

        for (Album album : user.getAlbums()) {
            for (Photo photo : album.getPhotos()) {
                if (!seen.contains(photo)) {
                    seen.add(photo);
                    result.add(photo);
                }
            }
        }

        return result;
    }

    // Searches a user's photos by one tag pair.
    public List<Photo> searchBySingleTag(User user, String tagName, String tagValue) {
        List<Photo> result = new ArrayList<>();

        if (user == null || tagName == null || tagValue == null) {
            return result;
        }

        for (Photo photo : getAllUniquePhotos(user)) {
            if (photo.hasTag(tagName, tagValue)) {
                result.add(photo);
            }
        }

        return result;
    }

     // Searches a user's photos by two tag pairs using AND.
    public List<Photo> searchByTwoTagsAnd(User user,
                                          String tagName1, String tagValue1,
                                          String tagName2, String tagValue2) {
        List<Photo> result = new ArrayList<>();

        if (user == null) {
            return result;
        }

        for (Photo photo : getAllUniquePhotos(user)) {
            boolean firstMatch = photo.hasTag(tagName1, tagValue1);
            boolean secondMatch = photo.hasTag(tagName2, tagValue2);

            if (firstMatch && secondMatch) {
                result.add(photo);
            }
        }

        return result;
    }

     // Searches a user's photos by two tag pairs using OR.
     
    public List<Photo> searchByTwoTagsOr(User user,
                                         String tagName1, String tagValue1,
                                         String tagName2, String tagValue2) {
        List<Photo> result = new ArrayList<>();

        if (user == null) {
            return result;
        }

        for (Photo photo : getAllUniquePhotos(user)) {
            boolean firstMatch = photo.hasTag(tagName1, tagValue1);
            boolean secondMatch = photo.hasTag(tagName2, tagValue2);

            if (firstMatch || secondMatch) {
                result.add(photo);
            }
        }

        return result;
    }

    /**
     * Searches a user's photos by date range.
     *
     * This assumes Photo has a method:
     *   boolean isInDateRange(Calendar start, Calendar end)
     */
    public List<Photo> searchByDate(User user, java.util.Calendar start, java.util.Calendar end) {
        List<Photo> result = new ArrayList<>();

        if (user == null || start == null || end == null) {
            return result;
        }

        for (Photo photo : getAllUniquePhotos(user)) {
            if (photo.isInDateRange(start, end)) {
                result.add(photo);
            }
        }

        return result;
    }
}
