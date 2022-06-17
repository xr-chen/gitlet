package gitlet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import static gitlet.Repository.GITLET_DIR;
import static gitlet.Utils.join;



public class branch implements Serializable {
    public static final File BRANCH = join(GITLET_DIR, "BRANCH");

    Map<String, String> nameToHead = new HashMap<String, String>();

    public branch(String name, String commit) {
        nameToHead.put(name, commit);
    }

    public void save() {
        Utils.writeObject(BRANCH, this);
    }
}
