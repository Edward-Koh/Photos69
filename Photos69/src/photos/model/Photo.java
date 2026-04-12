package photos.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

// Represents a photo with a file path, caption, date taken, and list of tags.

public class Photo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String filePath;
    private String caption;
    private Calendar dateTaken;
    private List<Tag> tags;


    // Constuctor that takes filepath and date modified as parameter 
    public Photo(String filePath, Calendar dateTaken) {
        this.filePath = filePath;
        this.dateTaken = dateTaken;
        this.dateTaken.set(Calendar.MILLISECOND, 0); // required for correct equality checks
        this.caption = "";
        this.tags = new ArrayList<>();
    }

    // returns absolute path
    public String getFilePath() {
        return filePath;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public Calendar getDateTaken() {
        return dateTaken;
    }

    public List<Tag> getTags() {
        return tags;
    }

    // adds tag if not duplicated
    public boolean addTag(Tag tag) {
        if(tags.contains(tag)) {
            return false;
        }
        tags.add(tag);

        return true;
    }

    // removes tag if found
    public boolean removeTag(Tag tag) {
        return tags.remove(tag);
    }

    /*
     * Returns all tags on this photo with the given name.
     * Used by the controller to enforce single-value tag types:
     * if this returns size >= 1 for a single-value type, adding another is blocked.
     */
    public List<Tag> getTagsByName(String name) {
        return tags.stream()
                .filter(t -> t.getName().equals(name.trim().toLowerCase()))
                .collect(Collectors.toList());
    }

    /*
     * Checks whether this photo has a tag with the given name and value.
     * Used during search to match tag-based criteria.
     */
    public boolean hasTag(String name, String value) {
        return tags.contains(new Tag(name, value));
    }
    // checks if this photo's date is inside the given date range.
     // Used for date search in UserManager.
   
    public boolean isInDateRange(Calendar start, Calendar end) {
        if (start == null || end == null || dateTaken == null) {
            return false;
        }

        return !dateTaken.before(start) && !dateTaken.after(end);
    }

    public String toString() {
        return filePath;
    }
}