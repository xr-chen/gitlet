package gitlet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.File;

import static gitlet.Repository.*;
import static gitlet.Utils.*;



public class Branch {
    public static final File BRANCH = join(GITLET_DIR, "heads");

    public static void createNewBranch(String branchName, String commitId) {
        File head = join(BRANCH, branchName);
        if (head.exists()) {
            System.out.println("A branch with provided name already exists.");
            System.exit(1);
        }
        writeContents(head, commitId);
    }

    public static void switchTo(String branchName) {
        writeContents(HEAD, branchName);
    }

    /* Update the head pointer of current branch. */
    public static void updateHead(String commmitID) {
        writeContents(join(BRANCH, readContentsAsString(HEAD)), commmitID);
    }

    private static void updateHead(String branchName, String commitID) {
        writeContents(join(BRANCH, branchName), commitID);
    }
    /* Update the head commit of current active branch,
       and store the sha1 hash of that commit in .gitlet/commits. */
    public static void storeHead(Commit head) {
        byte[] serializedHead =  Utils.serialize(head);
        String headID = Utils.sha1(serializedHead);
        // get the current branch
        String curBranch = Utils.readContentsAsString(HEAD);
        updateHead(curBranch, headID);
        /* store this commit in directory .getlet/commits, since commit object has been serialized,
        * we use writeContents method here*/
        Utils.writeContents(join(COMMITS, headID), serializedHead);
    }

    /* Get the sha1id of the commit at the front of current active branch. */
    public static String getHeadId() {
        return getHeadId(readContentsAsString(HEAD));
    }

    public static String getHeadId(String branchName) {
        if (!join(BRANCH, branchName).exists()) {
            System.out.println("No such branch exists.");
            System.exit(1);
        }
        return Utils.readContentsAsString(join(BRANCH, branchName));
    }
    /* Get the HEAD commit of current branch*/
    public static Commit getHead() {
        return Commit.getCommitByID(getHeadId());
    }

    public static Commit getHead(String branchName) {
        return Commit.getCommitByID(getHeadId(branchName));
    }

    public static void printAllBranches() {
        List<String> branches = plainFilenamesIn(BRANCH);
        String curBranch = readContentsAsString(HEAD);
        System.out.println("*" + curBranch);
        for (String branch : branches) {
            if (!branch.equals(curBranch)) {
                System.out.println(branch);
            }
        }
    }

    public static void deleteBranch(String branchName) {
        if (!join(BRANCH, branchName).exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(1);
        }
        if (readContentsAsString(HEAD).equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(1);
        }
        join(BRANCH, branchName).delete();
    }

}
