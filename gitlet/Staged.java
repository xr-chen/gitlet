package gitlet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Staged implements Serializable {
    // map the file name to its sha1ID
    private Map<String, String> stageMap = new HashMap<String, String>();

    public boolean isEmpty() {
        return stageMap.isEmpty();
    }

    public Map<String, String> getStageMap() {
        return stageMap;
    }
}
