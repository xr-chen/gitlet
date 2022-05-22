package gitlet;

import java.io.File;
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
    /* The file contains log of this repository. */
    public static final File HEADS = join(GITLET_DIR, "HEADS");



    public static void creatRepository() {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }
        // create all the working directories which are required.
        GITLET_DIR.mkdir();
        BLOBS.mkdir();
        COMMITS.mkdir();

        Commit HEAD = new Commit("initial commit", null);
        byte[] serializedHead =  Utils.serialize(HEAD);
        String headID = Utils.sha1(serializedHead);
        Utils.writeObject(HEADS, headID);
        File savedCommit = join(COMMITS, headID);
        Utils.writeObject(savedCommit, HEAD);

    }



}
