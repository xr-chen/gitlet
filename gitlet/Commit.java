package gitlet;

// TODO: any imports you need here

import java.io.File;
import java.io.Serializable;
import java.util.*;

import static gitlet.Repository.*;
import static gitlet.Utils.*;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author Xingrong Chen
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
        if (parent == null) {
            // no parent is provided we assume that this is the 0th commit
            this.commitDate.setTime(0);
        } else {
            this.parent = parent;
        }
        contentMapping = new TreeMap<>();
    }

    public Map<String, String> getContentMapping() {
        return contentMapping;
    }

    /* Return if content map of this commit contains the given file. */
    public boolean contains(String fileName) {
        return contentMapping.containsKey(fileName);
    }
    /* Return if the given file in working dir has the same content as it in this commit. */
    public boolean tracked(String fileName, String contentId) {
        return contentMapping.getOrDefault(fileName, "").equals(contentId);
    }


    public String getParent() {
        return parent;
    }

    public Date getCommitDate() {
        return commitDate;
    }

    public String getMessage() {
        return message;
    }

    public static Commit getCommitByID (String ID) {
        if (ID == null) {
            return null;
        }
        if (!join(COMMITS, ID).exists()) {
            System.out.println("Cannot find the commit with the given ID:" + ID);
            System.exit(1);
        }
        return readObject(join(COMMITS, ID), Commit.class);
    }

    public void checkoutFile(String fileName) {
        if (!contentMapping.containsKey(fileName)) {
            System.out.println("File does not exist in this commit.");
            System.exit(1);
        }
        String fileID = contentMapping.get(fileName);
        String fileContent = readContentsAsString(join(BLOBS, fileID));
        writeContents(join(CWD, fileName), fileContent);
    }

    public void updateContentForCommit(Staged staged) {
        contentMapping.putAll(getCommitByID(parent).getContentMapping());
        for (String file : staged.getStagedFiles()) {
            String fileID = staged.clearStageSymbol(file);
            if (fileID.equals("remove")) {
                contentMapping.remove(file);
            } else {
                contentMapping.put(file, fileID);
            }
        }
    }

    public void displayCommitNode (String nodeID) {
        System.out.println("===");
        System.out.println("commit" + " " + nodeID);
        System.out.println("Date:" + " " + commitDate.toString());
        System.out.println(message);
        System.out.println("");
    }

    public void checkout() {
        /*TODO:*/
        return;
    }
}
