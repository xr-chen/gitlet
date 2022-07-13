package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static gitlet.Repository.BLOBS;
import static gitlet.Repository.STAGED;
import static gitlet.Repository.CWD;
import static gitlet.Utils.join;
import static gitlet.Utils.plainFilenamesIn;
import static gitlet.Utils.readContentsAsString;
import static gitlet.Utils.readObject;
import static gitlet.Utils.sha1;
import static gitlet.Utils.writeContents;

/** Represents a gitlet Staged object, which contains the files were staged for addition,
 *  or staged for removal, assists to create a new commit object based on staged files,
 *  and identify untracked files and unknown modification of files in CWD
 *
 *  @author Xingrong Chen
 */
public class Staged implements Serializable {

    /* map the file name to its reference to blob (sha1ID of file content) or a string,
    * if the value is a reference, then this file was staged for addition
    * if the value is "remove", then this file was staged for removal. */
    private final Map<String, String> stageMap = new TreeMap<>();

    public boolean isEmpty() {
        return stageMap.isEmpty();
    }

    public List<String> getStagedFiles() { return new ArrayList<>(stageMap.keySet()); }

    /* When this file is ready to be committed, rename the corresponding blob file (remove the last two '-'),
    *  then remove this file from stage map. */
    public String clearStageSymbol(String fileName) {
        String curID = stageMap.get(fileName);
        String returnID = curID.substring(0, curID.length() - 2);
        if (curID.equals("remove")) {
            return "remove";
        }
        stageMap.remove(fileName);
        File blobName = join(BLOBS, curID);
        boolean succeed = blobName.renameTo(join(BLOBS, returnID));
        if (!succeed) {
            blobName.delete();
        }
        return returnID;
    }

    public boolean contains(String fileName) {
        return stageMap.containsKey(fileName);
    }

    public void stageForRemove(String fileName) {
        stageMap.put(fileName, "remove");
    }

    /* Remove file from stage map and delete the corresponding stage blob. */
    public void unStaged(String fileName) {
        if (!stageMap.containsKey(fileName)) {
            return;
        }
        String blobID = stageMap.get(fileName);
        if (!blobID.equals("remove")) {
            join(BLOBS, blobID).delete();
        }
        stageMap.remove(fileName);
    }

    /* Replace serialized Staged object in .getlet/ with this Staged object.*/
    public void updateStageFile() {
        if (stageMap.isEmpty()) {
            STAGED.delete();
        } else {
            Utils.writeObject(STAGED, this);
        }
    }
    /* Read Staged object from STAGED file*/
    public static Staged getStaged() {
        if (!STAGED.exists()) {
            return new Staged();
        }
        return readObject(STAGED, Staged.class);
    }
    /* Stage a file for addition*/
    public void stageFileForAddition(String fileName, String fileContent, String blobID) {
        if (stageMap.containsKey(fileName)) {
            if (stageMap.get(fileName).equals(blobID)) return;
            unStaged(fileName);
        }
        stageMap.put(fileName, blobID);
        writeContents(join(BLOBS, blobID), fileContent);
    }
    /* Delete all cache file (temporary blob) and delete the serialized Staged object.*/
    public void cleanStageArea() {
        for (String file : stageMap.keySet()) {
            String blobId = stageMap.get(file);
            join(BLOBS, blobId).delete();
        }
        STAGED.delete();
    }
    /* Print the status of staged files.*/
    public void printStageStatus() {
        System.out.println("=== Staged For Addition ===");
        List<String> stageForRemoval = new ArrayList<>();
        for (Map.Entry<String, String> file : stageMap.entrySet()) {
            if (file.getValue().equals("remove")) {
                stageForRemoval.add(file.getKey());
            } else {
                System.out.println(file.getKey());
            }
        }
        System.out.println();
        System.out.println("=== Staged For Removal ===");
        for (String file : stageForRemoval) {
            System.out.println(file);
        }
        System.out.println();
    }
    /* Find out any untracked files and unknown modification in CWD.*/
    public void reviewChange(List<String> unknownModification, List<String> unTracked) {
        List<String> allFiles = plainFilenamesIn(CWD);
        assert allFiles != null;
        Commit head = Branch.getHead();
        Map<String, String> storedFiles = new TreeMap<>(head.getContentMapping());
        /* Update the reference if the file was changed and was staged for addition. */
        storedFiles.putAll(stageMap);

        Iterator<String> iter = storedFiles.keySet().iterator();
        int ptr = 0;
        String trackedFile = iter.hasNext() ? iter.next() : null;
        String fileInCwd = ptr < allFiles.size() ? allFiles.get(ptr) : null;

        while (fileInCwd != null || trackedFile != null) {
            if (fileInCwd == null || (trackedFile != null && trackedFile.compareTo(fileInCwd) < 0)) {
                /* Staged for addition, but deleted in the working directory;
                   Not staged for removal, but tracked in the current commit and deleted from CWD. */
                if (!storedFiles.get(trackedFile).equals("remove")) {
                    unknownModification.add(trackedFile + " (Deleted)");
                }
                trackedFile = iter.hasNext() ? iter.next() : null;
            } else if (trackedFile == null || trackedFile.compareTo(fileInCwd) > 0) {
                /* Neither be staged for addition nor be tracked in current commit. */
                unTracked.add(fileInCwd);
                ptr += 1;
                fileInCwd = ptr < allFiles.size() ? allFiles.get(ptr) : null;
            } else if (trackedFile.equals(fileInCwd)) {
                /* Files that have been staged for removal, but then re-created without Gitlet’s knowledge.*/
                if (storedFiles.get(fileInCwd).equals("remove")) {
                    unTracked.add(fileInCwd);
                } else {
                    String cwdContent = readContentsAsString(join(CWD, fileInCwd));
                    String contentID = sha1(cwdContent);
                    String trackedID = storedFiles.get(fileInCwd);
                /* Tracked in the current commit, changed in the working directory, but not staged;
                   Staged for addition, but with different contents than in the working directory; */
                    if (!trackedID.equals(contentID) && !trackedID.equals(contentID + "--")) {
                        unknownModification.add(fileInCwd + " (Modified)");
                    }
                }
                trackedFile = iter.hasNext() ? iter.next() : null;
                ptr += 1;
                fileInCwd = ptr < allFiles.size() ? allFiles.get(ptr) : null;
            }
        }
    }
}
