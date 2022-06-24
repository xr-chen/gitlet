package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.*;

import static gitlet.Repository.*;
import static gitlet.Repository.CWD;
import static gitlet.Utils.*;

public class Staged implements Serializable {

    /* map the file name to sha1ID of file content or a string,
    if the value is sha1ID of file content, then this file was staged for addition
    * if the value is "remove", then this file was staged for removal. */
    private Map<String, String> stageMap = new TreeMap<String, String>();
    // Record files which are previously committed.
    /* Store name of files which are going to be removed in next commit*/

    public boolean isEmpty() {
        return stageMap.isEmpty();
    }

    public Map<String, String> getStageMap() {
        return stageMap;
    }

    public List<String> getStagedFiles() { return new ArrayList<>(stageMap.keySet()); }


    /* When this file is ready to be committed, remove last two '-' in the name of
       corresponding blob file, then delete this file name from stage map. */
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

    /* Deleted the file name from stage map and then the file was staged. */
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

    /* Update the file which contains information about a Staged object*/
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

    public void stageFileForAddition(String fileName, String fileContent, String blobID) {
        if (stageMap.containsKey(fileName)) {
            if (stageMap.get(fileName).equals(blobID)) return;
            unStaged(fileName);
        }
        stageMap.put(fileName, blobID);
        writeContents(join(BLOBS, blobID), fileContent);
    }

    public void cleanStageArea() {
        for (String file : stageMap.keySet()) {
            String blobId = stageMap.get(file);
            join(BLOBS, blobId).delete();
        }
        STAGED.delete();
    }

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
        System.out.println("");
        System.out.println("=== Staged For Removal ===");
        for (String file : stageForRemoval) {
            System.out.println(file);
        }
        System.out.println("");
    }

    public void reviewChange(List<String> unknownModification, List<String> unTracked) {
        List<String> allFiles = plainFilenamesIn(CWD);
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
                    continue;
                }
                String cwdContent = readContentsAsString(join(CWD, fileInCwd));
                String contentID = sha1(cwdContent);
                String trackedID = storedFiles.get(fileInCwd);
                /* Tracked in the current commit, changed in the working directory, but not staged;
                   Staged for addition, but with different contents than in the working directory; */
                if (!trackedID.equals(contentID) && !trackedID.equals(contentID + "--")) {
                    unknownModification.add(fileInCwd + " (Modified)");
                }
                trackedFile = iter.hasNext() ? iter.next() : null;
                ptr += 1;
                fileInCwd = ptr < allFiles.size() ? allFiles.get(ptr) : null;
            }
        }
    }
}
