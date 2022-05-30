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
        storeHead(head);

    }

    public static void addFiles(String fileName) {
        /* TODO : if the file exists in previous commit, instead of creating a new file,
           just change the stage map */
        /* TODO : what if two files with different name have the same content. */
        // TODO : 11:17pm,
        if (!checkExisted(fileName)) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        File newFile = join(CWD, fileName);
        Commit previous = getHead();
        // read previous commit from GITLET_DIR folder
        Map<String, String> commit_map = previous.getContentMapping();
        String fileContent = Utils.readContentsAsString(newFile);
        String contentId = Utils.sha1(fileContent);
        // current staged file
        Staged cur_staged = STAGED.exists() ? readObject(STAGED, Staged.class) : new Staged();
        Map<String, String> stage_map = cur_staged.getStageMap();
        // If the file user want to stage is the same as it in previous commit
        if (commit_map.containsKey(fileName) && commit_map.get(fileName).equals(contentId)) {
            // delete the previous staged file if exists
            if (stage_map.containsKey(fileName)) {
                File staged_file = join(BLOBS, stage_map.get(fileName));
                /* TODO : this will delete files in previous commit,
                   if added file have the exact same content as it in files in previous commit. */
                if (!cur_staged.isCommitted(fileName)) staged_file.delete();
                cur_staged.remove(fileName);
                if (cur_staged.isEmpty())  STAGED.delete(); else Utils.writeObject(STAGED, cur_staged);
            }
        } else {
            if (stage_map.containsKey(fileName)) {
                if (stage_map.get(fileName).equals(contentId)) {
                    return;
                }
                /* The files are staged in the blobs*/
                File staged_file = join(BLOBS, stage_map.get(fileName));
                /* If we don't change this file back to its previous version,*/
                if (!cur_staged.isCommitted(fileName)) staged_file.delete();
            }
            stage_map.put(fileName, contentId);
            File staged_file = join(BLOBS, contentId);
            // write the file contents as string and write serialized stage object
            if (staged_file.exists()) {
                cur_staged.addToCommitted(fileName);
            } else {
                Utils.writeContents(staged_file, fileContent);
            }
            Utils.writeObject(STAGED, cur_staged);
        }
    }

    public static void commit(String msg) {
        if (!STAGED.exists() || Utils.readObject(STAGED, Staged.class).isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        Commit previous = getHead();
        Commit new_commit = new Commit(msg, getHeadId());
        Staged staged_files = Utils.readObject(STAGED, Staged.class);
        Map<String, String> staged_map = staged_files.getStageMap();
        Map<String, String> commit_map = previous.getContentMapping();
        // update mapping of new commit
        new_commit.updateContent(commit_map, staged_map);
        STAGED.delete();
        storeHead(new_commit);
    }

    public static void rm(String fileName) {

        Staged cur_staged = getStaged();
        Map<String, String> cur_staged_map = cur_staged.getStageMap();
        Commit pre_commit = getHead();
        Map<String, String> pre_commit_map = pre_commit.getContentMapping();

        if (!pre_commit_map.containsKey(fileName) && !cur_staged_map.containsKey(fileName)) {
            System.out.println("No reason to remove the file.");
            System.exit(0);
        }

    }

    private static String getHeadId() {
        return Utils.readObject(HEADS, String.class);
    }

    private static void storeHead(Commit head) {
        byte[] serializedHead =  Utils.serialize(head);
        String headID = Utils.sha1(serializedHead);
        Utils.writeObject(HEADS, headID);
        File savedCommit = join(COMMITS, headID);
        Utils.writeContents(savedCommit, serializedHead);
    }


    private static Commit getHead() {
        String headID = getHeadId();
        File pre_commit = join(COMMITS, headID);
        Commit head = readObject(pre_commit, Commit.class);
        return head;
    }

    private static boolean checkExisted(String fileName) {
        File newFile = join(CWD, fileName);
        return newFile.exists();
    }

    private static Staged getStaged() {
        if (!STAGED.exists()) {
            return new Staged();
        }
        return readObject(STAGED, Staged.class);
    }

    private static void updateStageFile(Staged cur_staged) {
        if (cur_staged.isEmpty()) {
            STAGED.delete();
        } else {
            Utils.writeObject(STAGED, cur_staged);
        }
    }

    public static void test() {

        Staged stage1 = readObject(STAGED, Staged.class);
        Map<String, String> mapping = stage1.getStageMap();
        System.out.println(mapping);

    }


}
