package gitlet;

import java.io.File;
import java.util.*;

import static gitlet.Commit.getCommitByID;
import static gitlet.Utils.*;
import static gitlet.Branch.BRANCH;

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
    /* The file records name of branch or sha1Id of commit which head pointer is on.*/
    public static final File HEAD = join(GITLET_DIR, "HEAD");
    /* The file records the file be staged*/
    public static final File STAGED = join(GITLET_DIR, "STAGED");

    public static boolean isRepositoryDir() {
        return GITLET_DIR.exists();
    }

    public static void creatRepository() {
        if (isRepositoryDir()) {
            String msg = "A Gitlet version-control system already exists in the current directory.";
            System.out.println(msg);
            return;
        }
        // create all the necessary working directories.
        GITLET_DIR.mkdir();
        BLOBS.mkdir();
        COMMITS.mkdir();
        BRANCH.mkdir();

        Commit head = new Commit("initial commit", null);
        Branch.createNewBranch("master", "");
        Branch.switchTo("master");
        Branch.storeHead(head);

    }

    public static void add(String[] fileName) {
        Staged curStaged = Staged.getStaged();
        Commit previous = Branch.getHead();
        /* We distinguish staged blob from committed blob by file name,
         * if a blob is a staged blob, then its name ends with '--'. */
        List<String> allFiles = getFiles(fileName);
        for (String file : allFiles) {
            String fileContent = Utils.readContentsAsString(join(CWD, file));
            String contentId = Utils.sha1(fileContent);
            /* If the file user want to stage for addition has the same content
             *  as its version in the latest commit, we remove the file from staging area.*/
            if (previous.tracked(file, contentId)) {
                /* This could be the case where the file has been staged for removal then add it back,
                 *  or has been modified and staged then change it back to its original version. */
                curStaged.unStaged(file);
            } else {
                curStaged.stageFileForAddition(file, fileContent, contentId + "--");
            }
        }
        /* Update content of STAGE*/
        curStaged.updateStageFile();
    }

    public static void commit(String msg) {
        if (!STAGED.exists() || Utils.readObject(STAGED, Staged.class).isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(1);
        }
        Commit newCommit = new Commit(msg, Branch.getHeadId());
        Staged staged = Staged.getStaged();
        newCommit.updateContentForCommit(staged);
        staged.cleanStageArea();
        Branch.storeHead(newCommit);
    }

    public static void rm(String fileName) {
        Staged curStaged = Staged.getStaged();
        Commit preCommit = Branch.getHead();
        if (!curStaged.contains(fileName) && !preCommit.contains(fileName)) {
            System.out.println("No reason to remove the file.");
            return;
        }
        /* Unstaged file if it was staged for addition*/
        curStaged.unStaged(fileName);
        if (preCommit.contains(fileName)) {
            /* If the file was tracked by current commit, stage if for removal and remove it from CWD. */
            curStaged.stageForRemove(fileName);
            if (join(CWD, fileName).exists()) {
                join(CWD, fileName).delete();
            }
        }
        curStaged.updateStageFile();
    }

    public static void printLog() {
        String nodeID = Branch.getHeadId();
        Commit node = getCommitByID(nodeID);
        while (node != null) {
            node.displayCommitNode(nodeID);
            nodeID = node.getParent();
            node = getCommitByID(nodeID);
        }
    }

    public static void globalLog() {
        List<String> allCommit = plainFilenamesIn(COMMITS);
        for (String commit : allCommit) {
            Commit cur = readObject(join(COMMITS, commit), Commit.class);
            cur.displayCommitNode(commit);
        }
    }

    public static void findWithMsg(String msg) {
        List<String> allCommit = plainFilenamesIn(COMMITS);
        for (String commit : allCommit) {
            Commit cur = readObject(join(COMMITS, commit), Commit.class);
            if (cur.getMessage().equals(msg)) {
                System.out.println(commit);
            }
        }
    }

    public static void printStatus() {
        Staged curStage = Staged.getStaged();
        System.out.println("=== Branches ===");
        Branch.printAllBranches();
        System.out.println("");
        /* Status of files were staged for addition or removal. */
        curStage.printStageStatus();
        List<String> unknownModification = new ArrayList<>();
        List<String> unTracked = new ArrayList<>();
        curStage.reviewChange(unknownModification, unTracked);
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

    public static void checkoutFileInCommit(String fileName, String commitId) {
        getCommitByID(commitId).checkoutFile(fileName);
    }

    public static void checkOutBranch(String branchName) {
        if (readContentsAsString(HEAD).equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(1);
        }
        Commit head = Branch.getHead(branchName);
        Commit curHead = Branch.getHead();
        head.checkout(curHead);
        Branch.switchTo(branchName);
        Staged.getStaged().cleanStageArea();
    }

    public static void branch(String branchName) {
        Branch.createNewBranch(branchName, Branch.getHeadId());
    }

    public static void removeBranch(String branchName) {
        Branch.deleteBranch(branchName);
    }

    public static void reset(String commitId) {
        Commit head = getCommitByID(commitId);
        head.checkout(Branch.getHead());
        Branch.updateHead(commitId);
        Staged.getStaged().cleanStageArea();
    }

    public static void merge(String branchName) {
        String curBranchId = Branch.getHeadId();
        String givenBranchId = Branch.getHeadId(branchName);
        String splitPoint = Commit.latestCommonAncestor(curBranchId, givenBranchId);
        Staged curStage = Staged.getStaged();

        if (splitPoint.equals(givenBranchId)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        } else if (splitPoint.equals(curBranchId)) {
            System.out.println("Current branch fast-forwarded.");
            checkOutBranch(branchName);
            return;
        } else {
            Commit splitCommit = getCommitByID(splitPoint);

            Map<String, String> givenDiff = splitCommit.compareDiff(getCommitByID(givenBranchId));
            Map<String, String> curDiff = splitCommit.compareDiff(getCommitByID(curBranchId));

            Iterator<Map.Entry<String, String>> givenIter = givenDiff.entrySet().iterator();
            Iterator<Map.Entry<String, String>> thisIter = curDiff.entrySet().iterator();
            Map.Entry<String, String> givenFile = givenIter.hasNext() ? givenIter.next() : null;
            Map.Entry<String, String> thisFile = thisIter.hasNext() ? thisIter.next() : null;

            while (givenFile != null || thisFile != null) {
                if (thisFile == null || (givenFile != null && givenFile.getKey().compareTo(thisFile.getKey()) < 0)) {
                    String[] val = givenFile.getValue().split(",");
                    if (val[1].equals("deleted")) {
                        curStage.stageForRemove(givenFile.getKey());
                    } else {
                        String content = readContentsAsString(join(BLOBS, val[0]));
                        curStage.stageFileForAddition(givenFile.getKey(), content, val[0] + "--");
                    }
                    givenFile = givenIter.hasNext() ? givenIter.next() : null;
                } else if (givenFile == null || givenFile.getKey().compareTo(thisFile.getKey()) > 0) {
                    thisFile = thisIter.hasNext() ? thisIter.next() : null;
                } else if (givenFile.getKey().equals(thisFile.getKey())) {
                    String[] givenVal = givenFile.getValue().split(",");
                    String[] thisVal = thisFile.getValue().split(",");
                    if (givenVal[1].equals(thisVal[1]) && givenVal[0].equals(thisVal[0])) {
                        continue;
                    } else {
                        System.out.println("Encountered a merge conflict.");
                        StringBuilder sb = new StringBuilder();
                        sb.append("<<<<<<< HEAD\n");
                        sb.append(thisVal[1].equals("deleted") ? "" : readContentsAsString(join(BLOBS, thisVal[0])));
                        sb.append("=======\n");
                        sb.append(givenVal[1].equals("deleted") ? "" : readContentsAsString(join(BLOBS, givenVal[0])));
                        sb.append(">>>>>>>\n");
                        String content = sb.toString();
                        curStage.stageFileForAddition(thisFile.getKey(), content, sha1(content) + "--");
                    }
                }
            }
        }
        curStage.updateStageFile();
        commit(String.format("Merged %S into %s.", branchName, readContentsAsString(HEAD)));
        Branch.getHead().setSecondParent(givenBranchId);
    }

    private static List<String> getFiles(String[] fileName) {
        List<String> allFiles = new ArrayList<>();
        for (int i = 1; i < fileName.length; i += 1) {
            try {
                join(CWD, fileName[i]);
            } catch (Exception e) {
                System.out.println("File does not exist.");
                System.exit(1);
            }
            File cur = join(CWD, fileName[i]);
            if (!cur.exists()) {
                System.out.println("File does not exist.");
                System.exit(1);
            }
            if (cur.isFile()) {
                allFiles.add(fileName[i]);
            } else {
                allFiles.addAll(plainFilenamesIn(cur));
            }
        }
        return allFiles;
    }
}
