package gitlet;

import java.io.Serializable;
import static gitlet.Repository.GITLET_DIR;

public class Blobs implements Serializable {
    private boolean isCommitted;
    private String content;

    public Blobs (String file_content) {
        isCommitted = false;
        content = file_content;
    }

    public Blobs (String file_content, boolean committed) {
        content = file_content;
        isCommitted = committed;
    }


    public String getContent() {
        return content;
    }

    public boolean isCommitted() {
        return isCommitted;
    }
}

