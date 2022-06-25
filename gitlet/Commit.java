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

    private String secondParent = null;

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

    public String getMessage() {
        return message;
    }

    public static Commit getCommitByID (String ID) {
        if (ID == null) {
            return null;
        }
        if (!join(COMMITS, ID).exists()) {
            System.out.println("No commit with that id exists.");
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
        if (secondParent != null) {
            System.out.println(String.format("Merge: %s %s", parent.substring(0, 7), secondParent.substring(0, 7)));
        }
        System.out.println("Date:" + " " + commitDate.toString());
        System.out.println(message);
        System.out.println("");
    }

    public void deleteTrackedFileInCWD () {
        for (String file : contentMapping.keySet()) {
            File cur = join(CWD, file);
            if (cur.exists()) {
                join(CWD, file).delete();
            }
        }
    }

    public void checkout(Commit preHead) {
        preHead.checkUnTrackedFiles();
        preHead.deleteTrackedFileInCWD();
        for (String file : contentMapping.keySet()) {
            String fileContent = readContentsAsString(join(BLOBS, contentMapping.get(file)));
            writeContents(join(CWD, file), fileContent);
        }
    }

    public void checkUnTrackedFiles() {
        List<String> filesInCWD = plainFilenamesIn(CWD);
        for (String file : filesInCWD) {
            if (!contentMapping.containsKey(file)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(1);
            }
        }
    }

    public static String latestCommonAncestor(String c1, String c2) {
        while (!c1.equals(c2)) {
            c1 = c1 == null ? c2 : getCommitByID(c1).getParent();
            c2 = c2 == null ? c1 : getCommitByID(c2).getParent();
        }
        return c1;
    }

    public Map<String, String> compareDiff(Commit other) {
        Map<String, String> res = new TreeMap<>();

        Iterator<Map.Entry<String, String>> otherIter = other.getContentMapping().entrySet().iterator();
        Iterator<Map.Entry<String, String>> thisIter = contentMapping.entrySet().iterator();
        Map.Entry<String, String> thisFile = thisIter.hasNext() ? thisIter.next() : null;
        Map.Entry<String, String> otherFile = otherIter.hasNext() ? otherIter.next() : null;

        while (thisFile != null || otherFile != null) {
            if (otherFile == null || (thisFile != null &&thisFile.getKey().compareTo(otherFile.getKey()) < 0)) {
                /* File does not exist in other commit object. */
                res.put(thisFile.getKey(), thisFile.getValue() + ",deleted");
                thisFile = thisIter.hasNext() ? thisIter.next() : null;
            } else if (thisFile == null || thisFile.getKey().compareTo(otherFile.getKey()) > 0) {
                /* File does not exist in this commit object. */
                res.put(otherFile.getKey(), otherFile.getValue() + ",added");
                otherFile = otherIter.hasNext() ? otherIter.next() : null;
            } else {
                if (thisFile.getValue().equals(otherFile.getValue())) {
                    continue;
                } else {
                    /* Given commit has different file content than it in this commit. */
                    res.put(thisFile.getKey(), otherFile.getValue() + ",modified");
                }
                thisFile = thisIter.hasNext() ? thisIter.next() : null;
                otherFile = otherIter.hasNext() ? otherIter.next() : null;
            }
        }
        return res;
    }

    public void setSecondParent(String commitID) {
        secondParent = commitID;
    }
}
