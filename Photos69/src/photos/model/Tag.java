package photos.model;

import java.io.Serializable;

// Represents a tag as a name-value pair that can be attached to a photo
public class Tag implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String value;

    // Constructs a Tag with the given name and value.
    public Tag(String name, String value) {
        this.name = name.trim().toLowerCase();
        this.value = value.trim();
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(!(obj instanceof Tag)) return false;
        Tag other = (Tag) obj;
        return this.name.equals(other.name) && this.value.equals(other.value);
    }

    public int hashCode() {
        return 31 * name.hashCode() + value.hashCode();
    }

    public String toString() {
        return name + "=" + value;
    }
}