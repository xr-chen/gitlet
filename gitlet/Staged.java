package gitlet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Staged implements Serializable {
    // map the file name to its sha1ID
    private Map<String, String> stageMap = new HashMap<String, String>();
    // Record files which are previously committed.
    private Set<String> existed = new HashSet<String>();
    /* Store file which will be removed in next commit*/
    private Set<String> removal = new HashSet<String>();

    public boolean isEmpty() {
        return stageMap.isEmpty();
    }

    public Map<String, String> getStageMap() {
        return stageMap;
    }

    public Set<String> getRemoval() { return removal; }

    public boolean isCommitted(String fileName) {
        return existed.contains(stageMap.get(fileName));
    }

    public void addToCommitted(String fileName) {
        existed.add(stageMap.get(fileName));
    }

    public void remove(String fileName) {
        existed.remove(stageMap.get(fileName));
        stageMap.remove(fileName);
    }
}
