package gitlet;

// TODO: any imports you need here

import java.io.File;
import java.io.Serializable;
import java.util.Date; // TODO: You'll likely use this in this class
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Commit implements Serializable {
    /**
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */


    /* Combinations of log messages, other metadata (commit date, author, etc.), a reference to a tree,
     and references to parent commits. The repository also maintains a mapping from branch heads to references
     to commits, so that certain important commits have symbolic names.*/


    /** The message of this Commit. */
    private String message;
    /* The SHA-1 hash strings of parent commit of this commit. */
    private String parent;
    /* The date when this commit was created. */
    private Date commitDate;
    /* The map which maps the file name to the SHA-1 hash code. */
    private Map<String, String> contentMapping;

    public Commit(String msg, String parent) {
        this.message = msg;
        this.commitDate = new Date();

        if (this.parent == null) {
            // no parent is provided we assume that this is the 0th commit
            this.commitDate.setTime(0);
            contentMapping = new HashMap<String, String>();
        } else {
            this.parent = parent;
        }
    }


    public void updateContent(Map<String, String> previous, Map<String, String> staged) {
        /* Update previous commit map if new file was added or changed.*/
        this.contentMapping = new HashMap<String, String>(previous);
        for (String file : staged.keySet()) {
            this.contentMapping.put(file, staged.get(file));
        }
    }

    public Map<String, String> getContentMapping() {
        return contentMapping;
    }

    /* TODO: fill in the rest of this class. */
}
