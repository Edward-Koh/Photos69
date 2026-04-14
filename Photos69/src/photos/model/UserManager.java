package photos.model;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stores and manages all users in the photo application.
 * Handles saving and loading application data using Java serialization.
 * Implemented as a singleton so all controllers share the same instance.
 */
public class UserManager implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String DATA_FOLDER = "data";
    public static final String DATA_FILE   = DATA_FOLDER + File.separator + "users.dat";

    /**
     * Tag types that only allow one value per photo.
     * Checked by PhotoViewController before adding a tag.
     */
    public static final List<String> SINGLE_VALUE_TAG_TYPES = List.of("location");

    //-------------------------Singleton-------------------------

    private static UserManager instance;

    /**
     * Returns the singleton instance.
     * Loads from disk on first call.
     */
    public static UserManager getInstance() {
        if(instance == null) {
            instance = load();
            instance.ensureDefaultUsers();
        }
        return instance;
    }

    //-------------------------Session Tracking-------------------------

    /**
     * The currently logged-in user.
     * Transient — not serialized since session state is runtime only.
     */
    private transient User currentUser;

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    // returns the currently logged-in user
    public User getCurrentUser() {
        return currentUser;
    }

    //-------------------------Fields-------------------------

    private List<User> users;

    // Maps tag type name -> whether it allows multiple values per photo
    // false = single value only (e.g. location), true = multiple allowed (e.g. person)
    private Map<String, Boolean> tagTypes = new HashMap<>();

    public UserManager() {
        users = new ArrayList<>();
    }

    //-------------------------Tag Type Operations-------------------------

    public void initDefaultTagTypes() {
        if(tagTypes == null) {
            tagTypes = new HashMap<>();
        }
        tagTypes.putIfAbsent("location", false);
        tagTypes.putIfAbsent("person",   true);
    }

    public boolean allowsMultiple(String tagName) {
        return tagTypes.getOrDefault(tagName.trim().toLowerCase(), true);
    }

    /**
     * Adds a new tag type with a specified single/multiple value policy.
     * Returns false if the tag type already exists.
     */
    public boolean addTagType(String tagName, boolean allowMultiple) {
        String key = tagName.trim().toLowerCase();
        if(tagTypes.containsKey(key)) return false;
        tagTypes.put(key, allowMultiple);
        return true;
    }

    public Map<String, Boolean> getTagTypes() {
        return tagTypes;
    }

    //-------------------------User Operations-------------------------

    // returns all users in the system
    public List<User> getUsers() {
        return users;
    }

    //Finds a user by username (case-insensitive).
    public User getUser(String username) {
        if(username == null) return null;
        for(User user : users) {
            if(user.getUsername().equalsIgnoreCase(username.trim())) {
                return user;
            }
        }
        return null;
    }

    /**
     * Adds a new user by username.
     * Rejects blank, duplicate, or reserved usernames.
     */
    public boolean addUser(String username) {
        if(username == null || username.trim().isEmpty()) return false;
        if(isReserved(username)) return false;
        if(getUser(username) != null) return false;
        users.add(new User(username.trim()));
        return true;
    }

    /**
     * Removes a user by username.
     * Reserved users (admin, stock) cannot be removed.
     */
    public boolean removeUser(String username) {
        if(isReserved(username)) return false;
        User user = getUser(username);
        if(user == null) return false;
        return users.remove(user);
    }

    // Returns true if the username is reserved and cannot be deleted.
    private boolean isReserved(String username) {
        return username.equalsIgnoreCase("admin") || username.equalsIgnoreCase("stock");
    }

    //-------------------------Default Users-------------------------

    /**
     * Ensures admin and stock users exist on startup.
     * Populates the stock album with image files from data/.
     */
    public void ensureDefaultUsers() {
        initDefaultTagTypes(); // ensure default tag types on every load

        if(getUser("admin") == null) {
            users.add(new User("admin"));
        }

        if(getUser("stock") == null) {
            User stockUser = new User("stock");
            stockUser.addAlbum("stock");

            File dataDir = new File(DATA_FOLDER);
            Album stockAlbum = stockUser.getAlbum("stock");
            if(dataDir.exists() && dataDir.isDirectory() && stockAlbum != null) {
                for(File f : dataDir.listFiles()) {
                    if(isImageFile(f)) {
                        java.util.Calendar cal = java.util.Calendar.getInstance();
                        cal.setTimeInMillis(f.lastModified());
                        cal.set(java.util.Calendar.MILLISECOND, 0);
                        stockAlbum.addPhoto(new Photo(f.getAbsolutePath(), cal));
                    }
                }
            }
            users.add(stockUser);
        }
    }

    private boolean isImageFile(File file) {
        if(!file.isFile()) return false;
        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg")
            || name.endsWith(".png") || name.endsWith(".gif")
            || name.endsWith(".bmp");
    }

    //-------------------------Save/Load-------------------------

    //Saves this UserManager to disk.
    public void save() throws IOException {
        File folder = new File(DATA_FOLDER);
        if(!folder.exists()) folder.mkdirs();
        try(ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(DATA_FILE))) {
            out.writeObject(this);
        }
    }

    /**
     * Loads UserManager from disk.
     * Returns a new empty instance if no file exists.
     */
    public static UserManager load() {
        try {
            File file = new File(DATA_FILE);
            if(!file.exists()) return new UserManager();
            try(ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
                return (UserManager) in.readObject();
            }
        }catch(Exception e) {
            return new UserManager();
        }
    }

    //-------------------------Search Helpers-------------------------

    /**
     * Returns all unique photos for a user across all albums.
     * Uses object identity for deduplication since the same Photo
     * object is shared by reference across albums.
     */
    public List<Photo> getAllUniquePhotos(User user) {
        List<Photo> result = new ArrayList<>();
        if(user == null) return result;
        Set<Photo> seen = new HashSet<>();
        for(Album album : user.getAlbums()) {
            for(Photo photo : album.getPhotos()) {
                if(seen.add(photo)) result.add(photo);
            }
        }
        return result;
    }

    // Searches by a single tag name+value pair
    public List<Photo> searchBySingleTag(User user, String tagName, String tagValue) {
        List<Photo> result = new ArrayList<>();
        if(user == null || tagName == null || tagValue == null) return result;
        for(Photo photo : getAllUniquePhotos(user)) {
            if(photo.hasTag(tagName, tagValue)) result.add(photo);
        }
        return result;
    }

    //Searches by two tag pairs using AND logic
    public List<Photo> searchByTwoTagsAnd(User user,
                                          String tagName1, String tagValue1,
                                          String tagName2, String tagValue2) {
        List<Photo> result = new ArrayList<>();
        if(user == null) return result;
        for(Photo photo : getAllUniquePhotos(user)) {
            if(photo.hasTag(tagName1, tagValue1) && photo.hasTag(tagName2, tagValue2))
                result.add(photo);
        }
        return result;
    }

    // Searches by two tag pairs using OR logic
    public List<Photo> searchByTwoTagsOr(User user,
                                         String tagName1, String tagValue1,
                                         String tagName2, String tagValue2) {
        List<Photo> result = new ArrayList<>();
        if(user == null) return result;
        for(Photo photo : getAllUniquePhotos(user)) {
            if(photo.hasTag(tagName1, tagValue1) || photo.hasTag(tagName2, tagValue2))
                result.add(photo);
        }
        return result;
    }

    // Searches by date range (inclusive on both ends)
    public List<Photo> searchByDate(User user, java.util.Calendar start, java.util.Calendar end) {
        List<Photo> result = new ArrayList<>();
        if(user == null || start == null || end == null) return result;
        for(Photo photo : getAllUniquePhotos(user)) {
            if(photo.isInDateRange(start, end)) result.add(photo);
        }
        return result;
    }
}