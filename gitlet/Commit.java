package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static gitlet.Repository.BLOBS;
import static gitlet.Repository.COMMITS;
import static gitlet.Repository.CWD;
import static gitlet.Utils.join;
import static gitlet.Utils.plainFilenamesIn;
import static gitlet.Utils.readContentsAsString;
import static gitlet.Utils.readObject;
import static gitlet.Utils.writeContents;

/** Represents a gitlet commit object
 *  which includes metadata of a commit (data, commit message, reference to parent commit),
 *  also contains some methods to create a new commit, get the head commit object of each branch,
 *  and display metadata of a specific commit.
 *
 *  @author Xingrong Chen
 */
public class Commit implements Serializable {

    /** Commit message. */
    private final String message;
    /* Reference (SHA-1 hash) to parent commit object. */
    private final String parent;
    /* When this commit was created, date of initial commit is always the (Unix) Epoch. */
    private final Date commitDate;
    /* Maps the file name to the blob reference (SHA-1 hash). */
    private final Map<String, String> contentMapping;
    /* A merged commit will have the second parent.*/
    private String secondParent = null;

    public Commit(String msg, String parent) {
        this.message = msg;
        this.commitDate = new Date();
        this.parent = parent;
        if (parent == null) {
            /*initial commit have no parent.*/
            this.commitDate.setTime(0);
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
    /* Return if the given file was tracked and has the same content in this commit. */
    public boolean tracked(String fileName, String contentId) {
        return contentMapping.getOrDefault(fileName, "").equals(contentId);
    }
    /* Return the reference of parent commit.*/
    public String getParent() {
        return parent;
    }

    public String getMessage() {
        return message;
    }
    /* Return the commit object with given ID.*/
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
    /* Put the file tracked in this commit to current working directory.*/
    public void checkoutFile(String fileName) {
        if (!contentMapping.containsKey(fileName)) {
            System.out.println("File does not exist in this commit.");
            System.exit(1);
        }
        String fileID = contentMapping.get(fileName);
        String fileContent = readContentsAsString(join(BLOBS, fileID));
        writeContents(join(CWD, fileName), fileContent);
    }
    /* Inherit file information from parent commit, add files that were staged for addition
    , and untrack files that were staged for removal.*/
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
    /* Display sha1 hash of this node, commit date and commit message.*/
    public void displayCommitNode (String nodeID) {
        System.out.println("===");
        System.out.println("commit" + " " + nodeID);
        if (secondParent != null) {
            System.out.printf("Merge: %s %s%n", parent.substring(0, 7), secondParent.substring(0, 7));
        }
        System.out.println("Date:" + " " + commitDate.toString());
        System.out.println(message);
        System.out.println();
    }
    /* Delete all files in CWD if they were tracked by this commit.*/
    public void deleteTrackedFileInCWD () {
        for (String file : contentMapping.keySet()) {
            File cur = join(CWD, file);
            if (cur.exists()) {
                join(CWD, file).delete();
            }
        }
    }
    /* Delete all files tracked by preHead and put all files tracked by this commit to CWD.*/
    public void checkout(Commit preHead) {
        preHead.checkUnTrackedFiles();
        preHead.deleteTrackedFileInCWD();
        for (String file : contentMapping.keySet()) {
            String fileContent = readContentsAsString(join(BLOBS, contentMapping.get(file)));
            writeContents(join(CWD, file), fileContent);
        }
    }
    /* Check if any files in CWD is untracked by this commit.*/
    public void checkUnTrackedFiles() {
        List<String> filesInCWD = plainFilenamesIn(CWD);
        assert filesInCWD != null;
        for (String file : filesInCWD) {
            if (!contentMapping.containsKey(file)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                System.exit(1);
            }
        }
    }
    /* Find the latest common ancestor of two commits.*/
    public static String latestCommonAncestor(String c1, String c2) {
        while (!c1.equals(c2)) {
            c1 = c1 == null ? c2 : getCommitByID(c1).getParent();
            c2 = c2 == null ? c1 : getCommitByID(c2).getParent();
        }
        return c1;
    }
    /* Compare tracked files in two commits, */
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
