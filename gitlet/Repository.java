package gitlet;

import java.io.File;
import java.util.Map;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Iterator;

import static gitlet.Commit.getCommitByID;
import static gitlet.Utils.join;
import static gitlet.Utils.plainFilenamesIn;
import static gitlet.Utils.readObject;
import static gitlet.Utils.readContentsAsString;
import static gitlet.Utils.sha1;
import static gitlet.Branch.BRANCH;

/** Represents a gitlet repository.
 *  It contains several functional path for a gitlet repository,
 *  and directly handles all the command passed from Main
 *
 *  @author Xingrong Chen
 */
public class Repository {

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /* The directory contains backup files. */
    public static final File BLOBS = join(GITLET_DIR, "blobs");
    /* The folder contains serialized commit object. */
    public static final File COMMITS = join(GITLET_DIR, "commits");
    /* The file which records branch name or sha1Id of commit object which the head pointer is on.*/
    public static final File HEAD = join(GITLET_DIR, "HEAD");
    /* Serialized Staged object*/
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
        /* Create all necessary working directories.*/
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
        /* We distinguish temporary blob (blob of staged file) from permanent blob by file name,
         * if a blob is a temporary blob, then its name ends with '--'. */
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
        assert allCommit != null;
        for (String commit : allCommit) {
            Commit cur = readObject(join(COMMITS, commit), Commit.class);
            cur.displayCommitNode(commit);
        }
    }

    public static void findWithMsg(String msg) {
        List<String> allCommit = plainFilenamesIn(COMMITS);
        assert allCommit != null;
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
        System.out.println();
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
        System.out.println();
        System.out.println("=== Untracked Files ===");
        Collections.sort(unTracked);
        for (String file : unTracked) {
            System.out.println(file);
        }
        System.out.println();
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
                    if (!givenVal[1].equals(thisVal[1]) || !givenVal[0].equals(thisVal[0])) {
                        System.out.println("Encountered a merge conflict.");
                        String content = "<<<<<<< HEAD\n" +
                                (thisVal[1].equals("deleted") ? "" : readContentsAsString(join(BLOBS, thisVal[0]))) +
                                "=======\n" +
                                (givenVal[1].equals("deleted") ? "" : readContentsAsString(join(BLOBS, givenVal[0]))) +
                                ">>>>>>>\n";
                        curStage.stageFileForAddition(thisFile.getKey(), content, sha1(content) + "--");
                    }
                    givenFile = givenIter.hasNext() ? givenIter.next() : null;
                    thisFile = thisIter.hasNext() ? thisIter.next() : null;
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
                List<String> files = plainFilenamesIn(cur);
                assert files != null;
                allFiles.addAll(files);
            }
        }
        return allFiles;
    }
}
