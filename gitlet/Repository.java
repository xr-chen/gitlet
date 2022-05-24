package gitlet;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

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



    public static void creatRepository() {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }
        // create all the working directories which are required.
        GITLET_DIR.mkdir();
        BLOBS.mkdir();
        COMMITS.mkdir();

        Commit head = new Commit("initial commit", null);
        byte[] serializedHead =  Utils.serialize(head);
        String headID = Utils.sha1(serializedHead);
        Utils.writeObject(HEADS, headID);
        File savedCommit = join(COMMITS, headID);
        Utils.writeContents(savedCommit, serializedHead);

    }

    public static void addFiles(String fileName) {
        Commit previous = getHead();
        // read previous commit from GITLET_DIR folder
        Map<String, String> commit_map = previous.getContentMapping();
        File newFile = join(CWD, fileName);
        if (!newFile.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        String fileContent = Utils.readContentsAsString(newFile);
        String contentId = Utils.sha1(fileContent);
        // current staged file
        // TODO: correct the function of stage map
        Staged cur_staged = STAGED.exists() ? readObject(STAGED, Staged.class) : new Staged();
        Map<String, String> stage_map = cur_staged.getStageMap();
        // If the file user want to stage is the same as it in previous commit
        if (commit_map.containsKey(fileName) && commit_map.get(fileName).equals(contentId)) {
            // delete the previous staged file if exists
            if (stage_map.containsKey(fileName)) {
                File staged_file = join(BLOBS, stage_map.get(fileName));
                staged_file.delete();
                stage_map.remove(fileName);
                if (cur_staged.isEmpty())  STAGED.delete(); else Utils.writeObject(STAGED, cur_staged);
            }
        } else {
            if (stage_map.containsKey(fileName) && !stage_map.get(fileName).equals(contentId)) {
                File staged_file = join(BLOBS, stage_map.get(fileName));
                staged_file.delete();
            }
            stage_map.put(fileName, contentId);
            File stage_file = join(BLOBS, contentId);
            // write the file contents as string and write serialized stage object
            Utils.writeContents(stage_file, fileContent);
            Utils.writeObject(STAGED, cur_staged);
        }
    }


    private static Commit getHead() {
        String headID = Utils.readObject(HEADS, String.class);
        File pre_commit = join(COMMITS, headID);
        Commit head = readObject(pre_commit, Commit.class);
        return head;
    }

    public static void test() {

        Staged stage1 = readObject(STAGED, Staged.class);
        Map<String, String> mapping = stage1.getStageMap();
        System.out.println(mapping);

    }


}
