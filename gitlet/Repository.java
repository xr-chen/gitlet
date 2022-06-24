package gitlet;

import java.io.File;
import java.util.*;

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
            System.out.println("A Gitlet version-control system already exists in the current directory.");
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

    public static void add(String fileName) {
        Staged cur_staged = Staged.getStaged();
        Commit previous = Branch.getHead();
        if (fileName.equals(".")) {
            List<String> untracked = new ArrayList<>();
            List<String> unknownModification = new ArrayList<>();
            cur_staged.reviewChange(unknownModification, untracked);
            for (String file : untracked) {
                addFile(file, cur_staged, previous);
            }
            for (String file : unknownModification) {
                String[] splitFile = file.split(" ");
                if (splitFile[1].equals("(Modified)")) {
                    addFile(splitFile[0], cur_staged, previous);
                } else {
                    rm(splitFile[0]);
                }
            }
        } else {
            addFile(fileName, cur_staged, previous);
        }
        /* Update content of STAGE*/
        cur_staged.updateStageFile();
    }

    /* Passing staged and commit object to save the time for repeatedly reading head and staged object */
    private static void addFile(String fileName, Staged cur_staged, Commit previous) {
        /* We distinguish staged blob from committed blob by file name,
        * if a blob is a staged blob, then its name ends with '--'. */
        if (!join(CWD, fileName).exists()) {
            System.out.println("File does not exist.");
            System.exit(1);
        }
        String fileContent = Utils.readContentsAsString(join(CWD, fileName));
        String contentId = Utils.sha1(fileContent);
        /* If the file user want to stage for addition has the same content
        *  as its version in the latest commit, we remove the file from staging area.*/
        if (previous.tracked(fileName, contentId)) {
            /* This could be the case where the file has been staged for removal then add it back,
            *  or has been modified and staged then change it back to its original version. */
            cur_staged.unStaged(fileName);
        } else {
            cur_staged.stageFileForAddition(fileName, fileContent, contentId + "--");
        }
    }

    public static void commit(String msg) {
        if (!STAGED.exists() || Utils.readObject(STAGED, Staged.class).isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(1);
        }
        Commit new_commit = new Commit(msg, Branch.getHeadId());
        Staged staged = Staged.getStaged();
        new_commit.updateContentForCommit(staged);
        staged.cleanStageArea();
        Branch.storeHead(new_commit);
    }

    public static void rm(String fileName) {
        Staged cur_staged = Staged.getStaged();
        Commit pre_commit = Branch.getHead();
        if (!cur_staged.contains(fileName) && !pre_commit.contains(fileName)) {
            System.out.println("No reason to remove the file.");
            return;
        }
        /* Unstaged file if it was staged for addition*/
        cur_staged.unStaged(fileName);
        if (pre_commit.contains(fileName)) {
            /* If the file was tracked by current commit, stage if for removal and remove it from CWD. */
            cur_staged.stageForRemove(fileName);
            if (join(CWD, fileName).exists()) {
                join(CWD, fileName).delete();
            }
        }
        cur_staged.updateStageFile();
    }

    public static void printLog() {
        String nodeID = Branch.getHeadId();
        Commit node = Commit.getCommitByID(nodeID);
        while (node != null) {
            node.displayCommitNode(nodeID);
            nodeID = node.getParent();
            node = Commit.getCommitByID(nodeID);
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
        Staged curStaged = Staged.getStaged();
        System.out.println("=== Branches ===");
        Branch.printAllBranches();
        System.out.println("");
        /* Status of files were staged for addition or removal. */
        curStaged.printStageStatus();
        List<String> unknownModification = new ArrayList<>();
        List<String> unTracked = new ArrayList<>();
        curStaged.reviewChange(unknownModification, unTracked);
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


    public static void checkoutFileInCommit(String fileName, String commitID) {
        Commit.getCommitByID(commitID).checkoutFile(fileName);
    }

    public static void checkOutBranch(String branchName) {
        Commit head = Branch.getHead(branchName);
        head.checkout();
        Branch.switchTo(branchName);
    }



    public static void test() {
        System.out.println("test");
    }

}
