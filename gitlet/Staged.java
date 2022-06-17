package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;

import static gitlet.Repository.BLOBS;
import static gitlet.Repository.STAGED;
import static gitlet.Utils.join;

public class Staged implements Serializable {

    // map the file name to the sha1ID of the blob contains this file
    private Map<String, String> stageMap = new TreeMap<String, String>();
    // Record files which are previously committed.
    /* Store name of files which are going to be removed in next commit*/
    private Set<String> removal = new TreeSet<String>();

    public boolean isEmpty() {
        return stageMap.isEmpty() && removal.isEmpty();
    }

    public Map<String, String> getStageMap() {
        return stageMap;
    }

    public boolean contains(String fileName) {
        return stageMap.containsKey(fileName);
    }

    public String get(String fileName) {
        return stageMap.get(fileName);
    }

    public void put(String fileName, String fileId) {
        stageMap.put(fileName, fileId);
    }

    public Set<String> getRemoval() { return removal; }

    public void stageForRemove(String fileName) {
        removal.add(fileName);
    }

    public void remove(String fileName) {
        stageMap.remove(fileName);
    }

    public boolean isForRemoval(String fileName) {
        return removal.contains(fileName);
    }
    /* Deleted the file name from stage map and then remove staged file. */
    public void unStaged(String fileName) {
        if (!stageMap.containsKey(fileName)) {
            return;
        }
        String blobId = stageMap.get(fileName);
        join(BLOBS, blobId).delete();
        stageMap.remove(fileName);
    }

    /* Update the file which contains information about a Staged object*/
    public void updateStageFile() {
        if (isEmpty()) {
            STAGED.delete();
        } else {
            Utils.writeObject(STAGED, this);
        }
    }
}
