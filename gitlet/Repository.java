package gitlet;

import java.io.File;
import java.util.*;

import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author Xingrong Chen
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */
    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /* The directory contains backup files. */
    public static final File BLOBS = join(GITLET_DIR, "blobs");
    /* The directory contains information about commits. */
    public static final File COMMITS = join(GITLET_DIR, "commits");
    /* The file records the head commit of this repository. */
    public static final File HEADS = join(GITLET_DIR, "HEADS");
    /* The file records the file be staged*/
    public static final File STAGED = join(GITLET_DIR, "STAGED");

    public static boolean isRepositoryDir() {
        return GITLET_DIR.exists();
    }

    public static void creatRepository() {
        if (isRepositoryDir()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            return;
        }
        // create all the working directories which are required.
        GITLET_DIR.mkdir();
        BLOBS.mkdir();
        COMMITS.mkdir();

        Commit head = new Commit("initial commit", null);
        storeHead(head);

    }

    public static void add(String fileName) {
        Staged cur_staged = getStaged();
        Commit previous = getHead();
        if (fileName.equals(".")) {
            List<String> untracked = new ArrayList<String>();
            List<String> unknownModification = new ArrayList<String>();
            reviewChange(unknownModification, untracked);
            for (String file : untracked) {
                addFiles(file, cur_staged, previous);
            }
            for (String file : unknownModification) {
                String[] splitFile = file.split(" ");
                if (splitFile[1].equals("(Modified)")) {
                    addFiles(splitFile[0], cur_staged, previous);
                } else {
                    rm(splitFile[0]);
                }
            }
        } else {
            addFiles(fileName, cur_staged, previous);
        }
        /* Update content of STAGE*/
        cur_staged.updateStageFile();
    }


    public static void addFiles(String fileName, Staged cur_staged, Commit previous) {
        if (!join(CWD, fileName).exists()) {
            System.out.println("File does not exist.");
            System.exit(1);
        }
        // read previous commit from GITLET_DIR folder
        String fileContent = Utils.readContentsAsString(join(CWD, fileName));
        Blobs fileInBlob = new Blobs(fileContent, true);
        String contentId = Utils.sha1(Utils.serialize(fileInBlob));
        // If the file user want to stage is the same as it in previous commit
        if (previous.contains(fileName) && previous.getId(fileName).equals(contentId)) {
            // delete the previous staged file if exists
            cur_staged.unStaged(fileName);
        } else {
            stageFileForAddition(fileName, fileContent, cur_staged);
        }
    }

    public static void commit(String msg) {
        if (!STAGED.exists() || Utils.readObject(STAGED, Staged.class).isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        Commit previous = getHead();
        Commit new_commit = new Commit(msg, getHeadId(), previous.getContentMapping());
        Staged staged_files = Utils.readObject(STAGED, Staged.class);
        /* update mapping of new commit. */
        updateContentForCommit(new_commit.getContentMapping(), staged_files);
        cleanStageArea();
        storeHead(new_commit);
    }

    public static int rm(String fileName) {

        int status = 0;
        Staged cur_staged = getStaged();
        Commit pre_commit = getHead();

        if (!cur_staged.contains(fileName) && !pre_commit.contains(fileName)) {
            System.out.println("No reason to remove the file.");
            return status;
        }
        cur_staged.unStaged(fileName);
        if (pre_commit.contains(fileName)) {
            cur_staged.stageForRemove(fileName);
            if (join(CWD, fileName).exists()) {
                join(CWD, fileName).delete();
            }
        }
        cur_staged.updateStageFile();
        return status;
    }

    public static void printLog() {
        String nodeId = getHeadId();
        Commit node = getHead(nodeId);
        while (node != null) {
            displayNode(node, nodeId);
            nodeId = node.getParent();
            node = getHead(nodeId);
        }
    }

    public static void printStatus() {
        Staged curStaged = getStaged();
        System.out.println("=== Branches ===");
        /* TODO: implement the branches. */
        System.out.println("");
        System.out.println("=== Staged For Addition ===");
        for (String file : curStaged.getStageMap().keySet()) {
            System.out.println(file);
        }
        System.out.println("");
        System.out.println("=== Staged For Removal ===");
        for (String file : curStaged.getRemoval()) {
            System.out.println(file);
        }
        System.out.println("");
        List<String> unknownModification = new ArrayList<String>();
        List<String> unTracked = new ArrayList<String>();
        reviewChange(unknownModification, unTracked);
        System.out.println("=== Modifications Not Staged For Commit ===");
        Collections.sort(unknownModification);
        for (String file : unknownModification) {
            System.out.println(file);
        }
        System.out.println("");
        System.out.println("=== Untracked Files ===");
        Collections.sort(unTracked);
        for (String file : unTracked) {
            System.out.println(file);
        }
        System.out.println("");
    }

    public static void checkoutFile(String[] args) {
        if (args.length <= 1 || args[1] == null ||  args[1].equals("")) {
            System.out.println("Please enter a file name to checkout");
            System.exit(0);
        }
        /*TODO : java gitlet.Main checkout [branch name].*/
        if (args[1].equals("--") && args.length == 3) {
            checkoutFileInCommit(args[2], getHeadId());
        } else if (args.length == 4 && args[2].equals("--")) {
            checkoutFileInCommit(args[3], args[1]);
        } else {
            System.out.println("Wrong arguments,");
            System.out.println("    java gitlet.Main checkout -- [file name]");
            System.out.println("    java gitlet.Main checkout [commit id] -- [file name]");
            System.out.println("    java gitlet.Main checkout [branch name]");
        }
    }

    private static void checkoutFileInCommit(String fileName, String commitId) {
        Commit head = getHead(commitId);
        if (!head.contains(fileName)) {
            System.out.println("File does not exist in this commit.");
            System.exit(0);
        }
        String fileId = head.getId(fileName);
        String fileContent = readObject(join(BLOBS, fileId), Blobs.class).getContent();
        writeContents(join(CWD, fileName), fileContent);
    }

    private static String getHeadId() {
        return Utils.readObject(HEADS, String.class);
    }

    private static void displayNode (Commit node, String nodeId) {
        System.out.println("===");
        System.out.println("commit" + " " + nodeId);
        System.out.println("Date:" + " " + node.getCommitDate().toString());
        System.out.println(node.getMessage());
        System.out.println("");
    }

    private static void storeHead(Commit head) {
        byte[] serializedHead =  Utils.serialize(head);
        String headID = Utils.sha1(serializedHead);
        Utils.writeObject(HEADS, headID);
        Utils.writeContents(join(COMMITS, headID), serializedHead);
    }

    /* Get the HEAD commit object*/
    private static Commit getHead() {
        String headID = getHeadId();
        Commit head = readObject(join(COMMITS, headID), Commit.class);
        return head;
    }

    private static Commit getHead(String commitId) {
        if (commitId == null) {
            return null;
        }
        if (!join(COMMITS, commitId).exists()) {
            System.out.println("No commit with this id exists. ");
            System.exit(0);
        }
        Commit commit = readObject(join(COMMITS, commitId), Commit.class);
        return commit;
    }
    /* Check if a file is existed.*/
    private static boolean checkExisted(String fileName) {
        File newFile = join(CWD, fileName);
        return newFile.exists();
    }
    /* Read Staged object from STAGED file*/
    private static Staged getStaged() {
        if (!STAGED.exists()) {
            return new Staged();
        }
        return readObject(STAGED, Staged.class);
    }


    private static void stageFileForAddition(String fileName, String fileContent, Staged curStage) {
        Blobs blobs = new Blobs(fileContent);
        byte[] serialized = Utils.serialize(blobs);
        String blobId = Utils.sha1(serialized);
        if (curStage.contains(fileName)) {
            if (curStage.get(fileName).equals(blobId)) {
                return;
            }
            curStage.unStaged(fileName);
        }
        curStage.put(fileName, blobId);
        writeContents(join(BLOBS, blobId), serialized);
    }

    private static void updateContentForCommit(Map<String, String> commit, Staged staged) {
        Map<String, String> stageMap = staged.getStageMap();
        for (String file : stageMap.keySet()) {
            String blobId = stageMap.get(file);
            Blobs blob = Utils.readObject(join(BLOBS, blobId), Blobs.class);
            Blobs contentForCommit = new Blobs(blob.getContent(), true);
            byte[] serialized = Utils.serialize(contentForCommit);
            String contentId = Utils.sha1(serialized);
            commit.put(file, contentId);
            if (!join(BLOBS, contentId).exists()) {
                Utils.writeContents(join(BLOBS, contentId), serialized);
            }
        }
        for (String file : staged.getRemoval()) {
            commit.remove(file);
        }
    }

    private static void cleanStageArea() {
        Staged curStage = getStaged();
        for (String file : curStage.getStageMap().keySet()) {
            String blobId = curStage.get(file);
            join(BLOBS, blobId).delete();
        }
        STAGED.delete();
    }

    private static void reviewChange(List<String> unknownModification, List<String> unTracked) {
        /* TODO : check the logic of two pointer method*/
        List<String> allFiles = plainFilenamesIn(CWD);
        Staged curStaged = getStaged();
        Commit preCommit = getHead();
        Set<String> stagedSetCopy = new TreeSet<String>(curStaged.getStageMap().keySet());

        Iterator<String> commitIter = preCommit.getContentMapping().keySet().iterator();
        int ptr = 0;
        String fileInCommit = commitIter.hasNext() ? commitIter.next() : null;
        String fileInCwd = ptr < allFiles.size() ? allFiles.get(ptr) : null;
        while (fileInCwd != null || fileInCommit != null) {
            if (fileInCwd == null || (fileInCommit != null && fileInCommit.compareTo(fileInCwd) < 0)) {
                /* Not staged for removal,
                but tracked in the current commit and deleted from the working directory.*/
                if (!curStaged.isForRemoval(fileInCommit)) {
                    unknownModification.add(fileInCommit + " (Deleted)");
                    if (stagedSetCopy.contains(fileInCommit)) {
                        stagedSetCopy.remove(fileInCommit);
                    }
                }
                fileInCommit = commitIter.hasNext() ? commitIter.next() : null;
            } else if (fileInCommit == null || fileInCommit.compareTo(fileInCwd) > 0) {
                /* Neither be staged for addition nor be tracked in current commit. */
                if (!curStaged.contains(fileInCwd)) {
                    unTracked.add(fileInCwd);
                } else {
                    String cwdId = sha1(serialize(new Blobs(readContentsAsString(join(CWD, fileInCwd)))));
                    /* Staged for addition, but with different contents than in the working directory;*/
                    if (!curStaged.get(fileInCwd).equals(cwdId)) {
                        unknownModification.add(fileInCwd + " (Modified)");
                    }
                    stagedSetCopy.remove(fileInCwd);
                }
                ptr += 1;
                fileInCwd = ptr < allFiles.size() ? allFiles.get(ptr) : null;
            } else if (fileInCommit.equals(fileInCwd)) {
                String cwdContent = readContentsAsString(join(CWD, fileInCwd));
                String supposedCommitId = sha1(serialize(new Blobs(cwdContent, true)));
                String supposedStageId = sha1(serialize(new Blobs(cwdContent)));
                /* Tracked in the current commit, changed in the working directory, but not staged;*/
                if (!preCommit.getId(fileInCommit).equals(supposedCommitId)) {
                    if (!curStaged.contains(fileInCwd) || !curStaged.get(fileInCwd).equals(supposedStageId)) {
                        unknownModification.add(fileInCwd + " (Modified)");
                    }
                    if (curStaged.contains(fileInCwd)) {
                        stagedSetCopy.remove(fileInCwd);
                    }
                }
                ptr += 1;
                fileInCommit = commitIter.hasNext() ? commitIter.next() : null;
                fileInCwd = ptr < allFiles.size() ? allFiles.get(ptr) : null;
            }
        }
        /* Staged for addition, but deleted in the working directory; */
        for (String file : stagedSetCopy) {
            unknownModification.add(file + " (Deleted)");
        }
    }
    public static void test() {

        String content = readContentsAsString(join(CWD, "untracked.txt"));
        byte[] bytes = serialize(new Blobs(content, true));
        System.out.println(sha1(bytes));
        Commit head = getHead();
        System.out.println(head.getContentMapping());

    }


}
